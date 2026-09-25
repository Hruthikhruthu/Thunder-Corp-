Stop-Service MySQL80 -Force
Start-Sleep 3

# Start MySQL with skip-grant-tables in background
$mysqld = Start-Process -FilePath "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" -ArgumentList "--skip-grant-tables","--shared-memory" -PassThru -NoNewWindow

Start-Sleep 8

# Reset password
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -e "FLUSH PRIVILEGES; ALTER USER 'root'@'localhost' IDENTIFIED BY 'Thunder@2025'; FLUSH PRIVILEGES;"

Start-Sleep 2

# Kill mysqld process
Stop-Process -Id $mysqld.Id -Force -ErrorAction SilentlyContinue
Start-Sleep 3

# Restart MySQL service properly  
Start-Service MySQL80
Start-Sleep 5

# Test connection
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p"Thunder@2025" -e "SELECT 'PASSWORD_RESET_SUCCESS' AS status;"
