# Desenvolvimento

Como subir o projeto, rodar os testes e contribuir.

Última verificação contra o código: branch `docs`.

---

## 1. Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| JDK | 21 | o Gradle usa toolchain; outra versão instalada não impede |
| Docker + Docker Compose | recente | para o PostgreSQL |
| Node.js | 20+ | para o frontend |
| Git | — | |

Não é necessário instalar Gradle: use o wrapper (`./gradlew`).

---

## 2. Variáveis de ambiente — leia antes de tentar subir

**`docker compose up` falha sem um arquivo `.env` na raiz.** O `docker-compose.yml`
declara `env_file: ./.env` para os serviços `postgres` e `app`, e esse arquivo **não está
no repositório** (corretamente — contém segredo).

O que existe é o template `.env-example` (com hífen, não `.env.example`). Primeiro passo
de qualquer ambiente novo:

```bash
cp .env-example .env
```

### Variáveis

| Variável | Obrigatória | Default | Para quê |
|---|---|---|---|
| `POSTGRES_DB` | ✅ | — | nome do banco |
| `POSTGRES_HOST` | ✅ | `localhost` | no compose, o serviço sobrescreve para `postgres` |
| `POSTGRES_PORT` | ✅ | `5432` | |
| `POSTGRES_USER` | ✅ | — | |
| `POSTGRES_PASSWORD` | ✅ | — | |
| `SERVER_PORT` | ➖ | `8080` | |
| `JWT_SECRET` | ✅ | — | assinatura HS256. **Mínimo 32 caracteres** |
| `ADMIN_USERNAME` | ➖ | vazio | cria o ADMIN do SaaS na primeira subida |
| `ADMIN_PASSWORD` | ➖ | vazio | idem |
| `ADMIN_NOME` | ➖ | `Administrador do SaaS` | idem |

### Duas armadilhas

**`JWT_SECRET` com menos de 32 caracteres derruba a aplicação na subida.** É exigência do
HS256. O `JwtConfig` valida no startup e falha com mensagem explicando o que fazer, em
vez de estourar erro cru. Gere um segredo de verdade:

```bash
openssl rand -base64 48
```

**Sem `ADMIN_USERNAME`/`ADMIN_PASSWORD` você sobe a aplicação e não consegue logar.** O
`AdminSeeder` só cria o ADMIN inicial se as duas estiverem preenchidas. Sem ADMIN, não há
como criar oficina; sem oficina, não há como criar gerente. A aplicação sobe e fica
inutilizável.

O seeder também não roda se já existir qualquer usuário com papel `ADMIN` — não sobrescreve
nada. Depois da primeira subida, troque a senha e remova as variáveis.

---

## 3. Subindo

### Opção A — banco no Docker, aplicação local

Melhor para desenvolver o backend: reinício rápido, debug direto na IDE.

```bash
cp .env-example .env          # e edite JWT_SECRET e ADMIN_*
docker compose up postgres -d
./gradlew bootRun
```

Aplicação em http://localhost:8080, Swagger em http://localhost:8080/swagger-ui.html.

### Opção B — tudo no Docker

```bash
cp .env-example .env
docker compose up --build
```

| Serviço | Porta |
|---|---|
| frontend (nginx) | 3000 |
| backend | 8080 |
| postgres | valor de `POSTGRES_PORT` |

O `Dockerfile` do backend é multi-stage: builda com `temurin:21-jdk` e roda o jar em
`temurin:21-jre`.

### Frontend isolado

```bash
cd frontend
npm install
npm run dev
```

Vite em http://localhost:5173. Como o frontend ainda usa dados mockados, ele **não precisa
do backend rodando** — ver [frontend.md](./frontend.md).

---

## 4. Perfis

| Perfil | Quando | Características |
|---|---|---|
| `dev` | **default** | `show-sql: true`, log `DEBUG` em `com.oficinapro`, bind de parâmetros em `TRACE` |
| `prod` | explícito | `show-sql: false`, log `WARN`/`INFO` |
| `test` | automático nos testes | H2 em memória, Flyway desligado, `create-drop` |

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

---

## 5. Testes

```bash
./gradlew test                  # suíte completa
./gradlew build                 # compila + testes + spotlessCheck
./gradlew test --tests '*PagamentoServiceImplTest'
./gradlew test --tests '*StatusMachine*'
```

Relatório HTML em `build/reports/tests/test/index.html`.

### Composição da suíte

| Tipo | Classes | O que valida | Anotação |
|---|---|---|---|
| Unitário de service | 28 | regra de negócio isolada, dependências mockadas | `@ExtendWith(MockitoExtension.class)` |
| Web | 13 | contrato HTTP, validação de DTO, `403` por papel | `@WebMvcTest` |
| Integração | 2 | contexto sobe; `401` com a cadeia real | `@SpringBootTest` |

348 métodos no total (337 `@Test` + 11 `@ParameterizedTest`, que geram várias execuções
cada).

### Os testes de controller não são E2E

São `@WebMvcTest` com o service **mockado**: validam a borda HTTP — status, JSON,
`@PreAuthorize`, validação de DTO — e não o fluxo ponta a ponta. Chamá-los de E2E dá falsa
sensação de cobertura, e a documentação anterior fazia isso.

Consequência prática: `@WebMvcTest` **não carrega o `SecurityConfig`**. Ali só se verifica
`403`. A distinção entre `401` e `403` exige a aplicação completa, e é por isso que
`SecurityFilterChainIntegrationTest` existe como `@SpringBootTest`.

### O que a suíte NÃO cobre

Saber disso vale mais que o número de testes:

1. **Migrations.** O perfil de teste desliga o Flyway e gera o schema pelas entidades.
   Constraint declarada apenas em SQL não é exercida — foi assim que `chk_usuario_role`
   proibindo `'GERENTE'` passou sem ninguém notar. Ver
   [database.md §1](./database.md).
2. **Comportamento real do PostgreSQL.** O H2 em modo de compatibilidade não reproduz
   tudo.
3. **Frontend.** Zero testes.
4. **Integração frontend ↔ backend.** Não existe.

### Dívida conhecida na suíte

Sete classes de teste de service estão com `@MockitoSettings(strictness = LENIENT)` e um
`TODO` para voltar a `STRICT_STUBS`. O motivo: quando o isolamento por oficina migrou para
o `OficinaAccessValidator`, vários stubs de `AuthenticatedUserProvider` deixaram de ser
exercidos, e com strict stubs a classe inteira cairia por `UnnecessaryStubbingException`
em vez de apontar problema real.

`LENIENT` deixa passar stub morto. Limpar isso é trabalho pendente — faça depois de ter a
suíte verde, não antes.

---

## 6. Formatação e lint

Spotless com `googleJavaFormat` é obrigatório: `spotlessCheck` está pendurado no `check`,
logo no `build`.

```bash
./gradlew spotlessApply     # formata
./gradlew spotlessCheck     # só verifica
```

> **Rode `spotlessApply` antes de commitar.** Esquecer disso é a causa mais comum de CI
> vermelho neste projeto. O formato é `googleJavaFormat` — indentação de 2 espaços, não 4.
> Não tente formatar à mão.

Frontend:

```bash
cd frontend
npm run lint            # ESLint
npm run format          # Prettier
npm run format:check
```

Há um hook `pre-commit` (husky) na raiz que roda `lint-staged` e `npm run build` **no
frontend**. Ele **não** roda os testes do backend — o CI é que pega isso.

---

## 7. CI

Dois workflows, ambos em `push` e `pull_request` na `develop`:

| Workflow | Faz |
|---|---|
| `backend-ci.yml` | JDK 21 Temurin, cache Gradle, `./gradlew build` (compila + testa + spotlessCheck) |
| `frontend-ci.yml` | lint e build do frontend |

Como o `build` inclui `spotlessCheck`, código mal formatado reprova o CI mesmo com todos
os testes passando.

---

## 8. Comandos úteis

```bash
# Backend
./gradlew bootRun
./gradlew build -x test          # build sem testes
./gradlew clean build
./gradlew bootJar

# Banco
docker compose up postgres -d
docker compose logs -f postgres
docker compose exec postgres psql -U $POSTGRES_USER -d $POSTGRES_DB

# ⚠️ DESTRUTIVO: apaga o volume e todos os dados locais
docker compose down -v

# Frontend
cd frontend && npm run dev
cd frontend && npm run build     # tsc -b && vite build
```

---

## 9. Contribuindo

### Branches

| Branch | Papel |
|---|---|
| `main` | estável |
| `develop` | integração; alvo dos PRs e dos workflows de CI |
| `ci`, `docs`, `fix`, ... | trabalho pontual, mescladas em `develop` |

### Commits — Conventional Commits em português

```
<tipo>: <assunto no imperativo, minúsculo>

<corpo explicando POR QUE, não o que>
```

Tipos em uso: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.

```
fix: corrige CHECK de role que impedia cadastrar GERENTE

A V2 criou chk_usuario_role permitindo ('ADMIN','ADMINISTRATIVO','MECANICO')
e a V14 renomeou o cargo via UPDATE sem ajustar a constraint. Em base sem
usuarios ADMINISTRATIVO a V14 passa com zero linhas, mas a constraint
continua proibindo 'GERENTE' e qualquer cadastro de gerente morre como 409.
```

O diff mostra o que mudou; o corpo precisa dizer por que. Dica prática: aspas duplas
dentro de `-m` quebram o shell no PowerShell — escreva `roles=ADMIN` em vez de
`roles="ADMIN"`, ou use here-string.

### Checklist de PR

1. `./gradlew spotlessApply`
2. `./gradlew build` verde localmente
3. Teste cobrindo a mudança — ou justificativa de por que não dá
4. Se mudou permissão → atualizar [permissions.md](./permissions.md)
5. Se mudou regra financeira ou de status → atualizar [business-rules.md](./business-rules.md)
6. Se criou migration → atualizar [database.md](./database.md) e testar contra
   PostgreSQL real
7. Se mudou arquitetura ou dependências → atualizar [architecture.md](./architecture.md)

Os itens 4 a 7 existem porque a divergência entre documentação e código foi o principal
achado da auditoria deste projeto. Documentação errada é pior que documentação ausente:
ela é seguida.

---

## 10. Onde procurar quando quebrar

| Sintoma | Provável causa |
|---|---|
| App não sobe: erro de JWT | `JWT_SECRET` ausente ou com menos de 32 caracteres |
| App não sobe: `docker compose` reclama de env | falta `cp .env-example .env` |
| App não sobe: `BeanCurrentlyInCreationException` | dependência circular reintroduzida — ver [architecture.md §5](./architecture.md) |
| App não sobe: erro de `validate` do Hibernate | entidade e schema divergem; falta migration |
| Migration falha em banco limpo | ver o risco V10/V16 em [database.md §6](./database.md) |
| Subiu, mas não consigo logar | `ADMIN_USERNAME`/`ADMIN_PASSWORD` não definidos na primeira subida |
| Cadastro de gerente dá 409 sem motivo | `chk_usuario_role` desatualizada; conferir se a V19 foi aplicada |
| CI vermelho com testes passando | `spotlessCheck` — rode `./gradlew spotlessApply` |
| 401 onde era esperado 403 | não é bug: sem token válido o filtro responde 401 antes do `@PreAuthorize` |
| 404 em registro que existe | isolamento por oficina — é o comportamento correto. Ver [permissions.md §6](./permissions.md) |
| 500 em qualquer rota | sempre bug. Não há handler genérico; provavelmente falta um `@ExceptionHandler` |
