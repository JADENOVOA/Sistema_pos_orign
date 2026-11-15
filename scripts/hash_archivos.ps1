# Caso 20: calcula SHA-256 del .jar y de la configuración (ejecutar antes y después de modificar un archivo)
Get-FileHash -Algorithm SHA256 .\target\bodega-la-peruanita-pos-1.0.0.jar, .\bodega.properties | Format-Table Hash, Path -AutoSize
