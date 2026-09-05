-- ----------------------------
-- 资金池初始额度配置表
-- ----------------------------
DROP TABLE IF EXISTS `fee_fund_pool_config`;
CREATE TABLE `fee_fund_pool_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `level_type` varchar(20) NOT NULL COMMENT '层级类型（DEPT/BRANCH/HQ）',
  `level_name` varchar(50) NOT NULL COMMENT '层级名称（部门级/分公司级/总公司级）',
  `role_id` bigint DEFAULT NULL COMMENT '关联角色ID（sys_role.role_id）',
  `role_key` varchar(100) DEFAULT NULL COMMENT '角色标识（冗余）',
  `role_name` varchar(100) DEFAULT NULL COMMENT '角色名称（冗余）',
  `initial_amount` decimal(18,2) NOT NULL COMMENT '初始额度',
  `warning_threshold` decimal(5,2) NOT NULL DEFAULT 80.00 COMMENT '预警阈值百分比',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态（ENABLED/DISABLED）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_level_type` (`level_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金池初始额度配置表';

-- ----------------------------
-- 初始化数据
-- ----------------------------
INSERT INTO `fee_fund_pool_config` (`level_type`, `level_name`, `role_key`, `role_name`, `initial_amount`, `warning_threshold`) VALUES
('DEPT', '部门级', 'deptManager', '部门经理', 100000.00, 80.00),
('BRANCH', '分公司级', 'branch', '分公司管理员', 500000.00, 80.00),
('HQ', '总公司级', 'headquarters', '总公司管理员', 1000000.00, 80.00);
