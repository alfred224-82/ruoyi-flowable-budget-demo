-- =====================================================================
-- M21_08 小微版资源状态自动流转定时任务注册（T21.8，v1.79 新增）
-- 版本：v1.0  日期：2026-10-10
-- 依据：《资金管控系统开发任务文档（小微版）》M21 T21.8 — 补全 in_use 状态落地
-- 前置依赖：M15_08_xxl_job_micro_tasks.sql（xxl_job_info 表与执行器 job_group=1 已就绪）、
--           M21_02_resource_status.sql（service_item.resource_status 列 + resource_status_log 表）、
--           MicroSelfBookingServiceImpl.advanceResourceStatusByTime() 方法已交付、
--           MicroBizJob.resourceInUseFlowJob() Handler Bean 已部署
-- 规范：
--   1. 先删后插（按 executor_handler 精确删除），脚本可重复执行（幂等）；job_group=1（ruoyi-job）
--   2. ★一个 Handler 服务全量租户：executor_param 留空，租户列表由 @TenantJob 切面自己查活跃租户
--   3. ★版本过滤：@TenantJob(versions={"micro"})，企业版租户无执行记录；micro_lite 无自助预约数据自然空跑
--   4. Cron：每 10 分钟执行一次（0 */10 * * * ?），按预约时间窗 + 订单状态推进资源三态流转
--   5. ★幂等：仅处理 reserved→in_use 与 in_use→idle，当前状态==目标状态则跳过，不重复写日志
-- =====================================================================

-- 资源状态自动流转（每 10 分钟，@TenantJob(versions={"micro"})）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'resourceInUseFlowJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '小微版资源状态自动流转（按预约时间窗推进助教/房间 reserved→in_use→idle）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 */10 * * * ?', 'DO_NOTHING', 'FIRST',
    'resourceInUseFlowJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', '@TenantJob(versions={"micro"})', NOW(), '', 1, 0, 0
);

-- ----------------------------
-- 验收查询
-- ----------------------------
-- resourceInUseFlowJob 注册成功（trigger_status=1 启用）
SELECT id, job_desc, schedule_conf, executor_handler, glue_remark, trigger_status
  FROM xxl_job_info
 WHERE executor_handler = 'resourceInUseFlowJob';
-- 预期：1 行，schedule_conf='0 */10 * * * ?'，glue_remark 含 @TenantJob(versions={"micro"})，trigger_status=1
--
-- ★运行时验证（Handler Bean 部署后）：
--   1) 时间窗流转：bookingTimeSlot 覆盖当前时刻的 PENDING_SERVICE 预约 → 关联房间/助教 reserved→in_use + 写 resource_status_log
--   2) 结束回落：订单 COMPLETED/NO_SHOW 或超出时间窗 → 资源 in_use→idle + 写日志
--   3) 幂等性：同一时间窗内连续触发两次，第二次无新增 resource_status_log
--   4) 企业版/micro_lite 租户无执行记录（versions={"micro"} 过滤 + lite 无自助预约数据）
