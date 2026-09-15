# Permissões e isolamento entre oficinas

Este documento é a **fonte única de verdade** sobre quem pode fazer o quê no OficinaPro.

Antes dele, a matriz existia apenas implícita em ~80 anotações `@PreAuthorize` espalhadas
pelos controllers e em chamadas de validação dentro dos services. Frontend e backend já
chegaram a discordar sobre o nome de um papel. Se você alterar uma permissão no código,
**atualize este arquivo no mesmo commit**.

Última verificação contra o código: branch `docs`, após a suíte de 348 testes passar.

---

## 1. Os três papéis

O enum é `com.oficinapro.enums.Role`. No frontend, o tipo espelho é
`src/types/usuario/role.ts`.

| Papel | Vínculo com oficina | Para que existe |
|---|---|---|
| `ADMIN` | **Nenhum** (`oficina_id` nulo) | Administrador do SaaS. Cria oficinas e contas de usuário. |
| `GERENTE` | Obrigatório | Administra uma oficina: cadastros, OS, financeiro. |
| `MECANICO` | Obrigatório | Executa o serviço: lança peças e mão de obra, move a OS. |

---

## 2. O princípio: o ADMIN não acessa dado operacional

> O ADMIN do SaaS administra a plataforma, não as oficinas.

Ele gerencia oficinas e contas de usuário. Ele **não** deve ler clientes, veículos,
ordens de serviço, peças, mão de obra ou qualquer informação financeira das oficinas.
A razão é ética e contratual, não técnica: o operador do SaaS não tem por que ver a
carteira de clientes nem o faturamento de quem paga pelo serviço.


### Onde o princípio já vale

Os endpoints operacionais não listam `ADMIN` no `@PreAuthorize`. Um ADMIN autenticado
que chame `POST /api/ordens-servico` recebe `403`. Há teste para isso em
`MaoObraControllerTest` e `PagamentoControllerTest`.

### Onde o princípio ainda NÃO vale (dívida conhecida)

Três pontos em aberto. Estão documentados aqui em vez de escondidos:

| # | Ponto | Situação |
|---|---|---|
| P1 | `GET /api/clientes` | Restrito a `ADMIN` — e o service, para ADMIN, faz `findAll()`. Ou seja: **o ADMIN lista os clientes de todas as oficinas.** Contradiz o princípio. |
| P2 | `GET /api/mecanicos` | Idem: `ADMIN` recebe `findAll()` de todos os mecânicos de todas as oficinas. |
| P3 | `OficinaAccessValidator` | `validarAcessoOficina` e `validarAcessoAoRegistro` começam com `if (role == ADMIN) return;`. O ADMIN atravessa o isolamento na camada de serviço. Hoje o controller é a **única** barreira. |

Sobre P3: isso inverte a defesa em profundidade. Se algum endpoint operacional passar a
aceitar `ADMIN` por descuido, o isolamento não segura — o bypass está embaixo. O teste
`OficinaAccessValidatorTest.adminAtravessaValidacaoDeOficina` documenta esse
comportamento explicitamente e traz o comentário de que é o primeiro teste a mudar
quando a regra mudar.

Há também **código morto** decorrente da mudança: `OrdemDeServicoServiceImpl.listar()` e
`UnidadeServiceImpl.listar()` têm um branch `if (role == ADMIN) findAll()` que nenhum
ADMIN alcança, porque os controllers correspondentes não permitem `ADMIN`.

---

## 3. Matriz completa papel × endpoint

Extraída direto dos `@PreAuthorize`. "—" significa que o endpoint não exige papel
específico, apenas autenticação.

### Autenticação — `/api/auth`

| Método | Rota | ADMIN | GERENTE | MECANICO | Observação |
|---|---|:-:|:-:|:-:|---|
| POST | `/login` | público | público | público | Não exige token |
| GET | `/me` | ✅ | ✅ | ✅ | Só exige estar autenticado |

### Oficinas — `/api/oficinas` (plataforma)

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/` | ✅ | ❌ | ❌ |
| GET | `/{id}` | ✅ | ❌ | ❌ |
| POST | `/` | ✅ | ❌ | ❌ |
| PUT | `/{id}` | ✅ | ❌ | ❌ |
| DELETE | `/{id}` | ✅ | ❌ | ❌ |

Exclusivo do ADMIN, e o service reforça com `validarRole(ADMIN)` nos cinco métodos.

### Usuários — `/api/usuarios` (plataforma + oficina)

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/` | ✅ | ❌ | ❌ |
| GET | `/oficina/{oficinaId}` | ✅ | ✅ | ❌ |
| GET | `/{id}` | ✅ | ✅ | ❌ |
| GET | `/nome/{nome}` | ✅ | ❌ | ❌ |
| GET | `/documento/{documento}` | ✅ | ❌ | ❌ |
| POST | `/` | ✅ | ✅ | ❌ |
| PUT | `/{id}` | ✅ | ✅ | ❌ |
| DELETE | `/{id}` | ✅ | ✅ | ❌ |

Único módulo compartilhado entre ADMIN e GERENTE. As regras de hierarquia estão na §4.

### Unidades — `/api/unidades`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/` | ❌ | ✅ | ❌ |
| GET | `/{id}` | ❌ | ✅ | ❌ |
| GET | `/oficina/{oficinaId}` | ❌ | ✅ | ❌ |
| POST | `/oficina/{oficinaId}` | ❌ | ✅ | ❌ |
| PUT | `/{id}` | ❌ | ✅ | ❌ |
| DELETE | `/{id}` | ❌ | ✅ | ❌ |

### Clientes — `/api/clientes`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/` | ✅ ⚠️ | ❌ | ❌ |
| GET | `/oficina/{oficinaId}` | ❌ | ✅ | ❌ |
| GET | `/{id}` | ❌ | ✅ | ❌ |
| GET | `/nome/{nome}` | ❌ | ✅ | ❌ |
| GET | `/documento/{documento}` | ❌ | ✅ | ❌ |
| POST | `/` | ❌ | ✅ | ❌ |
| PUT | `/{id}` | ❌ | ✅ | ❌ |
| DELETE | `/{id}` | ❌ | ✅ | ❌ |

⚠️ P1 da §2. O MECANICO não acessa cadastro de clientes.

### Mecânicos — `/api/mecanicos`

Estrutura idêntica à de Clientes, incluindo o ⚠️ em `GET /` (P2 da §2).

### Veículos — `/api/veiculos`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/` | ❌ | ✅ | ✅ |
| GET | `/{id}` | ❌ | ✅ | ✅ |
| GET | `/placa/{placa}` | ❌ | ✅ | ✅ |
| GET | `/oficina/{oficinaId}` | ❌ | ✅ | ✅ |
| POST | `/` | ❌ | ✅ | ✅ |
| PUT | `/{id}` | ❌ | ✅ | ❌ |
| DELETE | `/{id}` | ❌ | ✅ | ❌ |

O MECANICO pode cadastrar um veículo (carro que chegou agora), mas não alterar nem
excluir.

`GET /placa/{placa}` escopa a busca na oficina do usuário logado lendo
`AuthenticatedUserProvider.getOficinaIdUsuarioLogado()`. Como o ADMIN não tem oficina,
esse endpoint nunca funcionaria para ele — coerente com o fato de não estar liberado.

### Ordens de serviço — `/api/ordens-servico`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| POST | `/` | ❌ | ✅ | ❌ |
| GET | `/` | ❌ | ✅ | ✅ |
| GET | `/{id}` | ❌ | ✅ | ✅ |
| GET | `/veiculo/{veiculoId}` | ❌ | ✅ | ✅ |
| GET | `/mecanico/{mecanicoId}` | ❌ | ✅ | ✅ |
| GET | `/unidade/{unidadeId}` | ❌ | ✅ | ✅ |
| GET | `/cliente/{clienteId}` | ❌ | ✅ | ✅ |
| GET | `/oficina/{oficinaId}` | ❌ | ✅ | ✅ |
| GET | `/status/{status}` | ❌ | ✅ | ✅ |
| GET | `/oficina/{oficinaId}/fluxo-mensal` | ❌ | ✅ | ✅ |
| PUT | `/{id}` | ❌ | ✅ | ❌ |
| DELETE | `/{id}` | ❌ | ✅ | ❌ |
| PATCH | `/{id}/status` | ❌ | ✅ | ✅ |
| PATCH | `/{id}/mecanico` | ❌ | ✅ | ❌ |
| PATCH | `/{id}/cliente` | ❌ | ✅ | ❌ |
| PATCH | `/{id}/desconto` | ❌ | ✅ | ❌ |

`PATCH /{id}/status` é liberado ao MECANICO, mas com restrição **dentro** do service —
ver §5.

Consequência de acoplamento em `POST /`: criar uma OS abre o pagamento dela, e
`PagamentoServiceImpl.criar` exige `validarRole(GERENTE)`. Logo a criação de OS é de
GERENTE tanto pelo controller quanto pelo service.

### Peças da OS — `/api/itens-os-peca`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/os/{osId}` | ❌ | ✅ | ✅ |
| GET | `/{id}` | ❌ | ✅ | ✅ |
| POST | `/` | ❌ | ✅ | ✅ |
| PUT | `/{id}` | ❌ | ✅ | ✅ |
| DELETE | `/{id}` | ❌ | ✅ | ✅ |

### Mão de obra — `/api/mao-obra`

Permissões idênticas às de peças: `GERENTE` e `MECANICO` em todos os cinco endpoints.

### Pagamentos — `/api/pagamentos`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| POST | `/` | ❌ | ✅ | ❌ |
| GET | `/{id}` | ❌ | ✅ | ❌ |
| GET | `/os/{osId}` | ❌ | ✅ | ❌ |
| GET | `/oficina/{oficinaId}` | ❌ | ✅ | ❌ |
| GET | `/oficina/{oficinaId}/a-receber` | ❌ | ✅ | ❌ |
| GET | `/oficina/{oficinaId}/status/{status}` | ❌ | ✅ | ✅ |
| PUT | `/{id}` | ❌ | ✅ | ❌ |

O financeiro é do GERENTE. A **única** exceção é a consulta por status, liberada ao
MECANICO — ela informa se a OS está paga, sem expor valores agregados da oficina.

### Registros de pagamento — `/api/registros-pagamento`

Todos os quatro endpoints: apenas `GERENTE`.

---

## 4. Hierarquia na gestão de usuários

Implementada em `UsuarioServiceImpl.validarPermissaoParaAtribuirRole` e
`validarCoerenciaRoleOficina`.

| Quem está logado | Pode criar/promover | Não pode |
|---|---|---|
| `ADMIN` | `ADMIN`, `GERENTE`, `MECANICO` | — |
| `GERENTE` | `GERENTE`, `MECANICO` | criar ou promover `ADMIN` |
| `MECANICO` | nada | gerenciar usuários |

Regras adicionais:

- **Coerência papel × oficina.** `ADMIN` precisa vir com `oficinaId` nulo; `GERENTE` e
  `MECANICO` exigem `oficinaId`. Violar isso dá `400`
  (`OficinaIncompativelComRoleException`), não `403` — é erro de payload.
- **Conta ADMIN só é editada por ADMIN.** Um GERENTE que tente editar uma conta com papel
  `ADMIN` recebe `403`, mesmo tendo permissão genérica de editar usuários.
- **Ordem das validações é intencional:** permissão antes de coerência. Quem não pode
  atribuir o papel recebe `403` sem descobrir qual seria o formato correto do payload.
- `MECANICO` é bloqueado no `@PreAuthorize` **e** no service. Redundância deliberada.

---

## 5. Restrições do MECANICO na ordem de serviço

O MECANICO pode mover a OS pelos status operacionais, mas **não** pode encerrá-la nem
cancelá-la. Bloqueado em `OrdemDeServicoServiceImpl.validarTransicaoStatus`:

| Status de destino | MECANICO |
|---|---|
| `DIAGNOSTICO`, `AGUARDANDO_APROVACAO`, `AGUARDANDO_PECAS`, `EM_EXECUCAO`, `ABERTA` | ✅ |
| `FINALIZADA` | ❌ `403` |
| `ENTREGUE` | ❌ `403` |
| `CANCELADA` | ❌ `403` |

Decidir que o serviço terminou, que o carro saiu, ou que a OS foi cancelada tem efeito
contratual e financeiro. Fica com o GERENTE.

O grafo completo de transições está em [business-rules.md](./business-rules.md).

---

## 6. Como o isolamento é implementado

Toda a lógica de multi-tenancy está centralizada em
`com.oficinapro.security.OficinaAccessValidator`. Antes ela estava duplicada em quatro
services, e a duplicação foi a causa-raiz de endpoints financeiros sem validação.

### Os quatro métodos

| Método | Uso | Falha com |
|---|---|---|
| `validarRole(Role...)` | operação restrita a papéis específicos | `403` |
| `getOficinaIdUsuarioLogado()` | obter a oficina do usuário | `403` se não tiver oficina |
| `validarAcessoAoRegistro(oficinaDoRegistro, notFound)` | ler/alterar um registro existente | a exceção **de "não encontrado"** recebida |
| `validarAcessoOficina(oficinaId)` | operar sobre uma oficina informada | `403` |

Detalhe importante: `validarRole` é **literal**. O ADMIN não recebe passe livre ali — se
`ADMIN` não estiver na lista, ele é negado. É o oposto de `validarAcessoOficina`, que tem
o bypass do P3.

### 404 em vez de 403 para registro alheio

Ao tentar ler um registro de outra oficina, a API responde **404, não 403**. Responder
"sem permissão" confirmaria que o registro existe — um oráculo que permitiria enumerar
ids e descobrir o volume de clientes de um concorrente.

A exceção devolvida é a da própria entidade (`ClienteNotFoundException`,
`VeiculoNotFoundException`, ...). Já foi um `UsuarioNotFoundException` fixo, o que fazia
a busca de um cliente alheio responder "Usuário não encontrado" — mensagem errada que,
por ser diferente do 404 normal, ainda denunciava o bloqueio.

### Validação na criação

`criar` valida a oficina de destino **antes** de resolver a oficina. A ordem importa: se
resolvesse primeiro, sondar ids alheios devolveria `404` (oficina não existe) ou `200`,
revelando quais oficinas existem. Validando antes, a resposta é sempre `403`.

Essa validação já não existiu: um GERENTE conseguia criar clientes, mecânicos e usuários
em **qualquer** oficina, bastando informar outro `oficinaId` no corpo. Só o
`oficinaId` nulo fica de fora da checagem, porque é a criação do ADMIN do SaaS.

### Camadas

```
1. SecurityConfig          →  autenticado? (401)
2. @PreAuthorize            →  o papel pode chamar esta rota? (403)
3. OficinaAccessValidator   →  o dado é da oficina dele? (403 ou 404)
4. Regras de negócio        →  a operação faz sentido agora? (400/409/422)
```

Nenhuma camada substitui a outra. A 2 protege a rota; a 3 protege o dado. Um endpoint
liberado ao GERENTE sem a camada 3 é exploitável por qualquer gerente contra qualquer
oficina — foi exatamente o que aconteceu com `/api/pagamentos/oficina/**`.

---

## 7. Onde isso é testado

| Arquivo | Cobre |
|---|---|
| `security/OficinaAccessValidatorTest` | os quatro métodos, incluindo o bypass do ADMIN e o 404-em-vez-de-403 |
| `security/SecurityFilterChainIntegrationTest` | 401 com a cadeia de filtros real: sem token, token malformado, assinatura inválida, header sem `Bearer` |
| `controller/*ControllerTest` | `403` por papel em cada rota (`@WebMvcTest` + `@EnableMethodSecurity`) |
| `service/*ServiceTest` | delegação ao validador e propagação da negação |
| `service/ordem_servico/OrdemDeServicoStatusMachineTest` | restrições do MECANICO nas transições |

Os testes de controller usam `@WebMvcTest`, que **não** carrega o `SecurityConfig`. Ali
só é possível verificar `403`. A distinção entre `401` e `403` exige a aplicação
completa, e é por isso que `SecurityFilterChainIntegrationTest` existe como
`@SpringBootTest`.

---

## 8. Checklist para alterar uma permissão

1. Alterar o `@PreAuthorize` no controller.
2. Verificar se o service tem validação correspondente (`validarRole` /
   `validarAcessoOficina`). A camada 2 sem a camada 3 não isola tenant.
3. Atualizar o teste de controller (o caso `403`) e o de service.
4. Atualizar a matriz da §3 **neste arquivo**.
5. Se envolver papel, conferir `src/types/usuario/role.ts` no frontend e o `RequireRole`
   em `App.tsx`.
6. Se persistir novo valor de papel: criar migration ajustando o `CHECK` de
   `chk_usuario_role`. Ver §1.
