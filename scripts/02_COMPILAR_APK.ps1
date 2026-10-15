#requires -Version 5.1
$ErrorActionPreference = 'Stop'
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not (Test-Path '.\gradlew.bat')) {
    throw 'Falta Gradle Wrapper. Ejecuta primero 01_CREAR_PROYECTO_POWERSHELL.ps1.'
}
& .\gradlew.bat --version
if ($LASTEXITCODE -ne 0) { throw 'Gradle no se pudo iniciar.' }
& .\gradlew.bat clean assembleDebug --stacktrace
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de Android.' }
$apk = Join-Path (Get-Location).Path 'app\build\outputs\apk\debug\app-debug.apk'
if (Test-Path $apk) { Write-Host "APK creada: $apk" -ForegroundColor Green }
