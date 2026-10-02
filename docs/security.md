# Segurança — operação e endurecimento

Complementa [api.md](./api.md) (autenticação, limites de login, auditoria) e
[permissions.md](./permissions.md) (quem pode o quê). Este arquivo reúne o que **não** está
no código: configuração de ambiente, rotação de segredos e o que configurar no Cloudflare e no
banco.

## 1. Segredos

- Todo `.env*` está no `.gitignore`, exceto o `.env-example` (que só tem modelos). Mantenha o
  `.env.prod` fora da pasta do repositório sempre que possível. O CI roda **gitleaks** em PRs e
  pushes.
- **Os repositórios são públicos.** Se um segredo for commitado, mesmo que depois apagado,
  considere-o vazado e **rotacione**; reescrever histórico não basta (há forks e caches).
- A senha do Postgres de **desenvolvimento** apareceu no histórico (README e `docker-compose.yml`
  iniciais) e é a mesma do `.env` local de quem a usou. Não protege nada além do banco local,
  mas troque-a: altere `POSTGRES_PASSWORD` no `.env`, rode `docker compose -f
  infra/docker/compose.dev.yml down -v` (apaga o volume de dev) e suba de novo. Se o gitleaks
  acusar esse valor no histórico, é esse achado conhecido.
- `JWT_SECRET`: a aplicação **não sobe** com menos de 32 bytes, com o texto de exemplo
  ("troque...") ou com poucos caracteres distintos. Gere com `openssl rand -base64 48`. Trocar o
  segredo derruba todas as sessões.
- `ADMIN_PASSWORD` precisa atender à política de senha (8 a 72 caracteres, letra e número, fora
  das comuns); senão o ADMIN **não é criado** e o motivo vai para o log. Depois do primeiro
  login, troque a senha e remova `ADMIN_USERNAME/ADMIN_PASSWORD` do ambiente.

## 2. Perfis e deploy

| Ambiente | Arquivo | Profile |
|---|---|---|
| Desenvolvimento | `infra/docker/compose.dev.yml` | `dev` (log de SQL/debug; portas só em loopback) |
| Produção com Neon + Cloudflare Tunnel | `infra/docker/compose.tunnel.yml` | `prod` + `TRUST_PROXY_HEADERS=true`, **nenhuma porta publicada** |
| Produção com Postgres no host | `infra/docker/compose.prod.yml` | `prod`, Postgres sem porta, API só em loopback |

Sem `SPRING_PROFILES_ACTIVE` a aplicação sobe com a configuração base, que é quieta. O padrão
já foi `dev`; uma produção sem o profile logava SQL **com parâmetros** (hashes de senha, CPF,
telefone). O profile `prod` também desliga Swagger/OpenAPI e o detalhe do health.

Containers rodam sem root, com `read_only`, `cap_drop: ALL` e `no-new-privileges`. Prefira
`IMAGE_TAG=<sha do commit>` a `:latest`. As imagens só são publicadas depois do merge na `main`
(workflow `Docker Images`), após scan do Trivy.

## 3. Banco de dados

**Conexão.** Use `sslmode=verify-full` (valida o certificado do servidor) em vez de
`sslmode=require` (só criptografa). Com `sslfactory=...DefaultJavaSSLFactory` a JVM usa o
próprio truststore (o certificado do Neon é de CA pública). Exemplo no `.env-example`.

**Menor privilégio.** Hoje o mesmo usuário roda as migrations e as consultas da aplicação, ou
seja, um SQL injection ou bug poderia fazer `DROP`. O recomendado é separar:

```sql
-- como dono do schema (uma vez):
CREATE ROLE oficinapro_app LOGIN PASSWORD '<gerada>';
GRANT CONNECT ON DATABASE <db> TO oficinapro_app;
GRANT USAGE ON SCHEMA public TO oficinapro_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO oficinapro_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO oficinapro_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO oficinapro_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO oficinapro_app;
```

e usar o dono **só** para o Flyway (por exemplo, rodando as migrations num passo de deploy
com `FLYWAY_*`) enquanto a aplicação conecta como `oficinapro_app`. Isso exige mudança no
processo de deploy (Flyway fora da aplicação ou com credenciais próprias), por isso não foi
feito no código.

**Isolamento entre oficinas** é garantido na aplicação (`OficinaAccessValidator`); o banco não
impõe (sem RLS nem FKs compostas). Qualquer endpoint novo precisa validar o tenant e ganhar um
caso em `TenantIsolationIntegrationTest`.

## 4. Cloudflare (recomendado)

- **Rate limiting** em `POST /api/auth/login`, por IP (ex.: 10 req/min), além do limite da
  aplicação — o da aplicação vale por instância; o do Cloudflare é global.
- **WAF** com o conjunto gerenciado ligado; **Always Use HTTPS** e **HSTS**.
- API só acessível pelo túnel (nenhuma porta publicada no host). Com isso `CF-Connecting-IP` é
  confiável.
- Headers de segurança do **frontend** estão em `nginx.conf` e `vercel.json` (CSP,
  `X-Frame-Options`, `nosniff`, `Referrer-Policy`, `Permissions-Policy`, HSTS). Se o domínio da
  API mudar, ajuste o `connect-src` da CSP nos dois arquivos.

## 5. Auditoria e monitoramento

`audit_log` guarda os eventos (ver [api.md](./api.md#auditoria)). Consultas úteis:

```sql
-- últimos estornos
SELECT ocorrido_em, ator_id, oficina_id, detalhe FROM audit_log
 WHERE acao = 'PAGAMENTO_ESTORNADO' ORDER BY ocorrido_em DESC LIMIT 50;

-- logins por IP nas últimas 24 h
SELECT ip, count(*) FROM audit_log
 WHERE acao = 'LOGIN_SUCESSO' AND ocorrido_em > now() - interval '1 day'
 GROUP BY ip ORDER BY 2 DESC;
```

Falhas de login saem no log como `evento=LOGIN_FALHA ip=... usuario_hash=...`. Crie um alerta
no seu agregador de logs para picos desse evento. A tabela cresce sem limite: defina uma
retenção (ex.: apagar com mais de 12 meses).

## 6. CI/CD

- Workflows com `permissions: contents: read` e actions fixadas por **SHA** (o Dependabot
  atualiza). `docker-ci` publica só após merge na `main`.
- **CodeQL**, **gitleaks** e **Trivy** (imagem) rodam no pipeline; `npm audit` roda no CI do
  frontend. Dependabot cobre Gradle/npm, GitHub Actions e Docker.
- Recomendado no GitHub: proteção de branch em `main`/`develop` exigindo PR, revisão e CI
  verde, e um *environment* de produção com aprovação manual para o deploy.
