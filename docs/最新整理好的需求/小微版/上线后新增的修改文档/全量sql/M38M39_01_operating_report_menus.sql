-- =====================================================================
-- M38/M39 PC 端经营报表菜单（7395~7399，PC 端 boss 权限）
-- 日报 / 月报 / 活动明细 + 导出权限
-- ★避让 H5 端 7380~7382（M38_01）+ 7390~7393（M39_01）
-- =====================================================================

-- 1. 清理旧数据（幂等）
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7395 AND 7399;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7395 AND 7399;

-- 2. 插入菜单
-- 7395: 经营报表（目录，parent=7100 小微版 PC 端）
-- 7396: 日报（C=菜单）
-- 7397: 月报（C=菜单）
-- 7398: 活动明细（C=菜单）
-- 7399: 导出CSV（F=按钮，挂在 7395 下）
INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7395, '经营报表',   7100, 9, 'operating',        NULL,                        1, 0, 'M', '0', '0', '',                        'pc', 'chart',  'admin', NOW(), 'M38/M39 PC 端经营报表父目录'),
(7396, '日报',       7395, 1, 'daily',            'micro/operating/daily',     1, 0, 'C', '0', '0', 'micro:report:daily',      'pc', 'date',   'admin', NOW(), 'M39 PC 端日报（当日结算活动汇总+明细）'),
(7397, '月报',       7395, 2, 'monthly',          'micro/operating/monthly',   1, 0, 'C', '0', '0', 'micro:report:operating',  'pc', 'chart',  'admin', NOW(), 'M38 PC 端月报（含固定成本分摊+税费净利+保本线）'),
(7398, '活动明细',   7395, 3, 'activityDetail',   'micro/operating/activityDetail', 1, 0, 'C', '0', '0', 'micro:report:activityDetail', 'pc', 'list', 'admin', NOW(), 'M39 PC 端活动明细表（一行一场+合计行）'),
(7399, '导出CSV',    7395, 4, '',                 NULL,                        1, 0, 'F', '0', '0', 'micro:report:export',     'pc', '#',      'admin', NOW(), 'M38/M39 PC 端报表导出权限');

-- 3. role_template：boss（micro_pro/micro_lite）追加报表权限
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:daily')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:daily"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:monthly')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:monthly"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:activityDetail')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:activityDetail"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:export')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:export"');

-- 4. platform_admin 追加报表权限
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:daily')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:daily"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:monthly')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:monthly"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:activityDetail')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:activityDetail"');

-- 5. 存量 sys_role_menu 补录
-- 5.1 boss 克隆角色（小微版）追加 7380~7384
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` BETWEEN 7395 AND 7399;

-- 5.2 platform_admin 克隆角色追加 7395~7398（不含导出按钮）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` BETWEEN 7395 AND 7398;

-- 5.3 超管 role_id=1 补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` BETWEEN 7395 AND 7399;

-- =====================================================================
-- 验收查询
-- =====================================================================
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, component
--   FROM sys_menu WHERE menu_id BETWEEN 7395 AND 7399 ORDER BY menu_id;
-- 预期：5 行
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
