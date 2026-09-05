-- ----------------------------
-- 工作流配置主表
-- ----------------------------
DROP TABLE IF EXISTS `fee_workflow_config`;
CREATE TABLE `fee_workflow_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_key` varchar(50) NOT NULL COMMENT '工作流标识（budget_preparation/expense_approval/budget_adjustment）',
  `workflow_name` varchar(100) NOT NULL COMMENT '工作流名称',
  `description` varchar(500) DEFAULT NULL COMMENT '流程说明',
  `node_count` int NOT NULL DEFAULT 0 COMMENT '审批节点数量',
  `deploy_id` varchar(64) DEFAULT NULL COMMENT '当前已部署的Flowable部署ID',
  `proc_def_id` varchar(64) DEFAULT NULL COMMENT '当前流程定义ID',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态（DRAFT未部署/DEPLOYED已部署/DISABLED已停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_key` (`workflow_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流配置主表';

-- ----------------------------
-- 初始化数据
-- ----------------------------
INSERT INTO `fee_workflow_config` (`workflow_key`, `workflow_name`, `description`, `node_count`, `status`) VALUES
('budget_preparation', '预算编制工作流', '预算编制提交后的多级审批流程', 3, 'DRAFT'),
('expense_approval', '报销审核工作流', '报销单提交后的审批流程', 2, 'DRAFT'),
('budget_adjustment', '预算调整工作流', '预算调整/超预算申请的审批流程', 3, 'DRAFT');
