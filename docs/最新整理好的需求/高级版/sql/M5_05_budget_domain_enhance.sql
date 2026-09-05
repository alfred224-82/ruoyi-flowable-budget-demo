-- =====================================================================
-- M5_05 预算域增强脚本（T5.14②拆分初稿 / T5.16财务科目导入 / T5.17大类管控 / T5.18发票验真）
-- 版本：v1.0  日期：2026-08-30
-- 依据：《资金管控系统开发任务文档》v1.2 T5.14~T5.18
--       《资金管控系统设计文档》1.3 科目三层 / 2.2 #1~#6 / 2.3
-- 规范：
--   1. 补列走存储过程幂等，菜单先删后插，脚本可重复执行
--   2. 新增列均服务多租户既有表（租户过滤由拦截器统一处理）
-- =====================================================================

-- ----------------------------
-- 1. fee_expense_bill 补列（T5.17 大类维度管控 + T5.18 发票验真与匹配依据）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m5_05_alter_expense_bill`;
DELIMITER $$
CREATE PROCEDURE `m5_05_alter_expense_bill`()
BEGIN
    -- 前置：expense_period 由预算编制迭代引入，部分环境缺失，幂等补列并按费用日期回填（Error 1072 修复）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_expense_bill' AND COLUMN_NAME = 'expense_period') THEN
        ALTER TABLE `fee_expense_bill`
            ADD COLUMN `expense_period` VARCHAR(7) DEFAULT NULL COMMENT '报销年月（格式：YYYY-MM，对应预算编制期间）' AFTER `expense_date`;
        UPDATE `fee_expense_bill` SET `expense_period` = DATE_FORMAT(`expense_date`, '%Y-%m') WHERE `expense_period` IS NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_expense_bill' AND COLUMN_NAME = 'category_code') THEN
        ALTER TABLE `fee_expense_bill`
            ADD COLUMN `category_code` VARCHAR(50) DEFAULT NULL COMMENT '费用大类编码(映射B定位，T5.17 大类管控维度)' AFTER `calc_accounting_class`,
            ADD KEY `idx_category_period` (`category_code`, `expense_period`);
    END IF;
    -- 前置：invoice_defer 由 M5_01 引入，防止未执行 M5_01 的环境因 AFTER 子句报 1054，幂等补列（与 M5_01 定义一致）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_expense_bill' AND COLUMN_NAME = 'invoice_defer') THEN
        ALTER TABLE `fee_expense_bill`
            ADD COLUMN `invoice_defer` TINYINT DEFAULT 0 COMMENT '是否发票后补(0否 1是)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_expense_bill' AND COLUMN_NAME = 'invoice_no') THEN
        ALTER TABLE `fee_expense_bill`
            ADD COLUMN `invoice_no` VARCHAR(50) DEFAULT NULL COMMENT '发票号码(T5.18 验真/查重)' AFTER `invoice_defer`,
            ADD COLUMN `invoice_amount` DECIMAL(14,2) DEFAULT NULL COMMENT '发票金额(与报销金额比对，T5.18)' AFTER `invoice_no`,
            ADD COLUMN `invoice_verify_status` VARCHAR(20) DEFAULT 'UNVERIFIED' COMMENT '发票验真状态(UNVERIFIED/VERIFIED/MISMATCH，T5.18)' AFTER `invoice_amount`,
            ADD COLUMN `match_stage` VARCHAR(20) DEFAULT NULL COMMENT '科目匹配阶段依据(EXACT/KEYWORD/FALLBACK/MANUAL，T5.18)' AFTER `invoice_verify_status`;
    END IF;
END$$
DELIMITER ;

CALL m5_05_alter_expense_bill();
DROP PROCEDURE IF EXISTS `m5_05_alter_expense_bill`;

-- ----------------------------
-- 2. 菜单：财务科目导入页（T5.16，挂预算管理目录 2000，与 2003 预算科目同级）
--    拆分初稿按钮（T5.14②，挂预算编制 2001 下）
--    menu_id 顺延占用 3316~3322（3310~3315 已被 M5_03 费用大类占用）
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (3316, 3317, 3318, 3319, 3320, 3321, 3322);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3316, '财务科目导入', 2000, 11, 'fee/financeSubject', 'fee/financeSubject/index', NULL, 1, 0, 'C', '0', '0', 'fee:financeSubject:list', 'upload', 'admin', NOW(), '', NULL, '基础版(T5.16 客户财务科目导入+映射A)');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3317, '科目导入',     3316, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:financeSubject:import', '#', 'admin', NOW(), '', NULL, '支持7套财务软件模板格式'),
(3318, '科目查询',     3316, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:financeSubject:query',  '#', 'admin', NOW(), '', NULL, ''),
(3319, '自动匹配',     3316, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:financeSubject:match',  '#', 'admin', NOW(), '', NULL, '关键字自动匹配建立映射A'),
(3320, '人工映射',     3316, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:financeSubject:map',    '#', 'admin', NOW(), '', NULL, '人工补完/纠偏映射A'),
(3321, '科目删除',     3316, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:financeSubject:remove', '#', 'admin', NOW(), '', NULL, ''),
(3322, '生成拆分初稿', 2001, 8, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:preparation:splitDraft', '#', 'admin', NOW(), '', NULL, 'T5.14② 大类额度按比例拆科目(历史实绩优先，无则财务默认比例)');

-- ----------------------------
-- 3. 三版本授权模板登记（基础版能力，三版本均含）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` IN (3316, 3317, 3318, 3319, 3320, 3321, 3322);
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('BASIC', 3316), ('BASIC', 3317), ('BASIC', 3318), ('BASIC', 3319), ('BASIC', 3320), ('BASIC', 3321), ('BASIC', 3322),
('ADVANCED', 3316), ('ADVANCED', 3317), ('ADVANCED', 3318), ('ADVANCED', 3319), ('ADVANCED', 3320), ('ADVANCED', 3321), ('ADVANCED', 3322),
('PROFESSIONAL', 3316), ('PROFESSIONAL', 3317), ('PROFESSIONAL', 3318), ('PROFESSIONAL', 3319), ('PROFESSIONAL', 3320), ('PROFESSIONAL', 3321), ('PROFESSIONAL', 3322);

-- ----------------------------
-- 4. 既有五角色授权补充（幂等：先删后插）
-- 财务(9202)：财务科目导入完整维护 + 拆分初稿生成；老板(9201)：科目只读
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE (`role_id` = 9202 AND `menu_id` IN (3316, 3317, 3318, 3319, 3320, 3321, 3322))
   OR (`role_id` = 9201 AND `menu_id` IN (3316, 3318));
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9202, 3316), (9202, 3317), (9202, 3318), (9202, 3319), (9202, 3320), (9202, 3321), (9202, 3322),
(9201, 3316), (9201, 3318);

-- ----------------------------
-- 验收查询
-- ----------------------------
-- SHOW COLUMNS FROM fee_expense_bill LIKE 'category_code';        -- 预期存在
-- SHOW COLUMNS FROM fee_expense_bill LIKE 'invoice_verify_status'; -- 预期存在
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3316 AND 3322;  -- 预期 7 行
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template WHERE menu_id BETWEEN 3316 AND 3322 GROUP BY edition;  -- 各 7
