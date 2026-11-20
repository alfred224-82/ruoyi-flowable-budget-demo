-- =====================================================================
-- M28_01  活动锁定/解锁 + 座位编排菜单 + FEAT_ACTIVITY_LOCK 功能开关（T28.1 / T28.4 / T28.5，v1.0）
-- 版本：v1.0  日期：2026-10-13
-- 依据：《小微版 V3.9 开发文档》v1.2 M28 里程碑（人员编排与活动锁定）
--        §三.2 任务清单 T28.1（LOCKED 状态 + 锁定/解锁接口）/ T28.4（操作日志切面）/ T28.5（菜单 + role_template）
--        §六 SQL 落地清单 [93]；§附录 B menu_id 段位（7287~7289 归 M28 活动锁定）+ feature_flag 编号（FEAT_ACTIVITY_LOCK=42）
-- 归属版本：小微版（MICRO，micro_lite + micro_pro）
-- 前置依赖：
--   M15_06（sys_menu.client_type + 小微版菜单 7000~7499；7150 推广发布 M / 7151 活动管理 C）
--   M15_07/M15_10（micro / micro_lite / micro_pro 三档 role_template 配方）
--   M16_01 / M15_11（promotion_activity 主表；status 列 VARCHAR(20)）
--   M22_06（promotion_activity.is_locked 列已补，锁定状态的运行时权威标志）
--   M26_03（activity_seat_assignment / activity_seat_operation_log 两表已建；locked 列已存在）
--   M1_05（feature_flag 表 + biz_bundle 表 + License 三维交集）
--   M21_07 / M26_02（biz_bundle 全量补录参考模式）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. promotion_activity.status 列 COMMENT 扩展 LOCKED 值域说明（★运行时锁定以 is_locked=1 为权威标志，
--      status 保持 PUBLISHED 不变，避免 listPublished（status=PUBLISHED）顾客浏览页丢失已锁定活动）
--   2. feature_flag 新增第 42 项 FEAT_ACTIVITY_LOCK（组 E，micro_enabled=1；micro_lite + micro_pro 均启用，
--      锁定/解锁为通用能力）+ biz_bundle 全部业态包 feature_flags 幂等补录（三维交集根因防御）
--   3. sys_menu 新增 7287（锁定活动 F）/ 7288（解锁活动 F）/ 7289（编排座位 F），parent=7151 活动管理 C
--   4. role_template boss 追加权限键：
--      - micro:activity:lock / micro:activity:unlock：micro / micro_lite / micro_pro 三档（锁定为通用能力）
--      - micro:seat:assign：micro / micro_pro 两档（座位编排依赖 FEAT_ROOM_SEAT_LAYOUT，micro_lite 不授）
--   5. 存量 sys_role_menu 补录：boss（7287/7288 全档；7289 排除 micro_lite）+ 超管 role_id=1
-- ---------------------------------------------------------------------
-- menu_id 段位说明（§附录 B）：
--   7280~7289：M26 房间布局（7280~7283/7285/7286）+ M28 活动锁定（7287/7288/7289）
--   7290~7299：M26 顾客选座（7290 由 M26_03 占用）
-- ---------------------------------------------------------------------
-- 规范：
--   1. 先删后插幂等 + NOT JSON_CONTAINS 守卫，重复执行不报错
--   2. ALTER / UPDATE 使用 INFORMATION_SCHEMA 守卫 + SQL_SAFE_UPDATES 包裹
--   3. Controller @SaCheckPermission keys 与 sys_menu.perms 一一对应（micro:activity:lock / unlock / micro:seat:assign）
--   4. ★执行后请重启后端，清除 BizBundleServiceImpl.flagCache / FeatureFlagServiceImpl.flagsCache /
--      LicenseServiceImpl.tenantFeaturesCache；用户需退出重新登录刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. promotion_activity.status 列 COMMENT 扩展 LOCKED 值域说明（T28.1）
--    ★设计决策：M22_06 已补 is_locked TINYINT 作为锁定状态的运行时权威标志，全部锁定校验
--      （SeatAssignmentServiceImpl.pickSeat/releaseSeat、PromotionActivityServiceImpl.cancelSignup）
--      均读取 is_locked。若运行时把 status 改写为 'LOCKED'，会导致 listPublished（过滤 status=PUBLISHED）
--      的顾客匿名浏览页丢失已锁定活动（锁定活动仍应对客可见，仅座位只读）。
--    ★因此本脚本仅在列 COMMENT 中登记 LOCKED 值域语义（满足 T28.1「值域扩展」的 schema 文档化要求），
--      运行时锁定 = is_locked=1 + activity_seat_assignment.locked=1 全量刷，status 维持 PUBLISHED。
--    ★幂等：仅当当前 COMMENT 不含 'LOCKED' 时才 MODIFY。
-- ----------------------------
SET @cur_status_comment := (
  SELECT COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity' AND COLUMN_NAME = 'status'
);
SET @ddl_status := IF(
  @cur_status_comment IS NOT NULL AND @cur_status_comment NOT LIKE '%LOCKED%',
  'ALTER TABLE `promotion_activity` MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''DRAFT'' COMMENT ''状态：DRAFT(草稿)/PUBLISHED(已发布)/ENDED(已结束)/LOCKED(已锁定,以 is_locked=1 为运行时权威标志)''',
  'SELECT 1'
);
PREPARE stmt_status FROM @ddl_status;
EXECUTE stmt_status;
DEALLOCATE PREPARE stmt_status;

-- ----------------------------
-- 2. feature_flag 新增第 42 项 FEAT_ACTIVITY_LOCK（组 E=小微版专属）
--    ★micro_enabled=1；basic/advanced/pro 三列均 0（企业版不生效）
--    ★锁定/解锁为通用能力：micro_lite + micro_pro 均启用（与 FEAT_ROOM_SEAT_LAYOUT 的 micro_pro 专属不同）
--    ★方法级 @RequireFeature("FEAT_ACTIVITY_LOCK") 标注 MicroActivityBrowseController 的 lock/unlock 端点
-- ----------------------------
DELETE FROM `feature_flag` WHERE `code` = 'FEAT_ACTIVITY_LOCK';
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `micro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('FEAT_ACTIVITY_LOCK', '活动锁定与座位编排(锁定/解锁+加人/移座/互换+操作日志留痕)', 'E', NULL, 0, 0, 0, 1, '0', 'admin', NOW(),
 'T28.1~T28.5；★小微版通用能力(micro_lite + micro_pro 均启用)；锁定后座位只读、编排动作全部留痕 activity_seat_operation_log；M28_01 首建');

-- ----------------------------
-- 2.1 biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_ACTIVITY_LOCK
--     ★三维交集根因防御（License ∩ feature_flag ∩ biz_bundle），参考 M21_07 / M26_02 模式
--     ★不限定 biz_code：覆盖三个标准业态包 + 后续新建的自定义业态包
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;

UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_ACTIVITY_LOCK')
WHERE `feature_flags` IS NOT NULL
  AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_ACTIVITY_LOCK"');

SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 3. sys_menu：活动锁定/解锁/编排座位 7287/7288/7289（Boss F，parent=7151 活动管理 C，client_type=pc）
--    ★先删后插幂等；menu_id 段位 7287~7289 归 M28（避开 M26 7280~7286 与 M26 顾客选座 7290）
--    ★MP Boss 端复用同一 perms 字符串（后端 @SaCheckPermission 校验），F 菜单 client_type 统一登记 pc
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7287, 7288, 7289);
DELETE FROM `sys_menu` WHERE `menu_id` IN (7287, 7288, 7289);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7287: 锁定活动（F 按钮，Boss；T28.1 PUT /micro/activity/{id}/lock）
(7287, '锁定活动', 7151, 10, '', NULL, 1, 0, 'F', '0', '0', 'micro:activity:lock', 'pc', '#', 'admin', NOW(), 'T28.1 锁定活动：is_locked=1 + activity_seat_assignment.locked 全量刷 1；操作日志 LOCK'),
-- 7288: 解锁活动（F 按钮，Boss；T28.1 PUT /micro/activity/{id}/unlock）
(7288, '解锁活动', 7151, 11, '', NULL, 1, 0, 'F', '0', '0', 'micro:activity:unlock', 'pc', '#', 'admin', NOW(), 'T28.1 解锁活动：is_locked=0 + activity_seat_assignment.locked 全量刷 0；前端二次确认弹窗；操作日志 UNLOCK'),
-- 7289: 编排座位（F 按钮，Boss；T28.2 assign/move/swap 三接口共用此权限键）
(7289, '编排座位', 7151, 12, '', NULL, 1, 0, 'F', '0', '0', 'micro:seat:assign', 'pc', '#', 'admin', NOW(), 'T28.2 Boss 座位编排：POST /micro/seat/assign(加人) + PUT /micro/seat/move(移座) + POST /micro/seat/swap(互换)；锁定后拒绝');

-- ----------------------------
-- 4. role_template：boss 追加权限键
--    4.1 micro:activity:lock / micro:activity:unlock → micro_pro / micro_lite 两档（锁定为通用能力）
--    4.2 micro:seat:assign → micro_pro 一档（座位编排依赖 FEAT_ROOM_SEAT_LAYOUT，micro_lite 不授）
--    ★JSON_CONTAINS 守卫幂等追加
-- ----------------------------
-- 4.1.a micro_pro boss：lock / unlock / seat:assign
UPDATE `role_template` SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:activity:lock')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:activity:lock"');
UPDATE `role_template` SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:activity:unlock')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:activity:unlock"');
UPDATE `role_template` SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:seat:assign')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:seat:assign"');

-- 4.1.b micro_lite boss：lock / unlock（★不授 seat:assign，座位布局为 micro_pro 专属）
UPDATE `role_template` SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:activity:lock')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:activity:lock"');
UPDATE `role_template` SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:activity:unlock')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:activity:unlock"');

-- ----------------------------
-- 5. 存量 sys_role_menu 补录
-- ----------------------------
-- 5.1 boss（小微版全档 micro_pro/micro_lite）：7287（锁定 F）+ 7288（解锁 F）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND m.`menu_id` IN (7287, 7288);

-- 5.2 boss（小微版 micro_pro，★排除 micro_lite）：7289（编排座位 F）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND (t.`version_type` IS NULL OR t.`version_type` <> 'micro_lite')
  AND m.`menu_id` = 7289;

-- 5.3 超管 role_id=1 全量补录（便于平台侧联调）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7287, 7288, 7289);

-- ----------------------------
-- 6. 验收查询（执行后人工核对）
-- ----------------------------
-- 6.1 promotion_activity.status COMMENT 已含 LOCKED
-- SELECT COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='promotion_activity' AND COLUMN_NAME='status';
-- 预期：COMMENT 含 'LOCKED'
--
-- 6.2 FEAT_ACTIVITY_LOCK 落表且 micro_enabled=1
-- SELECT code, flag_group, micro_enabled, basic_enabled, status FROM feature_flag WHERE code='FEAT_ACTIVITY_LOCK';
-- 预期：1 行，E / micro_enabled=1 / basic_enabled=0 / status=0
--
-- 6.3 每个业态包均含 FEAT_ACTIVITY_LOCK
-- SELECT biz_code, JSON_CONTAINS(feature_flags,'"FEAT_ACTIVITY_LOCK"') AS f_act_lock FROM biz_bundle;
-- 预期：所有 feature_flags 非 NULL 的业态包 f_act_lock=1
--
-- 6.4 菜单 7287/7288/7289 存在且挂 7151 下
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu
--  WHERE menu_id IN (7287, 7288, 7289) ORDER BY menu_id;
-- 预期：3 行
--   7287 F micro:activity:lock   pc（parent=7151）
--   7288 F micro:activity:unlock pc（parent=7151）
--   7289 F micro:seat:assign     pc（parent=7151）
--
-- 6.5 role_template boss 权限键就绪（micro_pro 含 3 键，micro_lite 仅含 lock/unlock）
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:activity:lock"')   AS has_lock,
--        JSON_CONTAINS(menu_keys,'"micro:activity:unlock"') AS has_unlock,
--        JSON_CONTAINS(menu_keys,'"micro:seat:assign"')     AS has_assign
--   FROM role_template WHERE role_code='boss' AND version_type IN ('micro_pro','micro_lite');
-- 预期：micro_pro 为 1/1/1；micro_lite 为 1/1/0
--
-- 6.6 存量小微版租户 boss 克隆角色已授权 7287/7288（全档）+ 7289（排除 micro_lite）
-- SELECT t.version_type, rm.menu_id, COUNT(DISTINCT r.role_id) AS granted_roles
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id=r.role_id AND r.role_key='boss' AND r.del_flag='0'
--   JOIN sys_tenant t ON r.tenant_id=t.id AND t.tenant_type='micro'
--  WHERE rm.menu_id IN (7287,7288,7289)
--  GROUP BY t.version_type, rm.menu_id;
-- 预期：7287/7288 各档均有；7289 仅 micro_pro 有，micro_lite 无
--
-- 6.7 端到端：boss 重新登录后
--     PUT /micro/activity/{id}/lock、PUT /micro/activity/{id}/unlock、
--     POST /micro/seat/assign、PUT /micro/seat/move、POST /micro/seat/swap 均返回 200（不再 403）
--     每次动作在 activity_seat_operation_log 留一条（LOCK/UNLOCK/ASSIGN/MOVE/SWAP），含 before/after 快照
--
-- ★执行后请重启后端，清除 BizBundleServiceImpl.flagCache / FeatureFlagServiceImpl.flagsCache /
--   LicenseServiceImpl.tenantFeaturesCache；用户需退出重新登录刷新 Sa-Token 权限快照
-- =====================================================================
