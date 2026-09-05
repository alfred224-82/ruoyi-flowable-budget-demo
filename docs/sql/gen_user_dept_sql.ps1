$ErrorActionPreference = 'Stop'

$deptFile = "D:\workspace\RuoYi-Flowable-Plus-0.8.X\docs\dept.md"
$userFile = "D:\workspace\RuoYi-Flowable-Plus-0.8.X\docs\user_dept.md"
$outputFile = "D:\workspace\RuoYi-Flowable-Plus-0.8.X\docs\sql\update_user_dept.sql"

$deptLines = Get-Content $deptFile -Encoding UTF8 | Where-Object { $_.Trim() -ne '' }
$userLines = Get-Content $userFile -Encoding UTF8 | Where-Object { $_.Trim() -ne '' }

$deptMap = @{}
$deptById = @{}
$deptByName = @{}

foreach ($line in $deptLines) {
    $parts = $line -split "`t"
    if ($parts.Count -lt 4) { continue }
    $deptId = $parts[0].Trim()
    $parentId = $parts[1].Trim()
    $deptName = $parts[3].Trim()
    $deptById[$deptId] = @{ Name = $deptName; ParentId = $parentId }
    $deptByName[$deptName] = $deptId
}

foreach ($deptId in $deptById.Keys) {
    $info = $deptById[$deptId]
    $parentId = $info.ParentId
    if ($deptById.ContainsKey($parentId)) {
        $parentName = $deptById[$parentId].Name
        $key = $parentName + '|' + $info.Name
        $deptMap[$key] = $deptId
    }
}

# Build Chinese keywords from char codes
$fengongsi = [string]::new([char[]]@(0x5206, 0x516C, 0x53F8))
$benbu = [string]::new([char[]]@(0x672C, 0x90E8))
$jishu = [string]::new([char[]]@(0x6280, 0x672F, 0x90E8))
$xiaoshou = [string]::new([char[]]@(0x9500, 0x552E, 0x90E8))
$caiwu = [string]::new([char[]]@(0x8D22, 0x52A1, 0x90E8))
$yanfa = [string]::new([char[]]@(0x7814, 0x53D1, 0x90E8))
$xingzheng = [string]::new([char[]]@(0x884C, 0x653F, 0x90E8))
$zonggongsi = [string]::new([char[]]@(0x9884, 0x7B97, 0x667A, 0x63A7, 0x96C6, 0x56E2, 0x603B, 0x516C, 0x53F8))

$deptSuffixes = @($jishu, $xiaoshou, $caiwu, $yanfa, $xingzheng)

$sb = [System.Text.StringBuilder]::new()
$now = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
[void]$sb.AppendLine("-- User-Dept Association Update Script")
[void]$sb.AppendLine("-- Generated: $now")
[void]$sb.AppendLine("")

$count = 0
$errors = @()

for ($i = 1; $i -lt $userLines.Count; $i++) {
    $parts = $userLines[$i] -split "`t"
    if ($parts.Count -lt 10) { continue }
    
    $seq = [int]$parts[0].Trim()
    $loginName = $parts[1].Trim()
    $userName = $parts[2].Trim()
    $fullDeptName = $parts[9].Trim()
    
    if ($seq -eq 1) { $userId = 1 } else { $userId = $seq + 1 }
    
    $sqlStmt = ""
    
    # Case 1: top-level company
    if ($fullDeptName -eq $zonggongsi) {
        $sqlStmt = "UPDATE sys_user SET dept_id = 1000 WHERE user_id = $userId;"
    }
    # Case 2: branch company level (e.g. "北京分公司")
    elseif ($fullDeptName.EndsWith($fengongsi) -and -not $fullDeptName.EndsWith($fengongsi + $benbu)) {
        if ($deptByName.ContainsKey($fullDeptName)) {
            $deptId = $deptByName[$fullDeptName]
            $sqlStmt = "UPDATE sys_user SET dept_id = $deptId WHERE user_id = $userId;"
        } else {
            $errors += "NOT FOUND TOP: $loginName -> $fullDeptName"
        }
    }
    # Case 3: benbu (e.g. "北京分公司分公司本部" -> parent="北京分公司", child="北京分公司本部")
    elseif ($fullDeptName.EndsWith($fengongsi + $benbu)) {
        $idx = $fullDeptName.IndexOf($fengongsi)
        if ($idx -ge 0) {
            $parentName = $fullDeptName.Substring(0, $idx + $fengongsi.Length)
            $childName = $parentName + $benbu
            $key = $parentName + '|' + $childName
            if ($deptMap.ContainsKey($key)) {
                $sqlStmt = "UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '" + $parentName + "' AND d.dept_name = '" + $childName + "') WHERE user_id = $userId;"
            } else {
                $errors += "NOT FOUND BB: $loginName -> $fullDeptName (key=$key)"
            }
        }
    }
    # Case 4: sub-departments (e.g. "青海分公司销售部" -> parent="青海分公司", child="销售部")
    else {
        $matched = $false
        foreach ($suffix in $deptSuffixes) {
            if ($fullDeptName.EndsWith($suffix)) {
                $parentName = $fullDeptName.Substring(0, $fullDeptName.Length - $suffix.Length)
                $childName = $suffix
                $key = $parentName + '|' + $childName
                if ($deptMap.ContainsKey($key)) {
                    $sqlStmt = "UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '" + $parentName + "' AND d.dept_name = '" + $childName + "') WHERE user_id = $userId;"
                    $matched = $true
                } else {
                    $errors += "NOT FOUND DEPT: $loginName -> $fullDeptName (key=$key)"
                    $matched = $true
                }
                break
            }
        }
        if (-not $matched) {
            $errors += "UNMATCHED: $loginName -> $fullDeptName"
        }
    }
    
    if ($sqlStmt -ne "") {
        [void]$sb.AppendLine("-- $loginName ($userName) -> $fullDeptName")
        [void]$sb.AppendLine($sqlStmt)
        $count++
    }
}

[void]$sb.AppendLine("")
[void]$sb.AppendLine("-- Total: $count UPDATE statements generated")

if ($errors.Count -gt 0) {
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("-- ERRORS ($($errors.Count)):")
    foreach ($e in $errors) {
        [void]$sb.AppendLine("-- $e")
    }
}

$outputDir = Split-Path $outputFile
if (!(Test-Path $outputDir)) { New-Item -ItemType Directory -Path $outputDir -Force | Out-Null }
[System.IO.File]::WriteAllText($outputFile, $sb.ToString(), (New-Object System.Text.UTF8Encoding $false))

Write-Host "Done! File: $outputFile"
Write-Host "Total UPDATE: $count"
Write-Host "Errors: $($errors.Count)"
if ($errors.Count -gt 0) {
    $errors | Select-Object -First 10 | ForEach-Object { Write-Host "  $_" }
}
