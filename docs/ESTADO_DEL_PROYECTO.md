# Estado técnico verificado — 15 de octubre de 2026

## Cliente Android (Premium V3)

- `assembleDebug`: **BUILD SUCCESSFUL**, APK instalada con ADB (`Success`).
- Android conectado mediante ADB; `adb reverse tcp:8787 tcp:8787` respondió correctamente y se verificó con `adb reverse --list`.
- Persistencia local Room/SQLite: integrantes/perfiles locales, tareas y evidencia fotográfica, calendario cronológico de tareas/pagos, gastos, pagos, metas monetarias y aportes; puntos básicos.
- No están implementadas: notas compartidas, notificaciones automáticas, actividades independientes con horarios complejos, rotación real, inicio de sesión conectado, sincronización entre dispositivos ni ranking completo.

## Backend Cloudflare Workers + D1

- Node 24, Wrangler 4.151.0, proyecto Worker only con TypeScript.
- Compilación de tipos local `npm.cmd exec tsc -- --noEmit` finalizó sin errores.
- D1 remoto `casa-conectada-db` creada; **no se aplicaron migraciones remotas ni se desplegó Worker** en el estado reportado.
- D1 local: 14 tablas de negocio + migración de autenticación (`auth_sessions`, `invitation_claims`, `setup_guard`).
- Pruebas realizadas en backend local: `GET /health`, creación inicial ADMIN, inicio de sesión, invitación a correo ficticio, alta de MEMBER y listado familiar.

## Conexión móvil: qué significa ADB reverse

El móvil puede dirigir sus conexiones a `127.0.0.1:8787` hacia la computadora por USB; **no demuestra que la APK tenga el cliente HTTP integrado**. La integración Android ↔ API todavía es la siguiente fase.

## Publicación responsable

El repositorio puede mostrar código y demostraciones, **no** cuentas privadas, respaldos locales, `.dev.vars`, tokens, fotografías reales ni información financiera de usuarios. La API de desarrollo no debe desplegarse públicamente sin control de permisos, verificación de correo, rate limiting y pruebas de seguridad.

Consulta [ALCANCE_FUNCIONAL.md](ALCANCE_FUNCIONAL.md) para escenarios y criterios futuros.
