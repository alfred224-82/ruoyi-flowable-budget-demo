-- =====================================================================
-- M15_18 boss 推广分销管理菜单拆分（T15.34，v1.30 新增）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档（小微版）》v1.30 T15.34 详细设计
-- 概述：将 boss 角色的「推广分销管理」拆分为业务员推广和顾客推广两个独立子菜单，
--       各自独立配置奖励规则。新增奖励记录按渠道拆分的两个独立页面菜单。
-- 变更内容：
--   1. 在既有 7180~7188 推广分销菜单基础上，新增两个奖励记录子菜单（7189/7190）
--   2. 更新 role_template micro_pro boss 的 menu_keys 追加新 perms
-- 前置依赖：M16_01（推广分销菜单 7180~7188 已建立）、M15_10（分档 role_template）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 新增奖励记录菜单（7189/7190，避让 7180~7188 既有菜单）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7189, 7190);
DELETE FROM `sys_menu` WHERE `menu_id` IN (7189, 7190);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 业务员奖励记录（挂在 7181 业务员推广目录下）
(7189, '业务员奖励记录', 7181, 4, 'salesmanRecords', 'micro/distribution/salesman/records', NULL, 1, 0, 'C', '0', '0', 'micro:distribution:salesman:records', 'pc', 'log', 'admin', NOW(), '渠道=业务员：拉新现金/首单提成/消费返佣逐笔明细，标记已线下发放状态'),
-- 顾客奖励记录（挂在 7185 顾客推广目录下）
(7190, '顾客奖励记录', 7185, 4, 'customerRecords', 'micro/distribution/customer/records', NULL, 1, 0, 'C', '0', '0', 'micro:distribution:customer:records', 'pc', 'log', 'admin', NOW(), '渠道=顾客：拉新优惠券/分享有礼/裂变奖励逐笔明细，含券面额/有效期/核销状态');

-- ----------------------------
-- 2. 授予超管 role_id=1
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 7189), (1, 7190);

-- ----------------------------
-- 3. 更新 micro_pro boss role_template 追加新 perms
-- ----------------------------
-- ★先删后插幂等
DELETE FROM `role_template`
WHERE `version_type` = 'micro_pro'
  AND `role_code` = 'boss';

INSERT INTO `role_template` (`version_type`, `role_code`, `menu_keys`, `block_keys`, `status`, `create_by`, `remark`) VALUES
('micro_pro', 'boss',
 JSON_ARRAY(
   -- 经营核心
   'micro:pro:boss:home', 'micro:pro:report:index', 'micro:pro:report:trend',
   'micro:pro:shop:config',
   -- 预约管理
   'micro:pro:booking:list', 'micro:pro:booking:detail', 'micro:pro:booking:add',
   -- 人员与结算
   'micro:pro:staff:list', 'micro:pro:settlement:index',
   -- 推广分销管理（业务员推广 + 顾客推广，含奖励记录按渠道拆分）
   'micro:distribution:*',
   'micro:distribution:salesman', 'micro:distribution:salesman:records',
   'micro:distribution:customer', 'micro:distribution:customer:records',
   -- 固定支出管理
   'micro:pro:cost:list', 'micro:pro:cost:edit',
   -- 个人中心
   'micro:pro:mine',
   -- lite 档继承
   'micro:lite:activity:publish', 'micro:lite:activity:view', 'micro:lite:boss:home',
   -- 活动浏览
   'micro:customer:activity:browse'
 ),
 JSON_ARRAY(),
 '0', 'admin',
 'micro_pro boss：PC 全经营（含推广分销拆分+固定支出+活动图片主题）');

-- ----------------------------
-- 4. 存量租户 boss 克隆角色补写新菜单（幂等 INSERT IGNORE）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type`
  AND rt.`role_code` = 'boss' AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON m.`perms` = j.`perm`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` IS NOT NULL AND t.`version_type` != ''
  AND m.`menu_id` IN (7189, 7190)
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`);

-- ----------------------------
-- 5. 验收查询
-- ----------------------------
-- A. 新增菜单存在
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id IN (7189, 7190);
-- 预期：2 行
--
-- B. boss role_template 包含新 perms
-- SELECT JSON_CONTAINS(menu_keys, '"micro:distribution:salesman:records"') AS has_sr,
--        JSON_CONTAINS(menu_keys, '"micro:distribution:customer:records"') AS has_cr
--   FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：1/1
