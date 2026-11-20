-- =====================================================================
-- M20_13 小微版活动自动结束定时任务注册（T15.28，v1.70 新增）
-- 版本：v1.0  日期：2026-10-02
-- 依据：《资金管控系统开发任务文档》— 活动 endTime 过期后自动置为 ENDED
-- 前置依赖：M15_08_xxl_job_micro_tasks.sql（xxl_job_info 表与执行器 job_group=1 已就绪）、
--           PromotionActivityServiceImpl.autoEndActivities() 方法已交付
-- 规范：
--   1. 先删后插（按 executor_handler 精确删除），脚本可重复执行（幂等）；job_group=1（ruoyi-job）
--   2. ★一个 Handler 服务全量租户：executor_param 留空，租户列表由 @TenantJob 切面自己查活跃租户
--   3. ★版本过滤：@TenantJob(versions={"micro"})，企业版租户无执行记录
--   4. Cron：每 6 小时执行一次（0 0 */6 * * ?），扫描 endTime 已过且 status=PUBLISHED 的活动
--   5. ★仅处理 PUBLISHED 状态：Boss 手动取消（改回 DRAFT）或已结束（ENDED）的活动不受影响
-- =====================================================================

-- 活动自动结束（每 6 小时，@TenantJob(versions={"micro"})）
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'activityEndJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '小微版活动自动结束（endTime 已过的 PUBLISHED 活动批量置为 ENDED）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 0 */6 * * ?', 'DO_NOTHING', 'FIRST',
    'activityEndJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', '@TenantJob(versions={"micro"})', NOW(), '', 1, 0, 0
);

-- ----------------------------
-- 验收查询
-- ----------------------------
-- activityEndJob 注册成功（trigger_status=1 启用）
SELECT id, job_desc, schedule_conf, executor_handler, glue_remark, trigger_status
  FROM xxl_job_info
 WHERE executor_handler = 'activityEndJob';
-- 预期：1 行，glue_remark 含 @TenantJob(versions={"micro"})，trigger_status=1
--
-- ★运行时验证：
--   1) endTime 已过的 PUBLISHED 活动 → 自动置为 ENDED
--   2) Boss 手动取消的活动（DRAFT）→ 不受影响
--   3) Boss 手动结束的活动（ENDED）→ 不重复处理
--   4) 企业版租户无执行记录（versions={"micro"} 过滤生效）
