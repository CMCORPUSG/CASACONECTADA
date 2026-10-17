# CASA CONECTADA — alcance funcional integral

> Visión de producto y criterios para futuras implementaciones. No todo lo documentado existe actualmente en Android. La comprobación técnica se describe en `ESTADO_DEL_PROYECTO.md`.

## Objetivo

Organizar la convivencia de personas que comparten una casa o departamento. Cada integrante tendrá su identidad y responsabilidades, y podrá seguir compromisos del hogar, tareas, actividades, gastos, metas y comunicaciones compartidas. Se privilegia el funcionamiento local y sincronización remota solo cuando hay cambios o acciones del usuario.

## Tipos de registro (no son sinónimos)

| Tipo | Propósito | Campos / operaciones previstos | Estado |
|---|---|---|---|
| Tarea | Responsabilidad por completar | Título, responsable(s), fecha límite, evidencia, estado, prioridad, puntos, recurrencia y rotaciones | CRUD y completado local básicos; recurrencia automática pendiente |
| Actividad | Evento del hogar planificado | Inicio, fin, asistentes, responsable, repetición, calendario compartido | Fechas se muestran desde tareas y pagos; alta de evento independiente pendiente |
| Recordatorio | Aviso oportuno | Antelación, disparador, destino, posponer, aviso local | Pendiente |
| Nota | Información compartida | Texto, categoría, adjuntos, listas de compra y vistos | Pendiente |
| Meta | Objetivo medible | Tipo (ahorro u objetivo no financiero), fecha, avance, aportes/evidencia | Ahorro local; metas no financieras pendientes |
| Gasto | Dinero utilizado o por pagar | Categoría, fecha, importe, comprobante, responsable, distribución | Registro local; división entre usuarios pendiente |
| Cuenta bancaria | Método de pago/recepción | Tipo, titular, datos privados, acceso autorizado, estado | Métodos locales en metas; registro por usuario pendiente |
| Perfil | Identidad de un integrante | Cuenta, foto, alias, datos personales, permisos y preferencias | Perfil local y API local por separado; integración pendiente |

## Casos de uso cotidianos que debe admitir

1. **Sacar al perro:** tarea/actividad de mañana y tarde, asignable a integrantes diferentes, calendario y avisos locales, completado verificable.
2. **Lavar los platos:** tarea diaria, responsable o turnos alternados, cumplimiento, puntos y resumen semanal.
3. **Limpiar servicios higiénicos:** responsable, fecha, frecuencia semanal, evidencia opcional y rotación justa.
4. **Sacar basura:** compromiso recurrente con hora y rotación, recordatorio antes del retiro.
5. **Compras de la casa:** lista compartida, artículos, persona encargada, comprobantes y reparto opcional del gasto.
6. **Pagos de agua, luz, gas, internet o mantenimiento:** vencimiento, responsable, aviso, pago con evidencia y reparto entre integrantes.
7. **Anuncios y notas:** instrucciones del hogar, fechas importantes, artículos por comprar, observaciones compartidas.
8. **Ahorro y objetivos:** viaje familiar, electrodoméstico, fondo común, progreso porcentual y aportes verificados.

## Roles e identidad

- **ADMIN:** crea la familia, administra invitaciones y permisos familiares. No debe tener acceso automático a contraseñas ni datos bancarios privados de otros miembros.
- **MEMBER:** se registra con una invitación y mantiene su propia cuenta, fotografía y perfil; interactúa con los recursos compartidos que tenga autorizados.
- **Nadie crea contraseñas de terceros.** Los nuevos miembros deben elegir sus credenciales personalmente; los perfiles locales existentes no son cuentas autenticadas.
- **Separación por familia y usuario:** todas las consultas y cambios deben comprobar la pertenencia y el permiso correspondiente.

## Sincronización y ahorro de recursos

- Guardar cambios primero en Room/SQLite cuando corresponda.
- Sincronizar al abrir la app, cuando se solicite manualmente o al guardar cambios que deban compartirse, con límites y control de reintentos; sin polling permanente.
- Mostrar estado: sincronizado / cambios pendientes / sin conexión / error, con última sincronización.
- Soportar conflictos, idempotencia y control de versiones antes de habilitar acceso remoto real.
- No almacenar fotografías reales en D1; evaluar opciones de almacenamiento externo seguro con límites y políticas claras.

## Logros, estadísticas e interfaz

- Progreso de tareas por usuario (% completado), tareas pendientes/vencidas y distribución semanal.
- Puntos por tareas completadas válidamente; prevención de dobles puntuaciones.
- Ranking familiar opcional, no coercitivo; posibilidad de ocultarlo según permisos/preferencias.
- Progreso de metas y gráficos de gastos por categoría, con montos coherentes y sin datos de demostración presentados como reales.
- Diseño premium accesible: márgenes que respeten barras de Android, botones distinguibles, iconografía consistente, imágenes 3D livianas y perfiles con fotografía recortable.

## Etapas de entrega

1. Acceso ADMIN/MEMBER real en Android con UI de perfiles y backend local.
2. Actividades y horarios compartidos + gestión de tareas recurrentes.
3. Notas, listas y recordatorios locales (sin nube permanente).
4. Bancos individuales, gastos y comprobantes con permisos y cifrado.
5. Sincronización remota segura, pruebas multi-dispositivo y recuperación.
6. Gráficos, estadísticas, logros, pulido, QA y versión de distribución.

**Nota:** Los registros de prueba no son datos reales. La publicación del código en GitHub es independiente del despliegue de una API de producción.
