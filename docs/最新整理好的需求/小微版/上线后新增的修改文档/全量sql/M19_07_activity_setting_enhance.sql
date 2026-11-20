-- =====================================================================
-- M19_07  活动设置增强——数据模型扩展 + 菜单 + role_template（v1.0）
-- =====================================================================
-- 依据：《活动功能需求草稿》v0.1
--
-- 需求概述：
--   老板端创建活动时的增强功能：
--     ① 选择项目（单选）/ 房间（单/多选）/ 员工（单/多选）
--     ② 活动标题自动生成（{房间名}玩{项目名}，由{员工名}主持）
--     ③ 背景图上传（仅对当前活动生效，不跨活动复用）
--     ④ 活动介绍语音上传（仅当前活动，约 3 分钟，单向播放）
--     ⑤ 推荐码与微信小程序码生成
--
-- 前置：
--   [26]M15_11（promotion_activity 表）
--   [36]M15_19（promotion_activity 补 logo_url/theme_key）
--   [62]M19_01（micro_lite boss 推广管理菜单）
--   [66]M19_05（boss OSS 上传权限 role_template 修复）
--
-- 幂等：条件 ALTER（INFORMATION_SCHEMA 守卫）+ INSERT IGNORE + JSON_CONTAINS 守卫
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. promotion_activity 补活动设置列
-- =====================================================================

-- 1.1 auto_title —— 自动生成的活动标题（支持手动修改覆盖）
--     拼接规则：{房间名}玩{项目名}，由{员工名}主持
--     多选超 3 个时「首个+等N个」
SET @tbl = 'promotion_activity';
SET @col = 'auto_title';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` VARCHAR(500) DEFAULT NULL COMMENT ''自动生成的活动标题（支持手动修改覆盖）'''),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 bg_image_url —— 背景图片（★仅对当前活动生效，不跨活动复用）
SET @col = 'bg_image_url';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` VARCHAR(500) DEFAULT NULL COMMENT ''背景图片URL（仅对当前活动生效，不跨活动复用）'''),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 voice_url —— 活动介绍语音（约 3 分钟，单向播放，非互动）
SET @col = 'voice_url';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` VARCHAR(500) DEFAULT NULL COMMENT ''活动介绍语音URL（约3分钟，单向播放）'''),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 qrcode_url —— 推荐微信小程序码
SET @col = 'qrcode_url';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` VARCHAR(500) DEFAULT NULL COMMENT ''推荐微信小程序码URL'''),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.5 recommend_scene —— 推荐码 scene（≤32 字符，格式 a=活动ID&e=主会员工ID）
SET @col = 'recommend_scene';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` VARCHAR(32) DEFAULT NULL COMMENT ''推荐码scene（≤32字符，a=活动ID&e=主会员工ID）'''),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.6 selected_project_id —— 选中的项目 ID（service_item.id，排除 space 分类）
SET @col = 'selected_project_id';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` BIGINT DEFAULT NULL COMMENT ''选中的项目ID（service_item，排除space分类）'''),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.7 selected_room_ids —— 选中的房间 ID 列表（JSON 数组，service_item.id，仅 space 分类）
SET @col = 'selected_room_ids';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` JSON DEFAULT NULL COMMENT ''选中的房间ID列表（JSON数组，service_item仅space分类）'''),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.8 selected_staff_ids —— 选中的员工 ID 列表（JSON 数组，tenant_role_mapping 中 salesman 角色用户）
SET @col = 'selected_staff_ids';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` JSON DEFAULT NULL COMMENT ''选中的员工ID列表（JSON数组，salesman角色用户）'''),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 2. 活动设置菜单（挂在 7150 活动管理目录下）
-- =====================================================================
-- 菜单对照：
--   menu_id  perms                          menu_name        menu_type  parent_id
--   7156     micro:activity:setting:list    活动设置         C          7150
--   7157     micro:activity:setting:query   查看设置         F          7156
--   7158     micro:activity:setting:edit    保存设置         F          7156

-- 2.1 活动设置页面（C 类型，挂在 7150 活动管理目录下）
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES (7156, '活动设置', 7150, 5, 'setting', 'micro/activity/setting/index',
    '', 1, 0, 'C', '0', '0', 'micro:activity:setting:list', 'edit', 'admin', NOW(), '活动设置页面（项目/房间/员工选择+自动标题+背景图+推荐码）');

-- 2.2 查看设置（F 类型按钮）
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES (7157, '查看设置', 7156, 1, '', '',
    '', 1, 0, 'F', '0', '0', 'micro:activity:setting:query', '#', 'admin', NOW(), '查看活动设置');

-- 2.3 保存设置（F 类型按钮）
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES (7158, '保存设置', 7156, 2, '', '',
    '', 1, 0, 'F', '0', '0', 'micro:activity:setting:edit', '#', 'admin', NOW(), '保存活动设置');

-- =====================================================================
-- 3. role_template boss 追加活动设置权限
--    ★双层同步：role_template（权限天花板）+ sys_role_menu（运行时实例）
-- =====================================================================

-- 3.1 micro_pro boss 追加 micro:activity:setting:list
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:activity:setting:list')
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:activity:setting:list"');

-- 3.2 micro_lite boss 追加 micro:activity:setting:list（免费版也有活动设置）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:activity:setting:list')
WHERE `version_type` = 'micro_lite'
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:activity:setting:list"');

-- 3.3 追加 micro:activity:setting:edit（保存设置按钮权限）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:activity:setting:edit')
WHERE `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:activity:setting:edit"');

-- =====================================================================
-- 4. 存量租户 boss sys_role_menu 补录（F 类型按钮不渲染侧边栏）
-- =====================================================================

-- 4.1 补录 7156(活动设置,C) + 7157(查看,F) + 7158(保存,F)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN (
    SELECT 7156 AS `menu_id`
    UNION ALL SELECT 7157
    UNION ALL SELECT 7158
) m
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm2
       WHERE rm2.`role_id` = r.`role_id` AND rm2.`menu_id` = m.`menu_id`
  );

-- 4.2 补全 M 类型祖先目录（7156 的祖先链：7156→7150→7100→7000，仅补缺失的）
--     ★注意：F 类型按钮（7157/7158）不触发 M 祖先补全，仅 C 类型 7156 需要
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
WITH RECURSIVE ancestor_tree AS (
    SELECT DISTINCT m.`parent_id` AS ancestor_id
    FROM `sys_role_menu` rm
    JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
    JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
    JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
    WHERE t.`tenant_type` = 'micro'
      AND r.`role_key` = 'boss'
      AND r.`del_flag` = '0'
      AND m.`menu_id` = 7156
      AND m.`parent_id` > 0
    UNION
    SELECT m2.`parent_id`
    FROM ancestor_tree at
    JOIN `sys_menu` m2 ON m2.`menu_id` = at.ancestor_id
    WHERE m2.`parent_id` > 0
)
SELECT rm.`role_id`, at2.`ancestor_id`
FROM ancestor_tree at2
CROSS JOIN (
    SELECT DISTINCT r2.`role_id`
    FROM `sys_role` r2
    JOIN `sys_tenant` t2 ON t2.`id` = r2.`tenant_id`
    WHERE t2.`tenant_type` = 'micro'
      AND r2.`role_key` = 'boss'
      AND r2.`del_flag` = '0'
) rm
WHERE at2.`ancestor_id` > 0
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm2
       WHERE rm2.`role_id` = rm.`role_id` AND rm2.`menu_id` = at2.`ancestor_id`
  );

-- =====================================================================
-- 5. 验证查询
-- =====================================================================

-- 5.1 promotion_activity 新增列就绪
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
--   AND COLUMN_NAME IN ('auto_title','bg_image_url','voice_url','qrcode_url',
--       'recommend_scene','selected_project_id','selected_room_ids','selected_staff_ids');
-- 预期：8 行

-- 5.2 活动设置菜单已落库
-- SELECT menu_id, menu_name, parent_id, menu_type, perms
-- FROM sys_menu WHERE menu_id BETWEEN 7156 AND 7158;
-- 预期：3 行（7156=C, 7157=F, 7158=F）

-- 5.3 role_template boss 含活动设置权限
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"micro:activity:setting:list"') AS has_list,
--        JSON_CONTAINS(menu_keys, '"micro:activity:setting:edit"') AS has_edit
-- FROM role_template
-- WHERE role_code = 'boss' AND version_type IN ('micro','micro_pro','micro_lite');
-- 预期：3 行，has_list=1, has_edit=1

-- 5.4 存量租户 boss 已关联活动设置菜单
-- SELECT t.id, t.company_name,
--        SUM(CASE WHEN rm.menu_id = 7156 THEN 1 ELSE 0 END) AS has_setting,
--        SUM(CASE WHEN rm.menu_id = 7157 THEN 1 ELSE 0 END) AS has_query,
--        SUM(CASE WHEN rm.menu_id = 7158 THEN 1 ELSE 0 END) AS has_edit
-- FROM sys_tenant t
-- JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
-- JOIN sys_role_menu rm ON rm.role_id = r.role_id
-- WHERE t.tenant_type = 'micro'
-- GROUP BY t.id, t.company_name;
-- 预期：每行 has_setting/has_query/has_edit 均为 1
