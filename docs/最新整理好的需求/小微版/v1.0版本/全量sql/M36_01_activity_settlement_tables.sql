-- =====================================================================
-- M36_01  活动结算单三表（T36.1）+ 行业结算模板表（T36.2）+ 菜单
-- =====================================================================
-- 需求来源：《小账本H5界面开发任务文档_v1.md》§六 M36 活动结算单
--          《小账本H5界面设计方案_v3_含结算分账与推广码.docx》§16 活动结算单
--
-- 交付范围：
--   T36.1 建表：activity_settlement（结算单主表）
--                + settlement_income_item（收入项明细）
--                + settlement_cost_item（成本项明细）
--   T36.2 建表：industry_settlement_template（行业收入项模板）+ 种子数据
--   菜单：7360~7364（PC 端结算管理，boss 权限）
--         7370~7372（PC 端海报管理，boss 权限）
--
-- 关键规则（§16）：
--   ① 代收类型（COLLECTED）不计入收入/利润/分账基数；
--   ② 员工成本由 activity_staff_snapshot 快照自动生成，不可手填；
--   ③ 状态机：DRAFT → SUBMITTED → LOCKED，提交后不可编辑；
--   ④ 退款单独记负向收入项（amount<0），不改原金额；
--   ⑤ 锁定后发现错误 → 追加调整单（adjustment 字段标记）。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + INFORMATION_SCHEMA 守卫 ALTER + 先删后插菜单
-- 前置：[26]M15_11（promotion_activity 表）+ [140]M29_03（staff_commission）
--       + [146]M31_04（隐私分级基座，L2/L3 数据分级参考）
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 活动结算单主表 activity_settlement（T36.1）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `activity_settlement` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`         bigint        NOT NULL                COMMENT '租户ID',
  `activity_id`       bigint        NOT NULL                COMMENT 'FK→promotion_activity.id',
  `settlement_no`     varchar(32)   DEFAULT NULL            COMMENT '结算单号（提交时生成，如 ST202610190001）',
  `status`            varchar(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT=草稿 / SUBMITTED=已提交 / LOCKED=已锁定',
  `is_adjustment`     tinyint(1)    NOT NULL DEFAULT 0      COMMENT '是否调整单（0=原单 1=调整单）',
  `origin_settlement_id` bigint     DEFAULT NULL            COMMENT '调整单原单ID（is_adjustment=1 时非空）',

  -- 收入汇总（后端自动计算，不接受前端直接传入）
  `ticket_revenue`    decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '票面收入合计（type=TICKET 汇总）',
  `addon_revenue`     decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '追加收入合计（type=ADDON 汇总）',
  `collected_amount`  decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '代收金额合计（type=COLLECTED 汇总，不计入利润）',
  `total_revenue`     decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '收入合计=ticket+addon（不含代收）',

  -- 成本汇总
  `employee_cost`     decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '员工成本合计（快照自动生成，不可手填）',
  `other_cost`        decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '其他成本合计（可手填）',
  `total_cost`        decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '成本合计=employee+other',

  -- 利润
  `profit`            decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '本场利润=total_revenue - total_cost',
  `estimated_profit`  decimal(12,2) DEFAULT NULL            COMMENT '预估利润（来自 M35.4 定价页快照）',
  `profit_diff`       decimal(12,2) DEFAULT NULL            COMMENT '利润差额=profit - estimated_profit',

  -- 结算元数据
  `settled_by`        bigint        DEFAULT NULL            COMMENT '结算操作人（sys_user.user_id）',
  `settled_time`      datetime      DEFAULT NULL            COMMENT '结算提交时间',
  `locked_time`       datetime      DEFAULT NULL            COMMENT '结算锁定时间',
  `remark`            varchar(500)  DEFAULT NULL            COMMENT '备注',

  `create_dept`       bigint        DEFAULT NULL            COMMENT '创建部门',
  `create_by`         varchar(64)   DEFAULT NULL            COMMENT '创建者',
  `create_time`       datetime      DEFAULT NULL            COMMENT '创建时间',
  `update_by`         varchar(64)   DEFAULT NULL            COMMENT '更新者',
  `update_time`       datetime      DEFAULT NULL            COMMENT '更新时间',
  `del_flag`          char(1)       DEFAULT '0'             COMMENT '删除标志（0-正常；2-已删除）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_activity` (`tenant_id`, `activity_id`, `is_adjustment`, `del_flag`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_settled_time` (`settled_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='活动结算单主表（§16，租户级）';

-- =====================================================================
-- 2. 结算收入项明细 settlement_income_item（T36.1）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `settlement_income_item` (
  `id`                    bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`             bigint        NOT NULL                COMMENT '租户ID',
  `settlement_id`         bigint        NOT NULL                COMMENT 'FK→activity_settlement.id',
  `item_name`             varchar(100)  NOT NULL                COMMENT '收入项名称（如"成人团费"/"饮料零食"）',
  `item_type`             varchar(20)   NOT NULL                COMMENT '类型：TICKET=票面 / ADDON=追加 / COLLECTED=代收',
  `amount`                decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '金额（退款为负值）',
  `quantity`              int           DEFAULT 1               COMMENT '数量（人数/份数）',
  `unit_price`            decimal(12,2) DEFAULT NULL            COMMENT '单价（可选，用于校验 amount=quantity*unit_price）',
  `include_in_commission` tinyint(1)    NOT NULL DEFAULT 1      COMMENT '是否计入分账基数（代收=0）',
  `include_in_profit`     tinyint(1)    NOT NULL DEFAULT 1      COMMENT '是否计入利润（代收=0）',
  `is_default_ticket`     tinyint(1)    NOT NULL DEFAULT 0      COMMENT '是否默认票面行（不可删）',
  `sort_order`            int           NOT NULL DEFAULT 0      COMMENT '排序',
  `remark`                varchar(200)  DEFAULT NULL            COMMENT '备注（退款原因等）',

  `create_by`             varchar(64)   DEFAULT NULL,
  `create_time`           datetime      DEFAULT NULL,
  `update_by`             varchar(64)   DEFAULT NULL,
  `update_time`           datetime      DEFAULT NULL,
  `del_flag`              char(1)       DEFAULT '0',

  PRIMARY KEY (`id`),
  KEY `idx_settlement` (`settlement_id`),
  KEY `idx_tenant_type` (`tenant_id`, `item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='结算收入项明细（§16.2）';

-- =====================================================================
-- 3. 结算成本项明细 settlement_cost_item（T36.1）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `settlement_cost_item` (
  `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`       bigint        NOT NULL                COMMENT '租户ID',
  `settlement_id`   bigint        NOT NULL                COMMENT 'FK→activity_settlement.id',
  `item_name`       varchar(100)  NOT NULL                COMMENT '成本项名称',
  `cost_type`       varchar(20)   NOT NULL                COMMENT '类型：EMPLOYEE=员工成本（快照自动生成）/ OTHER=其他成本（可手填）',
  `amount`          decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '金额（代付成本为正值，与代收配对）',
  `staff_id`        bigint        DEFAULT NULL            COMMENT 'EMPLOYEE 类型时关联员工（sys_user.user_id）',
  `snapshot_id`     bigint        DEFAULT NULL            COMMENT 'EMPLOYEE 类型时关联 activity_staff_snapshot.id',
  `is_auto_generated` tinyint(1)  NOT NULL DEFAULT 0      COMMENT '是否自动生成（员工成本=1，其他成本=0）',
  `sort_order`      int           NOT NULL DEFAULT 0      COMMENT '排序',
  `remark`          varchar(200)  DEFAULT NULL            COMMENT '备注（代付说明等）',

  `create_by`       varchar(64)   DEFAULT NULL,
  `create_time`     datetime      DEFAULT NULL,
  `update_by`       varchar(64)   DEFAULT NULL,
  `update_time`     datetime      DEFAULT NULL,
  `del_flag`        char(1)       DEFAULT '0',

  PRIMARY KEY (`id`),
  KEY `idx_settlement` (`settlement_id`),
  KEY `idx_tenant_type` (`tenant_id`, `cost_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='结算成本项明细（§16.3）';

-- =====================================================================
-- 4. 行业结算模板表 industry_settlement_template（T36.2）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `industry_settlement_template` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `industry_code`     varchar(32)   NOT NULL                COMMENT '行业编码（对齐 industry_template.industry_code）',
  `industry_name`     varchar(64)   DEFAULT NULL            COMMENT '行业名称（冗余展示）',
  `item_name`         varchar(100)  NOT NULL                COMMENT '收入项名称',
  `item_type`         varchar(20)   NOT NULL                COMMENT 'TICKET/ADDON/COLLECTED',
  `default_quantity`  int           DEFAULT 1               COMMENT '默认数量',
  `include_in_commission` tinyint(1) NOT NULL DEFAULT 1     COMMENT '默认是否计入分账',
  `include_in_profit` tinyint(1)    NOT NULL DEFAULT 1      COMMENT '默认是否计入利润',
  `is_default_ticket` tinyint(1)    NOT NULL DEFAULT 0      COMMENT '是否默认票面行',
  `sort_order`        int           NOT NULL DEFAULT 0      COMMENT '排序',
  `status`            char(1)       DEFAULT '0'             COMMENT '0=启用 1=停用',
  `remark`            varchar(200)  DEFAULT NULL,

  `create_by`         varchar(64)   DEFAULT NULL,
  `create_time`       datetime      DEFAULT NULL,
  `update_by`         varchar(64)   DEFAULT NULL,
  `update_time`       datetime      DEFAULT NULL,
  `del_flag`          char(1)       DEFAULT '0',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_industry_item` (`industry_code`, `item_name`, `del_flag`),
  KEY `idx_industry_status` (`industry_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='行业收入项模板（§16.4，创建活动时按行业自动带出）';

-- =====================================================================
-- 5. 行业结算模板种子数据（T36.2）
-- =====================================================================
-- 5.1 桌游/剧本杀行业（board_game）
DELETE FROM `industry_settlement_template` WHERE `industry_code` = 'board_game' AND `del_flag` = '0';
INSERT INTO `industry_settlement_template`
  (`industry_code`, `industry_name`, `item_name`, `item_type`, `default_quantity`, `include_in_commission`, `include_in_profit`, `is_default_ticket`, `sort_order`, `status`, `remark`, `create_by`, `create_time`)
VALUES
('board_game', '桌游/剧本杀', '场次费',     'TICKET',    1, 1, 1, 1, 1, '0', '默认票面行，不可删', 'admin', NOW()),
('board_game', '桌游/剧本杀', '饮料零食',   'ADDON',     1, 1, 1, 0, 2, '0', '店内追加消费',       'admin', NOW()),
('board_game', '桌游/剧本杀', '道具租赁',   'ADDON',     1, 1, 1, 0, 3, '0', '特殊道具收费',       'admin', NOW());

-- 5.2 旅游/户外行业（travel）
DELETE FROM `industry_settlement_template` WHERE `industry_code` = 'travel' AND `del_flag` = '0';
INSERT INTO `industry_settlement_template`
  (`industry_code`, `industry_name`, `item_name`, `item_type`, `default_quantity`, `include_in_commission`, `include_in_profit`, `is_default_ticket`, `sort_order`, `status`, `remark`, `create_by`, `create_time`)
VALUES
('travel', '旅游/户外', '成人团费',   'TICKET',    1, 1, 1, 1, 1, '0', '默认票面行，不可删',     'admin', NOW()),
('travel', '旅游/户外', '儿童团费',   'TICKET',    1, 1, 1, 0, 2, '0', '儿童价（可选）',         'admin', NOW()),
('travel', '旅游/户外', '单房差',     'ADDON',     1, 1, 1, 0, 3, '0', '单人入住补差',           'admin', NOW()),
('travel', '旅游/户外', '装备租赁',   'ADDON',     1, 1, 1, 0, 4, '0', '户外装备',               'admin', NOW()),
('travel', '旅游/户外', '保险',       'ADDON',     1, 1, 1, 0, 5, '0', '旅游意外险',             'admin', NOW()),
('travel', '旅游/户外', '代收门票',   'COLLECTED', 1, 0, 0, 0, 6, '0', '★代收类型：不计入收入/利润/分账基数', 'admin', NOW());

-- 5.3 按摩/足浴行业（massage）
DELETE FROM `industry_settlement_template` WHERE `industry_code` = 'massage' AND `del_flag` = '0';
INSERT INTO `industry_settlement_template`
  (`industry_code`, `industry_name`, `item_name`, `item_type`, `default_quantity`, `include_in_commission`, `include_in_profit`, `is_default_ticket`, `sort_order`, `status`, `remark`, `create_by`, `create_time`)
VALUES
('massage', '按摩/足浴', '服务费',     'TICKET',    1, 1, 1, 1, 1, '0', '默认票面行，不可删',   'admin', NOW()),
('massage', '按摩/足浴', '精油升级',   'ADDON',     1, 1, 1, 0, 2, '0', '精油档次升级',         'admin', NOW()),
('massage', '按摩/足浴', '茶水点心',   'ADDON',     1, 1, 1, 0, 3, '0', '店内追加消费',         'admin', NOW());

-- =====================================================================
-- 6. 菜单：M36 活动结算单（7360~7364，PC 端 boss 权限）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7360 AND 7364;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7360 AND 7364;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7360, '活动结算单', 7100, 8, 'settlement', 'micro/settlement/index', 1, 0, 'C', '0', '0', 'micro:settlement:list', 'pc', 'money', 'admin', NOW(), 'M36 T36.3 老板端结算单管理（含调整单）'),
(7361, '保存草稿',   7360, 1, '',           NULL,                     1, 0, 'F', '0', '0', 'micro:settlement:save',   'pc', '#', 'admin', NOW(), 'M36 T36.5 状态机 DRAFT'),
(7362, '提交结算',   7360, 2, '',           NULL,                     1, 0, 'F', '0', '0', 'micro:settlement:submit', 'pc', '#', 'admin', NOW(), 'M36 T36.5 状态机 DRAFT→SUBMITTED'),
(7363, '导出CSV',    7360, 3, '',           NULL,                     1, 0, 'F', '0', '0', 'micro:settlement:export', 'pc', '#', 'admin', NOW(), 'M36 T36.3 导出结算单'),
(7364, '追加调整单', 7360, 4, '',           NULL,                     1, 0, 'F', '0', '0', 'micro:settlement:adjust', 'pc', '#', 'admin', NOW(), 'M36 T36.5 锁定后追加调整');

-- =====================================================================
-- 7. 菜单：M37 推广码与海报（7370~7372，PC 端 boss 权限）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7370 AND 7372;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7370 AND 7372;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7370, '推广码与海报', 7150, 3, 'poster',     'micro/poster/index', 1, 0, 'C', '0', '0', 'micro:poster:list',   'pc', 'guide', 'admin', NOW(), 'M37 T37.1/T37.2 海报管理入口'),
(7371, '员工海报',     7370, 1, '',           NULL,                 1, 0, 'F', '0', '0', 'micro:poster:staff',  'pc', '#', 'admin', NOW(), 'M37 T37.1 一人一码海报生成'),
(7372, '门店码',       7370, 2, '',           NULL,                 1, 0, 'F', '0', '0', 'micro:poster:store',  'pc', '#', 'admin', NOW(), 'M37 T37.2 老板门店码');

-- =====================================================================
-- 8. role_template：boss（micro_pro/micro_lite）+ platform_admin 追加权限键
-- =====================================================================
-- 8.1 boss 追加 M36 结算单权限（micro_pro + micro_lite）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:settlement:list')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:settlement:list"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:settlement:save')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:settlement:save"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:settlement:submit')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:settlement:submit"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:settlement:export')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:settlement:export"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:settlement:adjust')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:settlement:adjust"');

-- 8.2 boss 追加 M37 海报权限（micro_pro + micro_lite）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:poster:list')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:poster:list"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:poster:staff')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:poster:staff"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:poster:store')
WHERE `role_code` = 'boss' AND `version_type` IN ('micro_pro','micro_lite') AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:poster:store"');

-- 8.3 platform_admin 追加 M36/M37 全部权限（用于平台侧运维查看）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:settlement:list')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:settlement:list"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:poster:list')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:poster:list"');

-- =====================================================================
-- 9. 存量 sys_role_menu 补录
-- =====================================================================
-- 9.1 boss 克隆角色（小微版）追加 7360~7364 + 7370~7372
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` BETWEEN 7360 AND 7372;

-- 9.2 platform_admin 克隆角色追加 7360 / 7370
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` IN (7360, 7370);

-- 9.3 超管 role_id=1 补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` BETWEEN 7360 AND 7372;

-- =====================================================================
-- 10. 验收查询（执行后人工核对）
-- =====================================================================
-- 10.1 四表就绪
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME IN ('activity_settlement','settlement_income_item',
--                       'settlement_cost_item','industry_settlement_template');
-- 预期：4 行
--
-- 10.2 行业模板种子数据就绪
-- SELECT industry_code, COUNT(*) FROM industry_settlement_template
--  WHERE del_flag = '0' GROUP BY industry_code;
-- 预期：board_game=3 / travel=6 / massage=3
--
-- 10.3 菜单 7360~7372 就绪
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--  WHERE menu_id BETWEEN 7360 AND 7372 ORDER BY menu_id;
-- 预期：8 行（7360~7364 + 7370~7372）
--
-- 10.4 role_template boss 两档均含结算+海报权限
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:settlement:list"') AS has_settle,
--        JSON_CONTAINS(menu_keys,'"micro:poster:list"')     AS has_poster
--   FROM role_template WHERE role_code = 'boss' AND version_type IN ('micro_pro','micro_lite');
-- 预期：两行均 1/1
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
