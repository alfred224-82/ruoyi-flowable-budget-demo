-- =====================================================================
-- M33_01  活动锁定申请审批表 activity_lock_request（§九 老板/员工权限分工）
-- =====================================================================
-- 需求来源：《小账本H5界面开发任务文档_v1.md》§九 M33 权限分工
--          （确认下放 + 锁定审批 + 联系方式可见性）
--          《小账本H5界面设计方案_v3》§九 老板/员工权限分工
--
-- 交付范围：
--   表：activity_lock_request（员工「申请锁定」→ 老板「审批锁定申请」的落地表）
--
-- 业务背景（§九 权限分工表）：
--   · 申请锁定：员工提交（无 micro:activity:lock 权限，只能「申请」）；
--   · 审批锁定申请：老板把关（approve → 触发 lockActivity 真正锁定 / reject → 驳回）；
--   · 员工端 pages/micro/staffBook/confirm.vue 全员确认后「申请锁定」→ 写入本表 PENDING；
--   · 老板端 GET /micro/boss/lockRequest/list 读取 PENDING → 一键审批。
--
-- 权限说明：
--   ① approve/reject 复用既有 micro:activity:lock（菜单 7287，[128]M28_01 交付，boss 已授权），
--      无需新增菜单/权限键；
--   ② list 同样以 micro:activity:lock 守卫（只有能锁定的老板才需查看待审批申请）；
--   ③ 员工申请端 /micro/staff/lockApply 仅需登录（@SaCheckLogin），Service 层校验活动归属与状态。
--
-- 幂等：CREATE TABLE IF NOT EXISTS（数据表不 DROP，可重复执行）
-- 前置：[26]M15_11（promotion_activity 表）+ [99]M22_06（is_locked 列）
--       + [128]M28_01（micro:activity:lock 权限 + FEAT_ACTIVITY_LOCK 开关）
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 活动锁定申请表
-- =====================================================================
CREATE TABLE IF NOT EXISTS `activity_lock_request` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id`        BIGINT       NOT NULL                COMMENT '租户ID（门店）',
  `activity_id`      BIGINT       NOT NULL                COMMENT 'FK→promotion_activity.id',
  `activity_title`   VARCHAR(200) DEFAULT NULL            COMMENT '活动标题快照（列表展示，避免联表）',
  `apply_staff_id`   BIGINT       NOT NULL                COMMENT '申请人员工ID（FK→sys_user.user_id）',
  `apply_staff_name` VARCHAR(64)  DEFAULT NULL            COMMENT '申请人姓名快照',
  `reason`           VARCHAR(500) DEFAULT NULL            COMMENT '申请说明（可选）',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待审批 / APPROVED=已通过并锁定 / REJECTED=已驳回',
  `approve_by`       BIGINT       DEFAULT NULL            COMMENT '审批人（老板）用户ID',
  `approve_by_name`  VARCHAR(64)  DEFAULT NULL            COMMENT '审批人姓名快照',
  `approve_time`     DATETIME     DEFAULT NULL            COMMENT '审批时间',
  `reject_reason`    VARCHAR(500) DEFAULT NULL            COMMENT '驳回原因',
  `del_flag`         CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志：0=存在 2=删除',
  `create_by`        VARCHAR(64)  DEFAULT NULL            COMMENT '创建者',
  `create_time`      DATETIME     DEFAULT NULL            COMMENT '创建时间',
  `update_by`        VARCHAR(64)  DEFAULT NULL            COMMENT '更新者',
  `update_time`      DATETIME     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动锁定申请审批表（§九 老板/员工权限分工）';

-- =====================================================================
-- 2. 验收查询（执行后人工核对）
-- =====================================================================
-- 2.1 表已建且含关键字段
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_lock_request'
--    AND COLUMN_NAME IN ('activity_id','apply_staff_id','status','approve_by','reject_reason');
-- 预期：5 行
--
-- 2.2 索引就绪
-- SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_lock_request'
--  GROUP BY INDEX_NAME;
-- 预期：PRIMARY / idx_tenant_status / idx_activity
--
-- ★本脚本仅建数据表，无菜单/权限变更（复用 [128]M28_01 的 micro:activity:lock），
--   执行后重启后端即可，无需重新登录刷新权限快照。
-- =====================================================================
