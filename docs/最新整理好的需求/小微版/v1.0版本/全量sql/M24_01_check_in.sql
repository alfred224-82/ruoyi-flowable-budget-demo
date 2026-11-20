-- =====================================================================
-- M24_01  员工打卡（T24.1 / T24.6，v1.82 新增）
-- 版本：v1.0  日期：2026-10-12
-- 依据：《资金管控系统开发任务文档（小微版）》v1.82 M24 里程碑 T24.1~T24.7
-- 前置依赖：M15_01（role_template）、M15_06（sys_menu 7000~7499）、
--           M15_07/M15_10（micro/micro_lite/micro_pro 角色模板）、
--           M1_05（feature_flag / biz_bundle）、M22_01（FEAT_STAFF_SCHEDULE 参考模式）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. 新建 check_in_record 表（打卡记录，租户级）
--   2. 新建 check_in_config 表（打卡配置，租户级，UNIQUE(tenant_id)）
--   3. industry_template 补 check_in_enabled 列（三级开关之平台行业级）
--   4. sys_tenant 补 check_in_enabled 列（三级开关之租户级）
--   5. feature_flag 新增第 40 项 FEAT_CHECK_IN（组 E=小微版专属，micro_enabled=1）
--   6. biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_CHECK_IN
--   7. sys_menu 新增 7250~7254（打卡记录 C + 2 F 按钮 + 2 小程序 F 按钮）
--   8. role_template：boss(micro/micro_pro) 追加 3 权限键；
--      salesman(micro/micro_pro) 追加 2 权限键；
--      ★micro_lite 三配方均不追加（免费版不启用打卡功能）
--   9. 存量 sys_role_menu 补录（小微版 boss/salesman 克隆角色）
-- 规范：
--   1. 建表使用 CREATE TABLE IF NOT EXISTS 语义（先删后建仅限本脚本自身创建的表）
--   2. ALTER/UPDATE 使用 INFORMATION_SCHEMA 条件判断 + SQL_SAFE_UPDATES 包裹
--   3. 先删后插幂等（菜单/角色关联），JSON_CONTAINS 守卫（模板追加）
--   4. ★三级开关链路：industry_template.check_in_enabled → sys_tenant.check_in_enabled → check_in_config.enabled
--   5. ★打卡不受活动锁定影响，锁定后仍可打卡
--   6. ★source 字段区分 NORMAL=正常打卡 / SUPPLEMENT=Boss 补录
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 新建 check_in_record 表（打卡记录，租户级）
--    UNIQUE(tenant_id, activity_id, staff_user_id, check_in_time) 防同一员工同一时间重复打卡
-- ----------------------------
DROP TABLE IF EXISTS `check_in_record`;
CREATE TABLE `check_in_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID（FK→promotion_activity.id）',
  `staff_user_id` BIGINT NOT NULL COMMENT '打卡员工 user_id（FK→sys_user.user_id，salesman 角色）',
  `check_in_time` DATETIME NOT NULL COMMENT '打卡时间',
  `location_name` VARCHAR(255) DEFAULT NULL COMMENT '打卡地点名称',
  `location_lat` DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
  `location_lng` DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
  `photo_url` VARCHAR(500) DEFAULT NULL COMMENT '打卡照片URL（OSS）',
  `is_late` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否迟到：0=否 1=是',
  `watermark_text` VARCHAR(500) DEFAULT NULL COMMENT '水印文字（时间/位置/用户名）',
  `source` VARCHAR(10) NOT NULL DEFAULT 'NORMAL' COMMENT '来源：NORMAL=正常打卡 / SUPPLEMENT=Boss 补录',
  `supplement_reason` VARCHAR(255) DEFAULT NULL COMMENT '补录原因（Boss 补录时填写）',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_activity_staff_time` (`tenant_id`, `activity_id`, `staff_user_id`, `check_in_time`),
  INDEX `idx_tenant_activity` (`tenant_id`, `activity_id`),
  INDEX `idx_tenant_staff` (`tenant_id`, `staff_user_id`),
  INDEX `idx_check_in_time` (`check_in_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录表（T24.1 员工到岗拍照+时间+地点+水印；迟到判定；Boss 可补录）';

-- ----------------------------
-- 2. 新建 check_in_config 表（打卡配置，租户级，UNIQUE(tenant_id)）
-- ----------------------------
DROP TABLE IF EXISTS `check_in_config`;
CREATE TABLE `check_in_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用打卡：0=否 1=是（三级开关之 Boss 活动级）',
  `photo_required` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '打卡是否必须拍照：0=否 1=是',
  `late_threshold_minutes` INT NOT NULL DEFAULT 15 COMMENT '迟到判定阈值（分钟）：打卡时间超过活动开始时间+N分钟视为迟到',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡配置表（T24.1 租户级打卡开关+拍照要求+迟到阈值）';

-- ----------------------------
-- 3. industry_template 补 check_in_enabled 列（三级开关之平台行业级）
--    ★幂等：INFORMATION_SCHEMA 守卫，列已存在则跳过
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'industry_template' AND COLUMN_NAME = 'check_in_enabled');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `industry_template` ADD COLUMN `check_in_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''行业级打卡开关（三级开关之平台 Admin 行业级，T24.2）''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 4. sys_tenant 补 check_in_enabled 列（三级开关之租户级）
--    ★幂等：INFORMATION_SCHEMA 守卫
-- ----------------------------
SET @col_exists2 = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'check_in_enabled');
SET @sql2 = IF(@col_exists2 = 0,
  'ALTER TABLE `sys_tenant` ADD COLUMN `check_in_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''租户级打卡开关（三级开关之租户 Admin 级，T24.2）''',
  'SELECT 1');
PREPARE stmt FROM @sql2;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 5. feature_flag 新增第 40 项 FEAT_CHECK_IN（组 E=小微版专属）
--    ★micro_enabled=1；basic/advanced/pro 三列均 0（企业版不生效）
--    ★micro_lite vs micro_pro 区分：role_template 权限控制
-- ----------------------------
DELETE FROM `feature_flag` WHERE `code` = 'FEAT_CHECK_IN';
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `micro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('FEAT_CHECK_IN', '员工打卡(H5拍照+时间+地点+水印+迟到判定+三级开关)', 'E', NULL, 0, 0, 0, 1, '0', 'admin', NOW(),
 'T24.6；★仅小微版启用；micro_lite 不授 boss 打卡管理权限；micro_pro 全启用；三级开关链路：行业→租户→Boss');

-- ----------------------------
-- 6. biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_CHECK_IN
--    ★与 M21_07/M22_01 同模式
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;

UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_CHECK_IN')
WHERE `feature_flags` IS NOT NULL
  AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_CHECK_IN"');

SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 7. sys_menu 打卡菜单（7250~7254）
--    ★7250~7252 挂 7100 小微运营（Boss PC）
--    ★7253 挂 7300 小程序·业务员（员工 MP 打卡提交）
--    ★7254 挂 7300 小程序·业务员（员工 MP 查看本人打卡）
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (7250, 7251, 7252, 7253, 7254);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7250: 打卡记录管理（C 类型页面，Boss PC，挂 7100 小微运营）
(7250, '打卡记录', 7100, 74, 'checkIn', 'micro/checkIn/index', 1, 0, 'C', '0', '0', 'micro:checkIn:view', 'pc', 'date', 'admin', NOW(), 'T24.5 Boss PC 打卡记录管理（查看/补录/导出）'),
-- 7251: 补录打卡（F 按钮）
(7251, '补录打卡', 7250, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:checkIn:supplement', 'pc', '#', 'admin', NOW(), 'T24.5 Boss 代员工补录打卡（员工忘打时）'),
-- 7252: 导出打卡记录（F 按钮）
(7252, '导出打卡记录', 7250, 2, '', NULL, 1, 0, 'F', '0', '0', 'micro:checkIn:export', 'pc', '#', 'admin', NOW(), 'T24.5 导出打卡记录 Excel'),
-- 7253: 员工打卡提交（F 按钮，MP，挂 7300 小程序·业务员）
(7253, '打卡提交', 7300, 5, '', NULL, 1, 0, 'F', '0', '0', 'micro:checkIn:submit', 'mini', '#', 'admin', NOW(), 'T24.4 员工小程序打卡提交（拍照+时间+地点）'),
-- 7254: 员工查看本人打卡（F 按钮，MP，挂 7300 小程序·业务员）
(7254, '查看我的打卡', 7300, 6, '', NULL, 1, 0, 'F', '0', '0', 'micro:checkIn:my', 'mini', '#', 'admin', NOW(), 'T24.4 员工小程序查看本人打卡记录');

-- ----------------------------
-- 8. role_template 追加权限（micro/micro_pro 两档；★micro_lite 不追加）
--    ★幂等：NOT JSON_CONTAINS 守卫
-- ----------------------------
-- 8.1 micro_pro boss 追加 3 权限键（view/supplement/export）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:checkIn:view')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:checkIn:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:checkIn:supplement')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:checkIn:supplement"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:checkIn:export')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:checkIn:export"');

-- 8.2 micro_pro salesman 追加 2 权限键（submit/my）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:checkIn:submit')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'salesman' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:checkIn:submit"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:checkIn:my')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'salesman' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:checkIn:my"');

-- 8.3 micro (旧档，兼容存量) boss 追加 3 权限键
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:checkIn:view')
WHERE `version_type` = 'micro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:checkIn:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:checkIn:supplement')
WHERE `version_type` = 'micro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:checkIn:supplement"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:checkIn:export')
WHERE `version_type` = 'micro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:checkIn:export"');

-- 8.4 micro (旧档) salesman 追加 2 权限键
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:checkIn:submit')
WHERE `version_type` = 'micro' AND `role_code` = 'salesman' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:checkIn:submit"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:checkIn:my')
WHERE `version_type` = 'micro' AND `role_code` = 'salesman' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:checkIn:my"');

-- ----------------------------
-- 9. 存量 sys_role_menu 补录（小微版 boss/salesman 克隆角色）
--    ★boss：7250~7252 + 7100 祖先目录
--    ★salesman：7253~7254 + 7300 祖先目录
-- ----------------------------
-- 9.1 boss（小微版 micro_pro / micro，★不含 micro_lite 免费版）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND t.`version_type` IN ('micro_pro', 'micro')
  AND m.`menu_id` IN (7250, 7251, 7252, 7100);

-- 9.2 salesman（小微版 micro_pro / micro）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'salesman' AND r.`del_flag` = '0'
  AND t.`version_type` IN ('micro_pro', 'micro')
  AND m.`menu_id` IN (7253, 7254, 7300);

-- 9.3 超管 role_id=1 全量补录（便于 admin 平台侧调试）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7250, 7251, 7252, 7253, 7254);

-- ----------------------------
-- 10. 验收查询（执行后人工核对）
-- ----------------------------
-- 10.1 check_in_record / check_in_config 两表已创建
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('check_in_record','check_in_config');
-- 预期：2 行
--
-- 10.2 industry_template.check_in_enabled 列已补
-- SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'industry_template' AND COLUMN_NAME = 'check_in_enabled';
-- 预期：1 行
--
-- 10.3 sys_tenant.check_in_enabled 列已补
-- SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'check_in_enabled';
-- 预期：1 行
--
-- 10.4 FEAT_CHECK_IN 落表且 micro_enabled=1
-- SELECT code, flag_group, micro_enabled, basic_enabled, status FROM feature_flag WHERE code = 'FEAT_CHECK_IN';
-- 预期：1 行，E / micro_enabled=1 / basic_enabled=0 / status=0
--
-- 10.5 菜单 7250~7254 存在
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu WHERE menu_id IN (7250,7251,7252,7253,7254);
-- 预期：5 行（C/F/F/F/F），7250~7252 client_type=pc，7253~7254 client_type=mini
--
-- 10.6 role_template micro_pro boss 含 3 新权限
-- SELECT JSON_CONTAINS(menu_keys,'"micro:checkIn:view"')       AS has_view,
--        JSON_CONTAINS(menu_keys,'"micro:checkIn:supplement"') AS has_supplement,
--        JSON_CONTAINS(menu_keys,'"micro:checkIn:export"')     AS has_export
--   FROM role_template WHERE version_type = 'micro_pro' AND role_code = 'boss';
-- 预期：1/1/1
--
-- 10.7 role_template micro_pro salesman 含 2 新权限
-- SELECT JSON_CONTAINS(menu_keys,'"micro:checkIn:submit"') AS has_submit,
--        JSON_CONTAINS(menu_keys,'"micro:checkIn:my"')     AS has_my
--   FROM role_template WHERE version_type = 'micro_pro' AND role_code = 'salesman';
-- 预期：1/1
--
-- 10.8 ★micro_lite 三配方均不含新权限（免费版不启用）
-- SELECT role_code,
--        JSON_CONTAINS(menu_keys,'"micro:checkIn:view"') AS has_view
--   FROM role_template WHERE version_type = 'micro_lite';
-- 预期：全部 0
--
-- ★执行后请重启后端，清除内存缓存（BizBundleServiceImpl.flagCache /
--   FeatureFlagServiceImpl.flagsCache / LicenseServiceImpl.tenantFeaturesCache），
--   否则三维交集仍按旧缓存计算
