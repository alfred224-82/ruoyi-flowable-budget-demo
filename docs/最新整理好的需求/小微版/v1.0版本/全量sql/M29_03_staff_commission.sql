-- =====================================================================
-- M29_03  员工提成比例配置（REQ-03，v1.3 需求清单，仅 PC）
-- =====================================================================
-- 需求概述：
--   Boss/租户管理员 → 系统设置（租户管理菜单 7500）→「员工提成」独立菜单：
--   · 为每个员工配置 提成比例(commission_rate, 0~1 小数，0.1000=10%) + 基础工资(base_salary)。
--   · 员工当月薪酬成本 = 基础工资 + 提成；提成 = 当月基数 × 比例。
--   · 提成基数口径：员工当月已完成订单金额（复用 StaffSettlement 口径：
--     booking_order.staff_user_id + order_status=COMPLETED + complete_time 落在当月）。
--   · 成本报表新增「人力成本 = 基础工资 + 提成」分类聚合。
--   · ★App 端本次不动（仅 PC）。
--
-- 前置：
--   M15_17（platform_admin 租户管理菜单目录 7500）
--   M18_03（预约策略 7630 同目录，本菜单 7350 与其平级）
--
-- 规范：
--   1. staff_commission 为【租户级表】(含 tenant_id)，多租户插件自动隔离；含 del_flag 逻辑删除。
--   2. UNIQUE(tenant_id, staff_user_id, del_flag) 每员工一条配置。
--   3. 继承 TenantEntity/BaseEntity 审计列（create_dept/create_by/create_time/update_by/update_time）。
--   4. 建表 CREATE TABLE IF NOT EXISTS 幂等；菜单先删后插幂等；授权 INSERT IGNORE / JSON_CONTAINS 守卫幂等。
--   5. ★menu_id 区段 7350~7354（已核实 7343~7399 空闲；避让 7280~7299 房间布局/M28 预留）。
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 员工提成配置表 staff_commission
-- =====================================================================
CREATE TABLE IF NOT EXISTS `staff_commission` (
  `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`       bigint        NOT NULL                COMMENT '租户ID',
  `staff_user_id`   bigint        NOT NULL                COMMENT '员工用户ID（sys_user.user_id）',
  `staff_name`      varchar(64)   DEFAULT NULL            COMMENT '员工姓名（冗余，便于列表展示）',
  `commission_rate` decimal(5,4)  NOT NULL DEFAULT 0.0000 COMMENT '提成比例（0~1 小数，如 0.1000=10%）',
  `base_salary`     decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '基础工资（元/月）',
  `effective_date`  date          DEFAULT NULL            COMMENT '生效日期',
  `status`          char(1)       DEFAULT '0'             COMMENT '状态（0-正常；1-停用）',
  `remark`          varchar(500)  DEFAULT NULL            COMMENT '备注',
  `create_dept`     bigint        DEFAULT NULL            COMMENT '创建部门',
  `create_by`       varchar(64)   DEFAULT NULL            COMMENT '创建者',
  `create_time`     datetime      DEFAULT NULL            COMMENT '创建时间',
  `update_by`       varchar(64)   DEFAULT NULL            COMMENT '更新者',
  `update_time`     datetime      DEFAULT NULL            COMMENT '更新时间',
  `del_flag`        char(1)       DEFAULT '0'             COMMENT '删除标志（0-正常；2-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_staff` (`tenant_id`, `staff_user_id`, `del_flag`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工提成比例配置表（REQ-03，租户级）';

-- =====================================================================
-- 2. 菜单：员工提成（7350 C）+ 4 个按钮（7351~7354 F），挂 7500 租户管理菜单下
--    ★platform_admin 专属（与预约策略 7630 同目录）；先删后插幂等
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7350, 7351, 7352, 7353, 7354);
DELETE FROM `sys_menu`      WHERE `menu_id` IN (7350, 7351, 7352, 7353, 7354);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7350, '员工提成', 7500, 26, 'staffCommission', 'micro/staffCommission/index', 1, 0, 'C', '0', '0', 'micro:staffCommission:list', 'pc', 'money', 'admin', NOW(), 'REQ-03 员工提成比例配置（platform_admin PC，与预约策略同级）'),
(7351, '提成新增', 7350, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:staffCommission:add', 'pc', '#', 'admin', NOW(), 'REQ-03 新增员工提成配置'),
(7352, '提成修改', 7350, 2, '', NULL, 1, 0, 'F', '0', '0', 'micro:staffCommission:edit', 'pc', '#', 'admin', NOW(), 'REQ-03 修改员工提成配置'),
(7353, '提成删除', 7350, 3, '', NULL, 1, 0, 'F', '0', '0', 'micro:staffCommission:remove', 'pc', '#', 'admin', NOW(), 'REQ-03 删除员工提成配置'),
(7354, '提成导出', 7350, 4, '', NULL, 1, 0, 'F', '0', '0', 'micro:staffCommission:export', 'pc', '#', 'admin', NOW(), 'REQ-03 导出员工提成配置');

-- =====================================================================
-- 3. role_template：platform_admin（全版本档）追加 5 个权限键
--    ★JSON_CONTAINS 守卫幂等追加；一条语句覆盖 micro/micro_pro/micro_lite 三档
-- =====================================================================
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffCommission:list')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffCommission:list"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffCommission:add')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffCommission:add"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffCommission:edit')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffCommission:edit"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffCommission:remove')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffCommission:remove"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffCommission:export')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffCommission:export"');

-- =====================================================================
-- 4. 存量 sys_role_menu 补录
-- =====================================================================
-- 4.1 platform_admin（小微版全版本档）：7350（C 页面）+ 7351~7354（F 按钮）
--   ★platform_admin 已有 7500 祖先目录（M15_17），7350 挂 7500 下可正常显示
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` IN (7350, 7351, 7352, 7353, 7354);

-- 4.2 超管 role_id=1 补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7350, 7351, 7352, 7353, 7354);

-- =====================================================================
-- 5. 验收查询（执行后人工核对）
-- =====================================================================
-- 5.1 staff_commission 表就绪
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_commission';
-- 预期：含 commission_rate decimal(5,4) / base_salary decimal(12,2) / uk_tenant_staff 唯一键
--
-- 5.2 菜单 7350~7354 存在且挂在 7500 下
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--  WHERE menu_id IN (7350,7351,7352,7353,7354) ORDER BY menu_id;
-- 预期：5 行（7350 C parent=7500 / 7351~7354 F parent=7350）
--
-- 5.3 role_template platform_admin 三档均含 5 个提成权限
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:staffCommission:list"')   AS has_list,
--        JSON_CONTAINS(menu_keys,'"micro:staffCommission:add"')    AS has_add,
--        JSON_CONTAINS(menu_keys,'"micro:staffCommission:edit"')   AS has_edit,
--        JSON_CONTAINS(menu_keys,'"micro:staffCommission:remove"') AS has_remove,
--        JSON_CONTAINS(menu_keys,'"micro:staffCommission:export"') AS has_export
--   FROM role_template WHERE role_code = 'platform_admin' AND status = '0';
-- 预期：micro/micro_pro/micro_lite 三行均 1/1/1/1/1
--
-- 5.4 存量 platform_admin 克隆角色已关联 7350~7354
-- SELECT COUNT(DISTINCT r.role_id) AS granted_roles
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'platform_admin' AND r.del_flag = '0'
--   JOIN sys_tenant t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
--  WHERE rm.menu_id IN (7350,7351,7352,7353,7354);
-- 预期：>= 小微版租户数量
--
-- ★执行后请重启后端，清除内存缓存；用户需退出重新登录刷新 Sa-Token 权限快照
-- =====================================================================
