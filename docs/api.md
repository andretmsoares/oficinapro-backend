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

{ "username": "admin", "password": "senha-com-8-ou-mais" }
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

Validação do corpo: `username` obrigatório (máx. 100), `password` obrigatório (entre 8 e
255). Violar isso dá `400`, não `401`.

### Usando o token

```http
GET /api/clientes/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

O filtro ignora o header quando ele não começa com `Bearer ` — nesse caso a requisição
segue como não autenticada e termina em `401`.

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
| `sub` | username |
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

`Credenciais inválidas` é intencionalmente genérica: não diz se o problema foi o usuário
inexistente ou a senha errada. Diferenciar permitiria enumerar usuários válidos.

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

**401 — não autenticado** · **403 — sem permissão**

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

Número decimal com 2 casas no JSON: `1234.56`. No banco, `NUMERIC(12,2)`; em Java,
`BigDecimal`.

> **O frontend usa outra representação.** Internamente ele guarda **centavos como
> inteiro** (`123456`). A conversão na borda HTTP é obrigatória e **ainda não existe**,
> porque a camada HTTP ainda não existe. Ver
> [business-rules.md §1](./business-rules.md).

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
