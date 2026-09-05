-- =====================================================================
-- M12_01 业态功能模块建表脚本（T12.1~T12.8）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》v1.12 M12（业态功能模块）
--       《多业态SaaS平台需求规格说明书》v1.0 §5.1 活动管理 / §5.2 海报主题 / §5.3 直播公告
--       《资金管控系统设计文档》v1.3 §1.8.5
-- 前置依赖：
--   M1_05_platform_foundation_tables.sql（tenant_theme 已建：preset_key/logo_url/logo_rendered_url/poster_url/rendered_poster_url）
--   M1_07_project_container_tables.sql（活动数据挂 project 容器 type=activity，报名写 project_signup）
-- 规范：
--   1. 先删后建 / 先删后插，脚本可重复执行（幂等）
--   2. 业务表逐表含 tenant_id + 租户索引；biz_theme_preset 为【系统级共享表】(无 tenant_id)，
--      须纳入 MybatisPlusConfig.IGNORE_TENANT_TABLES
--   3. 活动本身复用 project(type=activity) 容器，本脚本不重复建活动主表，仅建主题预设/直播公告/内容安全日志/异步衍生图任务
--   4. 业态功能为【基础版标配】（全版本可用），菜单三版本模板均登记
-- =====================================================================

-- ----------------------------
-- 1. 主题预设表 biz_theme_preset（T12.2 四套 Design Token，系统级共享）
--    Token 以 CSS 变量下发，PC 与小程序共用；禁止硬编码色值
-- ----------------------------
DROP TABLE IF EXISTS `biz_theme_preset`;
CREATE TABLE `biz_theme_preset` (
  `preset_key` VARCHAR(50) NOT NULL COMMENT '主题预设key：dark_boardgame/warm_party/fresh_japan/neon_cyber',
  `name` VARCHAR(100) NOT NULL COMMENT '主题名称',
  `tokens` JSON COMMENT 'Design Token(主色/辅色/字体/圆角/阴影/渐变，CSS变量键值)',
  `preview_url` VARCHAR(500) DEFAULT NULL COMMENT '主题预览图URL',
  `contrast_ratio` VARCHAR(20) DEFAULT NULL COMMENT '对比度达标标记(如 AA/AAA)',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`preset_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主题预设表(四套Design Token,系统级共享,IGNORE_TENANT_TABLES)';

-- ----------------------------
-- 2. 直播公告表 biz_live_announcement（T12.6 状态机；无直播时接口返回 has_announcement=false）
--    展示态由服务端按 now 与 start_time/end_time 派生：
--    living(排第一) / pending<=30min(临近提醒) / pending>30min(隐藏) / offline<=10min(可进回放) / offline>10min(隐藏)
-- ----------------------------
DROP TABLE IF EXISTS `biz_live_announcement`;
CREATE TABLE `biz_live_announcement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` BIGINT DEFAULT NULL COMMENT '关联活动容器ID(project.id,type=activity)',
  `platform` VARCHAR(30) NOT NULL DEFAULT 'video_account' COMMENT '平台：video_account(视频号,可跳转)/douyin(抖音,仅引流不跳转)',
  `title` VARCHAR(200) NOT NULL COMMENT '直播/公告标题',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `live_url` VARCHAR(500) DEFAULT NULL COMMENT '直播间外链(抖音引流/回放)',
  `room_id` VARCHAR(100) DEFAULT NULL COMMENT '视频号直播间ID(navigateToChannelsLive 跳转用)',
  `anchor` VARCHAR(100) DEFAULT NULL COMMENT '主播/发起人',
  `start_time` DATETIME DEFAULT NULL COMMENT '开播时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '下播时间(实际)',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '业务状态：PENDING(预告)/LIVING(直播中)/OFFLINE(已结束)；展示态由服务端派生',
  `sort` INT DEFAULT 0 COMMENT '排序(living 优先)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播公告表(状态机,T12.6)';

-- ----------------------------
-- 3. 内容安全审核日志表 biz_content_security_log（T12.4 imgSecCheck / T14.4 msgSecCheck，审核不通过一律拒绝并留痕）
-- ----------------------------
DROP TABLE IF EXISTS `biz_content_security_log`;
CREATE TABLE `biz_content_security_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `scene` VARCHAR(40) NOT NULL COMMENT '场景：logo/poster/ugc_image/ugc_text/nickname',
  `content_type` VARCHAR(20) NOT NULL COMMENT '内容类型：image/text',
  `content_ref` VARCHAR(500) DEFAULT NULL COMMENT '内容引用(图片URL/文本摘要,文本脱敏存储)',
  `provider` VARCHAR(40) DEFAULT NULL COMMENT '审核提供方：wechat_imgSecCheck/wechat_msgSecCheck/stub',
  `result` VARCHAR(20) NOT NULL DEFAULT 'PASS' COMMENT '审核结果：PASS/REJECT/ERROR',
  `label` VARCHAR(100) DEFAULT NULL COMMENT '命中标签(涉政/涉黄/广告等)',
  `raw_response` TEXT COMMENT '第三方原始响应(排障用,脱敏)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_scene` (`tenant_id`, `scene`),
  KEY `idx_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容安全审核日志表(imgSecCheck/msgSecCheck留痕)';

-- ----------------------------
-- 4. 异步衍生图任务表 biz_image_task（T12.7 上传即返回，后台生成完成后回写 URL；失败重试与告警）
-- ----------------------------
DROP TABLE IF EXISTS `biz_image_task`;
CREATE TABLE `biz_image_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `theme_id` BIGINT DEFAULT NULL COMMENT '关联 tenant_theme.id(可空)',
  `task_type` VARCHAR(30) NOT NULL COMMENT '任务类型：logo_render(80x80)/poster_card(750x400)/poster_detail(750x1000)',
  `source_url` VARCHAR(500) NOT NULL COMMENT '原图URL',
  `spec` VARCHAR(30) DEFAULT NULL COMMENT '目标规格：80x80/750x400/750x1000',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED',
  `result_url` VARCHAR(500) DEFAULT NULL COMMENT '成品URL(成功后回写 tenant_theme.logo_rendered_url/rendered_poster_url)',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `error_msg` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_theme` (`theme_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步衍生图任务表(Logo/海报成品生成队列)';

-- =====================================================================
-- 5. 四套主题 Design Token 初始数据（T12.2，先删后插幂等）
-- =====================================================================
DELETE FROM `biz_theme_preset` WHERE `preset_key` IN ('dark_boardgame', 'warm_party', 'fresh_japan', 'neon_cyber');
INSERT INTO `biz_theme_preset` (`preset_key`, `name`, `tokens`, `sort`, `status`, `remark`) VALUES
('dark_boardgame', '暗夜桌游',
 JSON_OBJECT('--primary-color','#6C4AB6','--secondary-color','#241B2F','--bg-color','#161322','--text-color','#EDE7F6','--radius','12px','--shadow','0 4px 16px rgba(0,0,0,.45)','--gradient','linear-gradient(135deg,#6C4AB6,#241B2F)','--font-family','"PingFang SC",sans-serif'),
 1, '0', '桌游轰趴业态默认主题'),
('warm_party', '暖光聚会',
 JSON_OBJECT('--primary-color','#F5A623','--secondary-color','#FFF3E0','--bg-color','#FFFBF5','--text-color','#4A3B2A','--radius','16px','--shadow','0 4px 12px rgba(245,166,35,.25)','--gradient','linear-gradient(135deg,#F5A623,#FFCC80)','--font-family','"PingFang SC",sans-serif'),
 2, '0', '聚会/轰趴暖光主题'),
('fresh_japan', '清新日系',
 JSON_OBJECT('--primary-color','#7FB069','--secondary-color','#EAF4E0','--bg-color','#FCFDF9','--text-color','#3A4A34','--radius','10px','--shadow','0 2px 8px rgba(127,176,105,.2)','--gradient','linear-gradient(135deg,#7FB069,#D6E9C6)','--font-family','"PingFang SC",sans-serif'),
 3, '0', '生鲜团购业态默认主题'),
('neon_cyber', '霓虹赛博',
 JSON_OBJECT('--primary-color','#00E5FF','--secondary-color','#FF2D95','--bg-color','#0A0A14','--text-color','#E0F7FA','--radius','8px','--shadow','0 0 12px rgba(0,229,255,.6)','--gradient','linear-gradient(135deg,#00E5FF,#FF2D95)','--font-family','"PingFang SC",sans-serif'),
 4, '0', '赛博霓虹主题');

-- =====================================================================
-- 6. 菜单：活动运营（目录 3440，parent_id=0；menu_id 占用 3440~3479）
-- =====================================================================
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 3440 AND 3479;
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3440, '活动运营', 0, 6, 'activity', NULL, NULL, 1, 0, 'M', '0', '0', '', 'star', 'admin', NOW(), '', NULL, '活动管理/直播公告/主题装修（M12，基础版标配）'),
-- 活动管理（T12.1）
(3441, '活动管理', 3440, 1, 'manage', 'biz/activity/index', NULL, 1, 0, 'C', '0', '0', 'biz:activity:list', 'date', 'admin', NOW(), '', NULL, 'project(type=activity) 创建/发布/报名/签到/结束全流程'),
(3442, '活动查询', 3441, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:activity:query',   '#', 'admin', NOW(), '', NULL, ''),
(3443, '活动新增', 3441, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:activity:add',     '#', 'admin', NOW(), '', NULL, ''),
(3444, '活动编辑', 3441, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:activity:edit',    '#', 'admin', NOW(), '', NULL, ''),
(3446, '活动发布', 3441, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:activity:publish', '#', 'admin', NOW(), '', NULL, ''),
(3447, '活动结束', 3441, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:activity:finish',  '#', 'admin', NOW(), '', NULL, '结束后不可报名'),
-- 直播公告（T12.6）
(3445, '直播公告', 3440, 2, 'announcement', 'biz/announcement/index', NULL, 1, 0, 'C', '0', '0', 'biz:announcement:list', 'monitor', 'admin', NOW(), '', NULL, '直播公告状态机：living/pending/offline 派生展示态'),
(3451, '公告查询', 3445, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:announcement:query',  '#', 'admin', NOW(), '', NULL, ''),
(3452, '公告新增', 3445, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:announcement:add',    '#', 'admin', NOW(), '', NULL, ''),
(3453, '公告编辑', 3445, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:announcement:edit',   '#', 'admin', NOW(), '', NULL, ''),
(3454, '公告删除', 3445, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:announcement:remove', '#', 'admin', NOW(), '', NULL, ''),
-- 主题装修（T12.2/T12.3/T12.4/T12.5）
(3450, '主题装修', 3440, 3, 'theme', 'biz/theme/index', NULL, 1, 0, 'C', '0', '0', 'biz:theme:list', 'color', 'admin', NOW(), '', NULL, 'tenant_theme：主题预设 + Logo/海报管线 + 渲染融合三层预览'),
(3455, '主题查询', 3450, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:theme:query',   '#', 'admin', NOW(), '', NULL, ''),
(3456, '主题保存', 3450, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:theme:edit',    '#', 'admin', NOW(), '', NULL, '选 preset_key + 上传 Logo/海报'),
(3457, '预览渲染', 3450, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:theme:preview', '#', 'admin', NOW(), '', NULL, '三层融合实时预览');

-- ----------------------------
-- 7. 三版本授权模板登记（基础版标配，三版本均含）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` BETWEEN 3440 AND 3479;
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT e.edition, m.menu_id
  FROM (SELECT 'BASIC' AS edition UNION ALL SELECT 'ADVANCED' UNION ALL SELECT 'PROFESSIONAL') e
  CROSS JOIN (SELECT menu_id FROM `sys_menu` WHERE menu_id BETWEEN 3440 AND 3479) m;

-- ----------------------------
-- 8. 角色授权（boss 9201 / manager 9203 店长可运营活动；employee 9204 无运营菜单）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 3440 AND 3479 AND `role_id` IN (9201, 9202, 9203);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9201, 3440), (9201, 3441), (9201, 3442), (9201, 3443), (9201, 3444), (9201, 3446), (9201, 3447),
(9201, 3445), (9201, 3451), (9201, 3452), (9201, 3453), (9201, 3454),
(9201, 3450), (9201, 3455), (9201, 3456), (9201, 3457),
(9203, 3440), (9203, 3441), (9203, 3442), (9203, 3443), (9203, 3444), (9203, 3446), (9203, 3447),
(9203, 3445), (9203, 3451), (9203, 3452), (9203, 3453),
(9203, 3450), (9203, 3455), (9203, 3456), (9203, 3457);

-- ----------------------------
-- 9. biz_bundle.menus 追加活动运营菜单（boardgame/fresh_food 主打活动运营；fund_control 亦可用）
-- ----------------------------
UPDATE `biz_bundle`
   SET `menus` = JSON_MERGE_PRESERVE(COALESCE(`menus`, JSON_ARRAY()), JSON_ARRAY(3440, 3441, 3445, 3450))
 WHERE `biz_code` IN ('fund_control', 'boardgame', 'fresh_food')
   AND NOT JSON_CONTAINS(COALESCE(`menus`, JSON_ARRAY()), '3441');

-- ----------------------------
-- 10. 验收查询（执行后人工核对）
-- ----------------------------
-- 10.1 四套主题 Token 可解析
-- SELECT preset_key, name, JSON_VALID(tokens) FROM biz_theme_preset;  -- 预期 4 行且均为 1
-- 10.2 表均含 tenant_id（biz_theme_preset 除外，系统级共享）
-- SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id'
--   AND TABLE_NAME IN ('biz_live_announcement','biz_content_security_log','biz_image_task');  -- 预期 3 行
-- 10.3 菜单与模板
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3440 AND 3479 ORDER BY menu_id;
