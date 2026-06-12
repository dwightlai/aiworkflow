@echo off
cd /d %~dp0
call "%~dp0stop.bat" 18080
echo.
echo Sync aiworkflow_demo from aiworkflow
echo Please stop dev server on 8080 before continue
echo.
python init-db.py
if errorlevel 1 echo failed, run: pip install psycopg2-binary
pause
