param(
    [Parameter(Position=0)]
    [ValidateSet('start','stop','restart','status','log')]
    [string]$Action
)

$APP_DIR   = 'D:\workspace\ruoyi-demo\server'
$APP_JAR   = "$APP_DIR\app.jar"
$PID_FILE  = "$APP_DIR\app.pid"
$LOG_FILE  = "$APP_DIR\logs\startup.log"
$JAVA_OPTS = '-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom -Dserver.port=8080 -Dspring.config.additional-location=D:\workspace\ruoyi-demo\server\application-dev.yml --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED'

function Start-App {
    if (Test-Path $PID_FILE) {
        $procId = Get-Content $PID_FILE
        try {
            $proc = Get-Process -Id $procId -ErrorAction Stop
            Write-Host "Already running (PID: $procId)" -ForegroundColor Red
            return
        } catch {}
    }
    Write-Host 'Starting RuoYi-Flowable-Plus...' -ForegroundColor Green
    $startInfo = @{
        FilePath               = 'java'
        ArgumentList           = "$JAVA_OPTS -jar $APP_JAR"
        WorkingDirectory       = $APP_DIR
        RedirectStandardOutput = $LOG_FILE
        RedirectStandardError  = "$APP_DIR\logs\startup-error.log"
        WindowStyle            = 'Hidden'
        PassThru               = $true
    }
    $process = Start-Process @startInfo
    $process.Id | Out-File -FilePath $PID_FILE -Encoding ASCII
    Write-Host "Started (PID: $($process.Id))" -ForegroundColor Green
}

function Stop-App {
    if (Test-Path $PID_FILE) {
        $procId = Get-Content $PID_FILE
        try {
            $proc = Get-Process -Id $procId -ErrorAction Stop
            Write-Host "Stopping (PID: $procId)..." -ForegroundColor Red
            Stop-Process -Id $procId
            Start-Sleep -Seconds 3
        } catch {
            Write-Host 'Process not found' -ForegroundColor Yellow
        }
        Remove-Item $PID_FILE -Force
    }
}

function Status-App {
    if (Test-Path $PID_FILE) {
        $procId = Get-Content $PID_FILE
        try {
            Get-Process -Id $procId -ErrorAction Stop | Out-Null
            Write-Host "Running (PID: $procId)" -ForegroundColor Green
            return
        } catch {}
    }
    Write-Host 'Not running' -ForegroundColor Red
}

switch ($Action) {
    'start'   { Start-App }
    'stop'    { Stop-App }
    'restart' { Stop-App; Start-Sleep 2; Start-App }
    'status'  { Status-App }
    'log'     { Get-Content $LOG_FILE -Wait -Tail 50 }
    default   { Write-Host 'Usage: .\app.ps1 {start|stop|restart|status|log}' }
}