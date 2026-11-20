-- =====================================================================
-- M15_20 boss「预约失败通知」权限修复脚本（V3.8 状态设置中心补漏）
-- 版本：v1.0  日期：2026-10-10
-- 背景：/micro/notify/list 校验 micro:notify:list（sys_menu 7220，7200 目录子节点）。
--       现网 boss 角色调用报「无此权限：micro:notify:list」，原因有二：
--       ① 历史清理脚本曾按 7200 目录树整树删除租户 boss 角色授权（含 7220）；
--       ② micro_lite 档 role_template 配方仅含 micro:notify:config，未含 micro:notify:list，
--          而 V3.8 状态设置中心「预约失败通知」卡为全档位 boss 枢纽卡（通知数据按登录人隔离）。
-- 修复逻辑（两步，全部幂等）：
--   Step 1: micro_lite boss 配方补 micro:notify:list（与 notify:config 成套）
--   Step 2: 存量小微版租户 boss 角色（含共享预设 boss）按 perms 补写 sys_role_menu
-- 前置：M15_06（sys_menu 7220）、M15_07/M15_10（role_template）、M15_15（克隆角色）
-- ★执行后需重新登录小程序（menuPermission 在登录时写入 token 会话缓存）
-- =====================================================================

-- =====================================================================
-- Step 1: micro_lite boss 配方补 micro:notify:list（幂等：JSON_CONTAINS 判断）
-- =====================================================================
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:notify:list'),
    `update_by` = 'system-fix',
    `update_time` = NOW()
WHERE `version_type` = 'micro_lite'
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:notify:list"');

-- =====================================================================
-- Step 2: 存量小微版租户 boss 角色补写 sys_role_menu（幂等：NOT EXISTS）
--   范围：role_key='boss' 且未删除；租户克隆角色限 tenant_type='micro'，
--         共享预设角色（tenant_id IS NULL，M15_07 micro 配方本含该 perms）一并补齐
-- =====================================================================
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
JOIN `sys_menu` m
  ON m.`perms` = 'micro:notify:list'
 AND m.`status` = '0'
LEFT JOIN `sys_tenant` t
  ON t.`id` = r.`tenant_id`
WHERE r.`role_key` = 'boss'
  AND r.`del_flag` = '0'
  AND (r.`tenant_id` IS NULL OR t.`tenant_type` = 'micro')
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm
       WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`
  );

-- =====================================================================
-- 验收查询（执行后人工核对）
-- =====================================================================
-- A. lite 配方已含 notify:list
-- SELECT version_type, role_code, JSON_CONTAINS(menu_keys, '"micro:notify:list"') AS has_notify_list
--   FROM role_template WHERE version_type = 'micro_lite' AND role_code = 'boss';
-- 预期：1
--
-- B. 各小微版租户 boss 角色已授权 7220
-- SELECT t.id, t.company_name, t.version_type, r.role_id, m.menu_id, m.perms
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.perms = 'micro:notify:list'
--  WHERE t.tenant_type = 'micro'
--  ORDER BY t.id;
-- 预期：每个小微版租户 1 行
--
-- C. ★AIA 桌游店 boss 角色专项核对（历史清理脚本受影响角色）
-- SELECT r.role_id, r.role_name, m.menu_id, m.menu_name, m.perms
--   FROM sys_role r
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE r.role_key = 'boss' AND r.del_flag = '0'
--    AND m.perms IN ('micro:notify:list', 'micro:notify:config')
--  ORDER BY r.role_id, m.menu_id;
-- 预期：每个 boss 角色 notify:list / notify:config 各 1 行
--
-- D. 运行时验证：boss 重新登录小程序 → 状态设置中心「预约失败通知」卡不再报
--    「无此权限：micro:notify:list」，后端日志无 GlobalExceptionHandler 权限校验失败记录
