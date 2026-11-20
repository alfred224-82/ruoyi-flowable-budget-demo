-- =====================================================================
-- M20_04  micro_pro boss 权限补全 + 推广分销菜单类型修正（v1.62 新增）
-- 版本：v1.0  日期：2026-09-26
-- 依据：用户反馈 4 项缺陷修复
--   1. 活动管理页面无「发布活动」按钮（role_template 缺 micro:promo:add/edit/remove/poster）
--   2. 推广分销管理点击无反应（菜单 7180 为 M 目录无 component，改为 C 指向概览页）
--   3. 固定支出菜单不可见（role_template 缺 micro:pro:cost:list/add/edit/remove）
--   4. 评价管理报 license 错误（Java 层 LicenseServiceImpl 已修复，本脚本不涉及）
-- 前置依赖：M20_01（boss_pro_menu_fix）、M15_21（固定支出菜单 7195~7199）、
--           M16_01（推广分销菜单 7180~7188）、M15_06（活动菜单 7150~7155）
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;

-- ─── 1. role_template：micro_pro boss menu_keys 追加缺失权限 ────────
-- M20_01 写了 31 个权限键，遗漏了活动 CRUD（4 个）和固定支出（4 个），共 8 个
-- 追加后总计 39 个权限键
UPDATE role_template SET menu_keys = JSON_ARRAY(
    -- === 经营概览 ===
    'micro:dashboard:view',
    -- === 预约管理 ===
    'micro:booking:list', 'micro:order:detail',
    -- === 人员与结算 ===
    'micro:settle:list', 'micro:settle:history',
    -- === 收支记账 ===
    'micro:income:list', 'micro:expense:list', 'micro:budgetAlert:edit',
    -- === 报表中心 ===
    'micro:report:weekly', 'micro:report:monthly', 'micro:report:profit',
    'micro:report:export', 'micro:report:cashFlow',
    -- === 活动管理（★M20_04 补入 CRUD + 海报）===
    'micro:promo:list', 'micro:promo:add', 'micro:promo:edit', 'micro:promo:remove', 'micro:promo:poster',
    'micro:activity:setting:list', 'micro:myPromo:view',
    -- === 推广分销 ===
    'micro:distribution:data', 'micro:distribution:attribution',
    'micro:distribution:stat', 'micro:distribution:rewardConfig',
    'micro:distribution:firstOrderConfig', 'micro:distribution:code',
    -- === 固定支出（★M20_04 补入）===
    'micro:pro:cost:list', 'micro:pro:cost:add', 'micro:pro:cost:edit', 'micro:pro:cost:remove',
    -- === 评价管理 ===
    'micro:rating:list',
    -- === 顾客管理 ===
    'micro:customer:list',
    -- === 合规 ===
    'micro:compliance:records', 'micro:boss:complianceHit:list',
    -- === 异常处理 ===
    'micro:exception:list',
    -- === 员工业绩榜 ===
    'micro:staffRanking:list',
    -- === OSS ===
    'system:oss:upload', 'system:oss:query', 'system:oss:remove'
),
remark = 'micro_pro 老板 V3.7 M20_04修正（补活动CRUD+固定支出，39权限键）'
WHERE version_type = 'micro_pro' AND role_code = 'boss';

-- ─── 2. 菜单 7180 从 M 目录改为 C 菜单（推广分销概览页）─────────────
-- M16_01 将 7180 建为 M 类型目录（component=NULL），点击仅展开/折叠子菜单，
-- 但 distribution/index.vue 已交付为概览页，应作为 C 类型可直接导航
UPDATE sys_menu SET
    menu_type = 'C',
    component = 'micro/distribution/index',
    path      = 'distribution',
    remark    = '★M20_04 改为 C 类型：推广分销概览页（含业务员/顾客推广入口+统计概览）'
WHERE menu_id = 7180;

-- ─── 3. 存量 micro_pro 租户 boss 克隆角色：补录缺失菜单关联 ────────
-- 3a. 按 role_template 新 menu_keys 匹配 sys_menu.perms → 补写 C + F 类型
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
JOIN role_template rt ON rt.version_type = t.version_type AND rt.role_code = r.role_key AND rt.status = '0'
CROSS JOIN JSON_TABLE(rt.menu_keys, '$[*]' COLUMNS (perm VARCHAR(64) PATH '$')) AS j
JOIN sys_menu m ON m.perms = j.perm
WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_pro'
  AND m.menu_type IN ('C', 'F')
  AND m.menu_id IN (
      7152, 7153, 7154, 7155,   -- 活动 CRUD 按钮
      7196, 7197, 7198, 7199    -- 固定支出 列表+CRUD 按钮
  );

-- 3b. 7180 改为 C 类型后需确保 boss 有权限访问
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 7180 AS menu_id
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_pro';

-- 3c. 固定支出 M 目录（7195）显式补入（M 类型无 perms，无法通过 perms 匹配）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 7195 AS menu_id
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_pro';

-- ─── 4. 验收查询（执行后人工核对） ─────────────────────────────────
-- 4.1 role_template 权限键数
-- SELECT JSON_LENGTH(menu_keys) FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：39
--
-- 4.2 role_template 包含活动 CRUD
-- SELECT
--   JSON_CONTAINS(menu_keys, '"micro:promo:add"')    AS has_promo_add,
--   JSON_CONTAINS(menu_keys, '"micro:promo:edit"')   AS has_promo_edit,
--   JSON_CONTAINS(menu_keys, '"micro:promo:remove"') AS has_promo_remove,
--   JSON_CONTAINS(menu_keys, '"micro:promo:poster"') AS has_promo_poster
-- FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：全部 1
--
-- 4.3 role_template 包含固定支出
-- SELECT
--   JSON_CONTAINS(menu_keys, '"micro:pro:cost:list"') AS has_cost_list,
--   JSON_CONTAINS(menu_keys, '"micro:pro:cost:add"')  AS has_cost_add,
--   JSON_CONTAINS(menu_keys, '"micro:pro:cost:edit"') AS has_cost_edit,
--   JSON_CONTAINS(menu_keys, '"micro:pro:cost:remove"') AS has_cost_remove
-- FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：全部 1
--
-- 4.4 菜单 7180 已改为 C 类型
-- SELECT menu_id, menu_name, menu_type, component FROM sys_menu WHERE menu_id = 7180;
-- 预期：menu_type='C', component='micro/distribution/index'
--
-- 4.5 boss sys_role_menu 含活动按钮
-- SELECT t.company_name, GROUP_CONCAT(m.menu_id ORDER BY m.menu_id) AS promo_btns
-- FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='boss' AND r.del_flag='0'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id
--   JOIN sys_menu m ON m.menu_id=rm.menu_id
-- WHERE t.tenant_type='micro' AND t.version_type='micro_pro'
--   AND m.menu_id IN (7152,7153,7154,7155)
-- GROUP BY t.company_name;
-- 预期：每租户含 7152,7153,7154,7155
--
-- 4.6 boss sys_role_menu 含固定支出
-- SELECT t.company_name, GROUP_CONCAT(m.menu_id ORDER BY m.menu_id) AS cost_menus
-- FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='boss' AND r.del_flag='0'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id
--   JOIN sys_menu m ON m.menu_id=rm.menu_id
-- WHERE t.tenant_type='micro' AND t.version_type='micro_pro'
--   AND m.menu_id IN (7195,7196,7197,7198,7199)
-- GROUP BY t.company_name;
-- 预期：每租户含 7195,7196,7197,7198,7199
--
-- 4.7 boss 菜单总数 >= 43
-- SELECT t.company_name, COUNT(rm.menu_id) AS cnt FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='boss' AND r.del_flag='0'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id
-- WHERE t.tenant_type='micro' AND t.version_type='micro_pro'
-- GROUP BY t.company_name;
-- 预期：cnt >= 43
