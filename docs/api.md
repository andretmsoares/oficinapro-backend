# API — contrato

Este documento **não lista endpoints**. A lista viva é o OpenAPI, gerado a partir do
código. Listas de rota em markdown envelhecem e passam a mentir — já aconteceu aqui.

O que está aqui é o que o OpenAPI não expressa bem: fluxo de autenticação, contrato de
erro, paginação e convenções de tipo.

Última verificação contra o código: branch `docs`.

---

## 1. Onde está a lista de endpoints

Com a aplicação rodando:

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

Ambos são rotas públicas — não exigem token.

Para chamar endpoint protegido pelo Swagger: botão **Authorize**, e cole **apenas o
token**, sem o prefixo `Bearer`.

Quem pode chamar cada rota está em [permissions.md](./permissions.md), que traz a matriz
completa papel × endpoint.

---

## 2. Autenticação

JWT HS256, stateless. Sem refresh token: expirado, faz login de novo.

### Login

```http
POST /api/auth/login
Content-Type: application/json

{ "username": "admin", "password": "sua-senha" }
```

Resposta `200`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 28800,
  "usuario": {
    "id": 1,
    "nome": "Administrador do SaaS",
    "telefone": null,
    "documento": null,
    "oficinaId": null,
    "username": "admin",
    "role": "ADMIN"
  }
}
```

`expiresIn` é em **segundos** (28800 = 8h).

O bloco `usuario` vem no login de propósito: evita que o frontend precise chamar
`/api/auth/me` imediatamente depois. `oficinaId` é `null` para ADMIN, que não pertence a
oficina.

Validação do corpo: `username` obrigatório (máx. 100), `password` obrigatório (máx. 255).
O tamanho mínimo da senha **não** é validado no login de propósito: responder `400` com a
regra de senha antes de autenticar vazaria a política. A política (`@SenhaForte`: 8 a 72
caracteres, com letra e número, fora de uma lista de senhas comuns) vale ao **definir** a
senha (criar/alterar usuário e `PUT /api/usuarios/me`).

### Usando o token

```http
GET /api/clientes/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

O filtro ignora o header quando ele não começa com `Bearer ` — nesse caso a requisição
segue como não autenticada e termina em `401`.

### Logout (revogação de tokens)

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

Responde `204` e **revoga todos os tokens** do usuário (o atual e os anteriores): o JWT
carrega a claim `tv` (versão), e o filtro recusa o token se `tv` for diferente de
`usuario.token_version`. A versão também é incrementada quando a senha do usuário é trocada
(por ele mesmo ou por ADMIN/GERENTE). Não existe lista de bloqueio: é só uma coluna.

### Usuário autenticado

```http
GET /api/auth/me
Authorization: Bearer <token>
```

Devolve o `UsuarioResponseDTO` do dono do token. Exige apenas estar autenticado, qualquer
papel.

### Conteúdo do token

| Claim | Conteúdo |
|---|---|
| `sub` | **id** do usuário (não o username: ele pode ser trocado e reaproveitado) |
| `tv` | versão de revogação do usuário na emissão |
| `jti` | identificador único do token |
| `role` | `ADMIN`, `GERENTE` ou `MECANICO` |
| `oficinaId` | id da oficina — **omitido** para ADMIN |
| `iss` | `oficinapro` |
| `exp` / `iat` | expiração e emissão |

O `role` no token é informativo. A autorização real usa o usuário recarregado do banco a
cada requisição — alterar o papel de alguém tem efeito imediato, sem esperar o token
expirar.

### Erros de autenticação

| Situação | Status | Mensagem |
|---|---|---|
| Sem header `Authorization` | 401 | `Não autenticado: envie um token válido no header Authorization.` |
| Header sem prefixo `Bearer` | 401 | idem |
| Token malformado | 401 | idem |
| Assinatura inválida | 401 | idem |
| Token expirado | 401 | idem |
| Issuer diferente | 401 | idem |
| Credenciais erradas no login | 401 | `Credenciais inválidas` |
| Autenticado, sem permissão | 403 | `Acesso negado: Você não tem permissão para acessar este recurso.` |

| Muitas tentativas de login (limite por IP) | 429 | `Muitas tentativas de login. Tente novamente em N minuto(s).` (+ header `Retry-After` e `retryAfterSeconds` no corpo) |
| Senha atual incorreta em `PUT /api/usuarios/me` | 400 | `Senha atual incorreta...` (400 e não 401, para o frontend não derrubar a sessão) |

`Credenciais inválidas` é intencionalmente genérica: não diz se o problema foi o usuário
inexistente ou a senha errada. Diferenciar permitiria enumerar usuários válidos.

### Logo da oficina (PDFs)

A logo impressa no cabeçalho da OS e do comprovante fica num bucket do Google Cloud Storage;
`oficina.logo_path` guarda só o caminho do objeto (gerado pelo servidor). Sem logo, ou se o
bucket falhar, o PDF usa a logo padrão do sistema (`resources/images/logo.png`) — a geração
do PDF nunca falha por causa da logo.

| Método | Rota | Papéis | Observação |
|---|---|---|---|
| PUT | `/api/oficinas/{id}/logo` | ADMIN, GERENTE | multipart, campo `arquivo`; PNG/JPEG até 2 MB |
| DELETE | `/api/oficinas/{id}/logo` | ADMIN, GERENTE | volta à logo padrão; idempotente |
| GET | `/api/oficinas/{id}/logo` | ADMIN, GERENTE, MECANICO | imagem; `404` se não houver |

GERENTE/MECANICO só acessam a própria oficina (`403` para outra). O tipo é verificado pelos
bytes do arquivo (não pelo content-type); SVG é recusado. Erros: `400` arquivo inválido,
`413` grande demais, `503` storage não configurado.

Configuração: `GCS_BUCKET` (sem ele o upload fica desabilitado) e credenciais por Application
Default Credentials — service account anexada ao serviço no GCP, ou
`GOOGLE_APPLICATION_CREDENTIALS` com o caminho do JSON da chave (nunca commitar). A service
account precisa de `roles/storage.objectAdmin` no bucket, que deve ser **privado**.

### Proteção contra força bruta no login

O controle é **por IP**, em memória (`LoginThrottleService`), e **não depende de o usuário
existir**: usuário real e inexistente recebem exatamente as mesmas respostas.

- **IP + username**: 5 falhas dentro de 15 min bloqueiam aquele par por 15 min (`429` com
  `Retry-After`). O bloqueio **nunca é permanente** e não afeta outros IPs, então ninguém
  consegue trancar a conta de terceiros (antes, 15 requisições anônimas trancavam qualquer
  conta, inclusive o ADMIN).
- **Por IP**: no máximo 30 tentativas de login a cada 5 min, qualquer que seja o username
  (barra *password spraying*, que o contador por username nunca pegaria).
- Login bem-sucedido zera o contador do par. Durante o bloqueio até a senha correta é recusada.
- `PATCH /api/usuarios/{id}/desbloquear` (ADMIN, ou GERENTE da mesma oficina) libera o username
  em todos os IPs — útil para um usuário legítimo atrás de uma rede compartilhada.
- A confirmação da senha atual em `PUT /api/usuarios/me` tem o mesmo limite (por usuário).
- Configurável: `oficinapro.login.tentativas-por-bloqueio` (5), `janela-falhas` (`15m`),
  `duracao-bloqueio` (`15m`), `max-tentativas-por-ip` (30) e `janela-ip` (`5m`).
- O IP vem de `CF-Connecting-IP` / `X-Forwarded-For` **somente** com
  `oficinapro.security.trust-proxy-headers=true` (`TRUST_PROXY_HEADERS`), que só é seguro
  quando a API não é alcançável diretamente (túnel Cloudflare, sem porta publicada).
- Com várias instâncias da API o limite vale por instância; o rate limit do Cloudflare
  continua sendo a barreira global.
- As colunas `falhas_login`, `bloqueado_ate` e `bloqueio_permanente` ficaram sem uso pelo
  login (legado da V13). O `UsuarioResponseDTO.bloqueado` reflete só esse legado.

### Alterar os próprios dados (`PUT /api/usuarios/me`)

Trocar a **senha** ou o **username** exige `senhaAtual` no corpo (um token roubado, sozinho,
não assume a conta). Trocar só nome/documento/telefone não exige. Ao trocar a senha, todos os
tokens do usuário são revogados: o frontend deve pedir novo login.

### Auditoria

Eventos de segurança e financeiros gravam uma linha em `audit_log` (ator, papel, oficina,
alvo, IP e um detalhe curto, sem dados pessoais) e no logger `AUDIT`: login, logout, troca de
senha, criação/alteração/exclusão/desbloqueio de usuário, ativar/desativar oficina,
exclusão de OS, desconto, recebimento e estorno. Falhas de login vão só para o log
(`evento=LOGIN_FALHA`, com um hash curto do username, nunca o valor digitado).

### Paginação e busca no servidor

As listagens que podem crescer sem limite devolvem **uma página** (formato do Spring Data:
`content`, `totalElements`, `totalPages`, `number`, `size`, `last`...), e **a busca roda no
servidor sobre TODOS os registros da oficina**, não sobre a página já carregada. É isso que
garante que um registro fora da página atual continue sendo encontrado.

| Rota | Parâmetros | A busca (`q`) casa com |
|---|---|---|
| `GET /api/ordens-servico` | `q`, `status`, `page`, `size`, `sort` | placa (sem hífen/caixa), nome do cliente, status, número da OS (`12`, `#0012`) |
| `GET /api/ordens-servico/{veiculo,mecanico,unidade,cliente}/{id}` e `/status/{status}` | `page`, `size`, `sort` | — (já filtradas pela relação) |
| `GET /api/pagamentos/oficina/{id}` | `q`, `status`, `page`, `size`, `sort` | parte do número da OS ou do pagamento |
| `GET /api/itens-os-peca` | `q`, `avulsas`, `page`, `size`, `sort` | nome da peça ou parte do número da OS |
| `GET /api/clientes`, `/veiculos`, `/mecanicos`, `/usuarios` e as respectivas `/buscar` | `q`, `page`, `size`, `sort` | (já existiam) |

Regras comuns:

- **Teto de 100 por página** (`spring.data.web.pageable.max-page-size`); `size=100000` vira 100.
- **Ordenação restrita**: só campos escalares permitidos por recurso (ex.: OS por `id`,
  `dataAbertura`, `dataFechamento`, `status`, `valorTotal`, `valorComDesconto`). Qualquer outro
  caminho (`oficina.cnpj`, `password`...) é ignorado e cai na ordenação padrão (`id` decrescente).
  Antes, o cliente podia ordenar por qualquer propriedade da entidade.
- **Curingas do usuário são texto comum**: `%` e `_` digitados na busca não casam tudo
  (`LIKE ... ESCAPE '!'`).
- **Sempre restrito à oficina do usuário**; OS/pagamentos/peças de outra oficina nunca entram,
  nem com placa ou nome iguais.

**Autocomplete.** `GET /api/clientes/nome/{nome}`, `/mecanicos/nome/{nome}` e
`/usuarios/nome/{nome}` pesquisam em toda a oficina e devolvem **no máximo 20** resultados em ordem
alfabética (quem precisa de mais, digita mais letras: o registro certo aparece mesmo estando além
dos 20 primeiros). `GET /api/veiculos/buscar?q=&size=10` e `GET /api/itens-os-peca?avulsas=true&q=`
servem os autocompletes de veículo e de relacionar peça.

**Totais e contagens vêm do banco**, não da soma da página:

- `GET /api/pagamentos/oficina/{id}/resumo` → `{ totalRecebido, valorAReceber, pendentes }`.
- `GET /api/pagamentos/oficina/{id}/por-os?osIds=1,2,3` → pagamentos só das OS pedidas (até 100),
  restritos à oficina; a tela de OS usa para mostrar o valor pendente das OS da página.
- O dashboard conta OS abertas e pagamentos pendentes com `COUNT` no banco.

Não paginadas de propósito: `GET /api/unidades` (poucas por oficina; o autocomplete de unidade
filtra essa lista) e as listas de peças e mão de obra **de uma OS** (limitadas pela própria OS).

### Buscas por documento (CPF/CNPJ)

Vão no **corpo** de um `POST`, não na URL (URLs ficam em logs de acesso, proxies e histórico):
`POST /api/clientes/documento/buscar`, `POST /api/mecanicos/documento/buscar`,
`POST /api/usuarios/documento/buscar` e `POST /api/usuarios/admin/documento/buscar`, com
`{ "documento": "12345678901" }`.

Os dois primeiros grupos (401 de filtro e 403) são respondidos pelo
`SecurityErrorResponder`, não pelo `GlobalExceptionHandler` — erro de filtro acontece
antes de chegar a um controller. O formato JSON é o mesmo.

---

## 3. Contrato de erro

Todo erro tratado responde com o mesmo envelope:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "A ordem de serviço já possui um pagamento.",
  "timestamp": "2025-01-15T10:30:00.123"
}
```

| Campo | Conteúdo |
|---|---|
| `status` | código HTTP, repetido no corpo |
| `error` | reason phrase do status (`Conflict`, `Not Found`, ...) |
| `message` | mensagem em português, destinada a ser exibida ao usuário |
| `timestamp` | `LocalDateTime` do servidor |

Nunca aparecem: stacktrace, nome de classe de exceção, SQL, nome de constraint.

### Erro de validação

Único caso com campo extra. `MethodArgumentNotValidException` (violação de Bean
Validation no `@Valid`) acrescenta `fields`:

```json
{
  "status": 400,
  "error": "Validation Error",
  "message": "Dados inválidos",
  "timestamp": "2025-01-15T10:30:00.123",
  "fields": {
    "osId": "O ID da Ordem de Serviço é obrigatório",
    "valor": "O valor é obrigatório"
  }
}
```

`fields` mapeia **nome do campo → mensagem**, e é o que o frontend usa para marcar o input
errado. Note que `error` aqui é `"Validation Error"`, não a reason phrase.

### Mapeamento completo exceção → status

**400 — requisição inválida**

| Exceção | Quando |
|---|---|
| `MethodArgumentNotValidException` | Bean Validation falhou (traz `fields`) |
| `HttpMessageNotReadableException` | JSON malformado |
| `MethodArgumentTypeMismatchException` | path variable com tipo errado (`/clientes/abc`) |
| `IllegalStateException` | transição de status proibida; fechar OS sem pagamento integral |
| `DescontoInvalidoException` | desconto negativo ou maior que o total |
| `PagamentoValorInvalidoException` | estorno maior que o valor pago |
| `OficinaIncompativelComRoleException` | papel incompatível com `oficinaId` |
| `InvalidDataAccessApiUsageException` | uso incorreto da API de dados |
| `DateTimeException` | data inválida (mês 13 no fluxo mensal) |

**401 — não autenticado** · **403 — sem permissão** · **429 — muitas tentativas de login**

Ver §2.

**404 — não encontrado**

`OficinaNotFoundException`, `UnidadeNotFoundException`, `ClienteNotFoundException`,
`MecanicoNotFoundException`, `UsuarioNotFoundException`, `VeiculoNotFoundException`,
`OrdemDeServicoNotFoundException`, `ItemOsPecaNotFoundException`,
`MaoObraNotFoundException`, `PagamentoNotFoundException`,
`PagamentoNotFoundForThisOsException`, `RegistroPagamentoNotFoundException`.

> **404 também significa "existe, mas não é seu".** Ao acessar registro de outra oficina
> a API responde 404, não 403. Responder "sem permissão" confirmaria a existência do
> registro e permitiria enumerar ids. Do lado do cliente, os dois casos são
> indistinguíveis — de propósito.

**409 — conflito**

| Exceção | Quando |
|---|---|
| `CnpjAlreadyExistsException` | CNPJ de oficina já cadastrado |
| `ClienteAlreadyExistsException` / `MecanicoAlreadyExistsException` / `UsuarioAlreadyExistsException` | documento já existe **na mesma oficina** |
| `UsernameAlreadyExistsException` | username já existe (escopo global) |
| `PlacaAlreadyExistsException` | placa já existe na oficina |
| `EnderecoAlreadyExistsException` | endereço já existe na oficina |
| `PagamentoAlreadyExistsException` | segundo pagamento para a mesma OS |
| `PagamentoValorExcedidoException` | pagar acima do valor; reduzir OS abaixo do já pago |
| `DataIntegrityViolationException` | violação de integridade no banco |
| `IncorrectResultSizeDataAccessException` | consulta que deveria trazer 1 trouxe N |

As duas últimas respondem com mensagem **genérica**, sem nome de constraint nem SQL.
Vazar `uk_pagamento_os` numa resposta HTTP entrega o desenho do schema.

**422 — regra de negócio da OS**

| Exceção | Quando |
|---|---|
| `OSCanceledException` | alterar status ou lançar item em OS cancelada |
| `OSFinishedException` | lançar item em OS fechada |
| `OSIsNotPossibleSwapWorkshopException` | tentar mover a OS para outra oficina |

A distinção 400 × 422 aqui é: **400** = a requisição está errada; **422** = a requisição
está correta, mas o estado atual da OS não permite a operação.

### Sem handler genérico

Não existe `@ExceptionHandler(Exception.class)`. Exceção não prevista vira `500` com o
tratamento padrão do Spring. É deliberado: um catch-all transforma bug em `500`
silencioso e maquiado, dificultando diagnóstico.

O reflexo prático é que **um `500` é sempre bug**, nunca uso incorreto da API. Se
aparecer, falta um handler ou há defeito — foi assim que se descobriu que
`PagamentoAlreadyExistsException` não tinha handler e o conflito 1:1 escapava como `500`.

---

## 4. Paginação

Apenas os endpoints de `pessoa` (clientes, mecânicos, usuários) são paginados. Usam
`Pageable` do Spring Data com `@PageableDefault(size = 20, sort = "nome")`.

```http
GET /api/clientes/oficina/1?page=0&size=20&sort=nome,asc
```

| Parâmetro | Default |
|---|---|
| `page` | `0` |
| `size` | `20` |
| `sort` | `nome` ascendente |

Resposta é o envelope `Page` do Spring:

```json
{
  "content": [ ... ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

Os demais endpoints devolvem **lista simples**, sem paginação — OS, peças, mão de obra,
pagamentos, unidades, oficinas. Numa oficina com histórico grande, `GET
/api/ordens-servico` retorna tudo. É limitação conhecida.

---

## 5. Convenções de tipo

### Dinheiro

Número inteiro **em centavos** no JSON (123456 = R$ 1.234,56). No banco, `NUMERIC(12,0)`; em Java,
`BigDecimal` com escala 0. É a mesma representação do frontend, sem conversão na borda HTTP;
quem exibe (telas, PDF) divide por 100. Ver [business-rules.md §1](./business-rules.md). Os DTOs de dinheiro (peça, mão de obra e registro
de pagamento) recusam casas decimais: `25000` é aceito, `250.00` responde `400`.

### Data e hora

`LocalDateTime` em ISO-8601 sem timezone: `2025-01-15T10:30:00`. O servidor opera em
`America/Sao_Paulo`.

### Enums

Sempre **string**, nunca ordinal:

| Enum | Valores |
|---|---|
| `Role` | `ADMIN`, `GERENTE`, `MECANICO` |
| `StatusOrdemDeServico` | `ABERTA`, `DIAGNOSTICO`, `AGUARDANDO_APROVACAO`, `AGUARDANDO_PECAS`, `EM_EXECUCAO`, `FINALIZADA`, `ENTREGUE`, `FECHADA`, `CANCELADA` |
| `StatusPagamento` | `PAGAMENTO_PENDENTE`, `PAGO_PARCIALMENTE`, `PAGA` |
| `MeioPagamento` | `PIX`, `DINHEIRO`, `CARTAO_CREDITO`, `CARTAO_DEBITO`, `CHEQUE` |

Valor inválido em path variable de enum dá `400` (`MethodArgumentTypeMismatchException`).

### Documento e placa

- `documento` (CPF/CNPJ) é armazenado **sem máscara**, só dígitos, máx. 14.
- `placa` é normalizada antes de gravar e de buscar: alfanuméricos, maiúsculas, 7
  caracteres. `abc-1234` e `ABC1234` são a mesma placa.

Formatar para exibição é responsabilidade do cliente.

---

## 6. Códigos de sucesso

| Operação | Status |
|---|---|
| `GET` | `200` |
| `POST` (criação) | `201` |
| `PUT` / `PATCH` | `200` com o recurso atualizado |
| `DELETE` | `204`, sem corpo |

---

## 7. Estado do OpenAPI

Configurado em `OpenApiConfig`, com o esquema `bearerAuth` aplicado globalmente. Rotas
públicas se desmarcam com `@SecurityRequirements` — como faz o `/api/auth/login`.

Todos os 12 controllers têm `@Tag`, `@Operation` em cada endpoint e `@ApiResponses`
cobrindo os status realistas (incluindo `403`/`404` de isolamento por oficina, `409` de
conflito e lock otimista, `422` de regra de estado da OS). O Swagger UI já é
autossuficiente para explorar a API.

Lacuna que ainda resta:

1. **Erros documentados como `Map` genérico.** Não há um `ErroResponseDTO` tipado, então
   o *schema* do corpo de erro não aparece no OpenAPI — só a descrição textual em cada
   `@ApiResponse`. É o que este documento supre na §3. Criar o DTO e referenciá-lo com
   `@Content(schema = @Schema(implementation = ErroResponseDTO.class))` eliminaria essa
   lacuna, mas exige tocar todo `GlobalExceptionHandler`.
