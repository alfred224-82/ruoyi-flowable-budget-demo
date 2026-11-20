-- =====================================================================
-- M31_04  T31.5 隐私分级基座：员工联系方式公开开关 + 门店客服微信号
-- =====================================================================
-- 需求概述（《小账本 H5 界面开发任务文档 v1.1》§一 M31 T31.5）：
--   1) 新建 staff_contact_public 表：按员工单独设置 show_phone / show_wechat 开关，
--      新入职默认全关；顾客端点头像弹名片时，按开关决定显示「打电话」/「加微信」入口。
--   2) sys_tenant 补 contact_wechat 列（L1 门店信息，顾客端「联系我们」长按识别二维码）。
--      ★contact_phone 已存在（M1 底座），本次仅补微信号列。
--   3) 落地两个新权限键对应的 F 级菜单按钮：
--        · micro:staff:manage（员工档案 7051 下）— 员工联系方式公开开关配置
--        · micro:store:manage（店铺信息设置 7058 下）— 门店联系方式配置
--   4) role_template boss / platform_admin 追加两个权限键（micro_pro + micro_lite 均含）；
--      存量克隆角色 sys_role_menu 补录。
--
-- 规范：
--   1. staff_contact_public 为【租户级表】(含 tenant_id)，多租户插件自动隔离。
--   2. UNIQUE(tenant_id, staff_id) 每员工一条配置。
--   3. 建表 CREATE TABLE IF NOT EXISTS 幂等；ALTER 用 INFORMATION_SCHEMA 守卫幂等；
--      菜单先删后插幂等；授权 INSERT IGNORE / JSON_CONTAINS 守卫幂等。
--   4. ★menu_id 区段：7059（挂 7051 员工档案下）+ 7063（挂 7058 店铺信息设置下）；
--      已核实 7059 / 7063 空闲（7058→7060 之间、7062 之后）。
--
-- 前置：
--   [45] M15_06（小微版菜单 7000~7499，含 7050/7051/7058 目录）
--   [46] M15_07（micro 角色模板三配方 boss/salesman/customer）
--   [49] M15_10（micro_lite / micro_pro 分档配方）
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 员工联系方式公开开关表 staff_contact_public
-- =====================================================================
CREATE TABLE IF NOT EXISTS `staff_contact_public` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`         bigint       NOT NULL                COMMENT '租户ID',
  `staff_id`          bigint       NOT NULL                COMMENT '员工用户ID（FK→sys_user.user_id）',
  `show_phone`        tinyint(1)   NOT NULL DEFAULT 0      COMMENT '公开手机号：0=关 1=开',
  `show_wechat`       tinyint(1)   NOT NULL DEFAULT 0      COMMENT '公开微信号：0=关 1=开',
  `wechat_qrcode_url` varchar(500) DEFAULT NULL            COMMENT '员工微信二维码 OSS URL（顾客长按识别添加）',
  `create_dept`       bigint       DEFAULT NULL            COMMENT '创建部门',
  `create_by`         varchar(64)  DEFAULT NULL            COMMENT '创建者',
  `create_time`       datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`         varchar(64)  DEFAULT NULL            COMMENT '更新者',
  `update_time`       datetime     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_staff` (`tenant_id`, `staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工联系方式公开开关（T31.5，§八之二）';

-- =====================================================================
-- 2. sys_tenant 补 contact_wechat 列（L1 门店客服微信号）
--    ★contact_phone 已存在（M1 底座），仅补微信号列；INFORMATION_SCHEMA 守卫幂等
-- =====================================================================
DROP PROCEDURE IF EXISTS m31_04_add_tenant_contact_wechat;
DELIMITER $$
CREATE PROCEDURE m31_04_add_tenant_contact_wechat()
BEGIN
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE()
                    AND TABLE_NAME = 'sys_tenant'
                    AND COLUMN_NAME = 'contact_wechat') THEN
        ALTER TABLE sys_tenant
          ADD COLUMN contact_wechat varchar(50) DEFAULT NULL
            COMMENT '门店客服微信号（T31.5，L1 门店信息，顾客端「联系我们」长按识别二维码）'
            AFTER contact_phone;
    END IF;
END$$
DELIMITER ;
CALL m31_04_add_tenant_contact_wechat();
DROP PROCEDURE IF EXISTS m31_04_add_tenant_contact_wechat;

-- =====================================================================
-- 3. 菜单：两个 F 级按钮权限
--    · 7059「员工联系方式公开开关」挂 7051（员工档案）下，perms=micro:staff:manage
--    · 7063「门店联系方式」挂 7058（店铺信息设置）下，perms=micro:store:manage
--    ★先删后插幂等；client_type=pc（老板 PC 端配置）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7059, 7063);
DELETE FROM `sys_menu`      WHERE `menu_id` IN (7059, 7063);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7059, '员工联系方式公开开关', 7051, 5, '', NULL, 1, 0, 'F', '0', '0', 'micro:staff:manage', 'pc', '#', 'admin', NOW(), 'T31.5 §八之二 老板按员工设 show_phone/show_wechat 开关'),
(7063, '门店联系方式',         7058, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:store:manage', 'pc', '#', 'admin', NOW(), 'T31.5 §六 TMP.8 联系我们：门店电话/客服微信号/地址');

-- =====================================================================
-- 4. role_template：boss + platform_admin 追加两个权限键
--    ★JSON_CONTAINS 守卫幂等追加；boss 覆盖 micro_pro + micro_lite 两档；
--      platform_admin 覆盖 micro + micro_pro + micro_lite 三档
-- =====================================================================
-- 4.1 boss → micro:staff:manage
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staff:manage')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staff:manage"');

-- 4.2 boss → micro:store:manage
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:store:manage')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:store:manage"');

-- 4.3 platform_admin → micro:staff:manage
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staff:manage')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staff:manage"');

-- 4.4 platform_admin → micro:store:manage
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:store:manage')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:store:manage"');

-- =====================================================================
-- 5. 存量 sys_role_menu 补录（小微版租户的 boss + platform_admin 克隆角色）
-- =====================================================================
-- 5.1 boss 克隆角色 → 7059 + 7063
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` IN (7059, 7063);

-- 5.2 platform_admin 克隆角色 → 7059 + 7063
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` IN (7059, 7063);

-- 5.3 超管 role_id=1 补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7059, 7063);

-- =====================================================================
-- 6. 验收查询（执行后人工核对）
-- =====================================================================
-- 6.1 staff_contact_public 表就绪
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_contact_public';
-- 预期：含 show_phone tinyint(1) / show_wechat tinyint(1) / uk_tenant_staff 唯一键
--
-- 6.2 sys_tenant.contact_wechat 列已加
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'contact_wechat';
-- 预期：1 行 varchar(50)
--
-- 6.3 菜单 7059 / 7063 存在且挂载正确
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--  WHERE menu_id IN (7059, 7063) ORDER BY menu_id;
-- 预期：7059 F parent=7051 perms=micro:staff:manage / 7063 F parent=7058 perms=micro:store:manage
--
-- 6.4 role_template boss + platform_admin 均含两个权限键
-- SELECT role_code, version_type,
--        JSON_CONTAINS(menu_keys,'"micro:staff:manage"') AS has_staff,
--        JSON_CONTAINS(menu_keys,'"micro:store:manage"') AS has_store
--   FROM role_template
--  WHERE role_code IN ('boss','platform_admin') AND status = '0'
--    AND version_type IN ('micro','micro_pro','micro_lite');
-- 预期：全部行 has_staff=1 且 has_store=1
--
-- 6.5 存量小微版 boss + platform_admin 克隆角色已关联 7059 / 7063
-- SELECT r.role_key, COUNT(DISTINCT r.role_id) AS granted_roles
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key IN ('boss','platform_admin') AND r.del_flag = '0'
--   JOIN sys_tenant t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
--  WHERE rm.menu_id IN (7059, 7063)
--  GROUP BY r.role_key;
-- 预期：boss / platform_admin 各 >= 小微版租户数量
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
