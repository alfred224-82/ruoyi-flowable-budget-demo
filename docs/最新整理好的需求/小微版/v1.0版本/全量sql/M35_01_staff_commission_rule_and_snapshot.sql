-- =====================================================================
-- M35_01  T35.1 结算分账机制：员工分成规则 + 活动分成快照 + 活动其他成本
-- =====================================================================
-- 需求概述（《小账本 H5 界面开发任务文档 v1.2》§五 M35 T35.1~T35.3）：
--   1) 新建 staff_commission_rule 表：员工分成规则挂员工档案，三种类型
--      · FIXED_AMOUNT 固定金额（元/场）
--      · PERCENTAGE   按比例（0~100%）
--      · TIERED       阶梯/按人头（结构预留，本期不做）
--      追加消费是否计提成 include_addon_in_commission（默认关）；月底薪 base_salary
--      进固定成本，不进单场。
--   2) 新建 activity_staff_snapshot 表：活动创建/编辑选中员工时按当前规则生成快照；
--      历史账不可变——规则变更后历史活动仍用旧快照；UNIQUE(activity_id, staff_id)。
--   3) promotion_activity 补 other_cost 列：定价页自动算账用（T35.4 保本人数分母）。
--      ★selected_staff_ids/participant_count/custom_price/auto_calculated_price 已存在
--        （M19 T19.2 + M22 T22.6），本次不重复添加。
--   4) 菜单 + 权限键：micro:commissionRule:list/add/edit/remove + micro:commissionRule:report；
--      挂 7051（员工档案）下作为 F 级按钮；报表页作为 C 级菜单挂 7100（老板 PC 首页）。
--   5) role_template boss + platform_admin 追加权限键；存量克隆角色 sys_role_menu 补录。
--
-- 规范：
--   1. 两表均为【租户级表】(含 tenant_id)，多租户插件自动隔离。
--   2. 建表 CREATE TABLE IF NOT EXISTS 幂等；ALTER 用 INFORMATION_SCHEMA 守卫幂等；
--      菜单先删后插幂等；授权 INSERT IGNORE / JSON_CONTAINS 守卫幂等。
--   3. ★menu_id 区段：7064~7068（F 按钮，挂 7051 员工档案）+ 7069（C 级报表页，挂 7100 老板 PC）；
--      已核实 7064~7069 空闲（7063 之后为 M31_04 已用）。
--
-- 前置：
--   [45] M15_06（小微版菜单 7000~7499）
--   [46] M15_07（micro 角色模板三配方）
--   [150] M31_04（隐私分级基座，占用 7059/7063）
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 员工分成规则表 staff_commission_rule
-- =====================================================================
CREATE TABLE IF NOT EXISTS `staff_commission_rule` (
  `id`                            bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`                     bigint        NOT NULL                COMMENT '租户ID',
  `staff_id`                      bigint        NOT NULL                COMMENT '员工用户ID（FK→sys_user.user_id）',
  `commission_type`               varchar(20)   NOT NULL DEFAULT 'FIXED_AMOUNT' COMMENT 'FIXED_AMOUNT=固定金额 / PERCENTAGE=按比例 / TIERED=阶梯(预留)',
  `fixed_amount`                  decimal(10,2) DEFAULT NULL            COMMENT '固定金额（元/场），仅 FIXED_AMOUNT 使用',
  `percentage_rate`               decimal(5,2)  DEFAULT NULL            COMMENT '百分比 0~100，仅 PERCENTAGE 使用',
  `tiered_config`                 json          DEFAULT NULL            COMMENT '阶梯规则 JSON（TIERED 预留）',
  `include_addon_in_commission`   tinyint(1)    NOT NULL DEFAULT 0      COMMENT '追加消费是否计提成：0=否 1=是（默认关）',
  `base_salary`                   decimal(10,2) DEFAULT NULL            COMMENT '月底薪（进固定成本，不进单场）',
  `is_active`                     tinyint(1)    NOT NULL DEFAULT 1      COMMENT '是否启用：0=停用 1=启用',
  `remark`                        varchar(500)  DEFAULT NULL            COMMENT '备注',
  `create_dept`                   bigint        DEFAULT NULL            COMMENT '创建部门',
  `create_by`                     varchar(64)   DEFAULT NULL            COMMENT '创建者',
  `create_time`                   datetime      DEFAULT NULL            COMMENT '创建时间',
  `update_by`                     varchar(64)   DEFAULT NULL            COMMENT '更新者',
  `update_time`                   datetime      DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_staff` (`tenant_id`, `staff_id`),
  KEY `idx_tenant_active` (`tenant_id`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工分成规则（M35 T35.1，§14.2 三种类型）';

-- =====================================================================
-- 2. 活动员工分成快照表 activity_staff_snapshot（历史账不可变）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `activity_staff_snapshot` (
  `id`                 bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`          bigint        NOT NULL                COMMENT '租户ID',
  `activity_id`        bigint        NOT NULL                COMMENT '活动ID（FK→promotion_activity.id）',
  `staff_id`           bigint        NOT NULL                COMMENT '员工用户ID（FK→sys_user.user_id）',
  `rule_type`          varchar(20)   NOT NULL                COMMENT '快照时的规则类型（FIXED_AMOUNT/PERCENTAGE/TIERED）',
  `snapshot_value`     json          NOT NULL                COMMENT '快照时的规则值 JSON（含 fixedAmount/percentageRate/includeAddon 等）',
  `calculated_amount`  decimal(10,2) NOT NULL DEFAULT 0.00   COMMENT '按实际人数/收入计算的应发金额（结算时回写）',
  `create_dept`        bigint        DEFAULT NULL            COMMENT '创建部门',
  `create_by`          varchar(64)   DEFAULT NULL            COMMENT '创建者',
  `create_time`        datetime      DEFAULT NULL            COMMENT '创建时间',
  `update_by`          varchar(64)   DEFAULT NULL            COMMENT '更新者',
  `update_time`        datetime      DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_staff` (`activity_id`, `staff_id`),
  KEY `idx_tenant_activity` (`tenant_id`, `activity_id`),
  KEY `idx_tenant_staff` (`tenant_id`, `staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='活动员工分成快照（M35 T35.1，§14.4 历史不可变）';

-- =====================================================================
-- 3. promotion_activity 补 other_cost 列（T35.4 定价页自动算账用）
--    ★selected_staff_ids/participant_count/custom_price/auto_calculated_price/receivable_amount 已存在
-- =====================================================================
DROP PROCEDURE IF EXISTS m35_01_add_activity_other_cost;
DELIMITER $$
CREATE PROCEDURE m35_01_add_activity_other_cost()
BEGIN
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE()
                    AND TABLE_NAME = 'promotion_activity'
                    AND COLUMN_NAME = 'other_cost') THEN
        ALTER TABLE promotion_activity
          ADD COLUMN other_cost decimal(10,2) DEFAULT NULL
            COMMENT '活动其他成本（场地/物料等，M35 T35.4 定价页算账用；员工成本走 activity_staff_snapshot 快照，不在此列）'
            AFTER participant_count;
    END IF;
END$$
DELIMITER ;
CALL m35_01_add_activity_other_cost();
DROP PROCEDURE IF EXISTS m35_01_add_activity_other_cost;

-- =====================================================================
-- 4. 菜单：分成规则 CRUD 按钮 + 预估vs实际报表页
--    · 7064 分成规则查看   F 挂 7051（员工档案）  perms=micro:commissionRule:list
--    · 7065 分成规则新增   F 挂 7051              perms=micro:commissionRule:add
--    · 7066 分成规则修改   F 挂 7051              perms=micro:commissionRule:edit
--    · 7067 分成规则删除   F 挂 7051              perms=micro:commissionRule:remove
--    · 7068 分成规则导出   F 挂 7051              perms=micro:commissionRule:export
--    · 7069 预估vs实际报表 C 挂 7100（老板 PC 首页）perms=micro:commissionRule:report
--    ★先删后插幂等
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7064, 7065, 7066, 7067, 7068, 7069);
DELETE FROM `sys_menu`      WHERE `menu_id` IN (7064, 7065, 7066, 7067, 7068, 7069);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7064, '分成规则查看',     7051, 10, '',                     NULL,                                       1, 0, 'F', '0', '0', 'micro:commissionRule:list',   'pc', '#',              'admin', NOW(), 'M35 T35.2 员工分成规则列表/详情'),
(7065, '分成规则新增',     7051, 11, '',                     NULL,                                       1, 0, 'F', '0', '0', 'micro:commissionRule:add',    'pc', '#',              'admin', NOW(), 'M35 T35.2 新增分成规则'),
(7066, '分成规则修改',     7051, 12, '',                     NULL,                                       1, 0, 'F', '0', '0', 'micro:commissionRule:edit',   'pc', '#',              'admin', NOW(), 'M35 T35.2 修改分成规则'),
(7067, '分成规则删除',     7051, 13, '',                     NULL,                                       1, 0, 'F', '0', '0', 'micro:commissionRule:remove', 'pc', '#',              'admin', NOW(), 'M35 T35.2 删除分成规则'),
(7068, '分成规则导出',     7051, 14, '',                     NULL,                                       1, 0, 'F', '0', '0', 'micro:commissionRule:export', 'pc', '#',              'admin', NOW(), 'M35 T35.2 导出分成规则'),
(7069, '预估vs实际报表',   7100, 30, 'commissionRule/report', 'micro/commissionRule/report',              1, 0, 'C', '0', '0', 'micro:commissionRule:report', 'pc', 'chart',          'admin', NOW(), 'M35 T35.7 预估vs实际利润报表');

-- =====================================================================
-- 5. role_template：boss + platform_admin 追加 6 个权限键
--    ★JSON_CONTAINS 守卫幂等追加
-- =====================================================================
-- 5.1 boss → 5 CRUD 权限 + 报表权限
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:list')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:list"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:add')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:add"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:edit')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:edit"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:remove')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:remove"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:export')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:export"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:report')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:report"');

-- 5.2 platform_admin → 6 权限键（含报表）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:list')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:list"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:add')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:add"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:edit')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:edit"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:remove')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:remove"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:export')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:export"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:commissionRule:report')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:commissionRule:report"');

-- =====================================================================
-- 6. 存量 sys_role_menu 补录（小微版租户 boss + platform_admin 克隆角色）
-- =====================================================================
-- 6.1 boss 克隆角色 → 7064~7069
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` IN (7064, 7065, 7066, 7067, 7068, 7069);

-- 6.2 platform_admin 克隆角色 → 7064~7069
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` IN (7064, 7065, 7066, 7067, 7068, 7069);

-- 6.3 超管 role_id=1 补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7064, 7065, 7066, 7067, 7068, 7069);

-- =====================================================================
-- 7. 验收查询（执行后人工核对）
-- =====================================================================
-- 7.1 staff_commission_rule 表就绪
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_commission_rule';
-- 预期：含 commission_type / fixed_amount / percentage_rate / tiered_config / include_addon_in_commission / base_salary
--
-- 7.2 activity_staff_snapshot 表就绪
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_staff_snapshot';
-- 预期：含 rule_type / snapshot_value(json) / calculated_amount + UNIQUE(activity_id, staff_id)
--
-- 7.3 promotion_activity.other_cost 列已加
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity' AND COLUMN_NAME = 'other_cost';
-- 预期：1 行 decimal(10,2)
--
-- 7.4 菜单 7064~7069 存在且挂载正确
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--  WHERE menu_id BETWEEN 7064 AND 7069 ORDER BY menu_id;
-- 预期：7064~7068 F parent=7051 / 7069 C parent=7100
--
-- 7.5 role_template boss + platform_admin 均含 6 个权限键
-- SELECT role_code, version_type,
--        JSON_CONTAINS(menu_keys,'"micro:commissionRule:list"')   AS has_list,
--        JSON_CONTAINS(menu_keys,'"micro:commissionRule:report"') AS has_report
--   FROM role_template
--  WHERE role_code IN ('boss','platform_admin') AND status = '0'
--    AND version_type IN ('micro','micro_pro','micro_lite');
-- 预期：全部行 has_list=1 且 has_report=1
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
