-- -----------------------------------------------------------
-- AI引导话术配置表（V5.4 Phase 10 Task 1.13）
-- 用于配置不同场景下的AI引导话术模板
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_ai_prompt_config`;
CREATE TABLE `fee_ai_prompt_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scene_code` varchar(50) NOT NULL COMMENT '场景编码（EXPENSE_BASIC/ASSET/LOAN/FOREIGN）',
  `scene_name` varchar(100) NOT NULL COMMENT '场景名称',
  `prompt_template` text NOT NULL COMMENT 'AI引导话术模板',
  `question_sequence` text DEFAULT NULL COMMENT '提问顺序JSON',
  `industry_code` varchar(50) DEFAULT '*' COMMENT '行业编码',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scene` (`scene_code`, `industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI引导话术配置表';

-- -----------------------------------------------------------
-- 初始化数据：4个默认场景话术
-- -----------------------------------------------------------
INSERT INTO `fee_ai_prompt_config` (`scene_code`, `scene_name`, `prompt_template`, `question_sequence`, `industry_code`, `status`) VALUES
('EXPENSE_BASIC', '基础报销引导', '您好！我来帮您填写报销单。请问您要报销什么类型的费用？我会引导您逐步完成填写。', '[{"step":1,"question":"请选择费用类型（物资小类）","field":"itemCategory"},{"step":2,"question":"请选择用途标签","field":"purposeTag"},{"step":3,"question":"请输入报销金额","field":"amount"},{"step":4,"question":"请选择费用发生日期","field":"expenseDate"},{"step":5,"question":"请补充备注说明（可选）","field":"remark"}]', '*', 'ENABLED'),
('ASSET', '固定资产报销引导', '您好！您正在填写固定资产类报销。我会额外询问使用年限等信息，以便系统自动计算折旧。', '[{"step":1,"question":"请选择资产类别（物资小类）","field":"itemCategory"},{"step":2,"question":"请选择用途标签","field":"purposeTag"},{"step":3,"question":"请输入资产金额","field":"amount"},{"step":4,"question":"请输入预计使用年限（年）","field":"usefulLife"},{"step":5,"question":"请选择购入日期","field":"expenseDate"},{"step":6,"question":"请补充备注说明（可选）","field":"remark"}]', '*', 'ENABLED'),
('LOAN', '借款报销引导', '您好！您正在填写借款类报销单。请提供借款相关信息，系统将自动关联借款台账。', '[{"step":1,"question":"请选择借款类型","field":"itemCategory"},{"step":2,"question":"请选择用途标签","field":"purposeTag"},{"step":3,"question":"请输入借款金额","field":"amount"},{"step":4,"question":"请选择借款日期","field":"expenseDate"},{"step":5,"question":"请输入关联借款编号（可选）","field":"projectCode"},{"step":6,"question":"请补充备注说明（可选）","field":"remark"}]', '*', 'ENABLED'),
('FOREIGN', '外币报销引导', '您好！您正在填写外币报销单。请选择币种，系统将自动按当月汇率折算为人民币。', '[{"step":1,"question":"请选择币种","field":"currencyCode"},{"step":2,"question":"请输入原币金额","field":"originalAmount"},{"step":3,"question":"请选择费用类型（物资小类）","field":"itemCategory"},{"step":4,"question":"请选择用途标签","field":"purposeTag"},{"step":5,"question":"请选择费用发生日期","field":"expenseDate"},{"step":6,"question":"请补充备注说明（可选）","field":"remark"}]', '*', 'ENABLED');
