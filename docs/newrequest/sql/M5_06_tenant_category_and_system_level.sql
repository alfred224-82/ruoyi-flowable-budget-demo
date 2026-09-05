-- =====================================================================
-- M5_06 费用大类三层角色分工增量脚本（T1.5 / T5.13）
-- 版本：v1.0  日期：2026-08-30
-- 依据：《资金管控系统开发任务文档》T1.5 / T5.13 三层角色分工
--       admin 维护大类主数据（系统级共享）→ platform_admin 勾选租户启用大类 → boss 录入预算金额
-- 说明：
--   1. fee_expense_category 改为系统级共享，不按 tenant_id 隔离
--   2. 新增 fee_tenant_category 租户大类启用表（platform_admin 勾选，绑定 tenant_id）
--   3. 先删后建/幂等补列，脚本可重复执行
-- =====================================================================

-- ----------------------------
-- 1. fee_expense_category 改为系统级共享
--    保留 tenant_id 列（兼容存量数据），但框架不再自动追加租户条件
--    内置大类 tenant_id 统一为 0（系统级）
-- ----------------------------
-- 将存量内置大类的 tenant_id 从 1 改为 0（系统级）
UPDATE `fee_expense_category` SET `tenant_id` = 0 WHERE `is_builtin` = 1 AND `tenant_id` = 1;

-- 唯一键改为全局唯一（不再按 tenant_id 分组）
-- 先删后建唯一键（幂等）
DROP PROCEDURE IF EXISTS `m5_fix_category_uk`;
DELIMITER $$
CREATE PROCEDURE `m5_fix_category_uk`()
BEGIN
    -- 删除旧的 tenant_id 组合唯一键
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_expense_category' AND INDEX_NAME = 'uk_tenant_code') THEN
        ALTER TABLE `fee_expense_category` DROP INDEX `uk_tenant_code`;
    END IF;
    -- 新增全局唯一键（category_code 全局唯一）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_expense_category' AND INDEX_NAME = 'uk_category_code') THEN
        ALTER TABLE `fee_expense_category` ADD UNIQUE KEY `uk_category_code` (`category_code`);
    END IF;
END$$
DELIMITER ;
CALL m5_fix_category_uk();
DROP PROCEDURE IF EXISTS `m5_fix_category_uk`;

-- ----------------------------
-- 2. 租户大类启用表 fee_tenant_category（T1.5 / T5.13）
--    platform_admin 从 admin 定义的大类中勾选租户需要启用的大类
--    绑定 tenant_id，按租户隔离
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

-- ----------------------------
-- 3. 存量租户自动启用全部内置大类（兼容既有行为）
--    查找所有已有预算编制记录的租户，为其启用全部 6 个内置大类
-- ----------------------------
-- 为 tenant_id=1 的存量租户初始化启用全部内置大类（幂等）
INSERT IGNORE INTO `fee_tenant_category` (`tenant_id`, `category_code`, `category_name`, `is_enabled`, `create_by`)
SELECT 1, `category_code`, `category_name`, 1, 'system-migration'
  FROM `fee_expense_category`
 WHERE `is_builtin` = 1;

-- ----------------------------
-- 4. 验收查询
-- ----------------------------
-- SELECT * FROM fee_expense_category WHERE is_builtin = 1;           -- 预期 6 行，tenant_id 均为 0
-- SELECT * FROM fee_tenant_category WHERE tenant_id = 1;             -- 预期 6 行（存量租户已启用全部大类）
-- SHOW CREATE TABLE fee_tenant_category;
