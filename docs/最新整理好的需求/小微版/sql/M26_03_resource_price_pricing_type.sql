-- =====================================================================
-- M26_03  资源价格设置升级：人员定价支持 4 种计价模式（v1.85）
-- 版本：v1.0  日期：2026-10-17
-- 依据：用户需求——人员定价与房间定价统一采用 jjadmin 四档计价模式
--       （计时/套餐/计量/一口价），不再仅支持小时单价。
-- 前置依赖：M21_03（staff_pricing 建表 + 资源价格设置菜单 7207~7209）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. staff_pricing 表新增 pricing_type + extra_config 两列
--   2. 存量数据迁移：hourly_rate > 0 的记录默认 pricing_type='duration'
-- 保留项：
--   ★ hourly_rate 保留（预约计费兼容，作为基准价）
--   ★ 资源价格设置菜单 7207~7209 保留（前端页面重新设计）
--   ★ IStaffPricingService 保留（batchGetHourlyRates 仍用 hourly_rate）
-- 规范：
--   1. INFORMATION_SCHEMA 幂等 ALTER
--   2. 存量数据 UPDATE WHERE 条件守卫
-- =====================================================================

-- ----------------------------
-- 1. staff_pricing 新增 pricing_type 列（计价模式：duration/package/times/fixed）
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_pricing' AND COLUMN_NAME = 'pricing_type');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `staff_pricing` ADD COLUMN `pricing_type` VARCHAR(16) NOT NULL DEFAULT ''duration'' COMMENT ''计价模式（duration计时/package套餐/times计量/fixed一口价）'' AFTER `hourly_rate`',
    'SELECT ''pricing_type column already exists'' AS skip_info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. staff_pricing 新增 extra_config 列（JSON 扩展配置）
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_pricing' AND COLUMN_NAME = 'extra_config');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `staff_pricing` ADD COLUMN `extra_config` TEXT DEFAULT NULL COMMENT ''扩展配置JSON（套餐/一口价的含时长、超时费等）'' AFTER `pricing_type`',
    'SELECT ''extra_config column already exists'' AS skip_info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 3. 存量数据迁移：hourly_rate > 0 的记录默认 pricing_type='duration'
--    （已有小时单价的助教，计价模式默认为计时）
-- ----------------------------
UPDATE `staff_pricing`
SET `pricing_type` = 'duration'
WHERE `pricing_type` = 'duration'  -- 仅更新默认值记录（幂等）
  AND `hourly_rate` > 0;

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 staff_pricing 表含新列
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_pricing'
--    AND COLUMN_NAME IN ('pricing_type', 'extra_config');
-- 预期：2 行
--
-- 4.2 资源价格设置菜单仍存在
-- SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id IN (7207, 7208, 7209);
-- 预期：3 行（7207 C + 7208/7209 F）
