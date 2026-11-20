-- =====================================================================
-- M15_03 小微版模块化页面与场景表建表脚本（T15.3，v1.13 新增）
-- 版本：v1.0  日期：2026-09-07
-- 依据：《小微版完整方案》v10.0 第十三章（小程序模块化页面架构）/ 第十六章
--       《资金管控系统开发任务文档》v1.13 T15.3 / T15.16
--       《多业态SaaS平台需求规格说明书》v1.0 第 15 章（小程序模块化页面架构）
-- 前置依赖：M15_01（role_template）、M15_02（industry_template）
-- 规范：
--   1. 先删后建 / 先删后插，脚本可重复执行（幂等）
--   2. page_block_config / share_scene 均含 tenant_id + idx_tenant_id（租户级表）
--   3. page_block_config 默认配置以 tenant_id=0 存「系统默认模板」，新建租户按行业模板克隆；
--      module_type 区分区块来源（micro/enterprise），企业版角色不会误返回小微版区块
--   4. 单一页面壳 + 区块组件库 + 后台配置：不存在 boss/home、salesman/home 多套页面
--   5. /page/config 返回区块清单 = page_block_config ∩ 该角色 perms（配置 ∩ 权限双层过滤）
-- =====================================================================

-- ----------------------------
-- 1. 页面区块配置表 page_block_config（模块化页面架构核心表）
-- ----------------------------
DROP TABLE IF EXISTS `page_block_config`;
CREATE TABLE `page_block_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID(0=系统默认模板，新建租户按行业克隆)',
  `role_code` VARCHAR(64) NOT NULL COMMENT '标准角色编码(boss/salesman/customer)',
  `page_key` VARCHAR(50) NOT NULL COMMENT '页面标识(home/mine/booking/report)',
  `block_key` VARCHAR(64) NOT NULL COMMENT '区块 key(指向组件库：revenue_week/quick_booking...)',
  `module_type` VARCHAR(20) NOT NULL DEFAULT 'micro' COMMENT '区块来源：micro(小微版)/enterprise(企业版)',
  `sort_order` INT DEFAULT 0 COMMENT '区块排序',
  `show_flag` TINYINT DEFAULT 1 COMMENT '是否显示(0隐藏 1显示)',
  `block_params` JSON COMMENT '区块自定义参数(如 {period:week}、{role:salesman})',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_role_page` (`tenant_id`, `role_code`, `page_key`),
  UNIQUE KEY `uk_tenant_role_page_block` (`tenant_id`, `role_code`, `page_key`, `block_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面区块配置表(模块化页面;tenant_id=0为系统默认模板)';

-- ----------------------------
-- 2. 小程序场景/分享表 share_scene（扫码、分享链接带租户号）
-- ----------------------------
DROP TABLE IF EXISTS `share_scene`;
CREATE TABLE `share_scene` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `scene_id` VARCHAR(64) NOT NULL COMMENT '场景标识(小程序 scene 参数，扫码/分享进入锁定租户)',
  `biz_type` VARCHAR(30) DEFAULT NULL COMMENT '业务类型(shop店铺/activity活动/booking预约)',
  `biz_id` BIGINT DEFAULT NULL COMMENT '业务ID(活动ID/服务ID等)',
  `qr_code_url` VARCHAR(500) DEFAULT NULL COMMENT '二维码/海报图URL(OSS)',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间(NULL=永久)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_scene` (`scene_id`),
  KEY `idx_tenant_biz` (`tenant_id`, `biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序场景/分享表(扫码带租户号)';

-- =====================================================================
-- 3. 桌游店默认页面配置（tenant_id=0 系统默认模板，先删后插幂等）
--    与《小微版完整方案》13.3「默认配置示例（桌游店·首页 home）」一致
-- =====================================================================
DELETE FROM `page_block_config` WHERE `tenant_id` = 0 AND `page_key` = 'home' AND `module_type` = 'micro';
INSERT INTO `page_block_config` (`tenant_id`, `role_code`, `page_key`, `block_key`, `module_type`, `sort_order`, `show_flag`, `block_params`, `create_by`) VALUES
-- boss 首页：收入概览 + 待结算 + 提醒 + 店铺信息
(0, 'boss',     'home', 'revenue_week',     'micro', 1, 1, JSON_OBJECT('period', 'week'), 'admin'),
(0, 'boss',     'home', 'revenue_pending',  'micro', 2, 1, NULL, 'admin'),
(0, 'boss',     'home', 'notice_bar',       'micro', 3, 1, NULL, 'admin'),
(0, 'boss',     'home', 'shop_info',        'micro', 9, 1, NULL, 'admin'),
-- salesman 首页：已结算 + 待结算 + 排班 + 打卡
(0, 'salesman', 'home', 'revenue_received', 'micro', 1, 1, NULL, 'admin'),
(0, 'salesman', 'home', 'revenue_pending',  'micro', 2, 1, NULL, 'admin'),
(0, 'salesman', 'home', 'my_schedule',      'micro', 3, 1, NULL, 'admin'),
(0, 'salesman', 'home', 'checkin_entry',    'micro', 4, 1, NULL, 'admin'),
-- customer 首页：助教照片墙 + 活动 + 快速预约 + 店铺信息
(0, 'customer', 'home', 'staff_photo_list', 'micro', 1, 1, JSON_OBJECT('role', 'salesman'), 'admin'),
(0, 'customer', 'home', 'activity_banner',  'micro', 2, 1, NULL, 'admin'),
(0, 'customer', 'home', 'quick_booking',    'micro', 3, 1, NULL, 'admin'),
(0, 'customer', 'home', 'shop_info',        'micro', 9, 1, NULL, 'admin');

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 两表均含 tenant_id + 索引
-- SELECT TABLE_NAME FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'tenant_id'
--    AND TABLE_NAME IN ('page_block_config','share_scene');  -- 预期 2 行
--
-- 4.2 桌游店默认首页配置：boss 4 块 / salesman 4 块 / customer 4 块
-- SELECT role_code, COUNT(*) FROM page_block_config WHERE tenant_id=0 AND page_key='home' GROUP BY role_code;
-- 预期：boss=4、salesman=4、customer=4
--
-- 4.3 module_type 正确区分 micro/enterprise（本脚本仅插 micro）
-- SELECT module_type, COUNT(*) FROM page_block_config GROUP BY module_type;
--
-- 4.4 区块组件库（10 个，前端注册，只增不改）：
--     revenue_week/revenue_received/revenue_pending/staff_photo_list/activity_banner/
--     quick_booking/my_schedule/checkin_entry/shop_info/notice_bar
