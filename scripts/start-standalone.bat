@echo off
cd /d %~dp0

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":18080" ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1

set SPRING_PROFILES_ACTIVE=standalone
set SERVER_PORT=18080
set POSTGRES_JDBC_URL=jdbc:postgresql://localhost:5432/aiworkflow_demo
set POSTGRES_USERNAME=aiworkflow
set POSTGRES_PASSWORD=aiworkflow
set NACOS_DISCOVERY_ENABLED=false

echo.
echo AI Workflow Demo
echo   URL : http://localhost:18080
echo   User: admin / admin123
echo   DB  : aiworkflow_demo
echo.

java -jar aiworkflow-server.jar
pause
