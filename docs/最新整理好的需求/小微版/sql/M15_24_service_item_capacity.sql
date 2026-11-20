-- =====================================================================
-- M15_24 service_item 补 capacity 列（T15.38 修复，v1.40 新增）
-- 版本：v1.0  日期：2026-09-19
-- 依据：《资金管控系统开发任务文档（小微版）》v1.40
-- 概述：service_item 补 capacity（可容纳人数，整数）列，替代原 description 文本中
--       「可容纳：X人」的非结构化存储，便于后续统计/筛选/排序。
-- 变更内容：
--   1. service_item 补 capacity 列（INT，可空，默认 NULL）
--   2. 存量房间数据从 description 中提取最大人数回填 capacity（幂等，仅 NULL 时更新）
-- =====================================================================

-- ① service_item 补 capacity 列（条件 ALTER，幂等）
DROP PROCEDURE IF EXISTS _m15_24_add_capacity;
DELIMITER $$
CREATE PROCEDURE _m15_24_add_capacity()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_item' AND COLUMN_NAME = 'capacity'
    ) THEN
        ALTER TABLE `service_item` ADD COLUMN `capacity` INT DEFAULT NULL COMMENT '可容纳人数（房间/空间用，项目为 NULL）' AFTER `description`;
    END IF;
END$$
DELIMITER ;
CALL _m15_24_add_capacity();
DROP PROCEDURE IF EXISTS _m15_24_add_capacity;

-- ② 存量房间数据：从 description 中提取最大人数回填 capacity（幂等，仅 NULL 时更新）
--    使用存储过程避免客户端编码问题
DROP PROCEDURE IF EXISTS _m15_24_backfill_capacity;
DELIMITER $$
CREATE PROCEDURE _m15_24_backfill_capacity()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_id BIGINT;
    DECLARE v_desc TEXT;
    DECLARE v_capacity INT;
    DECLARE cur CURSOR FOR
        SELECT id, description FROM service_item
        WHERE capacity IS NULL AND description IS NOT NULL AND description LIKE '%可容纳%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_id, v_desc;
        IF done THEN
            LEAVE read_loop;
        END IF;
        -- 提取「可容纳：」之后、「人」之前的部分，取范围最大值
        SET v_capacity = NULL;
        IF v_desc REGEXP '可容纳[：:][0-9]' THEN
            SET v_capacity = CAST(
                SUBSTRING_INDEX(
                    SUBSTRING_INDEX(
                        SUBSTRING(v_desc, LOCATE('可容纳', v_desc)),
                        '人', 1),
                    '：', -1)
                AS UNSIGNED);
            -- 处理范围 "2-4" → 取 4
            IF v_capacity IS NOT NULL AND v_desc LIKE '%-%人%' THEN
                SET v_capacity = CAST(
                    SUBSTRING_INDEX(
                        SUBSTRING_INDEX(
                            SUBSTRING_INDEX(v_desc, '：', -1),
                        '人', 1),
                    '-', -1)
                    AS UNSIGNED);
            END IF;
        END IF;
        IF v_capacity IS NOT NULL AND v_capacity > 0 THEN
            UPDATE service_item SET capacity = v_capacity WHERE id = v_id;
        END IF;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;
CALL _m15_24_backfill_capacity();
DROP PROCEDURE IF EXISTS _m15_24_backfill_capacity;

-- =====================================================================
-- 验收
-- =====================================================================
-- 1) capacity 列已建立
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME = 'service_item' AND COLUMN_NAME = 'capacity';
-- 预期：1 行（int / NULL / 可容纳人数（房间/空间用，项目为 NULL））
--
-- 2) 存量房间 capacity 已回填
-- SELECT id, name, description, capacity FROM service_item WHERE category_id IN
--   (SELECT id FROM service_category WHERE category_type = 'space');
-- 预期：小房间 capacity=4, 大房间 capacity=10, 大厅桌游位 capacity=NULL（"每位"非数字）
