-- =====================================================================
-- M20_11  小微版租户 version_type 为 NULL 修复（v1.68，2026-10-02）
-- =====================================================================
-- 根因：部分小微版租户 sys_tenant.version_type 为 NULL（创建时未正确回填），导致：
--   ① 登录响应不含 versionType → 前端 isPro=false → 经营驾驶舱不渲染
--   ② copyMenuTemplateForMicro 查 role_template 配方时 versionType 不匹配
--      → sys_role_menu 缺 M/C 类型菜单 → menus=[]
-- 修复：
--   ① 批量补设 version_type = 'micro_pro'（tenant_type='micro' 且 version_type 为 NULL）
--   ② 通过 role_template.menu_keys → sys_menu.perms 补录 C/F 类型菜单
--   ③ 保底插入 M 类型根目录 7100（小微运营，parent_id=0）
--   ④ 补全中间 M 类型祖先目录（C 菜单的 parent 链中 menu_type='M' 的节点）
-- 幂等：UPDATE WHERE NULL 幂等；INSERT IGNORE 幂等
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

-- =================== 诊断 ===================
-- D1: 查看 version_type 为 NULL 的小微版租户
SELECT id, company_name, tenant_type, version_type, status
FROM sys_tenant
WHERE tenant_type = 'micro' AND (version_type IS NULL OR version_type = '');

-- D2: 这些租户的 boss 克隆角色 M/C 菜单数（预期 0 = 确认根因）
SELECT t.id, t.company_name, COUNT(DISTINCT m.menu_id) AS mc_count
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.menu_type IN ('M','C') AND m.status = '0'
WHERE t.tenant_type = 'micro' AND (t.version_type IS NULL OR t.version_type = '')
GROUP BY t.id, t.company_name;

-- D3: role_template micro_pro boss 配方存在性
SELECT JSON_LENGTH(menu_keys) AS key_count FROM role_template
WHERE version_type = 'micro_pro' AND role_code = 'boss' AND status = '0';

-- =================== 修复 ===================
-- F1: 批量补设 version_type（所有 tenant_type='micro' 且 version_type 为 NULL 的租户）
UPDATE sys_tenant
SET version_type = 'micro_pro'
WHERE tenant_type = 'micro' AND (version_type IS NULL OR version_type = '');

-- F2: 通过 role_template.menu_keys → sys_menu.perms 补录 C/F 类型菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
JOIN `sys_tenant` t ON t.id = r.tenant_id
JOIN `role_template` rt ON rt.`version_type` = 'micro_pro' AND rt.`role_code` = 'boss' AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON m.`perms` = j.`perm`
WHERE t.`tenant_type` = 'micro'
  AND r.`role_key` = 'boss' AND r.`del_flag` = '0';

-- F3: ★保底 M 类型根目录 7100（小微运营，parent_id=0）
--     所有 boss 菜单的祖先链最终指向 7100，getChildPerms(menus, 0) 依赖它作为根节点
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 7100
FROM `sys_role` r
JOIN `sys_tenant` t ON t.id = r.tenant_id
WHERE t.`tenant_type` = 'micro'
  AND r.`role_key` = 'boss' AND r.`del_flag` = '0';

-- F4: 补全中间 M 类型祖先目录（F2 匹配的 C 菜单的 parent 链中 menu_type='M' 的节点）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, anc.`menu_id`
FROM `sys_role` r
JOIN `sys_tenant` t ON t.id = r.tenant_id
CROSS JOIN (
    -- 一级祖先：C 菜单的 parent_id
    SELECT DISTINCT m.parent_id AS menu_id FROM sys_menu m
    WHERE m.menu_id IN (
        SELECT m2.menu_id FROM sys_menu m2
        JOIN `role_template` rt ON rt.`version_type` = 'micro_pro' AND rt.`role_code` = 'boss' AND rt.`status` = '0'
        CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
        ON m2.`perms` = j.`perm`
        WHERE m2.menu_type IN ('M','C')
    )
    AND m.parent_id != 0
    UNION
    -- 二级祖先：一级祖先的 parent_id
    SELECT DISTINCT m3.parent_id AS menu_id FROM sys_menu m3
    WHERE m3.menu_id IN (
        SELECT m4.parent_id FROM sys_menu m4
        WHERE m4.menu_id IN (
            SELECT m5.menu_id FROM sys_menu m5
            JOIN `role_template` rt ON rt.`version_type` = 'micro_pro' AND rt.`role_code` = 'boss' AND rt.`status` = '0'
            CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
            ON m5.`perms` = j.`perm`
            WHERE m5.menu_type IN ('M','C')
        )
        AND m4.parent_id != 0
    )
    AND m3.parent_id != 0
) anc
WHERE t.`tenant_type` = 'micro'
  AND r.`role_key` = 'boss' AND r.`del_flag` = '0';

-- =================== 验证 ===================
-- V1: 无 version_type 为 NULL 的小微版租户
SELECT COUNT(*) AS null_count FROM sys_tenant
WHERE tenant_type = 'micro' AND (version_type IS NULL OR version_type = '');
-- ★预期：0

-- V2: 每个 micro_pro boss 的 M/C 菜单数 > 0
SELECT t.id, t.company_name, t.version_type,
       COUNT(DISTINCT m.menu_id) AS mc_count
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
JOIN sys_role_menu rm ON rm.role_id = r.role_id
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE t.tenant_type = 'micro'
  AND m.menu_type IN ('M','C') AND m.status = '0'
GROUP BY t.id, t.company_name, t.version_type;
-- ★预期：每行 mc_count > 0

-- V3: 完整菜单树（抽查一个租户）
SELECT DISTINCT m.menu_id, m.menu_name, m.parent_id, m.menu_type, m.perms
FROM sys_role_menu rm
JOIN sys_role r ON r.role_id = rm.role_id
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE r.tenant_id = (SELECT id FROM sys_tenant WHERE tenant_type='micro' LIMIT 1)
  AND r.role_key = 'boss' AND r.del_flag = '0'
  AND m.menu_type IN ('M','C') AND m.status = '0'
ORDER BY m.parent_id, m.menu_id;
-- ★预期：7100(M) + 各 C 类型菜单
