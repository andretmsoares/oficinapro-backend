#!/usr/bin/env bash
# Backup do Postgres LOCAL: guarda dumps na máquina e espelha o banco no Neon.
#   backup.sh loop   -> roda para sempre: 1º backup logo após subir, depois a cada BACKUP_INTERVAL_HOURS
#   backup.sh once   -> um backup só (para testar: docker compose ... exec backup backup.sh once)
# Intervalo (e não "todo dia às 2h") porque a máquina da oficina pode estar desligada de madrugada.
#
# O espelho no Neon SUBSTITUI o conteúdo do banco de lá pelo do banco local (pg_restore --clean).
# Por isso só roda com NEON_MIRROR_ENABLED=true; ligue depois de migrar os dados antigos do Neon
# para o local (docs/deploy-oficina.md), senão um local vazio sobrescreveria o Neon.
set -euo pipefail

: "${POSTGRES_HOST:=postgres}" "${POSTGRES_PORT:=5432}"
: "${POSTGRES_DB:?}" "${POSTGRES_USER:?}" "${POSTGRES_PASSWORD:?}"
: "${BACKUP_INTERVAL_HOURS:=6}" "${BACKUP_LOCAL_KEEP:=28}" "${NEON_MIRROR_ENABLED:=false}"

export PGPASSWORD="$POSTGRES_PASSWORD"

log() { echo "[backup $(date '+%F %T')] $*"; }

# $1 = "" (ok) ou "/fail". Opcional: healthchecks.io (ou similar) avisa se os pings pararem de chegar.
ping_monitor() {
  if [ -n "${BACKUP_PING_URL:-}" ]; then wget -q -T 10 -O /dev/null "${BACKUP_PING_URL}$1" || true; fi
}

run_backup() {
  local file
  file="/backups/${POSTGRES_DB}-$(date '+%Y%m%d-%H%M%S').dump"
  log "pg_dump -> $file"
  pg_dump -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f "$file.tmp"
  mv "$file.tmp" "$file"
  # Dump vazio/truncado não pode virar "backup bom".
  pg_restore --list "$file" > /dev/null

  if [ "$NEON_MIRROR_ENABLED" = "true" ]; then
    : "${BACKUP_NEON_URL:?defina BACKUP_NEON_URL (conexão direta, sslmode=require)}"
    log "espelhando no Neon"
    pg_restore --dbname="$BACKUP_NEON_URL" --clean --if-exists --no-owner --no-privileges --exit-on-error "$file"
  else
    log "espelho no Neon desligado (NEON_MIRROR_ENABLED=false)"
  fi

  # Cópias locais: mantém só as mais recentes (28 x 6h = 7 dias).
  ls -1t /backups/*.dump 2>/dev/null | tail -n +"$((BACKUP_LOCAL_KEEP + 1))" | xargs -r rm -f
  log "ok ($(du -h "$file" | cut -f1))"
}

backup_safe() {
  if run_backup; then ping_monitor ""; else log "FALHOU"; ping_monitor "/fail"; return 1; fi
}

case "${1:-loop}" in
  once) backup_safe ;;
  loop)
    sleep 120 # deixa o Postgres/app subirem
    while true; do
      backup_safe || true
      sleep "$((BACKUP_INTERVAL_HOURS * 3600))"
    done ;;
  *) echo "uso: $0 [loop|once]" >&2; exit 2 ;;
esac
