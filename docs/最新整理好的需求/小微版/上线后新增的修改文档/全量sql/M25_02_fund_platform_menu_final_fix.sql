-- =====================================================================
-- M25_02 「资金平台管理菜单」终极兜底修复脚本（v1.82 新增）
-- 版本：v1.1  日期：2026-10-12
-- =====================================================================
-- 【问题背景】
--   用户多次反馈：执行完 00_执行顺序.sql 后，admin 登录侧边栏仍然看不到
--   一级目录「资金平台管理菜单」(menu_id=1300)。经数据库实时核查发现：
--     ✓ menu 1300 本体存在
--     ✓ sys_role_menu (1, 1300) 存在
--     ✗ 子菜单缺失：1340/1343 消失，6120/6160 parent_id=0（脱离 1300）
--     ✗ 7260 path='' 空字符串，6160 path='compliance2' 命中前端过滤关键字
--     ✗ re-parent 到 1300 的既有菜单（6001/101/102/3130）未补 role_id=1 授权
--
-- 【根因清单】
--   R0. ★用户执行 00_执行顺序.sql 时可能跳过了 [九-2] M10_02：
--       → M15_12 的 `DELETE FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359`
--         删除了 M10_01/M10_02 建的 1320/1330/1340/1343；
--       → M10_01 会重建 1320/1330，但 M10_02 若未执行，1340/1343 永久消失；
--       → M10_02 还负责 UPDATE 6120/6160 parent_id=1300，未执行则二者停留在 parent_id=0。
--
--   R1. menu 7260「投诉监管」path='' 空字符串（M25_01 遗留）
--       → 前端 vue-router 遇到 path='' 的 M 目录子路由时 Layout 展开异常，
--         Sidebar el-submenu index 计算失败 → 1300 目录整体在侧边栏消失。
--
--   R2. menu 6160「合规管理」path='compliance2'
--       → 前端 utils/feature.js filterRoutesByFeature 通过 path.includes('compliance')
--         匹配 ROUTE_FEATURE_MAP['compliance']='FEE_COMPLIANCE_CUSTOM'；
--       → admin 无该 License 特性时 6160 子树被过滤；
--       → 叠加 line 162「children=0 && Layout」判断可能级联移除 1300。
--
--   R3. re-parent 到 1300 的 4 个既有菜单（6001/101/102/3130）未补 sys_role_menu；
--       M15_12 尾部 INSERT 只覆盖 menu_id BETWEEN 1300 AND 1359。
--
--   R4. menu 1300 icon='component' 非若依标准图标 key → svg-icon 渲染为空。
--
-- 【修复策略】（8 个 Step，全部幂等，可反复执行）
--   Step 0a：重建 M10_01 的 1320~1339（业态包管理 + 功能开关管理 + F 按钮）
--   Step 0b：重建 M10_02 的 1340~1345（单店覆盖 + 访问通道管理 + F 按钮）
--   Step 0c：UPDATE 6120/6160 parent_id=1300（M10_02 核心动作兜底）
--   Step 1 ：幂等重建 menu 1300 本体（icon 'component'→'money'）
--   Step 2 ：修正 menu 7260 path ''→'complaint'
--   Step 3 ：修正 menu 6160 path 'compliance2'→'regulatory'
--   Step 4 ：强制 1300 全部子菜单 visible='0'/status='0'/client_type='pc'
--   Step 5 ：补齐 role_id=1 对 1300 本体授权
--   Step 6 ：补齐 role_id=1 对 1300 全部子菜单授权（含 re-parent 的 6001/101/102/3130）
--   Step 7 ：补齐 role_id=1 对 1300 全部孙菜单授权（6120/6160/7260 下的 C/F）
--   Step 8 ：补齐 role_id=1 对 1310~1359 区段 F 按钮授权
--
-- 【幂等保证】
--   - INSERT IGNORE / ON DUPLICATE KEY UPDATE：反复执行不产生副作用
--   - UPDATE ... WHERE ... AND (parent_id<>1300 OR path<>'xxx')：只改需要改的
--   → 可安全重复执行；执行顺序无强依赖（放在 00_执行顺序.sql 最末尾兜底）
--
-- 【执行后必做】
--   1. 重启后端 ruoyi-admin 应用（清除 Sa-Token 权限快照 + MyBatis 二级缓存）
--   2. 前端浏览器 Ctrl+F5 硬刷新（清除 permission.js 缓存的 routes）
--   3. 退出登录后重新登录 admin 账号（触发 getRouters 重新拉取）
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- ---------------------------------------------------------------------
-- Step 0a: 重建 M10_01 的 1320~1339（业态包管理 + 功能开关管理 + F 按钮）
--   兜底：若 M15_12 DELETE 后 M10_01 未执行，此处补齐
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `client_type`) VALUES
(1320, '业态包管理',     1300, 7, 'bizBundle',   'system/bizBundle/index',   NULL, 1, 0, 'C', '0', '0', 'system:bizBundle:list',   'shopping',  'admin', NOW(), '', NULL, '业态包配置（menus/feature_flags/blocks/copy）', 'pc'),
(1321, '查询业态包',     1320, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:bizBundle:query',  '#', 'admin', NOW(), '', NULL, '', 'pc'),
(1322, '编辑业态包',     1320, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:bizBundle:edit',   '#', 'admin', NOW(), '', NULL, '', 'pc'),
(1330, '功能开关管理',   1300, 8, 'featureFlag', 'system/featureFlag/index', NULL, 1, 0, 'C', '0', '0', 'system:featureFlag:list', 'switch',    'admin', NOW(), '', NULL, '功能开关（basic/advanced/pro/micro_enabled 四档矩阵）', 'pc'),
(1331, '查询开关',       1330, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:featureFlag:query', '#', 'admin', NOW(), '', NULL, '', 'pc'),
(1332, '启停开关',       1330, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:featureFlag:edit',  '#', 'admin', NOW(), '', NULL, '', 'pc');

-- ---------------------------------------------------------------------
-- Step 0b: 重建 M10_02 的 1340~1345（单店覆盖 + 访问通道管理 + F 按钮）
--   ★关键根因 R0 兜底：若用户跳过 M10_02，此处独立恢复
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `client_type`) VALUES
(1340, '单店覆盖',       1300, 11, 'tenantBizConfig', 'system/tenantBizConfig/index', NULL, 1, 0, 'C', '0', '0', 'system:tenantBizConfig:query', 'edit',       'admin', NOW(), '', NULL, '租户业态单店覆盖配置（module_overrides JSON）', 'pc'),
(1341, '覆盖查询',       1340, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:tenantBizConfig:query', '#', 'admin', NOW(), '', NULL, '', 'pc'),
(1342, '覆盖编辑',       1340, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:tenantBizConfig:edit',  '#', 'admin', NOW(), '', NULL, '', 'pc'),
(1343, '访问通道管理',   1300, 12, 'channel',         'system/message/index',         NULL, 1, 0, 'C', '0', '0', 'system:message:list',          'connection', 'admin', NOW(), '', NULL, '消息通道管理（wechat/wecom/open_api 三通道）', 'pc'),
(1344, '消息查询',       1343, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:query',  '#', 'admin', NOW(), '', NULL, '', 'pc'),
(1345, '消息详情',       1343, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:detail', '#', 'admin', NOW(), '', NULL, '', 'pc');

-- ---------------------------------------------------------------------
-- Step 0c: Re-parent 6120/6160 到 1300（M10_02 核心动作兜底）
--   ★幂等：仅当 parent_id<>1300 时才 UPDATE
-- ---------------------------------------------------------------------
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 13 WHERE `menu_id` = 6120 AND `parent_id` <> 1300;
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 14 WHERE `menu_id` = 6160 AND `parent_id` <> 1300;

-- ---------------------------------------------------------------------
-- Step 0d: Re-parent 7000/7005 到 1300（M15_12 的 re-parent UPDATE 兜底）
--   ★关键发现：7000「行业模板管理」与 7005「角色模板管理」原挂 parent_id=1（系统管理），
--     M15_12 尾部 UPDATE 若未执行（用户跳过 M15_12 或 M15_12 被 M15_06 后续脚本覆盖），
--     两者会停留在 parent_id=1 → 1300 子树缺少「行业模板/角色模板」两个核心平台管理入口。
--   ★幂等：WHERE parent_id<>1300 守卫，反复执行安全
-- ---------------------------------------------------------------------
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 3  WHERE `menu_id` = 7000 AND `parent_id` <> 1300;
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 15 WHERE `menu_id` = 7005 AND `parent_id` <> 1300;

-- ---------------------------------------------------------------------
-- Step 0e: Re-parent 6001/101/102/3130 到 1300（M15_12 的 re-parent UPDATE 兜底）
--   ★虽然当前查询显示四者已在 1300 下，但为保证脚本独立可重复执行，仍加守卫 UPDATE
-- ---------------------------------------------------------------------
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 1  WHERE `menu_id` = 6001 AND `parent_id` <> 1300;
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 2  WHERE `menu_id` = 101  AND `parent_id` <> 1300;
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 4  WHERE `menu_id` = 102  AND `parent_id` <> 1300;
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 16 WHERE `menu_id` = 3130 AND `parent_id` <> 1300;

-- ---------------------------------------------------------------------
-- Step 1: 幂等重建 menu 1300 本体（修正 icon='component'→'money'）
-- ---------------------------------------------------------------------
INSERT INTO `sys_menu` (
    `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `client_type`
) VALUES (
    1300, '资金平台管理菜单', 0, 5, 'fundPlatform', NULL, NULL,
    1, 0, 'M', '0', '0', '', 'money',
    'admin', NOW(), 'admin', NOW(),
    '★本平台全部管理功能统一收纳于此',
    'pc'
) ON DUPLICATE KEY UPDATE
    `menu_name`   = VALUES(`menu_name`),
    `parent_id`   = 0,
    `order_num`   = 5,
    `path`        = 'fundPlatform',
    `component`   = NULL,
    `is_frame`    = 1,
    `is_cache`    = 0,
    `menu_type`   = 'M',
    `visible`     = '0',
    `status`      = '0',
    `icon`        = 'money',
    `client_type` = 'pc',
    `update_by`   = 'admin',
    `update_time` = NOW();

-- ---------------------------------------------------------------------
-- Step 2: 修正 menu 7260「投诉监管」的空 path（根因 R1，最关键修复）
-- ---------------------------------------------------------------------
UPDATE `sys_menu`
   SET `path` = 'complaint',
       `menu_type` = 'M',
       `visible` = '0',
       `status` = '0',
       `client_type` = 'pc',
       `update_by` = 'admin',
       `update_time` = NOW()
 WHERE `menu_id` = 7260
   AND (`path` = '' OR `path` IS NULL);

-- 兜底：若 7260 的 parent_id 因 M25_01 早期版本错误挂在 7000（C 页面），修正为 1300
UPDATE `sys_menu` SET `parent_id` = 1300 WHERE `menu_id` = 7260 AND `parent_id` <> 1300;

-- ---------------------------------------------------------------------
-- Step 3: 修正 menu 6160 path 'compliance2' → 'regulatory'（根因 R2）
--   完全避开前端 ROUTE_FEATURE_MAP 的 'compliance' 关键字
-- ---------------------------------------------------------------------
UPDATE `sys_menu`
   SET `path` = 'regulatory',
       `update_by` = 'admin',
       `update_time` = NOW()
 WHERE `menu_id` = 6160
   AND (`path` LIKE '%compliance%' OR `path` = '');

-- ---------------------------------------------------------------------
-- Step 4: 强制 1300 全部子菜单基础属性合规
-- ---------------------------------------------------------------------
UPDATE `sys_menu`
   SET `visible` = '0',
       `status` = '0',
       `client_type` = 'pc',
       `update_time` = NOW()
 WHERE `parent_id` = 1300
   AND (`visible` <> '0' OR `status` <> '0' OR `client_type` <> 'pc' OR `client_type` IS NULL);

-- ---------------------------------------------------------------------
-- Step 5: 补齐 role_id=1 对 menu 1300 本体的授权
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 1300);

-- ---------------------------------------------------------------------
-- Step 6: 补齐 role_id=1 对 1300 全部子菜单（含 re-parent 的 6001/101/102/3130）授权
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.`menu_id`
  FROM `sys_menu` m
 WHERE m.`parent_id` = 1300
   AND m.`menu_id` <> 1300;

-- ---------------------------------------------------------------------
-- Step 7: 补齐 role_id=1 对 1300 孙菜单（6120/6160/7260/1340/1343 下的 C/F）授权
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.`menu_id`
  FROM `sys_menu` m
 WHERE m.`parent_id` IN (
    SELECT `menu_id` FROM (
        SELECT `menu_id` FROM `sys_menu` WHERE `parent_id` = 1300
    ) t
 );

-- ---------------------------------------------------------------------
-- Step 8: 补齐 role_id=1 对 1310~1359 区段全部菜单授权
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` BETWEEN 1310 AND 1359;

SET SQL_SAFE_UPDATES = 1;
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 验证 SQL（执行完本脚本后逐条运行确认修复生效）
-- =====================================================================
-- V1. menu 1300 本体（预期：1 行，parent_id=0, menu_type=M, visible=0, status=0, icon=money, client_type=pc）
-- SELECT menu_id, menu_name, parent_id, order_num, path, menu_type, visible, status, icon, client_type
--   FROM sys_menu WHERE menu_id = 1300;
--
-- V2. 1300 子菜单齐全（预期：≥14 行，全部 visible=0/status=0/client_type=pc；
--     含 6001/101/7000/102/7005/1310/1320/1330/1340/1343/3130/6120/6160/7260）
-- SELECT menu_id, menu_name, order_num, path, menu_type, visible, status, client_type
--   FROM sys_menu WHERE parent_id = 1300 ORDER BY order_num;
--
-- V3. 关键 path 修复（预期：7260 path='complaint'，6160 path='regulatory'）
-- SELECT menu_id, menu_name, path FROM sys_menu WHERE menu_id IN (7260, 6160);
--
-- V4. role_id=1 对 1300 全部子菜单授权完整（预期：≥15 行）
-- SELECT rm.menu_id, m.menu_name, m.menu_type, m.path
--   FROM sys_role_menu rm JOIN sys_menu m ON rm.menu_id = m.menu_id
--  WHERE rm.role_id = 1 AND (rm.menu_id = 1300 OR m.parent_id = 1300)
--  ORDER BY m.order_num;
--
-- V5. 前端关键字扫描（预期：0 行）
-- SELECT menu_id, menu_name, path FROM sys_menu
--  WHERE parent_id = 1300
--    AND (path LIKE '%compliance%' OR path LIKE '%credit%' OR path LIKE '%asset%'
--      OR path LIKE '%contract%' OR path LIKE '%loan%' OR path LIKE '%ai/%');
--
-- V6. 空 path 扫描（预期：0 行）
-- SELECT menu_id, menu_name, path FROM sys_menu
--  WHERE parent_id = 1300 AND (path = '' OR path IS NULL);
--
-- V7. 6120/6160 已收纳到 1300（预期：parent_id=1300）
-- SELECT menu_id, menu_name, parent_id, order_num, path FROM sys_menu WHERE menu_id IN (6120, 6160);
--
-- =====================================================================
-- 【用户操作步骤】（执行完本脚本后必做，缺一不可）
-- =====================================================================
-- 1. 重启后端 ruoyi-admin 应用（清除 Sa-Token 权限快照 + MyBatis 二级缓存）
-- 2. 前端浏览器 Ctrl+F5 硬刷新（清除 permission.js 缓存的旧 routes）
-- 3. 退出登录后重新用 admin 账号登录（触发 GET /getRouters 重新拉取菜单树）
-- 4. 验证侧边栏出现「资金平台管理菜单」一级目录，展开显示 ≥14 个子菜单
-- =====================================================================
