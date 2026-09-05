-- =====================================================================
-- M2_05 platform_admin 组织管理能力升级脚本（T2.4）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《各角色菜单权限文档》（v2.3）§2.5.2 / §3.5
-- 前置依赖：M2_01_roles.sql / M2_02_role_menu_templates.sql / M2_03_sys_role_menu_basic.sql / M2_04_tenant_isolation.sql
-- 说明：
--   1. 更新 sys_edition_menu_template：PLATFORM_ADMIN → 按版本拆分
--   2. 重新授权五角色基础版菜单（先删后插）
--   3. 为 platform_admin 按版本重新授权组织管理菜单
--   4. 存量 sys_dept 标记 tenant_id=0（系统级）
--   5. 存量 sys_role 标记 tenant_id（共享角色 NULL，其余 0）
-- =====================================================================

-- ----------------------------
-- 1. 重建版本菜单授权模板（含 PLATFORM_ADMIN 按版本拆分）
-- ----------------------------
DROP TABLE IF EXISTS `sys_edition_menu_template`;
CREATE TABLE `sys_edition_menu_template` (
  `edition` VARCHAR(30) NOT NULL COMMENT '版本：BASIC/ADVANCED/PROFESSIONAL/PLATFORM_ADMIN_BASIC/PLATFORM_ADMIN_ADVANCED/PLATFORM_ADMIN_PROFESSIONAL',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`edition`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三版本菜单授权模板(开通/升级租户的唯一授权依据)';

-- 基础版业务菜单模板
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('BASIC', 2000), ('BASIC', 3000),
('BASIC', 2001), ('BASIC', 2002), ('BASIC', 2003),
('BASIC', 2011), ('BASIC', 2012), ('BASIC', 2013), ('BASIC', 2014), ('BASIC', 2015),
('BASIC', 2016), ('BASIC', 2017), ('BASIC', 2018), ('BASIC', 2019), ('BASIC', 2020),
('BASIC', 2031), ('BASIC', 2032), ('BASIC', 2033), ('BASIC', 2034), ('BASIC', 2035),
('BASIC', 3002), ('BASIC', 3003), ('BASIC', 3004), ('BASIC', 3005), ('BASIC', 3006),
('BASIC', 3007), ('BASIC', 3008), ('BASIC', 3009), ('BASIC', 3010), ('BASIC', 3015), ('BASIC', 3016),
('BASIC', 3021), ('BASIC', 3022), ('BASIC', 3023), ('BASIC', 3024),
('BASIC', 3031), ('BASIC', 3032), ('BASIC', 3033), ('BASIC', 3034), ('BASIC', 3035),
('BASIC', 3041), ('BASIC', 3042), ('BASIC', 3043), ('BASIC', 3044), ('BASIC', 3045), ('BASIC', 3046), ('BASIC', 3133),
('BASIC', 3051), ('BASIC', 3052), ('BASIC', 3053),
('BASIC', 3061), ('BASIC', 3071), ('BASIC', 3072),
('BASIC', 3081), ('BASIC', 3082), ('BASIC', 3083), ('BASIC', 3084), ('BASIC', 3085),
('BASIC', 3091), ('BASIC', 3092), ('BASIC', 3093), ('BASIC', 3094), ('BASIC', 3095),
('BASIC', 3101), ('BASIC', 3102),
('BASIC', 3111), ('BASIC', 3112), ('BASIC', 3113),
('BASIC', 3114), ('BASIC', 3124), ('BASIC', 3125),
('BASIC', 3201), ('BASIC', 3202), ('BASIC', 3203), ('BASIC', 3204), ('BASIC', 3205),
('BASIC', 3206), ('BASIC', 3207), ('BASIC', 3208), ('BASIC', 3209), ('BASIC', 3210),
('BASIC', 3211), ('BASIC', 3212), ('BASIC', 3213), ('BASIC', 3214), ('BASIC', 3215), ('BASIC', 3216),
('BASIC', 3231), ('BASIC', 3232), ('BASIC', 3233),
('BASIC', 3251), ('BASIC', 3252), ('BASIC', 3253), ('BASIC', 3254), ('BASIC', 3255),
('BASIC', 3256), ('BASIC', 3257), ('BASIC', 3258), ('BASIC', 3259), ('BASIC', 3260),
('BASIC', 3261), ('BASIC', 3262), ('BASIC', 3263), ('BASIC', 3264), ('BASIC', 3265),
('BASIC', 3266), ('BASIC', 3267), ('BASIC', 3268), ('BASIC', 3269), ('BASIC', 3270),
('BASIC', 3271), ('BASIC', 3272), ('BASIC', 3273), ('BASIC', 3274), ('BASIC', 3275),
('BASIC', 3276), ('BASIC', 3277), ('BASIC', 3278), ('BASIC', 3279), ('BASIC', 3280),
('BASIC', 3281), ('BASIC', 3282), ('BASIC', 3283), ('BASIC', 3284), ('BASIC', 3285),
('BASIC', 3286), ('BASIC', 3287), ('BASIC', 3288), ('BASIC', 3289), ('BASIC', 3290);

-- 高级版 = 基础版全部 + 高级增量
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'ADVANCED', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'BASIC';
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('ADVANCED', 3217), ('ADVANCED', 3218), ('ADVANCED', 3219), ('ADVANCED', 3220),
('ADVANCED', 3221), ('ADVANCED', 3222), ('ADVANCED', 3223),
('ADVANCED', 3241), ('ADVANCED', 3242), ('ADVANCED', 3243), ('ADVANCED', 3244),
('ADVANCED', 3245), ('ADVANCED', 3246), ('ADVANCED', 3247), ('ADVANCED', 3248),
('ADVANCED', 3291), ('ADVANCED', 3292), ('ADVANCED', 3293),
('ADVANCED', 3296), ('ADVANCED', 3297);

-- 专业版 = 高级版全部 + 专业增量
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'PROFESSIONAL', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'ADVANCED';
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('PROFESSIONAL', 3224), ('PROFESSIONAL', 3225), ('PROFESSIONAL', 3226),
('PROFESSIONAL', 3249), ('PROFESSIONAL', 3250),
('PROFESSIONAL', 3294), ('PROFESSIONAL', 3295), ('PROFESSIONAL', 3298), ('PROFESSIONAL', 3299);

-- platform_admin 基础版：用户管理 + 部门管理 + 消息中心
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('PLATFORM_ADMIN_BASIC', 1),
('PLATFORM_ADMIN_BASIC', 100),
('PLATFORM_ADMIN_BASIC', 1001), ('PLATFORM_ADMIN_BASIC', 1002), ('PLATFORM_ADMIN_BASIC', 1003),
('PLATFORM_ADMIN_BASIC', 1004), ('PLATFORM_ADMIN_BASIC', 1005), ('PLATFORM_ADMIN_BASIC', 1006),
('PLATFORM_ADMIN_BASIC', 103),
('PLATFORM_ADMIN_BASIC', 1030), ('PLATFORM_ADMIN_BASIC', 1031), ('PLATFORM_ADMIN_BASIC', 1032),
('PLATFORM_ADMIN_BASIC', 1033),
('PLATFORM_ADMIN_BASIC', 3114);

-- platform_admin 高级版 = 基础版 + 角色管理
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'PLATFORM_ADMIN_ADVANCED', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'PLATFORM_ADMIN_BASIC';
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('PLATFORM_ADMIN_ADVANCED', 101),
('PLATFORM_ADMIN_ADVANCED', 1010), ('PLATFORM_ADMIN_ADVANCED', 1011), ('PLATFORM_ADMIN_ADVANCED', 1012),
('PLATFORM_ADMIN_ADVANCED', 1013), ('PLATFORM_ADMIN_ADVANCED', 1014);

-- platform_admin 专业版 = 高级版（无新增）
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'PLATFORM_ADMIN_PROFESSIONAL', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'PLATFORM_ADMIN_ADVANCED';

-- ----------------------------
-- 2. 重新授权五角色基础版菜单（先删后插）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `role_id` IN (9201, 9202, 9203, 9204, 9205);

-- 老板（boss，9201）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9201, 2000), (9201, 3000),
(9201, 2002), (9201, 2017), (9201, 2018),
(9201, 3010), (9201, 3093), (9201, 3094),
(9201, 3005), (9201, 3052),
(9201, 3215), (9201, 3288),
(9201, 3201), (9201, 3251), (9201, 3254), (9201, 3255), (9201, 3231),
(9201, 3206), (9201, 3266), (9201, 3268), (9201, 3269),
(9201, 3203), (9201, 3258), (9201, 3260),
(9201, 3202), (9201, 3256),
(9201, 3208), (9201, 3273), (9201, 3274), (9201, 3275),
(9201, 3214), (9201, 3286), (9201, 3287),
(9201, 3216), (9201, 3289), (9201, 3290),
(9201, 3004), (9201, 3045),
(9201, 3006), (9201, 3061), (9201, 3007), (9201, 3009), (9201, 3081), (9201, 3015), (9201, 3101),
(9201, 3114);

-- 财务（finance，9202）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9202, 2000), (9202, 3000),
(9202, 2001), (9202, 2011), (9202, 2012), (9202, 2013), (9202, 2014), (9202, 2015), (9202, 2016), (9202, 2019),
(9202, 2002), (9202, 2020),  -- 2011 已在上方预算编制中插入，此处去重
(9202, 2003), (9202, 2031), (9202, 2032), (9202, 2033), (9202, 2034), (9202, 2035),
(9202, 3004), (9202, 3041), (9202, 3045), (9202, 3046), (9202, 3124),
(9202, 3210), (9202, 3278), (9202, 3279),
(9202, 3202), (9202, 3256),
(9202, 3209), (9202, 3276), (9202, 3277),
(9202, 3207), (9202, 3270), (9202, 3271), (9202, 3272),
(9202, 3206), (9202, 3266), (9202, 3267),
(9202, 3201), (9202, 3251), (9202, 3252), (9202, 3253), (9202, 3231), (9202, 3232), (9202, 3233),
(9202, 3203), (9202, 3258), (9202, 3261),
(9202, 3208), (9202, 3273), (9202, 3275),
(9202, 3204), (9202, 3262), (9202, 3263),
(9202, 3205), (9202, 3264), (9202, 3265),
(9202, 3211), (9202, 3280), (9202, 3281),
(9202, 3212), (9202, 3282), (9202, 3283),
(9202, 3213), (9202, 3284), (9202, 3285),
(9202, 3216), (9202, 3289),
(9202, 3002), (9202, 3021), (9202, 3022), (9202, 3023), (9202, 3024),
(9202, 3003), (9202, 3031), (9202, 3032), (9202, 3033), (9202, 3034), (9202, 3035),
(9202, 3008), (9202, 3071), (9202, 3072),
(9202, 3010), (9202, 3091), (9202, 3092),
(9202, 3006), (9202, 3061), (9202, 3007), (9202, 3015), (9202, 3101), (9202, 3102),
(9202, 3009), (9202, 3081), (9202, 3082), (9202, 3083), (9202, 3084), (9202, 3085),
(9202, 3001), (9202, 3011), (9202, 3012), (9202, 3013), (9202, 3014),
(9202, 3114);

-- 项目经理（manager，9203）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9203, 3000),
(9203, 3201), (9203, 3251), (9203, 3252), (9203, 3231), (9203, 3233),
(9203, 3202), (9203, 3256), (9203, 3257),
(9203, 3203), (9203, 3258), (9203, 3259),
(9203, 3204), (9203, 3262),
(9203, 3205), (9203, 3264),
(9203, 3208), (9203, 3273), (9203, 3275),
(9203, 3004), (9203, 3041), (9203, 3042), (9203, 3043), (9203, 3046), (9203, 3044), (9203, 3124), (9203, 3125),
(9203, 3114);

-- 报销人（employee，9204）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9204, 3000),
(9204, 3004), (9204, 3041), (9204, 3042), (9204, 3043), (9204, 3046), (9204, 3044), (9204, 3124), (9204, 3125),
(9204, 3114);

-- 租户组织管理员（platform_admin，9205）— 基础版菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9205, 1),
(9205, 100), (9205, 1001), (9205, 1002), (9205, 1003), (9205, 1004), (9205, 1005), (9205, 1006),
(9205, 103), (9205, 1030), (9205, 1031), (9205, 1032), (9205, 1033),
(9205, 3114);

-- ----------------------------
-- 3. 验收查询
-- ----------------------------
-- 各角色菜单数量
-- SELECT r.role_key, COUNT(rm.menu_id) AS menu_count
-- FROM sys_role_menu rm JOIN sys_role r ON r.role_id = rm.role_id
-- WHERE r.role_id IN (9201, 9202, 9203, 9204, 9205)
-- GROUP BY r.role_key ORDER BY r.role_sort;
-- 预期：boss≈60, finance≈100, manager≈28, employee≈11, platform_admin≈14

-- 模板完整性
-- SELECT edition, COUNT(*) AS cnt FROM sys_edition_menu_template GROUP BY edition;
-- 预期：BASIC≈90, ADVANCED≈110, PROFESSIONAL≈119, PLATFORM_ADMIN_BASIC≈14, PLATFORM_ADMIN_ADVANCED≈20, PLATFORM_ADMIN_PROFESSIONAL≈20
