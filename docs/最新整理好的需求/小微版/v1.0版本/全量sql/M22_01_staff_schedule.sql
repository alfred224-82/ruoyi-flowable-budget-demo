-- =====================================================================
-- M22_01  员工排班管理（T22.1 / T22.6，v1.72 新增）
-- 版本：v1.0  日期：2026-10-10
-- 依据：《资金管控系统开发任务文档（小微版）》v1.72 M22 里程碑 T22.1~T22.6
-- 前置依赖：M15_01（role_template）、M15_06（sys_menu 7000~7499）、
--           M15_07/M15_10（micro/micro_lite/micro_pro 角色模板）、
--           M1_05（feature_flag / biz_bundle）、M21_01（FEAT_SELF_BOOKING 参考模式）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. 新建 staff_schedule 表（员工排班，租户级，按天×班次×员工一条记录）
--   2. 新建 schedule_change_log 表（发布后修正留痕，租户级）
--   3. feature_flag 新增第 39 项 FEAT_STAFF_SCHEDULE（组 E=小微版专属，micro_enabled=1）
--   4. biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_STAFF_SCHEDULE
--   5. sys_menu 新增 7230~7235（员工排班 C + 3 F 按钮 + 员工/顾客小程序 F 按钮）
--      ★menu_id 避让 7210~7213（M15_06 已分配给老板小程序「定价管理(快改)」）
--   6. role_template：boss(micro/micro_pro) 追加 4 权限键；
--      salesman(micro/micro_pro) 追加 micro:staffSchedule:my；
--      customer(micro/micro_pro) 追加 micro:staffSchedule:store；
--      ★micro_lite 三配方均不追加（免费版不启用排班功能，前端 isPro 条件渲染）
--   7. 存量 sys_role_menu 补录（小微版 boss/salesman/customer 克隆角色）
-- 规范：
--   1. 建表使用 CREATE TABLE IF NOT EXISTS 语义（先删后建仅限本脚本自身创建的表）
--   2. ALTER/UPDATE 使用 INFORMATION_SCHEMA 条件判断 + SQL_SAFE_UPDATES 包裹
--   3. 先删后插幂等（菜单/角色关联），JSON_CONTAINS 守卫（模板追加）
--   4. ★班次分早班 morning / 晚班 evening；起止时间由 Boss 自定（禁止硬编码）
--   5. ★状态 draft → published，发布后仍可编辑，每次修正写 schedule_change_log
--   6. ★员工/顾客仅可见 published 记录；Boss 可见 draft + published
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 新建 staff_schedule 表（员工排班，租户级）
--    UNIQUE(tenant_id, staff_id, schedule_date, shift_type) 防同一员工同一天同一班次重复排班
-- ----------------------------
DROP TABLE IF EXISTS `staff_schedule`;
CREATE TABLE `staff_schedule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `staff_id` BIGINT NOT NULL COMMENT '员工 user_id（FK→sys_user.user_id，salesman 角色）',
  `schedule_date` DATE NOT NULL COMMENT '排班日期',
  `shift_type` VARCHAR(10) NOT NULL COMMENT '班次类型：morning=早班 / evening=晚班',
  `start_time` TIME NOT NULL COMMENT '★班次开始时间（HH:mm:ss，Boss 自定，禁止硬编码）',
  `end_time` TIME NOT NULL COMMENT '★班次结束时间（HH:mm:ss，Boss 自定，禁止硬编码）',
  `status` VARCHAR(10) NOT NULL DEFAULT 'draft' COMMENT '状态：draft=草稿 / published=已发布（发布后仍可修正）',
  `period_type` VARCHAR(10) NOT NULL COMMENT '周期类型：week=一周 / month=一月',
  `period_key` VARCHAR(20) NOT NULL COMMENT '周期键：week=2026-W41（ISO 周）/ month=2026-10',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_staff_date_shift` (`tenant_id`, `staff_id`, `schedule_date`, `shift_type`),
  INDEX `idx_tenant_period` (`tenant_id`, `period_type`, `period_key`),
  INDEX `idx_tenant_date_status` (`tenant_id`, `schedule_date`, `status`),
  INDEX `idx_staff_date` (`staff_id`, `schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工排班表（T22.1 按天×班次×员工一条记录；Boss 按周/月批量编排；草稿→发布→修正留痕）';

-- ----------------------------
-- 2. 新建 schedule_change_log 表（发布后修正留痕，租户级）
-- ----------------------------
DROP TABLE IF EXISTS `schedule_change_log`;
CREATE TABLE `schedule_change_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `schedule_id` BIGINT NOT NULL COMMENT '排班记录ID（FK→staff_schedule.id）',
  `staff_id` BIGINT NOT NULL COMMENT '员工 user_id（冗余快照）',
  `schedule_date` DATE NOT NULL COMMENT '排班日期（冗余快照）',
  `shift_type` VARCHAR(10) NOT NULL COMMENT '班次类型（冗余快照）',
  `change_type` VARCHAR(20) NOT NULL COMMENT '变更类型：TIME=起止时间 / SHIFT=班次 / STATUS=状态发布 / DELETE=删除',
  `before_time` VARCHAR(50) DEFAULT NULL COMMENT '变更前时间（如 09:00-18:00）',
  `after_time` VARCHAR(50) DEFAULT NULL COMMENT '变更后时间（如 10:00-19:00）',
  `before_status` VARCHAR(20) DEFAULT NULL COMMENT '变更前状态',
  `after_status` VARCHAR(20) DEFAULT NULL COMMENT '变更后状态',
  `change_reason` VARCHAR(255) DEFAULT NULL COMMENT '变更原因（Boss 修正时填写）',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人 user_id',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人昵称（冗余快照）',
  `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_schedule` (`tenant_id`, `schedule_id`),
  INDEX `idx_tenant_staff_date` (`tenant_id`, `staff_id`, `schedule_date`),
  INDEX `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班修正日志（T22.1 发布后每次修正留痕；员工/顾客端展示最新版本）';

-- ----------------------------
-- 3. feature_flag 新增第 39 项 FEAT_STAFF_SCHEDULE（组 E=小微版专属）
--    ★micro_enabled=1；basic/advanced/pro 三列均 0（企业版不生效）
--    ★micro_lite vs micro_pro 区分：role_template 权限 + 前端 isPro 条件渲染
-- ----------------------------
DELETE FROM `feature_flag` WHERE `code` = 'FEAT_STAFF_SCHEDULE';
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `micro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('FEAT_STAFF_SCHEDULE', '员工排班管理(周/月+早晚班+Boss 自定时间+发布后可修正)', 'E', NULL, 0, 0, 0, 1, '0', 'admin', NOW(),
 'T22.6；★仅小微版启用；micro_lite 不授 boss 排班权限（前端 isPro 条件渲染入口）；micro_pro 全启用；与 FEAT_SELF_BOOKING 联动（T22.5 助教数据源）');

-- ----------------------------
-- 4. biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_STAFF_SCHEDULE
--    ★与 M21_07 同模式：不限定 biz_code，NOT JSON_CONTAINS 保证幂等
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;

UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_STAFF_SCHEDULE')
WHERE `feature_flags` IS NOT NULL
  AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_STAFF_SCHEDULE"');

SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 5. sys_menu 员工排班菜单（7230~7235）
--    ★menu_id 避让 7210~7213（M15_06 老板小程序「定价管理(快改)」已占用）
--    ★7230~7233 挂 7100 小微运营（Boss PC）
--    ★7234 挂 7300 小程序·业务员（员工 MP，F 按钮，不在侧边栏渲染）
--    ★7235 挂 7400 小程序·顾客（顾客 MP，F 按钮，不在侧边栏渲染）
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (7230, 7231, 7232, 7233, 7234, 7235);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7230: 员工排班（C 类型页面，Boss PC，挂 7100 小微运营）
(7230, '员工排班', 7100, 73, 'staffSchedule', 'micro/staffSchedule/index', 1, 0, 'C', '0', '0', 'micro:staffSchedule:view', 'pc', 'date', 'admin', NOW(), 'T22.3 Boss PC 员工排班（周/月视图 + 早晚班 + 发布/修正）'),
-- 7231: 批量创建草稿（F 按钮）
(7231, '批量创建排班', 7230, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:staffSchedule:create', 'pc', '#', 'admin', NOW(), 'T22.2 按周/月批量创建草稿排班'),
-- 7232: 发布排班（F 按钮）
(7232, '发布排班', 7230, 2, '', NULL, 1, 0, 'F', '0', '0', 'micro:staffSchedule:publish', 'pc', '#', 'admin', NOW(), 'T22.2 草稿批量发布（draft→published）'),
-- 7233: 修正排班（F 按钮）
(7233, '修正排班', 7230, 3, '', NULL, 1, 0, 'F', '0', '0', 'micro:staffSchedule:edit', 'pc', '#', 'admin', NOW(), 'T22.2 发布后修正起止时间/班次（写 schedule_change_log）'),
-- 7234: 员工查看我的排班（F 按钮，MP，挂 7300 小程序·业务员）
(7234, '查看我的排班', 7300, 4, '', NULL, 1, 0, 'F', '0', '0', 'micro:staffSchedule:my', 'mini', '#', 'admin', NOW(), 'T22.4 员工小程序工作台「我的排班」区块（仅本人 published）'),
-- 7235: 顾客查看门店排班（F 按钮，MP，挂 7400 小程序·顾客）
(7235, '查看门店排班', 7400, 4, '', NULL, 1, 0, 'F', '0', '0', 'micro:staffSchedule:store', 'mini', '#', 'admin', NOW(), 'T22.5 顾客小程序发现页「门店最新排班」+ 自助预约助教数据源');

-- ----------------------------
-- 6. role_template 追加权限（micro/micro_pro 两档；★micro_lite 不追加）
--    ★幂等：NOT JSON_CONTAINS 守卫
-- ----------------------------
-- 6.1 micro_pro boss 追加 4 权限键（view/create/publish/edit）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:view')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:create')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:create"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:publish')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:publish"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:edit')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:edit"');

-- 6.2 micro_pro salesman 追加 micro:staffSchedule:my
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:my')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'salesman' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:my"');

-- 6.3 micro_pro customer 追加 micro:staffSchedule:store
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:store')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'customer' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:store"');

-- 6.4 micro (旧档，兼容存量) boss 追加 4 权限键
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:view')
WHERE `version_type` = 'micro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:create')
WHERE `version_type` = 'micro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:create"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:publish')
WHERE `version_type` = 'micro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:publish"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:edit')
WHERE `version_type` = 'micro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:edit"');

-- 6.5 micro (旧档) salesman 追加 my
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:my')
WHERE `version_type` = 'micro' AND `role_code` = 'salesman' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:my"');

-- 6.6 micro (旧档) customer 追加 store
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffSchedule:store')
WHERE `version_type` = 'micro' AND `role_code` = 'customer' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffSchedule:store"');

-- ----------------------------
-- 7. 存量 sys_role_menu 补录（小微版 boss/salesman/customer 克隆角色）
--    ★boss：7230~7233 + 7100 祖先目录
--    ★salesman：7234 + 7300 祖先目录
--    ★customer：7235 + 7400 祖先目录
-- ----------------------------
-- 7.1 boss（小微版 micro_pro / micro，★不含 micro_lite 免费版）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND t.`version_type` IN ('micro_pro', 'micro')
  AND m.`menu_id` IN (7230, 7231, 7232, 7233, 7100);

-- 7.2 salesman（小微版 micro_pro / micro）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'salesman' AND r.`del_flag` = '0'
  AND t.`version_type` IN ('micro_pro', 'micro')
  AND m.`menu_id` IN (7234, 7300);

-- 7.3 customer（小微版 micro_pro / micro）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'customer' AND r.`del_flag` = '0'
  AND t.`version_type` IN ('micro_pro', 'micro')
  AND m.`menu_id` IN (7235, 7400);

-- 7.4 超管 role_id=1 全量补录（便于 admin 平台侧调试）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7230, 7231, 7232, 7233, 7234, 7235);

-- ----------------------------
-- 8. 验收查询（执行后人工核对）
-- ----------------------------
-- 8.1 staff_schedule / schedule_change_log 两表已创建
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('staff_schedule','schedule_change_log');
-- 预期：2 行
--
-- 8.2 FEAT_STAFF_SCHEDULE 落表且 micro_enabled=1
-- SELECT code, flag_group, micro_enabled, basic_enabled, status FROM feature_flag WHERE code = 'FEAT_STAFF_SCHEDULE';
-- 预期：1 行，E / micro_enabled=1 / basic_enabled=0 / status=0
--
-- 8.3 biz_bundle 全部业态包均含 FEAT_STAFF_SCHEDULE
-- SELECT biz_code, JSON_CONTAINS(feature_flags,'"FEAT_STAFF_SCHEDULE"') AS has_flag FROM biz_bundle WHERE feature_flags IS NOT NULL;
-- 预期：所有行 has_flag=1
--
-- 8.4 菜单 7230~7235 存在
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu WHERE menu_id IN (7230,7231,7232,7233,7234,7235);
-- 预期：6 行（C/F/F/F/F/F），7230~7233 client_type=pc，7234~7235 client_type=mini
--
-- 8.5 role_template micro_pro boss 含 4 新权限
-- SELECT JSON_CONTAINS(menu_keys,'"micro:staffSchedule:view"')    AS has_view,
--        JSON_CONTAINS(menu_keys,'"micro:staffSchedule:create"')  AS has_create,
--        JSON_CONTAINS(menu_keys,'"micro:staffSchedule:publish"') AS has_publish,
--        JSON_CONTAINS(menu_keys,'"micro:staffSchedule:edit"')    AS has_edit
--   FROM role_template WHERE version_type = 'micro_pro' AND role_code = 'boss';
-- 预期：1/1/1/1
--
-- 8.6 role_template micro_pro salesman 含 my / customer 含 store
-- SELECT role_code,
--        JSON_CONTAINS(menu_keys,'"micro:staffSchedule:my"')    AS has_my,
--        JSON_CONTAINS(menu_keys,'"micro:staffSchedule:store"') AS has_store
--   FROM role_template WHERE version_type = 'micro_pro' AND role_code IN ('salesman','customer');
-- 预期：salesman 1/0；customer 0/1
--
-- 8.7 ★micro_lite 三配方均不含新权限（免费版不启用）
-- SELECT role_code,
--        JSON_CONTAINS(menu_keys,'"micro:staffSchedule:view"') AS has_view
--   FROM role_template WHERE version_type = 'micro_lite';
-- 预期：全部 0
--
-- 8.8 存量小微版 boss 已授权新菜单（不含 micro_lite）
-- SELECT r.role_id, r.tenant_id, t.version_type, COUNT(DISTINCT rm.menu_id) AS new_menu_count
--   FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_tenant t ON t.id = r.tenant_id
--  WHERE r.role_key = 'boss' AND rm.menu_id IN (7230,7231,7232,7233)
--  GROUP BY r.role_id, r.tenant_id, t.version_type;
-- 预期：每个 micro_pro/micro 版 boss 4 个新菜单；micro_lite 版 boss 0 个
--
-- 8.9 功能开关总数 +1（原 38 项 → 39 项）
-- SELECT COUNT(*) FROM feature_flag;  -- 预期 39
--
-- ★执行后请重启后端，清除内存缓存（BizBundleServiceImpl.flagCache /
--   FeatureFlagServiceImpl.flagsCache / LicenseServiceImpl.tenantFeaturesCache），
--   否则三维交集仍按旧缓存计算
