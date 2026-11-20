-- =====================================================================
-- M25_01  投诉与平台监管（T25.1 / T25.7，v1.82 新增）
-- 版本：v1.0  日期：2026-10-12
-- 依据：《资金管控系统开发任务文档（小微版）》v1.82 M25 里程碑 T25.1~T25.7
-- 前置依赖：M15_01（role_template）、M15_06（sys_menu 7000~7499）、
--           M15_07/M15_10（micro/micro_lite/micro_pro 角色模板）、
--           M1_05（feature_flag / biz_bundle）、M24_01（FEAT_CHECK_IN 参考模式）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. 新建 complaint_record 表（投诉记录，租户级）
--   2. 新建 compliance_agreement 表（合规协议签署，租户级）
--   3. feature_flag 新增第 41 项 FEAT_COMPLAINT（组 E=小微版专属，micro_enabled=1）
--   4. biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_COMPLAINT
--   5. sys_menu 新增 7260~7271（投诉监管目录 + Boss PC + platform Admin PC + F 按钮）
--      + 7520/7521（合规协议守卫 F 按钮，挂 7500 租户管理菜单）
--   6. role_template：platform_admin 追加 micro:complianceAgreement:view/sign（T25.5 签署守卫）；
--      boss(micro/micro_pro) 追加 micro:complaint:view/handle；
--      ★micro_lite boss 也追加（投诉为全版本基本能力）
--      ★v1.1 修正：监管权限 system:supervision:* 仅平台 Admin（超管 role_id=1）持有，
--        不入任何 role_template 配方（v1.0 误授 platform_admin，存量由 M25_04 清理）
--   7. 存量 sys_role_menu 补录（platform_admin 仅补 7520/7521；监管菜单仅补 role_id=1）
-- 规范：
--   1. 建表使用 CREATE TABLE IF NOT EXISTS 语义（先删后建仅限本脚本自身创建的表）
--   2. ALTER/UPDATE 使用 INFORMATION_SCHEMA 条件判断 + SQL_SAFE_UPDATES 包裹
--   3. 先删后插幂等（菜单/角色关联），JSON_CONTAINS 守卫（模板追加）
--   4. ★投诉功能全版本可用（micro_pro + micro_lite 均启用）
--   5. ★渠道一（BOSS）无额外门槛；渠道二（PLATFORM）必须拍照取证+文字≥20字
--   6. ★活动留痕数据租户不可删除，仅平台 Admin 可管理
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 新建 complaint_record 表（投诉记录，租户级）
-- ----------------------------
DROP TABLE IF EXISTS `complaint_record`;
CREATE TABLE `complaint_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `activity_id` BIGINT DEFAULT NULL COMMENT '关联活动ID（FK→promotion_activity.id，可空=不关联活动）',
  `staff_id` BIGINT DEFAULT NULL COMMENT '被投诉员工 user_id（可空=不指定员工）',
  `staff_name` VARCHAR(50) DEFAULT NULL COMMENT '被投诉员工姓名（冗余快照）',
  `complainant_phone` VARCHAR(20) DEFAULT NULL COMMENT '投诉人手机号',
  `complainant_name` VARCHAR(50) DEFAULT NULL COMMENT '投诉人姓名',
  `complaint_channel` VARCHAR(10) NOT NULL COMMENT '投诉渠道：BOSS=渠道一（Boss 门店内部）/ PLATFORM=渠道二（平台 Admin 违法违规）',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '投诉类别（渠道二必填：黄赌毒/违法宣传/违规活动/其他）',
  `content_text` TEXT NOT NULL COMMENT '投诉文字描述（渠道二要求≥20字）',
  `photo_urls` VARCHAR(2000) DEFAULT NULL COMMENT '附加图片URL列表（JSON 数组，渠道一可选≤3张，渠道二不使用此字段）',
  `evidence_photos` VARCHAR(2000) DEFAULT NULL COMMENT '取证照片URL列表（JSON 数组，渠道二必填≥1张，仅摄像头拍摄）',
  `status` VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待处理 / PROCESSING=处理中 / RESOLVED=已解决 / CLOSED=已关闭',
  `handler_reply` TEXT DEFAULT NULL COMMENT '处理回复（Boss/平台 Admin 填写）',
  `handler_user_id` BIGINT DEFAULT NULL COMMENT '处理人 user_id',
  `handler_name` VARCHAR(50) DEFAULT NULL COMMENT '处理人姓名（冗余快照）',
  `submit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_channel` (`tenant_id`, `complaint_channel`),
  INDEX `idx_tenant_status` (`tenant_id`, `status`),
  INDEX `idx_activity` (`activity_id`),
  INDEX `idx_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉记录表（T25.1 顾客双渠道投诉：BOSS 门店内部 / PLATFORM 平台 Admin 违法违规）';

-- ----------------------------
-- 2. 新建 compliance_agreement 表（合规协议签署，租户级）
--    UNIQUE(tenant_id, agreement_version) 防重复签署
-- ----------------------------
DROP TABLE IF EXISTS `compliance_agreement`;
CREATE TABLE `compliance_agreement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `agreement_version` VARCHAR(20) NOT NULL COMMENT '协议版本号（如 v1.0，平台方管理）',
  `agreement_content_url` VARCHAR(500) DEFAULT NULL COMMENT '协议内容URL/OSS路径',
  `signer_user_id` BIGINT NOT NULL COMMENT '签署人 user_id',
  `signer_name` VARCHAR(50) DEFAULT NULL COMMENT '签署人姓名（冗余快照）',
  `sign_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签署时间',
  `sign_ip` VARCHAR(50) DEFAULT NULL COMMENT '签署时IP地址',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_version` (`tenant_id`, `agreement_version`),
  INDEX `idx_tenant` (`tenant_id`),
  INDEX `idx_sign_time` (`sign_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规协议签署表（T25.5 租户 Admin 首次登录强制签署，协议版本更新仅新租户需重签）';

-- ----------------------------
-- 3. feature_flag 新增第 41 项 FEAT_COMPLAINT（组 E=小微版专属）
--    ★micro_enabled=1；basic/advanced/pro 三列均 0（企业版不生效）
--    ★投诉为基本能力，micro_pro + micro_lite 均启用
-- ----------------------------
DELETE FROM `feature_flag` WHERE `code` = 'FEAT_COMPLAINT';
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `micro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('FEAT_COMPLAINT', '投诉与平台监管(双渠道投诉+合规协议+停止租户+活动留痕)', 'E', NULL, 0, 0, 0, 1, '0', 'admin', NOW(),
 'T25.7；★仅小微版启用；micro_pro+micro_lite 均启用（投诉为基本能力）；渠道一→Boss/渠道二→平台 Admin');

-- ----------------------------
-- 4. biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_COMPLAINT
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;

UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_COMPLAINT')
WHERE `feature_flags` IS NOT NULL
  AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_COMPLAINT"');

SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 5. sys_menu 投诉与监管菜单（7260~7271）+ 合规协议守卫 F 按钮（7520/7521）
--    ★7260 为 M 目录，挂 1300 资金平台管理菜单（★超管专属监管目录）
--    ★7261~7262 挂 7100 小微运营（Boss PC 投诉管理；★v1.2 修正：v1.0 挂 7260 导致
--      M 祖先补全沿 7261→7260→1300 把平台目录拖入 boss 角色，破坏平台菜单隔离，存量由 M25_05 清理）
--    ★7265~7271 挂 7260（platform Admin PC 监管台，★超管专属，禁授租户管理员 platform_admin）
--    ★7520/7521 挂 7500（T25.5 登录签署守卫权限，platform_admin 配方见 §6.1）
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (7260, 7261, 7262, 7265, 7266, 7267, 7268, 7270, 7271, 7520, 7521);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7260: 投诉监管（M 目录，挂 1300 资金平台管理菜单。★不能用 7000，7000 是 C 页面不能有子菜单）
(7260, '投诉监管', 1300, 75, '', NULL, 1, 0, 'M', '0', '0', '', 'pc', 'email', 'admin', NOW(), 'T25.7 平台监管目录（超管专属：监管台/合规协议/活动留痕）'),
-- 7261: 投诉列表（C 类型页面，Boss PC；★v1.2 改挂 7100 小微运营，门店侧菜单禁挂平台目录）
(7261, '投诉列表', 7100, 20, 'complaint', 'micro/complaint/index', 1, 0, 'C', '0', '0', 'micro:complaint:view', 'pc', 'list', 'admin', NOW(), 'T25.4 Boss PC 查看/处理投诉（渠道一→Boss 门店内部）'),
-- 7262: 投诉处理（F 按钮）
(7262, '处理投诉', 7261, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:complaint:handle', 'pc', '#', 'admin', NOW(), 'T25.4 Boss 回复/处理投诉'),
-- 7265: 平台监管台（C 类型页面，platform Admin PC，★超管专属）
(7265, '平台监管台', 7260, 2, 'supervision', 'system/supervision/index', 1, 0, 'C', '0', '0', 'system:supervision:view', 'pc', 'monitor', 'admin', NOW(), 'T25.4 platform Admin 监管台（查看全部投诉+停止活动/租户+活动留痕）'),
-- 7266: 停止活动（F 按钮）
(7266, '停止活动', 7265, 1, '', NULL, 1, 0, 'F', '0', '0', 'system:supervision:stopActivity', 'pc', '#', 'admin', NOW(), 'T25.4 平台 Admin 停止活动（置为已停止，顾客端不再展示）'),
-- 7267: 停止租户（F 按钮）
(7267, '停止租户', 7265, 2, '', NULL, 1, 0, 'F', '0', '0', 'system:supervision:stopTenant', 'pc', '#', 'admin', NOW(), 'T25.6 平台 Admin 停止租户（全部账号无法登录）'),
-- 7268: 发放 license（F 按钮）
(7268, '发放License', 7265, 3, '', NULL, 1, 0, 'F', '0', '0', 'system:supervision:issueLicense', 'pc', '#', 'admin', NOW(), 'T25.4 平台 Admin 发放新 license'),
-- 7270: 合规协议管理（C 类型页面，platform Admin PC，★超管专属）
(7270, '合规协议', 7260, 3, 'agreement', 'system/supervision/agreement', 1, 0, 'C', '0', '0', 'system:supervision:agreement', 'pc', 'documentation', 'admin', NOW(), 'T25.5 platform Admin 合规协议管理（版本管理+签署记录查看）'),
-- 7271: 活动留痕（C 类型页面，platform Admin PC，★超管专属）
(7271, '活动留痕', 7260, 4, 'activityTrace', 'system/supervision/activityTrace', 1, 0, 'C', '0', '0', 'system:supervision:activityTrace', 'pc', 'log', 'admin', NOW(), 'T25.6 platform Admin 活动留痕查询（租户不可删除，强制保留）'),
-- 7520: 合规协议查看（F 按钮，T25.5 登录守卫查询当前协议与签署状态）
(7520, '合规协议查看', 7500, 5, '', NULL, 1, 0, 'F', '0', '0', 'micro:complianceAgreement:view', 'pc', '#', 'admin', NOW(), 'T25.5 登录守卫：查询当前协议版本与本租户签署状态'),
-- 7521: 合规协议签署（F 按钮，T25.5 租户 Admin 首次登录强制签署弹窗）
(7521, '合规协议签署', 7500, 6, '', NULL, 1, 0, 'F', '0', '0', 'micro:complianceAgreement:sign', 'pc', '#', 'admin', NOW(), 'T25.5 租户 Admin 首次登录强制签署弹窗');

-- ----------------------------
-- 6. role_template 追加权限
--    ★platform_admin（micro/micro_pro/micro_lite）追加 micro:complianceAgreement:view/sign
--    ★监管权限 system:supervision:* 为平台 Admin（超管 role_id=1）专属，不入配方
--    ★boss（micro/micro_pro + micro_lite）追加 micro:complaint:view/handle
--    ★投诉为全版本基本能力，micro_lite 也追加 boss 投诉权限
-- ----------------------------
-- 6.1 platform_admin 追加合规协议守卫 2 权限键（T25.5 登录签署守卫）
--   ★v1.1 修正：v1.0 此处误追加 system:supervision:* 6 键，已删除；存量由 M25_04 清理
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:complianceAgreement:view')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:complianceAgreement:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:complianceAgreement:sign')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:complianceAgreement:sign"');

-- 6.2 boss 追加 micro:complaint:view/handle（★含 micro_lite 免费版，投诉为基本能力）
-- micro_pro boss
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:complaint:view')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:complaint:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:complaint:handle')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:complaint:handle"');

-- micro (旧档) boss
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:complaint:view')
WHERE `version_type` = 'micro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:complaint:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:complaint:handle')
WHERE `version_type` = 'micro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:complaint:handle"');

-- ★micro_lite boss 也追加（投诉为基本能力，免费版可用）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:complaint:view')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:complaint:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:complaint:handle')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:complaint:handle"');

-- ----------------------------
-- 7. 存量 sys_role_menu 补录
-- ----------------------------
-- 7.1 platform_admin（小微版全版本档）：仅补合规协议守卫 F 按钮
--   ★v1.1 修正：v1.0 此处误授监管菜单（7260~7271）与 7000，已删除；存量由 M25_04 清理
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
  AND m.`menu_id` IN (7520, 7521);

-- 7.2 boss（小微版 micro_pro / micro / micro_lite，★投诉为全版本基本能力）
--   ★v1.2 修正：v1.0 误授平台目录 7260/7000，已删除；门店侧仅授 7100 目录 + 7261/7262，存量由 M25_05 清理
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND m.`menu_id` IN (7100, 7261, 7262);

-- 7.3 超管 role_id=1 全量补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7260, 7261, 7262, 7265, 7266, 7267, 7268, 7270, 7271);

-- ----------------------------
-- 8. sys_config 初始化合规协议配置键（compliance.agreement.current）
--    ★平台 Admin 通过 PC 监管台上传 OSS 后动态更新，此处仅建键占位（value 为空 JSON）
-- ----------------------------
INSERT IGNORE INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
VALUES ('合规协议当前版本配置', 'compliance.agreement.current', '{"version":"","contentUrl":""}', 'Y', 'admin', NOW(),
        'JSON格式：{"version":"v1.0","contentUrl":"https://oss.../agreement_v1.0.html"}；平台 Admin 通过监管台上传 OSS 后更新');

-- ----------------------------
-- 9. 验收查询（执行后人工核对）
-- ----------------------------
-- 8.1 complaint_record / compliance_agreement 两表已创建
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('complaint_record','compliance_agreement');
-- 预期：2 行
--
-- 8.2 FEAT_COMPLAINT 落表且 micro_enabled=1
-- SELECT code, flag_group, micro_enabled, basic_enabled, status FROM feature_flag WHERE code = 'FEAT_COMPLAINT';
-- 预期：1 行，E / micro_enabled=1 / basic_enabled=0 / status=0
--
-- 8.3 菜单 7260~7271 存在
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu
--  WHERE menu_id IN (7260,7261,7262,7265,7266,7267,7268,7270,7271);
-- 预期：9 行（M/C/F/C/F/F/F/C/C）
--
-- 8.4 role_template platform_admin 含合规协议守卫权限且不含监管权限
-- SELECT JSON_CONTAINS(menu_keys,'"micro:complianceAgreement:view"')  AS has_view,
--        JSON_CONTAINS(menu_keys,'"micro:complianceAgreement:sign"')  AS has_sign,
--        JSON_CONTAINS(menu_keys,'"system:supervision:view"')         AS bad_supervision
--   FROM role_template WHERE role_code = 'platform_admin' AND status = '0';
-- 预期：has_view/has_sign 均 1，bad_supervision 均 0
--
-- 8.5 role_template boss 含投诉权限（含 micro_lite）
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:complaint:view"')   AS has_view,
--        JSON_CONTAINS(menu_keys,'"micro:complaint:handle"') AS has_handle
--   FROM role_template WHERE role_code = 'boss' AND version_type IN ('micro','micro_pro','micro_lite');
-- 预期：三档均 1/1
--
-- ★执行后请重启后端，清除内存缓存 + 退出重新登录刷新权限快照
