-- =====================================================================
-- 生产增量脚本缺跑诊断（只读，手工执行，可重复跑）
-- ---------------------------------------------------------------------
-- 背景：部署新后端 jar 后生产库增量脚本滞后，报
--       Unknown column 'contact_wechat' in 'field list'（sys_tenant，[150]M31_04 交付列）。
-- 作用：按 00_执行顺序.sql 登记号 [138]~[156] 逐个检查「表结构/菜单/配方标记」，
--       一次性输出所有未执行（或未执行完整）的脚本清单，避免逐个报错逐个补。
-- 用法：在生产库直接执行本文件；
--       输出每一行 = 一个缺跑脚本的标记，按 00_执行顺序.sql 的 [登记号] 顺序补跑对应脚本；
--       结果为空集 = [138]~[156] 已全部执行。
-- ★纯 SELECT 无写操作；补跑脚本均幂等可重复执行（见各脚本头部 ★幂等 声明）。
-- ★[142]M30_01 / [143]M31_01 / [144]M31_02 / [145]M31_03 / [148]M29_05 为纯数据/菜单
--   修复脚本，无独立结构标记；若诊断发现其后脚本缺跑，建议从 [142] 起整段按序补跑。
-- =====================================================================

SELECT '[138] M29_01' AS script_no, 'booking_policy.project_booking_enabled 列' AS missing_object
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking_policy'
                      AND COLUMN_NAME = 'project_booking_enabled')
UNION ALL
SELECT '[139] M29_02', 'promotion_activity.receivable_amount 列'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
                      AND COLUMN_NAME = 'receivable_amount')
UNION ALL
SELECT '[140] M29_03', 'staff_commission 表'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_commission')
UNION ALL
SELECT '[141] M29_04', 'promotion_activity.cover_url 扩为 text'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
                      AND COLUMN_NAME = 'cover_url' AND DATA_TYPE = 'text')
UNION ALL
SELECT '[146] M26_04', 'sys_menu 7156 改名 H5推广码'
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu
                    WHERE menu_id = 7156 AND menu_name = 'H5推广码')
UNION ALL
SELECT '[147] M15_20_fix_boss_notify_menu_grant', 'role_template micro_lite boss 含 micro:notify:list'
 WHERE NOT EXISTS (SELECT 1 FROM role_template
                    WHERE version_type = 'micro_lite' AND role_code = 'boss'
                      AND JSON_CONTAINS(menu_keys, '"micro:notify:list"'))
UNION ALL
SELECT '[149] M21_10', 'staff_pricing.pricing_type 列'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_pricing'
                      AND COLUMN_NAME = 'pricing_type')
UNION ALL
SELECT '[150] M31_04', 'sys_tenant.contact_wechat 列'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant'
                      AND COLUMN_NAME = 'contact_wechat')
UNION ALL
SELECT '[150] M31_04', 'staff_contact_public 表'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_contact_public')
UNION ALL
SELECT '[151] M35_01', 'staff_commission_rule 表'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_commission_rule')
UNION ALL
SELECT '[151] M35_01', 'activity_staff_snapshot 表'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_staff_snapshot')
UNION ALL
SELECT '[151] M35_01', 'promotion_activity.other_cost 列'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
                      AND COLUMN_NAME = 'other_cost')
UNION ALL
SELECT '[152] M36_01', 'activity_settlement 表'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_settlement')
UNION ALL
SELECT '[153] M34_01', 'sys_menu 7070 员工联系方式公开页'
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 7070)
UNION ALL
SELECT '[154] M38_01', 'monthly_operating_closing 表'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_operating_closing')
UNION ALL
SELECT '[155] M39_01', 'sys_menu 7390 报表中心'
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 7390)
UNION ALL
SELECT '[156] M33_01', 'activity_lock_request 表'
 WHERE NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_lock_request');

-- =====================================================================
-- 补跑顺序（与 00_执行顺序.sql 阶段五十二~六十三一致，均幂等）：
--   [142] M30_01_cleanup_micro_version_type.sql
--   [143] M31_01_sensitive_word_activation.sql
--   [144] M31_02_sensitive_word_permission_fix.sql
--   [145] M31_03_boss_room_layout_menu_hide.sql
--   [146] M26_04_h5_qrcode_rename.sql
--   [147] M15_20_fix_boss_notify_menu_grant.sql
--   [148] M29_05_receivable_amount_publish_backfill.sql
--   [149] M21_10_staff_pricing_pricing_type.sql
--   [150] M31_04_staff_contact_public_and_store_wechat.sql   ← 本次报错根因
--   [151] M35_01_staff_commission_rule_and_snapshot.sql
--   [152] M36_01_activity_settlement_tables.sql
--   [153] M34_01_staff_contact_public_pc_menu.sql
--   [154] M38_01_monthly_operating_closing.sql
--   [155] M39_01_report_center_menus.sql
--   [156] M33_01_activity_lock_request.sql
-- 补跑完成后：重启后端 + 退出重新登录（刷新 Sa-Token 权限快照），再重跑本诊断应为空集。
-- =====================================================================
