-- =====================================================================
-- M15_11 lite 档活动浏览页脚本（T15.28，v1.14 新增 / 对齐《小微版完整方案》v10.5 §18.2）
-- 版本：v1.0  日期：2026-09-08
-- 依据：《小微版完整方案》v10.5 第十六章（promotion_activity 表）/ §18.2（lite vs pro 逐菜单对照）
--       《资金管控系统开发任务文档》v1.14 T15.28
-- 前置依赖：M15_05（业务表规范）、M15_06（micro 菜单 7000~7499 + client_type）
-- 规范：
--   1. promotion_activity 为【租户级表】(含 tenant_id)，多租户插件自动隔离，不入 IGNORE_TENANT_TABLES
--   2. ★老板 PC「推广发布」(micro:promo:*) 创建/发布活动；lite 档顾客/助教「活动浏览页」(micro:activity:browse)
--      按 tenant_id 只读本店 status=PUBLISHED 的活动（★无需登录可浏览，走 security.excludes 白名单）
--   3. ★lite 档浏览页无任何预约/支付/打卡按钮（纯只读数据源；pro 档在同源数据上扩展"预约"动作）
--   4. 活动浏览页菜单 client_type=mini，分别挂到业务员(7300)/顾客(7400)小程序目录下，共用 perms micro:activity:browse
--   5. 先删后建/先删后插幂等
-- =====================================================================

-- ----------------------------
-- 1. 活动/预告/海报表 promotion_activity（推广内容；lite 档活动浏览页只读数据源）
-- ----------------------------
DROP TABLE IF EXISTS `promotion_activity`;
CREATE TABLE `promotion_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `title` VARCHAR(120) NOT NULL COMMENT '活动标题',
  `activity_type` VARCHAR(20) NOT NULL DEFAULT 'activity' COMMENT '类型：activity(活动)/notice(预告)/poster(海报)',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `poster_url` VARCHAR(500) DEFAULT NULL COMMENT '分享海报URL(带租户号二维码)',
  `content` TEXT COMMENT '活动正文/详情',
  `start_time` DATETIME DEFAULT NULL COMMENT '活动开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '活动结束时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT(草稿)/PUBLISHED(已发布)/ENDED(已结束)',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_type` (`tenant_id`, `activity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动/预告/海报表(推广内容;lite档活动浏览页只读数据源)';

-- ----------------------------
-- 2. 活动浏览页菜单（micro:activity:browse，client_type=mini，先删后插幂等）
--    ★业务员小程序(7330，挂 7300 下) + 顾客小程序(7430，挂 7400 下) 各一个只读浏览页，共用同一 perms
--    ★lite 档下这是 salesman/customer 的唯一菜单（role_template micro_lite 配方仅绑 micro:activity:browse）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7330, 7430);
DELETE FROM `sys_menu` WHERE `menu_id` IN (7330, 7430);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7330, '活动浏览页', 7300, 4, 'activity', 'miniStaff/activity', NULL, 1, 0, 'C', '0', '0', 'micro:activity:browse', 'mini', '#', 'admin', NOW(), 'lite档业务员唯一菜单：只读本店海报/活动预告/店铺介绍，★无预约/打卡/收入入口'),
(7430, '活动浏览页', 7400, 4, 'activity', 'miniCustomer/activity', NULL, 1, 0, 'C', '0', '0', 'micro:activity:browse', 'mini', '#', 'admin', NOW(), 'lite档顾客唯一菜单：只读本店海报/活动预告/店铺介绍，★无需登录、无预约/支付入口');

-- 活动浏览页菜单授予超管 role_id=1（便于 admin 管理；业务角色授权由 role_template 驱动，见 M15_10）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id IN (7330, 7430);

-- ----------------------------
-- 3. 安全白名单：活动浏览页匿名可访问（T15.28「无需登录可浏览」）
--    ★说明：/micro/activity/browse 走 tenantId 请求参数 + TenantHelper.supply 显式限定租户，
--    仅返回 status=PUBLISHED 的公开活动；不写 TenantContext、不签发会话（与 /portal/** 引导态同理）。
--    白名单在 ruoyi-admin application.yml security.excludes 增加一行：- /micro/activity/browse
--    （配置变更不入本 SQL，此处仅登记约定，供部署核对）
-- ----------------------------

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 promotion_activity 为租户级表（含 tenant_id）
-- SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_activity' AND COLUMN_NAME='tenant_id';  -- 预期 1 行
--
-- 4.2 活动浏览页菜单存在且共用 perms、client_type=mini
-- SELECT menu_id, menu_name, parent_id, perms, client_type FROM sys_menu WHERE menu_id IN (7330,7430);
-- 预期：2 行，perms 均 micro:activity:browse，client_type 均 mini
--
-- 4.3 ★lite 档浏览页只读（运行时）：
--   老板发布活动(status=PUBLISHED) → 顾客/助教调 /micro/activity/browse?tenantId=X →
--   仅返回本店已发布活动，返回体无任何 booking/pay 动作字段
--
-- 4.4 ★跨租户隔离（运行时）：
--   A 店活动不出现在 B 店浏览页（tenant_id 过滤生效）
