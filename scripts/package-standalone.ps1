param(
    [string]$TargetDir = "D:\aiworkflow"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

Write-Host "==> Build backend jar"
Push-Location (Join-Path $Root "server")
mvn -q clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
$Jar = Get-ChildItem "target\aiworkflow-server-*.jar" | Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1
if (-not $Jar) { throw "Jar not found" }
Pop-Location

Write-Host "==> Build admin frontend"
Push-Location (Join-Path $Root "web")
if (-not (Test-Path "node_modules")) {
    pnpm install
    if ($LASTEXITCODE -ne 0) { throw "pnpm install failed" }
}
pnpm --filter @aiworkflow/admin build
if ($LASTEXITCODE -ne 0) { throw "frontend build failed" }
Pop-Location

Write-Host "==> Deploy to $TargetDir"
New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
$WebDir = Join-Path $TargetDir "web"
if (Test-Path $WebDir) { Remove-Item $WebDir -Recurse -Force }
Copy-Item -Recurse (Join-Path $Root "web\apps\admin\dist") $WebDir
Copy-Item $Jar.FullName (Join-Path $TargetDir "aiworkflow-server.jar") -Force

$batFiles = @(
    @{ Source = "scripts\start-standalone.bat"; Target = "start.bat" },
    @{ Source = "scripts\init-db-standalone.bat"; Target = "init-db.bat" },
    @{ Source = "scripts\stop-standalone.bat"; Target = "stop.bat" }
)
foreach ($item in $batFiles) {
    $content = Get-Content (Join-Path $Root $item.Source) -Raw
    [System.IO.File]::WriteAllText((Join-Path $TargetDir $item.Target), $content, [System.Text.Encoding]::Default)
}

@'
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$env:SPRING_PROFILES_ACTIVE = "standalone"
$env:SERVER_PORT = "18080"
$env:POSTGRES_JDBC_URL = "jdbc:postgresql://localhost:5432/aiworkflow_demo"
$env:POSTGRES_USERNAME = "aiworkflow"
$env:POSTGRES_PASSWORD = "aiworkflow"
$env:NACOS_DISCOVERY_ENABLED = "false"
Write-Host ""
Write-Host "AI Workflow 演示环境"
Write-Host "  地址: http://localhost:18080"
Write-Host "  账号: admin / admin123"
Write-Host "  数据库: aiworkflow_demo (与开发库 aiworkflow 隔离)"
Write-Host ""
java -jar (Join-Path $PSScriptRoot "aiworkflow-server.jar")
'@ | Set-Content -Encoding UTF8 (Join-Path $TargetDir "start.ps1")

Copy-Item (Join-Path $Root "scripts\init-demo-db.py") (Join-Path $TargetDir "init-db.py") -Force

Write-Host "Done: $TargetDir"
Write-Host "  1. 首次运行 init-db.bat"
Write-Host "  2. 运行 start.bat"
