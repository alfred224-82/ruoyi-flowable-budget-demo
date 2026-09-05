-- =====================================================================
-- M6_03 承接 M5 下移能力的菜单迁移脚本（T6.8 ②：菜单从基础版模板迁至高级版）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》T6.8 / M5 下移能力→开关对照（设计文档 5.1 组 A）
-- 前置依赖：
--   1. M2_02_role_menu_templates.sql（角色×版本累计制模板已建）
--   2. M2_08_tenant_role_clone_migration.sql（租户级克隆角色已授权）
-- 下移能力（组 A 开关，接口层由 @RequireFeature 拦截，本脚本仅收口菜单授权）：
--   FEAT_HIDDEN_COST    隐形成本 → 3204 分成/3205 资产折旧/3211 薪资社保/3212 折旧规则/3213 贷款利率/3214
--   FEAT_BAD_DEBT       烂账     → 3208
--   FEAT_VOUCHER_EXPORT 凭证导出 → 3209
--   FEAT_BOSS_DASHBOARD 老板看板 → 3215
--   FEAT_LINK_RULE      项目关联规则 → 3216
-- 规范：
--   1. 先确保 *_ADVANCED/*_PROFESSIONAL 已含下移菜单（累计制副本，从 *_BASIC 快照晋升），
--      再从 *_BASIC 移除——即使本脚本先于/后于 M2_02 重跑，高版本模板都不丢菜单；
--   2. 仅回收"非高级/专业版"租户克隆角色与共享预览角色（9201~9205）的下移菜单，
--      高级版/专业版租户菜单不变（降级保护 0.2：存量数据保留、仅关闭基础版入口）；
--   3. 全程数据驱动，不硬编码"角色→菜单"映射，避免越权；脚本可重复执行。
-- =====================================================================

-- ----------------------------
-- 0. 下移菜单集合（父菜单 + 对应按钮，与 M2_02 模板实际存在的 menu_id 一致）
--    3214/3274/3290 为文档区间占位，若 sys_menu 未定义则相关语句自然无匹配行
-- ----------------------------
DROP TEMPORARY TABLE IF EXISTS `tmp_downshift_menu`;
CREATE TEMPORARY TABLE `tmp_downshift_menu` (`menu_id` BIGINT NOT NULL PRIMARY KEY);
INSERT INTO `tmp_downshift_menu` (`menu_id`) VALUES
-- 父菜单
(3204), (3205), (3208), (3209), (3211), (3212), (3213), (3214), (3215), (3216),
-- 分成管理按钮（3204）
(3262), (3263),
-- 资产折旧按钮（3205）
(3264), (3265),
-- 烂账按钮（3208）
(3273), (3274), (3275),
-- 凭证导出按钮（3209）
(3276), (3277),
-- 薪资社保按钮（3211）
(3280), (3281),
-- 折旧规则按钮（3212）
(3282), (3283),
-- 贷款利率按钮（3213）
(3284), (3285),
-- 老板看板按钮（3215）
(3288),
-- 项目关联规则按钮（3216）
(3289), (3290);

-- =====================================================================
-- 1. 晋升：确保 *_ADVANCED / *_PROFESSIONAL 已包含各角色 *_BASIC 当前持有的下移菜单
--    数据驱动——按角色前缀从 *_BASIC 快照复制，只晋升该角色本就拥有的菜单，杜绝越权
--    （INSERT IGNORE 命中主键 (edition, menu_id) 时跳过，保证幂等）
-- =====================================================================
INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT CONCAT(SUBSTRING_INDEX(b.`edition`, '_BASIC', 1), '_ADVANCED'), b.`menu_id`
FROM `sys_edition_menu_template` b
JOIN `tmp_downshift_menu` d ON d.`menu_id` = b.`menu_id`
WHERE b.`edition` LIKE '%\_BASIC';

INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT CONCAT(SUBSTRING_INDEX(b.`edition`, '_BASIC', 1), '_PROFESSIONAL'), b.`menu_id`
FROM `sys_edition_menu_template` b
JOIN `tmp_downshift_menu` d ON d.`menu_id` = b.`menu_id`
WHERE b.`edition` LIKE '%\_BASIC';

-- =====================================================================
-- 2. 移除：从所有 *_BASIC 模板删除下移菜单（基础版仅保留轻量入口，能力走 @RequireFeature 拦截）
-- =====================================================================
DELETE b FROM `sys_edition_menu_template` b
JOIN `tmp_downshift_menu` d ON d.`menu_id` = b.`menu_id`
WHERE b.`edition` LIKE '%\_BASIC';

-- =====================================================================
-- 3. 回收：存量"非高级/专业版"租户克隆角色的下移菜单授权（sys_role_menu）
--    仅高级版/专业版租户保留；基础版/未知版本租户回收（防串第④层 + 降级保护）
-- =====================================================================
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
JOIN `tmp_downshift_menu` d ON d.`menu_id` = rm.`menu_id`
WHERE r.`tenant_id` IS NOT NULL
  AND r.`del_flag` = '0'
  AND UPPER(IFNULL(t.`edition`, 'BASIC')) NOT IN ('ADVANCED', 'ADV', 'PROFESSIONAL', 'PRO', 'ENTERPRISE');

-- ----------------------------
-- 3.1 共享预览角色（9201~9205，镜像 *_BASIC 模板，tenant_id 为空）同步回收
-- ----------------------------
DELETE rm FROM `sys_role_menu` rm
JOIN `tmp_downshift_menu` d ON d.`menu_id` = rm.`menu_id`
WHERE rm.`role_id` IN (9201, 9202, 9203, 9204, 9205);

DROP TEMPORARY TABLE IF EXISTS `tmp_downshift_menu`;

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 1) 基础版模板不应再含任何下移菜单（预期 0）
-- SELECT COUNT(*) FROM sys_edition_menu_template
--  WHERE edition LIKE '%\_BASIC'
--    AND menu_id IN (3204,3205,3208,3209,3211,3212,3213,3214,3215,3216,
--                    3262,3263,3264,3265,3273,3274,3275,3276,3277,
--                    3280,3281,3282,3283,3284,3285,3288,3289,3290);
-- 2) 财务高级版模板应保留下移菜单（预期 > 0，如 3204/3208/3209/3211~3213/3216）
-- SELECT menu_id FROM sys_edition_menu_template
--  WHERE edition = 'FINANCE_ADVANCED'
--    AND menu_id IN (3204,3205,3208,3209,3211,3212,3213,3216) ORDER BY menu_id;
-- 3) 老板高级版模板应保留看板菜单（预期含 3215,3288）
-- SELECT menu_id FROM sys_edition_menu_template
--  WHERE edition = 'BOSS_ADVANCED' AND menu_id IN (3215,3288) ORDER BY menu_id;
-- 4) 基础版租户克隆角色不应再持有下移菜单（预期 0）
-- SELECT COUNT(*) FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id
--   JOIN sys_tenant t ON t.id = r.tenant_id
--  WHERE r.tenant_id IS NOT NULL
--    AND UPPER(IFNULL(t.edition,'BASIC')) NOT IN ('ADVANCED','ADV','PROFESSIONAL','PRO','ENTERPRISE')
--    AND rm.menu_id IN (3204,3205,3208,3209,3211,3212,3213,3214,3215,3216,
--                       3262,3263,3264,3265,3273,3274,3275,3276,3277,
--                       3280,3281,3282,3283,3284,3285,3288,3289,3290);
