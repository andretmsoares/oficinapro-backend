#!/usr/bin/env bash
# Restaura dados no Postgres LOCAL. CUIDADO: sobrescreve os dados atuais do banco local.
#   restore.sh list             -> lista os dumps guardados na máquina
#   restore.sh <arquivo.dump>   -> restaura um dump local
#   restore.sh neon             -> copia o banco do Neon para o local (migração inicial / desastre)
# Pare a API antes (docker compose ... stop app) e suba de novo depois.
# Uso: docker compose -f infra/docker/compose.oficina.yml --env-file .env.oficina run --rm backup restore.sh ...
set -euo pipefail
: "${POSTGRES_HOST:=postgres}" "${POSTGRES_PORT:=5432}"
export PGPASSWORD="$POSTGRES_PASSWORD"

restore() {
  pg_restore -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    --clean --if-exists --no-owner --no-privileges "$1"
}

case "${1:-list}" in
  list) ls -1t /backups/*.dump ;;
  neon)
    : "${BACKUP_NEON_URL:?defina BACKUP_NEON_URL}"
    pg_dump "$BACKUP_NEON_URL" -Fc --no-owner --no-privileges -f /tmp/neon.dump
    restore /tmp/neon.dump
    echo "banco local restaurado a partir do Neon" ;;
  *) restore "/backups/$(basename "$1")"; echo "restaurado: $1" ;;
esac
