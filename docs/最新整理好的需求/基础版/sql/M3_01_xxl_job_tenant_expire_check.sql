-- =====================================================================
-- M3_01 XXL-JOB 任务注册脚本（T3.4 配套）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控系统开发任务文档》T3.4 / 开发规范第5条（新增任务登记JobHandler名与cron）
-- 规范：先删后插（按 executor_handler 精确删除），脚本可重复执行
-- 说明：
--   1. job_group=1 为既有默认执行器（ruoyi-job），如有差异请按实际环境调整
--   2. tenantExpireCheckJob 每日 00:30 执行：扫描到期租户→状态转EXPIRED→踢会话
--   3. 触发状态 trigger_status=1（启用）；上线前请先部署含该 JobHandler 的服务
-- =====================================================================

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
    0, 1,
    'BEAN', '', 'GLUE代码初始化', NOW(),
    '', 1, 0, 0
);

-- ----------------------------
-- 验收查询
-- ----------------------------
-- SELECT id, job_desc, schedule_conf, executor_handler, trigger_status
--   FROM xxl_job_info WHERE executor_handler = 'tenantExpireCheckJob';
