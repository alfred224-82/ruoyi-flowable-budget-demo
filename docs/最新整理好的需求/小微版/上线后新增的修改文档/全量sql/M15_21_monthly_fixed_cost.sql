-- =====================================================================
-- M15_21 每月固定支出表 + 管理菜单（T15.37，v1.30 新增）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档（小微版）》v1.30 T15.37 详细设计
-- 概述：boss 在 PC 端管理每月固定支出项（房租/水电/工资等），
--       首页盈亏三指标中「支出」= 现金流流水支出 + 本月固定支出合计。
-- 变更内容：
--   1. 新建 monthly_fixed_cost 表（含 tenant_id/name/category/amount/is_recurring/effective_month/enabled/remark + 审计列）
--   2. UNIQUE(tenant_id, name, effective_month) 防重复
--   3. 新增 PC 菜单：固定支出管理（menu_id 7195，micro:pro:cost:*，boss PC 端）
--   4. 更新 role_template micro_pro boss 追加 cost perms
-- 前置依赖：M15_10（role_template 分档已建立）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 新建 monthly_fixed_cost 表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `monthly_fixed_cost` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`       bigint       NOT NULL                COMMENT '租户ID',
  `name`            varchar(64)  NOT NULL                COMMENT '支出项名称（如：房租、水电费、员工工资）',
  `category`        varchar(32)  NOT NULL DEFAULT 'other' COMMENT '分类（rent-房租/utility-水电/staff_salary-员工工资/staff_reward-员工提成/other-其他）',
  `amount`          decimal(12,2) NOT NULL DEFAULT 0.00  COMMENT '金额（元）',
  `is_recurring`    tinyint(1)   NOT NULL DEFAULT 1      COMMENT '是否每月重复（0-否，仅当月；1-是，每月自动计入）',
  `effective_month` varchar(7)   NOT NULL DEFAULT ''     COMMENT '生效月份（格式：YYYY-MM，如 2026-09）',
  `enabled`         tinyint(1)   NOT NULL DEFAULT 1      COMMENT '是否启用（0-停用；1-启用）',
  `remark`          varchar(255) DEFAULT NULL            COMMENT '备注',
  `create_dept`     bigint       DEFAULT NULL            COMMENT '创建部门',
  `create_by`       varchar(64)  DEFAULT NULL            COMMENT '创建者',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`       varchar(64)  DEFAULT NULL            COMMENT '更新者',
  `update_time`     datetime     DEFAULT NULL            COMMENT '更新时间',
  `del_flag`        char(1)      DEFAULT '0'             COMMENT '删除标志（0-正常；1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_name_month` (`tenant_id`, `name`, `effective_month`),
  KEY `idx_tenant_month` (`tenant_id`, `effective_month`),
  KEY `idx_tenant_enabled` (`tenant_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每月固定支出表（T15.37）';

-- ----------------------------
-- 2. 新增 PC 菜单：固定支出管理（menu_id 7195，挂在 boss 经营核心目录下）
-- ----------------------------
-- ★挂在 7000 小微版根菜单下，与 7180 推广分销同级
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7195;
DELETE FROM `sys_menu` WHERE `menu_id` = 7195;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7195, '固定支出管理', 7000, 8, 'cost', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'money', 'admin', NOW(), '每月固定支出管理目录（房租/水电/工资等）');

-- 固定支出列表页（C 菜单）
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7196;
DELETE FROM `sys_menu` WHERE `menu_id` = 7196;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7196, '固定支出列表', 7195, 1, 'index', 'micro/cost/index', NULL, 1, 0, 'C', '0', '0', 'micro:pro:cost:list', 'pc', 'list', 'admin', NOW(), '固定支出列表页，含总支出概览+分项列表+启停开关');

-- 固定支出新增按钮
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7197;
DELETE FROM `sys_menu` WHERE `menu_id` = 7197;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7197, '固定支出新增', 7195, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'micro:pro:cost:add', 'pc', '#', 'admin', NOW(), '新增固定支出项');

-- 固定支出修改按钮
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7198;
DELETE FROM `sys_menu` WHERE `menu_id` = 7198;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7198, '固定支出修改', 7195, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'micro:pro:cost:edit', 'pc', '#', 'admin', NOW(), '修改固定支出项');

-- 固定支出删除按钮
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7199;
DELETE FROM `sys_menu` WHERE `menu_id` = 7199;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7199, '固定支出删除', 7195, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'micro:pro:cost:remove', 'pc', '#', 'admin', NOW(), '删除固定支出项');

-- ----------------------------
-- 3. 授予超管 role_id=1
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 7195), (1, 7196), (1, 7197), (1, 7198), (1, 7199);

-- ----------------------------
-- 4. 更新 micro_pro boss role_template 追加固定支出 perms
-- ----------------------------
-- ★先删后插幂等（覆盖 M15_18 写入的版本，追加 cost perms）
DELETE FROM `role_template`
WHERE `version_type` = 'micro_pro'
  AND `role_code` = 'boss';

INSERT INTO `role_template` (`version_type`, `role_code`, `menu_keys`, `block_keys`, `status`, `create_by`, `remark`) VALUES
('micro_pro', 'boss',
 JSON_ARRAY(
   -- 经营核心
   'micro:pro:boss:home', 'micro:pro:report:index', 'micro:pro:report:trend',
   'micro:pro:shop:config',
   -- 预约管理
   'micro:pro:booking:list', 'micro:pro:booking:detail', 'micro:pro:booking:add',
   -- 人员与结算
   'micro:pro:staff:list', 'micro:pro:settlement:index',
   -- 推广分销管理（含奖励记录按渠道拆分）
   'micro:distribution:*',
   'micro:distribution:salesman', 'micro:distribution:salesman:records',
   'micro:distribution:customer', 'micro:distribution:customer:records',
   -- 固定支出管理
   'micro:pro:cost:list', 'micro:pro:cost:add', 'micro:pro:cost:edit', 'micro:pro:cost:remove',
   -- 个人中心
   'micro:pro:mine',
   -- lite 档继承
   'micro:lite:activity:publish', 'micro:lite:activity:view', 'micro:lite:boss:home',
   -- 活动浏览
   'micro:customer:activity:browse'
 ),
 JSON_ARRAY(),
 '0', 'admin',
 'micro_pro boss：PC 全经营（含推广分销拆分+固定支出+活动图片主题）');

-- ----------------------------
-- 5. 存量租户 boss 克隆角色补写新菜单（幂等 INSERT IGNORE）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type`
  AND rt.`role_code` = 'boss' AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON m.`perms` = j.`perm`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` IS NOT NULL AND t.`version_type` != ''
  AND m.`menu_id` IN (7195, 7196, 7197, 7198, 7199)
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`);

-- ----------------------------
-- 6. 存量租户默认主题更新（M15_20 补列后）
-- ----------------------------
-- 将存量租户 app_theme_key 为 NULL 的设为默认值
SET SQL_SAFE_UPDATES = 0;
UPDATE `sys_tenant` SET `app_theme_key` = 'theme_default'
WHERE `tenant_type` = 'micro' AND (`app_theme_key` IS NULL OR `app_theme_key` = '');
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 7. 验收查询
-- ----------------------------
-- A. 表已建立
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost';
-- 预期：1
--
-- B. 表结构完整
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost'
--  ORDER BY ORDINAL_POSITION;
-- 预期：16 列
--
-- C. 菜单已建
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id BETWEEN 7195 AND 7199;
-- 预期：5 行（1 目录 + 1 列表 + 3 按钮）
--
-- D. boss role_template 包含 cost perms
-- SELECT JSON_CONTAINS(menu_keys, '"micro:pro:cost:list"') AS has_cost
--   FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：1
--
-- E. 存量租户默认主题
-- SELECT id, company_name, app_theme_key FROM sys_tenant WHERE tenant_type='micro' LIMIT 5;
-- 预期：app_theme_key 均为 theme_default
