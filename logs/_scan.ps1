$f = 'D:\workspace\RuoYi-Flowable-Plus-0.8.X\docs\' + [char]0x6700 + [char]0x65B0 + [char]0x6574 + [char]0x7406 + [char]0x597D + [char]0x7684 + [char]0x9700 + [char]0x6C42 + '\' + [char]0x8D44 + [char]0x91D1 + [char]0x7BA1 + [char]0x63A7 + [char]0x7CFB + [char]0x7EDF + [char]0x5F00 + [char]0x53D1 + [char]0x4EFB + [char]0x52A1 + [char]0x6587 + [char]0x6863 + '.md'
$lines = [System.IO.File]::ReadAllLines($f, [System.Text.Encoding]::UTF8)

$OK = [char]::ConvertFromUtf32(0x2705)
$YL = [char]::ConvertFromUtf32(0x1F7E1)
$RD = [char]::ConvertFromUtf32(0x1F504)
$NW = [char]::ConvertFromUtf32(0x1F195)
$H2 = '## ' + [char]0x4E8C

$rows = New-Object System.Collections.ArrayList
$ms = ''
$inch2 = $false
foreach ($ln in $lines) {
    if ($ln.StartsWith('## ')) {
        $inch2 = $ln.StartsWith($H2)
        continue
    }
    if (-not $inch2) { continue }
    if ($ln.StartsWith('### M')) {
        $p = $ln.IndexOf(' ', 4)
        if ($p -lt 0) { $p = $ln.Length }
        $ms = $ln.Substring(4, $p - 4)
        continue
    }
    if ($ln.StartsWith('| ') -and $ms -ne '') {
        $rest = $ln.Substring(2)
        $sp = $rest.IndexOf(' ')
        if ($sp -lt 1) { continue }
        $mark = $rest.Substring(0, $sp)
        $tid = $rest.Substring($sp + 1)
        $sp2 = $tid.IndexOf(' ')
        if ($sp2 -gt 0) { $tid = $tid.Substring(0, $sp2) }
        if ($mark -eq $OK -or $mark -eq $YL -or $mark -eq $RD -or $mark -eq $NW) {
            [void]$rows.Add(@{ ms = $ms; mark = $mark; tid = $tid })
        }
    }
}

Write-Output ('TOTAL=' + $rows.Count)
Write-Output ('OK=' + @($rows | Where-Object { $_.mark -eq $OK }).Count + ' YL=' + @($rows | Where-Object { $_.mark -eq $YL }).Count + ' RD=' + @($rows | Where-Object { $_.mark -eq $RD }).Count + ' NW=' + @($rows | Where-Object { $_.mark -eq $NW }).Count)

Write-Output '=== REDO ==='
$s = ''
foreach ($r in $rows) { if ($r.mark -eq $RD) { $s = $s + $r.tid + ' ' } }
Write-Output $s

Write-Output '=== NEW ==='
$s = ''
foreach ($r in $rows) { if ($r.mark -eq $NW) { $s = $s + $r.ms + ':' + $r.tid + ' ' } }
Write-Output $s

Write-Output '=== YELLOW ==='
$s = ''
foreach ($r in $rows) { if ($r.mark -eq $YL) { $s = $s + $r.ms + ':' + $r.tid + ' ' } }
Write-Output $s

Write-Output '=== PER-MS ==='
$seen = New-Object System.Collections.ArrayList
foreach ($r in $rows) { if (-not $seen.Contains($r.ms)) { [void]$seen.Add($r.ms) } }
foreach ($k in $seen) {
    $sub = @($rows | Where-Object { $_.ms -eq $k })
    $cOk = @($sub | Where-Object { $_.mark -eq $OK }).Count
    $cYl = @($sub | Where-Object { $_.mark -eq $YL }).Count
    $cRd = @($sub | Where-Object { $_.mark -eq $RD }).Count
    $cNw = @($sub | Where-Object { $_.mark -eq $NW }).Count
    Write-Output ($k + ' OK=' + $cOk + ' YL=' + $cYl + ' RD=' + $cRd + ' NW=' + $cNw + ' SUM=' + $sub.Count)
}
