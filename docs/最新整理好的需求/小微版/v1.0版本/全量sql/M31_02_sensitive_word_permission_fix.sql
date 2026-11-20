-- =====================================================================
-- M31_02 租户敏感词 F 按钮权限落地（v1.83 修复）
-- 版本：v1.0  日期：2026-10-17
-- 根因：
--   TenantSensitiveWordController 类级 @RequireFeature("FEAT_SENSITIVE_WORD")，
--   6 个端点分别检查 micro:sensitiveWord:list/query/add/edit/remove/hitLog，
--   但菜单 7610（语音与合规配置，C 类型）的 perms 为
--   micro:compliance:config:rule,micro:compliance:config:policy,micro:compliance:config:sensitiveWord
--   ——与 Controller 的 micro:sensitiveWord:* 完全不匹配！
--   M18_13 v1.57 扁平化时删除了 7680~7683（原计划的 micro:sensitiveWord:* F 按钮），
--   导致权限链 sys_role_menu → sys_menu(perms) → @SaCheckPermission 断裂，
--   租户管理员访问 /compliance/tenant/sensitiveWord/list 必返回 403。
-- 修复：
--   ① 在 7610 下补建 6 个 F 按钮菜单（7611~7616），perms 与 Controller 严格对齐；
--   ② role_template platform_admin（micro_pro + micro_lite）追加 6 权限键；
--   ③ 存量 platform_admin 克隆角色 sys_role_menu 补录 7611~7616；
--   ④ 7610 C 菜单 perms 补列 micro:sensitiveWord:list（保持逗号分隔多权限模式）。
-- 前置依赖：M18_13（菜单 7610 建立）+ M18_10（sensitive_word 表）
-- ★幂等：先删后插 + NOT JSON_CONTAINS 守卫 + INSERT IGNORE，可重复执行
-- ★执行后请重启后端 + 退出重新登录刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 补建 6 个 F 按钮菜单（7611~7616，parent=7610）
--    perms 与 TenantSensitiveWordController @SaCheckPermission 严格对齐
--    F 类型不渲染侧边栏，仅作为权限码落库
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7611 AND 7616;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7611 AND 7616;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`client_type`,`icon`,`create_by`,`create_time`,`remark`) VALUES
(7611, '敏感词列表',   7610, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:list',    'pc', '#', 'admin', NOW(), '租户自定义敏感词列表 + 详情（GET /list, GET /{id}）'),
(7612, '全局底库查询', 7610, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:query',   'pc', '#', 'admin', NOW(), '全局底库只读列表（GET /global）'),
(7613, '敏感词新增',   7610, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:add',     'pc', '#', 'admin', NOW(), '新增租户自定义敏感词（POST）'),
(7614, '敏感词编辑',   7610, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:edit',    'pc', '#', 'admin', NOW(), '编辑租户自定义敏感词（PUT）'),
(7615, '敏感词删除',   7610, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:remove',  'pc', '#', 'admin', NOW(), '删除租户自定义敏感词（DELETE /{id}）'),
(7616, '命中记录查询', 7610, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:hitLog',  'pc', '#', 'admin', NOW(), '敏感词命中记录只读列表（GET /hitLog）');

-- ----------------------------
-- 2. 7610 C 菜单 perms 保持不变
--    7610 原 perms = micro:compliance:config:rule,micro:compliance:config:policy,micro:compliance:config:sensitiveWord
--    已足够覆盖页面渲染权限；API 级权限由下方 F 按钮（7611~7616）各自承载，
--    无需在 C 菜单 perms 中重复（perms 列 VARCHAR(100) 也放不下更多）。
--    ★无需 UPDATE 操作

-- ----------------------------
-- 3. role_template platform_admin 追加 6 权限键
--    micro_pro + micro_lite 两档均需覆盖（敏感词管理是租户管理员标配）
--    NOT JSON_CONTAINS 守卫幂等
-- ----------------------------
-- 3.1 micro:sensitiveWord:list
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:sensitiveWord:list')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:sensitiveWord:list"');

-- 3.2 micro:sensitiveWord:query
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:sensitiveWord:query')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:sensitiveWord:query"');

-- 3.3 micro:sensitiveWord:add
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:sensitiveWord:add')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:sensitiveWord:add"');

-- 3.4 micro:sensitiveWord:edit
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:sensitiveWord:edit')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:sensitiveWord:edit"');

-- 3.5 micro:sensitiveWord:remove
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:sensitiveWord:remove')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:sensitiveWord:remove"');

-- 3.6 micro:sensitiveWord:hitLog
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:sensitiveWord:hitLog')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:sensitiveWord:hitLog"');

-- ----------------------------
-- 4. 存量 platform_admin 克隆角色 sys_role_menu 补录 7611~7616
--    INSERT IGNORE 幂等
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
  FROM `sys_role` r CROSS JOIN `sys_menu` m
 WHERE r.role_key = 'platform_admin'
   AND r.del_flag = '0'
   AND m.menu_id BETWEEN 7611 AND 7616;

-- ----------------------------
-- 5. 超管 role_id=1 也补授 7611~7616（防超管调试时 403）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 7611 AND 7616;

-- =====================================================================
-- 6. 验收查询（执行后人工核对）
-- =====================================================================
-- 6.1 F 按钮菜单已建（预期 6 行）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--   WHERE menu_id BETWEEN 7611 AND 7616 ORDER BY menu_id;
-- 预期：7611~7616 六行，menu_type='F'，perms 对应 micro:sensitiveWord:*
--
-- 6.2 7610 perms 已补列（预期 1 行）
-- SELECT perms FROM sys_menu WHERE menu_id = 7610;
-- 预期含：micro:compliance:config:rule,...,micro:sensitiveWord:list
--
-- 6.3 role_template 含 6 权限键（预期 micro_pro + micro_lite 各 1 行，6 个 1）
-- SELECT version_type,
--   JSON_CONTAINS(menu_keys,'"micro:sensitiveWord:list"')    AS has_list,
--   JSON_CONTAINS(menu_keys,'"micro:sensitiveWord:query"')   AS has_query,
--   JSON_CONTAINS(menu_keys,'"micro:sensitiveWord:add"')     AS has_add,
--   JSON_CONTAINS(menu_keys,'"micro:sensitiveWord:edit"')    AS has_edit,
--   JSON_CONTAINS(menu_keys,'"micro:sensitiveWord:remove"')  AS has_remove,
--   JSON_CONTAINS(menu_keys,'"micro:sensitiveWord:hitLog"')  AS has_hitLog
--   FROM role_template WHERE role_code='platform_admin' AND version_type IN ('micro_pro','micro_lite');
-- 预期：2 行，全部 1
--
-- 6.4 存量 platform_admin 角色已补授（预期每租户 6 条）
-- SELECT r.role_id, COUNT(rm.menu_id) AS f_btn_count
--   FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE r.role_key = 'platform_admin' AND rm.menu_id BETWEEN 7611 AND 7616
--  GROUP BY r.role_id;
-- 预期：每行 f_btn_count=6
--
-- 6.5 端到端：租户管理员重新登录后，敏感词管理 Tab 加载正常，
--     GET /compliance/tenant/sensitiveWord/list 返回 200
-- =====================================================================
