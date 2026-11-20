-- =====================================================================
-- M15_08 小微版 XXL-JOB 定时任务注册脚本（T15.22，v1.13 新增）
-- 版本：v1.0  日期：2026-09-07
-- 依据：《小微版完整方案》v10.0 第十五章（定时任务与多租户 XXL-JOB）
--       《资金管控系统开发任务文档》v1.13 T15.21 / T15.22
--       《多业态SaaS平台需求规格说明书》v1.0 第 19 章（定时任务与多租户 XXL-JOB）
-- 前置依赖：M3_02_xxl_job_all_tasks.sql（xxl_job_info 表与执行器 job_group=1 已就绪）、
--           M15_05（micro_daily_revenue / booking_order 表）
-- 规范：
--   1. 先删后插（按 executor_handler 精确删除），脚本可重复执行（幂等）；job_group=1（ruoyi-job）
--   2. ★一个 Handler 服务全量租户：executor_param 留空，租户列表由 @TenantJob 切面自己查活跃租户，
--      【调度中心不按租户建多条相同任务】
--   3. ★版本过滤：三个 Handler 均标注 @TenantJob(versions={"micro"})，企业版租户无执行记录
--   4. ★单租户异常 catch 隔离，不抛回 XXL-JOB 导致整任务失败；日志含 tenantId(MDC) 可按租户检索
--   5. Cron/报警统一在 XXL-ADMIN 管理，与若依 sys_job 无关
--   6. ★前置：须先部署 T15.21 TenantJobAspect 切面 + T15.22 三个 Handler Bean，再执行本注册脚本
-- =====================================================================

-- 1. 每日营收快照（每日 00:00 汇总上一自然日，@TenantJob(versions={"micro"})）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'microDailyRevenueJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '小微版每日营收快照（遍历小微租户汇总收入/支出/净利写 micro_daily_revenue）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 0 * * ?', 'DO_NOTHING', 'FIRST',
    'microDailyRevenueJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', '@TenantJob(versions={"micro"})', NOW(), '', 1, 0, 0
);

-- 2. 预约过期自动关单（每 30 分钟，@TenantJob(versions={"micro"})）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'bookingExpireJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '小微版预约过期关单（超时未到店预约自动置为已取消）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 */30 * * * ?', 'DO_NOTHING', 'FIRST',
    'bookingExpireJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', '@TenantJob(versions={"micro"})', NOW(), '', 1, 0, 0
);

-- 3. 顾客流失打标签（每日 01:00，7 天未到店，@TenantJob(versions={"micro"})）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'customerTagJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '小微版顾客流失打标签（7 天未到店顾客自动打「待召回」标签）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 1 * * ?', 'DO_NOTHING', 'FIRST',
    'customerTagJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', '@TenantJob(versions={"micro"})', NOW(), '', 1, 0, 0
);

-- ----------------------------
-- 验收查询
-- ----------------------------
-- 三个小微版任务注册成功（trigger_status=1 启用）
SELECT id, job_desc, schedule_conf, executor_handler, glue_remark, trigger_status
  FROM xxl_job_info
 WHERE executor_handler IN ('microDailyRevenueJob', 'bookingExpireJob', 'customerTagJob')
 ORDER BY schedule_conf;
-- 预期：3 行，glue_remark 均含 @TenantJob(versions={"micro"})
--
-- ★运行时验证（T15.21/T15.22 部署后）：
--   1) 企业版租户无这三个任务的执行记录（versions={"micro"} 过滤生效）
--   2) 单租户失败不影响其他租户（切面 catch 隔离）
--   3) 日志含 tenantId（MDC），可按租户检索定时任务执行记录
--   4) 调度中心每个 Handler 仅一条任务（未按租户建多条）
