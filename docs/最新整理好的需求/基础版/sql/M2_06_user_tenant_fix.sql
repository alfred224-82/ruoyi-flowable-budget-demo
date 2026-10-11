-- =====================================================================
-- M2_06 用户租户关联回填与角色管理菜单授权修复脚本
-- 版本：v1.0  日期：2026-08-29
-- 背景：
--   1. sys_user.tenant_id 为 NULL 的用户（如 mmadmin），登录后
--      LoginHelper.getTenantId() 返回 null，导致：
--      - 租户拦截器对所有表放行（SQL 不追加 tenant_id 条件）
--      - 用户管理/角色管理服务层手动租户过滤被跳过
--      即"用户管理/部门管理/角色管理未关联租户"问题
--   2. 该用户对应角色缺少角色管理菜单（101 + 1010~1014）
-- 说明：本脚本幂等，可安全重复执行
-- =====================================================================

SET @user_name = 'mmadmin';

-- ----------------------------
-- 1. 回填 mmadmin 的 tenant_id
--    优先级：sys_tenant.admin_user_id 关联的租户 > 手动指定租户
-- ----------------------------
-- 1.1 通过租户表 admin_user_id 关联回填
SET SQL_SAFE_UPDATES = 0;
UPDATE `sys_user` u
    INNER JOIN `sys_tenant` t ON t.admin_user_id = u.user_id
SET u.tenant_id = t.id
WHERE u.user_name = @user_name
  AND u.tenant_id IS NULL;
SET SQL_SAFE_UPDATES = 1;

-- 1.2 若 1.1 未命中（租户表无关联），请手动指定目标租户后执行：
-- UPDATE `sys_user` SET `tenant_id` = ? WHERE `user_name` = 'mmadmin' AND `tenant_id` IS NULL;

-- ----------------------------
-- 2. 为 mmadmin 持有的角色授权角色管理菜单（幂等）
--    菜单：1=系统管理目录 101=角色管理 1010~1014=角色管理按钮
--    注意：若角色为共享平台管理员角色（9205），授权后所有持有该角色的
--    平台管理员均可看到角色管理菜单，与 PLATFORM_ADMIN_ADVANCED 模板一致
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT ur.role_id, m.menu_id
FROM `sys_user` u
    INNER JOIN `sys_user_role` ur ON ur.user_id = u.user_id
    INNER JOIN `sys_menu` m ON m.menu_id IN (1, 101, 1010, 1011, 1012, 1013, 1014)
WHERE u.user_name = @user_name;

-- ----------------------------
-- 3. 验收查询
-- ----------------------------
-- 3.1 用户租户关联（tenant_id 应非 NULL）
-- SELECT user_id, user_name, tenant_id FROM sys_user WHERE user_name = 'mmadmin';
-- 3.2 角色管理菜单授权
-- SELECT rm.role_id, rm.menu_id FROM sys_role_menu rm
--  INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
--  INNER JOIN sys_user u ON u.user_id = ur.user_id
--  WHERE u.user_name = 'mmadmin' AND rm.menu_id IN (1, 101, 1010, 1011, 1012, 1013, 1014);
