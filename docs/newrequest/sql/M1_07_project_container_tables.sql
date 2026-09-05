-- =====================================================================
-- M1_07 项目统一容器建表脚本（T1.9，v1.12 新增）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》v1.12 T1.9
--       《资金管控系统设计文档》v1.3 §1.8.5（项目即容器）/ §1.7（项目容器功能域）
--       《多业态SaaS平台需求规格说明书》v1.0 §6（项目即容器：企业任务与活动报名共用 project，业态差异进 extra_config）
-- 前置依赖：M1_02_create_tables_basic.sql（fee_project 已建）
-- 规范：
--   1. 先删后建，脚本可重复执行（幂等）；fee_project 加列走 information_schema 条件判断（幂等）
--   2. 逐表含 tenant_id + 租户索引（全局多租户硬约束，设计文档 1.2）
--   3. project_task / project_signup 外键指向 project（逻辑外键 + 索引，不建物理 FK 约束以兼容分库/软删）
--   4. 与既有 fee_project 关系：fee_project 作为 fund_control 业态下的资金视图，通过 project_id 关联 project，
--      【不合并、不迁移存量】；fee_project.project_id 可空（存量兼容，设计文档冲突化解 #4）
--   5. project_member.role 五枚举与系统角色(sys_role)解耦；存量 fee_project_member VIEWER 映射为 member 只读（T11.2）
-- =====================================================================

-- ----------------------------
-- 1. 项目统一容器表 project（extra_config JSON 承载业态差异字段）
-- ----------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `biz_code` VARCHAR(50) NOT NULL DEFAULT 'fund_control' COMMENT '业态包编码(biz_bundle.biz_code)',
  `type` VARCHAR(30) NOT NULL DEFAULT 'general' COMMENT '容器类型：activity(活动)/team_buy(团购)/general(通用/企业任务)',
  `name` VARCHAR(200) NOT NULL COMMENT '项目/活动名称',
  `status` VARCHAR(30) DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/ONGOING/FINISHED/CLOSED/CANCELLED',
  `owner_user_id` BIGINT DEFAULT NULL COMMENT '负责人(发起老板/店长)用户ID',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `extra_config` JSON COMMENT '业态差异字段(桌游:桌数/时长/主持人；生鲜:产地/保鲜期/配送范围)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_type` (`tenant_id`, `type`),
  KEY `idx_tenant_biz` (`tenant_id`, `biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目统一容器表(项目即容器)';

-- ----------------------------
-- 2. 项目成员表 project_member（role 五枚举，与系统角色解耦）
-- ----------------------------
DROP TABLE IF EXISTS `project_member`;
CREATE TABLE `project_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` BIGINT NOT NULL COMMENT '项目容器ID(FK→project)',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '容器角色：owner/manager/member/participant/signup(与 sys_role 解耦)',
  `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project` (`project_id`),
  UNIQUE KEY `uk_project_user` (`project_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目容器成员表(五枚举角色)';

-- ----------------------------
-- 3. 企业任务表 project_task（type=general 项目使用，FK→project）
-- ----------------------------
DROP TABLE IF EXISTS `project_task`;
CREATE TABLE `project_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` BIGINT NOT NULL COMMENT '项目容器ID(FK→project)',
  `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
  `assignee_user_id` BIGINT DEFAULT NULL COMMENT '指派人用户ID',
  `progress` INT DEFAULT 0 COMMENT '进度百分比(0-100)',
  `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
  `attachment` TEXT COMMENT '附件(JSON数组/URL列表)',
  `status` VARCHAR(30) DEFAULT 'PENDING' COMMENT '状态：PENDING/DOING/DONE/CANCELLED',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_assignee` (`assignee_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目企业任务表(FK→project)';

-- ----------------------------
-- 4. 活动报名表 project_signup（type=activity/team_buy 项目使用，FK→project）
-- ----------------------------
DROP TABLE IF EXISTS `project_signup`;
CREATE TABLE `project_signup` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` BIGINT NOT NULL COMMENT '项目容器ID(FK→project)',
  `user_id` BIGINT DEFAULT NULL COMMENT '报名用户ID(可空=游客报名,记 openid)',
  `openid` VARCHAR(100) DEFAULT NULL COMMENT '报名微信 openid(共用小程序引导态)',
  `signup_no` VARCHAR(40) DEFAULT NULL COMMENT '报名编号',
  `people_count` INT DEFAULT 1 COMMENT '报名人数',
  `quota` INT DEFAULT NULL COMMENT '名额上限(NULL=不限)',
  `is_waitlist` TINYINT DEFAULT 0 COMMENT '是否候补(0否 1是；满员进候补)',
  `sign_in_status` VARCHAR(20) DEFAULT 'NOT_SIGNED' COMMENT '签到状态：NOT_SIGNED/SIGNED/ABSENT',
  `signup_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_project_user` (`project_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目活动报名表(FK→project)';

-- ----------------------------
-- 5. fee_project 增加 project_id 关联列（幂等；可空=存量兼容，不迁移存量）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m1_07_add_fee_project_ref`;
DELIMITER $$
CREATE PROCEDURE `m1_07_add_fee_project_ref`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_project' AND COLUMN_NAME = 'project_id') THEN
        ALTER TABLE `fee_project`
            ADD COLUMN `project_id` BIGINT DEFAULT NULL COMMENT '关联项目容器ID(project.id,资金视图外键;可空=存量兼容)' AFTER `tenant_id`,
            ADD KEY `idx_fee_project_ref` (`project_id`);
    END IF;
END$$
DELIMITER ;
CALL m1_07_add_fee_project_ref();
DROP PROCEDURE IF EXISTS `m1_07_add_fee_project_ref`;

-- ----------------------------
-- 6. 验收查询（执行后人工核对）
-- ----------------------------
-- 6.1 四表均含 tenant_id + 索引
-- SELECT TABLE_NAME FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'tenant_id'
--    AND TABLE_NAME IN ('project','project_member','project_task','project_signup');
-- 预期：4 行
--
-- 6.2 project_task / project_signup 外键列指向 project
-- SHOW CREATE TABLE project_task;  SHOW CREATE TABLE project_signup;
--
-- 6.3 extra_config JSON 可读写
-- SELECT JSON_VALID(extra_config) FROM project;  -- 有数据时预期均为 1
--
-- 6.4 fee_project.project_id 可空（存量兼容）
-- SELECT COLUMN_NAME, IS_NULLABLE FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_project' AND COLUMN_NAME = 'project_id';
-- 预期：IS_NULLABLE = YES
