-- =====================================================================
-- M15_02 小微版行业模板建表脚本（T15.2，v1.13 新增）
-- 版本：v1.0  日期：2026-09-07
-- 依据：《小微版完整方案》v10.0 第三章（行业模板·小微版专属）/ 第十六章
--       《资金管控系统开发任务文档》v1.13 T15.2
--       《多业态SaaS平台需求规格说明书》v1.0 第 13 章（行业模板）
-- 前置依赖：M15_01（role_template）、M2_01（标准角色集 sys_role）
-- 规范：
--   1. 先删后建 / 先删后插，脚本可重复执行（幂等）
--   2. industry_template / industry_role_map 均为【系统级共享表】（无 tenant_id），
--      须纳入 MybatisPlusConfig.IGNORE_TENANT_TABLES
--   3. 行业模板为小微版专属：企业版无需行业模板（角色由平台管理员直接映射）
--   4. 新增行业只需配行业模板（industry_template + industry_role_map + 预置数据），小程序零改动
--   5. industry_role_map：业务角色名（助教/配送员/教练/技师）→ 标准角色 code（boss/salesman/customer）
-- =====================================================================

-- ----------------------------
-- 1. 行业模板表 industry_template（系统级共享，小微版专属）
-- ----------------------------
DROP TABLE IF EXISTS `industry_template`;
CREATE TABLE `industry_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `industry_code` VARCHAR(50) NOT NULL COMMENT '行业编码(board_game/market/gym/spa...)',
  `industry_name` VARCHAR(100) NOT NULL COMMENT '行业名称(桌游店/买菜/健身房/汤泉)',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '行业图标',
  `preset_config` JSON COMMENT '预置数据(服务分类示例/结算规则示例/单位库示例/默认页面配置)',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_industry_code` (`industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业模板表(系统级共享,IGNORE_TENANT_TABLES;小微版专属)';

-- ----------------------------
-- 2. 行业角色映射表 industry_role_map（业务角色名 → 标准角色 code）
-- ----------------------------
DROP TABLE IF EXISTS `industry_role_map`;
CREATE TABLE `industry_role_map` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `industry_code` VARCHAR(50) NOT NULL COMMENT '行业编码(FK→industry_template.industry_code)',
  `biz_role_name` VARCHAR(64) NOT NULL COMMENT '业务角色显示名(桌游老板/助教/顾客/配送员/教练/技师)',
  `role_code` VARCHAR(64) NOT NULL COMMENT '映射到的标准角色 code(sys_role.role_key：boss/salesman/customer)',
  `default_flag` TINYINT DEFAULT 0 COMMENT '是否该行业默认启用角色(0否 1是)',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_industry` (`industry_code`),
  UNIQUE KEY `uk_industry_bizrole` (`industry_code`, `biz_role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业角色映射表(系统级共享;业务角色名→标准角色code)';

-- =====================================================================
-- 3. 行业模板初始数据（先删后插幂等，内置 4 个行业）
-- =====================================================================
DELETE FROM `industry_template` WHERE `industry_code` IN ('board_game', 'market', 'gym', 'spa');
INSERT INTO `industry_template` (`industry_code`, `industry_name`, `icon`, `preset_config`, `sort_order`, `status`, `create_by`, `remark`) VALUES
('board_game', '桌游店', 'game',    JSON_OBJECT('service_categories', JSON_ARRAY('房间','助教','附加'), 'settle_rule', '按单量周结', 'units', JSON_ARRAY('小时','局','人')), 1, '0', 'admin', '桌游/剧本杀：房间按时长 + 助教按单提成'),
('market',     '买菜/生鲜', 'shop',  JSON_OBJECT('service_categories', JSON_ARRAY('蔬菜','肉菜','水产','套餐'), 'settle_rule', '按配送单结', 'units', JSON_ARRAY('斤','份','箱')), 2, '0', 'admin', '社区团购/生鲜：今日团购 + 配送结算'),
('gym',        '健身房', 'peoples',  JSON_OBJECT('service_categories', JSON_ARRAY('课程','私教','会员卡'), 'settle_rule', '按课时月结', 'units', JSON_ARRAY('节','月','次')), 3, '0', 'admin', '健身/瑜伽：课程收入 + 教练结算'),
('spa',        '汤泉/美容', 'star',  JSON_OBJECT('service_categories', JSON_ARRAY('项目','套餐','产品'), 'settle_rule', '按单提成', 'units', JSON_ARRAY('次','分钟','套')), 4, '0', 'admin', '汤泉/美容/理发：项目预约 + 技师提成');

-- =====================================================================
-- 4. 行业角色映射初始数据（业务角色名 → 标准角色 code）
-- =====================================================================
DELETE FROM `industry_role_map` WHERE `industry_code` IN ('board_game', 'market', 'gym', 'spa');
INSERT INTO `industry_role_map` (`industry_code`, `biz_role_name`, `role_code`, `default_flag`, `sort_order`) VALUES
-- 桌游店：老板 / 助教 / 顾客
('board_game', '桌游老板', 'boss',     1, 1),
('board_game', '助教',     'salesman', 1, 2),
('board_game', '顾客',     'customer', 1, 3),
-- 买菜/生鲜：店主 / 配送员 / 顾客
('market',     '店主',     'boss',     1, 1),
('market',     '配送员',   'salesman', 1, 2),
('market',     '顾客',     'customer', 1, 3),
-- 健身房：老板 / 教练 / 会员
('gym',        '老板',     'boss',     1, 1),
('gym',        '教练',     'salesman', 1, 2),
('gym',        '会员',     'customer', 1, 3),
-- 汤泉/美容：老板 / 技师 / 顾客
('spa',        '老板',     'boss',     1, 1),
('spa',        '技师',     'salesman', 1, 2),
('spa',        '顾客',     'customer', 1, 3);

-- ----------------------------
-- 5. 验收查询（执行后人工核对）
-- ----------------------------
-- 5.1 4 个行业模板就绪
-- SELECT industry_code, industry_name, status FROM industry_template ORDER BY sort_order;  -- 预期 4 行
--
-- 5.2 每个行业 3 条角色映射（boss/salesman/customer）
-- SELECT industry_code, COUNT(*) FROM industry_role_map GROUP BY industry_code;  -- 预期各 3
--
-- 5.3 业务角色名正确映射到标准角色 code
-- SELECT industry_code, biz_role_name, role_code FROM industry_role_map WHERE industry_code='board_game';
-- 预期：桌游老板→boss、助教→salesman、顾客→customer
--
-- 5.4 ★代码侧：industry_template / industry_role_map 须加入 MybatisPlusConfig.IGNORE_TENANT_TABLES
