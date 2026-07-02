-- ----------------------------
-- 预算驳回历史记录表
-- ----------------------------
DROP TABLE IF EXISTS `budget_reject_history`;
CREATE TABLE `budget_reject_history` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` BIGINT(20) NOT NULL COMMENT '预算单ID',
  `sheet_no` VARCHAR(50) DEFAULT NULL COMMENT '预算单号',
  `reject_from_level` VARCHAR(10) NOT NULL COMMENT '驳回方层级(HQ/Branch)',
  `reject_from_user` VARCHAR(50) DEFAULT NULL COMMENT '驳回人工号',
  `reject_from_name` VARCHAR(50) DEFAULT NULL COMMENT '驳回人姓名',
  `reject_to_level` VARCHAR(10) NOT NULL COMMENT '被驳回方层级(Branch/Dept)',
  `reject_to_user` VARCHAR(50) DEFAULT NULL COMMENT '被驳回人工号',
  `reject_to_dept_id` BIGINT(20) DEFAULT NULL COMMENT '被驳回部门ID',
  `reject_to_dept_name` VARCHAR(100) DEFAULT NULL COMMENT '被驳回部门名称',
  `reject_reason` TEXT NOT NULL COMMENT '驳回理由',
  `deadline_time` DATETIME DEFAULT NULL COMMENT '截止时间',
  `reject_time` DATETIME NOT NULL COMMENT '驳回时间',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `handle_duration_hours` DECIMAL(8,2) DEFAULT NULL COMMENT '处理耗时(小时)',
  `is_timeout` TINYINT(1) DEFAULT 0 COMMENT '是否超时(0否1是)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`),
  KEY `idx_reject_time` (`reject_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='预算驳回历史记录表';

-- ----------------------------
-- budget_subject 增加 subject_type 列
-- ----------------------------
ALTER TABLE `budget_subject` ADD COLUMN `subject_type` VARCHAR(20) DEFAULT NULL COMMENT '科目类型（INCOME-收入类，COST-成本类，EXPENSE-费用类，ASSET-资产类，LIABILITY-负债类，EQUITY-权益类）' AFTER `remark`;
ALTER TABLE `budget_subject` ADD KEY `idx_subject_type` (`subject_type`);
