-- =====================================================================
-- M19_06  小微版 boss OSS 图片回显/删除权限补全（system:oss:query / system:oss:remove）（v1.0，2026-09-19）
-- =====================================================================
-- 问题：boss 打开「活动管理」列表/编辑页时，前端 ImageUpload 组件与 promo/index.vue resolveOssIds()
--       调用 GET /system/oss/listByIds/{ossId} 回显图片（coverUrl/posterUrl/logoUrl 存的是 ossId），
--       报 403「无此权限：system:oss:list」；删除已上传图片时调用 DELETE /system/oss/{ossId}（delOss），
--       需 system:oss:remove，同样会 403。
-- 根因：/system/oss/listByIds 原仅接受 system:oss:list，而该 perms 只挂在 C 类型菜单 118(文件管理) 上；
--       118 会在侧边栏渲染出「系统管理 > 文件管理」，与 micro_lite「仅经营概览+活动管理」隔离要求冲突，
--       故 boss 绝不能持有 118。M19_05 只补了 1131(system:oss:upload)，未覆盖回显(query)/删除(remove)。
-- 修复（双层同步，与 M19_05 同模式；role_template 源头 + sys_role_menu 运行表）：
--   ① 【Java】SysOssController.listByIds 权限放宽为 SaMode.OR(system:oss:list, system:oss:query)：
--      兼容 admin/企业版原有 list 权限，同时允许仅持 F 类型 query 权限的小微版 boss 回显；
--   ② 【SQL】role_template micro/micro_pro/micro_lite boss menu_keys 追加
--      system:oss:query + system:oss:remove（均为 F 类型按钮 perms，唯一映射 1130/1133）；
--   ③ 【SQL】所有小微版 boss sys_role_menu 补录 1130(文件查询,F) + 1133(文件删除,F)。
-- ★关键原理：1130/1133 均为 F 类型按钮，selectMenuTreeByUserId 只渲染 M/C 类型，故补录不污染侧边栏；
--            selectMenuPermsByUserId 只需菜单本身携带 perms 即放行，父级 118/1 完全不需要；
--            copyMenuTemplateForMicro 已修复为「F 类型不补 M 祖先」，角色重建不会沿 1130/1133→118→1 拖入系统管理。
-- ★菜单对照：1130 文件查询(F,system:oss:query,parent=118) / 1131 文件上传(F,system:oss:upload) /
--            1133 文件删除(F,system:oss:remove) / 118 文件管理(C,system:oss:list) ← 唯一 C 类型，禁止授予 boss
-- ★前置：[66]M19_05（boss OSS 上传权限 + 系统管理菜单根治）
-- ★幂等：JSON_CONTAINS 守卫 + INSERT IGNORE + NOT EXISTS + 条件 DELETE，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. role_template micro/micro_pro/micro_lite boss 追加 system:oss:query + system:oss:remove
--    ★修复根因：确保模板源头包含回显/删除权限，角色重建（clonePresetRoles/reCopyMenuTemplateForMicro）不再丢失
--    ★均为 F 类型按钮 perms，唯一映射 1130/1133，不污染侧边栏
-- ---------------------------------------------------------------------
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'system:oss:query'),
    `remark` = CONCAT(IFNULL(`remark`, ''), '；★补录 system:oss:query（M19_06，图片回显，仅 1130 不含 118/1）')
WHERE `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"system:oss:query"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'system:oss:remove'),
    `remark` = CONCAT(IFNULL(`remark`, ''), '；★补录 system:oss:remove（M19_06，图片删除，仅 1133 不含 118/1）')
WHERE `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"system:oss:remove"');

-- 验证：
-- SELECT version_type, JSON_CONTAINS(menu_keys, '"system:oss:query"') AS has_query,
--        JSON_CONTAINS(menu_keys, '"system:oss:remove"') AS has_remove
--   FROM role_template WHERE version_type IN ('micro','micro_pro','micro_lite') AND role_code='boss';
--   -- 预期：三档均 has_query=1, has_remove=1

-- ---------------------------------------------------------------------
-- 2. 所有小微版 boss sys_role_menu 补录 1130(文件查询,F) + 1133(文件删除,F)
--    ★F 类型不在侧边栏渲染，既放行回显/删除又不冒出「系统管理」
--    覆盖所有 version_type（micro/micro_pro/micro_lite），修复存量租户
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN (SELECT 1130 AS `menu_id` UNION ALL SELECT 1133) m
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`
  );

-- ---------------------------------------------------------------------
-- 3. ★防御兜底：确保所有小微版 boss 均无系统管理可见菜单 1/118（与 M19_05 一致，幂等）
--    即便历史脚本或角色重建误补，本脚本作为最后防线再次清除
-- ---------------------------------------------------------------------
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro'
  AND r.`role_key` = 'boss'
  AND r.`del_flag` = '0'
  AND rm.`menu_id` IN (1, 118);

-- =====================================================================
-- 验收查询
-- =====================================================================
-- A. role_template micro/micro_pro/micro_lite boss 均含 system:oss:query + system:oss:remove
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"system:oss:query"')  AS has_oss_query,
--        JSON_CONTAINS(menu_keys, '"system:oss:remove"') AS has_oss_remove
--   FROM role_template
--  WHERE version_type IN ('micro', 'micro_pro', 'micro_lite') AND role_code = 'boss';
--   -- 预期：三档均 has_oss_query=1, has_oss_remove=1
--
-- B. 所有小微版 boss 已包含 1130(文件查询) + 1133(文件删除)（F 类型）
-- SELECT t.id, t.company_name, t.version_type, r.role_id,
--        SUM(CASE WHEN rm.menu_id = 1130 THEN 1 ELSE 0 END) AS has_oss_query,
--        SUM(CASE WHEN rm.menu_id = 1133 THEN 1 ELSE 0 END) AS has_oss_remove
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, t.version_type, r.role_id;
--   -- 预期：每个租户 has_oss_query=1, has_oss_remove=1
--
-- C. ★所有小微版 boss 仍无系统管理菜单 1/118（侧边栏不出现「系统管理」）
-- SELECT COUNT(*) AS system_menu_leak
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND rm.menu_id IN (1, 118);
--   -- 预期：0
--
-- D. ★micro_lite boss 侧边栏（M/C）仍仅 7100/7101/7150/7151（新增 1130/1133 为 F 不渲染）
-- SELECT t.id, GROUP_CONCAT(m.menu_id ORDER BY m.menu_id) AS sidebar_menu_ids
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.menu_type IN ('M', 'C') AND m.status = '0'
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--  GROUP BY t.id;
--   -- 预期：每租户 sidebar_menu_ids = '7100,7101,7150,7151'
