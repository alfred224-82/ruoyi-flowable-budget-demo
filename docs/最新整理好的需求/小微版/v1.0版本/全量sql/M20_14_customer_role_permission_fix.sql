-- =====================================================================
-- M20_14  顾客小程序权限 403 修复（v1.71 新增）
-- 版本：v1.0  日期：2026-10-03
-- 依据：用户反馈「GET /micro/customer/shop 报 无此权限：micro:customer:shop」
-- ---------------------------------------------------------------------
-- 根因：
--   1. 顾客角色 (role_id=9207, role_key='customer', tenant_id=NULL) 在 sys_role 中存在，
--      但 sys_role_menu 无任何记录 → 该角色无菜单权限
--   2. 顾客用户通过 registerByPhone() 自动注册时，仅创建 sys_user，未分配 sys_user_role
--   3. selectMenuPermsByUserId 需要 sys_user_role → sys_role_menu → sys_menu 完整链路，
--      两个环节都缺失 → @SaCheckPermission("micro:customer:shop") 校验失败（403）
-- 修复：
--   1. 为顾客角色 (9207) 补录 sys_role_menu（取 micro_pro customer 权限集，覆盖 lite+pro）
--   2. 为所有无 sys_user_role 的小微版顾客用户补录角色关联
--   3. 【Java】registerByPhone() 自动注册时自动分配 customer 角色
-- 前置依赖：M15_07（customer 角色 9207 已创建）、M15_10（micro_pro customer role_template）
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

-- ----------------------------
-- 1. 顾客角色 (9207) 补录 sys_role_menu
-- ----------------------------
-- 1.1 从 role_template (micro_pro customer) 取 menu_keys → sys_menu.menu_id
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9207, m.`menu_id`
FROM `role_template` rt
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON m.`perms` = j.`perm`
WHERE rt.`version_type` = 'micro_pro' AND rt.`role_code` = 'customer' AND rt.`status` = '0'
  AND m.`status` = '0';

-- 1.2 ★补全 M 类型祖先目录（perms 为空，不会被 menu_keys→perms 匹配）
--     顾客小程序菜单 (7400~7422) 的祖先链：7401~7422 → 7400(M) → 0
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (9207, 7400);  -- 小程序·顾客 (M 目录)

-- 1.3 补充 micro:activity:browse（lite 档顾客也需要活动浏览权限）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9207, m.`menu_id`
FROM `sys_menu` m
WHERE m.`perms` = 'micro:activity:browse' AND m.`status` = '0'
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = 9207 AND rm.`menu_id` = m.`menu_id`);

-- ----------------------------
-- 2. 存量顾客用户补录 sys_user_role
-- ----------------------------
-- 2.1 为所有小微版租户下无 sys_user_role 的顾客用户补录角色关联
--     判定条件：sys_user.tenant_id 指向小微版租户，且该用户在 sys_user_role 中无任何记录
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.`user_id`, 9207
FROM `sys_user` u
JOIN `sys_tenant` t ON u.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE u.`status` = '0' AND u.`del_flag` = '0'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_user_role` sur WHERE sur.`user_id` = u.`user_id`
  );

-- ----------------------------
-- 3. 验收查询（执行后人工核对）
-- ----------------------------
-- 3.1 customer 角色菜单授权完整
-- SELECT COUNT(*) AS menu_count FROM sys_role_menu WHERE role_id = 9207;
-- 预期：>= 9（micro_pro customer 8 个 perms + 1 个 M 目录 7400）
--
-- 3.2 关键权限已授权
-- SELECT m.menu_id, m.menu_name, m.perms
-- FROM sys_role_menu rm
-- JOIN sys_menu m ON m.menu_id = rm.menu_id
-- WHERE rm.role_id = 9207 AND m.perms IN ('micro:customer:shop', 'micro:customer:submitBook', 'micro:activity:browse')
-- ORDER BY m.menu_id;
-- 预期：3 行（7330/7401/7413 或类似）
--
-- 3.3 受影响用户已补录角色
-- SELECT COUNT(*) AS fixed_users
-- FROM sys_user u
-- JOIN sys_tenant t ON u.tenant_id = t.id AND t.tenant_type = 'micro'
-- WHERE u.status = '0' AND u.del_flag = '0'
--   AND EXISTS (SELECT 1 FROM sys_user_role sur WHERE sur.user_id = u.user_id AND sur.role_id = 9207);
-- 预期：> 0（所有小微版顾客用户均有角色）
--
-- 3.4 权限链路验证（以某顾客用户为例）
-- SELECT u.user_id, u.user_name, u.phonenumber, sur.role_id, r.role_key
-- FROM sys_user u
-- JOIN sys_user_role sur ON sur.user_id = u.user_id
-- JOIN sys_role r ON r.role_id = sur.role_id
-- JOIN sys_tenant t ON u.tenant_id = t.id AND t.tenant_type = 'micro'
-- WHERE r.role_key = 'customer'
-- LIMIT 5;
-- 预期：5 行，role_key='customer'
--
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
