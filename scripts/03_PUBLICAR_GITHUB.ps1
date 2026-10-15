#requires -Version 5.1
$ErrorActionPreference = 'Stop'
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not (Test-Path '.git')) { throw 'No estás en un clon Git. Ejecuta primero el script 01.' }
if (Get-Command gh -ErrorAction SilentlyContinue) { gh auth setup-git }
git status --short
git add -A
if ($LASTEXITCODE -ne 0) { throw 'Error en git add.' }
git diff --cached --quiet
if ($LASTEXITCODE -eq 0) { Write-Host 'Sin cambios para publicar.'; exit 0 }
git commit -m 'feat: initialize Casa Conectada Android'
if ($LASTEXITCODE -ne 0) { throw 'Error en commit.' }
git push -u origin main
if ($LASTEXITCODE -ne 0) { throw 'Error al publicar en GitHub.' }
Write-Host 'Repositorio publicado.' -ForegroundColor Green
