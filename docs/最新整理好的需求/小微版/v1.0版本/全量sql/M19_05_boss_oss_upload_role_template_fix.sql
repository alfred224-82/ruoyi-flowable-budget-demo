-- =====================================================================
-- M19_05  小微版 boss 角色 OSS 上传权限彻底修复（v1.49，2026-09-19）
-- =====================================================================
-- 问题：M19_02 仅补了 sys_role_menu（运行表），未更新 role_template（模板源头），
--       导致角色重建（版本档位变更 reCopyMenuTemplateForMicro 先删后插、
--       新建租户 clonePresetRoles → copyMenuTemplateForMicro）时
--       从 role_template.menu_keys 读取权限，模板不含 system:oss:upload，
--       M19_02 的修复被覆盖，boss 再次报 403「无此权限：system:oss:upload」。
-- 根因：role_template 是权限天花板（source of truth），sys_role_menu 是运行时实例。
--       仅改 sys_role_menu 不改 role_template = 治标不治本。
-- 修复（本脚本为 boss 菜单授权链路的「终态权威脚本」，最后执行，定义最终状态）：
--   ① role_template micro/micro_pro/micro_lite boss menu_keys 追加 system:oss:upload，
--      确保模板源头包含 OSS 上传权限，角色重建（clonePresetRoles/reCopyMenuTemplateForMicro）不再丢失；
--   ② 所有小微版 boss（含 micro_lite）sys_role_menu 只补 1131（文件上传 F 按钮），
--      并删除历史脚本误补的 1(系统管理 M)/118(文件管理 C)——这两者会在侧边栏渲染出「系统管理」。
-- ★关键原理：1131 是 F 类型按钮，selectMenuTreeByUserId 只渲染 M/C 类型，故补 1131 不会污染侧边栏；
--            selectMenuPermsByUserId 只需 1131 即可放行 system:oss:upload，父级 1/118 完全不需要。
-- ★配套代码修复：SysTenantServiceImpl.copyMenuTemplateForMicro 已改为「F 类型菜单不补 M 祖先」，
--            从根本上杜绝角色重建时把 1131→118→1(系统管理) 拖入 boss 侧边栏。
-- ★前置：[63]M19_02（首次 sys_role_menu 补丁）+ [64]M19_03（micro_lite 精简）+ [65]M19_04（活动 CRUD）
-- ★幂等：JSON_CONTAINS 守卫 + DELETE + INSERT IGNORE + NOT EXISTS，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. role_template micro/micro_pro/micro_lite boss 追加 system:oss:upload
--    ★修复根因：确保模板源头包含 OSS 上传权限，角色重建不再丢失
--    ★含 micro_lite：免费版老板发布/编辑活动需上传封面/logo/海报（走 /system/oss/upload）
-- ---------------------------------------------------------------------
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'system:oss:upload'),
    `remark` = CONCAT(IFNULL(`remark`, ''), '；★补录 system:oss:upload（M19_05，仅 1131 不含 1/118）')
WHERE `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"system:oss:upload"');

-- 验证：
-- SELECT version_type, JSON_CONTAINS(menu_keys, '"system:oss:upload"') AS has_oss
--   FROM role_template WHERE version_type IN ('micro','micro_pro') AND role_code='boss';
--   -- 预期：micro=1, micro_pro=1

-- ---------------------------------------------------------------------
-- 2. 所有小微版 boss sys_role_menu：删除系统管理可见菜单 1/118（历史脚本误补，会在侧边栏渲染）
--    覆盖所有 version_type（micro/micro_pro/micro_lite），修复存量租户
-- ---------------------------------------------------------------------
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro'
  AND r.`role_key` = 'boss'
  AND r.`del_flag` = '0'
  AND rm.`menu_id` IN (1, 118);

-- ---------------------------------------------------------------------
-- 3. 所有小微版 boss sys_role_menu：只补 1131（文件上传 F 按钮，perms=system:oss:upload）
--    ★F 类型不在侧边栏渲染，既放行 OSS 上传又不冒出「系统管理」
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 1131
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 1131
  );

-- =====================================================================
-- 验收查询
-- =====================================================================
-- A. role_template micro/micro_pro/micro_lite boss 均已包含 system:oss:upload
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"system:oss:upload"') AS has_oss_upload
--   FROM role_template
--  WHERE version_type IN ('micro', 'micro_pro', 'micro_lite') AND role_code = 'boss';
--   -- 预期：micro=1, micro_pro=1, micro_lite=1
--
-- B. 所有小微版 boss 已包含 menu_id=1131（OSS 上传，F 类型）
-- SELECT t.id, t.company_name, t.version_type, r.role_id,
--        SUM(CASE WHEN rm.menu_id = 1131 THEN 1 ELSE 0 END) AS has_oss_upload
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, t.version_type, r.role_id;
--   -- 预期：每个租户 has_oss_upload = 1
--
-- C. ★所有小微版 boss 均无系统管理菜单 1/118（侧边栏不再出现「系统管理」）
-- SELECT COUNT(*) AS system_menu_leak
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND rm.menu_id IN (1, 118);
--   -- 预期：0
