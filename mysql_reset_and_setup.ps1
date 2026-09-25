# MySQL Password Reset via my.ini modification
# Must run as Administrator

$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin"
$myIni = "C:\ProgramData\MySQL\MySQL Server 8.0\my.ini"
$newPassword = "Thunder@2025"
$logFile = "C:\Users\admin\OneDrive\Desktop\thunder core project\thundercore-erp\mysql_setup.log"

"[$(Get-Date)] === MY.INI METHOD ===" | Out-File $logFile

# Step 1: Read current my.ini
"[$(Get-Date)] Reading my.ini..." | Out-File $logFile -Append
$content = Get-Content $myIni -Raw
"[$(Get-Date)] my.ini size: $($content.Length) chars" | Out-File $logFile -Append

# Step 2: Add skip-grant-tables under [mysqld]
"[$(Get-Date)] Adding skip-grant-tables..." | Out-File $logFile -Append
$modified = $content -replace '(\[mysqld\])', "`$1`r`nskip-grant-tables"
Set-Content $myIni -Value $modified

# Step 3: Restart MySQL service
"[$(Get-Date)] Restarting MySQL80..." | Out-File $logFile -Append
Restart-Service MySQL80 -Force
Start-Sleep 10
"[$(Get-Date)] MySQL80 status: $((Get-Service MySQL80).Status)" | Out-File $logFile -Append

# Step 4: Reset password
"[$(Get-Date)] Resetting password..." | Out-File $logFile -Append
$r1 = & "$mysqlBin\mysql.exe" -u root -e "FLUSH PRIVILEGES;" 2>&1
"[$(Get-Date)] Flush: $r1" | Out-File $logFile -Append

$r2 = & "$mysqlBin\mysql.exe" -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$newPassword';" 2>&1
"[$(Get-Date)] Alter: $r2" | Out-File $logFile -Append

$r3 = & "$mysqlBin\mysql.exe" -u root -e "FLUSH PRIVILEGES;" 2>&1
"[$(Get-Date)] Flush2: $r3" | Out-File $logFile -Append

# Step 5: Remove skip-grant-tables from my.ini
"[$(Get-Date)] Removing skip-grant-tables..." | Out-File $logFile -Append
$cleaned = $content  # Restore original content
Set-Content $myIni -Value $cleaned

# Step 6: Restart MySQL normally
"[$(Get-Date)] Restarting MySQL80 normally..." | Out-File $logFile -Append
Restart-Service MySQL80 -Force
Start-Sleep 10
"[$(Get-Date)] MySQL80 status: $((Get-Service MySQL80).Status)" | Out-File $logFile -Append

# Step 7: Test connection
"[$(Get-Date)] Testing connection..." | Out-File $logFile -Append
$test = & "$mysqlBin\mysql.exe" -u root -p"$newPassword" -e "SELECT 'PASSWORD_RESET_SUCCESS' AS result;" 2>&1
"[$(Get-Date)] Test: $test" | Out-File $logFile -Append

# Step 8: Create database
"[$(Get-Date)] Creating database..." | Out-File $logFile -Append
$db = & "$mysqlBin\mysql.exe" -u root -p"$newPassword" -e "CREATE DATABASE IF NOT EXISTS thundercore_db; SHOW DATABASES LIKE 'thunder%';" 2>&1
"[$(Get-Date)] DB: $db" | Out-File $logFile -Append

# Step 9: Import schema
$schemaFile = "C:\Users\admin\OneDrive\Desktop\thunder core project\thundercore-erp\database\schema.sql"
if (Test-Path $schemaFile) {
    "[$(Get-Date)] Importing schema..." | Out-File $logFile -Append
    $imp = Get-Content $schemaFile -Raw | & "$mysqlBin\mysql.exe" -u root -p"$newPassword" thundercore_db 2>&1
    "[$(Get-Date)] Import: $imp" | Out-File $logFile -Append
}

"[$(Get-Date)] === DONE ===" | Out-File $logFile -Append
