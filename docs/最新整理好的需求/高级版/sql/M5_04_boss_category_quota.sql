-- =====================================================================
-- M5_04 老板大类预算额度脚本（T5.14 ①：老板按费用大类设定预算额度，只管粗粒度）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控系统设计文档》1.3 核心业务主线 ① / 2.2 #1
--       《资金管控系统开发任务文档》T5.14、《基础版操作手册》2.1 步骤1
-- 规范：
--   1. 存储沿用既有策略：预算编制只动 budget_preparation + budget_preparation_detail，
--      大类额度以 category_quotas(JSON) 列挂主表，不新增表
--   2. 补列走存储过程幂等；菜单先删后插，脚本可重复执行
--   3. 授权遵循租户级克隆角色机制（M2_08）：模板登记角色×版本 key（BOSS_*），
--      存量租户按模板补授克隆角色，共享角色 9201 同步保持一致
-- =====================================================================

-- ----------------------------
-- 1. budget_preparation 补大类额度 JSON 列（幂等）
--    格式：[{"categoryCode":"TRAVEL","categoryName":"差旅招待","amount":50000}]
-- ----------------------------
DROP PROCEDURE IF EXISTS `m5_add_preparation_category_quotas`;
DELIMITER $$
CREATE PROCEDURE `m5_add_preparation_category_quotas`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budget_preparation' AND COLUMN_NAME = 'category_quotas') THEN
        ALTER TABLE `budget_preparation`
            ADD COLUMN `category_quotas` JSON NULL COMMENT '老板设定的大类预算额度(粗粒度) [{"categoryCode":"TRAVEL","categoryName":"差旅招待","amount":50000}]' AFTER `total_budget`;
    END IF;
END$$
DELIMITER ;

CALL m5_add_preparation_category_quotas();
DROP PROCEDURE IF EXISTS `m5_add_preparation_category_quotas`;

-- ----------------------------
-- 2. 菜单：大类预算设定（挂预算管理目录 2000，排序第一，编制闭环入口）
--    menu_id 顺延占用 3316~3317（3310~3315 已被 M5_03 费用大类管理占用）
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (3316, 3317);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3316, '大类预算设定', 2000, 0, 'categoryQuota', 'system/preparation/categoryQuota', NULL, 1, 0, 'C', '0', '0', 'system:preparation:categoryQuota', 'money', 'admin', NOW(), '', NULL, 'T5.14① 老板按费用大类设定预算额度(粗粒度)');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3317, '大类额度保存', 3316, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:preparation:categoryQuotaSave', '#', 'admin', NOW(), '', NULL, '');

-- ----------------------------
-- 3. 角色×版本模板登记（模板 key = 角色前缀_版本，基础版能力三版本均含）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` IN (3316, 3317);
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('BOSS_BASIC', 3316), ('BOSS_BASIC', 3317),
('BOSS_ADVANCED', 3316), ('BOSS_ADVANCED', 3317),
('BOSS_PROFESSIONAL', 3316), ('BOSS_PROFESSIONAL', 3317);

-- ----------------------------
-- 4. 存量租户：为已克隆的 boss 角色按租户版本模板补授（幂等：NOT EXISTS 去重，同 M2_08）
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, tpl.`menu_id`
FROM `sys_role` r
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
JOIN `sys_edition_menu_template` tpl
  ON tpl.`edition` = CONCAT(UPPER(r.`role_key`), '_',
       UPPER(CASE WHEN t.`edition` = 'ENTERPRISE' THEN 'PROFESSIONAL' ELSE t.`edition` END))
WHERE r.`tenant_id` IS NOT NULL
  AND r.`role_key` = 'boss'
  AND r.`del_flag` = '0'
  AND tpl.`menu_id` IN (3316, 3317)
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm
      WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = tpl.`menu_id`
  );

-- ----------------------------
-- 5. 共享预设角色 9201（克隆源/admin 预览）同步模板，保持一致（幂等）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `role_id` = 9201 AND `menu_id` IN (3316, 3317);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9201, `menu_id` FROM `sys_edition_menu_template`
WHERE `edition` = 'BOSS_BASIC' AND `menu_id` IN (3316, 3317);

-- ----------------------------
-- 验收查询
-- ----------------------------
-- SHOW COLUMNS FROM budget_preparation LIKE 'category_quotas';                        -- 预期 1 行
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id IN (3316, 3317);       -- 预期 2 行
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template WHERE menu_id IN (3316, 3317) GROUP BY edition;  -- BOSS_* 各 2
-- SELECT r.role_id, t.tenant_code, COUNT(*) FROM sys_role_menu rm
--  JOIN sys_role r ON r.role_id = rm.role_id JOIN sys_tenant t ON t.id = r.tenant_id
--  WHERE rm.menu_id IN (3316, 3317) GROUP BY r.role_id, t.tenant_code;                 -- 每租户 boss 克隆角色各 2
