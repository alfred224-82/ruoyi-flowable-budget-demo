-- =====================================================================
-- M6_02 XXL-JOB 任务注册脚本（T6.3 creditScoreCalcJob 配套）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控系统设计文档》3.5 高级版增量定时任务
--       《资金管控系统开发任务文档》T6.3 / 开发规范第5条
-- 规范：先删后插（按 executor_handler 精确删除），脚本可重复执行
-- 说明：
--   1. job_group=1 为既有默认执行器（ruoyi-job），如有差异请按实际环境调整
--   2. creditScoreCalcJob 每周一 02:00 执行：重算员工信用等级
--      （仅对开启 FEAT_CREDIT_SCORE 的租户生效，任务内校验开关，基础版跳过）
--   3. 触发状态 trigger_status=1（启用）；上线前请先部署含该 JobHandler 的服务
-- =====================================================================

DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'creditScoreCalcJob';

INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '员工信用分等级重算（每周一，高级版+，执行前校验租户开关）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 2 ? * MON', 'DO_NOTHING', 'FIRST',
    'creditScoreCalcJob', '', 'SERIAL_EXECUTION',
    0, 1,
    'BEAN', '', 'GLUE代码初始化', NOW(),
    '', 1, 0, 0
);

-- ----------------------------
-- 验收查询
-- ----------------------------
-- SELECT id, job_desc, schedule_conf, executor_handler, trigger_status
--   FROM xxl_job_info WHERE executor_handler = 'creditScoreCalcJob';
