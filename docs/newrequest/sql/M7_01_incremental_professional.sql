-- =====================================================================
-- M7_01 专业版增量脚本（T7.1~T7.6）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控系统开发任务文档》M7 / 《资金管控系统设计文档》4.2~4.4
-- 规范：
--   1. 先删后建 / 条件 ALTER，脚本可重复执行
--   2. 逐表含 tenant_id + 租户索引
--   3. 建表不等于开放，开放由功能开关（FEAT_*）控制
-- 说明：专业版增量表主体（fee_org/fee_org_budget_version/
--       fee_face_verify_record/fee_rule_version）已在 M1_03 建齐，
--       本脚本仅补用户-组织关系表与存量表字段扩展
-- =====================================================================

-- ----------------------------
-- 1. 用户-组织关系表（FEAT_MULTI_ORG；用户挂组织，设计文档 4.2.1）
-- ----------------------------
DROP TABLE IF EXISTS `fee_org_user`;
CREATE TABLE `fee_org_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `org_id` BIGINT NOT NULL COMMENT '组织ID(fee_org)',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `user_name` VARCHAR(64) DEFAULT NULL COMMENT '用户姓名(冗余)',
  `is_primary` TINYINT DEFAULT 1 COMMENT '是否主组织(多组织用户登录选择用)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_org` (`org_id`),
  KEY `idx_user` (`user_id`),
  UNIQUE KEY `uk_tenant_org_user` (`tenant_id`, `org_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-组织关系表';

-- ----------------------------
-- 2. 存量表字段扩展（幂等条件 ALTER）
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

-- 预算编制关联规则版本快照（FEAT_RULE_GRAYSCALE；锁定预算使用当时规则快照，设计文档 4.2.4）
CALL sp_add_column_if_missing('budget_preparation', 'rule_version_id',
  '`rule_version_id` BIGINT DEFAULT NULL COMMENT ''锁定时的规则版本快照ID(fee_rule_version,FEAT_RULE_GRAYSCALE)''');

-- 租户物理隔离模式（FEAT_ISOLATED_SCHEMA；预留，设计文档 4.2.6）
CALL sp_add_column_if_missing('sys_tenant', 'isolation_mode',
  '`isolation_mode` VARCHAR(20) DEFAULT ''SHARED'' COMMENT ''隔离模式 SHARED共享表/DEDICATED独立Schema(预留,FEAT_ISOLATED_SCHEMA)''');

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
