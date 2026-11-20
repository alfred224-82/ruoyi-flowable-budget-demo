[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$path = Get-ChildItem "D:\workspace\RuoYi-Flowable-Plus-0.8.X\docs" -Recurse -Filter "*.xlsx" | Where-Object { $_.Name -like "*上海*" } | Select-Object -First 1 -ExpandProperty FullName
Write-Host "Found: $path"
$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false
$wb = $excel.Workbooks.Open($path)
$sheetCount = $wb.Sheets.Count
Write-Host "Total sheets: $sheetCount"
for ($s=1; $s -le $sheetCount; $s++) {
    $ws = $wb.Sheets.Item($s)
    $rows = $ws.UsedRange.Rows.Count
    $cols = $ws.UsedRange.Columns.Count
    Write-Host "=== Sheet $s : $($ws.Name) === Rows=$rows Cols=$cols ==="
    for ($r=1; $r -le [Math]::Min($rows,200); $r++) {
        $line = ""
        for ($c=1; $c -le $cols; $c++) {
            $val = $ws.Cells.Item($r,$c).Text
            if ($c -gt 1) { $line += "`t" }
            $line += $val
        }
        Write-Host $line
    }
}
$wb.Close($false)
$excel.Quit()
[System.Runtime.Interopservices.Marshal]::ReleaseComObject($excel) | Out-Null
