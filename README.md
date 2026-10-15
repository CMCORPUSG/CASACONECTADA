# CASA CONECTADA · Android nativo (base V0.1)

> **Estado real: código inicial, NO V1.0 terminada y NO APK verificada.**
> La referencia de diseño aprobada es `docs/referencia` y proviene del ZIP *Casa_Conectada_Premium_Facil_V2*.

Proyecto Android nativo en **Kotlin + Jetpack Compose + Material 3**, con **MVVM**,
principio de inversión de dependencias (dominio/repositorio), **Hilt**, **Room/SQLite**
y almacenamiento de imágenes privado. No usa WebView, Firebase, servicios externos ni suscripciones.

## 1. Crear el proyecto en Windows con PowerShell

Requisitos: Windows, Git for Windows, Android Studio con **Android SDK 35** instalado,
**JDK 17 o 21**, conexión para resolver dependencias iniciales y cuenta GitHub autorizada
solo si vas a publicar. La primera ejecución descarga el wrapper oficial de Gradle 8.11.1.

1. Descomprime este ZIP, por ejemplo en `Descargas\CASA_CONECTADA_V1_BASE`.
2. Abre PowerShell en la carpeta descomprimida.
3. Ejecuta:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\01_CREAR_PROYECTO_POWERSHELL.ps1 -Destino 'D:\CASACONECTADA'
```

4. Abre `D:\CASACONECTADA` en Android Studio, espera **Gradle Sync**.
5. Si lo deseas, en el terminal de PowerShell:

```powershell
cd D:\CASACONECTADA
.\gradlew.bat assembleDebug
```

6. La APK, **si la compilación tiene éxito**, quedará en:

`D:\CASACONECTADA\app\build\outputs\apk\debug\app-debug.apk`

7. Para publicar en tu repositorio GitHub, **después de revisar y probar**:

```powershell
cd D:\CASACONECTADA
.\scripts\03_PUBLICAR_GITHUB.ps1
```

También puedes hacer las tres acciones de una vez pasando los modificadores `-Compilar -Publicar`
al script 01, pero recomendamos revisar la compilación antes del push.

> No se ejecutó una compilación Android real en el entorno en que se preparó el proyecto:
> aquí no había Android SDK ni acceso a los repositorios de dependencias. La verificación
> final de la APK y las pruebas en dispositivo quedan pendientes.

## 2. Características realmente implementadas en código

| Módulo | Base implementada | Por completar para V1.0 |
|---|---|---|
| Identidad visual | Paleta original, icono original, navegación nativa en seis áreas | Pulido visual y accesibilidad con dispositivo |
| Inicio | Resúmenes calculados con datos locales reales | Avisos, estadísticas más profundas |
| Familia | Crear integrantes locales y visualizar puntos | Identidad verificable, invitaciones QR, autorización |
| Tareas | Crear, asignar a un integrante, fecha, frecuencia registrada, completar con evidencia fotográfica | Varias personas por tarea, rotaciones y repetición automática, historial completo |
| Calendario | Lista cronológica agrupada por fecha | Vistas semanal/mensual, filtros |
| Finanzas | Crear pagos y gastos, montos en céntimos, marcar pago, foto opcional | Historial detallado, pagos periódicos automáticos |
| Metas | Crear meta, añadir Yape/Plin/bancos, copiar datos, aportes con comprobante opcional, confirmación manual y saldo confirmado | Autorización de quien confirma, historial auditable, QR bancario |
| Seguridad | Cifrado de número/cuenta/CCI con AES-GCM y Android Keystore, imágenes privadas, backup automático deshabilitado | Cifrado integral, roles, controles de sesión, auditoría y pentesting |
| Offline | Room SQLite, datos no dependen de internet para uso básico | Copias de seguridad cifradas |
| Sincronización | Documento de arquitectura | Backend Ktor, invitaciones y sincronización real entre celulares |
| Notificaciones | Sin implementación | Recordatorios locales/remotos reales |
| APK | Archivos Gradle para compilar localmente | Compilación y QA reales |

**No existen usuarios remotos ni seguridad entre familiares en esta base**. No almacenar
información financiera familiar real de producción hasta implementar autenticación,
autorización y pruebas de recuperación. Los métodos de aporte se cifran en el teléfono,
pero aún no se controla qué integrante puede verlos.

## 3. Arquitectura

```text
app/src/main/java/pe/casaconectada/
├── MainActivity.kt
├── CasaApplication.kt
├── domain/
│   ├── model/Models.kt
│   └── repository/FamilyRepository.kt
├── data/
│   ├── local/Entities.kt, FamilyDao.kt, FamilyDatabase.kt, EvidenceStore.kt
│   └── repository/RoomFamilyRepository.kt
├── di/DataModule.kt
├── security/SensitiveValueCipher.kt
├── design/Theme.kt
└── presentation/CasaApp.kt, FamilyViewModel.kt
```

- **Domain** no importa Android ni Room; contiene los modelos y contratos.
- **Data/Infrastructure** aplica la persistencia SQLite mediante Room.
- **Presentation** usa Jetpack Compose y ViewModel/StateFlow.
- Los montos se representan como **Long en céntimos** para evitar errores binarios monetarios.
- **Únicamente** los aportes `CONFIRMADO` forman parte del balance ahorrado.
- Los perfiles se crean manualmente en Familia; no se cargan nombres de ejemplo a la base.

## 4. Referencia visual

- `docs/referencia/prototipo_casa_conectada.html`: prototipo original (NO usado como WebView).
- `docs/referencia/assets/design_tokens.json`: colores y medidas.
- `docs/referencia/assets/logo_casa_conectada.svg`: logo SVG editable.
- `docs/referencia/previews/`: vistas de referencia.

## 5. Fases siguientes

1. Cerrar fases 1 y 2 con compilación, QA visual, Room migrations, turnos reales y calendario.
2. Completar métodos financieros, QR, permisos, historial y evidencias.
3. Servidor Ktor+SQLite autohospedable, identidad/invitaciones, TLS, sincronización.
4. Recordatorios y estadísticas.
5. Exportar/importar backups cifrados, tests automatizados, APK firmada.

Véase `docs/PLAN_TECNICO.md` para límites y decisiones de sincronización.
