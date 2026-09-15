# Arquitetura

Visão real do sistema, incluindo o frontend 

---

## 1. Visão geral

Monorepo com dois artefatos independentes e um banco.

```
┌─────────────────────────┐        ┌──────────────────────────────┐
│  frontend               │        │  backend                     │
│  React 19 + TS + Vite   │        │  Spring Boot 4.1 · Java 21   │
│  porta 3000 (nginx)     │        │  porta 8080                  │
│                         │        │                              │
│  ⚠️ dados 100% mockados  │╌╌╌╌╌╌╌>│  REST /api/**                │
│  (sem camada HTTP)      │ ainda  │  JWT HS256                   │
└─────────────────────────┘  não   └──────────────┬───────────────┘
                            ligado                │ JPA + Flyway
                                                  ▼
                                    ┌──────────────────────────────┐
                                    │  PostgreSQL 16               │
                                    │  19 migrations               │
                                    └──────────────────────────────┘
```

> **O frontend ainda não conversa com o backend.** Não existe `fetch`, `axios` nem
> qualquer cliente HTTP no `frontend/src`. Todas as telas leem de arquivos em
> `src/mocks/` e mutam estado local com `useState`. O login grava a string literal
> `"mock-token-123"` no `localStorage` e a usa apenas como flag booleana.
>
> Isso é o maior item em aberto do projeto. Detalhes e plano em
> [frontend.md](./frontend.md).

---

## 2. Backend — camadas

```
Controller       @RestController · @PreAuthorize · @Valid · DTOs
                 Sem regra de negócio. Traduz HTTP ↔ service.
      │
      ▼
Service          Regra de negócio · @Transactional · isolamento por oficina
                 Recebe e devolve DTO. Nunca expõe entidade para fora.
      │
      ▼
Repository       Spring Data JPA. Queries derivadas + @Query pontuais.
      │
      ▼
Model            Entidades JPA. ddl-auto=validate — o schema vem do Flyway.
```

Transversais:

| Componente | Papel |
|---|---|
| `security/` | filtro JWT, resolução do usuário logado, isolamento multi-tenant |
| `exception/GlobalExceptionHandler` | `@RestControllerAdvice` — exceção → status HTTP |
| `config/` | `SecurityConfig`, `OpenApiConfig`, `AdminSeeder` |
| `enums/` | `Role`, `StatusOrdemDeServico`, `StatusPagamento`, `MeioPagamento` |

### Pacotes

```
com.oficinapro
├── config/          SecurityConfig · OpenApiConfig · SpringDocConfig · AdminSeeder
├── controller/      12 controllers REST
├── dto/             records por domínio, subpasta por agregado
├── enums/           Role · StatusOrdemDeServico · StatusPagamento · MeioPagamento
├── exception/       GlobalExceptionHandler + exceções por domínio
├── model/           12 entidades JPA
├── repository/      interfaces Spring Data
├── security/        AuthenticatedUserProvider · OficinaAccessValidator
│   │                SecurityErrorResponder · UsuarioDetailsService
│   └── jwt/         JwtService · JwtConfig · JwtProperties · JwtAuthenticationFilter
└── service/         subpasta por agregado, interface + Impl
```

Os DTOs são `record`. Requisição e resposta são separados — nunca o mesmo tipo para
entrada e saída.

---

## 3. Segurança

### Cadeia de autenticação

```
Request
  │
  ├─ rota pública? (/api/auth/login, /actuator/health, /swagger-ui/**, /v3/api-docs/**)
  │     └─ segue sem autenticar
  │
  ├─ JwtAuthenticationFilter
  │     ├─ sem header Authorization ou sem prefixo "Bearer " → segue sem autenticar
  │     └─ com token → valida assinatura, issuer e expiração
  │           ├─ inválido → 401 via SecurityErrorResponder
  │           └─ válido   → carrega o Usuario e popula o SecurityContext
  │
  ├─ não autenticado em rota protegida → 401
  ├─ @PreAuthorize reprova o papel     → 403
  └─ controller → service
```

Configuração: `SecurityConfig` é `STATELESS`, **CSRF desabilitado** (não há cookie de
sessão, logo não há vetor de CSRF) e registra o `SecurityErrorResponder` como
`authenticationEntryPoint` e `accessDeniedHandler`.

`SecurityErrorResponder` existe porque erros da cadeia de filtros acontecem **antes** de
chegar a um controller e, portanto, não passam pelo `GlobalExceptionHandler`. Sem ele, o
Spring devolveria página HTML de erro. Ele responde no mesmo formato JSON do resto da
API.

### JWT

| Item | Valor |
|---|---|
| Algoritmo | HS256 (simétrico) |
| Segredo | `JWT_SECRET`, mínimo 32 bytes — a aplicação **não sobe** sem ele |
| Issuer | `oficinapro` |
| Expiração | 8h (`oficinapro.jwt.expiration`) |
| Claims | `sub` (username), `role`, `oficinaId` |

`oficinaId` é omitido no token do ADMIN, que não pertence a oficina.

`JwtConfig` valida o tamanho do segredo na subida e falha com mensagem explicando o que
fazer, em vez de estourar um erro cru de placeholder.

### Isolamento multi-tenant

Centralizado em `OficinaAccessValidator`. Antes estava duplicado em quatro services, e a
duplicação foi a causa-raiz de endpoints financeiros sem validação.

Regra que merece destaque: registro de outra oficina responde **404, não 403**.
Responder "sem permissão" confirmaria que o registro existe.

Matriz completa e dívidas conhecidas em [permissions.md](./permissions.md).

### AdminSeeder

`ApplicationRunner` que cria o ADMIN do SaaS na primeira subida. Não roda se
`ADMIN_USERNAME`/`ADMIN_PASSWORD` estiverem vazios, nem se já existir algum ADMIN. É
por isso que o perfil de teste define essas variáveis como vazias.

---

## 4. Modelo de domínio

```
Oficina 1──N Unidade
   │
   ├──N Pessoa (herança JOINED)
   │      ├── Cliente
   │      ├── Mecanico
   │      └── Usuario  (username, password, role)
   │
   ├──N Veiculo
   │
   └──N OrdemDeServico ──1 Pagamento ──N RegistroPagamento
             │
             ├──N ItemOsPeca
             └──N MaoObra
```

`Pessoa` usa `InheritanceType.JOINED`: tabela `pessoa` com os campos comuns (nome,
telefone, documento, oficina) e uma tabela por subtipo com a PK compartilhada. Evita
repetir os campos de pessoa em três lugares.

Detalhe do `Usuario`: além de entidade, implementa `UserDetails` do Spring Security —
é a mesma classe usada como principal autenticado.

ER completo, tabelas e migrations em [database.md](./database.md).

---

## 5. Um acoplamento que vale conhecer

`OrdemDeServicoServiceImpl` e `PagamentoServiceImpl` **dependem um do outro**:

- a OS abre o pagamento ao ser criada, e consulta o pagamento para permitir a transição
  para `FECHADA`;
- o pagamento resolve e valida a OS para saber a oficina e o valor.

Como o Spring Boot proíbe referências circulares por padrão, isso impedia o contexto de
subir — **a aplicação não iniciava**. Resolvido com `@Lazy` no parâmetro do construtor de
`OrdemDeServicoServiceImpl`:

```java
public OrdemDeServicoServiceImpl(
    ...,
    @Lazy PagamentoService pagamentoService,
    OficinaAccessValidator oficinaAccessValidator) {
```

Dois pontos importantes:

1. O `@Lazy` precisa estar **no ponto de injeção**. Anotar a classe
   `PagamentoServiceImpl` apenas adiaria a instanciação do bean, sem criar o proxy que
   rompe o ciclo — não funciona.
2. É **paliativo**. A correção estrutural é extrair a abertura do pagamento da criação da
   OS, seja com um service coordenador, seja com evento de domínio. Enquanto o `@Lazy`
   existir, o acoplamento continua lá.

O teste `OficinaProApplicationTests.contextLoads` é o que protege contra a reintrodução
do ciclo. Ele não é decorativo: foi ele que detectou o problema.

---

## 6. Fluxo completo — abrir uma OS e receber

```
1. POST /api/ordens-servico           GERENTE
   ├─ valida acesso à oficina
   ├─ resolve oficina, unidade, veículo (cliente e mecânico são opcionais)
   ├─ status ABERTA, valores zerados, dataAbertura = agora
   └─ cria o Pagamento (PAGAMENTO_PENDENTE, valorPago 0)

2. POST /api/itens-os-peca            GERENTE, MECANICO
   ├─ valida que a OS aceita edição (não CANCELADA nem FECHADA)
   ├─ valorTotal = quantidade × valorUnitario
   └─ recalculador: total = peças + mão de obra → recalcula status do pagamento

3. POST /api/mao-obra                 GERENTE, MECANICO
   └─ mesmo caminho do passo 2

4. PATCH /api/ordens-servico/{id}/desconto      GERENTE
   └─ valorComDesconto = valorTotal − desconto

5. PATCH /api/ordens-servico/{id}/status        GERENTE, MECANICO
   └─ ABERTA → DIAGNOSTICO → ... → EM_EXECUCAO → FINALIZADA → ENTREGUE
      (MECANICO não pode FINALIZADA, ENTREGUE nem CANCELADA)

6. POST /api/registros-pagamento      GERENTE
   └─ acumula valorPago → status vira PAGO_PARCIALMENTE ou PAGA

7. PATCH /api/ordens-servico/{id}/status → FECHADA     GERENTE
   └─ só passa se o pagamento estiver PAGA
```

Regras de cada passo em [business-rules.md](./business-rules.md).

---

## 7. Frontend — arquitetura

Organização **por tipo**, não por feature:

```
frontend/src
├── App.tsx        roteamento + todo o estado global (265 linhas)
├── main.tsx       entry
├── index.css      reset + classes globais de layout e modal
├── assets/
├── mocks/         12 arquivos MOCK_* — a fonte de dados atual
├── types/         interfaces por domínio
├── services/      formatters.ts · ordemServicoCalculos.ts · pagamentoCalculos.ts
│                  (funções puras, nenhuma faz HTTP)
├── pages/         11 páginas, pasta por página
└── components/    componentes, pasta por componente com CSS co-localizado
```

Pontos estruturais:

- **Sem gerenciador de estado e sem Context.** O estado de autenticação e os dados vivem
  em `useState` dentro de `App.tsx` e descem por props.
- **Guard de rota:** `RequireRole` em `App.tsx`, usado em 3 grupos de rotas
  (operacional = MECANICO+GERENTE, GERENTE, ADMIN). Redireciona para a home do papel em
  vez de mostrar 403.
- **Três componentes genéricos** sustentam quase todas as telas: `EntityForm`,
  `EntityTable`, `EntityViewModal`. API detalhada em [frontend.md](./frontend.md).
- **CSS global puro**, sem CSS Modules nem Tailwind, com arquivo `.style.css`
  co-localizado por componente.
- **`Role` do frontend está alinhado ao backend:** `"ADMIN" | "GERENTE" | "MECANICO"`.
  Essa divergência já existiu (o frontend usava um nome e o backend outro) e está
  resolvida.

Pontos frágeis conhecidos: `strict` desligado no TypeScript, `react-router-dom` em
`devDependencies`, nenhum teste. Lista completa em [frontend.md](./frontend.md).

---

## 8. Stack

### Backend

| Item | Versão / escolha |
|---|---|
| Java | 21 (toolchain) |
| Spring Boot | 4.1.1 |
| Módulos | webmvc, data-jpa, security, oauth2-resource-server, validation, jackson, actuator |
| Banco | PostgreSQL 16 |
| Migrations | Flyway (`baseline-on-migrate`, `ddl-auto=validate`) |
| Docs | springdoc-openapi 3.0.1 |
| Build | Gradle, Spotless com googleJavaFormat |
| Testes | JUnit 5, Mockito, AssertJ, spring-security-test, H2 |

`spring-boot-starter-oauth2-resource-server` entra para o suporte a JWT
(`JwtEncoder`/`JwtDecoder`), não para OAuth2 de verdade — não há provedor externo.

### Frontend

| Item | Versão / escolha |
|---|---|
| React | 19 |
| TypeScript | 6 (⚠️ `strict` desligado) |
| Build | Vite 8 |
| Rotas | react-router-dom 7 |
| Ícones | lucide-react |
| Gráficos | recharts |
| Qualidade | ESLint flat config + Prettier + husky/lint-staged |
| HTTP | **nenhum** |
| Testes | **nenhum** |

---

## 9. Qualidade e CI

| Verificação | Comando | Onde roda |
|---|---|---|
| Formatação Java | `./gradlew spotlessCheck` | no `check`, logo no `build` |
| Testes backend | `./gradlew test` | GitHub Actions (`backend-ci.yml`) |
| Build backend | `./gradlew build` | GitHub Actions |
| Lint frontend | `npm run lint` | GitHub Actions (`frontend-ci.yml`) |
| Build frontend | `npm run build` (`tsc -b && vite build`) | GitHub Actions + hook pre-commit |

Os workflows rodam em `push` e `pull_request` na `develop`, com JDK 21 Temurin.

O hook `pre-commit` na raiz roda `lint-staged` e `npm run build` no frontend. Ele **não**
roda os testes do backend.

### Composição da suíte

| Tipo | Quantidade | O que valida |
|---|---|---|
| Unitário de service (`MockitoExtension`) | 28 classes | regra de negócio isolada |
| Web (`@WebMvcTest`) | 13 classes | contrato HTTP, validação de DTO, `403` por papel |
| Integração (`@SpringBootTest`) | 2 classes | contexto sobe; `401` com a cadeia real |

348 métodos de teste no total (337 `@Test` + 11 `@ParameterizedTest`, que geram várias
execuções cada).

Nomenclatura importante: os testes de controller **não são E2E**. São `@WebMvcTest` com
o service mockado — validam a borda HTTP, não o fluxo ponta a ponta. A documentação
anterior chamava-os de E2E, o que dava falsa sensação de cobertura.

---

## 10. Decisões e seus motivos

| Decisão | Motivo |
|---|---|
| `ddl-auto=validate` + Flyway | schema versionado e revisável; Hibernate não altera banco |
| DTO `record` separado por operação | entidade não vaza para a borda HTTP; entrada ≠ saída |
| `OficinaAccessValidator` único | a versão duplicada em 4 services gerou endpoint sem validação |
| 404 em vez de 403 para registro alheio | 403 confirmaria a existência do registro |
| `BigDecimal` para dinheiro | `double` acumula erro de arredondamento |
| Um recalculador de valor da OS | dois services calculando o total se sobrescreviam |
| `STATELESS` + CSRF off | sem cookie de sessão não há vetor de CSRF |
| `SecurityErrorResponder` à parte | erro de filtro não passa pelo `@RestControllerAdvice` |
| `@Lazy` no ciclo OS ↔ Pagamento | destrava a subida da aplicação; paliativo, ver §5 |
| Spotless no `check` | formatação deixa de ser assunto de code review |

---

## 11. Dívida arquitetural conhecida

Em ordem de impacto:

1. **Frontend não integrado.** Nenhuma tela usa o backend. É o que separa o projeto de
   ser utilizável.
2. **Ciclo OS ↔ Pagamento** mascarado por `@Lazy` (§5).
3. **Bypass do ADMIN no `OficinaAccessValidator`**, deixando o controller como única
   barreira de isolamento — ver [permissions.md §2](./permissions.md).
4. **`GET /api/clientes` e `GET /api/mecanicos`** permitem ao ADMIN listar dados de todas
   as oficinas, contrariando o princípio de separação.
5. **`strict` desligado no TypeScript** — o frontend não tem checagem de nulos.
6. **Zero teste no frontend.**
7. **Três tabelas órfãs** (`fornecedor`, `nota_compra`, `item_nota_compra`) criadas por
   migration sem entidade correspondente — ver [database.md](./database.md).
8. **Sete testes de service em `LENIENT`** com `TODO` para voltar a `STRICT_STUBS`.
