-- =====================================================================
-- M15_12 资金平台管理菜单 + 版本档位管理脚本（T15.29，v1.14 新增 / 对齐《小微版完整方案》v10.5 §5.0/§5.1）
-- 版本：v1.0  日期：2026-09-08
-- 依据：《小微版完整方案》v10.5 §5.0（资金平台管理菜单收纳规则）/ §5.1（admin PC 菜单明细）
--       《资金管控系统开发任务文档》v1.14 T15.29
-- 前置依赖：M1_04_sys_menu.sql（若依原生菜单）、M15_06（micro 菜单 7000~7499 + client_type）、
--           phase16_17_tenant_welfare.sql（租户管理菜单 6000/6001）、M15_10（version_type 分档）
-- ★menu_id 区段说明（重要）：
--   方案原文建议「menu_id 从 1000 起」，但经核对若依原生按钮已占用 1000~1230（1001~1230 为用户/角色/菜单等按钮），
--   1231~1999 为空闲安全区。为避免主键冲突，本脚本将「资金平台管理菜单」目录及其子菜单置于 1300~1359 区段。
-- 规范：
--   1. 新建一级目录「资金平台管理菜单」(menu_id=1300, parent=0)，与若依原生「系统管理」并列保留
--   2. 新增「版本档位管理」(system:edition:*)：维护 micro_lite/micro_pro/basic/advanced/pro 五档 + 计费 + 每档菜单范围
--   3. 平台自有管理菜单统一收纳：按 perms 将「行业模板/角色模板/菜单管理/标准角色(角色管理)/租户管理」re-parent 到 1300
--      （UPDATE by perms 幂等安全：目标不存在则 no-op；仅改 parent_id/order_num，role_menu 授权不受影响）
--   4. 新建租户级联（version-options/industry-options/default-role-preview）为 Java 接口，perms 复用 system:tenant:list，见 §3 注释
--   5. 全部新菜单授予超管 role_id=1
-- =====================================================================

-- ----------------------------
-- 1. 资金平台管理菜单目录 + 版本档位管理（先删后插幂等，menu_id 1300~1359）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1300 AND 1359;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 1300 AND 1359;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 一级目录：资金平台管理菜单（本平台管理功能唯一根目录，与若依「系统管理」并列）
(1300, '资金平台管理菜单', 0, 5, 'fundPlatform', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'component', 'admin', NOW(), '★本平台全部管理功能统一收纳于此(租户/标准角色/行业模板/菜单/角色模板/版本档位)；若依原生「系统管理」并列保留'),
-- 版本档位管理（T15.29 新增：维护版本档位 + 计费 + 每档菜单范围）
(1310, '版本档位管理', 1300, 6, 'edition', 'system/edition/index', NULL, 1, 0, 'C', '0', '0', 'system:edition:list', 'pc', 'star', 'admin', NOW(), '维护 micro_lite/micro_pro/basic/advanced/pro 五档 + 计费规则 + 每档菜单范围(升级即扩围)'),
(1311, '查询档位',     1310, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:edition:query',    'pc', '#', 'admin', NOW(), ''),
(1312, '新建档位',     1310, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:edition:add',      'pc', '#', 'admin', NOW(), ''),
(1313, '编辑档位',     1310, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:edition:edit',     'pc', '#', 'admin', NOW(), ''),
(1314, '绑定菜单范围', 1310, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:edition:bindMenu', 'pc', '#', 'admin', NOW(), '★该档位解锁的菜单集合(role_template.menu_keys)，升级即扩围');

-- ----------------------------
-- 2. 平台自有管理菜单 re-parent 到「资金平台管理菜单」(1300)
--    ★按 perms/menu_name 匹配，UPDATE 幂等安全（不存在则 no-op）；仅调整 parent_id + order_num
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;
-- 2.1 租户管理目录（re-parent 目录本身，其子菜单随目录一起移动）
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 1 WHERE `menu_name` = '租户管理'   AND `menu_type` = 'M' AND `parent_id` <> 1300;
-- 2.2 标准角色管理（若依原生角色管理 system:role:list）
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 2 WHERE `perms` = 'system:role:list'        AND `menu_type` = 'C' AND `parent_id` <> 1300;
-- 2.3 行业模板管理（小微版专属 system:industry:list，M15_06 建，原挂系统管理下）
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 3 WHERE `perms` = 'system:industry:list'    AND `menu_type` = 'C' AND `parent_id` <> 1300;
-- 2.4 菜单管理（若依原生 system:menu:list，方案规则2：移到本目录统一维护）
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 4 WHERE `perms` = 'system:menu:list'        AND `menu_type` = 'C' AND `parent_id` <> 1300;
-- 2.5 角色模板管理（system:roleTemplate:list，M15_06 建，原挂系统管理下）
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 5 WHERE `perms` = 'system:roleTemplate:list' AND `menu_type` = 'C' AND `parent_id` <> 1300;
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 3. 授予超管 role_id=1（新增目录 + 版本档位管理菜单）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 1300 AND 1359;

-- ★新建租户级联接口（Java 交付，T15.29，perms 复用 system:tenant:list，前端在「新建/编辑租户」表单调用）：
--   GET /system/edition/version-options?tenantType=micro|enterprise
--       → 返回该产品线可选 version_type 列表（micro→[micro_lite,micro_pro]；enterprise→[basic,advanced,pro]）
--   GET /system/edition/industry-options?tenantType=micro
--       → 返回可选行业模板（小微版专属，来自 industry_template）
--   GET /system/edition/default-role-preview?versionType=micro_pro
--       → 返回该档位默认角色配方预览（role_template：role_code + menu_keys 数量）
--   级联顺序：选产品线(tenant_type) → 级联版本档位(version_type) → 级联行业模板 → 预览默认角色

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 资金平台管理菜单目录 + 版本档位管理存在
-- SELECT menu_id, menu_name, parent_id, perms, menu_type FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359 ORDER BY menu_id;
-- 预期：1300(目录) + 1310(版本档位管理) + 1311~1314(4个按钮)
--
-- 4.2 平台自有管理菜单已收纳到 1300 下
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE parent_id = 1300 ORDER BY order_num;
-- 预期：租户管理/标准角色管理/行业模板管理/菜单管理/角色模板管理/版本档位管理 均 parent_id=1300
--
-- 4.3 ★若依原生「系统管理」目录仍保留（与新目录并列）
-- SELECT menu_id, menu_name FROM sys_menu WHERE menu_id = 1 AND menu_type = 'M';  -- 预期：系统管理
--
-- 4.4 新菜单已授权 admin(role_id=1)
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 1300 AND 1359;  -- 预期 6
--
-- 4.5 ★menu_id 无冲突（1300~1359 未与若依原生 1000~1230 重叠）
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359;  -- 预期 6（无重复主键报错即通过）
