# Regras de negócio

---

## 1. Unidade monetária

A convenção mais importante do sistema, e a que mais causou bug.

| Camada | Representação | Exemplo para R$ 1.234,56 |
|---|---|---|
| Banco | `NUMERIC(12,2)` | `1234.56` |
| Backend (Java) | `BigDecimal` | `new BigDecimal("1234.56")` |
| JSON da API | número decimal | `1234.56` |
| Frontend — valor canônico | **inteiro, em centavos** | `123456` |
| Frontend — exibição | string formatada pt-BR | `"R$ 1.234,56"` |

O frontend guarda **centavos como inteiro** no `rawValues` do `EntityForm` e a string
mascarada no `displayValues`. A conversão acontece em `src/services/formatters.ts`
(`parseCurrencyToCents` e `formatCurrencyDisplay`).

> **Atenção na integração.** O backend espera decimal (`1234.56`); o frontend mantém
> centavos (`123456`). A conversão na borda HTTP **ainda não existe**, porque a camada
> HTTP ainda não existe. Quando ela for escrita, esta divisão por 100 é obrigatória —
> ignorá-la multiplica todo valor por 100.

Nunca use `double` ou `float` para dinheiro. Nunca compare `BigDecimal` com `equals`:
use `compareTo`, porque `equals` também compara escala e `2.0` não é `equals` a `2.00`.
Nos testes, use `isEqualByComparingTo` do AssertJ.

---

## 2. Composição do valor da OS

```
valorTotal       = soma(itens de peça) + soma(mão de obra)
valorComDesconto = valorTotal − desconto
```

A única fonte de verdade é `OrdemDeServicoValorRecalculator.recalcular()`.

### Por que existe um recalculador

O total já foi calculado apenas a partir das peças, e mão de obra não existia no backend
— o frontend somava peças + mão de obra e o backend só peças, então as duas telas
mostravam números diferentes para a mesma OS.

Quando a mão de obra virou entidade, dois services passaram a precisar recalcular o
total (`ItemOsPecaServiceImpl` e `MaoObraServiceImpl`). Se cada um fizesse sua conta, um
sobrescreveria o total ignorando a contribuição do outro. O recalculador centraliza isso:

```java
BigDecimal totalPecas   = soma de ItemOsPeca.valorTotal   da OS
BigDecimal totalMaoObra = soma de MaoObra.valor           da OS
ordemDeServicoService.recalcularValorTotal(os.getId(), totalPecas.add(totalMaoObra));
pagamentoService.recalcularStatus(os.getId());
```

O teste `OrdemDeServicoValorRecalculatorTest.deveSomarPecasEMaoDeObra` existe
especificamente para impedir que uma das duas origens volte a ser ignorada.

### Valor de um item de peça

```
ItemOsPeca.valorTotal = quantidade × valorUnitario, arredondado a 2 casas (HALF_UP)
```

`quantidade` é sempre **inteira** — peça é contada em unidades (2 pastilhas, 1 correia),
nunca em fração. Validado com `@Digits(fraction = 0)` tanto na criação quanto na
atualização. A coluna no banco é `NUMERIC(12,3)` por herança da migration original, mas
não representa fração válida — é só precisão não utilizada.

Validações do DTO: `quantidade` inteira e positiva, `valorUnitario >= 0.01`, nome
obrigatório com no máximo 255 caracteres.

### Valor de mão de obra

`MaoObra` tem `valor` e `descricao`, ambos obrigatórios. Não há cálculo derivado: o valor
é informado direto.

### Quando o total é recalculado

Criar, atualizar ou excluir peça **ou** mão de obra dispara `recalcular()`. Sempre na
ordem: persiste primeiro, recalcula depois (verificado com `InOrder` nos testes).

---

## 3. Desconto

- É **valor absoluto em reais**, não percentual.
- Não pode ser negativo → `400` (`DescontoInvalidoException`).
- Não pode ser maior que o `valorTotal` → `400`.
- Aplicado por `PATCH /api/ordens-servico/{id}/desconto`, exclusivo do `GERENTE`.

### Desconto quando o total diminui

Situação: OS de R$ 500 com desconto de R$ 100. Alguém remove peças e o total cai para
R$ 80. O desconto de R$ 100 passou a ser maior que o total.

Regra em `recalcularValorTotal`: o desconto é **travado no novo valor total**.

```
novoValorTotal = 80
desconto (era 100) → 80
valorComDesconto = 0
```

`valorComDesconto` nunca fica negativo. O desconto é reduzido silenciosamente — decisão
consciente, para não bloquear a remoção de uma peça lançada por engano.

---

## 4. Máquina de estados da OS

Enum `StatusOrdemDeServico`, nove valores. Implementada em
`OrdemDeServicoServiceImpl.transicaoPermitida`.

```
ABERTA               → DIAGNOSTICO | CANCELADA
DIAGNOSTICO          → AGUARDANDO_APROVACAO | CANCELADA
AGUARDANDO_APROVACAO → AGUARDANDO_PECAS | CANCELADA
AGUARDANDO_PECAS     → EM_EXECUCAO | CANCELADA
EM_EXECUCAO          → FINALIZADA | CANCELADA
FINALIZADA           → ENTREGUE | ABERTA
ENTREGUE             → FECHADA | ABERTA
FECHADA              → ABERTA
CANCELADA            → (terminal)
```

Em diagrama:

```
ABERTA ─→ DIAGNOSTICO ─→ AGUARDANDO_APROVACAO ─→ AGUARDANDO_PECAS ─→ EM_EXECUCAO
   │           │                  │                      │                │
   └───────────┴──────────────────┴──────────────────────┴────────────────┤
                                                                          ↓
                                                                     CANCELADA
                                                                     (terminal)

EM_EXECUCAO ─→ FINALIZADA ─→ ENTREGUE ─→ FECHADA
                    │             │           │
                    └─────────────┴───────────┴──→ ABERTA   (reabertura)
```

### Regras que atravessam o grafo

Avaliadas em `validarTransicaoStatus`, **nesta ordem**:

1. **OS cancelada é terminal.** Qualquer alteração de status em uma OS `CANCELADA` lança
   `OSCanceledException` → `422`. Inclusive para o mesmo status.
2. **MECANICO não encerra nem cancela.** Destinos `FINALIZADA`, `ENTREGUE` e `CANCELADA`
   são negados ao MECANICO com `403`.
3. **`FECHADA` exige pagamento integral.** O pagamento da OS precisa estar com status
   `PAGA`, senão `400` ("a Ordem de Serviço só pode ser fechada após o pagamento
   integral").
4. **Repetir o status atual é permitido** e não faz nada (no-op idempotente).
5. Fora disso, transição fora do grafo lança `IllegalStateException` → `400`.

Consequência da ordem: a checagem de pagamento (3) roda **antes** da validação do grafo
(5). Tentar `ABERTA → FECHADA` com pagamento pendente devolve o erro de pagamento, não o
de transição inválida. Há teste cobrindo isso
(`checagemDePagamentoPrecedeValidacaoDeTransicao`).

### `dataFechamento`

Status de conclusão — `FINALIZADA`, `ENTREGUE`, `FECHADA` — **preservam** a
`dataFechamento`, preenchendo-a com o horário atual se ainda estiver nula. Qualquer outro
status a **limpa**.

`FECHADA` já ficou fora dessa lista, e o efeito era silencioso e grave: ao concluir o
ciclo, a data registrada na entrega era apagada. Como o relatório de fluxo mensal conta
OS concluídas por `dataFechamento`, a OS **desaparecia do relatório exatamente ao ser
finalizada**.

Ponto aberto: `CANCELADA` limpa a `dataFechamento`. É decisão de produto pendente — se
cancelamento contar como encerramento, basta incluí-lo em `ehStatusDeConclusao`.

### Editabilidade de peças e mão de obra

`OrdemDeServicoValorRecalculator.validarOsEditavel` bloqueia lançamentos em:

| Status | Aceita lançar peça / mão de obra? |
|---|---|
| `CANCELADA` | ❌ `422` (`OSCanceledException`) |
| `FECHADA` | ❌ `422` (`OSFinishedException`) |
| todos os outros | ✅ |

`FINALIZADA` e `ENTREGUE` **ainda aceitam** alteração. É intencional: descobrir uma peça
esquecida depois de finalizar é comum, e a OS só trava de vez ao ser fechada, o que exige
pagamento integral. Essa regra já foi mais restritiva, e havia teste afirmando que
`ENTREGUE` bloqueava — hoje não bloqueia.

### Troca de oficina

`PUT /api/ordens-servico/{id}` não permite mover a OS para outra oficina. Tentar lança
`OSIsNotPossibleSwapWorkshopException` → `422`.

---

## 5. Pagamento

### Relação 1:1 com a OS

Cada OS tem exatamente um pagamento. Garantido em dois níveis:

- aplicação: `PagamentoAlreadyExistsException` → `409`;
- banco: constraint `uk_pagamento_os` (`V13`).

O pagamento é **criado automaticamente junto com a OS** — `OrdemDeServicoServiceImpl.criar`
chama `pagamentoService.criar`. Não é preciso criá-lo à mão no fluxo normal.

O handler de `PagamentoAlreadyExistsException` já faltou no `GlobalExceptionHandler`, e a
violação escapava como `500` — um erro de uso da API mascarado de erro de servidor.

### Estado inicial

Um pagamento nasce sempre com `valorPago = 0` e status `PAGAMENTO_PENDENTE`. O valor
**não** vem do corpo da requisição: `PagamentoRequestDTO` tem apenas `(osId, obs)`.

### Status

Enum `StatusPagamento`, três valores:

| Status | Condição |
|---|---|
| `PAGAMENTO_PENDENTE` | `valorPago == 0` |
| `PAGO_PARCIALMENTE` | `0 < valorPago < valorComDesconto` |
| `PAGA` | `valorPago == valorComDesconto` e `valorPago > 0` |

Nunca existe status para "pago a mais": isso é recusado (§5.4).

Detalhe: uma OS com `valorComDesconto = 0` e nada pago fica `PAGAMENTO_PENDENTE`, não
`PAGA`. A checagem de `valorPago == 0` vem primeiro, de propósito — uma OS sem itens não
deve aparecer como paga.

`dataPagamentoTotal` é preenchida quando o status vira `PAGA` e limpa nos outros casos.

### Receber e estornar

Os dois caminhos compartilham o núcleo `ajustarValorPago(id, delta)`; estorno é delta
negativo.

- Receber acumula: pagou 300, depois 200, numa OS de 500 → `PAGA`.
- Estorno total zera e volta a `PAGAMENTO_PENDENTE`.
- Estorno parcial de uma OS `PAGA` volta para `PAGO_PARCIALMENTE`.
- Estornar mais do que foi pago → `400` (`PagamentoValorInvalidoException`).

Ambos exigem `GERENTE` no service, além do `@PreAuthorize`.

### Pagamento a maior é recusado

Não existe "crédito" nem valor negativo. Receber acima do `valorComDesconto` lança
`PagamentoValorExcedidoException` → `409`.

### Mudar o valor da OS repercute no pagamento

`recalcularStatus(osId)` é chamado sempre que o valor da OS muda (via recalculador).
Comportamento:

| Situação | Resultado |
|---|---|
| OS aumenta de valor e estava `PAGA` | volta a `PAGO_PARCIALMENTE`, `dataPagamentoTotal` limpa |
| OS cai exatamente para o valor já pago | vira `PAGA` |
| OS cai **abaixo** do valor já pago | ❌ `409` — operação recusada |

O último caso é o mais importante. Antes, reduzir o valor de uma OS já paga produzia
"valor a receber" negativo, que contaminava o total da oficina. Hoje a operação é
recusada em vez de gerar dado inconsistente.

`recalcularStatus` **não** exige papel: é chamado por fluxo interno, nunca direto por
controller. Há teste garantindo que ele não invoca `validarRole`.

### Excluir peça ou mão de obra de uma OS já paga

Antes de excluir, o service calcula o que sobraria:

```
valorRestante = valorComDesconto − valor do item a excluir
se valorRestante < valorPago  →  409 (PagamentoValorExcedidoException)
```

Ou seja: não se remove um item se isso deixaria a OS valendo menos do que o cliente já
pagou. Igualdade exata é permitida (`valorRestante == valorPago` passa).

Para fazer essa remoção é preciso estornar o pagamento primeiro.

### Atualizar pagamento

`PUT /api/pagamentos/{id}` altera **apenas a observação**. Valor pago e status não são
editáveis por esse caminho — só por recebimento, estorno ou recálculo. Já foi possível
sobrescrever o valor livremente.

### Valor a receber da oficina

`GET /api/pagamentos/oficina/{oficinaId}/a-receber` soma o saldo em aberto considerando
somente pagamentos `PAGAMENTO_PENDENTE` e `PAGO_PARCIALMENTE`. Exige `GERENTE` e valida
acesso à oficina — esse endpoint já expôs o faturamento de qualquer oficina.

### Registro de pagamento

`RegistroPagamento` é o histórico: cada recebimento gera um registro com meio de
pagamento (`MeioPagamento`: `PIX`, `DINHEIRO`, `CARTAO_CREDITO`, `CARTAO_DEBITO`,
`CHEQUE`). Excluir um registro **estorna** o valor correspondente no pagamento.

---

## 6. Cadastros

### Documento único por oficina

A unicidade de `documento` (CPF/CNPJ) em `pessoa` é **por oficina**, não global:
constraint `(oficina_id, documento)`. O mesmo CPF pode ser cliente de duas oficinas
diferentes — situação normal.

No PostgreSQL, `NULL` não colide com `NULL` em índice único; logo registros sem oficina
(o ADMIN do SaaS) não disputam unicidade. Por isso a verificação de duplicidade só roda
quando há `oficinaId`.

### Endereço da unidade

Único **por oficina**: constraint `uq_unidade_oficina_endereco` (`V18`). Duas oficinas
podem operar no mesmo endereço (prédio compartilhado, troca de ponto comercial).

Já foi único globalmente, e a oficina B recebia `409` por um endereço usado pela oficina
A — o que confirmava a existência de unidades de outro tenant.

### Placa de veículo

Normalizada antes de gravar e antes de buscar: só alfanuméricos, maiúsculas, 7
caracteres. `abc-1234` e `ABC1234` são a mesma placa. Duplicidade → `409`
(`PlacaAlreadyExistsException`).

### Username único globalmente

Diferente do documento, `username` é único em toda a plataforma — é credencial de login.
Duplicidade → `409` (`UsernameAlreadyExistsException`).

### Papel × oficina

`ADMIN` não pertence a oficina (`oficinaId` nulo). `GERENTE` e `MECANICO` exigem oficina.
Violação → `400`. Ver [permissions.md §4](./permissions.md).

---

## 7. Resumo de erros por regra

| Regra violada | Status | Exceção |
|---|---|---|
| Desconto negativo ou maior que o total | 400 | `DescontoInvalidoException` |
| Transição de status fora do grafo | 400 | `IllegalStateException` |
| Fechar OS sem pagamento integral | 400 | `IllegalStateException` |
| Estorno maior que o valor pago | 400 | `PagamentoValorInvalidoException` |
| Papel incompatível com oficina | 400 | `OficinaIncompativelComRoleException` |
| Alterar status de OS cancelada | 422 | `OSCanceledException` |
| Lançar item em OS fechada | 422 | `OSFinishedException` |
| Trocar a oficina da OS | 422 | `OSIsNotPossibleSwapWorkshopException` |
| Pagar acima do valor da OS | 409 | `PagamentoValorExcedidoException` |
| Reduzir OS abaixo do já pago | 409 | `PagamentoValorExcedidoException` |
| Segundo pagamento para a mesma OS | 409 | `PagamentoAlreadyExistsException` |
| Documento duplicado na oficina | 409 | `*AlreadyExistsException` |
| Placa duplicada | 409 | `PlacaAlreadyExistsException` |
| Endereço duplicado na oficina | 409 | `EnderecoAlreadyExistsException` |
| Username duplicado | 409 | `UsernameAlreadyExistsException` |
| MECANICO finalizando/cancelando OS | 403 | `AccessDeniedException` |

Contrato completo de erro em [api.md](./api.md).

---

## 8. Onde isso é testado

| Arquivo | Cobre |
|---|---|
| `OrdemDeServicoStatusMachineTest` | grafo completo (transições válidas e inválidas geradas por diferença de conjuntos), restrições do MECANICO, `FECHADA` × pagamento, `dataFechamento` |
| `OrdemDeServicoValorRecalculatorTest` | soma peças + mão de obra, editabilidade por status |
| `PagamentoServiceImplTest` | status, acúmulo, estorno, pagamento a maior, recálculo ao mudar o valor da OS, 1:1 |
| `ItemOsPecaServiceTest` | cálculo do valor do item, exclusão vs. valor pago |
| `MaoObraServiceTest` | CRUD, exclusão vs. valor pago, `osId` ignorado no update |
| `OrdemDeServicoServiceTest` | desconto, troca de oficina, atribuições |

O grafo de estados no teste é declarado **independentemente da implementação**: ele
descreve a regra pretendida. Alterar `transicaoPermitida` sem alterar a regra quebra o
teste — que é o objetivo.
