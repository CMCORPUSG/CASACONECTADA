#requires -Version 5.1
<#
.SYNOPSIS
  Copia este proyecto Android al clon del repositorio CASACONECTADA.
.EXAMPLE
  .\scripts\01_CREAR_PROYECTO_POWERSHELL.ps1 -Destino 'D:\CASACONECTADA'
.EXAMPLE
  .\scripts\01_CREAR_PROYECTO_POWERSHELL.ps1 -Destino 'D:\CASACONECTADA' -Compilar -Publicar
#>
param(
    [string]$Destino = 'D:\CASACONECTADA',
    [switch]$Compilar,
    [switch]$Publicar
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repo = 'https://github.com/CMCORPUSG/CASACONECTADA.git'
$fuente = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$destinoCompleto = [System.IO.Path]::GetFullPath($Destino)
if ($destinoCompleto.TrimEnd('\') -eq $fuente.TrimEnd('\')) {
    throw 'La ruta de destino debe ser diferente a la carpeta extraída del ZIP.'
}
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw 'Git no está disponible. Instala Git for Windows y abre una nueva terminal PowerShell.'
}
if (-not (Test-Path -LiteralPath $destinoCompleto)) {
    Write-Host "Clonando $repo en $destinoCompleto..." -ForegroundColor Cyan
    git clone $repo $destinoCompleto
    if ($LASTEXITCODE -ne 0) { throw 'Error al clonar GitHub.' }
} else {
    if (-not (Test-Path -LiteralPath (Join-Path $destinoCompleto '.git'))) {
        throw "La ruta $destinoCompleto existe, pero no es un clon Git. Usa otra ruta o un clon válido."
    }
    $url = (git -C $destinoCompleto remote get-url origin).Trim()
    if ($LASTEXITCODE -ne 0 -or $url -notmatch 'CMCORPUSG/CASACONECTADA(\.git)?$') {
        throw "El repositorio de destino no es CASACONECTADA: $url"
    }
}
Write-Host 'Copiando código Android y referencias visuales...' -ForegroundColor Cyan
& robocopy.exe $fuente $destinoCompleto /E /XD .git .gradle .idea build /XF local.properties *.apk *.aab /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw "Robocopy terminó con error $LASTEXITCODE" }

# Recupera los archivos oficiales del Gradle Wrapper. Se descargan de gradle/gradle,
# no de sitios de terceros. La primera ejecución necesita conexión a internet.
$base = 'https://raw.githubusercontent.com/gradle/gradle/v8.11.1/'
$descargas = @(
    @{Rel='gradlew'; Url='gradlew'},
    @{Rel='gradlew.bat'; Url='gradlew.bat'},
    @{Rel='gradle/wrapper/gradle-wrapper.jar'; Url='https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar'}
)
foreach ($elemento in $descargas) {
    $ruta = Join-Path $destinoCompleto ($elemento.Rel -replace '/', '\')
    if (-not (Test-Path -LiteralPath $ruta)) {
        Write-Host "Descargando archivo oficial: $($elemento.Rel)" -ForegroundColor Cyan
        New-Item -ItemType Directory -Path (Split-Path $ruta -Parent) -Force | Out-Null
        Invoke-WebRequest -UseBasicParsing -Uri $(if ($elemento.Url.StartsWith('https://')) { $elemento.Url } else { $base + $elemento.Url }) -OutFile $ruta
        if ((Get-Item $ruta).Length -lt 100) { throw "Descarga incompleta: $ruta" }
    }
}
# Comparar el SHA-256 del wrapper JAR con el publicado por Gradle.
$jar = Join-Path $destinoCompleto 'gradle\wrapper\gradle-wrapper.jar'
$shaPublicada = (Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar.sha256').Content.Trim().Split(' ')[0].ToLowerInvariant()
$shaLocal = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()
if ($shaLocal -ne $shaPublicada) { throw 'El SHA-256 del wrapper JAR no coincide con la publicación oficial de Gradle.' }
Write-Host "Proyecto preparado en: $destinoCompleto" -ForegroundColor Green
Write-Host 'Abrir en Android Studio: File > Open > seleccionar la carpeta del proyecto.'

if ($Compilar) {
    Push-Location $destinoCompleto
    try {
        if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
            Write-Warning 'Java no está disponible en PowerShell. Instala JDK 17 o usa Gradle JDK de Android Studio.'
        }
        Write-Host 'Compilando APK de depuración...' -ForegroundColor Cyan
        & .\gradlew.bat assembleDebug --stacktrace
        if ($LASTEXITCODE -ne 0) { throw 'Gradle no compiló. Revisa Android SDK API 35 y el JDK 17 o 21.' }
        Write-Host 'APK: app\build\outputs\apk\debug\app-debug.apk' -ForegroundColor Green
    } finally { Pop-Location }
}

if ($Publicar) {
    if (Get-Command gh -ErrorAction SilentlyContinue) { gh auth setup-git }
    git -C $destinoCompleto add -A
    if ($LASTEXITCODE -ne 0) { throw 'Error al preparar cambios.' }
    git -C $destinoCompleto diff --cached --quiet
    if ($LASTEXITCODE -eq 0) {
        Write-Host 'No hay cambios pendientes de publicar.' -ForegroundColor Yellow
    } else {
        git -C $destinoCompleto commit -m 'feat: initial native Casa Conectada Android app'
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el commit.' }
        git -C $destinoCompleto push -u origin main
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo publicar. Verifica gh auth status o las credenciales Git.' }
        Write-Host 'Cambios publicados en GitHub.' -ForegroundColor Green
    }
}
