param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

if (-not $SkipBuild) {
    & "$raiz\gradlew.bat" ":lwjgl3:jar" "--console=plain"
    if ($LASTEXITCODE -ne 0) { throw "No se pudo compilar el JAR del juego." }
}

$jar = Get-ChildItem "$raiz\lwjgl3\build\libs" -Filter "*.jar" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $jar) { throw "No existe el JAR. Ejecuta primero :lwjgl3:jar." }

$jpackage = $null
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jpackage.exe")) {
    $jpackage = "$env:JAVA_HOME\bin\jpackage.exe"
}
if ($null -eq $jpackage) {
    $candidato = Get-ChildItem "$env:ProgramFiles\Java\jdk*\bin\jpackage.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1
    if ($candidato) { $jpackage = $candidato.FullName }
}
if ($null -eq $jpackage) {
    $comando = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($comando) { $jpackage = $comando.Source }
}
if ($null -eq $jpackage) {
    throw "No se encontro jpackage. Instala un JDK 17 o superior y define JAVA_HOME."
}

$dist = Join-Path $raiz "dist"
$app = Join-Path $dist "Elemental Escape"
if (Test-Path $app) { Remove-Item -Recurse -Force $app }
New-Item -ItemType Directory -Force -Path $dist | Out-Null

# Carpeta temporal con un unico JAR: evita empaquetar versiones antiguas.
$stage = Join-Path $dist "_jar"
if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
New-Item -ItemType Directory -Force -Path $stage | Out-Null
Copy-Item $jar.FullName (Join-Path $stage $jar.Name)

$icono = Join-Path $raiz "lwjgl3\icons\logo.ico"
$argumentos = @(
    "--type", "app-image",
    "--name", "Elemental Escape",
    "--app-version", "1.2.0",
    "--vendor", "Elemental Escape",
    "--description", "Aventura cooperativa de plataformas para dos jugadores",
    "--input", $stage,
    "--main-jar", $jar.Name,
    "--main-class", "io.github.ElementalEscape.lwjgl3.Lwjgl3Launcher",
    "--dest", $dist,
    "--add-modules", "java.base,java.desktop,java.management,jdk.unsupported",
    "--java-options", "--enable-native-access=ALL-UNNAMED"
)
if (Test-Path $icono) { $argumentos += @("--icon", $icono) }

Write-Host "Generando aplicacion Windows con runtime incluido..."
& $jpackage @argumentos
if ($LASTEXITCODE -ne 0) { throw "jpackage fallo con codigo $LASTEXITCODE." }
if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }

$leeme = @"
ELEMENTAL ESCAPE - DISTRIBUCION WINDOWS
=======================================

Ejecutable:
  dist\Elemental Escape\Elemental Escape.exe

Esta carpeta es portable: incluye el runtime de Java y todas las
dependencias. No es necesario instalar Java en el equipo de destino.

Datos del jugador:
  %USERPROFILE%\.elemental-escape\
  (progreso.json, ajustes.json y niveles personalizados)

Los datos quedan fuera de la carpeta de instalacion, por lo que no se
pierden al actualizar o reemplazar el juego.

Como volver a generar esta distribucion:
  .\gradlew.bat :lwjgl3:distWindows --console=plain

Tambien se puede ejecutar solo el JAR con Java 8 o superior:
  java -jar "lwjgl3\build\libs\$($jar.Name)"
"@
Set-Content -Path (Join-Path $dist "LEEME-DISTRIBUCION.txt") -Value $leeme -Encoding UTF8

$zip = Join-Path $dist "Elemental-Escape-Windows.zip"
if (Test-Path $zip) { Remove-Item -Force $zip }
Compress-Archive -Path $app -DestinationPath $zip -CompressionLevel Optimal

Write-Host ""
Write-Host "Distribucion lista:"
Write-Host "  $app\Elemental Escape.exe"
Write-Host "  $zip"
