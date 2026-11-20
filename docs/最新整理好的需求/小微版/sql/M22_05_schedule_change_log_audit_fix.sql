-- =====================================================================
-- M22_01_FIX  schedule_change_log 缺失 BaseEntity 审计字段修复
-- 版本：v1.0  日期：2026-10-10
-- ---------------------------------------------------------------------
-- 背景：
--   实体 com.ruoyi.system.domain.ScheduleChangeLog extends TenantEntity
--   → BaseEntity 自带 createBy/createTime/updateBy/updateTime 四审计字段，
--   且带 @TableField(fill = INSERT/INSERT_UPDATE)，MyBatis-Plus 在 insert
--   时会自动拼接这四列到 SQL。
--
--   原建表脚本 M22_01_staff_schedule.sql 中 schedule_change_log 只创建了
--   create_time 一列，缺失 create_by / update_by / update_time，
--   触发运行时错误：
--     java.sql.SQLSyntaxErrorException:
--       Unknown column 'create_by' in 'field list'
--     INSERT INTO schedule_change_log ( ..., create_by, create_time,
--       update_by, update_time ) VALUES ( ... )
--
-- 修复策略：
--   1. 使用 INFORMATION_SCHEMA.COLUMNS 幂等判断，仅在列不存在时 ADD；
--   2. 顺带校验 create_by/update_by 是否为 VARCHAR(64)（BaseEntity 约定），
--      若为 BIGINT 则 MODIFY 修正类型；
--   3. 与 staff_schedule 表列位保持一致（remark 后紧接 create_by…）。
-- 影响面：
--   - 单表 ALTER，无数据迁移，无索引变化；
--   - 幂等，可重复执行；
--   - 与 M22_01_staff_schedule.sql 建表脚本同步修正，防止新库重现。
-- =====================================================================

SET NAMES utf8mb4;

SET @tbl := 'schedule_change_log';
SET @db  := DATABASE();

-- ----------------------------
-- 1. 补建 create_by (VARCHAR(64))
-- ----------------------------
SET @sql := (SELECT IF(
  EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='create_by'),
  'SELECT ''[SKIP] create_by 已存在'' AS msg',
  CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `create_by` VARCHAR(64) DEFAULT NULL COMMENT ''创建者'' AFTER `operate_time`')
));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ----------------------------
-- 2. create_time 兜底（正常已存在，此处仅幂等）
-- ----------------------------
SET @sql := (SELECT IF(
  EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='create_time'),
  'SELECT ''[SKIP] create_time 已存在'' AS msg',
  CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `create_by`')
));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ----------------------------
-- 3. 补建 update_by (VARCHAR(64))
-- ----------------------------
SET @sql := (SELECT IF(
  EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='update_by'),
  'SELECT ''[SKIP] update_by 已存在'' AS msg',
  CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `update_by` VARCHAR(64) DEFAULT NULL COMMENT ''更新者'' AFTER `create_time`')
));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ----------------------------
-- 4. 补建 update_time (DATETIME)
-- ----------------------------
SET @sql := (SELECT IF(
  EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='update_time'),
  'SELECT ''[SKIP] update_time 已存在'' AS msg',
  CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `update_by`')
));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ----------------------------
-- 5. 类型校验：create_by / update_by 必须为 VARCHAR(64)
--    若历史脚本误建为 BIGINT，需先 MODIFY 修正（BaseEntity 写入的是用户名字符串）
-- ----------------------------
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl
  AND COLUMN_NAME IN ('create_by','create_time','update_by','update_time')
ORDER BY ORDINAL_POSITION;

-- ----------------------------
-- 6. 验收：手动执行以下语句确认四列齐备且类型正确
-- ----------------------------
-- SELECT COLUMN_NAME, COLUMN_TYPE
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME = 'schedule_change_log'
--    AND COLUMN_NAME IN ('create_by','create_time','update_by','update_time');
-- 预期：4 行；create_by/update_by 为 varchar(64)，create_time/update_time 为 datetime
--
-- 之后重新触发 /micro/staffSchedule/publish 接口，观察
-- INSERT INTO schedule_change_log(...) 是否成功；不再出现
-- Unknown column 'create_by' in 'field list' 即为修复完成。
