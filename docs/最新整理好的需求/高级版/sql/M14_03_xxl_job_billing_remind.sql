-- =====================================================================
-- M14_03 XXL-JOB 任务注册脚本（T14.2 配套：计费到期提醒）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》v1.12 T14.2（到期前 7/3/1 天提醒，走 T13.3 Provider）
--       开发规范第5条（新增任务登记 JobHandler 名与 cron）
-- 前置依赖：
--   M14_01_billing_settlement.sql（计费套餐/订单表）
--   M13_01_access_channel.sql（消息 Provider / biz_message_log）
--   M3_01_xxl_job_tenant_expire_check.sql（到期停用任务，职责分离）
-- 规范：先删后插（按 executor_handler 精确删除），脚本可重复执行
-- 说明：
--   1. job_group=1 为既有默认执行器（ruoyi-job），如有差异请按实际环境调整
--   2. billingRemindJob 每日 09:00 执行：扫描到期前 7/3/1 天的正常租户 → 发送续费提醒
--   3. 【严禁自动扣款】：本任务仅提醒，绝不发起扣款；到期停用由 tenantExpireCheckJob 负责
--   4. 触发状态 trigger_status=1（启用）；上线前请先部署含该 JobHandler 的服务
-- =====================================================================

DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'billingRemindJob';

INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '计费到期提醒（到期前7/3/1天提醒续费，严禁自动扣款）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 9 * * ?', 'DO_NOTHING', 'FIRST',
    'billingRemindJob', '', 'SERIAL_EXECUTION',
    0, 1,
    'BEAN', '', 'GLUE代码初始化', NOW(),
    '', 1, 0, 0
);

-- ----------------------------
-- 验收查询
-- ----------------------------
-- SELECT id, job_desc, schedule_conf, executor_handler, trigger_status
--   FROM xxl_job_info WHERE executor_handler = 'billingRemindJob';
