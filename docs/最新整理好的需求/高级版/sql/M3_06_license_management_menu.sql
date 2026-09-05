-- =====================================================================
-- M3_06 License 管理页面菜单增强（T3.14 / T3.17）
-- 版本：v1.0  日期：2026-08-30
-- 依据：《资金管控系统开发任务文档》M3.5 License 与租户关联
-- 说明：
--   1. License 管理页（6004）已存在，此处确保按钮级权限完整
--   2. 新增"导入租户 License"按钮权限（6022，system:tenant:edit 复用）
--   3. sys_tenant_version_log.changeType 新增 LICENSE_IMPORT/LICENSE_REPLACE/LICENSE_EXPIRE/LICENSE_DOWNGRADE
-- =====================================================================

-- ----------------------------
-- 1. 新增"导入 License"按钮菜单（挂在 License 管理 6004 下）
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` = 6022;
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
(6022, '导入租户License', 6004, 5, '', NULL, 'F', '0', '0', 'system:tenant:edit', '#', 'admin', NOW());

-- ----------------------------
-- 2. admin 角色授权（admin 角色 ID = 1）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 6022);

-- ----------------------------
-- 3. sys_tenant_version_log.changeType 扩展说明（无需 DDL，字段为 varchar）
--    新增枚举值：
--    - LICENSE_IMPORT    : platform_admin 首次为租户导入 License
--    - LICENSE_REPLACE   : platform_admin 替换租户已有 License
--    - LICENSE_EXPIRE    : License 到期系统自动降级
--    - LICENSE_DOWNGRADE : License 吊销/手动降级
-- ----------------------------

-- ----------------------------
-- 4. 验收查询
-- ----------------------------
-- SELECT * FROM sys_menu WHERE menu_id IN (6004, 6022);
-- SELECT * FROM sys_tenant_version_log WHERE change_type LIKE 'LICENSE_%';
