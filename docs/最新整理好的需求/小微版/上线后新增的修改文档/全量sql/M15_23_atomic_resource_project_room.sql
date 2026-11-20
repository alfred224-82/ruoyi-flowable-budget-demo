-- =====================================================================
-- M15_23 platform_admin 原子资源管理：项目管理 + 房间/空间管理（T15.38，v1.38 新增）
-- 版本：v1.0  日期：2026-09-18
-- 依据：《SaaS 平台需求草稿_最终定稿版 V3.7》§4.2「原子资源管理」（Project/Room/Role/Staff）
--       《资金管控系统开发任务文档（小微版）》v1.31 T15.38
-- 概述：为 platform_admin 补齐「原子资源管理」缺失的两项——项目管理、房间/空间管理，
--       与既有 角色映射(7504)=角色管理、人员管理(7507)=员工库 归入同一「原子资源管理」目录(7510)。
--       ★数据来源：与角色映射自愈同模式，从 admin 维护的行业模板 industry_template.preset_config
--         （projects[] 项目 / rooms[] 房间）在租户首次进入或建租户时自动初始化（幂等仅空表补录）。
-- 变更内容：
--   1. service_category 补 category_type 列（service=服务分类（默认）/ space=房间·空间分类）
--      —— 房间/空间 = space 分类下的 service_item（元/小时按时长计价），零新表，
--         天然兼容 booking_order.service_item_id / price_change_log 改价留痕。
--   2. industry_template.preset_config 补 rooms[] 预设（23 行业各 2~3 间，条件 JSON_SET，仅补空）
--      —— T15.33 验收要求「每行业 ≥2 房间」，admin 端 JSON textarea 无需改 UI 即可维护。
--   3. 菜单：新建「原子资源管理」目录 7510 + 项目管理 7511~7513 + 房间/空间管理 7514~7516；
--      7504(角色映射)/7507(人员管理) re-parent 到 7510（角色管理/员工库）。
--   4. role_template micro_lite/micro_pro platform_admin 追加 6 个新 perms（JSON_CONTAINS 守卫）。
--   5. sys_role_menu：role_id=1 全量 + 存量租户 platform_admin 克隆角色补录（INSERT IGNORE 幂等）。
-- 前置依赖：M15_04（service_category/service_item）、M15_17（platform_admin 菜单 7500~7508）、
--           M15_22（23 行业 preset_config 种子）
-- ★menu_id 区段：7510~7519（避让 M15_17 的 7500~7509、M18 的 7600~7679）
-- ★幂等：条件 ALTER（INFORMATION_SCHEMA 守卫）+ JSON_EXTRACT IS NULL 守卫 + 先删后插 + INSERT IGNORE
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 1. service_category 补 category_type 列（条件 ALTER，幂等）
-- =====================================================================
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_category'
    AND COLUMN_NAME = 'category_type');
SET @sql := IF(@exist = 0,
    'ALTER TABLE `service_category` ADD COLUMN `category_type` VARCHAR(20) NOT NULL DEFAULT ''service'' COMMENT ''分类类型(service=服务分类(默认)/space=房间·空间分类，V3.7 原子资源)'' AFTER `industry_type`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 2. industry_template.preset_config 补 rooms[] 预设（23 行业，条件 JSON_SET 仅补空）
--    结构：rooms[{name, capacity, reference_price}]（reference_price 单位：元/小时）
--    ★幂等守卫：JSON_EXTRACT(preset_config,'$.rooms') IS NULL 才写入，不覆盖 admin 已配置值
-- =====================================================================

SET SQL_SAFE_UPDATES = 0;

-- [1] board_game 桌游（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','小房间','capacity','2-4人','reference_price',50),
  JSON_OBJECT('name','大房间','capacity','6-10人','reference_price',80),
  JSON_OBJECT('name','大厅桌游位','capacity','每位','reference_price',25)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='board_game' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [2] script_kill 剧本杀（2 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','标准剧本房','capacity','4-8人','reference_price',80),
  JSON_OBJECT('name','沉浸换装主题房','capacity','6-10人','reference_price',150)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='script_kill' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [3] chess_room 棋牌室（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','麻将包间','capacity','4-8人','reference_price',40),
  JSON_OBJECT('name','大台位包间','capacity','10-12人','reference_price',60),
  JSON_OBJECT('name','围棋象棋台位','capacity','2-4人','reference_price',20)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='chess_room' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [4] ktv KTV（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','小包厢','capacity','4-6人','reference_price',60),
  JSON_OBJECT('name','中包厢','capacity','8-12人','reference_price',100),
  JSON_OBJECT('name','大包厢Party房','capacity','15-20人','reference_price',200)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='ktv' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [5] esports 电竞馆（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','5v5对战房','capacity','10机位','reference_price',80),
  JSON_OBJECT('name','开黑房','capacity','5人','reference_price',60),
  JSON_OBJECT('name','主机游戏区','capacity','2人','reference_price',40)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='esports' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [6] party_room 轰趴馆（2 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','主题派对房','capacity','10-15人','reference_price',300),
  JSON_OBJECT('name','别墅大厅','capacity','20-30人','reference_price',600)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='party_room' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [7] foot_bath 足浴（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','单人足浴位','capacity','1人','reference_price',30),
  JSON_OBJECT('name','双人足浴间','capacity','2人','reference_price',50),
  JSON_OBJECT('name','VIP足浴房','capacity','4人','reference_price',80)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='foot_bath' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [8] massage 推拿（2 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','单人理疗间','capacity','1人','reference_price',60),
  JSON_OBJECT('name','双人理疗间','capacity','2人','reference_price',100)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='massage' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [9] spa SPA（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','单人SPA间','capacity','1人','reference_price',120),
  JSON_OBJECT('name','双人SPA间','capacity','2人','reference_price',200),
  JSON_OBJECT('name','VIP SPA套房','capacity','2-4人','reference_price',300)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='spa' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [10] beauty_salon 美容美发（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','剪发造型工位','capacity','1人','reference_price',30),
  JSON_OBJECT('name','烫染工位','capacity','1人','reference_price',50),
  JSON_OBJECT('name','面部护理间','capacity','1-2人','reference_price',80)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='beauty_salon' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [11] nail_lash 美甲美睫（2 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','美甲工位','capacity','1人','reference_price',30),
  JSON_OBJECT('name','美睫床','capacity','单人','reference_price',50)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='nail_lash' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [12] laundry 洗衣洗鞋（3 间，作业区无计时价）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','前台接待区','capacity','—','reference_price',0),
  JSON_OBJECT('name','洗护作业间','capacity','—','reference_price',0),
  JSON_OBJECT('name','皮具护理间','capacity','—','reference_price',0)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='laundry' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [13] pet_grooming 宠物美容（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','小型犬美容位','capacity','1只','reference_price',40),
  JSON_OBJECT('name','猫咪洗护间','capacity','1只','reference_price',60),
  JSON_OBJECT('name','大型犬美容位','capacity','1只','reference_price',50)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='pet_grooming' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [14] gym 健身房（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','团操房','capacity','10-20人','reference_price',100),
  JSON_OBJECT('name','私教区','capacity','1v1','reference_price',150),
  JSON_OBJECT('name','自由力量区','capacity','每机位','reference_price',30)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='gym' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [15] yoga 瑜伽馆（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','大团瑜伽房','capacity','8-15人','reference_price',80),
  JSON_OBJECT('name','空中瑜伽房','capacity','6-10人','reference_price',100),
  JSON_OBJECT('name','私教瑜伽房','capacity','1v1','reference_price',150)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='yoga' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [16] pilates 普拉提（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','垫上团课房','capacity','8-12人','reference_price',80),
  JSON_OBJECT('name','器械房','capacity','4-6人','reference_price',120),
  JSON_OBJECT('name','私教普拉提房','capacity','1v1','reference_price',200)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='pilates' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [17] badminton 羽毛球馆（2 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','标准场地','capacity','2-4人','reference_price',40),
  JSON_OBJECT('name','比赛场地','capacity','2-4人','reference_price',60)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='badminton' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [18] tennis 网球馆（2 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','室外硬地场','capacity','2-4人','reference_price',120),
  JSON_OBJECT('name','室内场','capacity','2-4人','reference_price',180)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='tennis' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [19] swimming 游泳馆（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','标准泳道','capacity','每道','reference_price',50),
  JSON_OBJECT('name','私人小泳池','capacity','4-6人','reference_price',200),
  JSON_OBJECT('name','亲子池','capacity','2大2小','reference_price',100)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='swimming' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [20] dance 舞蹈室（2 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','大团舞房','capacity','10-20人','reference_price',80),
  JSON_OBJECT('name','私教舞房','capacity','1v1','reference_price',150)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='dance' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [21] photo_studio 摄影写真（3 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','证件照摄影间','capacity','1-2人','reference_price',50),
  JSON_OBJECT('name','室内摄影棚','capacity','3-6人','reference_price',150),
  JSON_OBJECT('name','服装化妆间','capacity','—','reference_price',80)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='photo_studio' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [22] barber_studio 理发工作室（2 间）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','男士剪发位','capacity','1人','reference_price',40),
  JSON_OBJECT('name','烫染位','capacity','1人','reference_price',60)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='barber_studio' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

-- [23] housekeeping 家政保洁团队（2 间，无计时价）
UPDATE `industry_template` SET `preset_config` = JSON_SET(`preset_config`, '$.rooms', JSON_ARRAY(
  JSON_OBJECT('name','装备存放间','capacity','—','reference_price',0),
  JSON_OBJECT('name','接待会议室','capacity','—','reference_price',0)
)), `update_by`='admin', `update_time`=NOW()
WHERE `industry_code`='housekeeping' AND JSON_EXTRACT(`preset_config`,'$.rooms') IS NULL;

SET SQL_SAFE_UPDATES = 1;

-- =====================================================================
-- 3. 菜单：原子资源管理目录 7510 + 项目管理 7511~7513 + 房间/空间管理 7514~7516
--    先删后插幂等（7510~7519 区段）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7510 AND 7519;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 7510 AND 7519;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 二级目录：原子资源管理（V3.7 §4.2，挂 7500 租户管理菜单下）
(7510, '原子资源管理', 7500, 2, 'atomicResource', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'component', 'admin', NOW(), '★V3.7 原子资源管理：项目管理/房间空间管理/角色管理/员工库（platform_admin 专属）'),
-- 菜单一：项目管理（Project 原子资源，service_item 排除空间分类项）
(7511, '项目管理', 7510, 1, 'project', 'micro/platform/project/index', NULL, 1, 0, 'C', '0', '0', 'micro:platform:project', 'pc', 'shopping', 'admin', NOW(), '维护本租户「项目」原子资源，首次进入按行业模板 preset_config.projects 自愈初始化'),
(7512, '项目查询', 7511, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:platform:project:query', 'pc', '#', 'admin', NOW(), ''),
(7513, '项目编辑', 7511, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:platform:project:edit', 'pc', '#', 'admin', NOW(), ''),
-- 菜单二：房间/空间管理（Room 原子资源，space 分类下的 service_item）
(7514, '房间/空间管理', 7510, 2, 'room', 'micro/platform/room/index', NULL, 1, 0, 'C', '0', '0', 'micro:platform:room', 'pc', 'build', 'admin', NOW(), '维护本租户「房间/空间」原子资源，首次进入按行业模板 preset_config.rooms 自愈初始化'),
(7515, '房间查询', 7514, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:platform:room:query', 'pc', '#', 'admin', NOW(), ''),
(7516, '房间编辑', 7514, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:platform:room:edit', 'pc', '#', 'admin', NOW(), '');

-- re-parent：角色映射(7504)=角色管理、人员管理(7507)=员工库 归入 7510 原子资源管理
UPDATE `sys_menu` SET `parent_id` = 7510, `order_num` = 3 WHERE `menu_id` = 7504;
UPDATE `sys_menu` SET `parent_id` = 7510, `order_num` = 4 WHERE `menu_id` = 7507;
-- 小程序主题(7508) 仍挂 7500，顺序置于 原子资源管理(7510,order2) 之后
UPDATE `sys_menu` SET `order_num` = 3 WHERE `menu_id` = 7508;

-- =====================================================================
-- 4. 授予超管 role_id=1（7510~7516 全量）
-- =====================================================================
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 7510 AND 7519;

-- =====================================================================
-- 5. 更新 platform_admin 角色模板（micro_lite/micro_pro 追加 6 个新 perms，JSON_CONTAINS 守卫）
-- =====================================================================
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`,
      '$', 'micro:platform:project',
      '$', 'micro:platform:project:query',
      '$', 'micro:platform:project:edit',
      '$', 'micro:platform:room',
      '$', 'micro:platform:room:query',
      '$', 'micro:platform:room:edit'),
    `update_by` = 'admin', `update_time` = NOW()
WHERE `version_type` IN ('micro_lite', 'micro_pro')
  AND `role_code` = 'platform_admin'
  AND JSON_CONTAINS(`menu_keys`, '"micro:platform:project"') = 0;

-- =====================================================================
-- 6. 存量租户 platform_admin 克隆角色补写 sys_role_menu（幂等 INSERT IGNORE）
-- =====================================================================
-- 6.1 通过 role_template.menu_keys → sys_menu.perms 匹配（C/F 类型菜单）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type`
  AND rt.`role_code` = 'platform_admin' AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON m.`perms` = j.`perm`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` IS NOT NULL AND t.`version_type` != ''
  AND m.`menu_id` BETWEEN 7510 AND 7519
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`);

-- 6.2 ★M 类型目录 7510 的 perms 为空，不会被 menu_keys→perms 匹配，
--     但 selectMenuTreeByUserId 依赖 sys_role_menu 找到中间目录节点才能组装完整树。
--     若不补录，子菜单 7511~7516（及 re-parent 的 7504/7507）成为孤儿，侧边栏不显示。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 7510
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin'
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 7510);

-- =====================================================================
-- 7. 验收查询
-- =====================================================================
-- A. service_category.category_type 列已建立
-- SELECT COLUMN_NAME, DATA_TYPE, COLUMN_DEFAULT FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='service_category' AND COLUMN_NAME='category_type';
-- 预期：1 行（varchar / service）
--
-- B. 23 行业 preset_config 均含 rooms 数组（≥2 间）
-- SELECT industry_code, JSON_LENGTH(JSON_EXTRACT(preset_config,'$.rooms')) AS room_count
--   FROM industry_template WHERE status='0' ORDER BY sort_order;
-- 预期：23 行，room_count >= 2
--
-- C. 原子资源管理菜单目录 + 子菜单存在
-- SELECT menu_id, menu_name, parent_id, perms, menu_type FROM sys_menu WHERE menu_id BETWEEN 7510 AND 7516 ORDER BY menu_id;
-- 预期：7 行（7510 目录 + 7511/7514 两 C 菜单 + 7512/7513/7515/7516 四 F 按钮）
--
-- D. 7504/7507 已 re-parent 到 7510
-- SELECT menu_id, menu_name, parent_id, order_num FROM sys_menu WHERE menu_id IN (7504,7507,7508) ORDER BY menu_id;
-- 预期：7504 parent=7510 / 7507 parent=7510 / 7508 parent=7500
--
-- E. role_template 含新 perms
-- SELECT JSON_CONTAINS(menu_keys,'"micro:platform:project"') AS has_project,
--        JSON_CONTAINS(menu_keys,'"micro:platform:room"') AS has_room
--   FROM role_template WHERE version_type='micro_lite' AND role_code='platform_admin';
-- 预期：1/1
--
-- F. 存量租户 platform_admin 已关联 7510~7516
-- SELECT t.id, t.company_name, COUNT(rm.menu_id) AS atomic_menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='platform_admin'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id AND rm.menu_id BETWEEN 7510 AND 7516
--  WHERE t.tenant_type='micro' GROUP BY t.id, t.company_name;
-- 预期：atomic_menu_count = 7
