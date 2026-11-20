param(
    [Parameter(Position=0)]
    [ValidateSet('start','stop','restart','status','log','start-all','stop-all','restart-all','status-all')]
    [string]$Action,

    [Parameter(Position=1)]
    [ValidateSet('app','monitor','job','all')]
    [string]$Service = 'all'
)

# Resolve deploy dir from script location so it works anywhere
$APP_DIR = $PSScriptRoot

# License RSA private key (used by application-prod.yml hard-mode signing)
$env:LICENSE_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDNfbwA2c/bJCpH0t44MscwfmR6SpIIkP7FZ8dDq3eQtv1W305AA8xC5auq0y8O+Z2dqxGm0HUi6ZhOR72gAxdcS+gBc3dbdKFtukxDhFzPVQWUEXlGvxSN6wL6+mv2BQ3d4tLoETTrLWFXO7fY/k+GR0P3/dDcNzv9wHJlZfHiVkcacwy4v9Fql139drXWHnPD2oC+6oFa14zzqtNkVOPFsR/gGMzrzszvZPtV7oTbQNRDB98kAdCQbRcSkSgxUdbi8/CwJwgejS3Z+TIVrYiFI64blj3feEyF4XkyGgekNPFPK5GXZZW92//3PQcCi6jQuh25YUdkdFJGRadTIxSbAgMBAAECggEAW8ngIpkoiYCcCj0c2gmEhBrREvcgSAdMZi03CvkajV/PxfA1ac7IfNFOXx26FXxWdUkmt1bCG/gv+jd9cm4vDyEw3wUOuaFdQkBHNJl5Q3t85GjxvTI5IjNpv2YAt4IGcaiSAOs+shYxNuAL/TeLmjVXHP0ZQGTDvVMrRjPFWg3WzWZCQUuJrN1UqtMHDGtfvRHD4+/ilSSUP2zlfUqGMgUmbQm8z541n+90IeWa0ksl1m7bMTUn8nxz2p6bPgxNqKmsL9O0+7/17M8Jxa63umdcLsgl0K7tK1glV5gtITAHpdo0f0TTFLSom8S2IImW2O1ksn7Kj/I4mfNWi3TcMQKBgQDwoE7eUbN5QVBrHs6ISFU4/68bi7f7+sG9nG1oDpl4j2Idm26DmFbPM2Q1LYE4wJwSUwl1HzCXi6Fy6aGp0niKrtlY8XtT5/z3X/xzXyyBjLgXV8VJc27fSkKGWhZ5r/Oq22qbFmeyX9jzYbAqQe2vu0dA1FSVX73xvPrnazl4nwKBgQDansGGDGZWyyM4jFofHKzOms9oTF4ICJcnOkQ/9U9e3AgH5fkq1EpiUVKS5o/rybfNjfBxZiVJZHY8J8tuPTPCE8DcrU3dChQFCxNEVGGaXZuBlVe1m8KlcXGA+Byy3Qkkikyv1OoRqsuB1Tgxy4QKcl7bkQPWE4CFVWZhHv5WhQKBgBE962jSweF2ZKTIWFQ7mp+y8oEuycgsd6V2iABky2pwG7+zW3/icpX2QL/uusuuQYCuoobZ7Ulun+r0ArmAc1K8pKZS51tjzCSNpDFAKJg/1REXIPPaaZ9mWs31tpaRnQwpHMJwII4PgCQwY8cjYjytVrW2gTkS2zFvMStT65JRAoGBALRU3dcf5U7Us1HS3Afr1FN7+XfT/oBr7ZrLAj8rr3PU1b8GIPkGMfFVmWJZUwAw3S9uVJaFpl4awzitrq1ZSJdfSef+AEtDI+5hC1yCqhv1+O7z7Gx+dhmYMQMsJigYEZGVjto/K3C1TLbk1Tyl3l3rp3k/7kUowwu4vQGB2lQtAoGBALt7dsX42ijI8bErwQD5WSM4JKBu9NC5HzmY1m2XKp1pHoD4q0/savx+oFO5dZaugEdXZrS1ObKm8zMftEutqu7PrbONi5recwm1kTowN88VGwIPqYS5eLVVOFOSnUe7WEgQKoaemYWxGqwxnDafppkP/szrWFe66Nq9L+u9+kD8"

$SERVICES = @{
    'app' = @{
        Jar      = "$APP_DIR\app.jar"
        PidFile  = "$APP_DIR\app.pid"
        LogFile  = "$APP_DIR\logs\app.log"
        ErrFile  = "$APP_DIR\logs\app-error.log"
        JavaOpts = "-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom -Dserver.port=8080 -Dserver.forward-headers-strategy=native -Dspring.profiles.active=prod -Dspring.config.additional-location=$APP_DIR\application-prod.yml --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"
        Name     = 'Main-App(8080)'
    }
    'monitor' = @{
        Jar      = "$APP_DIR\ruoyi-monitor-admin.jar"
        PidFile  = "$APP_DIR\monitor.pid"
        LogFile  = "$APP_DIR\logs\monitor.log"
        ErrFile  = "$APP_DIR\logs\monitor-error.log"
        JavaOpts = '-Xms256m -Xmx512m -Dserver.port=9090 -Dserver.forward-headers-strategy=native --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED'
        Name     = 'Monitor-Admin(9090)'
    }
    'job' = @{
        Jar      = "$APP_DIR\ruoyi-xxl-job-admin.jar"
        PidFile  = "$APP_DIR\job.pid"
        LogFile  = "$APP_DIR\logs\xxl-job.log"
        ErrFile  = "$APP_DIR\logs\xxl-job-error.log"
        JavaOpts = "-Xms256m -Xmx512m -Dserver.port=9100 -Dserver.forward-headers-strategy=native -Dspring.profiles.active=prod -Dspring.config.additional-location=$APP_DIR\application-prod.yml --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"
        Name     = 'XXL-Job-Admin(9100)'
    }
}

function Get-Targets {
    if ($Service -eq 'all') { return @('app','monitor','job') }
    return @($Service)
}

function Start-Svc {
    param([string]$Key)
    $svc = $SERVICES[$Key]
    if (Test-Path $svc.PidFile) {
        $procId = Get-Content $svc.PidFile
        try {
            Get-Process -Id $procId -ErrorAction Stop | Out-Null
            Write-Host "  [$($svc.Name)] Already running (PID: $procId)" -ForegroundColor Yellow
            return
        } catch {}
    }
    Write-Host "  Starting $($svc.Name) ..." -ForegroundColor Green
    $startInfo = @{
        FilePath               = 'java'
        ArgumentList           = "$($svc.JavaOpts) -jar $($svc.Jar)"
        WorkingDirectory       = $APP_DIR
        RedirectStandardOutput = $svc.LogFile
        RedirectStandardError  = $svc.ErrFile
        WindowStyle            = 'Hidden'
        PassThru               = $true
    }
    $process = Start-Process @startInfo
    $process.Id | Out-File -FilePath $svc.PidFile -Encoding ASCII
    Write-Host "  [$($svc.Name)] Started (PID: $($process.Id))" -ForegroundColor Green
}

function Stop-Svc {
    param([string]$Key)
    $svc = $SERVICES[$Key]
    if (Test-Path $svc.PidFile) {
        $procId = Get-Content $svc.PidFile
        try {
            Get-Process -Id $procId -ErrorAction Stop | Out-Null
            Write-Host "  [$($svc.Name)] Stopping (PID: $procId) ..." -ForegroundColor Red
            Stop-Process -Id $procId
            Start-Sleep -Seconds 3
        } catch {
            Write-Host "  [$($svc.Name)] Process not found" -ForegroundColor Yellow
        }
        Remove-Item $svc.PidFile -Force
    } else {
        Write-Host "  [$($svc.Name)] PID file not found" -ForegroundColor Yellow
    }
}

function Status-Svc {
    param([string]$Key)
    $svc = $SERVICES[$Key]
    if (Test-Path $svc.PidFile) {
        $procId = Get-Content $svc.PidFile
        try {
            Get-Process -Id $procId -ErrorAction Stop | Out-Null
            Write-Host "  [$($svc.Name)] Running (PID: $procId)" -ForegroundColor Green
            return
        } catch {}
    }
    Write-Host "  [$($svc.Name)] Not running" -ForegroundColor Red
}

function Show-Log {
    param([string]$Key)
    $svc = $SERVICES[$Key]
    Write-Host "Tailing: $($svc.LogFile)" -ForegroundColor Cyan
    Get-Content $svc.LogFile -Wait -Tail 50
}

switch ($Action) {
    'start'       { foreach ($t in (Get-Targets)) { Start-Svc $t } }
    'stop'        { foreach ($t in (Get-Targets)) { Stop-Svc $t } }
    'restart'     { foreach ($t in (Get-Targets)) { Stop-Svc $t }; Start-Sleep 2; foreach ($t in (Get-Targets)) { Start-Svc $t } }
    'status'      { foreach ($t in (Get-Targets)) { Status-Svc $t } }
    'log'         { Show-Log $Service }
    'start-all'   { foreach ($t in @('app','monitor','job')) { Start-Svc $t } }
    'stop-all'    { foreach ($t in @('app','monitor','job')) { Stop-Svc $t } }
    'restart-all' { foreach ($t in @('app','monitor','job')) { Stop-Svc $t }; Start-Sleep 2; foreach ($t in @('app','monitor','job')) { Start-Svc $t } }
    'status-all'  { foreach ($t in @('app','monitor','job')) { Status-Svc $t } }
    default {
        Write-Host ''
        Write-Host 'Usage: .\app.ps1 <command> [service]' -ForegroundColor Cyan
        Write-Host ''
        Write-Host 'Commands:  start | stop | restart | status | log | start-all | stop-all | restart-all | status-all'
        Write-Host 'Services:  app | monitor | job | all (default)'
        Write-Host ''
        Write-Host 'Examples:' -ForegroundColor Yellow
        Write-Host '  .\app.ps1 start-all'
        Write-Host '  .\app.ps1 start app'
        Write-Host '  .\app.ps1 status-all'
        Write-Host '  .\app.ps1 log monitor'
        Write-Host '  .\app.ps1 restart job'
    }
}