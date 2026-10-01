# Banco de dados

PostgreSQL 16, schema versionado com Flyway. Este documento traz o ER, o que cada
migration fez e as divergências conhecidas entre entidade e schema.

Última verificação contra o código: branch `mvp/fix`, 12 migrations (consolidadas, ver §4).
A execução em PostgreSQL real ainda precisa ser feita em máquina com Docker (ver §6).

---

## 1. Configuração

| Item | Valor | Por quê |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `validate` | o Hibernate **nunca** altera o banco; só confere que o schema atende ao mapeamento |
| `spring.flyway.enabled` | `true` | o schema é versionado em SQL revisável |
| `spring.flyway.baseline-on-migrate` | `false` | evita que um schema não vazio sem histórico do Flyway seja "baselinado" e pule a `V1` |
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

> **Consequência prática:** o SQL das migrations é exercido por `FlywayMigrationsTest`
> (H2 em modo PostgreSQL), mas a validação completa exige um PostgreSQL de verdade. Ver §6.

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

### Tabelas órfãs

Não há mais. `fornecedor`, `nota_compra` e `item_nota_compra` (módulo de compras, fora do MVP)
foram removidas na consolidação das migrations — ver §4.

---

## 3. Chaves e constraints

### Unicidade

| Tabela | Constraint | Escopo | Nota |
|---|---|---|---|
| `oficina` | `uq_oficina_cnpj` | global | CNPJ é identificador nacional |
| `pessoa` | `uq_pessoa__oficina_doc` | `(oficina_id, documento)` | o mesmo CPF pode ser cliente de duas oficinas |
| `veiculo` | `uq_veiculo_placa_oficina` | `(oficina_id, placa)` | idem para veículos |
| `unidade` | `uq_unidade_oficina_endereco` | `(oficina_id, endereco)` | por oficina, nunca global (V2) |
| `usuario` | `uq_usuario_username` | **global** | é credencial de login |
| `pagamento` | `uk_pagamento_os` | `(os_id)` | garante a relação 1:1 com a OS |

Detalhe do PostgreSQL que sustenta o desenho: em índice único, `NULL` **não colide** com
`NULL`. Logo vários registros com `oficina_id` nulo (o ADMIN do SaaS) coexistem sem
disputar unicidade de documento.

### Check

`usuario.chk_usuario_role` → `role IN ('ADMIN', 'GERENTE', 'MECANICO')`.

Precisa ficar sincronizada com o enum `com.oficinapro.enums.Role`.

Os demais enums persistidos como texto também têm `CHECK`: `chk_os_status`
(`StatusOrdemDeServico`), `chk_pagamento_status` (`StatusPagamento`) e
`chk_registro_pagamento_meio` (`MeioPagamento`). Há ainda `chk_pagamento_valor_pago_nao_negativo`,
`chk_registro_pagamento_valor_positivo` e `chk_os_valores_nao_negativos`.

### Estratégia de deleção

`ON DELETE CASCADE` nas relações de composição — apagar a oficina apaga suas unidades,
pessoas e veículos; apagar a OS apaga suas peças, mão de obra e pagamento; apagar o
pagamento apaga seus registros.

As FKs da `ordem_servico` para `cliente`, `veiculo`, `unidade` e `mecanico` **não** têm
`ON DELETE`, ficando no `NO ACTION` padrão. É deliberado: não se apaga um veículo que tem
histórico de OS. A tentativa vira `DataIntegrityViolationException` → `409`.

### Índices

Índices explícitos nas FKs consultadas: `idx_os_oficina`, `idx_os_cliente`, `idx_os_veiculo`,
`idx_os_unidade`, `idx_os_mecanico`, `idx_item_os_peca_os`, `idx_item_os_peca_oficina`,
`idx_mao_obra_os` e `idx_registro_pagamento`. `unidade`, `pessoa`, `veiculo` e `pagamento`
não precisam de índice próprio na FK: as constraints únicas `(oficina_id, …)` e
`uk_pagamento_os` já os fornecem.
---

## 4. Migrations

O projeto ainda não estava em produção, então o histórico incremental (24 migrations, com
correções de desenvolvimento como `V10`/`V16` adicionando a mesma coluna) foi **consolidado**:
hoje existe **uma migration por entidade persistente**, em ordem de dependência, e cada
arquivo descreve o schema atual, não o histórico de alterações.

| # | Arquivo | Tabela | Depende de |
|---|---|---|---|
| V1 | `create_oficina` | `oficina` | — |
| V2 | `create_unidade` | `unidade` | `oficina` |
| V3 | `create_pessoa` | `pessoa` (raiz da herança JOINED) | `oficina` |
| V4 | `create_cliente` | `cliente` | `pessoa` |
| V5 | `create_usuario` | `usuario` (`chk_usuario_role`) | `pessoa` |
| V6 | `create_mecanico` | `mecanico` | `pessoa` |
| V7 | `create_veiculo` | `veiculo` | `oficina` |
| V8 | `create_ordem_servico` | `ordem_servico` (`chk_os_status`, valores não negativos) | `oficina`, `cliente`, `veiculo`, `unidade`, `mecanico` |
| V9 | `create_item_os_peca` | `item_os_peca` | `oficina`, `ordem_servico` |
| V10 | `create_mao_obra` | `mao_obra` | `ordem_servico` |
| V11 | `create_pagamento` | `pagamento` (`uk_pagamento_os`, `chk_pagamento_status`) | `ordem_servico` |
| V12 | `create_registro_pagamento` | `registro_pagamento` (`chk_registro_pagamento_meio`) | `pagamento` |
| V13 | `add_login_lockout_usuario` | `usuario` (`falhas_login`, `bloqueado_ate`, `bloqueio_permanente`) | `usuario` |

`V13` é a primeira migration **incremental** depois da consolidação: bancos que já aplicaram
V1–V12 seguem normalmente, sem recriar o volume.

### O que mudou na consolidação

- **Tabelas de compras removidas.** `fornecedor`, `nota_compra` e `item_nota_compra` não têm
  entidade, repositório, service nem controller, e o módulo de compras está fora do MVP.
  Também saiu a coluna morta `item_os_peca.item_nota_compra_id` (FK para tabela órfã).
- **Correções históricas absorvidas.** `chk_usuario_role` já nasce com `GERENTE`;
  `unidade.endereco` nasce único só por oficina; `pagamento` nasce sem `desconto` (o desconto
  é da OS) e com `status`/`version`; `veiculo.cor`, `oficina.ativo` e `item_os_peca.oficina_id`
  já fazem parte do `CREATE TABLE`.
- **CHECKs novos** alinhados aos enums Java: `chk_os_status`, `chk_pagamento_status`,
  `chk_registro_pagamento_meio`, além de `valor_pago >= 0`, `registro_pagamento.valor > 0` e
  valores da OS não negativos. Ao alterar um enum persistido, altere o `CHECK` junto.
- **Índices redundantes removidos:** `idx_unidade_oficina`, `idx_pessoa_oficina` e
  `idx_veiculo_oficina` — as constraints únicas `(oficina_id, …)` já têm índice com
  `oficina_id` na frente. `idx_pagamento_os` idem (`uk_pagamento_os`). Índices novos:
  `idx_os_mecanico`, `idx_item_os_peca_oficina`, `idx_mao_obra_os`.
- `baseline-on-migrate` passou a `false`: com ele ligado, um schema não vazio sem histórico
  do Flyway seria "baselinado" e a `V1` consolidada seria pulada silenciosamente.

> **Bancos de desenvolvimento existentes precisam ser recriados.** As versões e checksums
> mudaram, então o Flyway recusa o histórico antigo. Use `docker compose -f infra/docker/compose.dev.yml down -v` (apaga o
> volume) e suba de novo.

---

## 5. Divergências entidade ↔ schema

| # | Divergência | Impacto |
|---|---|---|
| E1 | `pessoa.documento` é nullable no banco; DTOs tratam como opcional com `@Size(max=14)` | documento não é obrigatório — decisão de produto a confirmar |
| E2 | `ordem_servico.cliente_id` e `mecanico_id` nullable | OS pode nascer sem cliente e sem mecânico atribuído; é intencional |
| E3 | `ordem_servico.unidade_id` é `NOT NULL` no banco, mas `@JoinColumn` da entidade não declara `nullable = false` | o service sempre informa a unidade (`@NotNull` no DTO); o banco é a barreira final |
| E4 | `item_os_peca.os_id` nullable | peça pode existir sem estar vinculada a uma OS (vincular/desvincular) |
| E5 | `pagamento` é `@ManyToOne` na entidade, mas 1:1 no banco (`uk_pagamento_os`) | a unicidade é garantida pela constraint e por `PagamentoAlreadyExistsException` |

---

## 6. Validação das migrations

Duas camadas:

1. **`FlywayMigrationsTest`** (roda em `gradlew test`, sem Docker): executa os scripts reais, na
   ordem de versão e do zero, em H2 (modo PostgreSQL) — sem o motor do Flyway — e confere versões
   sequenciais sem buracos, o conjunto exato de
   tabelas (sem as órfãs de compras) e constraints que já falharam em produção-like
   (`GERENTE` no `chk_usuario_role`, endereço único por oficina, CNPJ único). Não substitui o
   PostgreSQL real: `ddl-auto=validate` não é exercido nele.
2. **PostgreSQL limpo + `ddl-auto=validate`** — obrigatório antes de entregar:

```bash
docker compose -f infra/docker/compose.dev.yml down -v          # ⚠️ apaga o volume do banco
docker compose -f infra/docker/compose.dev.yml up postgres -d
./gradlew bootRun               # Flyway aplica V1..V12 e o Hibernate valida o schema
```

Se a aplicação subir sem erro de Flyway nem `SchemaManagementException`, o schema bate com
as entidades.

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
| Dinheiro | `NUMERIC(12,0)` | **centavos**, nunca `float`/`double`; em Java, `BigDecimal` com escala 0 |
| Quantidade de peça | `NUMERIC(12,3)` | coluna comporta fração, mas a regra de negócio exige quantidade inteira |
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
   migration.
4. Coluna `NOT NULL` em tabela com dados precisa de `DEFAULT`, ou do trio
   `ADD COLUMN nullable` → `UPDATE` → `SET NOT NULL` (ou recriar o schema, enquanto o projeto não estiver em produção).
5. Comentar o **porquê**, não o o quê. `ALTER TABLE` já diz o que faz.
6. Evitar `DELETE`/`DROP` de dados. Se for inevitável, dizer no comentário o que se
   perde e por quê.
7. Atualizar a entidade JPA correspondente — `ddl-auto=validate` derruba a aplicação na
   subida se o mapeamento não corresponder.
8. Atualizar a tabela da §4 **deste arquivo**.
9. Testar contra PostgreSQL real, com banco limpo (§6), e ajustar
   FlywayMigrationsTest se a lista de tabelas mudar.
