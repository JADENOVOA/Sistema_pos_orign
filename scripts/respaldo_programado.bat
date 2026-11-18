@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0respaldo_programado.ps1"
exit /b %errorlevel%
