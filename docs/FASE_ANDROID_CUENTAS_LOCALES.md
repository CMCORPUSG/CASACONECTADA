# CASA CONECTADA — Fase Android cuentas locales (debug)

## Alcance verificable
- Inicio de sesión con cuenta ADMIN/MEMBER que ya existe en el Worker local.
- Registro MEMBER mediante código de invitación privado creado por ADMIN.
- Consulta `GET /api/v1/me`, actualización de perfil `PATCH /api/v1/me`, miembros familiares y cierre de sesión.
- Bearer token cifrado usando AES-GCM con Android Keystore, no se guarda contraseña ni token en código o logs.
- Acceso de red sin TLS **SOLO** a `127.0.0.1` en compilación debug por `adb reverse` (política `networkSecurityConfig`).
- La app Room anterior se conserva como **modo local del dispositivo sin vincular a la cuenta**. No interpreta sus datos como privados de un usuario remoto ni sincronizados.

## Limitaciones importantes
- No hay envío/verificación de correo, recuperación de contraseña, protección de intentos de login ni fotos de cuenta desde servidor. **NO desplegar el Worker en producción**.
- No se han implementado roles sobre los datos de Room, ni migrado registros antiguos a cuentas personales.
- No se comparten datos entre dos teléfonos. Cada celular debe tener acceso físico a una máquina con `wrangler dev --local`, usando `adb reverse`, para esta prueba.
- Las invitaciones aparecen como códigos que el ADMIN debe compartir de forma privada; no se envían automáticamente por correo.
- En un futuro se aplicará una migración explícita y autorizada que vincule registros a `familyId/userId` con permisos y sincronización.
- No guardar datos financieros reales ni distribuir APK debug para uso doméstico habitual.

## Pasos
1. PC PowerShell #1: `cd 'D:\CASA CONECTADA\backend'; npm.cmd exec wrangler -- dev --local --port 8787`.
2. PC PowerShell #2: `$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"; & $adb reverse tcp:8787 tcp:8787`.
3. Compilar en proyecto raíz: `.\gradlew.bat assembleDebug`.
4. Instalar actualización sin desinstalar: `& $adb install -r '.\app\build\outputs\apk\debug\app-debug.apk'`.
5. Abrir CASA CONECTADA. Entrar con correo y contraseña ADMIN ya creados. Consultar familia, editar perfil e invitar email ficticio de prueba.
6. Cerrar sesión; pulsar "Tengo una invitación"; registrar MEMBER ficticio y comprobar perfil/rol.
7. No compartir correo, contraseña, token de sesión, invitación, `.dev.vars` ni claves en capturas de pantalla o GitHub.

## Publicación
- Probar primero en PC y teléfono. Subir a GitHub en una nueva rama mediante Pull Request solo cuando la compilación sea correcta.
- No modificar ni ejecutar D1 remoto, ni activar despliegue Cloudflare automático.
