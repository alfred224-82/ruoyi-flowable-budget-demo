-- =====================================================================
-- M1_05 多业态平台底座建表脚本（T1.7，v1.12 新增）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》v1.12 T1.7
--       《资金管控系统设计文档》v1.3 §1.8.1（三层架构）/ §1.8.3（feature_flag 表驱动）/ §5.1（33 项开关清单 组 A~D）
--       《多业态SaaS平台需求规格说明书》v1.0 §2.1（公共底座层 / 业态包层 / 租户实例层）
-- 规范：
--   1. 先删后建，脚本可重复执行（幂等）
--   2. 业务表逐表含 tenant_id + 租户索引（全局多租户硬约束，设计文档 1.2）
--   3. biz_bundle / feature_flag 为【系统级共享表】，不含 tenant_id，
--      须纳入 MybatisPlusConfig.IGNORE_TENANT_TABLES（与 fee_expense_category 同策略，见 M5_06）
--   4. 内置 3 个业态包（fund_control / boardgame / fresh_food）+ 33 项开关（组 A~D）
--   5. 三维能力模型：有效功能 = feature_flag(edition) ∩ biz_bundle.feature_flags ∩ License ∪/− sys_tenant.features
-- =====================================================================

-- ----------------------------
-- 1. 业态包表 biz_bundle（系统级共享，一行一业态；新增业态仅需插数据，不动核心表结构）
-- ----------------------------
DROP TABLE IF EXISTS `biz_bundle`;
CREATE TABLE `biz_bundle` (
  `biz_code` VARCHAR(50) NOT NULL COMMENT '业态包编码：fund_control/boardgame/fresh_food',
  `name` VARCHAR(100) NOT NULL COMMENT '业态包名称',
  `menus` JSON COMMENT '业态菜单声明（menu_id 数组，叠加在版本授权模板之上做业态过滤）',
  `copy` JSON COMMENT '文案映射（key→业态文案，前端 t(key) 取值，缺失回落默认包）',
  `schema_blocks` JSON COMMENT 'Schema 页面区块声明（声明式表单/列表渲染，PC 与小程序共用）',
  `engine_strategy` VARCHAR(100) COMMENT '组队/成团引擎策略Bean名：FullHouseStrategy/GroupBuyStrategy（策略模式分发，引擎源码禁止 if(bizCode==)）',
  `feature_flags` JSON COMMENT '业态包声明的开关编码数组（未声明的能力不开放，参与三维交集）',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业态包表(系统级共享,IGNORE_TENANT_TABLES)';

-- ----------------------------
-- 2. 功能开关表 feature_flag（系统级共享，表驱动开关；EditionFeatureConfig 保留为兜底常量）
-- ----------------------------
DROP TABLE IF EXISTS `feature_flag`;
CREATE TABLE `feature_flag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(60) NOT NULL COMMENT '开关编码（FEAT_*，与设计文档 5.1 一致）',
  `name` VARCHAR(100) NOT NULL COMMENT '开关名称',
  `flag_group` VARCHAR(10) COMMENT '开关分组：A(资金管控下移11)/B(高级版原有9)/C(多业态底座新增6)/D(专业版原有7)',
  `biz_code` VARCHAR(50) DEFAULT NULL COMMENT '所属业态包（NULL=全业态通用）',
  `basic_enabled` TINYINT DEFAULT 0 COMMENT '基础版默认是否开启（0否 1是）',
  `advanced_enabled` TINYINT DEFAULT 0 COMMENT '高级版默认是否开启（0否 1是）',
  `pro_enabled` TINYINT DEFAULT 0 COMMENT '专业版默认是否开启（0否 1是）',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0启用 1停用；platform_admin 可启停，不可改版本归属以免串版本）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flag_code` (`code`),
  KEY `idx_biz_code` (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能开关表(系统级共享,表驱动,IGNORE_TENANT_TABLES)';

-- ----------------------------
-- 3. 单店覆盖表 tenant_biz_config（module_overrides 覆盖模块开关/文案/字段可见性，禁止 fork 业态包）
-- ----------------------------
DROP TABLE IF EXISTS `tenant_biz_config`;
CREATE TABLE `tenant_biz_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `module_overrides` JSON COMMENT '单店覆盖配置（模块开关/文案/字段可见性；合并优先级=租户覆盖>业态包默认）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户业态单店覆盖表';

-- ----------------------------
-- 4. 租户主题表 tenant_theme（Logo/海报成品与主题预设；配合 M12 Design Token 与图片处理管线）
-- ----------------------------
DROP TABLE IF EXISTS `tenant_theme`;
CREATE TABLE `tenant_theme` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `activity_id` BIGINT DEFAULT NULL COMMENT '活动ID（NULL=租户级默认主题，非NULL=活动级主题）',
  `preset_key` VARCHAR(50) COMMENT '主题预设：dark_boardgame/warm_party/fresh_japan/neon_cyber（T12.2 四套 Design Token）',
  `logo_url` VARCHAR(500) COMMENT 'Logo 原图URL（必须透明底 PNG）',
  `logo_rendered_url` VARCHAR(500) COMMENT 'Logo 成品URL（自动加衬底，输出 80×80px）',
  `poster_url` VARCHAR(500) COMMENT '海报原图URL',
  `rendered_poster_url` VARCHAR(500) COMMENT '海报成品URL（750×400 列表卡 / 750×1000 详情，叠加渐变遮罩）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_activity` (`tenant_id`, `activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户主题表(Logo/海报成品与主题预设)';

-- ----------------------------
-- 5. 用户租户绑定表 user_tenant_bind（openid 多租户归属；登录必须显式选定，禁止按 openid 自动猜测）
-- ----------------------------
DROP TABLE IF EXISTS `user_tenant_bind`;
CREATE TABLE `user_tenant_bind` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `openid` VARCHAR(100) NOT NULL COMMENT '微信 openid',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '绑定的系统用户ID（可空=仅引导态绑定，登录后回填）',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认门店（0否 1是；多绑定时登录页仍须显式选定）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_openid` (`openid`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_openid_tenant` (`openid`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户租户绑定表(openid 多租户归属)';

-- =====================================================================
-- 6. 内置 3 个业态包初始数据（先删后插幂等）
--    feature_flags 声明本业态开放的开关；未声明的能力即使版本/License 授权也不开放（三维交集）
-- =====================================================================
DELETE FROM `biz_bundle` WHERE `biz_code` IN ('fund_control', 'boardgame', 'fresh_food');
INSERT INTO `biz_bundle`
  (`biz_code`, `name`, `menus`, `copy`, `schema_blocks`, `engine_strategy`, `feature_flags`, `status`, `remark`) VALUES
-- 6.1 资金管控业态包（本设计文档全部能力归属此包）
('fund_control', '资金管控',
 JSON_ARRAY(2000,3000,3201,3202,3203,3206,3207,3210,3004),
 JSON_OBJECT('project','项目','expense','报销','income','收入确认','dashboard','老板看板'),
 JSON_ARRAY('fund_project_form','fund_expense_form','fund_income_form'),
 NULL,
 JSON_ARRAY('FEAT_BUDGET_PREPARE','FEAT_SUBJECT_3LAYER','FEAT_EXPENSE_CONTROL','FEAT_RULE_ENGINE_BASE','FEAT_INVOICE_VERIFY','FEAT_INVOICE_SUPPLEMENT','FEAT_BAD_DEBT','FEAT_LINK_RULE','FEAT_HIDDEN_COST','FEAT_BOSS_DASHBOARD','FEAT_VOUCHER_EXPORT','FEAT_TIME_CLOCK','FEAT_CREDIT_SCORE','FEAT_DEPT_BUDGET','FEAT_ELASTIC_QUOTA','FEAT_RULE_ENGINE_CUSTOM','FEAT_PROFIT_REPORT','FEAT_EXPORT_TEMPLATE_CUSTOM','FEAT_EVIDENCE_CHAIN','FEAT_LINK_RULE_CUSTOM','FEAT_FLOW_MULTI_LEVEL','FEAT_OWN_MINIAPP','FEAT_WECOM','FEAT_FLOW_CUSTOM','FEAT_MULTI_ACCOUNT_SET','FEAT_OPEN_API','FEAT_FACE_RECOGNITION','FEAT_MULTI_ORG','FEAT_ORG_SNAPSHOT','FEAT_EXPORT_2D','FEAT_RULE_GRAYSCALE','FEAT_MEMBER_LOAD','FEAT_ISOLATED_SCHEMA'),
 '0', '资金管控业态包：预算编制/科目三层/报销管控/发票验真/烂账/隐形成本/看板/凭证导出等全部能力'),
-- 6.2 桌游轰趴业态包（基础版典型业态，主打活动/拼桌）
('boardgame', '桌游轰趴',
 JSON_ARRAY(3000,3201),
 JSON_OBJECT('project','桌局/活动','expense','支出','income','收款','signup','报名','team','拼桌'),
 JSON_ARRAY('activity_form','signup_form','fullhouse_board'),
 'FullHouseStrategy',
 JSON_ARRAY('FEAT_FLOW_MULTI_LEVEL','FEAT_OWN_MINIAPP','FEAT_WECOM','FEAT_FLOW_CUSTOM','FEAT_OPEN_API','FEAT_FACE_RECOGNITION','FEAT_ISOLATED_SCHEMA'),
 '0', '桌游轰趴业态包：活动管理/拼桌成团（满员即成团、超时解散）/海报主题/直播公告'),
-- 6.3 生鲜团购业态包（基础版典型业态，主打团购/达量成团）
('fresh_food', '生鲜团购',
 JSON_ARRAY(3000,3201),
 JSON_OBJECT('project','团购/批次','expense','进货支出','income','销售收入','signup','下单','team','团购'),
 JSON_ARRAY('groupbuy_form','signup_form','fresh_product_block'),
 'GroupBuyStrategy',
 JSON_ARRAY('FEAT_FLOW_MULTI_LEVEL','FEAT_OWN_MINIAPP','FEAT_WECOM','FEAT_FLOW_CUSTOM','FEAT_OPEN_API','FEAT_FACE_RECOGNITION','FEAT_ISOLATED_SCHEMA'),
 '0', '生鲜团购业态包：团购活动/达量成团（阶梯价）/产地保鲜期字段进 extra_config');

-- =====================================================================
-- 7. 33 项开关初始数据（设计文档 5.1 组 A~D，先删后插幂等）
--    basic/advanced/pro 三列 = 版本默认开启集（feature_flag 表驱动，T4.2/T10.3）
-- =====================================================================
DELETE FROM `feature_flag`;
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `status`, `remark`) VALUES
-- 组 A：资金管控下移开关（11 项，v1.3 新增；原基础版能力→高级版起，均属 fund_control 业态）
('FEAT_BUDGET_PREPARE',        '预算编制闭环+预算调整+公司级预算',        'A', 'fund_control', 0, 1, 1, '0', 'T5.14/T5.15'),
('FEAT_SUBJECT_3LAYER',        '科目三层架构+费用大类主数据+财务科目导入', 'A', 'fund_control', 0, 1, 1, '0', 'T5.13/T5.16'),
('FEAT_EXPENSE_CONTROL',       '报销管控+大类硬拦截+三级预警+追溯时间线',   'A', 'fund_control', 0, 1, 1, '0', 'T5.17'),
('FEAT_RULE_ENGINE_BASE',      '规则引擎(内置关键字规则)',                 'A', 'fund_control', 0, 1, 1, '0', 'T5.18'),
('FEAT_INVOICE_VERIFY',        '发票 OCR+验真+金额比对',                   'A', 'fund_control', 0, 1, 1, '0', 'T5.18'),
('FEAT_INVOICE_SUPPLEMENT',    '无发票后补通道',                           'A', 'fund_control', 0, 1, 1, '0', 'T5.8'),
('FEAT_BAD_DEBT',              '烂账标记+坏账计提',                        'A', 'fund_control', 0, 1, 1, '0', 'T5.6'),
('FEAT_LINK_RULE',             '项目关联规则(内置三层)',                   'A', 'fund_control', 0, 1, 1, '0', 'T5.7'),
('FEAT_HIDDEN_COST',           '隐形成本四项(薪资/折旧/分成/利息)',        'A', 'fund_control', 0, 1, 1, '0', 'T5.9'),
('FEAT_BOSS_DASHBOARD',        '老板看板+利润基础展示+固定成本卡片',        'A', 'fund_control', 0, 1, 1, '0', 'T5.10'),
('FEAT_VOUCHER_EXPORT',        '凭证导出(内置 7 套模板)',                  'A', 'fund_control', 0, 1, 1, '0', 'T5.11'),
-- 组 B：高级版原有增量（9 项，编码不变）
('FEAT_TIME_CLOCK',            '打卡定位',                                 'B', 'fund_control', 0, 1, 1, '0', 'T6.2'),
('FEAT_CREDIT_SCORE',          '信用体系',                                 'B', 'fund_control', 0, 1, 1, '0', 'T6.3'),
('FEAT_DEPT_BUDGET',           '部门级预算',                               'B', 'fund_control', 0, 1, 1, '0', 'T6.1'),
('FEAT_ELASTIC_QUOTA',         '弹性额度',                                 'B', 'fund_control', 0, 1, 1, '0', 'T6.1'),
('FEAT_RULE_ENGINE_CUSTOM',    '规则引擎自定义+影响分析',                  'B', 'fund_control', 0, 1, 1, '0', 'T6.4'),
('FEAT_PROFIT_REPORT',         '项目利润报表',                             'B', 'fund_control', 0, 1, 1, '0', 'T6.5'),
('FEAT_EXPORT_TEMPLATE_CUSTOM','自定义凭证模板',                           'B', 'fund_control', 0, 1, 1, '0', 'T6.6'),
('FEAT_EVIDENCE_CHAIN',        '证据链分级存储',                           'B', 'fund_control', 0, 1, 1, '0', 'T6.7'),
('FEAT_LINK_RULE_CUSTOM',      '项目关联规则自定义',                       'B', 'fund_control', 0, 1, 1, '0', 'T6.7'),
-- 组 C：多业态底座新增（6 项，v1.3 新增；通道/组织类多为全业态通用 biz_code=NULL）
('FEAT_FLOW_MULTI_LEVEL',      '完整多级审批流(按金额/部门/职级条件)',      'C', NULL,           0, 1, 1, '0', 'T13.4'),
('FEAT_OWN_MINIAPP',           '独立小程序(客户自有 AppID)',               'C', NULL,           0, 1, 1, '0', 'T8.11'),
('FEAT_WECOM',                 '企业微信自建应用+OAuth 免登',              'C', NULL,           0, 1, 1, '0', 'T13.2'),
('FEAT_FLOW_CUSTOM',           '自定义流程设计器',                         'C', NULL,           0, 0, 1, '0', 'T13.5'),
('FEAT_MULTI_ACCOUNT_SET',     '多账套',                                   'C', 'fund_control', 0, 0, 1, '0', 'T7.7'),
('FEAT_OPEN_API',              '开放平台 API',                             'C', NULL,           0, 0, 1, '0', 'T7.8/T13.5'),
-- 组 D：专业版原有增量（7 项，编码不变）
('FEAT_FACE_RECOGNITION',      '人脸比对',                                 'D', NULL,           0, 0, 1, '0', 'T7.2'),
('FEAT_MULTI_ORG',             '多组织',                                   'D', 'fund_control', 0, 0, 1, '0', 'T7.1'),
('FEAT_ORG_SNAPSHOT',          '组织版本快照',                             'D', 'fund_control', 0, 0, 1, '0', 'T7.1'),
('FEAT_EXPORT_2D',             '项目-科目二维导出',                        'D', 'fund_control', 0, 0, 1, '0', 'T7.3'),
('FEAT_RULE_GRAYSCALE',        '规则灰度+AI 语义',                         'D', 'fund_control', 0, 0, 1, '0', 'T7.4'),
('FEAT_MEMBER_LOAD',           '成员负载预警',                             'D', 'fund_control', 0, 0, 1, '0', 'T7.5'),
('FEAT_ISOLATED_SCHEMA',       '物理隔离(预留)',                           'D', NULL,           0, 0, 1, '0', 'T7.6');

-- =====================================================================
-- 8. 验收查询（执行后人工核对）
-- =====================================================================
-- 8.1 业态包 3 条，feature_flags JSON 可解析
-- SELECT biz_code, name, engine_strategy, JSON_LENGTH(feature_flags) AS flag_cnt FROM biz_bundle;
-- 预期：fund_control flag_cnt=33；boardgame/fresh_food flag_cnt=7
--
-- 8.2 开关共 33 项，分组数量 A=11 / B=9 / C=6 / D=7
-- SELECT flag_group, COUNT(*) FROM feature_flag GROUP BY flag_group;
--
-- 8.3 版本默认开启集数量：基础版=0 / 高级版=23(组A11+组B9+组C高级3) / 专业版=33
-- SELECT SUM(basic_enabled) AS basic_cnt, SUM(advanced_enabled) AS adv_cnt, SUM(pro_enabled) AS pro_cnt FROM feature_flag;
--
-- 8.4 系统级共享表须纳入 IGNORE_TENANT_TABLES（代码校验，见 MybatisPlusConfig）
-- SHOW CREATE TABLE biz_bundle;  SHOW CREATE TABLE feature_flag;
