-- =====================================================================
-- M26_01  房间座位布局建表 + service_item 二态字段（T26.1 / T26.2 / T26.3 上传按钮菜单）
-- 版本：v1.0  日期：2026-10-12
-- 依据：《小微版 V3.9 开发文档》M26 房间座位布局与选座 T26.1~T26.3
-- 归属版本：小微版（MICRO，micro_pro 专属；micro_lite 不启用座位布局）
-- 前置依赖：M15_04（service_item）、M15_23（房间=service_item category_type=space）、
--           M15_24（service_item.capacity）、M15_01（BaseEntity 审计列）、
--           M15_06（sys_menu 7000~7499 段位 + 7100 小微运营目录）、
--           M15_07/M15_10（boss 角色模板 micro/micro_pro）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. 新建 room_seat_layout（房间座位布局，一房一份，UNIQUE(tenant_id, room_id)）—— T26.1
--   2. 新建 room_seat（座位点，坐标以背景图百分比存储，UNIQUE(layout_id, seat_no)）—— T26.1
--   3. 新建 room_seat_layout_template（布局模板，Boss 存为模板 / 平台行业默认模板）—— T26.1
--   4. service_item 补 has_seat_layout（房间有无位置二态）+ seat_capacity（可排座位数）—— T26.2
--   5. sys_menu 新增 7280（房间布局配置 C，暂隐藏，T26.4 建页后置显）+ 7281（上传背景图 F）
--      + boss(micro/micro_pro) role_template 追加 micro:room:layout / micro:room:uploadBg
--      + 存量 boss sys_role_menu 补录（★T26.3 上传接口鉴权依赖，micro_lite 不授）
-- ---------------------------------------------------------------------
-- 范围说明：
--   ★本脚本仅覆盖 T26.1（建表）/ T26.2（二态字段）/ T26.3（上传校验所需的最小菜单权限）。
--   ★activity_seat_assignment / activity_seat_operation_log（T26.8 / T28.4）不在本脚本，随 M28_01 落地。
--   ★复制布局/存为模板/平台默认模板/顾客选座菜单（7282/7283/7285/7286/7290）+ FEAT_ROOM_SEAT_LAYOUT
--     功能开关随 M26_02（T26.5 / T26.6）落地；届时 M26_02 先删后插 7280~7299 会幂等重覆盖本脚本菜单。
-- 规范：
--   1. 建表使用 CREATE TABLE IF NOT EXISTS 幂等语义（不用 DROP，保护存量数据）
--   2. ALTER 使用 INFORMATION_SCHEMA 条件判断（PREPARE 动态执行），幂等可重复
--   3. 菜单先删后插幂等；role_template 使用 JSON_CONTAINS 守卫追加；sys_role_menu 使用 INSERT IGNORE
--   4. 新表继承 BaseEntity 审计列（create_by/create_time/update_by/update_time）+ tenant_id 显式声明
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. room_seat_layout 房间座位布局（一个房间一份）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `room_seat_layout` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `room_id` BIGINT NOT NULL COMMENT '房间ID（FK→service_item.id, category_type=space）',
  `bg_image_url` VARCHAR(500) NOT NULL COMMENT '背景图 OSS URL',
  `bg_image_source` VARCHAR(10) NOT NULL DEFAULT 'UPLOAD' COMMENT '背景图来源：UPLOAD=Boss 自传 / PRESET=平台默认模板',
  `layout_width` INT NOT NULL DEFAULT 1920 COMMENT '布局基准宽度（px，坐标百分比换算基准）',
  `layout_height` INT NOT NULL DEFAULT 1080 COMMENT '布局基准高度（px，坐标百分比换算基准）',
  `template_id` BIGINT DEFAULT NULL COMMENT '来源模板ID（复制/应用模板后脱钩，仅留痕）',
  `is_default_template` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否作为该房间默认布局：0=否 1=是',
  `template_name` VARCHAR(100) DEFAULT NULL COMMENT '布局名称（存为模板时使用）',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_room` (`tenant_id`, `room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间座位布局（T26.1，一房一份，坐标百分比基准）';

-- ----------------------------
-- 2. room_seat 座位点（坐标以背景图百分比 0~1 存储，跨端一致）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `room_seat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `layout_id` BIGINT NOT NULL COMMENT '所属布局ID（FK→room_seat_layout.id）',
  `seat_no` VARCHAR(32) NOT NULL COMMENT '座位号（如 A1 / 1 / 前排-左1）',
  `pos_x_percent` DECIMAL(6,4) NOT NULL COMMENT 'X 坐标百分比 0~1',
  `pos_y_percent` DECIMAL(6,4) NOT NULL COMMENT 'Y 坐标百分比 0~1',
  `is_preset` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否预置人员（导游/司机）：0=否 1=是',
  `preset_role` VARCHAR(20) DEFAULT NULL COMMENT '预置角色：guide/driver/assistant',
  `preset_user_id` BIGINT DEFAULT NULL COMMENT '预置人员 user_id',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_layout_seat_no` (`layout_id`, `seat_no`),
  INDEX `idx_tenant_layout` (`tenant_id`, `layout_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间座位点（T26.1，坐标百分比存储，UNIQUE(layout_id, seat_no)）';

-- ----------------------------
-- 3. room_seat_layout_template 布局模板（Boss 存为模板 / 平台行业默认模板）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `room_seat_layout_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID（平台行业默认模板 tenant_id=1）',
  `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `bg_image_url` VARCHAR(500) DEFAULT NULL COMMENT '模板默认背景图 OSS URL',
  `is_industry_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否行业默认模板：0=否（Boss 私有）1=是（平台配置）',
  `industry_code` VARCHAR(50) DEFAULT NULL COMMENT '行业编码（is_industry_default=1 时必填，FK→industry_template.industry_code）',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_name` (`tenant_id`, `name`),
  INDEX `idx_industry_default` (`industry_code`, `is_industry_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间座位布局模板（T26.1，Boss 私有模板 + 平台行业默认模板）';

-- ----------------------------
-- 4. service_item 补 has_seat_layout + seat_capacity 列（T26.2 房间二态，幂等）
--    ★仅 category_type=space 的房间有实际意义；项目/服务项保持默认 0 / NULL
-- ----------------------------
SET @tbl = 'service_item';

SET @col = 'has_seat_layout';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否有座位布局：0=无位置 1=有位置（房间/空间用，T26.2）'' AFTER `capacity`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'seat_capacity';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` INT DEFAULT NULL COMMENT ''可排座位数（has_seat_layout=1 时有效，房间/空间用，T26.2）'' AFTER `has_seat_layout`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 5. sys_menu：房间布局配置 7280（C，暂隐藏）+ 上传背景图 7281（F）
--    ★7280 挂 7100 小微运营（Boss PC）；visible='1' 隐藏，待 T26.4 建 micro/room/layout/index.vue 后置 '0'
--    ★7281 为 T26.3 上传接口 /micro/room/uploadBgImage 的鉴权权限串（micro:room:uploadBg）
--    ★先删后插幂等（7280/7281）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7280, 7281);
DELETE FROM `sys_menu` WHERE `menu_id` IN (7280, 7281);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7280: 房间布局配置（C 页面，Boss PC；★visible='1' 隐藏，T26.4 建页后置 '0'）
(7280, '房间布局配置', 7100, 21, 'roomLayout', 'micro/room/layout/index', 1, 0, 'C', '1', '0', 'micro:room:layout', 'pc', 'edit', 'admin', NOW(), 'T26.4 拖拽定位主页面（暂隐藏，页面建成后置显）'),
-- 7281: 上传背景图（F 按钮，T26.3 上传前置校验接口鉴权）
(7281, '上传背景图', 7280, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:room:uploadBg', 'pc', '#', 'admin', NOW(), 'T26.3 POST /micro/room/uploadBgImage（格式/大小/尺寸前置校验）');

-- ----------------------------
-- 6. role_template：boss(micro_pro) 追加 micro:room:layout / micro:room:uploadBg
--    ★micro_lite 不授（FEAT_ROOM_SEAT_LAYOUT micro_pro 专属，座位布局为付费档能力）
--    ★JSON_CONTAINS 守卫幂等追加
-- ----------------------------
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:layout')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro_pro')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:layout"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:uploadBg')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro_pro')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:uploadBg"');

-- ----------------------------
-- 7. 存量 sys_role_menu 补录：boss（小微版 micro_pro，★排除 micro_lite）
--    ★7280（C 父）+ 7281（F 上传按钮）一并授予，保证 @SaCheckPermission("micro:room:uploadBg") 生效
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND (t.`version_type` IS NULL OR t.`version_type` <> 'micro_lite')
  AND m.`menu_id` IN (7280, 7281);

-- 超管 role_id=1 全量补录（便于平台侧联调）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7280, 7281);

-- ----------------------------
-- 8. 验收查询（执行后人工核对）
-- ----------------------------
-- 8.1 三表已创建
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME IN ('room_seat_layout','room_seat','room_seat_layout_template');
-- 预期：3 行
--
-- 8.2 service_item 二态列已建立
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_item'
--    AND COLUMN_NAME IN ('has_seat_layout','seat_capacity');
-- 预期：2 行（has_seat_layout tinyint(1)/0，seat_capacity int/NULL）
--
-- 8.3 菜单 7280/7281 存在
-- SELECT menu_id, menu_name, parent_id, menu_type, visible, perms, client_type FROM sys_menu
--  WHERE menu_id IN (7280, 7281);
-- 预期：2 行（7280 C visible=1 micro:room:layout / 7281 F micro:room:uploadBg）
--
-- 8.4 role_template boss(micro_pro) 含布局权限，micro_lite 不含
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:room:layout"')   AS has_layout,
--        JSON_CONTAINS(menu_keys,'"micro:room:uploadBg"')  AS has_upload
--   FROM role_template WHERE role_code = 'boss' AND version_type IN ('micro_pro','micro_lite');
-- 预期：micro_pro 为 1/1，micro_lite 为 0/0
--
-- ★执行后请重启后端，清除内存缓存 + 退出重新登录刷新权限快照
