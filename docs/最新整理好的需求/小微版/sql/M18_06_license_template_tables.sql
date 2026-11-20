-- =====================================================================
-- M18_06 License 模板管理与使用报表建表脚本（T18.14，v1.29 新增）
-- 版本：v1.0  日期：2026-09-17
-- 依据：《资金管控系统开发任务文档（小微版）》v1.29 M18
--       T18.14 License模板管理与使用报表（BE+PC）
-- 前置依赖：sys_license 表（已有）、sys_tenant 表（已有）
-- 规范：
--   1. sys_license_template 为【平台级表】（无 tenant_id），仅 Admin 管理
--   2. 先删后建幂等
--   3. menu_id 占用 7660~7669 区段
-- =====================================================================

SET NAMES utf8mb4;

-- ===== 1. License 模板表 sys_license_template =====
DROP TABLE IF EXISTS `sys_license_template`;
CREATE TABLE `sys_license_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称（如：基础版/高级版/专业版）',
  `edition` VARCHAR(30) NOT NULL COMMENT '版本等级：BASIC/ADVANCED/PROFESSIONAL',
  `license_type` VARCHAR(20) NOT NULL DEFAULT 'FORMAL' COMMENT '授权类型：TRIAL(试用)/FORMAL(正式)',
  `trial_days` INT DEFAULT NULL COMMENT '试用天数（仅TRIAL有效，NULL=不限）',
  `industries` VARCHAR(500) DEFAULT NULL COMMENT '适用行业列表JSON（如["桌游","健身","餐饮"]）',
  `max_users` INT NOT NULL DEFAULT 10 COMMENT '最大用户数',
  `max_roles` INT DEFAULT NULL COMMENT '最大角色数（NULL=不限）',
  `features` TEXT DEFAULT NULL COMMENT '功能开关列表JSON',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edition_license_type` (`edition`, `license_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='License版本模板表（T18.14，Admin管理，平台级无tenant_id）';

-- ===== 2. 预置 3 个版本模板 =====
INSERT IGNORE INTO `sys_license_template` (`template_name`, `edition`, `license_type`, `trial_days`, `industries`, `max_users`, `max_roles`, `features`, `status`, `create_by`, `remark`)
VALUES
('基础版-正式', 'BASIC', 'FORMAL', NULL, '["桌游","健身","餐饮"]', 10, 2, '["FEAT_BOOKING","FEAT_CASH_FLOW"]', '0', 'admin', '基础版正式授权：预约+记账'),
('高级版-正式', 'ADVANCED', 'FORMAL', NULL, '["桌游","健身","餐饮","美容","教育"]', 50, 6, '["FEAT_BOOKING","FEAT_CASH_FLOW","FEAT_RATING","FEAT_PROMOTION_DISTRIBUTION"]', '0', 'admin', '高级版正式授权：预约+记账+评价+推广'),
('专业版-正式', 'PROFESSIONAL', 'FORMAL', NULL, NULL, 999, NULL, '["FEAT_BOOKING","FEAT_CASH_FLOW","FEAT_RATING","FEAT_PROMOTION_DISTRIBUTION","FEAT_RATING","FEAT_REPORT"]', '0', 'admin', '专业版正式授权：全功能'),
('基础版-试用', 'BASIC', 'TRIAL', 7, '["桌游","健身","餐饮"]', 5, 2, '["FEAT_BOOKING","FEAT_CASH_FLOW"]', '0', 'admin', '基础版试用7天');

-- ===== 3. 菜单 SQL（Admin License 管理，menu_id 7660~7669）=====
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7660 AND 7669;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7660 AND 7669;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
(7660, 'License模板管理', 100, 50, 'licenseTemplate', 'system/licenseTemplate/index', NULL, 1, 0, 'C', '0', '0', 'system:licenseTemplate:list', 'server', 'component', 'admin', NOW(), '★T18.14 License版本模板CRUD（Admin专属）'),
(7661, 'License使用报表', 100, 51, 'licenseReport', 'system/licenseReport/index', NULL, 1, 0, 'C', '0', '0', 'system:licenseReport:list', 'server', 'chart', 'admin', NOW(), '★T18.14 License使用报表：版本分布/到期预警（Admin专属）');

-- 角色菜单关联（admin 角色）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE m.menu_id BETWEEN 7660 AND 7669
  AND r.role_key = 'admin';

-- ===== 验证 =====
-- SELECT * FROM sys_license_template;
-- SELECT * FROM sys_menu WHERE menu_id BETWEEN 7660 AND 7669;
