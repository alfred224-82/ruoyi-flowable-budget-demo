-- =====================================================================
-- M20_12  biz_bundle.fund_control.menus 业态菜单声明修复（v1.69，2026-10-02）
-- =====================================================================
-- 根因：biz_bundle.fund_control.menus 包含企业版费用管控菜单 ID
--       ([2000,3000,3201,3202,3203,3206,3207,3210,3004])，
--       与小微版 7100+ 菜单无交集 → applyBizFilter 过滤异常 → menus 只返回极少量菜单。
-- 修复：fund_control 是小微版默认业态包，menus 置 NULL 跳过业态过滤（兼容存量设计），
--       菜单可见性完全由 sys_role_menu 控制。
-- 幂等：UPDATE 幂等
-- ★执行后需重启后端（清除 BizBundleService 内存缓存 menuCache）
-- =====================================================================

-- D1: 修复前确认
SELECT biz_code, menus, JSON_LENGTH(menus) AS cnt
FROM biz_bundle WHERE biz_code = 'fund_control';
-- ★预期：menus 包含企业版旧 ID [2000,3000,...]

-- F1: 置空 menus（跳过业态过滤，菜单可见性由 sys_role_menu 控制）
UPDATE biz_bundle SET menus = NULL WHERE biz_code = 'fund_control';

-- V1: 修复后确认
SELECT biz_code, menus FROM biz_bundle WHERE biz_code = 'fund_control';
-- ★预期：menus = NULL
