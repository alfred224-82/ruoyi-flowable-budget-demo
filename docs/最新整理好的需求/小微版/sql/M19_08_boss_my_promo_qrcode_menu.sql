-- =====================================================================
-- M19_08  Boss PC「我的推广码」菜单（v1.56 新增）
-- =====================================================================
-- 依据：用户新增需求——boss PC 菜单增加「我的推广码」页面，展示门店/个人推广二维码，
--       供手机扫描进入微信小程序。免费版（micro_lite）与付费版（micro_pro）均需加上。
--
-- 需求概述：
--   老板 PC 端「推广发布」目录下新增「我的推广码」页面（C 类型），
--   展示门店推广二维码（客户扫码进入小程序）+ 个人推广码，支持一键生成/刷新。
--   ★免费版和付费版都需要。
--
-- 菜单设计：
--   menu_id 7159 —— 我的推广码（C 类型，parent_id=7150 推广发布，client_type=pc）
--   perms: micro:myPromo:view
--   避让：7156~7158（M19_07 活动设置）/ 7160~7164（M17 评价管理）
--
-- 权限设计：
--   新增 perms `micro:myPromo:view` 专用于此页面；
--   后端 MicroPromotionCodeController 的 generate/my 端点放宽为
--   SaMode.OR(micro:distribution:*, micro:myPromo:view)，
--   使免费版 boss 无需获得完整 micro:distribution:* 即可访问。
--
-- 前置依赖：
--   [21]M15_06（菜单 7000~7499 + client_type 补列）
--   [68]M19_07（活动设置菜单 7156~7158，避让）
--
-- 幂等：INSERT IGNORE + JSON_CONTAINS 守卫 + NOT EXISTS，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 菜单注册（menu_id 7159，挂在 7150 推广发布目录下）
-- =====================================================================
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`,
    `create_by`, `create_time`, `remark`)
VALUES (7159, '我的推广码', 7150, 10, 'myQrcode', 'micro/promo/myQrcode/index',
    '', 1, 0, 'C', '0', '0', 'micro:myPromo:view', 'pc', 'qrcode',
    'admin', NOW(), '老板PC端展示门店/个人推广二维码，供手机扫描进入微信小程序（免费版+付费版均有）');

-- =====================================================================
-- 2. 超管 role_id=1 授权
-- =====================================================================
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 7159);

-- =====================================================================
-- 3. role_template 更新（权限天花板）
--    ★免费版 micro_lite + 付费版 micro_pro 均追加 micro:myPromo:view
-- =====================================================================

-- 3.1 micro_pro / micro boss 追加 micro:myPromo:view
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:myPromo:view')
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:myPromo:view"');

-- 3.2 micro_lite（免费版）boss 追加 micro:myPromo:view
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:myPromo:view')
WHERE `version_type` = 'micro_lite'
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:myPromo:view"');

-- =====================================================================
-- 4. 存量租户 boss sys_role_menu 补录
-- =====================================================================

-- 4.1 为所有小微版 boss 克隆角色补录 7159
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 7159 AS `menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm2
       WHERE rm2.`role_id` = r.`role_id` AND rm2.`menu_id` = 7159
  );

-- 4.2 补全 M 类型祖先目录（7159→7150→7100→7000，仅补缺失的）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
WITH RECURSIVE ancestor_tree AS (
    SELECT DISTINCT m.`parent_id` AS ancestor_id
    FROM `sys_role_menu` rm
    JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
    JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
    WHERE m.`menu_id` = 7159
      AND r.`role_key` = 'boss'
      AND m.`parent_id` > 0
    UNION
    SELECT m2.`parent_id`
    FROM ancestor_tree at2
    JOIN `sys_menu` m2 ON m2.`menu_id` = at2.`ancestor_id`
    WHERE m2.`parent_id` > 0
)
SELECT r.`role_id`, at3.`ancestor_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN ancestor_tree at3
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm2
       WHERE rm2.`role_id` = r.`role_id` AND rm2.`menu_id` = at3.`ancestor_id`
  );

-- =====================================================================
-- 5. 后端权限放宽（登记说明，实际改动在 Java 代码中）
--    MicroPromotionCodeController 的 generate / my 端点：
--    @SaCheckPermission(value = {"micro:distribution:*", "micro:myPromo:view"}, mode = SaMode.OR)
--    使免费版 boss 持有 micro:myPromo:view 即可调通，无需完整 micro:distribution:*
-- =====================================================================

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 5.1 菜单已落库
-- SELECT menu_id, menu_name, parent_id, perms, menu_type, client_type
--  FROM sys_menu WHERE menu_id = 7159;
-- 预期：1 行，C 类型，client_type=pc，parent_id=7150
--
-- 5.2 role_template 含新权限（付费版 + 免费版均含）
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"micro:myPromo:view"') AS has_my_promo
--   FROM role_template
--  WHERE role_code = 'boss' AND version_type IN ('micro', 'micro_pro', 'micro_lite');
-- 预期：3 行，has_my_promo 均为 1
--
-- 5.3 存量租户 boss 已关联
-- SELECT t.id, t.company_name,
--        SUM(CASE WHEN rm.menu_id = 7159 THEN 1 ELSE 0 END) AS has_my_qrcode
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name;
-- 预期：每行 has_my_qrcode = 1
--
-- 5.4 ★micro_lite 免费版 boss 侧边栏增加一项（7159 挂在 7150 下，侧边栏可见）
-- SELECT t.id, GROUP_CONCAT(m.menu_id ORDER BY m.menu_id) AS sidebar_menu_ids
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.menu_type IN ('M', 'C') AND m.status = '0'
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--  GROUP BY t.id;
--   -- 预期：每租户 sidebar_menu_ids 含 7159（在 7150 推广发布目录下展开）
