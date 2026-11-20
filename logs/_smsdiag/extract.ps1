$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$out = 'D:\workspace\RuoYi-Flowable-Plus-0.8.X\logs\_smsdiag\deployed'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$zip = [System.IO.Compression.ZipFile]::OpenRead('D:\workspace\ruoyi-demo\server\app.jar')
$smsEntry = $zip.Entries | Where-Object { $_.FullName -eq 'BOOT-INF/lib/ruoyi-sms-0.8.3.jar' }
Write-Host ("nested ruoyi-sms jar entry: " + ($null -ne $smsEntry))
if ($null -ne $smsEntry) {
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($smsEntry, "$out\ruoyi-sms.jar", $true)
}
$zip.Dispose()
if (Test-Path "$out\ruoyi-sms.jar") {
    $zip2 = [System.IO.Compression.ZipFile]::OpenRead("$out\ruoyi-sms.jar")
    $cls = $zip2.Entries | Where-Object { $_.FullName -like 'com/ruoyi/sms/numberauth/*.class' }
    foreach ($e in $cls) {
        $name = $e.FullName -replace '.*/', ''
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($e, "$out\$name", $true)
        Write-Host ("extracted: " + $name)
    }
    $zip2.Dispose()
}
