-- AI报表生成记录表
CREATE TABLE `ai_report_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_type` VARCHAR(50) NOT NULL COMMENT '报表类型(MONTHLY_DETAIL/APPROVAL_SUMMARY/YEARLY_FORECAST/AUDIT_ARCHIVE)',
  `report_name` VARCHAR(200) DEFAULT NULL COMMENT '报表名称',
  `sheet_ids` VARCHAR(500) DEFAULT NULL COMMENT '关联预算单ID(逗号分隔)',
  `report_content` LONGTEXT DEFAULT NULL COMMENT '报表内容(AI生成的Markdown文本)',
  `export_format` VARCHAR(20) DEFAULT 'HTML' COMMENT '导出格式(PDF/EXCEL/WORD/HTML)',
  `file_path` VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径',
  `status` VARCHAR(20) NOT NULL DEFAULT 'GENERATING' COMMENT '生成状态(GENERATING-生成中/COMPLETED-已完成/FAILED-失败)',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `cost_time_ms` BIGINT(20) DEFAULT NULL COMMENT '生成耗时(毫秒)',
  `generate_time` DATETIME DEFAULT NULL COMMENT '生成时间',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_report_type` (`report_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI报表生成记录表';
