# Permissões e isolamento entre oficinas

Este documento é a **fonte única de verdade** sobre quem pode fazer o quê no OficinaPro.

Antes dele, a matriz existia apenas implícita em ~80 anotações `@PreAuthorize` espalhadas
pelos controllers e em chamadas de validação dentro dos services. Frontend e backend já
chegaram a discordar sobre o nome de um papel. Se você alterar uma permissão no código,
**atualize este arquivo no mesmo commit**.

A matriz da §3 é **gerada a partir dos `@PreAuthorize`** dos controllers; regenere-a ao mudar uma permissão.

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

Um ponto em aberto. Está documentado aqui em vez de escondido. (P1 e P2, que diziam que o ADMIN
listava clientes e mecânicos de todas as oficinas, **já não existem**: `GET /api/clientes` é de
GERENTE/MECANICO e `GET /api/mecanicos` é só de GERENTE, e o ADMIN recebe `403`.)

| # | Ponto | Situação |
|---|---|---|
| P3 | `OficinaAccessValidator` | `validarAcessoOficina` e `validarAcessoAoRegistro` começam com `if (role == ADMIN) return;`. O ADMIN atravessa o isolamento na camada de serviço. Hoje o controller é a **única** barreira. |

Sobre P3: isso inverte a defesa em profundidade. Se algum endpoint operacional passar a
aceitar `ADMIN` por descuido, o isolamento não segura — o bypass está embaixo. O teste
`OficinaAccessValidatorTest.adminAtravessaValidacaoDeOficina` documenta esse
comportamento explicitamente e traz o comentário de que é o primeiro teste a mudar
quando a regra mudar.

Há também **código morto** decorrente da mudança: `OrdemDeServicoServiceImpl.listar()` e
`UnidadeServiceImpl.listar()` têm um branch `if (role == ADMIN) findAll()` que nenhum
ADMIN alcança, porque os controllers correspondentes não permitem `ADMIN`.

### ADMIN em rota operacional: `403` por design

O ADMIN não pertence a nenhuma oficina. Toda rota operacional que depende "da oficina do
usuário logado" (`getOficinaIdUsuarioLogado()`) responde **`403`** ao ADMIN, mesmo nas
rotas cujo `@PreAuthorize` aceitaria o papel — por exemplo `GET /api/itens-os-peca`,
`GET /api/ordens-servico` ou o dashboard. Isso é intencional e não deve ser corrigido para
"listar tudo": o ADMIN vê apenas contagens agregadas em `/api/admin/estatisticas`.

O comportamento é `403` (e não lista vazia) porque devolver `[]` esconderia o erro de uso
atrás de uma resposta de sucesso — bug que já ocorreu neste projeto (ver §6).

---

## 3. Matriz completa papel × endpoint

Gerada a partir dos `@PreAuthorize` dos controllers (a fonte de verdade é o código). "público"
= `permitAll` no `SecurityConfig`; "token" = só exige estar autenticado. O service ainda aplica
o isolamento por oficina (§6) e, em vários casos, `validarRole` como segunda barreira.

### Autenticação — `/api/auth`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| POST | `/api/auth/login` | público | público | público |
| POST | `/api/auth/logout` | token | token | token |
| GET | `/api/auth/me` | token | token | token |

### Oficinas (plataforma) — `/api/oficinas`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/oficinas` | ✅ | ❌ | ❌ |
| GET | `/api/oficinas/buscar` | ✅ | ❌ | ❌ |
| GET | `/api/oficinas/{id}` | ✅ | ❌ | ❌ |
| POST | `/api/oficinas` | ✅ | ❌ | ❌ |
| PUT | `/api/oficinas/{id}` | ✅ | ❌ | ❌ |
| PATCH | `/api/oficinas/{id}/ativar` | ✅ | ❌ | ❌ |
| PATCH | `/api/oficinas/{id}/desativar` | ✅ | ❌ | ❌ |

### Logo da oficina — `/api/oficinas/{oficinaId}/logo`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| PUT | `/api/oficinas/{oficinaId}/logo` | ✅ | ✅ | ❌ |
| DELETE | `/api/oficinas/{oficinaId}/logo` | ✅ | ✅ | ❌ |
| GET | `/api/oficinas/{oficinaId}/logo` | ✅ | ✅ | ✅ |

### Usuários — `/api/usuarios`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/usuarios` | ✅ | ✅ | ❌ |
| GET | `/api/usuarios/buscar` | ✅ | ✅ | ❌ |
| PUT | `/api/usuarios/me` | ✅ | ✅ | ✅ |
| GET | `/api/usuarios/{id}` | ✅ | ✅ | ❌ |
| GET | `/api/usuarios/nome/{nome}` | ❌ | ✅ | ❌ |
| POST | `/api/usuarios/documento/buscar` | ❌ | ✅ | ❌ |
| GET | `/api/usuarios/admin/nome/{nome}` | ✅ | ❌ | ❌ |
| POST | `/api/usuarios/admin/documento/buscar` | ✅ | ❌ | ❌ |
| POST | `/api/usuarios` | ✅ | ✅ | ❌ |
| PUT | `/api/usuarios/{id}` | ✅ | ✅ | ❌ |
| PATCH | `/api/usuarios/{id}/desbloquear` | ✅ | ✅ | ❌ |
| DELETE | `/api/usuarios/{id}` | ✅ | ✅ | ❌ |

### Unidades — `/api/unidades`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/unidades` | ❌ | ✅ | ✅ |
| GET | `/api/unidades/{id}` | ❌ | ✅ | ✅ |
| POST | `/api/unidades/oficina/{oficinaId}` | ❌ | ✅ | ❌ |
| PUT | `/api/unidades/{id}` | ❌ | ✅ | ❌ |
| DELETE | `/api/unidades/{id}` | ❌ | ✅ | ❌ |

### Clientes — `/api/clientes`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/clientes` | ❌ | ✅ | ✅ |
| GET | `/api/clientes/buscar` | ❌ | ✅ | ✅ |
| GET | `/api/clientes/{id}` | ❌ | ✅ | ✅ |
| GET | `/api/clientes/nome/{nome}` | ❌ | ✅ | ✅ |
| POST | `/api/clientes/documento/buscar` | ❌ | ✅ | ✅ |
| POST | `/api/clientes` | ❌ | ✅ | ❌ |
| PUT | `/api/clientes/{id}` | ❌ | ✅ | ❌ |
| DELETE | `/api/clientes/{id}` | ❌ | ✅ | ❌ |

### Mecânicos — `/api/mecanicos`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/mecanicos` | ❌ | ✅ | ❌ |
| GET | `/api/mecanicos/buscar` | ❌ | ✅ | ❌ |
| GET | `/api/mecanicos/{id}` | ❌ | ✅ | ❌ |
| GET | `/api/mecanicos/nome/{nome}` | ❌ | ✅ | ❌ |
| POST | `/api/mecanicos/documento/buscar` | ❌ | ✅ | ❌ |
| POST | `/api/mecanicos` | ❌ | ✅ | ❌ |
| PUT | `/api/mecanicos/{id}` | ❌ | ✅ | ❌ |
| DELETE | `/api/mecanicos/{id}` | ❌ | ✅ | ❌ |

### Veículos — `/api/veiculos`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/veiculos` | ❌ | ✅ | ✅ |
| GET | `/api/veiculos/buscar` | ❌ | ✅ | ✅ |
| GET | `/api/veiculos/{id}` | ❌ | ✅ | ✅ |
| GET | `/api/veiculos/placa/{placa}` | ❌ | ✅ | ✅ |
| POST | `/api/veiculos` | ❌ | ✅ | ❌ |
| PUT | `/api/veiculos/{id}` | ❌ | ✅ | ❌ |
| DELETE | `/api/veiculos/{id}` | ❌ | ✅ | ❌ |

### Ordens de serviço — `/api/ordens-servico`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| POST | `/api/ordens-servico` | ❌ | ✅ | ❌ |
| GET | `/api/ordens-servico` | ❌ | ✅ | ✅ |
| GET | `/api/ordens-servico/{id}` | ❌ | ✅ | ✅ |
| GET | `/api/ordens-servico/fluxo-mensal` | ❌ | ✅ | ✅ |
| PUT | `/api/ordens-servico/{id}` | ❌ | ✅ | ✅ |
| DELETE | `/api/ordens-servico/{id}` | ❌ | ✅ | ❌ |
| PATCH | `/api/ordens-servico/{id}/status` | ❌ | ✅ | ✅ |
| PATCH | `/api/ordens-servico/{id}/mecanico` | ❌ | ✅ | ✅ |
| PATCH | `/api/ordens-servico/{id}/cliente` | ❌ | ✅ | ✅ |
| PATCH | `/api/ordens-servico/{id}/desconto` | ❌ | ✅ | ❌ |
| GET | `/api/ordens-servico/veiculo/{veiculoId}` | ❌ | ✅ | ✅ |
| GET | `/api/ordens-servico/mecanico/{mecanicoId}` | ❌ | ✅ | ✅ |
| GET | `/api/ordens-servico/unidade/{unidadeId}` | ❌ | ✅ | ✅ |
| GET | `/api/ordens-servico/cliente/{clienteId}` | ❌ | ✅ | ✅ |
| GET | `/api/ordens-servico/status/{status}` | ❌ | ✅ | ✅ |
| GET | `/api/ordens-servico/{id}/pdf` | ❌ | ✅ | ✅ |
| GET | `/api/ordens-servico/{id}/comprovante-pagamento` | ❌ | ✅ | ✅ |

### Peças da OS — `/api/itens-os-peca`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/itens-os-peca/os/{osId}` | ❌ | ✅ | ✅ |
| GET | `/api/itens-os-peca` | ❌ | ✅ | ✅ |
| GET | `/api/itens-os-peca/{id}` | ❌ | ✅ | ✅ |
| POST | `/api/itens-os-peca` | ❌ | ✅ | ✅ |
| PUT | `/api/itens-os-peca/{id}` | ❌ | ✅ | ✅ |
| DELETE | `/api/itens-os-peca/{id}` | ❌ | ✅ | ✅ |
| PUT | `/api/itens-os-peca/{id}/os/{osId}` | ❌ | ✅ | ✅ |
| DELETE | `/api/itens-os-peca/{id}/os` | ❌ | ✅ | ✅ |

### Mão de obra — `/api/mao-obra`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/mao-obra/os/{osId}` | ❌ | ✅ | ✅ |
| GET | `/api/mao-obra/{id}` | ❌ | ✅ | ✅ |
| POST | `/api/mao-obra` | ❌ | ✅ | ✅ |
| PUT | `/api/mao-obra/{id}` | ❌ | ✅ | ✅ |
| DELETE | `/api/mao-obra/{id}` | ❌ | ✅ | ✅ |

### Pagamentos — `/api/pagamentos`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/pagamentos/{id}` | ❌ | ✅ | ❌ |
| GET | `/api/pagamentos/os/{osId}` | ❌ | ✅ | ❌ |
| GET | `/api/pagamentos/oficina/{oficinaId}` | ❌ | ✅ | ❌ |
| GET | `/api/pagamentos/oficina/{oficinaId}/status/{status}` | ❌ | ✅ | ❌ |
| GET | `/api/pagamentos/oficina/{oficinaId}/a-receber` | ❌ | ✅ | ❌ |
| GET | `/api/pagamentos/oficina/{oficinaId}/resumo` | ❌ | ✅ | ❌ |
| GET | `/api/pagamentos/oficina/{oficinaId}/por-os` | ❌ | ✅ | ❌ |
| PUT | `/api/pagamentos/{id}` | ❌ | ✅ | ❌ |

### Registros de pagamento — `/api/registros-pagamento`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| POST | `/api/registros-pagamento` | ❌ | ✅ | ❌ |
| GET | `/api/registros-pagamento/{id}` | ❌ | ✅ | ❌ |
| GET | `/api/registros-pagamento/pagamento/{pagamentoId}` | ❌ | ✅ | ❌ |
| DELETE | `/api/registros-pagamento/{id}` | ❌ | ✅ | ❌ |

### Dashboard — `/api/dashboard`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/dashboard/data` | ❌ | ✅ | ❌ |

### Estatísticas do sistema — `/api/admin/estatisticas`

| Método | Rota | ADMIN | GERENTE | MECANICO |
|---|---|:-:|:-:|:-:|
| GET | `/api/admin/estatisticas` | ✅ | ❌ | ❌ |
| GET | `/api/admin/estatisticas/oficina/{oficinaId}` | ✅ | ❌ | ❌ |

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
5. Se envolver papel, abrir PR correspondente no repositório `oficinapro-frontend`: conferir
   `src/types/usuario/role.ts` e o `RequireRole` em `App.tsx`.
6. Se persistir novo valor de papel: criar migration ajustando o `CHECK` de
   `chk_usuario_role`. Ver §1.
