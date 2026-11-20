-- =====================================================================
-- M20_05  micro_pro 业态包能力开关补全 + 推广分销/固定支出菜单重构（v1.63 新增）
-- 版本：v1.0  日期：2026-09-27
-- 依据：用户反馈「① 菜单不正确；② 当前为小微版付费版(micro_pro)，不应再出现升级拦截页面」
--       + 《五角色菜单骨架_V3.7_最终完整版》§三 老板菜单
-- ---------------------------------------------------------------------
-- 根因（升级拦截弹窗「当前版本不支持此功能，请升级License」）：
--   LicenseServiceImpl.getTenantActiveFeatures() 三维交集 = License ∩ feature_flag ∩ biz_bundle。
--   biz_bundle 三个业态包(boardgame/fresh_food/fund_control)的 feature_flags 只声明了企业版 FEAT_*，
--   缺小微版 FEAT_PROMOTION_DISTRIBUTION / FEAT_RATING / FEAT_SENSITIVE_WORD / FEAT_IMAGE_COMPLIANCE，
--   导致 applyBizFilter 第三维剔除 → @RequireFeature 抛「请升级License」。
--   （License 与 feature_flag 两维均正常，故根因唯一在 biz_bundle。）
-- ---------------------------------------------------------------------
-- 菜单缺陷（对照 V3.7 §三 老板菜单）：
--   1. 7101 命名「经营概览」→ V3.7 要求「经营驾驶舱」（老板首页）。
--   2. 推广分销子树 7181(data)/7182(attribution)/7183(stat)/7186(firstOrder)/7187(code)
--      指向不存在的 Vue 组件（死链）；而真实交付页面 salesman/records、customer/records 无菜单。
--      → 按 V3.7「推广管理 = 拉新奖励配置 + 推广数据看板(拉新排行榜/推广奖励发放记录)」重构。
--   3. 固定支出管理 7195~7199 在 sys_menu 完全缺失（M15_21 定义但库中不存在，M20_04 补录空转）。
--      → 重建并挂到 7190「数据与报表」下（V3.7 数据与报表含固定支出管理）。
--   4. boss 缺一批 F 按钮授权（按钮被 v-hasPermi 隐藏）。
-- ---------------------------------------------------------------------
-- 前置依赖：M20_01（7190 数据与报表目录）、M20_04（7180 类型/role_template 39 键）
-- ★执行前请设置 PowerShell 编码：$OutputEncoding = [System.Text.Encoding]::UTF8
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照 + 前端重新拉取路由
-- ★幂等：UPDATE + DELETE/INSERT + INSERT IGNORE，可重复执行
-- =====================================================================

SET NAMES utf8mb4;

-- ─── 1. 【根因】biz_bundle 三业态包补齐小微版 FEAT_* 能力开关 ──────────
-- 每个 flag 一条 UPDATE，覆盖三个业态包；NOT JSON_CONTAINS 保证幂等（已存在则跳过）
UPDATE biz_bundle
SET feature_flags = JSON_ARRAY_APPEND(feature_flags, '$', 'FEAT_PROMOTION_DISTRIBUTION')
WHERE biz_code IN ('boardgame', 'fresh_food', 'fund_control')
  AND feature_flags IS NOT NULL
  AND NOT JSON_CONTAINS(feature_flags, '"FEAT_PROMOTION_DISTRIBUTION"');

UPDATE biz_bundle
SET feature_flags = JSON_ARRAY_APPEND(feature_flags, '$', 'FEAT_RATING')
WHERE biz_code IN ('boardgame', 'fresh_food', 'fund_control')
  AND feature_flags IS NOT NULL
  AND NOT JSON_CONTAINS(feature_flags, '"FEAT_RATING"');

UPDATE biz_bundle
SET feature_flags = JSON_ARRAY_APPEND(feature_flags, '$', 'FEAT_SENSITIVE_WORD')
WHERE biz_code IN ('boardgame', 'fresh_food', 'fund_control')
  AND feature_flags IS NOT NULL
  AND NOT JSON_CONTAINS(feature_flags, '"FEAT_SENSITIVE_WORD"');

UPDATE biz_bundle
SET feature_flags = JSON_ARRAY_APPEND(feature_flags, '$', 'FEAT_IMAGE_COMPLIANCE')
WHERE biz_code IN ('boardgame', 'fresh_food', 'fund_control')
  AND feature_flags IS NOT NULL
  AND NOT JSON_CONTAINS(feature_flags, '"FEAT_IMAGE_COMPLIANCE"');

-- ─── 2. 7101 改名「经营驾驶舱」（V3.7 §三 老板首页）───────────────────
UPDATE sys_menu SET menu_name = '经营驾驶舱', remark = '★M20_05 V3.7 老板首页：经营概览→经营驾驶舱'
WHERE menu_id = 7101;

-- ─── 3. 推广分销子树重构（对齐 V3.7「推广管理」）──────────────────────
-- 3a. 7180 由 C 概览页改回 M 目录「推广管理」（作为容器，index.vue 下移至 7181 看板）
UPDATE sys_menu SET
    menu_name = '推广管理',
    menu_type = 'M',
    component = NULL,
    path      = 'distribution',
    icon      = 'share',
    remark    = '★M20_05 V3.7 推广管理目录（拉新奖励配置 + 推广数据看板）'
WHERE menu_id = 7180;

-- 3b. 7181 死链(data) → 推广数据看板（复用 index.vue 概览页）
UPDATE sys_menu SET
    menu_name = '推广数据看板',
    parent_id = 7180,
    order_num = 1,
    path      = 'dashboard',
    component = 'micro/distribution/index',
    perms     = 'micro:distribution:stat',
    icon      = 'chart',
    remark    = '★M20_05 推广数据看板（拉新排行/统计概览，原死链 data 重构）'
WHERE menu_id = 7181;

-- 3c. 7182 死链(attribution) → 业务员奖励记录（真实页面 salesman/records）
UPDATE sys_menu SET
    menu_name = '业务员奖励记录',
    parent_id = 7180,
    order_num = 2,
    path      = 'salesmanRecords',
    component = 'micro/distribution/salesman/records',
    perms     = 'micro:distribution:salesman',
    icon      = 'user',
    remark    = '★M20_05 业务员推广奖励发放记录（渠道=salesman）'
WHERE menu_id = 7182;

-- 3d. 7183 死链(stat) → 顾客奖励记录（真实页面 customer/records）
UPDATE sys_menu SET
    menu_name = '顾客奖励记录',
    parent_id = 7180,
    order_num = 3,
    path      = 'customerRecords',
    component = 'micro/distribution/customer/records',
    perms     = 'micro:distribution:customer',
    icon      = 'peoples',
    remark    = '★M20_05 顾客推广奖励发放记录（渠道=customer）'
WHERE menu_id = 7183;

-- 3e. 7184 拉新奖励配置：归位到 7180 下并调整排序（组件已存在，仅校准层级）
UPDATE sys_menu SET parent_id = 7180, order_num = 4 WHERE menu_id = 7184;

-- 3f. 删除死链菜单（组件不存在的 firstOrderConfig/code + 其子按钮 toggleCode）
DELETE FROM sys_role_menu WHERE menu_id IN (7186, 7187, 7188);
DELETE FROM sys_menu      WHERE menu_id IN (7186, 7187, 7188);

-- ─── 4. 重建固定支出管理菜单 7195~7199（挂 7190 数据与报表，V3.7）──────
-- 先删后插幂等；组件 micro/cost/index 已交付
DELETE FROM sys_role_menu WHERE menu_id IN (7195, 7196, 7197, 7198, 7199);
DELETE FROM sys_menu      WHERE menu_id IN (7195, 7196, 7197, 7198, 7199);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, client_type, icon, create_by, create_time, remark) VALUES
(7195, '固定支出管理', 7190, 5,  'cost',   NULL,                 NULL, 1, 0, 'M', '0', '0', '',                     'pc', 'money', 'admin', NOW(), '★M20_05 数据与报表>固定支出管理目录（房租/水电/工资等）'),
(7196, '固定支出列表', 7195, 1,  'index',  'micro/cost/index',   NULL, 1, 0, 'C', '0', '0', 'micro:pro:cost:list',  'pc', 'list',  'admin', NOW(), '固定支出列表页，含总支出概览+分项列表+启停开关'),
(7197, '固定支出新增', 7195, 2,  '',       '',                   NULL, 1, 0, 'F', '0', '0', 'micro:pro:cost:add',   'pc', '#',     'admin', NOW(), '新增固定支出项'),
(7198, '固定支出修改', 7195, 3,  '',       '',                   NULL, 1, 0, 'F', '0', '0', 'micro:pro:cost:edit',  'pc', '#',     'admin', NOW(), '修改固定支出项'),
(7199, '固定支出删除', 7195, 4,  '',       '',                   NULL, 1, 0, 'F', '0', '0', 'micro:pro:cost:remove','pc', '#',     'admin', NOW(), '删除固定支出项');

-- 超管 role_id=1 授权固定支出
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 7195), (1, 7196), (1, 7197), (1, 7198), (1, 7199);

-- ─── 5. role_template：micro_pro boss menu_keys 调整 ──────────────────
-- 移除死链键(data/attribution/firstOrderConfig/code)，保留 stat/rewardConfig，
-- 新增 salesman/customer（对应重构后的 7182/7183），固定支出键保留。共 37 键。
UPDATE role_template SET menu_keys = JSON_ARRAY(
    -- === 经营驾驶舱 ===
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
    -- === 活动管理 ===
    'micro:promo:list', 'micro:promo:add', 'micro:promo:edit', 'micro:promo:remove', 'micro:promo:poster',
    'micro:activity:setting:list', 'micro:myPromo:view',
    -- === 推广管理（★M20_05 重构：看板/业务员记录/顾客记录/拉新奖励配置）===
    'micro:distribution:stat', 'micro:distribution:salesman',
    'micro:distribution:customer', 'micro:distribution:rewardConfig',
    -- === 固定支出 ===
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
remark = 'micro_pro 老板 V3.7 M20_05修正（推广管理重构+固定支出重建，37权限键）'
WHERE version_type = 'micro_pro' AND role_code = 'boss';

-- ─── 6. 存量 micro_pro 租户 boss 克隆角色：sys_role_menu 补录 ─────────
-- 6a. 补录重构后的推广管理子树 + 固定支出菜单（M/C/F 全量）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
CROSS JOIN (
    SELECT 7180 AS menu_id UNION ALL SELECT 7181 UNION ALL SELECT 7182 UNION ALL
    SELECT 7183 UNION ALL SELECT 7184 UNION ALL SELECT 7185 UNION ALL
    SELECT 7195 UNION ALL SELECT 7196 UNION ALL SELECT 7197 UNION ALL
    SELECT 7198 UNION ALL SELECT 7199
) m
WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_pro';

-- 6b. 通用补授权：boss 已授权菜单(7100~7199)下的所有 F 子按钮
--     （修复 7112~7115/7122~7125/7132/7134/7157/7158/7161~7164/7171/7172/7185 等按钮被隐藏）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, f.menu_id
FROM sys_role_menu rm
JOIN sys_role r   ON r.role_id = rm.role_id AND r.role_key = 'boss' AND r.del_flag = '0'
JOIN sys_tenant t ON t.id = r.tenant_id AND t.tenant_type = 'micro' AND t.version_type = 'micro_pro'
JOIN sys_menu f   ON f.parent_id = rm.menu_id AND f.menu_type = 'F'
WHERE rm.menu_id BETWEEN 7100 AND 7199;

-- ─── 7. 验收查询（执行后人工核对） ─────────────────────────────────
-- 7.1 三业态包均含 4 个 micro flag（预期每行 4 个 1）
-- SELECT biz_code,
--   JSON_CONTAINS(feature_flags,'"FEAT_PROMOTION_DISTRIBUTION"') AS f_promo,
--   JSON_CONTAINS(feature_flags,'"FEAT_RATING"')                 AS f_rating,
--   JSON_CONTAINS(feature_flags,'"FEAT_SENSITIVE_WORD"')         AS f_word,
--   JSON_CONTAINS(feature_flags,'"FEAT_IMAGE_COMPLIANCE"')       AS f_image
-- FROM biz_bundle WHERE biz_code IN ('boardgame','fresh_food','fund_control');
--
-- 7.2 7101 已改名（预期：经营驾驶舱）
-- SELECT menu_id, menu_name FROM sys_menu WHERE menu_id = 7101;
--
-- 7.3 推广管理子树（预期：7180=M；7181/7182/7183/7184=C 且 component 均存在；无 7186/7187/7188）
-- SELECT menu_id, parent_id, menu_name, menu_type, path, component, perms
-- FROM sys_menu WHERE menu_id BETWEEN 7180 AND 7188 ORDER BY menu_id;
--
-- 7.4 固定支出菜单（预期：5 行 7195~7199）
-- SELECT menu_id, parent_id, menu_name, menu_type, component, perms
-- FROM sys_menu WHERE menu_id BETWEEN 7195 AND 7199 ORDER BY menu_id;
--
-- 7.5 role_template micro_pro boss 权限键数（预期：37，含 salesman/customer，不含 data/code）
-- SELECT JSON_LENGTH(menu_keys) AS cnt,
--   JSON_CONTAINS(menu_keys,'"micro:distribution:salesman"') AS has_salesman,
--   JSON_CONTAINS(menu_keys,'"micro:distribution:data"')     AS has_dead_data
-- FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
--
-- 7.6 boss 无残留死链菜单关联（预期：0 行）
-- SELECT rm.* FROM sys_role_menu rm
-- JOIN sys_role r ON r.role_id=rm.role_id AND r.role_key='boss'
-- JOIN sys_tenant t ON t.id=r.tenant_id AND t.version_type='micro_pro'
-- WHERE rm.menu_id IN (7186,7187,7188);
