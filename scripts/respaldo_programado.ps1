$ErrorActionPreference = 'Stop'

if (-not $env:BODEGA_POS_HOME) {
    throw 'Defina BODEGA_POS_HOME como la carpeta local de la aplicación portable.'
}

$java = Join-Path $env:BODEGA_POS_HOME 'runtime\bin\java.exe'
$jar = Join-Path $env:BODEGA_POS_HOME 'app\bodega-la-peruanita-pos-1.0.0.jar'
$pgDump = if ($env:BODEGA_PG_DUMP) { $env:BODEGA_PG_DUMP } else { 'C:\Program Files\PostgreSQL\17\bin\pg_dump.exe' }
$destino = if ($env:BODEGA_BACKUP_DIR) { $env:BODEGA_BACKUP_DIR } else { Join-Path $env:USERPROFILE '.bodega_laperuanita\respaldos' }
$archivo = Join-Path $destino ("BODEGA_LAPERUANITA_{0}.dump" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))

foreach ($requerido in @($java, $jar, $pgDump)) {
    if (-not (Test-Path -LiteralPath $requerido -PathType Leaf)) { throw "No se encontró el archivo requerido: $requerido" }
}

& $java -cp $jar util.RespaldoPostgreSQL localhost 5432 pos_backup BODEGA_LAPERUANITA $archivo $pgDump
if ($LASTEXITCODE -ne 0) { throw "Falló el respaldo de PostgreSQL (código $LASTEXITCODE)." }
