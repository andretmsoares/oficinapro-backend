# Deploy de validação (1 usuário, custo ~zero)

| Parte | Onde | Custo |
|---|---|---|
| Front (Vite/React) | Vercel | grátis |
| Back (Spring Boot) | Sua máquina + Cloudflare Tunnel | domínio (~US$ 10/ano) |
| Banco (Postgres) | Neon | grátis |
| Logos | Cloudflare R2 | grátis (10 GB) |

Por que o back no Tunnel e não no Render grátis: o Render free hiberna após ~15 min sem
tráfego (primeira requisição leva cerca de 1 min) e tem 512 MB de RAM, apertado para
Spring Boot. Com o Tunnel não há cold start nem limite de memória; o custo é a máquina
precisar ficar ligada. Confira os limites atuais dos planos gratuitos antes de decidir.

## 1. Domínio no Cloudflare
1. Compre ou transfira um domínio no Cloudflare (Registrar) ou aponte os nameservers de um
   domínio existente para o Cloudflare.
2. Vai usar `api.appoficinapro.com.br` (back) e `appoficinapro.com.br` (front, apex).

## 2. Banco: Neon
1. Crie conta em neon.tech e um projeto (região mais próxima, ex.: São Paulo se disponível).
2. Em **Connection details**, desmarque **Pooled connection** e copie host, banco, usuário e
   senha. O Flyway precisa de conexão **direta**, não do pooler.
3. O `application.yml` monta a URL sem `sslmode`, e o Neon exige SSL. Use a variável
   `SPRING_DATASOURCE_URL` (sobrescreve a URL montada):
   `jdbc:postgresql://<host-direto>/<banco>?sslmode=require`
4. As migrations rodam sozinhas na primeira subida.

## 3. Logos: Cloudflare R2
1. Painel Cloudflare > **R2** > **Create bucket** (ex.: `oficinapro-logos`). Deixe privado.
2. **Manage R2 API Tokens** > criar token com permissão **Object Read & Write**, restrito ao
   bucket. Guarde **Access Key ID** e **Secret Access Key** (a secret aparece uma vez).
3. O endpoint é `https://<ACCOUNT_ID>.r2.cloudflarestorage.com` (o Account ID está na página
   do R2).
4. Não precisa de acesso público: o backend lê o objeto e serve os bytes.

## 4. Back: Cloudflare Tunnel
1. Painel Cloudflare > **Zero Trust** > **Networks** > **Tunnels** > **Create a tunnel**
   (tipo Cloudflared). Copie o **token**.
2. Em **Public Hostname**: `api.appoficinapro.com.br` apontando para `http://app:8080`.
3. No `.env.prod` (gitignored; nunca commitar), preencha:

```env
POSTGRES_DB=...            # exigido pelo application.yml mesmo usando SPRING_DATASOURCE_URL
POSTGRES_USER=<usuario-neon>
POSTGRES_PASSWORD=<senha-neon>
SPRING_DATASOURCE_URL=jdbc:postgresql://<host-direto>/<banco>?sslmode=require
JWT_SECRET=<openssl rand -base64 48>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<senha-forte>
R2_BUCKET=oficinapro-logos
R2_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
R2_ACCESS_KEY=...
R2_SECRET_KEY=...
OFICINAPRO_CORS_ALLOWED_ORIGINS=https://appoficinapro.com.br,https://www.appoficinapro.com.br
TUNNEL_TOKEN=<token-do-tunel>
DOCKER_USERNAME=<seu-usuario-dockerhub>
```

4. Suba:

```bash
docker compose -f infra/docker/compose.tunnel.yml --env-file .env.prod up -d
```

5. Teste: `https://api.appoficinapro.com.br/actuator/health` deve responder `UP`.
6. No Windows: desative a hibernação (Configurações > Energia) e deixe o Docker Desktop
   iniciar com o sistema.

## 5. Front: Vercel
1. Importe o repositório `oficinapro-frontend` na Vercel (preset Vite).
2. Em **Environment Variables**: `VITE_API_URL=https://api.appoficinapro.com.br`. A variável é
   embutida no build, então mudar o valor exige novo deploy.
3. Se o app usa rotas do React Router, adicione `vercel.json` na raiz do front para o refresh
   de página não dar 404:

```json
{ "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }] }
```

4. Depois do primeiro deploy, copie a URL final e confirme que ela está em
   `OFICINAPRO_CORS_ALLOWED_ORIGINS` no `.env` do back (reinicie o container se mudar).

## 6. Backup
Neon mantém histórico de curto prazo no plano grátis, mas faça um `pg_dump` periódico:

```bash
pg_dump "postgresql://<usuario>:<senha>@<host-direto>/<banco>?sslmode=require" > backup.sql
```

## Migrando depois
Para sair da sua máquina, o mesmo container roda em Render, Fly.io ou um VPS sem mudar
código: só trocar onde o `docker compose` roda e mover o hostname do túnel.
