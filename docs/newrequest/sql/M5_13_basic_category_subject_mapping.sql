-- =====================================================================
-- M5_13 基础版「费用大类 ↔ 预算科目对应关系」补齐脚本（映射B 下放基础版）
-- 版本：v1.12  日期：2026-09-06
-- 背景：
--   基础版「项目立项」(菜单 3201 / ruoyi-ui fee/project/create.vue) 按大类分配项目预算，
--   依赖三件套：
--     ① fee_expense_category        费用大类主数据（大类下拉 /fee/category/activeList）
--     ② fee_tenant_category         租户大类启用表（activeList 存量兼容判定依赖此表存在）
--     ③ budget_subject.category_code 映射B（科目下拉 /system/subject/all 按 categoryCode 联动过滤）
--   上述体系原属高级版 M5_02 + M5_06，基础版目录缺失，独立部署时立项「按大类选科目」失效：
--     activeList 因 fee_tenant_category 表不存在报 500；budget_subject 无 category_code 无法联动。
-- 目标：以纯 SQL 在基础版一次性补齐，前后端零改动；表结构与 M5_02 / M5_06 最终态幂等一致。
-- 规范：
--   1. 先删后建 / 条件 ALTER / 只填空值，脚本可重复执行（幂等）；
--   2. fee_expense_category 为系统级共享表（已在 MybatisPlusConfig.IGNORE_TENANT_TABLES），
--      内置大类 tenant_id=0，category_code 全局唯一（对齐 M5_06 系统级改造最终态）；
--   3. 映射B：预算科目 → 费用大类，由 admin 内置固定、前端不可改，按科目名称关键字回填，
--      仅填 category_code IS NULL 的行，不覆盖已有映射。
-- =====================================================================

-- ----------------------------
-- 1. 费用大类主数据表（系统级共享；= M5_02 建表 + M5_06 系统级改造最终态）
-- ----------------------------
DROP TABLE IF EXISTS `fee_expense_category`;
CREATE TABLE `fee_expense_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID(0=系统级共享，框架不追加租户条件)',
  `category_code` VARCHAR(50) NOT NULL COMMENT '大类编码(全局唯一，如 TRAVEL)',
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
  UNIQUE KEY `uk_category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用大类主数据表';

-- 内置 6 大类（系统级 tenant_id=0；新租户开通时按 T2.4 初始化逻辑复制启用）
DELETE FROM `fee_expense_category` WHERE `is_builtin` = 1;
INSERT INTO `fee_expense_category` (`tenant_id`, `category_code`, `category_name`, `sort_order`, `is_active`, `is_builtin`, `remark`, `create_by`) VALUES
(0, 'TRAVEL',      '差旅招待', 1, 1, 1, '内置大类：差旅费/住宿费/业务招待费', 'admin'),
(0, 'PROCUREMENT', '采购支出', 2, 1, 1, '内置大类：材料/设备/服务采购', 'admin'),
(0, 'PERSONNEL',   '人力成本', 3, 1, 1, '内置大类：工资/社保/公积金', 'admin'),
(0, 'OFFICE',      '办公费用', 4, 1, 1, '内置大类：房租/水电/办公用品', 'admin'),
(0, 'MARKETING',   '市场推广', 5, 1, 1, '内置大类：广告/推广/活动', 'admin'),
(0, 'OTHER',       '其他支出', 6, 1, 1, '内置大类：未归类费用兜底', 'admin');

-- ----------------------------
-- 2. 租户大类启用表（platform_admin 勾选；activeList 存量兼容判定依赖此表存在，= M5_06）
-- ----------------------------
DROP TABLE IF EXISTS `fee_tenant_category`;
CREATE TABLE `fee_tenant_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `category_code` VARCHAR(50) NOT NULL COMMENT '费用大类编码（关联 fee_expense_category.category_code）',
  `category_name` VARCHAR(100) COMMENT '大类名称（冗余，便于展示）',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用（0-停用 1-启用）',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant_category` (`tenant_id`, `category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户大类启用表（platform_admin 勾选）';

-- 为存量租户 tenant_id=1 启用全部内置大类（幂等；无配置时 activeList 亦走存量兼容返回全部启用大类）
INSERT IGNORE INTO `fee_tenant_category` (`tenant_id`, `category_code`, `category_name`, `is_enabled`, `create_by`)
SELECT 1, `category_code`, `category_name`, 1, 'system-migration'
  FROM `fee_expense_category`
 WHERE `is_builtin` = 1;

-- ----------------------------
-- 3. 映射B：预算科目表补费用大类编码列（存量表幂等补列，= M5_02）
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

-- ----------------------------
-- 4. 映射B 内置初始化：按科目名称关键字回填，仅填空值不覆盖已有映射（可重复执行，= M5_02）
--    执行顺序即优先级：TRAVEL → PROCUREMENT → PERSONNEL → OFFICE → MARKETING → OTHER(费用类兜底)
--    映射范围限定支出侧科目（EXPENSE 费用类 / COST 成本类）：种子 10xx 树误标 INCOME 且与
--    20xx/30xx 支出树重名，不限类型时同名科目（差旅费/业务招待费等）会被双重命中，
--    立项下拉出现重复选项；故先清非支出侧误映射，再按关键字回填。
-- ----------------------------
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
-- SELECT category_code, category_name FROM fee_expense_category WHERE is_builtin = 1;              -- 预期 6 行，tenant_id 均为 0
-- SELECT COUNT(*) FROM fee_tenant_category WHERE tenant_id = 1;                                     -- 预期 6 行
-- SELECT subject_code, subject_name, category_code FROM budget_subject
--   WHERE category_code IS NOT NULL ORDER BY category_code, subject_code;                          -- 映射B 已回填
-- SELECT COUNT(*) FROM budget_subject WHERE category_code IS NULL AND subject_type = 'EXPENSE';     -- 预期 0（费用类全部归类）
-- SELECT COUNT(*) FROM budget_subject WHERE category_code IS NOT NULL AND subject_type NOT IN ('EXPENSE','COST'); -- 预期 0（映射B 仅支出侧）
-- SELECT subject_code, subject_name, category_code FROM budget_subject
--   WHERE tenant_id = 0 AND category_code = 'TRAVEL';   -- 预期 2 行（3001004/3002002，无重名）
