-- =====================================================================
-- M21_01  自助预约菜单 + 功能开关 FEAT_SELF_BOOKING（T21.1 / T21.2 / T21.4，v1.67 新增）
-- 版本：v1.0  日期：2026-10-07
-- 依据：《资金管控系统开发任务文档（小微版）》v1.68 M21 里程碑 T21.1~T21.4
-- 前置依赖：M15_06（sys_menu.client_type 列 + 小微版菜单 7000~7499）、
--           M15_07（role_template 标准角色集）、M10_05（feature_flag.micro_enabled 列）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. feature_flag 新增第 38 项 FEAT_SELF_BOOKING（组 E，micro_enabled=1，仅小微版启用；
--      micro_pro 可用 / micro_lite 不可用——由前端菜单可见性 + 后端 Service 层 version_type 双重控制）
--   2. sys_menu 新增自助预约菜单 7200~7202（client_type=mini，挂 7400 小程序·顾客下）
--   3. role_template micro_pro customer menu_keys 追加 3 个权限键
--   4. 存量 customer 角色 sys_role_menu 补录
-- 规范：
--   1. 先删后插幂等（DELETE + INSERT），重复执行不报错
--   2. menu_id 7200~7202 区段避让既有 7100~7199 / 7400~7442 / 7500~7699
--   3. ★自助预约仅 micro_pro 顾客可用；micro_lite 顾客不展示入口（前端按 isPro 条件渲染）
-- =====================================================================

-- ----------------------------
-- 1. 功能开关 FEAT_SELF_BOOKING（feature_flag 第 38 项，组 E=小微版专属）
--    ★micro_enabled=1（小微版默认启用）；basic/advanced/pro 三列均=0（企业版不生效）
--    ★micro_lite vs micro_pro 区分：前端按 isPro 条件渲染入口，后端 Service 层校验 version_type
-- ----------------------------
DELETE FROM `feature_flag` WHERE `code` = 'FEAT_SELF_BOOKING';
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `micro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('FEAT_SELF_BOOKING', '自助预约(项目/房间/助教三类+提前一天限制)', 'E', NULL, 0, 0, 0, 1, '0', 'admin', NOW(),
 'T21.4；★仅小微版 micro_pro 启用(tenant_type=micro/version_type=micro_pro)；micro_lite 不展示入口；basic/advanced/pro 不生效');

-- ----------------------------
-- 2. sys_menu 自助预约菜单（7200~7202，client_type=mini，挂 7400 小程序·顾客）
--    ★先删后插幂等
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (7200, 7201, 7202);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7200: 自助预约（C 类型页面，顾客小程序发现页入口）
(7200, '自助预约', 7400, 50, 'selfBooking', 'micro/customer/selfBooking/index', 1, 0, 'C', '0', '0', 'micro:selfBooking:view', 'mini', 'calendar', 'admin', NOW(), 'T21.4 顾客小程序自助预约页（项目/房间/助教三类入口+可选关联资源+提前一天限制）；★仅 micro_pro 可见'),
-- 7201: 提交预约（F 按钮，perms=micro:selfBooking:submit）
(7201, '提交预约', 7200, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:selfBooking:submit', 'mini', '#', 'admin', NOW(), 'T21.1 提交自助预约'),
-- 7202: 查询可选资源（F 按钮，perms=micro:selfBooking:query）
(7202, '查询可选资源', 7200, 2, '', NULL, 1, 0, 'F', '0', '0', 'micro:selfBooking:query', 'mini', '#', 'admin', NOW(), 'T21.1 查询可选项目/房间/助教');

-- ----------------------------
-- 3. role_template micro_pro customer menu_keys 追加权限
--    ★幂等：JSON_ARRAY_APPEND 仅在不含该键时追加（NOT JSON_CONTAINS 守卫）
-- ----------------------------
-- 3.1 追加 micro:selfBooking:view
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:selfBooking:view')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'customer' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:selfBooking:view"');

-- 3.2 追加 micro:selfBooking:submit
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:selfBooking:submit')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'customer' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:selfBooking:submit"');

-- 3.3 追加 micro:selfBooking:query
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:selfBooking:query')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'customer' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:selfBooking:query"');

-- ----------------------------
-- 4. 存量 customer 角色 sys_role_menu 补录
--    顾客角色 role_id=9207（M15_07 创建），补录 7200~7202 + 7400 祖先目录
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9207, 7200);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9207, 7201);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9207, 7202);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9207, 7400);

-- 其他存量 micro_pro customer 克隆角色补录（非 9207 的 customer 角色）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.`role_key` = 'customer' AND r.`del_flag` = '0' AND r.`role_id` != 9207
  AND m.`menu_id` IN (7200, 7201, 7202, 7400);

-- ----------------------------
-- 5. 验收查询（执行后人工核对）
-- ----------------------------
-- 5.1 FEAT_SELF_BOOKING 落表
-- SELECT code, flag_group, micro_enabled, basic_enabled FROM feature_flag WHERE code = 'FEAT_SELF_BOOKING';
-- 预期：1 行，E / micro_enabled=1 / basic=0
--
-- 5.2 菜单 7200~7202 存在
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu WHERE menu_id IN (7200, 7201, 7202);
-- 预期：3 行，C/F/F，client_type=mini
--
-- 5.3 role_template micro_pro customer 含新权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:selfBooking:view"') AS has_view,
--        JSON_CONTAINS(menu_keys, '"micro:selfBooking:submit"') AS has_submit,
--        JSON_CONTAINS(menu_keys, '"micro:selfBooking:query"') AS has_query
--   FROM role_template WHERE version_type = 'micro_pro' AND role_code = 'customer';
-- 预期：1/1/1
--
-- 5.4 顾客角色 9207 已授权新菜单
-- SELECT COUNT(*) AS menu_count FROM sys_role_menu WHERE role_id = 9207 AND menu_id IN (7200, 7201, 7202);
-- 预期：3
--
-- 5.5 开关总数 +1（原 37 项 → 38 项）
-- SELECT COUNT(*) FROM feature_flag;  -- 预期 38
