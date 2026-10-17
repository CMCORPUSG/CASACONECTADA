# Lista de seguridad antes de publicar en GitHub

Este repositorio es público. Publica únicamente código y recursos de ejemplo. Nunca publiques cuentas familiares reales, credenciales ni bases locales.

## Prohibido versionar

- `backend/.dev.vars` y variantes, `.env`, tokens, claves API, llaves privadas y contraseñas.
- Carpetas de ejecución `backend/.wrangler`, `backend/node_modules`, `app/build`, `.gradle` y APKs locales.
- Archivos de D1 local (`*.sqlite`, `*.db`) o exportaciones con registros reales.
- Fotografías de usuarios, capturas de pantallas con datos bancarios o correos personales.
- Configuraciones `local.properties` y claves de firma Android (`*.jks`, `*.keystore`).

## Antes del `git push`

```powershell
cd "D:\CASA CONECTADA"
git status --short
git diff --cached --name-only
git diff --cached --stat
git ls-files "*dev.vars*" "*.env*" "*.sqlite" "*.db" "*.jks" "*.keystore"
```

El último comando debe estar vacío. Si aparece algún secreto, no publiques. Para quitarlo del índice **sin borrar el archivo local** usa `git rm --cached <ruta>`; si ya fue publicado, revoca y rota sus claves.

## Antes de desplegar Cloudflare

Requisitos pendientes: autenticación robusta, verificación de correo, control de abuso, permisos en cada endpoint, límites de consulta/escritura, políticas de datos personales y planes de recuperación.

> Que una API funcione localmente no significa que sea segura para internet.
