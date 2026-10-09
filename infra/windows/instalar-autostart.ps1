# Execute UMA vez, como Administrador, na máquina da oficina:
#   powershell -ExecutionPolicy Bypass -File infra\windows\instalar-autostart.ps1
# Cria uma tarefa que, ao logar, espera o Docker ficar pronto e roda "docker compose up -d".
# (Os containers já têm restart: unless-stopped; a tarefa cobre o caso de terem sido derrubados
#  com "docker compose down".) Também faz o Docker Desktop abrir sozinho ao logar.
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$cmd = "while (-not (docker info 2>`$null)) { Start-Sleep 5 }; Set-Location '$repo'; " +
       "docker compose -f infra/docker/compose.oficina.yml --env-file .env.oficina up -d"
$action   = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -WindowStyle Hidden -Command `"$cmd`""
$trigger  = New-ScheduledTaskTrigger -AtLogOn
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -RestartCount 5 -RestartInterval (New-TimeSpan -Minutes 2)
Register-ScheduledTask -TaskName 'OficinaPro - subir containers' -Action $action -Trigger $trigger `
  -Settings $settings -RunLevel Highest -Force | Out-Null

$dd = "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe"
if (Test-Path $dd) {
  Set-ItemProperty -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run' -Name 'Docker Desktop' -Value "`"$dd`" -Autostart"
}
Write-Host 'Pronto. Configure também o login automático do Windows e a BIOS "Restore on AC Power Loss" (docs/deploy-oficina.md).'
