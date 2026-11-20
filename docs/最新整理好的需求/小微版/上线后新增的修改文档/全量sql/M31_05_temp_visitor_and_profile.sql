-- =====================================================================
-- M31_05  临时游客账号表 + 游客信息补全表（T31.3）
-- 版本：v1.0  日期：2026-10-24
-- 依据：《小微版 V3.9 开发文档》M31 活动海报生成与游客扫码登录 T31.3
-- 归属版本：小微版（MICRO，micro_pro + micro_lite 通用，FEAT_VISITOR_SCAN_LOGIN）
-- 前置依赖：M15_01（sys_user 表）、M15_06（sys_dept 根部门）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. 新建 temp_visitor_account（临时游客账号，30 天有效期，到期自动废弃）
--   2. 新建 visitor_profile（游客信息补全表，与 sys_user 一对一）
--   3. sys_menu 新增 7300~7305（M31 Boss 海报生成 + 游客信息管理）
--   4. feature_flag 新增 FEAT_VISITOR_SCAN_LOGIN（第 43 项，组 E，全版本启用）
--   5. role_template boss + platform_admin 追加 M31 权限键
--   6. 存量 sys_role_menu 补录
-- ---------------------------------------------------------------------
-- 规范：
--   1. 建表使用 CREATE TABLE IF NOT EXISTS 幂等语义（不用 DROP，保护存量数据）
--   2. ALTER 使用 INFORMATION_SCHEMA 条件判断（PREPARE 动态执行），幂等可重复
--   3. 菜单先删后插幂等；role_template 使用 JSON_CONTAINS 守卫追加；sys_role_menu 使用 INSERT IGNORE
--   4. 新表继承 BaseEntity 审计列（create_by/create_time/update_by/update_time）+ tenant_id 显式声明
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. temp_visitor_account 临时游客账号（30 天有效期，到期自动废弃）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `temp_visitor_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID（FK→sys_tenant.id）',
  `phone_last4` VARCHAR(4) NOT NULL COMMENT '手机号后 4 位，如 1234',
  `user_id` BIGINT NOT NULL COMMENT 'FK→sys_user.user_id',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '完整手机号（脱敏存储）',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `expire_time` DATETIME NOT NULL COMMENT '30 天后过期',
  `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0=有效 1=已补全 2=已废弃',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_phone` (`tenant_id`, `phone_last4`),
  INDEX `idx_expire` (`expire_time`),
  INDEX `idx_status` (`status`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='临时游客账号（T31.3，30 天有效期，到期自动废弃）';

-- ----------------------------
-- 2. visitor_profile 游客信息补全表（与 sys_user 一对一）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `visitor_profile` (
  `user_id` BIGINT NOT NULL COMMENT 'PK=FK→sys_user.user_id',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号（脱敏存储）',
  `birthday` DATE DEFAULT NULL COMMENT '生日',
  `gender` TINYINT(1) DEFAULT NULL COMMENT '1=男 2=女',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `wechat_openid` VARCHAR(100) DEFAULT NULL COMMENT '微信 openid（升级正式账号时绑定）',
  `alipay_user_id` VARCHAR(100) DEFAULT NULL COMMENT '支付宝 user_id（升级正式账号时绑定）',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_complete` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0=未完善 1=已完善',
  PRIMARY KEY (`user_id`),
  INDEX `idx_tenant` (`tenant_id`),
  INDEX `idx_is_complete` (`is_complete`),
  INDEX `idx_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游客信息补全表（T31.3，与 sys_user 一对一）';

-- ----------------------------
-- 3. sys_menu 新增 7300~7305（M31 Boss 海报生成 + 游客信息管理）
--    ★先删后插幂等
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7300 AND 7305;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 7300 AND 7305;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7300: 生成活动海报（F 按钮，挂在 7151 活动管理下）
-- ★权限复用 micro:promo:edit（活动编辑），不引入独立权限码，避免租户克隆角色未授权 403
(7300, '生成活动海报', 7151, 30, '', NULL, 1, 0, 'F', '0', '0', 'micro:promo:edit', 'pc', '#', 'admin', NOW(), 'T31.1 Boss 上传海报底图 + 合成二维码'),
-- 7301: 游客信息管理（C 页面，Boss PC 顾客管理下）
(7301, '游客信息管理', 7100, 25, 'visitorProfile', 'micro/customer/profile/index', 1, 0, 'C', '0', '0', 'micro:customer:profile:view', 'pc', 'peoples', 'admin', NOW(), 'T31.6 Boss 补全游客信息列表页'),
-- 7302: 补全游客信息（F 按钮）
(7302, '补全游客信息', 7301, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:customer:profile:edit', 'pc', '#', 'admin', NOW(), 'T31.6 Boss 补全游客信息弹窗'),
-- 7303: 游客升级正式顾客（F 按钮）
(7303, '游客升级正式顾客', 7301, 2, '', NULL, 1, 0, 'F', '0', '0', 'micro:customer:upgrade', 'pc', '#', 'admin', NOW(), 'T31.7 单个游客升级为正式顾客'),
-- 7304: 批量转正临时游客（F 按钮）
(7304, '批量转正临时游客', 7301, 3, '', NULL, 1, 0, 'F', '0', '0', 'micro:customer:profile:upgrade', 'pc', '#', 'admin', NOW(), 'T31.8 Boss 批量转正临时游客'),
-- 7305: 下载海报（F 按钮）
(7305, '下载海报', 7151, 31, '', NULL, 1, 0, 'F', '0', '0', 'micro:promo:edit', 'pc', '#', 'admin', NOW(), 'T31.2 下载合成后的海报图片');

-- ----------------------------
-- 4. feature_flag 新增 FEAT_VISITOR_SCAN_LOGIN（第 44 项，组 E，小微版启用）
--    ★REPLACE INTO 幂等（主键冲突时自动替换，无需前置 DELETE）
-- ----------------------------
REPLACE INTO `feature_flag`
  (`id`, `code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `micro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
(44, 'FEAT_VISITOR_SCAN_LOGIN', '游客扫码登录', 'E', NULL, 0, 0, 0, 1, '0', 'admin', NOW(),
 'T31.3~T31.8；★仅小微版启用；游客扫码海报二维码→短信登录→临时账号→补全信息/升级正式/批量转正');

-- biz_bundle 全部业态包 feature_flags 幂等补录（三维交集根因防御，参考 M26_02 / M28_01 模式）
UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_VISITOR_SCAN_LOGIN')
WHERE NOT JSON_CONTAINS(`feature_flags`, '"FEAT_VISITOR_SCAN_LOGIN"');

-- ----------------------------
-- 5. role_template boss + platform_admin 追加 M31 权限键
--    ★海报生成/下载复用 micro:promo:edit（两档 boss 已有），无需追加 role_template
--    ★JSON_CONTAINS 守卫幂等追加（仅游客管理权限）
-- ----------------------------
-- boss: micro:customer:profile:view / edit / upgrade（micro_pro 专属，游客管理为付费档能力）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:customer:profile:view')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro_pro')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:customer:profile:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:customer:profile:edit')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro_pro')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:customer:profile:edit"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:customer:upgrade')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro_pro')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:customer:upgrade"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:customer:profile:upgrade')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro_pro')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:customer:profile:upgrade"');

-- platform_admin: micro:customer:profile:view / edit / upgrade（两档）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:customer:profile:view')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND `version_type` IN ('micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:customer:profile:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:customer:profile:edit')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND `version_type` IN ('micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:customer:profile:edit"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:customer:upgrade')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND `version_type` IN ('micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:customer:upgrade"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:customer:profile:upgrade')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND `version_type` IN ('micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:customer:profile:upgrade"');

-- ----------------------------
-- 6. 存量 sys_role_menu 补录
--    ★boss（micro_pro 全档 + micro_lite 仅海报权限）+ platform_admin（全档）+ 超管 role_id=1
-- ----------------------------
-- boss micro_pro: 7301~7304（游客信息管理页 + 按钮；7300/7305 复用 micro:promo:edit 已授权）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND t.`version_type` = 'micro_pro'
  AND m.`menu_id` BETWEEN 7301 AND 7304;

-- boss micro_lite: 无游客管理权限，仅补 7301/7305 作为菜单占位（海报功能由 micro:promo:edit 放行）
-- （micro_lite boss 无需额外 sys_role_menu，micro:promo:edit 已在 M19_04 授权）

-- platform_admin: 7301~7304（游客信息管理 + 补全 + 升级 + 批量转正）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
  AND m.`menu_id` IN (7301, 7302, 7303, 7304);

-- 超管 role_id=1 全量补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` BETWEEN 7300 AND 7305;

-- ----------------------------
-- 7. 验收查询（执行后人工核对）
-- ----------------------------
-- 7.1 两表已创建
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME IN ('temp_visitor_account','visitor_profile');
-- 预期：2 行
--
-- 7.2 菜单 7300~7305 存在
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--  WHERE menu_id BETWEEN 7300 AND 7305 ORDER BY menu_id;
-- 预期：6 行
--
-- 7.3 FEAT_VISITOR_SCAN_LOGIN 功能开关已注册
-- SELECT code, flag_group, micro_enabled FROM feature_flag WHERE code = 'FEAT_VISITOR_SCAN_LOGIN';
-- 预期：1 行（E / micro_enabled=1）
--
-- 7.4 role_template boss 游客管理权限键就绪
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:customer:profile:view"')   AS has_view,
--        JSON_CONTAINS(menu_keys,'"micro:customer:profile:edit"')   AS has_edit
--   FROM role_template WHERE role_code='boss' AND version_type IN ('micro_pro','micro_lite');
-- 预期：micro_pro 为 1/1；micro_lite 为 0/0
--
-- 7.5 存量 boss micro_pro 已关联 7301~7304
-- SELECT COUNT(*) AS granted FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_tenant t ON r.tenant_id = t.id AND t.tenant_type = 'micro' AND t.version_type = 'micro_pro'
--  WHERE rm.menu_id BETWEEN 7301 AND 7304;
-- 预期：>= 小微版 micro_pro 租户数 × 4
