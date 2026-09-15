# Banco de dados

PostgreSQL 16, schema versionado com Flyway. Este documento traz o ER, o que cada
migration fez e as divergências conhecidas entre entidade e schema.

Última verificação contra o código: branch `docs`, 19 migrations.

---

## 1. Configuração

| Item | Valor | Por quê |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `validate` | o Hibernate **nunca** altera o banco; só confere que o schema atende ao mapeamento |
| `spring.flyway.enabled` | `true` | o schema é versionado em SQL revisável |
| `spring.flyway.baseline-on-migrate` | `true` | permite adotar Flyway em banco que já tinha tabelas |
| `spring.jpa.open-in-view` | `false` | evita lazy loading acidental na camada web |
| `hibernate.jdbc.time_zone` | `America/Sao_Paulo` | `TIMESTAMP` sem timezone interpretado de forma consistente |

### O schema de teste NÃO é este

No perfil `test`:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL
spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false
```

O schema dos testes é **gerado pelas entidades JPA**, não pelas migrations. Isso é rápido
e isolado, mas tem uma consequência que já custou um bug grave: **constraints declaradas
apenas em SQL não são exercidas pelos testes**.

Foi exatamente o que aconteceu com `chk_usuario_role` — a constraint proibia o valor
`'GERENTE'` e nenhum teste percebeu, porque as entidades não declaram esse `CHECK`. O
problema só apareceria em produção.

> **Consequência prática:** validar migrations exige subir um PostgreSQL de verdade. A
> suíte de testes não substitui isso. Ver §6.

---

## 2. Modelo entidade-relacionamento

```
                        ┌──────────────┐
                        │   oficina    │
                        │──────────────│
                        │ id           │
                        │ nome         │
                        │ cnpj    UQ   │
                        │ telefone     │
                        └──────┬───────┘
                               │ 1
        ┌──────────────┬───────┴───────┬────────────────┐
        │ N            │ N             │ N              │ N
┌───────▼──────┐ ┌─────▼──────┐ ┌──────▼──────┐ ┌───────▼────────┐
│   unidade    │ │   pessoa   │ │   veiculo   │ │ ordem_servico  │
│──────────────│ │────────────│ │─────────────│ │────────────────│
│ id           │ │ id         │ │ id          │ │ id             │
│ oficina_id   │ │ oficina_id │ │ oficina_id  │ │ oficina_id     │
│ nome         │ │ nome       │ │ modelo      │ │ cliente_id   ○ │
│ endereco     │ │ telefone   │ │ ano         │ │ veiculo_id     │
│ telefone     │ │ documento  │ │ marca       │ │ unidade_id     │
│              │ │            │ │ placa       │ │ mecanico_id  ○ │
│ UQ(oficina_  │ │ UQ(oficina_│ │ UQ(oficina_ │ │ data_abertura  │
│    id,       │ │    id,     │ │    id,      │ │ data_fechamento│
│    endereco) │ │    doc)    │ │    placa)   │ │ status         │
└──────────────┘ └─────┬──────┘ └─────────────┘ │ obs            │
                       │ 1  (herança JOINED)    │ valor_total    │
         ┌─────────────┼─────────────┐          │ desconto       │
         │             │             │          │ valor_com_     │
  ┌──────▼─────┐ ┌─────▼──────┐ ┌────▼───────┐  │   desconto     │
  │  cliente   │ │  usuario   │ │  mecanico  │  └───────┬────────┘
  │────────────│ │────────────│ │────────────│          │ 1
  │ pessoa_id  │ │ pessoa_id  │ │ pessoa_id  │     ┌────┴──────────┬──────────────┐
  │    (PK/FK) │ │ username UQ│ │ salario    │     │ 1:1           │ N            │ N
  └────────────┘ │ password   │ │ obs        │ ┌───▼──────────┐ ┌──▼──────────┐ ┌─▼─────────┐
                 │ role   CHK │ └────────────┘ │  pagamento   │ │ item_os_peca│ │ mao_obra  │
                 └────────────┘                │──────────────│ │─────────────│ │───────────│
                                               │ id           │ │ id          │ │ id        │
                                               │ os_id     UQ │ │ os_id       │ │ os_id     │
                                               │ valor_pago   │ │ nome        │ │ valor     │
                                               │ obs          │ │ quantidade  │ │ descricao │
                                               │ data_pagam...│ │ valor_unit. │ └───────────┘
                                               │ status       │ │ valor_total │
                                               └──────┬───────┘ └─────────────┘
                                                      │ 1
                                                      │ N
                                          ┌───────────▼──────────┐
                                          │ registro_pagamento   │
                                          │──────────────────────│
                                          │ id                   │
                                          │ pagamento_id         │
                                          │ valor                │
                                          │ meio_pagamento       │
                                          │ data                 │
                                          └──────────────────────┘

○ = nullable      UQ = unique      CHK = check constraint
```

### Tabelas órfãs (sem entidade)

```
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│  fornecedor  │1───N│ nota_compra  │1───N│ item_nota_compra │
└──────────────┘     └──────────────┘     └──────────────────┘
```

Criadas por `V7` e `V8` para o módulo de compras, que **não foi implementado**. Não têm
entidade JPA, repositório, service nem controller. Ver §5.

---

## 3. Chaves e constraints

### Unicidade

| Tabela | Constraint | Escopo | Nota |
|---|---|---|---|
| `oficina` | `uq_oficina_cnpj` | global | CNPJ é identificador nacional |
| `pessoa` | `uq_pessoa__oficina_doc` | `(oficina_id, documento)` | o mesmo CPF pode ser cliente de duas oficinas |
| `veiculo` | `uq_veiculo_placa_oficina` | `(oficina_id, placa)` | idem para veículos |
| `unidade` | `uq_unidade_oficina_endereco` | `(oficina_id, endereco)` | ver histórico na §4 (V15/V18) |
| `usuario` | `username UNIQUE` | **global** | é credencial de login |
| `pagamento` | `uk_pagamento_os` | `(os_id)` | garante a relação 1:1 com a OS |

Detalhe do PostgreSQL que sustenta o desenho: em índice único, `NULL` **não colide** com
`NULL`. Logo vários registros com `oficina_id` nulo (o ADMIN do SaaS) coexistem sem
disputar unicidade de documento.

### Check

`usuario.chk_usuario_role` → `role IN ('ADMIN', 'GERENTE', 'MECANICO')`.

Precisa ficar sincronizada com o enum `com.oficinapro.enums.Role`. Ver §4 (V19).

### Estratégia de deleção

`ON DELETE CASCADE` nas relações de composição — apagar a oficina apaga suas unidades,
pessoas e veículos; apagar a OS apaga suas peças, mão de obra e pagamento; apagar o
pagamento apaga seus registros.

As FKs da `ordem_servico` para `cliente`, `veiculo`, `unidade` e `mecanico` **não** têm
`ON DELETE`, ficando no `NO ACTION` padrão. É deliberado: não se apaga um veículo que tem
histórico de OS. A tentativa vira `DataIntegrityViolationException` → `409`.

### Índices

`V9` cria índices nas FKs mais consultadas: `idx_unidade_oficina`, `idx_pessoa_oficina`,
`idx_veiculo_oficina`, `idx_os_oficina`, `idx_os_cliente`, `idx_os_veiculo`,
`idx_os_unidade`, `idx_pagamento_os`, `idx_registro_pagamento`, `idx_item_os_peca_os`,
além dos das tabelas de compras.

`idx_pagamento_os` foi substituído em `V13` pela constraint única `uk_pagamento_os` — que
já cria índice próprio, tornando o anterior redundante.

---

## 4. Histórico de migrations

| # | Arquivo | O que faz |
|---|---|---|
| V1 | `create_oficina_table_schema` | `oficina`, `unidade`. `endereco` nasce com `UNIQUE` global |
| V2 | `create_usuarios_table_schemas` | `pessoa` + herança JOINED: `cliente`, `usuario`, `mecanico`. Cria `chk_usuario_role` com `ADMINISTRATIVO` |
| V3 | `create_veiculo_table_schema` | `veiculo`, único por `(oficina_id, placa)` |
| V4 | `create_os_table_schema` | `ordem_servico` |
| V5 | `peca_create_table_schema` | `item_os_peca` |
| V6 | `pagamento_table_schema` | `pagamento` (com `desconto`) e `registro_pagamento` |
| V7 | `create_fornecedor_table_schema` | `fornecedor` — **órfã** |
| V8 | `create_notas_table_schema` | `nota_compra`, `item_nota_compra` — **órfãs**. Adiciona `item_os_peca.item_nota_compra_id` |
| V9 | `create_indexes` | índices nas FKs |
| V10 | `add_status_pagamento` | `pagamento.status VARCHAR(30) NOT NULL` — ⚠️ ver §6 |
| V11 | `add_valor_com_desconto_os` | `ordem_servico.valor_com_desconto` e `.desconto` |
| V12 | `drop_desconto_pagamento` | remove `pagamento.desconto` — o desconto passou a ser da OS |
| V13 | `fix_payment_os` | troca `idx_pagamento_os` por `uk_pagamento_os` → **1 pagamento por OS** |
| V14 | `fix_role` | `UPDATE usuario SET role='GERENTE' WHERE role='ADMINISTRATIVO'` |
| V15 | `fix_unidade_enderco` | remove o `UNIQUE` **global** de `unidade.endereco` |
| V16 | `fix_status_in_payment` | adiciona `pagamento.status`, preenche e torna `NOT NULL` — ⚠️ ver §6 |
| V17 | `create_mao_obra_table` | `mao_obra` com `ON DELETE CASCADE` para a OS |
| V18 | `unidade_endereco_unico_por_oficina` | cria `uq_unidade_oficina_endereco` |
| V19 | `corrige_check_role_gerente` | recria `chk_usuario_role` aceitando `GERENTE` |

### Três correções que valem estudo

**V11 + V12 — desconto mudou de lugar.** O desconto era do pagamento e passou a ser da
ordem de serviço. Faz sentido: desconto é negociação comercial do serviço, não
característica do recebimento. A V11 adiciona na OS, a V12 remove do pagamento.

**V15 + V18 — unicidade no escopo errado.** A V1 declarou `endereco` como único
globalmente. O efeito colateral era grave: a oficina B recebia `409` ao cadastrar um
endereço já usado pela oficina A, o que **confirmava a existência de unidades de outro
tenant**. A V15 removeu a constraint global; a V18 recolocou no escopo correto
`(oficina_id, endereco)`.

Atenção: entre a V15 e a V18 não havia constraint alguma — a mesma oficina podia cadastrar
duas unidades no mesmo endereço.

**V14 + V19 — renomear papel exige mexer na constraint.** A V14 gravou `'GERENTE'`, valor
que o `CHECK` criado na V2 proibia. Efeito: em base com usuários `ADMINISTRATIVO` a
própria V14 falha e a aplicação não sobe; em base sem eles a V14 passa com zero linhas,
mas a constraint continua proibindo `'GERENTE'` e **nenhum gerente pode ser cadastrado**.
A V19 recria a constraint alinhada ao enum.

A V19 não faz limpeza de duplicatas nem `DELETE` de dados: apaga o mínimo possível
(`DROP CONSTRAINT IF EXISTS`), reaplica o `UPDATE` de forma idempotente e recria o
`CHECK`. Migration não é lugar para destruir dado silenciosamente.

---

## 5. Divergências entidade ↔ schema

| # | Divergência | Impacto |
|---|---|---|
| E1 | `fornecedor`, `nota_compra`, `item_nota_compra` sem entidade | nenhum em runtime; são tabelas mortas. `ddl-auto=validate` não reclama de tabela não mapeada |
| E2 | `item_os_peca.item_nota_compra_id` existe no banco, não existe na entidade `ItemOsPeca` | coluna morta com FK para tabela órfã |
| E3 | `pessoa.documento` é nullable no banco; DTOs tratam como opcional com `@Size(max=14)` | documento não é obrigatório — decisão de produto a confirmar |
| E4 | `ordem_servico.cliente_id` e `mecanico_id` nullable | OS pode nascer sem cliente e sem mecânico atribuído; é intencional |

Sobre E1/E2: são resíduo do módulo de compras planejado e não implementado (RF10–RF14).
Duas saídas — implementar o módulo, ou remover as tabelas em uma migration. Manter tabela
sem uso gera dúvida recorrente sobre o que está ou não implementado. Não decida isso sem
confirmar o roadmap.

---

## 6. ⚠️ Risco conhecido: a cadeia de migrations pode não rodar do zero

**V10 e V16 adicionam a mesma coluna.**

```sql
-- V10
ALTER TABLE pagamento ADD COLUMN status VARCHAR(30) NOT NULL;
-- V16
ALTER TABLE pagamento ADD COLUMN status VARCHAR(30);
```

Nenhuma migration entre as duas remove essa coluna (a V12 remove `desconto`, não
`status`). Em um banco **vazio**, a sequência esperada é:

1. V10 aplica com sucesso (tabela sem linhas, então o `NOT NULL` sem `DEFAULT` passa);
2. V16 falha com `column "status" of relation "pagamento" already exists`.

Se for isso, a cadeia não é reproduzível em ambiente novo — CI com banco limpo, máquina
de um dev novo, ou produção.

Por que ninguém percebeu ainda, provavelmente: `baseline-on-migrate=true`. Em um banco que
já tinha tabelas quando o Flyway foi adotado, o Flyway cria a baseline na versão atual e
**pula** as migrations anteriores a ela. O ambiente de desenvolvimento existente nunca
executou a V10.

E a suíte de testes não cobre isso, porque o perfil de teste desabilita o Flyway (§1).

**Isto não foi verificado contra um PostgreSQL real** — é análise de código. Verificação:

```bash
docker compose down -v          # ⚠️ apaga o volume do banco
docker compose up postgres -d
./gradlew bootRun
```

Se a V16 falhar, a correção é tornar a V16 idempotente (`ADD COLUMN IF NOT EXISTS`) ou
consolidar as duas. **Editar uma migration já aplicada quebra o checksum do Flyway** em
quem já rodou, exigindo `flyway repair` — por isso a decisão precisa ser consciente e não
foi feita por conta própria.

---

## 7. Convenções

**Nomenclatura**

| Objeto | Padrão | Exemplo |
|---|---|---|
| Tabela | `snake_case` singular | `ordem_servico`, `item_os_peca` |
| Coluna | `snake_case` | `valor_com_desconto`, `data_fechamento` |
| PK | `id` (`BIGSERIAL`), ou `pessoa_id` nos subtipos JOINED | |
| FK | `<entidade>_id` | `oficina_id`, `os_id` |
| Constraint FK | `fk_<tabela>_<referência>` | `fk_mao_obra_os` |
| Constraint única | `uq_<tabela>_<colunas>` | `uq_unidade_oficina_endereco` |
| Check | `chk_<tabela>_<coluna>` | `chk_usuario_role` |
| Índice | `idx_<tabela>_<coluna>` | `idx_os_oficina` |

Há inconsistência histórica: `uk_pagamento_os` usa prefixo `uk_` em vez de `uq_`, e
`uq_pessoa__oficina_doc` tem underscore duplo. Não vale corrigir — renomear constraint
exige migration e não traz ganho.

**Tipos**

| Dado | Tipo | Regra |
|---|---|---|
| Dinheiro | `NUMERIC(12,2)` | nunca `float`/`double`; em Java, `BigDecimal` |
| Quantidade de peça | `NUMERIC(12,3)` | aceita fração (0,5 litro de óleo) |
| Data/hora | `TIMESTAMP` | sem timezone; convenção `America/Sao_Paulo` |
| Enum | `VARCHAR(30)` + `CHECK` | persistido como texto, `@Enumerated(EnumType.STRING)` |
| Texto longo | `TEXT` | `obs`, `descricao` |

Enum como texto e não como ordinal é intencional: `ordinal` quebra silenciosamente quando
alguém reordena o enum Java.

---

## 8. Escrevendo uma migration nova

1. Nome: `V{n}__descricao_em_snake_case.sql`, `n` sequencial sem buraco.
2. **Nunca editar migration já aplicada.** O Flyway valida checksum; alterar quebra quem
   já rodou.
3. Se mudar valor de enum persistido, ajustar o `CHECK` correspondente na **mesma**
   migration. Ver V14/V19.
4. Coluna `NOT NULL` em tabela com dados precisa de `DEFAULT`, ou do trio
   `ADD COLUMN nullable` → `UPDATE` → `SET NOT NULL` (padrão usado na V16).
5. Comentar o **porquê**, não o o quê. `ALTER TABLE` já diz o que faz.
6. Evitar `DELETE`/`DROP` de dados. Se for inevitável, dizer no comentário o que se
   perde e por quê.
7. Atualizar a entidade JPA correspondente — `ddl-auto=validate` derruba a aplicação na
   subida se o mapeamento não corresponder.
8. Atualizar a tabela da §4 **deste arquivo**.
9. Testar contra PostgreSQL real, com banco limpo. A suíte de testes não valida
   migration (§1).
