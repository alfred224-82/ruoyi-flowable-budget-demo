-- =====================================================================
-- M2_11 基础版存量租户克隆角色菜单对齐修复脚本（v1.12，T2.2 / 手册 v2.0 验收）
-- 版本：v1.0  日期：2026-09-06
-- 依据：《基础版操作手册》v2.0 1.2 验收检查 / 0.3 角色与审批（2 角色，boss 兼任财务/经理）
-- 背景：M2_02 v2.2 重排 BOSS_BASIC 模板（补收入确认 3202/收入核验 3210/收款单 3207/
--       报销管理 3004 精简，移除预算管理目录 2000/预算审核 2002/老板看板 3215），
--       M2_10 v1.1 将目录 2000 纳入下移集合；存量基础版租户的克隆角色 sys_role_menu
--       系开通时按旧模板复制，需对齐修复（页面表现：侧边栏残留空壳「预算管理」目录、
--       费用管控下缺收入确认/收款单/报销管理菜单）。
-- 前置：按序重放 [08]M2_02(v2.2) → [09]M2_03(v1.2) → [16]M2_10(v1.1)（均幂等），
--       再执行本脚本；新环境随索引执行即可（INSERT IGNORE + 定向 DELETE 幂等）。
-- 规范：
--   1. 角色保全：仅按 CONCAT(UPPER(role_key),'_BASIC') 模板补本角色菜单，不跨角色授权
--   2. 幂等：INSERT IGNORE（role_id+menu_id 主键去重）+ 定向 DELETE（重复执行影响 0 行）
--   3. 降级保护：仅回收/补齐菜单授权，业务数据全部保留不删
-- =====================================================================

-- ----------------------------
-- 1. 回收：基础版租户克隆角色仍残留的下移菜单（含预算管理空壳目录 2000）
-- ----------------------------
DELETE rm FROM `sys_role_menu` rm
  JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
  JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
 WHERE UPPER(t.`edition`) = 'BASIC'
   AND r.`tenant_id` IS NOT NULL
   AND rm.`menu_id` IN (
       2000,2001,2002,2003,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,
       2031,2032,2033,2034,2035,3310,3311,3312,3313,3314,3315,3316,3317,3318,3319,3320,3321,
       3322,3323,3324,3204,3205,3262,3263,3264,3265,3211,3212,3213,3214,3280,3281,3282,3283,
       3284,3285,3286,3287,3208,3273,3274,3275,3209,3276,3277,3215,3288,3216,3289,3290);

-- ----------------------------
-- 2. 补齐：基础版租户克隆角色（boss/employee）按当前 *_BASIC 模板复制授权
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, tp.`menu_id`
  FROM `sys_role` r
  JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
  JOIN `sys_edition_menu_template` tp
    ON tp.`edition` = CONCAT(UPPER(r.`role_key`), '_BASIC')
 WHERE UPPER(t.`edition`) = 'BASIC'
   AND r.`tenant_id` IS NOT NULL
   AND r.`role_key` IN ('boss', 'employee');

-- ----------------------------
-- 3. 验收查询
-- ----------------------------
-- 3.1 BOSS_BASIC 模板菜单数（预期 35）
-- SELECT COUNT(*) FROM sys_edition_menu_template WHERE edition = 'BOSS_BASIC';
--
-- 3.2 基础版租户克隆 boss 角色残留下移菜单/空壳目录数（预期 0）
-- SELECT COUNT(*) FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id
--   JOIN sys_tenant t ON t.id = r.tenant_id
--  WHERE UPPER(t.edition) = 'BASIC' AND r.tenant_id IS NOT NULL AND r.role_key = 'boss'
--    AND rm.menu_id IN (2000,2002,2017,2018,3215,3288);
--
-- 3.3 基础版租户克隆 boss 角色菜单数（预期 35，与模板一致）
-- SELECT r.role_id, r.role_key, COUNT(*) AS menu_count
--   FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id
--   JOIN sys_tenant t ON t.id = r.tenant_id
--  WHERE UPPER(t.edition) = 'BASIC' AND r.tenant_id IS NOT NULL AND r.role_key = 'boss'
--  GROUP BY r.role_id, r.role_key;
--
-- 3.4 侧边栏验收（对应手册 1.2）：基础版 boss 登录仅见费用管控目录下
--     项目管理/收入确认/收入核验/合同变更/付款单管理/收款单管理/报销管理（精简），
--     无「预算管理」空壳目录
