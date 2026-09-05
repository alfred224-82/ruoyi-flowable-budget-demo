-- =====================================================================
-- M7_02 专业版多账套脚本（T7.7：租户内多账套独立核算 + 单据归属账套 + 合并报表）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》T7.7 / 需求文档 4.1「专业版资金管控 = 多账套」
-- 开关：FEAT_MULTI_ACCOUNT_SET（专业版专属，接口层由 @RequireFeature 拦截）
-- 前置依赖：
--   1. M1_03（专业版增量表）/ M7_01（fee_org 多组织，账套的上级维度）
--   2. M2_02 v2.0（角色×版本模板）/ M2_08（租户级克隆角色）
-- 规范：
--   1. 先删后建 / 条件 ALTER / 先删后插，脚本可重复执行
--   2. 逐表含 tenant_id + 租户索引；建表 ≠ 开放，开放由 FEAT_MULTI_ACCOUNT_SET 控制
--   3. 授权遵循租户级克隆角色机制（M2_08）：模板登记角色×版本 key（*_PROFESSIONAL），
--      存量专业版租户按模板补授克隆角色（数据驱动，非专业版租户自然无匹配）
--   4. menu_id 顺延占用 3325~3331（3323~3324 已被 M5_07 占用，3400+ 属 M11）
-- =====================================================================

-- ----------------------------
-- 1. 账套主表（FEAT_MULTI_ACCOUNT_SET；每账套独立科目表/预算版本/凭证导出）
--    org_id 关联 fee_org：多组织为账套的上级维度（一个组织可含多个账套）
-- ----------------------------
DROP TABLE IF EXISTS `fee_account_set`;
CREATE TABLE `fee_account_set` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账套ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `org_id` BIGINT DEFAULT NULL COMMENT '所属组织ID(fee_org，多组织为账套上级维度，空=租户级账套)',
  `name` VARCHAR(100) NOT NULL COMMENT '账套名称',
  `code` VARCHAR(50) NOT NULL COMMENT '账套编码(租户内唯一)',
  `fiscal_year_start` VARCHAR(10) DEFAULT '01-01' COMMENT '财年起始(格式 MM-DD，如 01-01 自然年)',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认账套(每租户至多一个，存量单据归属)',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE启用/FROZEN冻结/CLOSED已结账)',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_org` (`org_id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多账套主表(专业版 FEAT_MULTI_ACCOUNT_SET)';

-- ----------------------------
-- 2. 单据/预算/科目表补 account_set_id（幂等条件 ALTER）
--    存量数据 account_set_id=NULL，租户启用多账套后由业务侧回填至默认账套
-- ----------------------------
DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `sp_add_column_if_missing`(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(500))
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO v_cnt FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column;
    IF v_cnt = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 报销单归属账套（单据维度）
CALL sp_add_column_if_missing('fee_expense_bill', 'account_set_id',
  '`account_set_id` BIGINT DEFAULT NULL COMMENT ''归属账套ID(fee_account_set,FEAT_MULTI_ACCOUNT_SET)''');
-- 预算编制归属账套（独立预算版本）
CALL sp_add_column_if_missing('budget_preparation', 'account_set_id',
  '`account_set_id` BIGINT DEFAULT NULL COMMENT ''归属账套ID(fee_account_set,FEAT_MULTI_ACCOUNT_SET)''');
-- 预算科目归属账套（独立科目表）
CALL sp_add_column_if_missing('budget_subject', 'account_set_id',
  '`account_set_id` BIGINT DEFAULT NULL COMMENT ''归属账套ID(fee_account_set,FEAT_MULTI_ACCOUNT_SET)''');

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;

-- ----------------------------
-- 3. 菜单：账套管理页（挂费用管控目录 3000，与 3224 组织管理同级，专业版）
--    C 型主菜单 3325 + F 型按钮 3326~3331
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (3325, 3326, 3327, 3328, 3329, 3330, 3331);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3325, '账套管理', 3000, 46, 'fee/accountSet', 'fee/accountSet/index', NULL, 1, 0, 'C', '0', '0', 'fee:accountSet:list', 'money', 'admin', NOW(), '', NULL, '专业版 FEAT_MULTI_ACCOUNT_SET 多账套独立核算');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3326, '账套查询', 3325, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:accountSet:query',       '#', 'admin', NOW(), '', NULL, ''),
(3327, '账套新增', 3325, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:accountSet:add',         '#', 'admin', NOW(), '', NULL, '每账套独立科目表/预算版本'),
(3328, '账套编辑', 3325, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:accountSet:edit',        '#', 'admin', NOW(), '', NULL, ''),
(3329, '账套删除', 3325, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:accountSet:remove',      '#', 'admin', NOW(), '', NULL, '有单据归属的账套禁止删除'),
(3330, '账套切换', 3325, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:accountSet:switch',      '#', 'admin', NOW(), '', NULL, '设置当前操作账套上下文'),
(3331, '合并报表', 3325, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:accountSet:mergeReport', '#', 'admin', NOW(), '', NULL, '集团视角：各账套汇总(老板只读)');

-- ----------------------------
-- 4. 角色×版本模板登记（专业版专属，防串第④层：低版本模板不含）
--    财务：账套全量管理（3325~3331）；老板：账套查看 + 合并报表只读（3325/3326/3331）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` IN (3325, 3326, 3327, 3328, 3329, 3330, 3331);
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('FINANCE_PROFESSIONAL', 3325), ('FINANCE_PROFESSIONAL', 3326), ('FINANCE_PROFESSIONAL', 3327),
('FINANCE_PROFESSIONAL', 3328), ('FINANCE_PROFESSIONAL', 3329), ('FINANCE_PROFESSIONAL', 3330),
('FINANCE_PROFESSIONAL', 3331),
('BOSS_PROFESSIONAL', 3325), ('BOSS_PROFESSIONAL', 3326), ('BOSS_PROFESSIONAL', 3331);

-- ----------------------------
-- 5. 存量租户：为已克隆的 finance/boss 角色按租户版本模板补授（幂等 NOT EXISTS 去重，同 M5_04）
--    仅专业版租户命中 *_PROFESSIONAL 模板；基础版/高级版租户无匹配行，自然不授权
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, tpl.`menu_id`
FROM `sys_role` r
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
JOIN `sys_edition_menu_template` tpl
  ON tpl.`edition` = CONCAT(UPPER(r.`role_key`), '_',
       UPPER(CASE WHEN t.`edition` = 'ENTERPRISE' THEN 'PROFESSIONAL' ELSE t.`edition` END))
WHERE r.`tenant_id` IS NOT NULL
  AND r.`role_key` IN ('finance', 'boss')
  AND r.`del_flag` = '0'
  AND tpl.`menu_id` IN (3325, 3326, 3327, 3328, 3329, 3330, 3331)
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm
      WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = tpl.`menu_id`
  );

-- ----------------------------
-- 验收查询
-- ----------------------------
-- SHOW CREATE TABLE fee_account_set;
-- SHOW COLUMNS FROM fee_expense_bill LIKE 'account_set_id';    -- 预期存在
-- SHOW COLUMNS FROM budget_preparation LIKE 'account_set_id';  -- 预期存在
-- SHOW COLUMNS FROM budget_subject LIKE 'account_set_id';      -- 预期存在
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3325 AND 3331;  -- 预期 7 行
-- 防串：低版本模板不得含账套菜单（预期 0）
-- SELECT COUNT(*) FROM sys_edition_menu_template
--  WHERE edition IN ('BOSS_BASIC','FINANCE_BASIC','BOSS_ADVANCED','FINANCE_ADVANCED')
--    AND menu_id BETWEEN 3325 AND 3331;
-- 存量专业版租户财务克隆角色应获授（预期每租户 7）
-- SELECT t.tenant_code, r.role_id, COUNT(*) FROM sys_role_menu rm
--  JOIN sys_role r ON r.role_id = rm.role_id JOIN sys_tenant t ON t.id = r.tenant_id
--  WHERE rm.menu_id BETWEEN 3325 AND 3331 AND r.role_key = 'finance'
--  GROUP BY t.tenant_code, r.role_id;
