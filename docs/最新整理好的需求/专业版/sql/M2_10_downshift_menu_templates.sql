-- =====================================================================
-- M2_10 三版本授权模板下移重排脚本（T1.3 / T2.2，防串第④层，v1.12）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》v1.12 T1.3 / T2.2 / T6.8②、第五章用例 18/19/20、第六章附录速查表
--       《资金管控系统设计文档》v1.3 §1.7（版本功能矩阵重排）/ §2.2（基础版模块版本归属划分）/ §5.1 组 A
-- 前置依赖：M2_02_role_menu_templates.sql（角色×版本模板 BOSS_/FINANCE_/MANAGER_/EMPLOYEE_/PLATFORM_ADMIN_）
--           M2_05_platform_admin_upgrade.sql（版本全量模板 BASIC/ADVANCED/PROFESSIONAL）
--           M5_03~M5_09（费用大类/财务科目/大类预算/租户大类菜单 3310~3324）
-- 背景：v1.12「折中下移」——资金管控 11 类重能力由基础版下移至高级版。对应菜单须从
--       *_BASIC / BASIC 授权模板【迁出】，迁入 *_ADVANCED / *_PROFESSIONAL（防串第④层：低版本模板不含高版本菜单）。
-- 规范：
--   1. 数据驱动、角色保全：以「当前 *_BASIC 模板已含哪些下移菜单」为准，逐条 REPLACE 后缀晋升到同角色的
--      _ADVANCED / _PROFESSIONAL，避免跨角色越权授予（如老板不会被授予财务的预算编制编辑菜单）
--   2. 幂等：INSERT IGNORE（主键 edition+menu_id 去重）+ DELETE（重复执行影响 0 行）
--   3. 降级保护（设计文档 0.2）：仅回收菜单授权，业务数据全部保留不删
--   4. 本脚本仅整理授权模板与存量租户克隆角色的 sys_role_menu；接口层 @RequireFeature 由 T6.8（M6）落地，
--      即使模板误分配，②③ 层 API 拦截仍为防串底线
-- =====================================================================

-- ----------------------------
-- 1. 下移菜单集合（附录速查表 + 用例 20 权威枚举：预算/科目/大类域 + 隐形成本 + 烂账 + 凭证 + 看板 + 关联规则）
-- ----------------------------
DROP TEMPORARY TABLE IF EXISTS `tmp_downshift_menu`;
CREATE TEMPORARY TABLE `tmp_downshift_menu` (`menu_id` BIGINT PRIMARY KEY);
INSERT INTO `tmp_downshift_menu` (`menu_id`) VALUES
-- 预算编制 / 审核 / 科目（2001~2003 + 隐藏页 2019/2020 + 按钮 2011~2018、2031~2035）——FEAT_BUDGET_PREPARE / FEAT_SUBJECT_3LAYER
(2001),(2002),(2003),
(2011),(2012),(2013),(2014),(2015),(2016),(2017),(2018),(2019),(2020),
(2031),(2032),(2033),(2034),(2035),
-- 费用大类管理 / 大类预算设定 / 财务科目导入 / 拆分初稿 / 租户大类配置（3310~3324）——FEAT_SUBJECT_3LAYER / FEAT_BUDGET_PREPARE
(3310),(3311),(3312),(3313),(3314),(3315),
(3316),(3317),(3318),(3319),(3320),(3321),(3322),(3323),(3324),
-- 隐形成本：分成管理 3204 / 资产折旧 3205 + 按钮 3262~3265——FEAT_HIDDEN_COST
(3204),(3205),(3262),(3263),(3264),(3265),
-- 隐形成本配置：薪资社保 3211 / 折旧规则 3212 / 贷款利率 3213 / 分成规则 3214 + 按钮 3280~3287
(3211),(3212),(3213),(3214),(3280),(3281),(3282),(3283),(3284),(3285),(3286),(3287),
-- 烂账管理 3208 + 按钮 3273~3275——FEAT_BAD_DEBT
(3208),(3273),(3274),(3275),
-- 凭证导出 3209 + 按钮 3276/3277——FEAT_VOUCHER_EXPORT
(3209),(3276),(3277),
-- 老板看板 3215 + 按钮 3288——FEAT_BOSS_DASHBOARD
(3215),(3288),
-- 项目关联规则 3216 + 按钮 3289/3290——FEAT_LINK_RULE
(3216),(3289),(3290);

-- ----------------------------
-- 2. 晋升：角色×版本模板 *_BASIC → *_ADVANCED（角色保全，仅晋升该角色 BASIC 已有的下移菜单）
-- ----------------------------
INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT REPLACE(t.`edition`, '_BASIC', '_ADVANCED'), t.`menu_id`
  FROM `sys_edition_menu_template` t
  JOIN `tmp_downshift_menu` d ON d.`menu_id` = t.`menu_id`
 WHERE t.`edition` LIKE '%\_BASIC';

-- ----------------------------
-- 3. 晋升：角色×版本模板 *_BASIC → *_PROFESSIONAL
-- ----------------------------
INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT REPLACE(t.`edition`, '_BASIC', '_PROFESSIONAL'), t.`menu_id`
  FROM `sys_edition_menu_template` t
  JOIN `tmp_downshift_menu` d ON d.`menu_id` = t.`menu_id`
 WHERE t.`edition` LIKE '%\_BASIC';

-- ----------------------------
-- 4. 晋升：版本全量模板 BASIC → ADVANCED / PROFESSIONAL（M2_05 方案，兼容并存）
-- ----------------------------
INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'ADVANCED', t.`menu_id`
  FROM `sys_edition_menu_template` t
  JOIN `tmp_downshift_menu` d ON d.`menu_id` = t.`menu_id`
 WHERE t.`edition` = 'BASIC';

INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'PROFESSIONAL', t.`menu_id`
  FROM `sys_edition_menu_template` t
  JOIN `tmp_downshift_menu` d ON d.`menu_id` = t.`menu_id`
 WHERE t.`edition` = 'BASIC';

-- ----------------------------
-- 5. 迁出：从全部 BASIC 模板（版本全量 BASIC + 角色×版本 *_BASIC）删除下移菜单
--    注意：第 2~4 步晋升须先于本步执行（本脚本已按序编排）
-- ----------------------------
DELETE t FROM `sys_edition_menu_template` t
  JOIN `tmp_downshift_menu` d ON d.`menu_id` = t.`menu_id`
 WHERE t.`edition` = 'BASIC' OR t.`edition` LIKE '%\_BASIC';

-- ----------------------------
-- 6. 回收：存量【基础版】租户克隆角色的 sys_role_menu（降级保护——仅回收菜单授权，业务数据保留不删）
--    克隆角色识别：sys_role.tenant_id = 租户ID（非 NULL，区别于全局共享角色 9201~9205）
-- ----------------------------
DELETE rm FROM `sys_role_menu` rm
  JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
  JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
  JOIN `tmp_downshift_menu` d ON d.`menu_id` = rm.`menu_id`
 WHERE UPPER(t.`edition`) = 'BASIC';

DROP TEMPORARY TABLE IF EXISTS `tmp_downshift_menu`;

-- ----------------------------
-- 7. 验收查询（防串第④层：低版本模板不得包含任何下移菜单）
-- ----------------------------
-- 7.1 基础版模板残留下移菜单数（预期 0）
-- SELECT COUNT(*) FROM sys_edition_menu_template
--  WHERE (edition = 'BASIC' OR edition LIKE '%\_BASIC')
--    AND menu_id IN (2001,2002,2003,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,
--        2031,2032,2033,2034,2035,3310,3311,3312,3313,3314,3315,3316,3317,3318,3319,3320,3321,
--        3322,3323,3324,3204,3205,3262,3263,3264,3265,3211,3212,3213,3214,3280,3281,3282,3283,
--        3284,3285,3286,3287,3208,3273,3274,3275,3209,3276,3277,3215,3288,3216,3289,3290);
--
-- 7.2 高级版/专业版模板已含下移菜单（角色保全，预期 > 0）
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template
--  WHERE edition LIKE '%\_ADVANCED' AND menu_id IN (2001,2003,3208,3209,3215,3216,3204,3205)
--  GROUP BY edition;
--
-- 7.3 存量基础版租户克隆角色已回收下移菜单（预期 0）
-- SELECT COUNT(*) FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id
--   JOIN sys_tenant t ON t.id = r.tenant_id
--  WHERE UPPER(t.edition) = 'BASIC' AND rm.menu_id IN (2001,2003,3208,3209,3215,3216);
