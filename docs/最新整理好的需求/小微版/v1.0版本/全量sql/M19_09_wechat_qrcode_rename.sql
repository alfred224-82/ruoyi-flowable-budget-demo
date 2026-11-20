-- =====================================================================
-- M19_09  「活动设置」改名为「微信推广码」+ 页面简化（v1.57 新增）
-- =====================================================================
-- 依据：用户需求——将「活动设置」页面改名为「微信推广码」，
--       页面只展示微信二维码，供手机扫码进入活动。
--
-- 需求概述：
--   ① 菜单 7156 改名：活动设置 → 微信推广码
--   ② 权限 perms 改名：micro:activity:setting:* → micro:wechat:qrcode:*
--   ③ 页面简化：只保留活动选择 + 二维码展示
--
-- 前置依赖：
--   [68]M19_07（活动设置菜单 7156~7158）
--
-- 幂等：UPDATE 操作天然幂等，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 菜单改名（7156 活动设置 → 微信推广码）
-- =====================================================================
UPDATE `sys_menu`
SET `menu_name` = '微信推广码',
    `path` = 'wechatQrcode',
    `component` = 'micro/activity/wechatQrcode/index',
    `icon` = 'qrcode',
    `remark` = '微信推广码页面：选择活动后展示微信二维码，供手机扫码进入'
WHERE `menu_id` = 7156;

-- =====================================================================
-- 2. 子菜单/按钮权限改名
-- =====================================================================
UPDATE `sys_menu`
SET `menu_name` = '查看推广码',
    `perms` = 'micro:wechat:qrcode:list',
    `remark` = '查看微信推广码'
WHERE `menu_id` = 7157;

UPDATE `sys_menu`
SET `menu_name` = '生成推广码',
    `perms` = 'micro:wechat:qrcode:edit',
    `remark` = '生成微信推广码'
WHERE `menu_id` = 7158;

-- =====================================================================
-- 3. role_template 权限更新（旧权限 → 新权限）
-- =====================================================================

-- 3.1 micro / micro_pro boss：替换权限
UPDATE `role_template`
SET `menu_keys` = JSON_REPLACE(
    JSON_REPLACE(`menu_keys`,
        '$[0]', 'micro:wechat:qrcode:list'),
    '$[1]', 'micro:wechat:qrcode:edit')
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'boss'
  AND JSON_CONTAINS(`menu_keys`, '"micro:activity:setting:list"');

-- 3.2 micro_lite boss：替换权限
UPDATE `role_template`
SET `menu_keys` = JSON_REPLACE(
    JSON_REPLACE(`menu_keys`,
        '$[0]', 'micro:wechat:qrcode:list'),
    '$[1]', 'micro:wechat:qrcode:edit')
WHERE `version_type` = 'micro_lite'
  AND `role_code` = 'boss'
  AND JSON_CONTAINS(`menu_keys`, '"micro:activity:setting:list"');

-- =====================================================================
-- 4. 存量租户 sys_role_menu 权限更新
-- =====================================================================

-- 4.1 将旧权限菜单 7157/7158 的 perms 更新为新权限（sys_menu 已更新，此处无需额外操作）
-- 4.2 确保所有已有 7156 的租户角色都能正常访问（菜单 ID 不变，只是名称和路径变了）

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 1. 菜单已改名
-- SELECT menu_id, menu_name, path, component, perms, icon
--   FROM sys_menu WHERE menu_id IN (7156, 7157, 7158);
-- 预期：
--   7156 | 微信推广码 | wechatQrcode | micro/activity/wechatQrcode/index | micro:wechat:qrcode:list | qrcode
--   7157 | 查看推广码 | # | # | micro:wechat:qrcode:list | #
--   7158 | 生成推广码 | # | # | micro:wechat:qrcode:edit | #
--
-- 2. role_template 权限已更新
-- SELECT version_type, role_code, menu_keys
--   FROM role_template
--  WHERE role_code = 'boss' AND version_type IN ('micro', 'micro_pro', 'micro_lite');
-- 预期：menu_keys 含 micro:wechat:qrcode:list 和 micro:wechat:qrcode:edit
--
