-- =====================================================================
-- M13_02 企微通讯录同步菜单脚本（T13.2 补充）
-- 版本：v1.0  日期：2026-09-06
-- 依据：《资金管控系统开发任务文档》v1.12 M13（访问通道）
--       《多业态SaaS平台需求规格说明书》v1.0 §4.1（企微自建应用）
-- 前置依赖：
--   M13_01_access_channel.sql（wecom_sync_log 表已建；通道与集成目录 3480 已登记）
--   M1_05_platform_foundation_tables.sql（feature_flag 含 FEAT_WECOM）
-- 规范：
--   1. 先删后插，脚本可重复执行（幂等）
--   2. menu_id 占用 3490~3492（M13_01 使用 3480~3489，本脚本续用 3490+）
--   3. 企微同步为【高级版+】（FEAT_WECOM），仅 ADVANCED/PROFESSIONAL 模板登记
--   4. 角色授权：boss(9201)/manager(9203) 可触发同步与查看日志
-- =====================================================================

-- ----------------------------
-- 1. 菜单：企微同步（挂在「通道与集成」目录 3480 下）
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 3490 AND 3492;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
-- 页面：企微同步（T13.2）
(3490, '企微同步', 3480, 3, 'wecom', 'biz/wecom/index', NULL, 1, 0, 'C', '0', '0', 'biz:wecom:list', 'peoples', 'admin', NOW(), '', NULL, '企微通讯录同步（部门/成员），FEAT_WECOM 高级版+'),
-- 按钮权限
(3491, '同步触发', 3490, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:wecom:sync', '#', 'admin', NOW(), '', NULL, '触发 dept/user/all 同步'),
(3492, '同步日志', 3490, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:wecom:list', '#', 'admin', NOW(), '', NULL, '查看同步日志（与页面权限相同，保留按钮粒度）');

-- ----------------------------
-- 2. 版本授权模板登记（高级版+，FEAT_WECOM）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` BETWEEN 3490 AND 3492;
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('ADVANCED', 3490), ('ADVANCED', 3491), ('ADVANCED', 3492),
('PROFESSIONAL', 3490), ('PROFESSIONAL', 3491), ('PROFESSIONAL', 3492);

-- ----------------------------
-- 3. 角色授权（boss 9201 / manager 9203 可操作企微同步）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 3490 AND 3492 AND `role_id` IN (9201, 9203);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9201, 3490), (9201, 3491), (9201, 3492),
(9203, 3490), (9203, 3491), (9203, 3492);

-- ----------------------------
-- 4. 「角色×版本」授权模板登记（克隆角色授权唯一依据）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` BETWEEN 3490 AND 3492
  AND `edition` IN ('BOSS_ADVANCED','BOSS_PROFESSIONAL',
                    'MANAGER_ADVANCED','MANAGER_PROFESSIONAL');
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT CONCAT(UPPER(r.`role_key`), '_', e.`edition`), rm.`menu_id`
  FROM `sys_role` r
  JOIN `sys_role_menu` rm ON rm.`role_id` = r.`role_id`
  CROSS JOIN (SELECT 'ADVANCED' AS edition UNION ALL SELECT 'PROFESSIONAL') e
 WHERE r.`tenant_id` IS NULL
   AND r.`role_key` IN ('boss', 'manager')
   AND rm.`menu_id` BETWEEN 3490 AND 3492;

-- ----------------------------
-- 5. 存量租户克隆角色菜单对齐（幂等）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, tp.`menu_id`
  FROM `sys_role` r
  JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
  JOIN `sys_edition_menu_template` tp
    ON tp.`edition` = CONCAT(UPPER(r.`role_key`), '_', UPPER(t.`edition`))
 WHERE r.`tenant_id` IS NOT NULL
   AND r.`role_key` IN ('boss', 'manager')
   AND t.`edition` IN ('ADVANCED', 'PROFESSIONAL')
   AND tp.`menu_id` BETWEEN 3490 AND 3492;

-- ----------------------------
-- 6. 验收查询（执行后人工核对）
-- ----------------------------
-- 6.1 菜单落库
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3490 AND 3492 ORDER BY menu_id;
-- 6.2 版本模板登记（预期 ADVANCED/PROFESSIONAL 各 3 行 + BOSS_/MANAGER_ 各 6 行）
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template WHERE menu_id BETWEEN 3490 AND 3492 GROUP BY edition;
-- 6.3 角色授权
-- SELECT role_id, COUNT(*) FROM sys_role_menu WHERE menu_id BETWEEN 3490 AND 3492 GROUP BY role_id;
