-- =====================================================================
-- M15_10 小微版分档机制脚本（T15.26 + T15.27，v1.14 新增 / 对齐《小微版完整方案》v10.1）
-- 版本：v1.0  日期：2026-09-08
-- 依据：《小微版完整方案》v10.5 第二章 2.1（小微版分档）/ 第十二章（分档计费）
--       《资金管控系统开发任务文档》v1.14 T15.26 / T15.27
-- 前置依赖：M15_01（role_template）、M15_07（micro 配方）、M14_01（sys_billing_plan）
-- 规范：
--   1. tenant_type / version_type 补列走 INFORMATION_SCHEMA 条件判断（幂等）
--   2. ★分档差异只体现在 role_template 勾选的菜单范围，禁止为分档新增角色 code 或复制模板
--   3. ★lite→pro 升级 = UPDATE tenant.version_type + 刷新菜单缓存，禁止任何 DDL 与数据搬迁
--   4. role_template 按 version_type 分档：micro_lite(免费·仅推广) / micro_pro(收费·全经营)
--   5. 计费复用 M14 四档套餐（sys_billing_plan），micro_pro 收费档直接复用，micro_lite 免费档不激活计费周期
--   6. 存量回填：edition=BASIC→version_type='basic'；ADVANCED→'advanced'；PROFESSIONAL→'pro'；tenant_type='enterprise'
-- =====================================================================

-- ----------------------------
-- 1. sys_tenant 增加 tenant_type / version_type 列（幂等条件 ALTER）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_10_add_tenant_type_columns`;
DELIMITER $$
CREATE PROCEDURE `m15_10_add_tenant_type_columns`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'tenant_type') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `tenant_type` VARCHAR(16) NOT NULL DEFAULT 'enterprise'
                COMMENT '产品线：micro(小微版)/enterprise(企业版)；T15.25 硬隔离 + T15.29 级联' AFTER `edition`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'version_type') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `version_type` VARCHAR(20) DEFAULT NULL
                COMMENT '版本档位：micro_lite/micro_pro/basic/advanced/pro；T15.26 分档' AFTER `tenant_type`;
    END IF;
END$$
DELIMITER ;
CALL m15_10_add_tenant_type_columns();
DROP PROCEDURE IF EXISTS `m15_10_add_tenant_type_columns`;

-- ----------------------------
-- 2. 存量租户回填 tenant_type / version_type（按 edition 映射，幂等 UPDATE）
--    ★企业线：BASIC→basic / ADVANCED→advanced / PROFESSIONAL→pro；tenant_type=enterprise
--    ★小微线存量（若有 edition=MICRO）：默认 micro_pro（收费档），tenant_type=micro
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;
UPDATE `sys_tenant` SET `tenant_type` = 'enterprise', `version_type` = 'basic'      WHERE `edition` = 'BASIC'      AND (`version_type` IS NULL OR `version_type` = '');
UPDATE `sys_tenant` SET `tenant_type` = 'enterprise', `version_type` = 'advanced'   WHERE `edition` = 'ADVANCED'   AND (`version_type` IS NULL OR `version_type` = '');
UPDATE `sys_tenant` SET `tenant_type` = 'enterprise', `version_type` = 'pro'        WHERE `edition` IN ('PROFESSIONAL','ENTERPRISE') AND (`version_type` IS NULL OR `version_type` = '');
UPDATE `sys_tenant` SET `tenant_type` = 'micro',      `version_type` = 'micro_pro'  WHERE `edition` = 'MICRO'      AND (`version_type` IS NULL OR `version_type` = '');
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 3. 索引：支撑按 tenant_type / version_type 过滤（可选角色列表 + 分档菜单加载）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_10_add_tenant_type_index`;
DELIMITER $$
CREATE PROCEDURE `m15_10_add_tenant_type_index`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND INDEX_NAME = 'idx_tenant_type_version') THEN
        ALTER TABLE `sys_tenant` ADD INDEX `idx_tenant_type_version` (`tenant_type`, `version_type`);
    END IF;
END$$
DELIMITER ;
CALL m15_10_add_tenant_type_index();
DROP PROCEDURE IF EXISTS `m15_10_add_tenant_type_index`;

-- ----------------------------
-- 4. role_template 小微版分档配方（先删后插幂等，按 version_type+role_code 精确删除）
--    ★micro_lite(免费·仅推广)：boss 只绑「推广发布/店铺管理/系统设置」；salesman/customer 只绑「活动浏览页」
--    ★micro_pro(收费·全经营)：= lite 集合 ∪ 经营集合（预约/结算/收支/报表/定价/人员管理）
--    升级 lite→pro = UPDATE tenant.version_type + 刷新菜单缓存，角色 code / 用户 / 业务数据零迁移
-- ----------------------------
DELETE FROM `role_template` WHERE `version_type` IN ('micro_lite', 'micro_pro');

-- 4.1 micro_lite 档（免费·仅推广）
INSERT INTO `role_template` (`version_type`, `role_code`, `menu_keys`, `block_keys`, `status`, `create_by`, `remark`) VALUES
-- boss lite：推广发布 + 店铺管理 + 经营概览（简化版） + 系统设置（无预约/结算/收支/报表/定价）
('micro_lite', 'boss',
 JSON_ARRAY(
   'micro:dashboard:view',
   'micro:promo:list','micro:promo:add','micro:promo:edit','micro:promo:remove','micro:promo:poster',
   'micro:service:toggle','micro:shop:hours','micro:shop:payInfo','micro:notify:config',
   'micro:activity:browse'
 ),
 JSON_ARRAY('activity_banner','shop_info','notice_bar'),
 '0', 'admin', 'micro_lite 老板：推广发布/店铺管理/经营概览(简化)/系统设置；★无预约/结算/收支/报表/定价'),

-- salesman lite：仅活动浏览页（只读）
('micro_lite', 'salesman',
 JSON_ARRAY('micro:activity:browse'),
 JSON_ARRAY('activity_banner','shop_info'),
 '0', 'admin', 'micro_lite 业务员：仅活动浏览页(只读)；升级 pro 后自动扩展排班/收入/打卡'),

-- customer lite：仅活动浏览页（只读，无需登录可浏览）
('micro_lite', 'customer',
 JSON_ARRAY('micro:activity:browse'),
 JSON_ARRAY('activity_banner','shop_info','staff_photo_list'),
 '0', 'admin', 'micro_lite 顾客：仅活动浏览页(只读)；★无预约/打卡入口；升级 pro 后自动扩展预约/我的预约');

-- 4.2 micro_pro 档（收费·全经营，= lite 集合 ∪ 经营集合）
INSERT INTO `role_template` (`version_type`, `role_code`, `menu_keys`, `block_keys`, `status`, `create_by`, `remark`) VALUES
-- boss pro：PC 全量管理 + 小程序「报表+改报价+提醒」（复用 M15_07 micro 配方 + lite 推广集合）
('micro_pro', 'boss',
 JSON_ARRAY(
   'micro:dashboard:view','micro:dashboard:today',
   'micro:booking:list','micro:booking:add','micro:booking:edit','micro:booking:cancel','micro:booking:complete','micro:order:detail',
   'micro:settle:list','micro:settle:create','micro:settle:generate','micro:settle:markPaid','micro:settle:detail','micro:settle:history',
   'micro:income:list','micro:income:add','micro:expense:list','micro:expense:add','micro:budgetAlert:edit',
   'micro:report:weekly','micro:report:monthly','micro:report:profit','micro:report:export','micro:report:cashFlow',
   'micro:promo:list','micro:promo:add','micro:promo:edit','micro:promo:remove','micro:promo:poster',
   'micro:service:list','micro:service:add','micro:service:edit','micro:service:editConfirm','micro:service:toggle',
   'micro:shop:hours','micro:shop:payInfo','micro:notify:list','micro:notify:config',
   'micro:activity:browse',
   'micro:customer:list','micro:customer:unblacklist'
 ),
 JSON_ARRAY('revenue_week','revenue_pending','notice_bar','shop_info','activity_banner'),
 '0', 'admin', 'micro_pro 老板：全经营(预约/结算/收支/报表/定价/推广/店铺/顾客管理)；★含防爽约黑名单移出'),

-- salesman pro：仅小程序「排班/收入/打卡」+ 活动浏览页
('micro_pro', 'salesman',
 JSON_ARRAY(
   'micro:staff:schedule','micro:staff:scheduleDetail',
   'micro:staff:income','micro:staff:incomeWeek','micro:staff:incomeMonth','micro:staff:incomeDetail',
   'micro:staff:checkin','micro:staff:checkinSubmit',
   'micro:activity:browse'
 ),
 JSON_ARRAY('revenue_received','revenue_pending','my_schedule','checkin_entry','activity_banner'),
 '0', 'admin', 'micro_pro 业务员：排班/收入/打卡 + 活动浏览页'),

-- customer pro：仅小程序「浏览/预约/到店付」+ 活动浏览页，★无 micro:customer:pay
('micro_pro', 'customer',
 JSON_ARRAY(
   'micro:customer:shop',
   'micro:customer:book','micro:customer:selectService','micro:customer:selectTime','micro:customer:submitBook',
   'micro:customer:myBook','micro:customer:cancelBook','micro:customer:payInfo',
   'micro:activity:browse'
 ),
 JSON_ARRAY('staff_photo_list','activity_banner','quick_booking','shop_info'),
 '0', 'admin', 'micro_pro 顾客：浏览/预约/到店付 + 活动浏览页；★无在线支付 perms');

-- ----------------------------
-- 5. 分档计费套餐（复用 M14 sys_billing_plan 表结构，先删后插幂等）
--    ★sys_billing_plan 实际列：plan_code(PK)/name/price/days/license_type/edition/first_only/need_pay/sort/status/remark
--    ★本脚本为分档计费补充 version_type 列（幂等 ALTER），用于按档位过滤套餐；NULL=通用套餐
--    ★micro_lite 永久免费（0 元，need_pay=0，days=0 不激活计费周期）
--    ★micro_pro 复用 M14 四档定价（试用 0 元 7 天 / 首月 299 / 月付 499 / 包年 4790）
--    plan_code 命名：micro_lite_free / micro_pro_trial / micro_pro_first_month / micro_pro_monthly / micro_pro_yearly
-- ----------------------------
-- 5.1 sys_billing_plan 补 version_type 列（幂等条件 ALTER）
DROP PROCEDURE IF EXISTS `m15_10_add_plan_version_type`;
DELIMITER $$
CREATE PROCEDURE `m15_10_add_plan_version_type`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_billing_plan' AND COLUMN_NAME = 'version_type') THEN
        ALTER TABLE `sys_billing_plan`
            ADD COLUMN `version_type` VARCHAR(20) DEFAULT NULL
                COMMENT '适用版本档位：micro_lite/micro_pro/basic/advanced/pro；NULL=通用套餐；T15.27 分档计费' AFTER `edition`;
    END IF;
END$$
DELIMITER ;
CALL m15_10_add_plan_version_type();
DROP PROCEDURE IF EXISTS `m15_10_add_plan_version_type`;

-- 5.2 小微版分档套餐数据（先删后插幂等）
DELETE FROM `sys_billing_plan` WHERE `plan_code` LIKE 'micro\_%';

INSERT INTO `sys_billing_plan` (`plan_code`, `name`, `price`, `days`, `license_type`, `edition`, `version_type`, `first_only`, `need_pay`, `sort`, `status`, `create_by`, `create_time`, `remark`) VALUES
('micro_lite_free',       '小微版·免费档(仅推广)',  0.00,    0,   'FORMAL', 'MICRO', 'micro_lite', 0, 0, 10, '0', 'admin', NOW(), '永久免费；need_pay=0 不激活计费周期；仅推广发布/店铺管理/活动浏览页'),
('micro_pro_trial',       '小微版·专业档试用(7天)', 0.00,    7,   'TRIAL',  'MICRO', 'micro_pro',  0, 0, 11, '0', 'admin', NOW(), '7 天免费试用；不绑卡、不自动扣款、无隐藏收费'),
('micro_pro_first_month', '小微版·专业档首月特惠',  299.00,  30,  'FORMAL', 'MICRO', 'micro_pro',  1, 1, 12, '0', 'admin', NOW(), '首月 299 元；仅首次可用(first_only=1)；同租户仅一次'),
('micro_pro_monthly',     '小微版·专业档月付',      499.00,  30,  'FORMAL', 'MICRO', 'micro_pro',  0, 1, 13, '0', 'admin', NOW(), '月付 499 元/30 天'),
('micro_pro_yearly',      '小微版·专业档包年',      4790.00, 365, 'FORMAL', 'MICRO', 'micro_pro',  0, 1, 14, '0', 'admin', NOW(), '包年 4790 元(约 8 折：499×12×0.8≈4790)');

-- ----------------------------
-- 6. 验收查询（执行后人工核对）
-- ----------------------------
-- 6.1 tenant_type / version_type 列存在
-- SELECT COLUMN_NAME, COLUMN_DEFAULT, COLUMN_COMMENT FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_tenant' AND COLUMN_NAME IN ('tenant_type','version_type');
--
-- 6.2 存量租户回填正确
-- SELECT id, company_name, edition, tenant_type, version_type FROM sys_tenant ORDER BY id;
-- 预期：edition=BASIC→version_type=basic；ADVANCED→advanced；PROFESSIONAL→pro；MICRO→micro_pro
--
-- 6.3 role_template 分档配方完整
-- SELECT version_type, role_code, JSON_LENGTH(menu_keys) AS menus, JSON_LENGTH(block_keys) AS blocks
--  FROM role_template WHERE version_type IN ('micro_lite','micro_pro') ORDER BY version_type, role_code;
-- 预期：micro_lite 3 行(boss/salesman/customer)；micro_pro 3 行
--
-- 6.4 ★lite 档 boss 无预约/结算/收支/报表/定价 perms
-- SELECT JSON_CONTAINS(menu_keys, '"micro:booking:list"') AS has_booking,
--        JSON_CONTAINS(menu_keys, '"micro:settle:list"')  AS has_settle,
--        JSON_CONTAINS(menu_keys, '"micro:report:weekly"')AS has_report
--  FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
-- 预期：全部 0
--
-- 6.5 ★pro 档为 lite 超集（含经营集合）
-- SELECT JSON_CONTAINS(menu_keys, '"micro:booking:list"') AS has_booking,
--        JSON_CONTAINS(menu_keys, '"micro:settle:list"')  AS has_settle,
--        JSON_CONTAINS(menu_keys, '"micro:customer:unblacklist"') AS has_unblacklist
--  FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：全部 1
--
-- 6.6 分档计费套餐完整
-- SELECT plan_code, name, version_type, price, days, first_only, need_pay FROM sys_billing_plan
--  WHERE plan_code LIKE 'micro\_%' ORDER BY version_type, price;
-- 预期：5 行（micro_lite_free + micro_pro 四档）
--
-- 6.7 ★升级零迁移验证（运行时）：
--   UPDATE sys_tenant SET version_type='micro_pro' WHERE id=<lite租户ID>;
--   → 刷新菜单缓存 → 同租户 boss 登录看到经营菜单扩围；角色 code / 用户 / 业务数据零迁移；lite 期间推广数据保留
