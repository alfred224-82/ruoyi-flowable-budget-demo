-- =====================================================================
-- M5_02 预算域增量建表脚本（T1.5：费用大类主数据 / 映射B / 拆分比例 / 财务科目导入）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控系统设计文档》1.3 科目三层 / 2.2 #3 / 2.6 数据模型
--       《资金管控系统开发任务文档》T1.5、T5.13、T5.14
-- 规范：
--   1. 逐表含 tenant_id + 租户索引（全局多租户硬约束，设计文档 1.2）
--   2. 先删后建，脚本可重复执行；补列走存储过程幂等
--   3. 科目三层：财务科目 --映射A(租户级)--> 预算科目 --映射B(内置)--> 费用大类
-- =====================================================================

-- ----------------------------
-- 1. 费用大类主数据表（T5.13：admin 维护，约 6 个大类）
--    大类额度为预算编制与报销管控的上层限额（设计文档 2.2 #3）
-- ----------------------------
DROP TABLE IF EXISTS `fee_expense_category`;
CREATE TABLE `fee_expense_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `category_code` VARCHAR(50) NOT NULL COMMENT '大类编码(租户内唯一，如 TRAVEL)',
  `category_name` VARCHAR(100) NOT NULL COMMENT '大类名称',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否启用(0-禁用 1-启用)',
  `is_builtin` TINYINT DEFAULT 0 COMMENT '是否内置(1-内置不可删除，可停用)',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`, `category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用大类主数据表';

-- 内置约 6 个大类（默认租户初始化；新租户开通时按 T2.4 初始化逻辑复制）
DELETE FROM `fee_expense_category` WHERE `tenant_id` = 1 AND `is_builtin` = 1;
INSERT INTO `fee_expense_category` (`tenant_id`, `category_code`, `category_name`, `sort_order`, `is_active`, `is_builtin`, `remark`, `create_by`) VALUES
(1, 'TRAVEL',      '差旅招待', 1, 1, 1, '内置大类：差旅费/住宿费/业务招待费', 'admin'),
(1, 'PROCUREMENT', '采购支出', 2, 1, 1, '内置大类：材料/设备/服务采购', 'admin'),
(1, 'PERSONNEL',   '人力成本', 3, 1, 1, '内置大类：工资/社保/公积金', 'admin'),
(1, 'OFFICE',      '办公费用', 4, 1, 1, '内置大类：房租/水电/办公用品', 'admin'),
(1, 'MARKETING',   '市场推广', 5, 1, 1, '内置大类：广告/推广/活动', 'admin'),
(1, 'OTHER',       '其他支出', 6, 1, 1, '内置大类：未归类费用兜底', 'admin');

-- ----------------------------
-- 2. 大类默认拆分比例表（T5.14：财务设置；编制无历史实绩时按此比例拆科目）
-- ----------------------------
DROP TABLE IF EXISTS `fee_category_split_ratio`;
CREATE TABLE `fee_category_split_ratio` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `category_id` BIGINT NOT NULL COMMENT '费用大类ID',
  `category_code` VARCHAR(50) COMMENT '大类编码(冗余)',
  `subject_code` VARCHAR(20) NOT NULL COMMENT '预算科目编码',
  `subject_name` VARCHAR(100) COMMENT '预算科目名称(冗余)',
  `ratio_percent` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '拆分比例%(同一大类合计=100)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_category` (`tenant_id`, `category_id`),
  UNIQUE KEY `uk_tenant_category_subject` (`tenant_id`, `category_id`, `subject_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大类默认拆分比例表';

-- ----------------------------
-- 3. 财务科目导入表（T5.16：客户自有财务科目导入存储 + 映射A，此处仅建表）
-- ----------------------------
DROP TABLE IF EXISTS `fee_finance_subject`;
CREATE TABLE `fee_finance_subject` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `import_batch_no` VARCHAR(64) COMMENT '导入批次号',
  `source_system` VARCHAR(30) COMMENT '来源系统 KINGDEE/YONYOU/CHANJET/SUPERDATA/GRASP/LEMONCLOUD/GENERAL',
  `subject_code` VARCHAR(50) NOT NULL COMMENT '财务科目编码(客户自有)',
  `subject_name` VARCHAR(200) NOT NULL COMMENT '财务科目名称',
  `parent_code` VARCHAR(50) COMMENT '父级科目编码',
  `subject_level` INT DEFAULT 1 COMMENT '科目层级',
  `mapped_subject_code` VARCHAR(20) COMMENT '映射A：对应预算科目编码',
  `match_type` VARCHAR(20) DEFAULT NULL COMMENT '映射方式 AUTO(关键字自动)/MANUAL(人工补完)',
  `status` VARCHAR(20) DEFAULT 'UNMAPPED' COMMENT 'UNMAPPED未映射/MAPPED已映射/IGNORED忽略',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_batch` (`tenant_id`, `import_batch_no`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`, `subject_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务科目导入表(映射A载体)';

-- ----------------------------
-- 4. 映射B：预算科目表补费用大类编码（系统内置固定，财务可查看不可随意变更）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m5_add_subject_category`;
DELIMITER $$
CREATE PROCEDURE `m5_add_subject_category`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budget_subject' AND COLUMN_NAME = 'category_code') THEN
        ALTER TABLE `budget_subject`
            ADD COLUMN `category_code` VARCHAR(50) DEFAULT NULL COMMENT '费用大类编码(映射B，系统内置)' AFTER `subject_type`,
            ADD KEY `idx_category_code` (`category_code`);
    END IF;
END$$
DELIMITER ;

CALL m5_add_subject_category();
DROP PROCEDURE IF EXISTS `m5_add_subject_category`;

-- 映射B内置初始化（按科目名称关键字回填，仅填空值不覆盖已有映射；可重复执行）
--   映射范围限定支出侧科目（EXPENSE 费用类 / COST 成本类）：种子 10xx 树误标 INCOME 且与
--   20xx/30xx 支出树重名，不限类型时同名科目（差旅费/业务招待费等）会被双重命中，
--   立项/编制下拉出现重复选项；故先清非支出侧误映射，再按关键字回填。
UPDATE `budget_subject` SET `category_code` = NULL
 WHERE `category_code` IS NOT NULL AND `subject_type` NOT IN ('EXPENSE', 'COST');
UPDATE `budget_subject` SET `category_code` = 'TRAVEL'      WHERE `category_code` IS NULL AND `subject_type` IN ('EXPENSE', 'COST') AND (`subject_name` LIKE '%差旅%' OR `subject_name` LIKE '%招待%');
UPDATE `budget_subject` SET `category_code` = 'PROCUREMENT' WHERE `category_code` IS NULL AND `subject_type` IN ('EXPENSE', 'COST') AND (`subject_name` LIKE '%材料%' OR `subject_name` LIKE '%采购%' OR `subject_name` LIKE '%设备%');
UPDATE `budget_subject` SET `category_code` = 'PERSONNEL'   WHERE `category_code` IS NULL AND `subject_type` IN ('EXPENSE', 'COST') AND (`subject_name` LIKE '%薪%'   OR `subject_name` LIKE '%人工%' OR `subject_name` LIKE '%社保%');
UPDATE `budget_subject` SET `category_code` = 'OFFICE'      WHERE `category_code` IS NULL AND `subject_type` IN ('EXPENSE', 'COST') AND (`subject_name` LIKE '%办公%' OR `subject_name` LIKE '%水电%' OR `subject_name` LIKE '%房租%');
UPDATE `budget_subject` SET `category_code` = 'MARKETING'   WHERE `category_code` IS NULL AND `subject_type` IN ('EXPENSE', 'COST') AND (`subject_name` LIKE '%广告%' OR `subject_name` LIKE '%推广%');
UPDATE `budget_subject` SET `category_code` = 'OTHER'       WHERE `category_code` IS NULL AND `subject_type` = 'EXPENSE'
    AND `subject_name` NOT LIKE '%差旅%' AND `subject_name` NOT LIKE '%招待%' AND `subject_name` NOT LIKE '%薪%'
    AND `subject_name` NOT LIKE '%办公%' AND `subject_name` NOT LIKE '%广告%' AND `subject_name` NOT LIKE '%推广%';

-- ----------------------------
-- 验收查询
-- ----------------------------
-- SELECT category_code, category_name FROM fee_expense_category WHERE tenant_id = 1;   -- 预期 6 行
-- SELECT COUNT(*) FROM budget_subject WHERE category_code IS NOT NULL;                  -- 映射B已回填
-- SHOW CREATE TABLE fee_category_split_ratio;
-- SHOW CREATE TABLE fee_finance_subject;
