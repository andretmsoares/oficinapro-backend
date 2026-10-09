# Deploy na máquina da oficina (cliente único)

Tudo roda na máquina da oficina via Docker: **Postgres 18 local**, API, Cloudflare Tunnel e um
container de **backup** que, a cada 6 h, guarda um dump na máquina e espelha o banco no **Neon**.
O Cloudflare R2 continua só para as logos (nada é gravado nem apagado lá pelo backup).

Arquivos: `infra/docker/compose.oficina.yml`, `.env.oficina-example`, `infra/backup/`,
`infra/windows/instalar-autostart.ps1`.

## Por que um `.env.oficina` separado
- `.env` é de desenvolvimento (`localhost`, senha fraca).
- `.env.prod` aponta para o Neon via `SPRING_DATASOURCE_URL`, que sobrescreve o host do banco: a API
  ignoraria o Postgres local. Não use `.env.prod` com este compose.

## 1. Primeira instalação
1. Instale o Docker Desktop (WSL2) e clone o repositório.
2. `copy .env.oficina-example .env.oficina` e preencha: senhas fortes, `JWT_SECRET`, `TUNNEL_TOKEN`,
   credenciais do R2 (logos) e `BACKUP_NEON_URL` (conexão **direta** do Neon, sem `-pooler`,
   `?sslmode=require`). No Zero Trust, o Public Hostname aponta para `http://app:8080`.
   Dica: evite `$` e `"` nas senhas e coloque entre aspas valores com espaço.
3. Suba só o banco e **migre os dados atuais do Neon para o local**:
   ```
   docker compose -f infra/docker/compose.oficina.yml --env-file .env.oficina up -d --build postgres
   docker compose -f infra/docker/compose.oficina.yml --env-file .env.oficina run --rm --no-deps backup restore.sh neon
   ```
   Isso **sobrescreve** o banco local com o conteúdo do Neon (só faça na instalação ou em desastre).
4. Suba o resto: `docker compose -f infra/docker/compose.oficina.yml --env-file .env.oficina up -d --build`
5. Confira o login no front. Só então ligue o espelho: `NEON_MIRROR_ENABLED=true` no `.env.oficina` e
   `... up -d backup`. **Antes de ligar, tenha certeza de que o local tem os dados**: o espelho
   substitui o conteúdo do Neon pelo do banco local.
6. Teste o backup: `... exec backup backup.sh once` e veja `docker logs oficinapro-backup`.

## 2. Subir sozinho ao ligar a máquina
- Os containers têm `restart: unless-stopped`: se caírem, voltam; quando o Docker inicia, eles sobem.
- Rode `infra\windows\instalar-autostart.ps1` como Administrador (Docker Desktop abre no login +
  tarefa agendada que executa o `compose up -d`).
- O Docker Desktop só inicia depois do **login** no Windows: configure login automático
  (`netplwiz`, ou Sysinternals Autologon) para a máquina voltar sozinha após reboot/queda de energia.
- Na BIOS, ative **Restore on AC Power Loss = Power On** e desative a suspensão do Windows.
- Evite `docker compose down` (remove containers); use `stop`/`start`. Nunca use `down -v`: apaga o banco.

## 3. Backups
- A cada `BACKUP_INTERVAL_HOURS` (padrão 6): `pg_dump` formato custom em `/backups` (volume
  `oficinapro-backups`), validado com `pg_restore --list`; ficam os últimos 28 (~7 dias).
- Com `NEON_MIRROR_ENABLED=true`, cada dump também é restaurado no Neon (`--clean`): o Neon fica como
  cópia fora da máquina, **uma só e sempre a mais recente**. Se o banco local for corrompido e o
  espelho rodar antes de você perceber, o Neon recebe a corrupção; por isso os dumps locais existem.
  Se a máquina for perdida, os dumps locais vão junto: o Neon é a única cópia remota.
- Logs: `docker logs oficinapro-backup`. Para ser avisado quando parar, crie um check no
  healthchecks.io e coloque a URL em `BACKUP_PING_URL`.
- Limite do plano grátis do Neon (armazenamento) vale também para o espelho: confira o tamanho.

## 4. Restaurar
Pare a API antes e suba depois (`stop app` / `start app`):
```
... run --rm --no-deps backup restore.sh list              # dumps locais
... run --rm --no-deps backup restore.sh <arquivo.dump>    # restaura um dump local
... run --rm --no-deps backup restore.sh neon              # recupera do espelho no Neon
```
Todos sobrescrevem o banco local. Teste o restore pelo menos uma vez.

## 5. Atualizar a versão
```
docker compose -f infra/docker/compose.oficina.yml --env-file .env.oficina pull app
docker compose -f infra/docker/compose.oficina.yml --env-file .env.oficina up -d
```
As migrations Flyway rodam sozinhas. Faça um `backup.sh once` antes de atualizar.
