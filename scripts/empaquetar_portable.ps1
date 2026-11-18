$ErrorActionPreference = 'Stop'

$javaVersion = (& java -version 2>&1 | Out-String)
if ($javaVersion -notmatch 'version "(\d+)') {
    throw 'No se pudo determinar la versión de Java. Compruebe: java -version'
}
$javaMajor = [int]$Matches[1]
if ($javaMajor -lt 17) {
    throw 'Se requiere JDK 17 o superior para compilar y empaquetar el POS.'
}

$jpackageCommand = Get-Command jpackage -ErrorAction SilentlyContinue
$jpackageExe = if ($jpackageCommand) { $jpackageCommand.Source } else { $null }
if (-not $jpackageExe -or -not (Test-Path -LiteralPath $jpackageExe)) {
    $jdkRoots = @()
    if ($env:JAVA_HOME) { $jdkRoots += $env:JAVA_HOME }
    if ($env:ProgramFiles) { $jdkRoots += (Join-Path $env:ProgramFiles "Java\jdk-$javaMajor") }
    foreach ($jdkRoot in $jdkRoots) {
        $candidate = Join-Path $jdkRoot 'bin\jpackage.exe'
        if (Test-Path -LiteralPath $candidate) { $jpackageExe = $candidate; break }
    }
}
if (-not $jpackageExe -or -not (Test-Path -LiteralPath $jpackageExe)) {
    throw "No se encontró jpackage del JDK $javaMajor. Configure JAVA_HOME con un JDK completo."
}
$jpackageVersion = (& $jpackageExe --version | Out-String).Trim()
if ($jpackageVersion -notmatch '^(\d+)' -or [int]$Matches[1] -ne $javaMajor) {
    throw "La versión de jpackage debe coincidir con Java $javaMajor (encontrada: $jpackageVersion)."
}
$jdkHome = Split-Path (Split-Path $jpackageExe -Parent) -Parent
$javaExe = Join-Path $jdkHome 'bin\java.exe'
$javawExe = Join-Path $jdkHome 'bin\javaw.exe'
if (-not (Test-Path -LiteralPath $javaExe)) {
    throw "No se encontró java.exe en el JDK: $jdkHome"
}

$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$jar = Join-Path $project 'target\bodega-la-peruanita-pos-1.0.0.jar'
$marcaTiempo = Get-Date -Format 'yyyyMMdd-HHmmss'
$destino = Join-Path $project "dist\portable\build-$marcaTiempo"
$entradaPortable = Join-Path $project "target\portable-input-$marcaTiempo"

Push-Location $project
try {
    & .\mvnw.cmd package
    if ($LASTEXITCODE -ne 0) { throw 'Falló la generación del JAR.' }
    if (-not (Test-Path -LiteralPath $jar)) { throw "No se encontró el JAR esperado: $jar" }

    New-Item -ItemType Directory -Force -Path $entradaPortable | Out-Null
    Copy-Item -LiteralPath $jar -Destination $entradaPortable
    New-Item -ItemType Directory -Force -Path $destino | Out-Null
    & $jpackageExe --type app-image --name BodegaLaPeruanitaPOS --app-version 1.0.0 `
        --input $entradaPortable --main-jar (Split-Path $jar -Leaf) `
        --main-class vistas.FrmLogin --dest $destino
    if ($LASTEXITCODE -ne 0) { throw "Falló jpackage $jpackageVersion. Revise la salida de jpackage." }
    $carpetaApp = Join-Path $destino 'BodegaLaPeruanitaPOS'
    Copy-Item -LiteralPath $javaExe -Destination (Join-Path $carpetaApp 'runtime\bin\java.exe')
    if (Test-Path -LiteralPath $javawExe) {
        Copy-Item -LiteralPath $javawExe -Destination (Join-Path $carpetaApp 'runtime\bin\javaw.exe')
    }
    @(
        'db.url=jdbc:postgresql://localhost:5432/BODEGA_LAPERUANITA'
        'db.sslmode=disable'
        'db.admin.user=postgres'
        'db.bootstrap=true'
        'mail.smtp.host=smtp.gmail.com'
        'mail.smtp.port=587'
        'mail.smtp.user=marx62953@gmail.com'
        'mail.smtp.from=marx62953@gmail.com'
        'mail.smtp.starttls=true'
        'mail.smtp.ssl=false'
    ) | Set-Content -LiteralPath (Join-Path $carpetaApp 'bodega.properties') -Encoding ascii
    Write-Host "Aplicación portable creada en: $carpetaApp"
    Write-Host "Runtime incluido: Java $jpackageVersion. No se copiaron contraseñas ni secretos."
} finally {
    Pop-Location
}
