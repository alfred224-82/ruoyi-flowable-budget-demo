$src = "D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/sql"
$dst = "D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/小微版/sql"
$files = @(
  "M15_09_micro_role_scope_hard_isolation.sql",
  "M15_10_micro_edition_tier_role_template.sql",
  "M15_11_promotion_activity_browse.sql",
  "M15_12_fund_platform_menu_edition.sql",
  "M15_13_customer_credit_no_show.sql"
)
foreach ($f in $files) {
  Copy-Item -Path (Join-Path $src $f) -Destination (Join-Path $dst $f) -Force
  Write-Host "Copied: $f"
}
Write-Host "Done. Files in dst:"
Get-ChildItem $dst -Name | Where-Object { $_ -like "M15*" }
