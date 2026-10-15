# Plan técnico — V1.0 (pendiente de implementación total)

## Prioridades y límites

- **Sin Firebase**. El cliente funciona localmente con Room/SQLite desde primera instalación.
- **Sin prometer sincronización gratuita sin servidor**. Un equipo familiar encendido o
  VPS/autohospedaje proveerá la infraestructura; la electricidad, el tráfico, el
  almacenamiento, el ancho de banda y el mantenimiento sí tienen costos potenciales.
- Backend previsto: Ktor (Kotlin), SQLite, HTTPS con reverse proxy, JSON REST.
- Backend **NO implementado** en esta base: no debe desplegarse ni afirmarse sincronización real.

## Autenticación y autorización previstas

- Un dispositivo o usuario administrador crea la familia (llave pública y credencial local).
- Códigos/QR de invitación con expiración y uso único. Nunca equivalen a credenciales fijas.
- Vincular dispositivos mediante intercambio autenticado y TLS, evitando secretos en el QR.
- Roles para administrar la casa, confirmar aportes y consultar números bancarios.
- Sincronizar únicamente información autorizada por integrante.
- En servidor, proteger contraseñas/token y los archivos recibidos; cifrar copias de seguridad.

## Sincronización futura

- Entidades con UUID, reloj de actualización, revisión de cambios y tombstones para eliminaciones.
- Cola durable de cambios locales, envío por HTTPS y descarga incremental.
- Modelo de conflictos específico: confirmación de dinero protegida contra reintentos
  y duplicados con idempotency key; actividades concurrentes con versiones.
- Fotos con identificador y hash, transferidas separadamente de los metadatos.
- No mostrar estados sincronizados hasta recibir confirmación de servidor.
- Un equipo desconectado conserva lo local; no podrá enterarse de novedades ajenas.

## Seguridad y cuidados

- La capa de dominio ya separa aportes pendientes de confirmados; esto no sustituye
  la verificación bancaria, porque no hay integración bancaria automática.
- AES-GCM de ciertos campos financieros usa clave en Keystore local; sin backup
  seguro separado, **desinstalar la app puede volver irrecuperables esos campos**.
- Prohibido guardar claves de banca por internet, PIN, CVV y tokens bancarios.
- Para producción, añadir modelos de amenazas, pruebas de seguridad, autorización
  y cifrado autenticado del archivo backup con contraseña de recuperación.

## Criterios de aceptación pendientes

- Compilar `assembleDebug` y ejecutar en varios Android API 26–35.
- Probar asignación, modificación, rotación, evidencia y almacenamiento tras reinicio.
- Probar pagos, aportes concurrentes, historial e idempotencia.
- Probar exportación/restauración sin perder fotografías ni datos financieros.
- Pruebas de red offline→online, autenticar invitaciones, revocar dispositivos.
- Asegurar compatibilidad y licencias de todas las dependencias y recursos.
