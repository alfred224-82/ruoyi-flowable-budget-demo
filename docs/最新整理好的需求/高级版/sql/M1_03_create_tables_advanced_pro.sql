-- =====================================================================
-- M1_03 高级版/专业版增量表建表脚本（T1.2）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控系统设计文档》3.4 / 4.4 增量数据模型
--       《资金管控系统开发任务文档》T1.2
-- 规范：
--   1. 高级/专业表一次建齐（一套系统策略：建表 ≠ 开放，开放由功能开关控制）
--   2. 逐表含 tenant_id + 租户索引
--   3. 先删后建 / 条件 ALTER，脚本可重复执行
-- =====================================================================

-- =====================================================================
-- 第一部分：高级版增量表
-- =====================================================================

-- ----------------------------
-- 1. 员工信用分表（FEAT_CREDIT_SCORE；基础版仅建表不使用，高级版启用）
-- ----------------------------
DROP TABLE IF EXISTS `fee_employee_credit`;
CREATE TABLE `fee_employee_credit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `credit_score` INT DEFAULT 100 COMMENT '当前信用分(默认100)',
  `credit_level` VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'EXCELLENT优秀≥80/NORMAL正常50-79/RESTRICTED受限<50',
  `on_time_count` INT DEFAULT 0 COMMENT '按时补票次数',
  `overdue_count` INT DEFAULT 0 COMMENT '逾期次数',
  `consecutive_on_time` INT DEFAULT 0 COMMENT '连续按时次数',
  `last_calc_date` DATE COMMENT '最近重算日期(creditScoreCalcJob每周一)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工信用分表';

-- ----------------------------
-- 2. 信用分规则配置表（按时+2/逾期-5/连续3次+10，财务可维护）
-- ----------------------------
DROP TABLE IF EXISTS `fee_credit_rule`;
CREATE TABLE `fee_credit_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `event_type` VARCHAR(30) NOT NULL COMMENT 'ON_TIME补票按时/OVERDUE补票逾期/CONSECUTIVE_BONUS连续按时奖励',
  `score_delta` INT NOT NULL COMMENT '加减分值(+2/-5/+10)',
  `threshold` INT DEFAULT NULL COMMENT '触发阈值(连续次数等)',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant_event` (`tenant_id`, `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用分规则配置表';

-- 内置默认信用规则（先删后插幂等）
DELETE FROM `fee_credit_rule` WHERE `tenant_id` = 0;
INSERT INTO `fee_credit_rule` (`tenant_id`, `event_type`, `score_delta`, `threshold`, `is_enabled`) VALUES
(0, 'ON_TIME', 2, NULL, 1),
(0, 'OVERDUE', -5, NULL, 1),
(0, 'CONSECUTIVE_BONUS', 10, 3, 1);

-- ----------------------------
-- 3. 打卡记录表（FEAT_TIME_CLOCK；定位存疑仅标记不拦截，ADR-014）
-- ----------------------------
DROP TABLE IF EXISTS `fee_clock_record`;
CREATE TABLE `fee_clock_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `bill_id` BIGINT NOT NULL COMMENT '关联报销单',
  `user_id` BIGINT NOT NULL COMMENT '打卡用户',
  `trigger_rule` VARCHAR(50) COMMENT '触发规则(TRAVEL_CATEGORY/AMOUNT_GE_500/HISTORY_ABNORMAL)',
  `longitude` DECIMAL(10,7) COMMENT '经度',
  `latitude` DECIMAL(10,7) COMMENT '纬度',
  `address` VARCHAR(500) COMMENT '定位地址',
  `accuracy` DECIMAL(8,2) COMMENT '定位精度(米)',
  `photo_urls` TEXT COMMENT '现场照片',
  `clock_time` DATETIME COMMENT '打卡时间',
  `is_suspicious` TINYINT DEFAULT 0 COMMENT '定位存疑(精度>50米)',
  `evidence_level` VARCHAR(10) DEFAULT 'medium' COMMENT '证据等级 strong/medium/weak',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_bill` (`bill_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录表';

-- ----------------------------
-- 4. 映射规则变更审计表（FEAT_RULE_ENGINE_CUSTOM；草稿→发布留痕，ADR-008）
-- ----------------------------
DROP TABLE IF EXISTS `fee_mapping_audit_log`;
CREATE TABLE `fee_mapping_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `rule_id` BIGINT NOT NULL COMMENT '映射规则ID',
  `action` VARCHAR(20) NOT NULL COMMENT 'CREATE/UPDATE/PUBLISH/ROLLBACK/DELETE',
  `draft_content` JSON COMMENT '变更前内容',
  `publish_content` JSON COMMENT '变更后内容',
  `impact_analysis` JSON COMMENT '影响分析(影响预算/报销数量)',
  `oper_user_id` BIGINT COMMENT '操作人',
  `oper_user_name` VARCHAR(50) COMMENT '操作人姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_rule` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='映射规则变更审计表';

-- ----------------------------
-- 5. 弹性额度配置表（FEAT_ELASTIC_QUOTA；部门大类超支阈值内放行）
-- ----------------------------
DROP TABLE IF EXISTS `fee_elastic_quota_config`;
CREATE TABLE `fee_elastic_quota_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `category_id` BIGINT COMMENT '费用大类ID(可空=全局)',
  `threshold_pct` DECIMAL(5,2) DEFAULT 10.00 COMMENT '弹性阈值百分比(如+10%)',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='弹性额度配置表';

-- =====================================================================
-- 第二部分：专业版增量表
-- =====================================================================

-- ----------------------------
-- 6. 组织树表（FEAT_MULTI_ORG；租户内二级维度，设计文档 4.2.1）
-- ----------------------------
DROP TABLE IF EXISTS `fee_org`;
CREATE TABLE `fee_org` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `org_code` VARCHAR(50) NOT NULL COMMENT '组织编码',
  `org_name` VARCHAR(200) NOT NULL COMMENT '组织名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父组织ID(0=集团总部)',
  `ancestors` VARCHAR(500) DEFAULT '' COMMENT '祖级列表',
  `leader_user_id` BIGINT COMMENT '负责人',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant_org_code` (`tenant_id`, `org_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织树表(租户内)';

-- ----------------------------
-- 7. 组织级预算版本快照表（FEAT_ORG_SNAPSHOT；组织分别编制与锁定）
-- ----------------------------
DROP TABLE IF EXISTS `fee_org_budget_version`;
CREATE TABLE `fee_org_budget_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `org_id` BIGINT NOT NULL COMMENT '组织ID',
  `period` CHAR(7) NOT NULL COMMENT '预算期间 yyyy-MM',
  `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本号(锁定后+1,不覆盖)',
  `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/LOCKED',
  `snapshot_data` JSON COMMENT '预算快照(锁定后不可变)',
  `locked_by` BIGINT COMMENT '锁定人',
  `locked_at` DATETIME COMMENT '锁定时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_org_period_version` (`tenant_id`, `org_id`, `period`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织级预算版本快照表';

-- ----------------------------
-- 8. 人脸核身记录表（FEAT_FACE_RECOGNITION；存疑仅标记不拦截，设计文档 4.2.2）
-- ----------------------------
DROP TABLE IF EXISTS `fee_face_verify_record`;
CREATE TABLE `fee_face_verify_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `bill_id` BIGINT COMMENT '关联报销单',
  `user_id` BIGINT NOT NULL COMMENT '核身用户',
  `amount` DECIMAL(14,2) DEFAULT 0 COMMENT '触发金额(默认阈值≥2000)',
  `trigger_type` VARCHAR(30) COMMENT 'AMOUNT_THRESHOLD/SENSITIVE_CATEGORY',
  `face_image_url` VARCHAR(500) COMMENT '采集人脸图',
  `reference_image_url` VARCHAR(500) COMMENT '入职底照',
  `result` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'MATCH/MISMATCH/PENDING',
  `similarity` DECIMAL(5,4) COMMENT '比对相似度',
  `is_suspicious` TINYINT DEFAULT 0 COMMENT '人脸存疑(进异常池)',
  `evidence_level` VARCHAR(10) DEFAULT 'strong' COMMENT '证据等级(strong)',
  `verify_time` DATETIME COMMENT '核身时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸核身记录表';

-- ----------------------------
-- 9. 规则引擎版本快照表（FEAT_RULE_GRAYSCALE；灰度+锁定预算用当时快照）
-- ----------------------------
DROP TABLE IF EXISTS `fee_rule_version`;
CREATE TABLE `fee_rule_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `version_no` INT NOT NULL COMMENT '规则版本号',
  `rule_snapshot` JSON COMMENT '规则快照内容',
  `grayscale_pct` DECIMAL(5,2) DEFAULT 100.00 COMMENT '灰度百分比(新规则默认10%)',
  `grayscale_status` VARCHAR(20) DEFAULT 'FULL' COMMENT 'GRAYSCALE灰度中/FULL全量/ROLLBACK回滚',
  `hit_rate` DECIMAL(5,4) DEFAULT NULL COMMENT '灰度期命中率(比对用)',
  `publish_by` BIGINT COMMENT '发布人',
  `publish_at` DATETIME COMMENT '发布时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant_version` (`tenant_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则引擎版本快照表';

-- =====================================================================
-- 第三部分：存量表字段扩展（幂等条件 ALTER）
-- =====================================================================

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

-- 预算双粒度（FEAT_DEPT_BUDGET，高级版启用，设计文档 3.2.3）
CALL sp_add_column_if_missing('budget_preparation', 'department_id',
  '`department_id` BIGINT DEFAULT NULL COMMENT ''部门ID(高级版部门级预算双粒度)''');
CALL sp_add_column_if_missing('budget_preparation_detail', 'department_id',
  '`department_id` BIGINT DEFAULT NULL COMMENT ''部门ID(高级版部门级预算双粒度)''');

-- 报销单关联项目（Phase 23.3：project_id，0=公司级/未归类）
CALL sp_add_column_if_missing('fee_expense_bill', 'project_id',
  '`project_id` BIGINT DEFAULT 0 COMMENT ''关联项目(0=公司级/未归类)''');

-- 报销单证据等级（FEAT_EVIDENCE_CHAIN，高级版）
CALL sp_add_column_if_missing('fee_expense_bill', 'evidence_level',
  '`evidence_level` VARCHAR(10) DEFAULT ''weak'' COMMENT ''证据等级 strong/medium/weak(FEAT_EVIDENCE_CHAIN)''');

-- 预留组织字段（专业版多组织数据范围：本组织+下级组织）
CALL sp_add_column_if_missing('budget_preparation', 'org_id',
  '`org_id` BIGINT DEFAULT NULL COMMENT ''组织ID(专业版多组织预留)''');
CALL sp_add_column_if_missing('fee_expense_bill', 'org_id',
  '`org_id` BIGINT DEFAULT NULL COMMENT ''组织ID(专业版多组织预留)''');

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;

-- 报销单项目索引（如不存在则创建）
DROP PROCEDURE IF EXISTS `sp_add_index_if_missing`;
DELIMITER $$
CREATE PROCEDURE `sp_add_index_if_missing`(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_ddl VARCHAR(500))
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO v_cnt FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index;
    IF v_cnt = 0 THEN
        SET @ddl = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` ', p_ddl);
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL sp_add_index_if_missing('fee_expense_bill', 'idx_project_id', '(`project_id`)');
CALL sp_add_index_if_missing('budget_preparation', 'idx_department_id', '(`department_id`)');

DROP PROCEDURE IF EXISTS `sp_add_index_if_missing`;
