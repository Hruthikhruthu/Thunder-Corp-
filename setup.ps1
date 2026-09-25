# ============================================================
# ThunderCore ERP - Setup Script (Windows PowerShell)
# ============================================================
# Usage: .\setup.ps1 -MySQLPassword "YOUR_MYSQL_ROOT_PASSWORD"
# ============================================================

param(
    [Parameter(Mandatory = $true)]
    [string]$MySQLPassword
)

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "`n⚡ ThunderCore ERP - Setup Script" -ForegroundColor Cyan
Write-Host "================================`n" -ForegroundColor Cyan

# ── Step 1: Create MySQL Database ──────────────────────────
Write-Host "[1/6] Creating MySQL database..." -ForegroundColor Yellow
mysql -u root -p"$MySQLPassword" -e "CREATE DATABASE IF NOT EXISTS thundercore_db;" 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✅ Database 'thundercore_db' created/verified" -ForegroundColor Green
}
else {
    Write-Host "  ❌ Failed to create database. Check your MySQL password." -ForegroundColor Red
    Write-Host "  Usage: .\setup.ps1 -MySQLPassword 'YOUR_PASSWORD'" -ForegroundColor Yellow
    exit 1
}

# ── Step 2: Run Schema ─────────────────────────────────────
Write-Host "[2/6] Importing schema and seed data..." -ForegroundColor Yellow
mysql -u root -p"$MySQLPassword" thundercore_db < "$ProjectRoot\database\schema.sql" 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✅ Schema imported successfully" -ForegroundColor Green
}
else {
    Write-Host "  ⚠️  Schema import had warnings (tables may already exist)" -ForegroundColor Yellow
}

# ── Step 3: Update .env with correct password ──────────────
Write-Host "[3/6] Updating .env file..." -ForegroundColor Yellow
$envContent = @"
# ============================================================
# ThunderCore ERP - Environment Variables
# ============================================================

# Database
DB_ROOT_PASSWORD=$MySQLPassword
DB_NAME=thundercore_db
DB_USER=root
DB_PASSWORD=$MySQLPassword

# JWT - 64-char hex secret
JWT_SECRET=4f783b9c025d81a942e6f982348a7b9c45a72b904e1f6d3a82746b1a8d05273f

# Mail (optional)
MAIL_USERNAME=test@example.com
MAIL_PASSWORD=password
"@
Set-Content -Path "$ProjectRoot\.env" -Value $envContent
Write-Host "  ✅ .env updated" -ForegroundColor Green

# ── Step 4: Build Backend ──────────────────────────────────
Write-Host "[4/6] Building backend (Maven)..." -ForegroundColor Yellow
Push-Location "$ProjectRoot\backend"
mvn clean package -DskipTests -B -q 2>$null
if ($LASTEXITCODE -eq 0 -or (Test-Path "target\erp-0.0.1-SNAPSHOT.jar")) {
    Write-Host "  ✅ Backend JAR built successfully" -ForegroundColor Green
}
else {
    Write-Host "  ❌ Backend build failed" -ForegroundColor Red
}
Pop-Location

# ── Step 5: Install Frontend Dependencies ──────────────────
Write-Host "[5/6] Installing frontend dependencies..." -ForegroundColor Yellow
Push-Location "$ProjectRoot\frontend"
npm install --silent 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✅ Frontend dependencies installed" -ForegroundColor Green
}
else {
    Write-Host "  ❌ Frontend install failed" -ForegroundColor Red
}
Pop-Location

# ── Step 6: Done ───────────────────────────────────────────
Write-Host "`n[6/6] Setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║          ThunderCore ERP - Ready to Launch!         ║" -ForegroundColor Cyan
Write-Host "╠══════════════════════════════════════════════════════╣" -ForegroundColor Cyan
Write-Host "║                                                      ║" -ForegroundColor Cyan
Write-Host "║  To start the backend:                               ║" -ForegroundColor Cyan
Write-Host "║    cd backend                                        ║" -ForegroundColor White
Write-Host "║    mvn spring-boot:run                               ║" -ForegroundColor White
Write-Host "║                                                      ║" -ForegroundColor Cyan
Write-Host "║  To start the frontend (new terminal):               ║" -ForegroundColor Cyan
Write-Host "║    cd frontend                                       ║" -ForegroundColor White
Write-Host "║    npm run dev                                       ║" -ForegroundColor White
Write-Host "║                                                      ║" -ForegroundColor Cyan
Write-Host "║  Open browser:                                       ║" -ForegroundColor Cyan
Write-Host "║    http://localhost:5173                              ║" -ForegroundColor White
Write-Host "║                                                      ║" -ForegroundColor Cyan
Write-Host "║  Login:                                              ║" -ForegroundColor Cyan
Write-Host "║    admin@thundercore.com / Admin@123                  ║" -ForegroundColor White
Write-Host "║                                                      ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
