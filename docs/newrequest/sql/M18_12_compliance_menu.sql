-- =====================================================================
-- M18_12 图片合规与全局敏感词菜单 + role_template 脚本（T18.4 / T18.5，v1.30 新增）
-- 版本：v1.0  日期：2026-09-19
-- 依据：《资金管控系统开发任务文档》v1.27 M18 图片合规与全局敏感词
--       T18.4 Admin 全局词库/图片违规类型配置/合规告警看板；T18.5 租户自定义词 + 老板只读记录
-- 前置依赖：M1_04（sys_menu）+ M15_06（sys_menu.client_type）+ M18_10（三表已建立）
-- 归属：全版本通用（Admin 平台合规菜单 1370~1380 各版本部署；
--       租户/老板菜单 7680~7690 为小微版区段，企业版无对应 role_key 时 INSERT IGNORE no-op，安全）
-- 规范：
--   1. 先删后插幂等（DELETE + INSERT），仅删本脚本自建 menu_id 区段，不动其他脚本菜单
--   2. Admin 平台合规目录 1370 parent_id=0（与 1300 资金平台管理并列，均为超管顶级目录）
--   3. 租户敏感词管理 7680 挂 7500（platform_admin 租户管理员目录）；老板命中记录 7690 挂 7100（老板运营）
--   4. role_template 追加走 JSON_CONTAINS 守卫，幂等
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 一、Admin 平台合规菜单（menu_id 1370~1380，全版本通用，超管 role_id=1）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1370 AND 1389;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 1370 AND 1389;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`client_type`,`icon`,`create_by`,`create_time`,`remark`) VALUES
(1370, '平台合规', 0, 7, 'compliance', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'shield', 'admin', NOW(), '★平台级合规底线：全局敏感词库 + 图片违规类型配置 + 合规告警看板（M18 T18.4/T18.5）'),
-- 1.1 全局敏感词库（T18.4 ①）
(1371, '全局敏感词库', 1370, 1, 'sensitiveWord', 'compliance/sensitiveWord/index', NULL, 1, 0, 'C', '0', '0', 'compliance:sensitiveWord:list', 'pc', 'documentation', 'admin', NOW(), 'Admin 维护全局底库（tenant_id=0），八大类分类管理'),
(1372, '敏感词新增', 1371, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:sensitiveWord:add', 'pc', '#', 'admin', NOW(), NULL),
(1373, '敏感词修改', 1371, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:sensitiveWord:edit', 'pc', '#', 'admin', NOW(), NULL),
(1374, '敏感词删除', 1371, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:sensitiveWord:remove', 'pc', '#', 'admin', NOW(), NULL),
(1375, '敏感词批量导入', 1371, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:sensitiveWord:import', 'pc', '#', 'admin', NOW(), NULL),
(1376, '敏感词总开关', 1371, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:sensitiveWord:toggle', 'pc', '#', 'admin', NOW(), 'FEAT_SENSITIVE_WORD 全局启停（T18.8）'),
-- 1.2 图片违规类型配置（T18.4 ②）
(1377, '图片违规类型配置', 1370, 2, 'imageConfig', 'compliance/imageConfig/index', NULL, 1, 0, 'C', '0', '0', 'compliance:imageConfig:list', 'pc', 'image', 'admin', NOW(), 'porn/violence/politics 三类违规检测开关 + 置信度阈值 + 两路(OCR/视觉)启停'),
(1378, '图片配置保存', 1377, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:imageConfig:save', 'pc', '#', 'admin', NOW(), NULL),
(1379, '图片合规总开关', 1377, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:imageConfig:toggle', 'pc', '#', 'admin', NOW(), 'FEAT_IMAGE_COMPLIANCE 全局启停（T18.8）'),
-- 1.3 合规告警看板（T18.5 ③）
(1380, '合规告警看板', 1370, 3, 'alert', 'compliance/alert/index', NULL, 1, 0, 'C', '0', '0', 'compliance:alert:dashboard', 'pc', 'chart', 'admin', NOW(), '命中趋势 + TOP10 命中词 + 高风险租户聚合（sensitive_word_hit_log）');

-- 授予超管 role_id=1（全版本通用）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 1370 AND 1389;

-- =====================================================================
-- 二、租户管理员敏感词管理菜单（menu_id 7680~7686，小微版区段，platform_admin）
--     ★挂 7500（platform_admin 租户管理员目录）；企业版无此目录/角色则 no-op
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7680 AND 7689;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7680 AND 7689;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`client_type`,`icon`,`create_by`,`create_time`,`remark`) VALUES
(7680, '敏感词管理', 7500, 30, 'complianceWord', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'shield', 'admin', NOW(), '★租户管理员：全局底库只读 + 自定义词 CRUD + 命中记录只读（M18 T18.5）'),
(7681, '全局底库(只读)', 7680, 1, 'globalWord', 'micro/sensitiveWord/global', NULL, 1, 0, 'C', '0', '0', 'micro:sensitiveWord:query', 'pc', 'documentation', 'admin', NOW(), '只读展示平台底库（tenant_id=0），租户不可修改'),
(7682, '自定义词管理', 7680, 2, 'customWord', 'micro/sensitiveWord/index', NULL, 1, 0, 'C', '0', '0', 'micro:sensitiveWord:list', 'pc', 'edit', 'admin', NOW(), '租户自定义敏感词 CRUD（tenant_id=当前）'),
(7683, '命中记录(只读)', 7680, 3, 'hitLog', 'micro/sensitiveWord/hitLog', NULL, 1, 0, 'C', '0', '0', 'micro:sensitiveWord:hitLog', 'pc', 'log', 'admin', NOW(), '只读查看本租户敏感词命中记录'),
(7684, '自定义词新增', 7682, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:add', 'pc', '#', 'admin', NOW(), NULL),
(7685, '自定义词修改', 7682, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:edit', 'pc', '#', 'admin', NOW(), NULL),
(7686, '自定义词删除', 7682, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:remove', 'pc', '#', 'admin', NOW(), NULL);

-- =====================================================================
-- 三、老板端合规命中记录只读菜单（menu_id 7690，小微版区段，boss）
--     ★挂 7100（老板 PC 运营目录）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7690;
DELETE FROM `sys_menu`      WHERE `menu_id` = 7690;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`client_type`,`icon`,`create_by`,`create_time`,`remark`) VALUES
(7690, '合规命中记录', 7100, 90, 'complianceHit', 'micro/compliance/hitLog', NULL, 1, 0, 'C', '0', '0', 'micro:boss:complianceHit:list', 'pc', 'log', 'admin', NOW(), '★老板只读：本店敏感词/图片合规命中记录（M18 T18.5）');

-- =====================================================================
-- 四、sys_role_menu 存量补录（租户/老板菜单，仅小微版角色存在时生效）
-- =====================================================================
-- 4.1 租户敏感词管理菜单（7680~7686）→ platform_admin 克隆角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r CROSS JOIN `sys_menu` m
WHERE r.role_key = 'platform_admin' AND m.menu_id BETWEEN 7680 AND 7689;

-- 4.2 老板合规命中记录菜单（7690）→ micro_pro / boss 角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r CROSS JOIN `sys_menu` m
WHERE r.role_key IN ('micro_pro', 'boss') AND m.menu_id = 7690;

-- =====================================================================
-- 五、role_template 追加（小微版分档配方，JSON_CONTAINS 守卫幂等）
-- =====================================================================
-- 5.1 boss(micro/micro_pro) 追加 老板合规命中记录只读权限
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:boss:complianceHit:list')
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:boss:complianceHit:list"');

-- 5.2 platform_admin(micro/micro_pro) 追加 租户敏感词管理权限（含只读底库 + 自定义 CRUD + 命中记录）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(
    JSON_ARRAY_APPEND(
      JSON_ARRAY_APPEND(
        JSON_ARRAY_APPEND(
          JSON_ARRAY_APPEND(
            JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:sensitiveWord:query'),
            '$', 'micro:sensitiveWord:list'),
          '$', 'micro:sensitiveWord:add'),
        '$', 'micro:sensitiveWord:edit'),
      '$', 'micro:sensitiveWord:remove'),
    '$', 'micro:sensitiveWord:hitLog')
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:sensitiveWord:list"');

-- =====================================================================
-- 六、验收查询（执行后人工核对）
-- =====================================================================
-- 6.1 Admin 平台合规菜单落库（1370~1380）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--   WHERE menu_id BETWEEN 1370 AND 1389 ORDER BY menu_id;
--   -- 预期 11 行（1370 目录 + 1371/1377/1380 三 C 菜单 + 1372~1376/1378~1379 七 F 按钮）
--
-- 6.2 超管授权（role_id=1）
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id=1 AND menu_id BETWEEN 1370 AND 1389;
--   -- 预期 11
--
-- 6.3 租户/老板菜单落库（7680~7690）
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 7680 AND 7690 ORDER BY menu_id;
--   -- 预期 8 行（7680 目录 + 7681/7682/7683 三 C + 7684~7686 三 F + 7690 老板 C）
--
-- 6.4 role_template 追加（小微版）
-- SELECT JSON_CONTAINS(menu_keys,'"micro:boss:complianceHit:list"') AS boss_hit,
--        JSON_CONTAINS(menu_keys,'"micro:sensitiveWord:list"')       AS pa_word
--   FROM role_template WHERE version_type='micro_pro' AND role_code IN ('boss','platform_admin');
--   -- boss 行 boss_hit=1；platform_admin 行 pa_word=1
--
-- 6.5 权限标识唯一（无重复 perms）
-- SELECT perms, COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 1370 AND 1389 AND perms<>''
--   GROUP BY perms HAVING COUNT(*)>1;  -- 预期 0 行
-- =====================================================================
