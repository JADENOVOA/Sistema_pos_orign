@echo off
rem Respaldo periódico con el Programador de tareas de Windows (ej. diario 23:00).
rem Ajuste la ruta de PostgreSQL y la contraseña.
set PGPASSWORD=CAMBIAR_CLAVE
set PGSSLMODE=require
set DEST=%USERPROFILE%\.bodega_laperuanita\respaldos
if not exist "%DEST%" mkdir "%DEST%"
"C:\Program Files\PostgreSQL\17\bin\pg_dump.exe" -h localhost -U postgres -Fc -f "%DEST%\BODEGA_LAPERUANITA_%date:~-4%%date:~3,2%%date:~0,2%.backup" BODEGA_LAPERUANITA
rem Programar: schtasks /create /tn "Respaldo Bodega" /tr "%~f0" /sc daily /st 23:00
