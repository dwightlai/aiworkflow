@echo off
cd /d %~dp0
set PORT=18080
if not "%~1"=="" set PORT=%~1
echo Free port %PORT% ...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
  echo kill PID %%a
  taskkill /F /PID %%a >nul 2>&1
)
echo Done.
if "%~1"=="" pause
