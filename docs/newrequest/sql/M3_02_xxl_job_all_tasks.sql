-- =====================================================================
-- M3_02 XXL-JOB 全量任务注册脚本
-- 版本：v1.0  日期：2026-08-29
-- 说明：注册全部 11 个定时任务，先删后插（按 executor_handler 精确删除）
--       脚本可重复执行，job_group=1 为默认执行器（ruoyi-job）
-- =====================================================================

-- 1. 租户到期检测（每日 00:30）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'tenantExpireCheckJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '租户到期每日检测（到期租户转EXPIRED并踢出在线会话）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 30 0 * * ?', 'DO_NOTHING', 'FIRST',
    'tenantExpireCheckJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- 2. 补票超时释放（每日 00:40）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'invoiceSupplementOverdueJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '补票超时释放（超时未补票→预占释放+异常标记）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 40 0 * * ?', 'DO_NOTHING', 'FIRST',
    'invoiceSupplementOverdueJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- 3. 合同变更逾期预警（每日 00:50）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'changeOverdueWarnJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '合同变更逾期预警（30/60/90天阶梯升级）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 50 0 * * ?', 'DO_NOTHING', 'FIRST',
    'changeOverdueWarnJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- 4. 老板每日简报（每日 09:00）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'bossDailyBriefingJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '老板每日简报（聚合6类待办，限频每日1条）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 9 * * ?', 'DO_NOTHING', 'FIRST',
    'bossDailyBriefingJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- 5. 信用分重算（每周一 02:00，仅高级版+）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'creditScoreCalcJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '员工信用分等级重算（每周一，高级版+）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 2 ? * MON', 'DO_NOTHING', 'FIRST',
    'creditScoreCalcJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- 6. 编制期开始提醒（每月26日 09:00）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'budgetPeriodStartJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '编制期开始提醒（每月26日提醒各部门编制下月预算）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 9 26 * ?', 'DO_NOTHING', 'FIRST',
    'budgetPeriodStartJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- 7. 预算超时监控（每小时）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'budgetTimeoutMonitorJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '预算超时监控（驳回超时提醒/告警）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 * * * ?', 'DO_NOTHING', 'FIRST',
    'budgetTimeoutMonitorJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- 8. 薪资计提（每月1日 01:00）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'salaryAccrualJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '薪资社保计提（每月1日，计提上一自然月）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 1 1 * ?', 'DO_NOTHING', 'FIRST',
    'salaryAccrualJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- 9. 资产折旧（每月1日 01:10）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'assetDepreciationJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '资产折旧计提（每月1日，含项目分摊）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 10 1 1 * ?', 'DO_NOTHING', 'FIRST',
    'assetDepreciationJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- 10. 贷款利息（每月1日 01:20）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'loanInterestJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '贷款利息计提（每月1日）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 20 1 1 * ?', 'DO_NOTHING', 'FIRST',
    'loanInterestJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- 11. 费用分摊（每月1日 02:00）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'allocationMonthlyJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '月度费用分摊（每月1日）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 2 1 * ?', 'DO_NOTHING', 'FIRST',
    'allocationMonthlyJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', 'GLUE代码初始化', NOW(), '', 1, 0, 0
);

-- ----------------------------
-- 验收查询
-- ----------------------------
SELECT id, job_desc, schedule_conf, executor_handler, trigger_status
  FROM xxl_job_info
 WHERE executor_handler IN (
    'tenantExpireCheckJob', 'invoiceSupplementOverdueJob', 'changeOverdueWarnJob',
    'bossDailyBriefingJob', 'creditScoreCalcJob', 'budgetPeriodStartJob',
    'budgetTimeoutMonitorJob', 'salaryAccrualJob', 'assetDepreciationJob',
    'loanInterestJob', 'allocationMonthlyJob'
 )
 ORDER BY schedule_conf;
