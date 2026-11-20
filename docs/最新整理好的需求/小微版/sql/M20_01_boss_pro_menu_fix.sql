-- =====================================================================
-- M20_01_boss_pro_menu_fix.sql  v3.0  (2026-09-26)
-- 修复 micro_pro boss 角色菜单：V3.7 §6.5 菜单骨架完整对齐
-- =====================================================================
-- ★执行前请设置 PowerShell 编码：$OutputEncoding = [System.Text.Encoding]::UTF8
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
-- 修复内容：
--   1. 补建缺失菜单（7159/7650/7698）
--   2. 新建「数据与报表」M 目录（7190），收纳 V3.7 数据报表类页面
--   3. 移动 7160/7170/7618/7650/7690 至 7190 下
--   4. role_template 31 权限键重写
--   5. sys_role_menu 清理 + 重授（含 M 目录显式补入）
-- =====================================================================

SET NAMES utf8mb4;

-- ─── 1. 补建缺失菜单（3 个 C 类型） ─────────────────────────────
DELETE FROM sys_role_menu WHERE menu_id IN (7159, 7650, 7698);
DELETE FROM sys_menu WHERE menu_id IN (7159, 7650, 7698);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, client_type, icon, create_by, remark)
VALUES (7159, '我的推广码', 7150, 20, 'myQrcode', 'micro/promo/myQrcode/index', 1, 0, 'C', '0', '0', 'micro:myPromo:view', 'pc', 'qrcode', 'admin', 'T19.6 Boss PC 我的推广码');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, client_type, icon, create_by, remark)
VALUES (7650, '员工业绩榜', 7190, 30, 'staffRanking', 'micro/staffRanking/index', 1, 0, 'C', '0', '0', 'micro:staffRanking:list', 'pc', 'chart', 'admin', 'T18.13 老板员工业绩排行');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, client_type, icon, create_by, remark)
VALUES (7698, '异常处理中心', 7100, 10, 'exceptionCenter', 'micro/exceptionCenter/index', 1, 0, 'C', '0', '0', 'micro:exception:list', 'pc', 'warning', 'admin', 'T18.8 业务异常处理中心');

-- ─── 2. 新建「数据与报表」M 目录（7190） ────────────────────────
DELETE FROM sys_role_menu WHERE menu_id = 7190;
DELETE FROM sys_menu WHERE menu_id = 7190;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, client_type, icon, create_by, remark)
VALUES (7190, '数据与报表', 7100, 40, 'report', NULL, 1, 0, 'M', '0', '0', '', 'pc', 'chart', 'admin', 'V3.7 数据与报表目录');

-- ─── 3. 移动散落 C 菜单至 7190 数据与报表 ───────────────────────
-- V3.7 §6.5 老板「数据与报表」：固定支出/合规记录/业绩榜/评价/顾客
UPDATE sys_menu SET parent_id = 7190, order_num = 10 WHERE menu_id = 7160;  -- 评价管理
UPDATE sys_menu SET parent_id = 7190, order_num = 20 WHERE menu_id = 7170;  -- 顾客管理
UPDATE sys_menu SET parent_id = 7190, order_num = 40 WHERE menu_id = 7618;  -- 语音合规记录
UPDATE sys_menu SET parent_id = 7190, order_num = 50 WHERE menu_id = 7690;  -- 合规命中记录

-- 收支记录(7130)与报表中心(7140)改为 7190 子目录
UPDATE sys_menu SET parent_id = 7190, order_num = 60 WHERE menu_id = 7130;  -- 收支记录（含固定支出）
UPDATE sys_menu SET parent_id = 7190, order_num = 70 WHERE menu_id = 7140;  -- 报表中心

-- ─── 4. role_template：micro_pro boss menu_keys 重写（31 个权限键） ─
UPDATE role_template SET menu_keys = JSON_ARRAY(
    'micro:dashboard:view',
    'micro:booking:list', 'micro:order:detail',
    'micro:settle:list', 'micro:settle:history',
    'micro:income:list', 'micro:expense:list', 'micro:budgetAlert:edit',
    'micro:report:weekly', 'micro:report:monthly', 'micro:report:profit',
    'micro:report:export', 'micro:report:cashFlow',
    'micro:promo:list', 'micro:activity:setting:list', 'micro:myPromo:view',
    'micro:distribution:data', 'micro:distribution:attribution',
    'micro:distribution:stat', 'micro:distribution:rewardConfig',
    'micro:distribution:firstOrderConfig', 'micro:distribution:code',
    'micro:rating:list',
    'micro:customer:list',
    'micro:compliance:records', 'micro:boss:complianceHit:list',
    'micro:exception:list',
    'micro:staffRanking:list',
    'system:oss:upload', 'system:oss:query', 'system:oss:remove'
),
remark = 'micro_pro 老板 V3.7 M20修正'
WHERE version_type = 'micro_pro' AND role_code = 'boss';

-- ─── 5. 存量 micro_pro 租户 boss 克隆角色：清理 + 重新授权 ──────
-- 5a. 清理 7000-7699 + 7190 区间的旧关联
DELETE rm FROM sys_role_menu rm
JOIN sys_role r ON r.role_id = rm.role_id
JOIN sys_tenant t ON t.id = r.tenant_id
WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_pro'
  AND r.role_key = 'boss' AND r.del_flag = '0'
  AND (rm.menu_id BETWEEN 7000 AND 7699 OR rm.menu_id = 7190);

-- 5b. 按 role_template menu_keys 匹配 sys_menu.perms → 写入 sys_role_menu（C + F 类型）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
JOIN role_template rt ON rt.version_type = t.version_type AND rt.role_code = r.role_key AND rt.status = '0'
CROSS JOIN JSON_TABLE(rt.menu_keys, '$[*]' COLUMNS (perm VARCHAR(64) PATH '$')) AS j
JOIN sys_menu m ON m.perms = j.perm
WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_pro' AND m.menu_type IN ('M', 'C', 'F');

-- 5c. 显式补入 M 类型目录菜单（JSON perms 为空字符串，无法通过 perms 匹配）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, dirs.menu_id
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
CROSS JOIN (
    SELECT 7100 AS menu_id UNION ALL
    SELECT 7110 UNION ALL
    SELECT 7120 UNION ALL
    SELECT 7130 UNION ALL
    SELECT 7140 UNION ALL
    SELECT 7150 UNION ALL
    SELECT 7180 UNION ALL
    SELECT 7190
) dirs
WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_pro';
