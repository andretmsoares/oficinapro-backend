# OficinaPro

**SaaS para gerenciamento de oficinas automotivas**

> Projeto desenvolvido inicialmente para a **Soares Auto Center**, com o objetivo de centralizar e facilitar o gerenciamento de clientes, veículos, ordens de serviço, mecânicos, peças e pagamentos.

---

## Sobre o projeto

O **OficinaPro** é uma aplicação web para gerenciamento de oficinas automotivas.

O projeto surgiu a partir da identificação de necessidades reais da **Soares Auto Center**, permitindo transformar processos atualmente realizados de forma manual em um sistema centralizado.

A primeira versão está sendo desenvolvida como um **MVP**, focado nas operações essenciais da oficina.

Posteriormente, o projeto poderá evoluir para um **SaaS multiempresa**, permitindo que diferentes oficinas utilizem a plataforma de forma isolada e segura.

---

## Objetivos

O OficinaPro tem como principais objetivos:

* Centralizar o cadastro de oficinas.
* Gerenciar unidades por oficina.
* Centralizar o cadastro de clientes.
* Gerenciar veículos.
* Criar e acompanhar Ordens de Serviço.
* Associar mecânicos aos serviços.
* Registrar peças utilizadas nas ordens de serviço.
* Registrar pagamentos.
* Manter o histórico dos veículos.
* Facilitar o acompanhamento operacional da oficina.
* Gerenciar futuramente peças, fornecedores e compras.
* Automatizar futuramente o cadastro de documentos utilizando OCR.
* Utilizar Inteligência Artificial para processamento e estruturação de documentos.
* Evoluir para uma plataforma SaaS multiempresa.

---

# Status do projeto

**Em desenvolvimento**

Atualmente o projeto está na fase de construção do **MVP**.

### Progresso atual

**Infraestrutura**
* [x] Inicialização do projeto Spring Boot
* [x] Configuração do Gradle
* [x] Configuração do Java 21
* [x] Configuração do PostgreSQL
* [x] Configuração do Flyway (migrations V1 a V9)
* [x] Configuração do Docker
* [x] Docker Compose para desenvolvimento
* [x] Configuração de profiles `dev` e `prod`
* [x] Configuração de variáveis de ambiente
* [x] Configuração do Actuator (health check)
* [x] Configuração inicial do Spring Security (stateless, CORS, CSRF desabilitado)
* [x] Configuração do OpenAPI/Swagger

**Domínios implementados**
* [x] CRUD de Oficinas
* [x] CRUD de Unidades
* [x] CRUD de Clientes (com paginação e busca por nome/documento)
* [x] CRUD de Mecânicos (com paginação, salário e observações)
* [x] CRUD de Usuários (com roles e criptografia de senha)
* [x] CRUD de Veículos (com validação de placa no formato mercosul/antigo)
* [x] CRUD de Ordens de Serviço
* [x] Controle de status da OS (8 status operacionais)
* [x] Atribuição de mecânico à OS
* [x] Atribuição de cliente à OS
* [x] Itens de peça da OS com recálculo automático do valor total
* [x] Isolamento de dados por oficina (multi-tenancy básico)
* [x] Autorização por roles: ADMIN, ADMINISTRATIVO, MECANICO
* [x] Global Exception Handler
* [x] Tratamento de exceções de todos os módulos

**Testes**
* [x] Testes do controller de Oficinas (WebMvcTest + MockMvc)
* [x] Testes unitários do service de Oficinas
* [x] Testes unitários dos services de Clientes, Mecânicos e Usuários
* [x] Testes unitários dos services de Veículos, Unidades
* [x] Testes unitários dos services de Ordens de Serviço e Itens de OS
* [x] Testes E2E (MockMvc) dos controllers de Clientes, Mecânicos
* [x] Testes E2E (MockMvc) dos controllers de Veículos, Unidades, Usuários
* [x] Testes E2E (MockMvc) dos controllers de Ordens de Serviço e Itens de OS

**Pendente**
* [ ] Autenticação JWT completa (login, refresh token)
* [ ] Pagamentos
* [ ] Histórico de veículos
* [ ] Dashboard
* [ ] Fornecedores / Distribuidoras
* [ ] Compras e peças externas
* [ ] OCR e Inteligência Artificial
* [ ] Multi-tenancy completo (SaaS)

---

# Arquitetura

A primeira versão do projeto é desenvolvida utilizando um **monólito modular**.

```text
                       ┌───────────────┐
                       │    Usuário    │
                       └───────┬───────┘
                               │
                               ▼
                       ┌───────────────┐
                       │    React      │
                       │   Frontend    │
                       └───────┬───────┘
                               │
                            REST API
                               │
                               ▼
                ┌─────────────────────────────┐
                │        Spring Boot          │
                │                             │
                │  Autenticação               │
                │  Oficinas                   │
                │  Unidades                   │
                │  Clientes                   │
                │  Veículos                   │
                │  Ordens de Serviço          │
                │  Mecânicos                  │
                │  Itens de OS (Peças)        │
                │  Usuários                   │
                │  Pagamentos (futuro)        │
                │  Compras (futuro)           │
                │  Relatórios (futuro)        │
                └──────────────┬──────────────┘
                               │
                               ▼
                       ┌───────────────┐
                       │  PostgreSQL   │
                       └───────────────┘
```

---

# Tecnologias

## Backend

* **Java 21**
* **Spring Boot 4.1.1**
* **Spring Web MVC**
* **Spring Data JPA**
* **Hibernate**
* **Spring Security**
* **Bean Validation**
* **Gradle 9.5.1**
* **Flyway**
* **Spring Boot Actuator**
* **Lombok**

## Banco de dados

* **PostgreSQL 16**

## Frontend

Planejado:

* **React**
* **TypeScript**
* **Vite**
* **React Router**
* **Axios**

## Infraestrutura

* **Docker**
* **Docker Compose**
* **Git**
* **GitHub**
* **GitHub Actions**

## Testes

* **JUnit 5**
* **Mockito**
* **Spring Boot Test**
* **Spring MVC Test**
* **Spring Security Test**
* **MockMvc**
* **`@WebMvcTest`**
* **`@MockitoBean`**
* **H2 (banco em memória para testes)**

## Documentação

* **OpenAPI**
* **Swagger UI**

---

# Estrutura do projeto

```text
oficinapro/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/oficinapro/
│   │   │       │
│   │   │       ├── config/          # SecurityConfig, OpenApiConfig, SpringDocConfig
│   │   │       ├── controller/      # Controladores REST
│   │   │       ├── dto/             # DTOs de entrada e saída por módulo
│   │   │       ├── enums/           # StatusOrdemDeServico
│   │   │       ├── exception/       # Exceções de domínio + GlobalExceptionHandler
│   │   │       ├── model/           # Entidades JPA
│   │   │       ├── repository/      # Repositórios Spring Data
│   │   │       ├── security/        # AuthenticatedUserProvider, Role, JWT (parcial)
│   │   │       └── service/         # Interfaces e implementações de serviços
│   │   │
│   │   └── resources/
│   │       ├── db/
│   │       │   └── migration/       # Scripts Flyway (V1 a V9)
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   │
│   └── test/
│       ├── java/
│       │   └── com/oficinapro/
│       │       ├── controller/      # Testes E2E com MockMvc
│       │       └── service/         # Testes unitários dos services
│       └── resources/
│           └── application-test.properties  # H2 + Flyway desabilitado
│
├── docker-compose.yml
├── Dockerfile
├── build.gradle
├── settings.gradle
├── .gitignore
├── .env-example
└── README.md
```

---

# Configuração da aplicação

O projeto utiliza **Spring Profiles** para separar configurações comuns, desenvolvimento e produção.

```text
application.yml
       │
       ├───────────────┐
       ▼               ▼
application-dev.yml  application-prod.yml
       │               │
       ▼               ▼
 Desenvolvimento      Produção
```

## `application.yml`

Contém configurações comuns da aplicação:

* Nome da aplicação
* Profile padrão (`dev`)
* Configuração do JPA
* Flyway
* Actuator
* Porta do servidor

## `application-dev.yml`

Utilizado no desenvolvimento local.

As configurações do PostgreSQL são obtidas através de variáveis de ambiente:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
```

## `application-prod.yml`

Utilizado em produção.

As credenciais e informações do banco são obrigatoriamente fornecidas através de variáveis de ambiente:

```text
POSTGRES_HOST
POSTGRES_PORT
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
```

Nenhuma credencial de produção deve ser armazenada diretamente no código-fonte.

---

# Variáveis de ambiente

O projeto utiliza variáveis de ambiente para configuração sensível e específica de cada ambiente.

Exemplo de `.env` para desenvolvimento:

```env
POSTGRES_DB=oficinapro
POSTGRES_USER=oficinapro
POSTGRES_HOST=localhost
POSTGRES_PASSWORD=oficinapro
POSTGRES_PORT=5432
SERVER_PORT=8080
```

O arquivo `.env` **não deve ser versionado**.

O repositório disponibiliza apenas um `.env-example`:

```env
POSTGRES_DB=oficinapro
POSTGRES_USER=oficinapro
POSTGRES_HOST=localhost
POSTGRES_PASSWORD=
POSTGRES_PORT=5432
SERVER_PORT=8080
```

---

# Banco de dados

O banco principal utilizado pelo projeto é o **PostgreSQL**.

A persistência é realizada utilizando:

```text
Spring Data JPA
       ↓
   Hibernate
       ↓
  PostgreSQL
```

As alterações do banco são controladas utilizando **Flyway**.

As migrations ficam em:

```text
src/main/resources/db/migration/
```

Exemplo:

```text
V1__create_oficina_table.sql
V2__create_usuario_table.sql
V3__create_veiculo_table.sql
V4__create_os_table.sql
V5__peca_create_table.sql
V6__pagamento_table.sql
V7__create_fornecedor_table.sql
V8__create_notas_compra_table.sql
V9__create_indexes.sql
```

O Hibernate é utilizado apenas para validar o schema:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

---

# Docker

O projeto possui ambientes Docker separados para desenvolvimento e testes.

## Desenvolvimento

O PostgreSQL de desenvolvimento é executado através do:

```text
docker-compose.yml
```

---

# Executando o projeto

## Pré-requisitos

Antes de executar o projeto, tenha instalado:

* Java 21+
* Git
* Docker
* Docker Compose

O **Gradle Wrapper** (`gradlew`) é utilizado pelo projeto, portanto não é necessário instalar o Gradle globalmente.

---

## 1. Clonar o repositório

```powershell
git clone <URL_DO_REPOSITORIO>
cd projeto-oficinaPro
```

---

## 2. Configurar o ambiente

Crie o `.env` a partir do exemplo:

```powershell
Copy-Item .env-example .env
```

Configure as variáveis:

```env
POSTGRES_DB=oficinapro
POSTGRES_USER=oficinapro
POSTGRES_HOST=localhost
POSTGRES_PASSWORD=oficinapro
POSTGRES_PORT=5432
SERVER_PORT=8080
```

---

## 3. Iniciar o PostgreSQL

```powershell
docker compose up -d
```

Verifique os containers:

```powershell
docker compose ps
```

---

## 4. Executar a aplicação

```powershell
.\gradlew bootRun
```

A aplicação estará disponível em:

```text
localhost:8080
```

---

# Profiles

O profile pode ser alterado explicitamente.

## Desenvolvimento

```powershell
.\gradlew bootRun --args="--spring.profiles.active=dev"
```

## Produção

```powershell
.\gradlew bootRun --args="--spring.profiles.active=prod"
```

Ou utilizando variável de ambiente:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
.\gradlew bootRun
```

---

# Testes

O projeto utiliza testes automatizados com JUnit 5, Mockito e Spring Boot Test.

## Tipos de testes

### Testes unitários (service layer)

Utilizam `@ExtendWith(MockitoExtension.class)` com `@Mock` e `@InjectMocks`.

Não carregam contexto Spring — são rápidos e isolados.

Cobrem as regras de negócio dos services:

* Retorno correto dos dados
* Lançamento de exceções (não encontrado, já existe, acesso negado)
* Isolamento de dados por oficina
* Validação de roles para operações sensíveis

### Testes E2E de controller (camada web)

Utilizam `@WebMvcTest` com `MockMvc`, `@MockitoBean` e `@WithMockUser`:

```java
@WebMvcTest(ClienteController.class)
@ActiveProfiles("test")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;
}
```

Os testes de endpoints validam:

* Status HTTP correto (200, 201, 204, 400, 403, 404, 409)
* Corpo da resposta JSON
* Criação de recursos
* Atualização de recursos
* Exclusão de recursos
* Tratamento de recursos inexistentes
* Autorização por role (403 para roles sem permissão)

## Executar os testes

### Windows

```powershell
.\gradlew test
```

### Linux/macOS

```bash
./gradlew test
```

### Com relatório

```powershell
.\gradlew test --info
```

Os resultados ficam em:

```text
build/reports/tests/test/
build/test-results/test/
```

---

# Documentação da API

A API é documentada utilizando **OpenAPI/Swagger**.

Após iniciar o backend:

```text
http://localhost:8080/swagger-ui/index.html
```

A especificação OpenAPI está disponível em:

```text
http://localhost:8080/v3/api-docs
```

---

# Módulos implementados

## Oficinas (`/api/oficinas`)

Gerencia o cadastro de oficinas automotivas.

Endpoints:

```text
GET    /api/oficinas
GET    /api/oficinas/{id}
POST   /api/oficinas
PUT    /api/oficinas/{id}
DELETE /api/oficinas/{id}
```

DTOs: `OficinaRequestDTO`, `OficinaResponseDTO`

Acesso restrito a: `ADMIN`

---

## Unidades (`/api/unidades`)

Gerencia unidades (filiais) de cada oficina.

Endpoints:

```text
GET    /api/unidades
GET    /api/unidades/{id}
GET    /api/unidades/oficina/{oficinaId}
POST   /api/unidades/oficina/{oficinaId}
PUT    /api/unidades/{id}
DELETE /api/unidades/{id}
```

DTOs: `UnidadeRequestDTO`, `UnidadeResponseDTO`

Acesso: `ADMIN`, `ADMINISTRATIVO`

---

## Clientes (`/api/clientes`)

Gerencia o cadastro de clientes.

Endpoints:

```text
GET    /api/clientes                         (ADMIN)
GET    /api/clientes/{id}
GET    /api/clientes/oficina/{oficinaId}
GET    /api/clientes/nome/{nome}
GET    /api/clientes/documento/{documento}
POST   /api/clientes
PUT    /api/clientes/{id}
DELETE /api/clientes/{id}
```

DTOs: `ClienteRequestDTO`, `ClienteResponseDTO`

Suporta paginação com `Pageable`.

Acesso: `ADMIN`, `ADMINISTRATIVO`

---

## Mecânicos (`/api/mecanicos`)

Gerencia o cadastro de mecânicos com suporte a salário e observações.

Endpoints:

```text
GET    /api/mecanicos                         (ADMIN)
GET    /api/mecanicos/{id}
GET    /api/mecanicos/oficina/{oficinaId}
GET    /api/mecanicos/nome/{nome}
GET    /api/mecanicos/documento/{documento}
POST   /api/mecanicos
PUT    /api/mecanicos/{id}
DELETE /api/mecanicos/{id}
```

DTOs: `MecanicoRequestDTO`, `MecanicoResponseDTO`

Suporta paginação com `Pageable`.

Acesso: `ADMIN`, `ADMINISTRATIVO`

---

## Usuários (`/api/usuarios`)

Gerencia os usuários do sistema.

Endpoints:

```text
GET    /api/usuarios                         (ADMIN)
GET    /api/usuarios/{id}
GET    /api/usuarios/oficina/{oficinaId}
GET    /api/usuarios/nome/{nome}             (ADMIN)
GET    /api/usuarios/documento/{documento}   (ADMIN)
POST   /api/usuarios
PUT    /api/usuarios/{id}
DELETE /api/usuarios/{id}                    (ADMIN)
```

DTOs: `UsuarioRequestDTO`, `UsuarioResponseDTO`, `UsuarioUpdateRequestDTO`

Suporta paginação com `Pageable`.

Regras de role:
* Apenas `ADMIN` pode criar ou promover usuários com role `ADMIN`.
* `ADMINISTRATIVO` pode criar usuários com role `ADMINISTRATIVO` ou `MECANICO`.

---

## Veículos (`/api/veiculos`)

Gerencia o cadastro de veículos.

Endpoints:

```text
GET    /api/veiculos
GET    /api/veiculos/{id}
GET    /api/veiculos/placa/{placa}
GET    /api/veiculos/oficina/{oficinaId}
POST   /api/veiculos
PUT    /api/veiculos/{id}
DELETE /api/veiculos/{id}
```

DTOs: `VeiculoRequestDTO`, `VeiculoResponseDTO`

Suporta paginação com `Pageable`.

A placa é validada com regex para o formato antigo (`ABC1234`) e Mercosul (`ABC1D23`).

Acesso leitura: `ADMIN`, `ADMINISTRATIVO`, `MECANICO`

Acesso escrita: `ADMIN`, `ADMINISTRATIVO`

---

## Ordens de Serviço (`/api/ordens-servico`)

Gerencia o ciclo de vida completo das ordens de serviço.

Endpoints:

```text
GET    /api/ordens-servico
GET    /api/ordens-servico/{id}
GET    /api/ordens-servico/veiculo/{veiculoId}
GET    /api/ordens-servico/mecanico/{mecanicoId}
GET    /api/ordens-servico/unidade/{unidadeId}
GET    /api/ordens-servico/oficina/{oficinaId}
GET    /api/ordens-servico/cliente/{clienteId}
POST   /api/ordens-servico
PUT    /api/ordens-servico/{id}
DELETE /api/ordens-servico/{id}
PATCH  /api/ordens-servico/{id}/status
PATCH  /api/ordens-servico/{id}/mecanico
PATCH  /api/ordens-servico/{id}/cliente
```

DTOs: `OrdemDeServicoRequestDTO`, `OrdemDeServicoResponseDTO`, `AtualizarStatusOSRequestDTO`, `AtribuirMecanicoRequestDTO`, `AtribuirClienteRequestDTO`

Status da OS:

```text
ABERTA → DIAGNOSTICO → AGUARDANDO_APROVACAO → AGUARDANDO_PECAS → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                                                                           ↘ CANCELADA
```

Regras de negócio:
* O cliente pode ser nulo na criação (OS sem proprietário identificado).
* Não é possível alterar o status de uma OS cancelada.
* Não é possível mover uma OS entregue de volta para aberta.
* Não é possível trocar a oficina de uma OS após a criação.

Acesso leitura: `ADMIN`, `ADMINISTRATIVO`, `MECANICO`

Acesso escrita: `ADMIN`, `ADMINISTRATIVO`

---

## Itens de OS — Peças (`/api/itens-os-peca`)

Gerencia as peças associadas a uma ordem de serviço.

Endpoints:

```text
GET    /api/itens-os-peca/os/{osId}
GET    /api/itens-os-peca/{id}
POST   /api/itens-os-peca
PUT    /api/itens-os-peca/{id}
DELETE /api/itens-os-peca/{id}
```

DTOs: `ItemOsPecaRequestDTO`, `ItemOsPecaResponseDTO`, `ItemOsPecaUpdateRequestDTO`

Regras de negócio:
* O `valorTotal` do item é calculado automaticamente: `quantidade × valorUnitario`.
* O `valorTotal` da OS é recalculado automaticamente a cada inserção, atualização ou remoção de item.
* Não é possível adicionar ou modificar itens em uma OS cancelada ou entregue.

Acesso: `ADMIN`, `ADMINISTRATIVO`, `MECANICO`

---

# Autenticação e autorização

O OficinaPro utiliza **Spring Security** para controle de acesso.

A arquitetura de segurança é **stateless** (sem sessão).

## Roles

| Role             | Descrição                                   |
| ---------------- | ------------------------------------------- |
| `ADMIN`          | Administrador da plataforma                 |
| `ADMINISTRATIVO` | Gerencia operações da sua oficina           |
| `MECANICO`       | Executa operações relacionadas aos serviços |

O `ADMIN` possui acesso irrestrito à plataforma.

Os demais usuários (`ADMINISTRATIVO`, `MECANICO`) são vinculados a uma oficina específica e só podem acessar dados da própria oficina.

## Isolamento por oficina

O sistema aplica isolamento de dados em nível de serviço:

* Ao listar recursos, usuários não-ADMIN recebem apenas registros da sua própria oficina.
* Ao acessar um recurso específico, se o registro pertencer a outra oficina, o sistema retorna `404 Not Found` (sem revelar a existência do registro).
* Não é possível criar ou mover registros para uma oficina diferente da do usuário autenticado.

## Endpoints públicos

```text
POST /api/auth/**
GET  /actuator/health
GET  /swagger-ui/**
GET  /v3/api-docs/**
```

## Autenticação JWT (planejado)

```text
POST /api/auth/login
  username + password
       ↓
  UserDetailsService
       ↓
  BCrypt (verificação)
       ↓
  JwtService
       ↓
  Token JWT
```

O JWT ainda não está totalmente implementado. Atualmente a camada de autenticação é parcial.

---

# Health Check

O projeto utiliza **Spring Boot Actuator** para monitoramento básico.

Endpoint:

```text
GET /actuator/health
http://localhost:8080/actuator/health
```

---

# Módulos

## MVP

* [x] Oficinas
* [x] Unidades
* [x] Clientes
* [x] Veículos
* [x] Mecânicos
* [x] Usuários
* [x] Ordens de Serviço
* [x] Itens de OS (Peças)
* [x] Isolamento de dados por oficina
* [x] Autorização por roles
* [ ] Autenticação JWT completa
* [ ] Pagamentos
* [ ] Histórico de veículos
* [ ] Dashboard

## Gestão de compras

* [ ] Fornecedores
* [ ] Peças (catálogo)
* [ ] Documentos de compra
* [ ] Itens de compra
* [ ] Associação entre peças e OS
* [ ] Relatórios de compras

## Automação

* [ ] Upload de documentos
* [ ] OCR
* [ ] Extração de dados
* [ ] Validação dos dados extraídos
* [ ] Cadastro automático

## Inteligência Artificial

* [ ] Processamento do texto extraído
* [ ] Estruturação utilizando LLM
* [ ] Normalização de peças
* [ ] Classificação de documentos

## SaaS

* [ ] Multi-tenancy completo
* [ ] Cadastro de oficinas via portal
* [ ] Isolamento de dados avançado
* [ ] Planos
* [ ] Assinaturas
* [ ] Billing

---

# Roadmap

```text
                   OficinaPro
                       │
                       ▼
               ┌───────────────┐
               │ Infraestrutura│  ← concluído
               │               │
               │ Spring Boot   │
               │ PostgreSQL    │
               │ Flyway        │
               │ Docker        │
               │ Profiles      │
               │ Testes        │
               └───────┬───────┘
                       │
                       ▼
               ┌───────────────┐
               │      MVP      │  ← em andamento
               │               │
               │ Oficinas   ✓  │
               │ Unidades   ✓  │
               │ Clientes   ✓  │
               │ Veículos   ✓  │
               │ Mecânicos  ✓  │
               │ OS         ✓  │
               │ Itens OS   ✓  │
               │ Auth JWT   ~  │
               │ Pagamentos    │
               └───────┬───────┘
                       │
                       ▼
               ┌───────────────┐
               │    Gestão     │
               │               │
               │ Peças         │
               │ Fornecedores  │
               │ Compras       │
               │ Relatórios    │
               └───────┬───────┘
                       │
                       ▼
               ┌───────────────┐
               │  Automação    │
               │               │
               │ OCR           │
               │ Documentos    │
               └───────┬───────┘
                       │
                       ▼
               ┌───────────────┐
               │      IA       │
               │               │
               │ LLM           │
               │ Extração      │
               │ Classificação │
               └───────┬───────┘
                       │
                       ▼
               ┌───────────────┐
               │     SaaS      │
               │               │
               │ Multi-tenant  │
               │ Planos        │
               │ Billing       │
               └───────────────┘
```

---

# Requisitos principais

## Requisitos funcionais

| Código | Requisito                        | Status        |
| ------ | -------------------------------- | ------------- |
| RF01   | Cadastro de clientes             | Implementado  |
| RF02   | Consulta de clientes             | Implementado  |
| RF03   | Cadastro de veículos             | Implementado  |
| RF04   | Histórico do veículo             | Pendente      |
| RF05   | Cadastro de Ordem de Serviço     | Implementado  |
| RF06   | Registro de avarias              | Parcial (obs) |
| RF07   | Controle de status da OS         | Implementado  |
| RF08   | Cadastro de mecânicos            | Implementado  |
| RF09   | Acompanhamento de mecânicos      | Parcial       |
| RF10   | Cadastro de fornecedores         | Pendente      |
| RF11   | Cadastro de documentos de compra | Pendente      |
| RF12   | Cadastro de itens de compra      | Pendente      |
| RF13   | Relatórios de compras            | Pendente      |
| RF14   | Associação entre compras e OS    | Pendente      |
| RF15   | Controle de pagamentos           | Pendente      |
| RF16   | Dashboard                        | Pendente      |
| RF17   | Auditoria                        | Pendente      |

---

# Estratégia de testes

Os testes são organizados em dois grupos:

## Testes unitários (service layer)

```text
@ExtendWith(MockitoExtension.class)
         │
         ▼
  @Mock dependencies
         │
         ▼
  @InjectMocks service
         │
         ▼
  Testa regras de negócio
  sem contexto Spring
```

Cobrem os seguintes cenários:
* Operações CRUD corretas
* Exceções de recurso não encontrado
* Exceções de conflito (duplicidade de dados)
* Controle de acesso por oficina
* Regras específicas de domínio (ex: OS cancelada, recálculo de valor total)

## Testes E2E de controller

```text
@WebMvcTest(XxxController.class)
         │
         ▼
  MockMvc + @WithMockUser
         │
         ▼
  @MockitoBean XxxService
         │
         ▼
  Testa HTTP: status, body,
  autorização por role
```

Cobrem os seguintes cenários:
* Retorno correto de status HTTP (200, 201, 204, 400, 403, 404, 409)
* Corpo da resposta JSON
* Proteção por role (`403 Forbidden` para roles sem permissão)

---

# Princípios de segurança

* Senhas nunca são armazenadas em texto puro.
* Senhas são protegidas utilizando BCrypt.
* A autenticação utilizará JWT (em implementação).
* A API é stateless.
* A autorização utiliza roles.
* O `ADMIN` possui acesso à plataforma como um todo.
* `ADMINISTRATIVO` e `MECANICO` são vinculados a uma oficina.
* Usuários de uma oficina não podem acessar dados de outras oficinas.
* O `oficina_id` utilizado para autorização não é confiado ao cliente — é extraído do usuário autenticado.
* Credenciais de banco não são armazenadas no código-fonte.

---

# Autor

**André Tharssys Marques Soares**

Projeto desenvolvido no contexto de aplicação prática de tecnologias de desenvolvimento de software, utilizando como cenário inicial a **Soares Auto Center**.
