# OficinaPro

**SaaS para gerenciamento de oficinas automotivas**

> Projeto desenvolvido inicialmente para a **Soares Auto Center**, com o objetivo de
> centralizar e facilitar o gerenciamento de clientes, veículos, ordens de serviço,
> mecânicos, peças, mão de obra e pagamentos.

---

## Sobre o projeto

O **OficinaPro** é uma aplicação web para gerenciamento de oficinas automotivas.

O projeto surgiu a partir da identificação de necessidades reais da **Soares Auto
Center**, permitindo transformar processos hoje manuais em um sistema centralizado.

A primeira versão está sendo desenvolvida como **MVP**, focado nas operações essenciais da
oficina. A arquitetura já é multi-tenant: cada oficina só acessa seus próprios dados, e
existe um papel de administrador da plataforma separado dos papéis de oficina.

---

## Objetivos

* Centralizar o cadastro de oficinas e gerenciar unidades por oficina.
* Centralizar o cadastro de clientes e veículos.
* Criar e acompanhar ordens de serviço, associando mecânicos aos serviços.
* Registrar peças e mão de obra nas ordens de serviço.
* Controlar pagamentos e o valor a receber.
* Manter o histórico dos veículos.
* Gerenciar futuramente peças, fornecedores e compras.
* Automatizar futuramente o cadastro de documentos com OCR.
* Utilizar IA para processamento e estruturação de documentos.
* Evoluir para plataforma SaaS multiempresa completa.

---

## Status

**Em desenvolvimento** — construção do MVP.

### Backend

| Módulo | Estado |
|---|---|
| Oficinas | ✅ |
| Unidades | ✅ |
| Clientes | ✅ |
| Veículos | ✅ |
| Mecânicos | ✅ |
| Usuários | ✅ |
| Ordens de serviço (com máquina de estados) | ✅ |
| Itens de OS — peças | ✅ |
| Mão de obra | ✅ |
| Pagamentos e registros de pagamento | ✅ |
| Autenticação JWT | ✅ |
| Autorização por papel | ✅ |
| Isolamento por oficina | ✅ |
| Relatório de fluxo mensal de OS | ✅ |
| Histórico de veículos | ⬜ |
| Auditoria | ⬜ |
| Fornecedores e compras | ⬜ |

348 métodos de teste (unitários de service, `@WebMvcTest` e integração).

### Frontend

11 telas implementadas: Login, Dashboard, Clientes, Veículos, Mecânicos, Peças, Ordens de
Serviço, Pagamentos, Usuários, Unidades e Oficinas.

> ⚠️ **O frontend ainda não está integrado ao backend.** Não existe camada HTTP: as telas
> leem de `frontend/src/mocks/` e o login aceita qualquer credencial. É o maior item em
> aberto do projeto. Detalhes e plano de integração em
> [`docs/frontend.md`](./docs/frontend.md).

---

## Documentação

O detalhe técnico vive em `docs/`. Este README é só a porta de entrada.

| Documento | Conteúdo |
|---|---|
| [`docs/architecture.md`](./docs/architecture.md) | camadas, segurança, modelo de domínio, stack, dívida arquitetural |
| [`docs/permissions.md`](./docs/permissions.md) | **matriz papel × endpoint**, isolamento multi-tenant, hierarquia de usuários |
| [`docs/business-rules.md`](./docs/business-rules.md) | máquina de estados da OS, cálculo de valores, desconto, pagamento |
| [`docs/api.md`](./docs/api.md) | contrato de erro, fluxo de autenticação, paginação, convenções de tipo |
| [`docs/database.md`](./docs/database.md) | ER, constraints, histórico das 19 migrations |
| [`docs/development.md`](./docs/development.md) | como subir, variáveis de ambiente, testes, contribuição |
| [`docs/frontend.md`](./docs/frontend.md) | arquitetura do frontend, componentes genéricos, integração |
| [`frontend/README.md`](./frontend/README.md) | início rápido do frontend |

**A lista de endpoints não está aqui de propósito.** A fonte viva é o OpenAPI, gerado a
partir do código:

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

Listas de rota em markdown envelhecem e passam a mentir — foi o que aconteceu com a versão
anterior deste arquivo.

---

## Início rápido

**Pré-requisitos:** JDK 21, Docker, Node 20+.

```bash
# 1. Configurar o ambiente (obrigatório — o compose falha sem .env)
cp .env-example .env
#    edite JWT_SECRET (mínimo 32 caracteres) e ADMIN_USERNAME/ADMIN_PASSWORD

# 2. Subir o banco
docker compose up postgres -d

# 3. Subir a aplicação
./gradlew bootRun
```

http://localhost:8080 · Swagger em http://localhost:8080/swagger-ui.html

Frontend:

```bash
cd frontend && npm install && npm run dev
```

Duas armadilhas que valem ler antes:

- **`JWT_SECRET` com menos de 32 caracteres derruba a aplicação na subida** (exigência do
  HS256).
- **Sem `ADMIN_USERNAME`/`ADMIN_PASSWORD` a aplicação sobe e você não consegue logar** — o
  `AdminSeeder` só cria o ADMIN inicial se as duas estiverem preenchidas, e sem ADMIN não
  há como criar oficina nem gerente.

Tudo em [`docs/development.md`](./docs/development.md).

---

## Arquitetura em uma tela

```
┌─────────────────────────┐        ┌──────────────────────────────┐
│  frontend               │        │  backend                     │
│  React 19 + TS + Vite   │        │  Spring Boot 4.1 · Java 21   │
│  porta 3000 (nginx)     │╌╌╌╌╌╌╌>│  REST /api/** · JWT HS256    │
│  ⚠️ dados mockados       │ ainda  │  porta 8080                  │
└─────────────────────────┘  não   └──────────────┬───────────────┘
                            ligado                │ JPA + Flyway
                                                  ▼
                                    ┌──────────────────────────────┐
                                    │  PostgreSQL 16 · 19 migrations│
                                    └──────────────────────────────┘
```

Backend em camadas: `controller` → `service` → `repository` → `model`. Regra de negócio só
no service; o controller traduz HTTP e o `model` nunca vaza para fora.

Detalhes em [`docs/architecture.md`](./docs/architecture.md).

---

## Papéis

| Papel | Vínculo com oficina | O que faz |
|---|---|---|
| `ADMIN` | nenhum | administrador do SaaS: gerencia oficinas e contas de usuário |
| `GERENTE` | obrigatório | administra uma oficina: cadastros, OS, financeiro |
| `MECANICO` | obrigatório | executa o serviço: lança peças e mão de obra, move a OS |

> **O ADMIN do SaaS não acessa dado operacional das oficinas.** Ele não deve ler clientes,
> veículos, ordens de serviço ou informação financeira — a razão é ética e contratual, não
> técnica. Os endpoints operacionais não liberam `ADMIN`.
>
> Existem exceções ainda não resolvidas, documentadas abertamente em
> [`docs/permissions.md`](./docs/permissions.md) §2.

Dois pontos de segurança que valem destaque:

- O `oficina_id` usado na autorização **nunca** é confiado ao cliente: é extraído do
  usuário autenticado.
- Acessar registro de outra oficina responde **404, não 403**. Responder "sem permissão"
  confirmaria que o registro existe.

Matriz completa em [`docs/permissions.md`](./docs/permissions.md).

---

## Stack

**Backend** — Java 21, Spring Boot 4.1 (webmvc, data-jpa, security,
oauth2-resource-server, validation, actuator), PostgreSQL 16, Flyway, springdoc-openapi,
Gradle, Spotless (googleJavaFormat), JUnit 5 + Mockito + AssertJ + H2.

**Frontend** — React 19, TypeScript 6, Vite 8, react-router-dom 7, lucide-react, recharts,
ESLint + Prettier + husky. Sem biblioteca de HTTP ainda.

**Infra** — Docker Compose (postgres + app + frontend), GitHub Actions
(`backend-ci.yml`, `frontend-ci.yml`).

---

## Testes

```bash
./gradlew test            # suíte completa
./gradlew build           # compila + testes + spotlessCheck
```

| Tipo | Classes | O que valida |
|---|---|---|
| Unitário de service (Mockito) | 28 | regra de negócio isolada |
| Web (`@WebMvcTest`) | 13 | contrato HTTP, validação de DTO, `403` por papel |
| Integração (`@SpringBootTest`) | 2 | contexto sobe; `401` com a cadeia de filtros real |

> Os testes de controller **não são E2E**: são `@WebMvcTest` com o service mockado, que
> validam a borda HTTP e não o fluxo ponta a ponta. A versão anterior deste README os
> chamava de E2E, o que dava falsa sensação de cobertura.

O que a suíte **não** cobre: migrations (o perfil de teste desliga o Flyway e gera o
schema pelas entidades), comportamento real do PostgreSQL, e o frontend. Ver
[`docs/development.md`](./docs/development.md) §5.

Lembre-se de `./gradlew spotlessApply` antes de commitar — `spotlessCheck` está pendurado
no `build`, e é a causa mais comum de CI vermelho aqui.

---

## Requisitos funcionais

| Código | Requisito | Status |
|---|---|---|
| RF01 | Cadastro de clientes | Implementado |
| RF02 | Consulta de clientes | Implementado |
| RF03 | Cadastro de veículos | Implementado |
| RF04 | Histórico do veículo | Pendente |
| RF05 | Cadastro de ordem de serviço | Implementado |
| RF06 | Registro de avarias | Parcial (campo `obs`) |
| RF07 | Controle de status da OS | Implementado |
| RF08 | Cadastro de mecânicos | Implementado |
| RF09 | Acompanhamento de mecânicos | Parcial |
| RF10 | Cadastro de fornecedores | Pendente |
| RF11 | Cadastro de documentos de compra | Pendente |
| RF12 | Cadastro de itens de compra | Pendente |
| RF13 | Relatórios de compras | Pendente |
| RF14 | Associação entre compras e OS | Pendente |
| RF15 | Controle de pagamentos | **Implementado (backend)** |
| RF16 | Dashboard | **Parcial** — fluxo mensal no backend; tela com dados mockados |
| RF17 | Auditoria | Pendente |

RF10–RF14 têm tabelas criadas no banco (`fornecedor`, `nota_compra`, `item_nota_compra`)
sem entidade nem código. Ver [`docs/database.md`](./docs/database.md) §5.

---

## Roadmap

```text
┌───────────────┐
│ Infraestrutura│  ✅ concluído
│ Spring Boot · PostgreSQL · Flyway · Docker · CI · Testes
└───────┬───────┘
        ▼
┌───────────────┐
│      MVP      │  🔄 em andamento
│ Oficinas ✓ · Unidades ✓ · Clientes ✓ · Veículos ✓ · Mecânicos ✓
│ Usuários ✓ · OS ✓ · Peças ✓ · Mão de obra ✓ · Pagamentos ✓
│ Auth JWT ✓ · Isolamento por oficina ✓
│ ⬜ Integração frontend ↔ backend      ← próximo passo crítico
│ ⬜ Histórico de veículos · Auditoria
└───────┬───────┘
        ▼
┌───────────────┐
│    Compras    │  ⬜ Fornecedores · Catálogo de peças · Notas · Relatórios
└───────┬───────┘
        ▼
┌───────────────┐
│   Automação   │  ⬜ Upload de documentos · OCR · Extração · Validação
└───────┬───────┘
        ▼
┌───────────────┐
│      IA       │  ⬜ LLM · Estruturação · Normalização de peças · Classificação
└───────┬───────┘
        ▼
┌───────────────┐
│     SaaS      │  ⬜ Portal de cadastro · Planos · Assinaturas · Billing
└───────────────┘
```

---

## Princípios de segurança

* Senhas nunca são armazenadas em texto puro — BCrypt.
* Autenticação por JWT HS256, stateless, sem sessão no servidor.
* O segredo do JWT vem de variável de ambiente e tem mínimo de 32 caracteres validado na
  subida.
* Autorização por papel no controller (`@PreAuthorize`) **e** validação de tenant no
  service. Uma camada não substitui a outra: a primeira protege a rota, a segunda protege
  o dado.
* O `oficina_id` da autorização é extraído do usuário autenticado, nunca do payload.
* Registro de outra oficina responde 404, para não confirmar sua existência.
* Mensagens de erro não expõem stacktrace, SQL nem nome de constraint.
* Credenciais de banco não ficam no código-fonte.

---

## Contribuindo

Branches: `main` (estável), `develop` (integração e alvo do CI), demais branches de
trabalho mescladas em `develop`.

Commits seguem Conventional Commits em português, com o corpo explicando **por que** a
mudança foi feita — o diff já mostra o que mudou.

Se você alterar permissão, regra de negócio, schema ou arquitetura, **atualize o documento
correspondente em `docs/` no mesmo commit**. A divergência entre documentação e código foi
o principal achado da auditoria técnica deste projeto: documentação errada é pior que
documentação ausente, porque é seguida.

Checklist de PR em [`docs/development.md`](./docs/development.md) §9.

---

## Autor

**André Tharssys Marques Soares**

Projeto desenvolvido no contexto de aplicação prática de tecnologias de desenvolvimento de
software, utilizando como cenário inicial a **Soares Auto Center**.
