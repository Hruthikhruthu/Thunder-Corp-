# ============================================================
# ThunderCore ERP - Start Backend
# ============================================================
# Usage: .\start-backend.ps1 -MySQLPassword "YOUR_MYSQL_ROOT_PASSWORD"
# ============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$MySQLPassword
)

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "`n⚡ Starting ThunderCore ERP Backend...`n" -ForegroundColor Cyan

$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/thundercore_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = $MySQLPassword
$env:JWT_SECRET = "4f783b9c025d81a942e6f982348a7b9c45a72b904e1f6d3a82746b1a8d05273f"

Push-Location "$ProjectRoot\backend"
mvn spring-boot:run
Pop-Location
