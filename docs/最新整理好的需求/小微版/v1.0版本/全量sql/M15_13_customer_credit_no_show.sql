-- =====================================================================
-- M15_13 防爽约/黑名单建表脚本（T15.31 + T15.32，v1.14 新增 / 对齐《小微版完整方案》v10.5 §5.6.1/§16/§17.F）
-- 版本：v1.0  日期：2026-09-08
-- 依据：《小微版完整方案》v10.5 §5.6.1（防爽约机制）/ 第十六章（customer_credit / no_show_record 表）
--       《资金管控系统开发任务文档》v1.14 T15.31 / T15.32
-- 前置依赖：M15_05（booking_order）、M15_06（micro 菜单）、M15_08（xxl_job_info）、M15_10（version_type 分档）
-- 规范：
--   1. customer_credit / no_show_record 均为【租户级表】(含 tenant_id)，多租户插件自动隔离；跨租户互不影响
--      （A 店黑名单不影响该用户在 B 店预约）
--   2. booking_order 扩展 expire_time 列（幂等 ALTER）+ order_status 增补 NO_SHOW 态说明 + 扫描索引
--   3. ★仅 micro_pro 生效：爽约判定任务标注 @TenantJob(versions={"micro"}, versionTypes={"micro_pro"})，lite 租户跳过
--   4. ★满 3 次自动拉黑为原子操作（version 乐观锁防并发重复计数）；移出黑名单 no_show_count 保留不清零
--   5. 顾客管理菜单（老板 PC）：micro:customer:list + micro:customer:unblacklist（唯一解除出口）
--   6. noShowCheckJob 注册到 xxl_job_info（job_group=1，@TenantJob 版本+档位过滤）
--   7. 先删后建/先删后插幂等
-- =====================================================================

-- ----------------------------
-- 1. 顾客信誉表 customer_credit（按 tenant_id + user_id 聚合；防爽约核心）
-- ----------------------------
DROP TABLE IF EXISTS `customer_credit`;
CREATE TABLE `customer_credit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '顾客用户ID',
  `no_show_count` INT NOT NULL DEFAULT 0 COMMENT '累计爽约次数(保留历史,不清零)',
  `is_blacklisted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否拉黑：0正常 1已拉黑(满3次自动置1)',
  `blacklisted_at` DATETIME DEFAULT NULL COMMENT '最近一次拉黑时间',
  `unblacklisted_at` DATETIME DEFAULT NULL COMMENT '最近一次被老板移出时间',
  `operator_id` BIGINT DEFAULT NULL COMMENT '移出操作的老板用户ID',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号(防并发重复计数)',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_tenant_blacklist` (`tenant_id`, `is_blacklisted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顾客信誉表(防爽约;累计爽约+黑名单;按tenant_id隔离)';

-- ----------------------------
-- 2. 爽约明细表 no_show_record（每次 no_show 一行，可追溯）
-- ----------------------------
DROP TABLE IF EXISTS `no_show_record`;
CREATE TABLE `no_show_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '顾客用户ID',
  `order_no` VARCHAR(40) DEFAULT NULL COMMENT '爽约订单编号(booking_order.order_no)',
  `book_time` DATETIME DEFAULT NULL COMMENT '原预约到店时间',
  `marked_at` DATETIME DEFAULT NULL COMMENT '标记爽约时间',
  `marked_by` VARCHAR(20) NOT NULL DEFAULT 'system' COMMENT '标记来源：system(定时任务自动)/boss(老板手动)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爽约明细表(每次no_show一行;marked_by=system/boss;可追溯)';

-- ----------------------------
-- 3. booking_order 扩展：expire_time 列 + NO_SHOW 态扫描索引（幂等条件 ALTER）
--    ★order_status 为 VARCHAR，NO_SHOW 态无需 DDL 变更（枚举在 Java BookingOrderStatus 增补）；此处仅补列注释与索引
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_13_extend_booking_order`;
DELIMITER $$
CREATE PROCEDURE `m15_13_extend_booking_order`()
BEGIN
    -- 3.1 expire_time 列（预约过期判定基准：预约时间 + 15 分钟）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking_order' AND COLUMN_NAME = 'expire_time') THEN
        ALTER TABLE `booking_order`
            ADD COLUMN `expire_time` DATETIME DEFAULT NULL
                COMMENT '预约过期时间(booking_time+15分钟)；过期仍PENDING_SERVICE→noShowCheckJob判定爽约(T15.31)' AFTER `booking_time`;
    END IF;
    -- 3.2 order_status 注释增补 NO_SHOW 态
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking_order' AND COLUMN_NAME = 'order_status'
                 AND COLUMN_COMMENT NOT LIKE '%NO_SHOW%') THEN
        ALTER TABLE `booking_order`
            MODIFY COLUMN `order_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_SERVICE'
                COMMENT '订单状态：PENDING_SERVICE(待服务)/COMPLETED(已完成)/CANCELLED(已取消)/NO_SHOW(爽约,T15.31)';
    END IF;
    -- 3.3 爽约扫描索引（noShowCheckJob：tenant_id + PENDING_SERVICE + expire_time 已过）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking_order' AND INDEX_NAME = 'idx_tenant_status_expire') THEN
        ALTER TABLE `booking_order` ADD INDEX `idx_tenant_status_expire` (`tenant_id`, `order_status`, `expire_time`);
    END IF;
END$$
DELIMITER ;
CALL m15_13_extend_booking_order();
DROP PROCEDURE IF EXISTS `m15_13_extend_booking_order`;

-- ----------------------------
-- 4. 顾客管理菜单（老板 PC，挂「小微运营」7100 下；menu_id 7170~7172，先删后插幂等）
--    ★micro:customer:unblacklist = 黑名单唯一解除出口（老板手动移出）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7170 AND 7179;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 7170 AND 7179;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7170, '顾客管理', 7100, 8, 'customerMgr', 'micro/customer/index', NULL, 1, 0, 'C', '0', '0', 'micro:customer:list', 'pc', 'peoples', 'admin', NOW(), '老板PC顾客管理：信誉/爽约次数/黑名单状态 + 爽约明细'),
(7171, '移出黑名单', 7170, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:customer:unblacklist', 'pc', '#', 'admin', NOW(), '★黑名单唯一解除出口：is_blacklisted=0，no_show_count保留不清零'),
(7172, '查看爽约明细', 7170, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:customer:creditDetail', 'pc', '#', 'admin', NOW(), '顾客完整爽约历史(no_show_record)');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 7170 AND 7179;

-- ----------------------------
-- 5. noShowCheckJob 注册（T15.32，每 10 分钟；@TenantJob(versions={"micro"}, versionTypes={"micro_pro"})）
--    ★仅 micro_pro 生效（lite 无预约）；先删后插幂等；executor_param 留空(租户列表由切面自查)
-- ----------------------------
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'noShowCheckJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '小微版防爽约判定（扫描expire_time已过仍待服务的预约→标记no_show+累计爽约，满3次自动拉黑；仅micro_pro）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 */10 * * * ?', 'DO_NOTHING', 'FIRST',
    'noShowCheckJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', '@TenantJob(versions={"micro"},versionTypes={"micro_pro"})', NOW(), '', 1, 0, 0
);

-- ----------------------------
-- 6. 验收查询（执行后人工核对）
-- ----------------------------
-- 6.1 两张防爽约表均含 tenant_id + 唯一/索引
-- SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id'
--   AND TABLE_NAME IN ('customer_credit','no_show_record');  -- 预期 2 行
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='customer_credit' AND INDEX_NAME='uk_tenant_user';  -- 预期 uk_tenant_user
--
-- 6.2 booking_order 扩展 expire_time 列 + 扫描索引 + NO_SHOW 注释
-- SELECT COLUMN_NAME, COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='booking_order' AND COLUMN_NAME IN ('expire_time','order_status');
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='booking_order' AND INDEX_NAME='idx_tenant_status_expire';
--
-- 6.3 顾客管理菜单 + unblacklist 唯一出口
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 7170 AND 7179 ORDER BY menu_id;
-- 预期：7170(micro:customer:list) + 7171(micro:customer:unblacklist) + 7172(micro:customer:creditDetail)
--
-- 6.4 noShowCheckJob 注册且 trigger_status=1、版本+档位过滤
-- SELECT id, job_desc, schedule_conf, executor_handler, glue_remark, trigger_status FROM xxl_job_info
--  WHERE executor_handler='noShowCheckJob';
-- 预期：1 行，glue_remark 含 @TenantJob(versions={"micro"},versionTypes={"micro_pro"})
--
-- 6.5 ★防爽约闭环验证（运行时，T15.32 部署后）：
--   ① 顾客预约(status=PENDING_SERVICE, expire_time=booking_time+15min)
--   ② 过期未到店未取消 → noShowCheckJob 扫描 → status=NO_SHOW + customer_credit.no_show_count+1 + no_show_record(marked_by=system)
--   ③ no_show_count>=3 → is_blacklisted=1(原子,version乐观锁)
--   ④ 黑名单顾客提交预约被拒(接口拦截) + 活动推送名单过滤(is_blacklisted=0)
--   ⑤ 老板 PC「顾客管理」移出(micro:customer:unblacklist) → is_blacklisted=0 + no_show_count保留 + 恢复预约/推送
--   ⑥ ★主动取消(CANCELLED)不计入 no_show_count；★仅 micro_pro 生效，lite 租户跳过；★跨租户隔离
