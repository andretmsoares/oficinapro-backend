# OficinaPro

**SaaS para gerenciamento de oficinas automotivas**

> Projeto desenvolvido inicialmente para a **Soares Auto Center**, com o objetivo de centralizar e facilitar o gerenciamento de clientes, veículos, ordens de serviço, mecânicos, pagamentos e, futuramente, compras e documentos de peças.

---

## 📋 Sobre o projeto

O **OficinaPro** é uma aplicação web para gerenciamento de oficinas automotivas.

O projeto surgiu a partir da identificação de necessidades reais da **Soares Auto Center**, permitindo transformar processos atualmente realizados de forma manual em um sistema centralizado.

A primeira versão será desenvolvida como um **MVP**, focado nas operações essenciais da oficina.

Posteriormente, o projeto poderá evoluir para um **SaaS multiempresa**, permitindo que diferentes oficinas utilizem a plataforma de forma isolada e segura.

---

## 🎯 Objetivos

O OficinaPro tem como principais objetivos:

- Centralizar o cadastro de clientes.
- Gerenciar veículos.
- Criar e acompanhar Ordens de Serviço.
- Associar mecânicos aos serviços.
- Registrar pagamentos.
- Manter o histórico dos veículos.
- Facilitar o acompanhamento operacional da oficina.
- Futuramente gerenciar peças, fornecedores e compras.
- Futuramente automatizar o cadastro de documentos utilizando OCR.
- Evoluir para uma plataforma SaaS multiempresa.

---

## 🚧 Status do projeto

**Em desenvolvimento 🚧**

Atualmente o projeto está na fase de construção do **MVP**.

### Roadmap inicial

- [x] Inicialização do projeto Spring Boot
- [ ] Configuração do PostgreSQL
- [ ] Configuração do Flyway
- [ ] Configuração do Docker
- [ ] Cadastro de oficinas
- [ ] Autenticação
- [ ] Cadastro de clientes
- [ ] Cadastro de veículos
- [ ] Cadastro de mecânicos
- [ ] Ordens de Serviço
- [ ] Pagamentos
- [ ] Histórico de veículos
- [ ] Dashboard
- [ ] Fornecedores
- [ ] Compras e peças
- [ ] OCR
- [ ] Inteligência Artificial
- [ ] Multi-tenancy
- [ ] SaaS

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
````

A arquitetura poderá evoluir futuramente para serviços independentes caso exista necessidade.

---

# 🛠️ Tecnologias

## Backend

* **Java 21**
* **Spring Boot**
* **Spring Web**
* **Spring Data JPA**
* **Hibernate**
* **Spring Security**
* **JWT**
* **Bean Validation**
* **Gradle**
* **Flyway**

## Banco de dados

* **PostgreSQL**

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

* **JUnit**
* **Mockito**
* **Spring Boot Test**

## Documentação

* **OpenAPI**
* **Swagger**

---

# 📁 Estrutura do projeto

A estrutura inicial do backend seguirá uma organização por responsabilidades:

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
│
├── docker-compose.yml
├── Dockerfile
├── build.gradle
├── settings.gradle
├── .gitignore
├── .env.example
└── README.md
```

---

# 🗄️ Banco de dados

O banco principal utilizado pelo projeto será o **PostgreSQL**.

A persistência será realizada utilizando:

```text
Spring Data JPA
       ↓
   Hibernate
       ↓
  PostgreSQL
```

As alterações no banco serão controladas utilizando **Flyway**.

As migrations ficarão em:

```text
src/main/resources/db/migration/
```

Exemplo:

```text
V1__create_oficinas.sql
V2__create_clientes.sql
V3__create_veiculos.sql
```

O Hibernate será utilizado apenas para validar o schema:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

---

# 🐳 Executando o projeto

## Pré-requisitos

Antes de executar o projeto, tenha instalado:

* Java 21+
* Git
* Docker
* Docker Compose

O Gradle Wrapper (`gradlew`) será utilizado pelo projeto, portanto não é necessário instalar o Gradle globalmente.

---

## 1. Clonar o repositório

```powershell
git clone <URL_DO_REPOSITORIO>
cd oficinapro
```

---

## 2. Configurar variáveis de ambiente

Crie um arquivo `.env` baseado no exemplo:

```powershell
Copy-Item .env.example .env
```

Configure as variáveis necessárias:

```env
POSTGRES_DB=oficinapro
POSTGRES_USER=oficinapro
POSTGRES_PASSWORD=oficinapro
POSTGRES_PORT=5432
```

> O arquivo `.env` não deve ser versionado.

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

No Windows:

```powershell
.\gradlew bootRun
```

No Linux/macOS:

```bash
./gradlew bootRun
```

A aplicação estará disponível em:

```text
http://localhost:8080
```

---

# 📚 Documentação da API

A API será documentada utilizando OpenAPI/Swagger.

Após iniciar o backend:

```text
http://localhost:8080/swagger-ui/index.html
```

A documentação permitirá visualizar e testar os endpoints diretamente pelo navegador.

---

# 🧪 Testes

Para executar os testes:

### Windows

```powershell
.\gradlew test
```

### Linux/macOS

```bash
./gradlew test
```

Para executar o build completo:

```powershell
.\gradlew clean build
```

---

# 🐳 Docker

Para iniciar os serviços:

```powershell
docker compose up -d
```

Para visualizar os logs:

```powershell
docker compose logs -f
```

Para parar os serviços:

```powershell
docker compose down
```

Para remover também os volumes:

```powershell
docker compose down -v
```

> O comando `down -v` remove os dados persistidos do PostgreSQL. Utilize apenas em ambientes de desenvolvimento quando isso for desejado.

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

# 📦 Módulos planejados

## MVP

* [ ] Oficinas
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
                │      MVP      │
                │               │
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

# 🔐 Segurança

A aplicação utilizará:

* Spring Security
* JWT
* Hash seguro de senhas
* Controle de acesso baseado em roles

Perfis inicialmente previstos:

```text
ADMIN
GERENTE
ADMINISTRATIVO
MECANICO
```

---

# 🏢 Modelo SaaS

Embora a primeira versão seja destinada à Soares Auto Center, o sistema será projetado considerando uma futura arquitetura multiempresa.

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

# 📌 Desenvolvimento

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

# 👨‍💻 Autor

**André Tharssys Marques Soares**

Projeto desenvolvido no contexto de aplicação prática de tecnologias de desenvolvimento de software, utilizando como cenário inicial a **Soares Auto Center**.

```
```
