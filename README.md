# OficinaPro

**SaaS para gerenciamento de oficinas automotivas**

> Projeto desenvolvido inicialmente para a **Soares Auto Center**, com o objetivo de centralizar e facilitar o gerenciamento de clientes, veículos, ordens de serviço, mecânicos, pagamentos e, futuramente, compras e documentos de peças.

---

## 📋 Sobre o projeto

O **OficinaPro** é uma aplicação web para gerenciamento de oficinas automotivas.

O projeto surgiu a partir da identificação de necessidades reais da **Soares Auto Center**, permitindo transformar processos atualmente realizados de forma manual em um sistema centralizado.

A primeira versão está sendo desenvolvida como um **MVP**, focado nas operações essenciais da oficina.

Posteriormente, o projeto poderá evoluir para um **SaaS multiempresa**, permitindo que diferentes oficinas utilizem a plataforma de forma isolada e segura.

---

## 🎯 Objetivos

O OficinaPro tem como principais objetivos:

* Centralizar o cadastro de oficinas.
* Centralizar o cadastro de clientes.
* Gerenciar veículos.
* Criar e acompanhar Ordens de Serviço.
* Associar mecânicos aos serviços.
* Registrar pagamentos.
* Manter o histórico dos veículos.
* Facilitar o acompanhamento operacional da oficina.
* Gerenciar futuramente peças, fornecedores e compras.
* Automatizar futuramente o cadastro de documentos utilizando OCR.
* Utilizar Inteligência Artificial para processamento e estruturação de documentos.
* Evoluir para uma plataforma SaaS multiempresa.

---

# 🚧 Status do projeto

**Em desenvolvimento 🚧**

Atualmente o projeto está na fase de construção do **MVP**.

### Progresso atual

* [x] Inicialização do projeto Spring Boot
* [x] Configuração do Gradle
* [x] Configuração do Java 21
* [x] Configuração do PostgreSQL
* [x] Configuração do Flyway
* [x] Configuração do Docker
* [x] Docker Compose para desenvolvimento
* [x] Docker Compose para testes
* [x] Configuração de profiles `dev` e `prod`
* [x] Configuração de variáveis de ambiente
* [x] Configuração do Actuator
* [x] Configuração inicial do Spring Security
* [x] Configuração do OpenAPI/Swagger
* [x] CRUD de oficinas
* [x] DTOs de oficinas
* [x] Tratamento de exceções de oficinas
* [x] Testes do controller de oficinas
* [ ] Correção de todas as falhas da suíte de testes
* [ ] Autenticação completa
* [ ] Cadastro de clientes
* [ ] Cadastro de veículos
* [ ] Cadastro de mecânicos
* [ ] Ordens de Serviço
* [ ] Pagamentos
* [ ] Histórico de veículos
* [ ] Dashboard
* [ ] Fornecedores
* [ ] Compras e peças
* [ ] OCR
* [ ] Inteligência Artificial
* [ ] Multi-tenancy
* [ ] SaaS

---

# 🏗️ Arquitetura

A primeira versão do projeto será desenvolvida utilizando um **monólito modular**.

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
                 │  Clientes                   │
                 │  Veículos                   │
                 │  Ordens de Serviço          │
                 │  Mecânicos                  │
                 │  Pagamentos                 │
                 │  Compras                    │
                 │  Relatórios                 │
                 └──────────────┬──────────────┘
                                │
                                ▼
                        ┌───────────────┐
                        │  PostgreSQL   │
                        └───────────────┘
```

A arquitetura poderá evoluir futuramente para serviços independentes caso exista necessidade.

---

# 🛠️ Tecnologias

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

## Documentação

* **OpenAPI**
* **Swagger UI**

---

# 📁 Estrutura do projeto

A estrutura do backend segue uma organização por responsabilidades:

```text
oficinapro/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/oficinapro/
│   │   │       │
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── enums/
│   │   │       ├── exception/
│   │   │       ├── mapper/
│   │   │       ├── repository/
│   │   │       ├── security/
│   │   │       └── service/
│   │   │
│   │   └── resources/
│   │       ├── db/
│   │       │   └── migration/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   │
│   └── test/
│       └── java/
│           └── com/oficinapro/
│
├── docker-compose.yml
├── docker-compose.test.yml
├── Dockerfile
├── dockerfile-test
├── build.gradle
├── settings.gradle
├── .gitignore
├── .env.example
└── README.md
```

---

# ⚙️ Configuração da aplicação

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
* Profile padrão
* Configuração do JPA
* Flyway
* Actuator
* Porta do servidor

O profile padrão é:

```yaml
spring:
  profiles:
    default: dev
```

Portanto, ao executar a aplicação sem especificar outro profile, o ambiente `dev` será utilizado.

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

O ambiente de desenvolvimento utiliza logs mais detalhados para facilitar o diagnóstico da aplicação.

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

# 🔐 Variáveis de ambiente

O projeto utiliza variáveis de ambiente para configuração sensível e específica de cada ambiente.

Exemplo de `.env` para desenvolvimento:

```env
POSTGRES_DB=oficinapro
POSTGRES_USER=oficinapro
POSTGRES_HOST=localhost
POSTGRES_PASSWORD=oficinapro
POSTGRES_PORT=5433
SERVER_PORT=8080
```

O arquivo `.env` **não deve ser versionado**.

O repositório deve disponibilizar apenas um `.env.example`:

```env
POSTGRES_DB=oficinapro
POSTGRES_USER=oficinapro
POSTGRES_HOST=localhost
POSTGRES_PASSWORD=
POSTGRES_PORT=5433
SERVER_PORT=8080
```

---

# 🗄️ Banco de dados

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
V2__create_unidade_table.sql
V3__create_cliente_table.sql
```

O Hibernate é utilizado apenas para validar o schema:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

O projeto não utiliza o Hibernate para criação automática das tabelas.

---

# 🐳 Docker

O projeto possui ambientes Docker separados para desenvolvimento e testes.

## Desenvolvimento

O PostgreSQL de desenvolvimento é executado através do:

```text
docker-compose.yml
```

A aplicação local acessa o PostgreSQL através da porta:

```text
5433
```

Fluxo:

```text
Spring Boot
    │
    │ localhost:5433
    ▼
PostgreSQL Docker
```

## Testes

Os testes possuem um ambiente Docker isolado através de:

```text
docker-compose.test.yml
```

O ambiente possui:

```text
postgres-test
      │
      ▼
PostgreSQL 16
      │
      │
      ▼
app-tests
      │
      ▼
Gradle Test
```

O banco de testes utiliza:

```text
oficinapro_test
```

e não interfere no banco utilizado pelo ambiente de desenvolvimento.

---

# 🚀 Executando o projeto

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
Copy-Item .env.example .env
```

Configure as variáveis:

```env
POSTGRES_DB=oficinapro
POSTGRES_USER=oficinapro
POSTGRES_HOST=localhost
POSTGRES_PASSWORD=oficinapro
POSTGRES_PORT=5433
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

## 4. Executar o backend

### Windows

```powershell
.\gradlew bootRun
```

### Linux/macOS

```bash
./gradlew bootRun
```

Como o profile padrão é `dev`, a aplicação utilizará:

```text
application.yml
       +
application-dev.yml
```

A aplicação estará disponível em:

```text
http://localhost:8080
```

---

# 🔄 Profiles

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

# 🧪 Testes

O projeto utiliza testes automatizados com JUnit, Mockito e Spring Boot Test.

Os testes de controller utilizam:

```java
@WebMvcTest(OficinaController.class)
```

e os serviços são isolados utilizando:

```java
@MockitoBean
private OficinaService oficinaService;
```

Os testes de endpoints utilizam `MockMvc`, incluindo validação de:

* Status HTTP
* JSON de resposta
* Criação de recursos
* Atualização de recursos
* Exclusão de recursos
* Tratamento de recursos inexistentes
* Autorização por role
* Proteção CSRF

Exemplo de teste de autorização:

```text
USER
  │
  └── GET /api/oficinas
            │
            ▼
         HTTP 403
```

Enquanto um usuário com:

```text
ROLE_ADMIN
```

deve possuir acesso ao recurso.

---

## Executar os testes localmente

### Windows

```powershell
.\gradlew test
```

### Linux/macOS

```bash
./gradlew test
```

---

## Executar os testes no Docker

O ambiente de testes utiliza um Compose separado:

```powershell
docker compose -f docker-compose.test.yml up --build --abort-on-container-exit --exit-code-from app-tests
```

Para executar apenas os testes do controller de oficinas:

```powershell
docker compose -f docker-compose.test.yml run --rm app-tests ./gradlew test --tests "com.oficinapro.controller.OficinaControllerTest"
```

Os relatórios são gerados em:

```text
build/reports/tests/test/
```

e:

```text
build/test-results/test/
```

---

# 📚 Documentação da API

A API será documentada utilizando **OpenAPI/Swagger**.

Após iniciar o backend:

```text
http://localhost:8080/swagger-ui/index.html
```

A documentação permite visualizar e testar os endpoints diretamente pelo navegador.

A especificação OpenAPI está disponível em:

```text
http://localhost:8080/v3/api-docs
```

---

# 🏢 Gerenciamento de Oficinas

O módulo de oficinas já possui a estrutura inicial de CRUD.

Endpoints:

```text
GET    /api/oficinas
GET    /api/oficinas/{id}
POST   /api/oficinas
PUT    /api/oficinas/{id}
DELETE /api/oficinas/{id}
```

A camada é organizada da seguinte forma:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
    ↓
PostgreSQL
```

Os dados de entrada e saída são representados através de DTOs:

```text
OficinaRequest
OficinaResponse
```

Exceções específicas também são utilizadas para tratar situações como oficina não encontrada.

---

# 🔐 Autenticação e autorização

O OficinaPro utiliza **Spring Security** para controle de acesso.

A arquitetura de segurança foi projetada para utilizar autenticação baseada em **JWT (JSON Web Token)** e autorização baseada em **roles**.

A implementação está sendo desenvolvida incrementalmente.

## 👥 Roles

| Role             | Descrição                                   |
| ---------------- | ------------------------------------------- |
| `ADMIN`          | Administrador da plataforma SaaS            |
| `GERENTE`        | Gerencia operações da sua oficina           |
| `ADMINISTRATIVO` | Executa operações administrativas           |
| `MECANICO`       | Executa operações relacionadas aos serviços |

O `ADMIN` representa um administrador da plataforma.

Os demais usuários são vinculados a uma oficina específica através de `oficina_id`.

---

## 🔑 Fluxo de autenticação planejado

```text
┌─────────────────┐
│     Cliente     │
│   Frontend/API  │
└────────┬────────┘
         │
         │ POST /api/auth/login
         │ username + password
         ▼
┌─────────────────────────┐
│   AuthenticationService │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│      UserDetailsService │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│       PostgreSQL        │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│     PasswordEncoder     │
│         BCrypt          │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│       JwtService        │
└────────────┬────────────┘
             │
             ▼
┌─────────────────┐
│     Cliente     │
└─────────────────┘
```

---

# 🛡️ Autorização

A autenticação identifica o usuário.

A autorização determina o que ele pode fazer.

Exemplo:

```java
@PreAuthorize("hasRole('ADMIN')")
public Oficina criar(OficinaRequest dto) {
    // ...
}
```

Para múltiplas roles:

```java
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
```

---

# 🏢 Isolamento entre oficinas

O projeto será preparado para isolamento de dados entre diferentes oficinas.

Um usuário pertencente a uma oficina deverá acessar apenas dados associados à sua própria oficina.

Exemplo:

```text
Oficina 1
├── João       (GERENTE)
├── Maria      (ADMINISTRATIVO)
└── Carlos     (MECANICO)


Oficina 2
├── Pedro      (GERENTE)
└── Ana        (MECANICO)
```

João possui:

```text
role = GERENTE
oficina_id = 1
```

Uma consulta de clientes deverá considerar a oficina do usuário autenticado:

```sql
WHERE oficina_id = 1
```

e não confiar em uma oficina arbitrariamente informada pelo cliente.

---

# 🌐 Endpoints públicos

A configuração de segurança prevê inicialmente os seguintes endpoints públicos:

```text
POST /api/auth/**
GET  /actuator/health
GET  /swagger-ui/**
GET  /v3/api-docs/**
```

Os demais endpoints deverão exigir autenticação conforme as regras de segurança.

---

# ❤️ Health Check

O projeto utiliza **Spring Boot Actuator** para monitoramento básico da aplicação.

Endpoint:

```text
GET /actuator/health
```

Exemplo:

```text
http://localhost:8080/actuator/health
```

Esse endpoint será utilizado também para verificar a disponibilidade da aplicação em ambientes Docker e futuramente em infraestrutura de produção.

---

# 🔄 Fluxo principal

O fluxo principal esperado para o MVP será:

```text
Cliente
   ↓
Veículo
   ↓
Ordem de Serviço
   ↓
Mecânico
   ↓
Execução do serviço
   ↓
Finalização
   ↓
Pagamento
   ↓
Histórico do veículo
```

---

# 📦 Módulos

## MVP

* [x] Oficinas
* [ ] Autenticação
* [ ] Usuários
* [ ] Clientes
* [ ] Veículos
* [ ] Mecânicos
* [ ] Ordens de Serviço
* [ ] Pagamentos
* [ ] Histórico
* [ ] Dashboard

## Gestão de compras

* [ ] Fornecedores
* [ ] Peças
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

* [ ] Multi-tenancy
* [ ] Cadastro de oficinas
* [ ] Isolamento de dados
* [ ] Planos
* [ ] Assinaturas
* [ ] Billing

---

# 🗺️ Roadmap

```text
                    OficinaPro
                        │
                        ▼
                ┌───────────────┐
                │ Infraestrutura│
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
                │      MVP      │
                │               │
                │ Oficinas      │
                │ Clientes      │
                │ Veículos      │
                │ OS            │
                │ Mecânicos     │
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

# 📋 Requisitos principais

## Requisitos funcionais

| Código | Requisito                        |
| ------ | -------------------------------- |
| RF01   | Cadastro de clientes             |
| RF02   | Consulta de clientes             |
| RF03   | Cadastro de veículos             |
| RF04   | Histórico do veículo             |
| RF05   | Cadastro de Ordem de Serviço     |
| RF06   | Registro de avarias              |
| RF07   | Controle de status da OS         |
| RF08   | Cadastro de mecânicos            |
| RF09   | Acompanhamento de mecânicos      |
| RF10   | Cadastro de fornecedores         |
| RF11   | Cadastro de documentos de compra |
| RF12   | Cadastro de itens de compra      |
| RF13   | Relatórios de compras            |
| RF14   | Associação entre compras e OS    |
| RF15   | Controle de pagamentos           |
| RF16   | Dashboard                        |
| RF17   | Auditoria                        |

---

# 🧪 Estratégia de testes

Os testes são executados em um ambiente Docker separado do ambiente de desenvolvimento.

```text
                 docker-compose.test.yml
                          │
              ┌───────────┴───────────┐
              │                       │
              ▼                       ▼
       postgres-test              app-tests
              │                       │
              │                       ▼
              │                    Gradle
              │                       │
              │                       ▼
              │                    JUnit 5
              │                       │
              └───────────────────────┘
```

Os testes de controller utilizam `MockMvc` e isolam os serviços com Mockito.

A suíte também possui testes relacionados à segurança, incluindo validação de acesso por roles.

Os resultados dos testes são armazenados em:

```text
build/reports/tests/test/
build/test-results/test/
```

---

# 🤖 OCR

A automação de documentos será implementada somente após o funcionamento do cadastro manual de compras.

O fluxo planejado será:

```text
Foto/PDF
   ↓
OCR
   ↓
Texto
   ↓
Extração
   ↓
Validação pelo usuário
   ↓
Banco de dados
```

O OCR deverá identificar informações como:

* Fornecedor
* Número do documento
* Número do pedido
* Data
* Peças
* Quantidade
* Valores

Posteriormente, uma LLM poderá ser utilizada para melhorar a interpretação e estruturação dos dados.

---

# 🏢 Modelo SaaS

Embora a primeira versão seja destinada à **Soares Auto Center**, o sistema será projetado considerando uma futura arquitetura multiempresa.

```text
Oficina A
 ├── Clientes
 ├── Veículos
 ├── OS
 └── Compras

Oficina B
 ├── Clientes
 ├── Veículos
 ├── OS
 └── Compras
```

Os dados de uma oficina deverão permanecer isolados dos dados das demais oficinas.

---

# 📌 Princípios de desenvolvimento

O projeto será desenvolvido de forma incremental, priorizando primeiro as necessidades reais da Soares Auto Center.

A estratégia inicial será:

```text
Problema real
      ↓
MVP
      ↓
Uso na oficina
      ↓
Feedback
      ↓
Melhorias
      ↓
Automação
      ↓
SaaS
```

O objetivo é evitar desenvolver funcionalidades complexas antes de validar o fluxo básico do sistema em um ambiente real.

---

# 🔒 Princípios de segurança

* Senhas nunca devem ser armazenadas em texto puro.
* Senhas devem ser protegidas utilizando BCrypt.
* A autenticação utilizará JWT.
* A API deve ser stateless.
* A autorização utiliza roles.
* O `ADMIN` possui acesso à plataforma como um todo.
* `GERENTE`, `ADMINISTRATIVO` e `MECANICO` são vinculados a uma oficina.
* Usuários de uma oficina não podem acessar dados de outras oficinas.
* O `oficina_id` utilizado para autorização não deve ser confiado diretamente ao cliente.
* Dados devem ser filtrados pela oficina no nível de serviço/repositório.
* Credenciais de banco não devem ser armazenadas no código-fonte.
* Configurações específicas de ambiente devem utilizar variáveis de ambiente.

---

# 👨‍💻 Autor

**André Tharssys Marques Soares**

Projeto desenvolvido no contexto de aplicação prática de tecnologias de desenvolvimento de software, utilizando como cenário inicial a **Soares Auto Center**.
