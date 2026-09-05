-- ----------------------------
-- 工作流审批节点配置表
-- ----------------------------
DROP TABLE IF EXISTS `fee_workflow_node_config`;
CREATE TABLE `fee_workflow_node_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_id` bigint NOT NULL COMMENT '所属工作流ID（关联fee_workflow_config.id）',
  `node_seq` int NOT NULL COMMENT '节点顺序（从1开始）',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称（如：部门经理审批）',
  `role_id` bigint NOT NULL COMMENT '审批角色ID（关联sys_role.role_id）',
  `role_key` varchar(100) DEFAULT NULL COMMENT '角色标识（冗余，如：deptManager）',
  `role_name` varchar(100) DEFAULT NULL COMMENT '角色名称（冗余，如：部门经理）',
  `approval_type` varchar(20) NOT NULL DEFAULT 'ANY' COMMENT '审批方式（ANY单人审批/ALL会签）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_seq` (`workflow_id`, `node_seq`),
  KEY `idx_workflow_id` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流审批节点配置表';
