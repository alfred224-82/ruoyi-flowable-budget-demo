-- =====================================================================
-- M15_07 标准角色集补充 + 小微版角色模板初始化脚本（T15.7，v1.13 新增）
-- 版本：v1.0  日期：2026-09-07
-- 依据：《小微版完整方案》v10.0 第二章（标准角色集）/ 第四章（角色模板）
--       《资金管控系统开发任务文档》v1.13 T15.7
--       《多业态SaaS平台需求规格说明书》v1.0 第 12 章（标准角色集与角色映射机制）
-- 前置依赖：M2_01_roles.sql（boss/finance/manager/employee/platform_admin 已建）、
--           M15_01（role_template）、M15_06（micro 菜单 perms）
-- 规范：
--   1. 先删后插（按 role_key / version_type+role_code 精确删除），脚本可重复执行（幂等）
--   2. ★全版本共用同一张 sys_role 表，【不存在「小微版角色表」和「企业版角色表」两张表】
--   3. 标准角色集（sys_role.role_key）：
--        boss(老板) / finance(财务) / manager(项目经理≈project_manager) /
--        employee(报销人≈expense_staff) / platform_admin(租户组织管理员)  ← M2_01 既有
--        salesman(业务员) / customer(顾客)                                ← 本脚本新增(小微版)
--      说明：方案中 project_manager/expense_staff 为概念命名，DB role_key 沿用既有
--           manager/employee 以保持企业版兼容，不新增重复角色 code
--   4. 角色 ID 续用固定段：salesman=9206、customer=9207（避免与雪花 ID/既有角色冲突）
--   5. role_template.version_type='micro' 区分企业版配方；同一 boss 角色在小微版绑小微菜单、
--      在企业版绑企业版菜单（靠 version_type 区分，不新建角色 code）
--   6. 平台管理员只能做角色映射（tenant_role_mapping），权限永远由 admin 在 role_template 预置
-- =====================================================================

-- ----------------------------
-- 1. 补充小微版标准角色（salesman / customer），先删后插幂等
-- ----------------------------
DELETE FROM `sys_role` WHERE `role_key` IN ('salesman', 'customer');
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `remark`) VALUES
(9206, '业务员', 'salesman', 14, '5', 1, 1, '0', '0', 'admin', NOW(), '小微版：排班/收入/打卡；主用小程序；可被租户映射改名为「助教/配送员/教练/技师」'),
(9207, '顾客',   'customer', 15, '5', 1, 1, '0', '0', 'admin', NOW(), '小微版：浏览/预约/到店付(V1无在线支付)；主用小程序；可免密轻登');

-- ----------------------------
-- 1.1 防御性校正：salesman/customer 为全版本共享标准角色，tenant_id 须为 NULL（跨租户可见）
--     与 M2_04 对 9201~9205 的处理一致；防 M2_04 幂等重跑（line「NOT IN 9201~9205」）误置为 0
--     走 INFORMATION_SCHEMA 条件判断（sys_role.tenant_id 由 M2_04 补列，未补则跳过）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_07_fix_shared_role_tenant`;
DELIMITER $$
CREATE PROCEDURE `m15_07_fix_shared_role_tenant`()
BEGIN
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND COLUMN_NAME = 'tenant_id') THEN
        UPDATE `sys_role` SET `tenant_id` = NULL WHERE `role_key` IN ('salesman', 'customer');
    END IF;
END$$
DELIMITER ;
CALL m15_07_fix_shared_role_tenant();
DROP PROCEDURE IF EXISTS `m15_07_fix_shared_role_tenant`;

-- ----------------------------
-- 2. 小微版角色模板 role_template（version_type='micro'，先删后插幂等）
--    menu_keys = 该角色可见 perms 集合；block_keys = 小程序首页区块（module_type=micro）
--    运行时：role_code + version_type → role_template → menu_keys → 按 client_type 端过滤
-- ----------------------------
DELETE FROM `role_template` WHERE `version_type` = 'micro';
INSERT INTO `role_template` (`version_type`, `role_code`, `menu_keys`, `block_keys`, `status`, `create_by`, `remark`) VALUES
-- boss：PC 全量管理 + 小程序「报表+改报价+提醒」
('micro', 'boss',
 JSON_ARRAY(
   'micro:dashboard:view',
   'micro:booking:list','micro:booking:add','micro:booking:edit','micro:booking:cancel','micro:booking:complete','micro:order:detail',
   'micro:settle:list','micro:settle:create','micro:settle:generate','micro:settle:markPaid','micro:settle:detail','micro:settle:history',
   'micro:income:list','micro:income:add','micro:expense:list','micro:expense:add','micro:budgetAlert:edit',
   'micro:report:weekly','micro:report:monthly','micro:report:profit','micro:report:export','micro:report:cashFlow',
   'micro:promo:list','micro:promo:add','micro:promo:edit','micro:promo:remove','micro:promo:poster',
   'micro:service:toggle','micro:shop:hours',
   'micro:dashboard:today','micro:service:list','micro:service:edit','micro:service:editConfirm','micro:notify:list'
 ),
 JSON_ARRAY('revenue_week','revenue_pending','notice_bar','shop_info'),
 '0', 'admin', '老板：PC 全量管理；小程序仅报表+定价快改+提醒(client_type=mini 过滤)'),

-- salesman：仅小程序「排班/收入/打卡」
('micro', 'salesman',
 JSON_ARRAY(
   'micro:staff:schedule','micro:staff:scheduleDetail',
   'micro:staff:income','micro:staff:incomeWeek','micro:staff:incomeMonth','micro:staff:incomeDetail',
   'micro:staff:checkin','micro:staff:checkinSubmit'
 ),
 JSON_ARRAY('revenue_received','revenue_pending','my_schedule','checkin_entry'),
 '0', 'admin', '业务员：仅小程序排班/收入/打卡；看不到任何管理类菜单'),

-- customer：仅小程序「浏览/预约/到店付」，★无 micro:customer:pay
('micro', 'customer',
 JSON_ARRAY(
   'micro:customer:shop',
   'micro:customer:book','micro:customer:selectService','micro:customer:selectTime','micro:customer:submitBook',
   'micro:customer:myBook','micro:customer:cancelBook','micro:customer:payInfo'
 ),
 JSON_ARRAY('staff_photo_list','activity_banner','quick_booking','shop_info'),
 '0', 'admin', '顾客：仅小程序浏览/预约/到店付；★无在线支付 perms(micro:customer:pay 不存在)');

-- ----------------------------
-- 3. 验收查询（执行后人工核对）
-- ----------------------------
-- 3.1 标准角色集共用一张 sys_role 表（boss/salesman/customer 均存在，role_key 唯一）
-- SELECT role_id, role_name, role_key FROM sys_role
--  WHERE role_key IN ('boss','salesman','customer','finance','manager','employee','platform_admin')
--  ORDER BY role_id;  -- 预期 7 行（含新增 salesman=9206/customer=9207）
--
-- 3.2 小微版三配方完整
-- SELECT version_type, role_code, JSON_LENGTH(menu_keys) AS menus, JSON_LENGTH(block_keys) AS blocks
--  FROM role_template WHERE version_type='micro';  -- 预期 boss/salesman/customer 各 1 行
--
-- 3.3 ★不存在「小微版角色表」与「企业版角色表」两张表（仅 sys_role 一张 + role_template 按 version_type 区分）
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('micro_role','enterprise_role');  -- 预期 0
--
-- 3.4 ★顾客模板无在线支付 perms
-- SELECT JSON_CONTAINS(menu_keys, '"micro:customer:pay"') FROM role_template
--  WHERE version_type='micro' AND role_code='customer';  -- 预期 0
--
-- 3.5 同一 boss 角色跨版本：micro 绑小微菜单、basic/advanced/professional 绑企业版菜单（version_type 区分）
-- SELECT version_type, role_code FROM role_template WHERE role_code='boss' ORDER BY version_type;
