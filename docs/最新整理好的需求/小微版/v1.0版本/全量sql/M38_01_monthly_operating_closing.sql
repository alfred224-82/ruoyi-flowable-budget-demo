-- =====================================================================
-- M38_01  月度经营月结表 monthly_operating_closing（T38.4）+ 菜单
-- =====================================================================
-- 需求来源：《小账本H5界面开发任务文档_v1.md》§八 M38 月度经营核算
--          《小账本H5界面设计方案_v3》§20 三层利润模型 / §21 三张报表
--
-- 交付范围：
--   T38.4 建表：monthly_operating_closing（月度税费 + 月结锁定状态，租户级）
--   菜单：7380~7382（小程序·老板端「月度经营报表」C-mini + 税费保存/月结锁定 F）
--         perms：micro:report:operating（查看月报）/ micro:report:closing（税费+月结）
--
-- 关键口径（§20，已锁定）：
--   ① 数据源为 activity_settlement（M36 [152] 已交付），本表仅承载税费与月结锁定；
--   ② 月净利 = 月毛利 − 固定成本合计 − 当月税费；月保本毛利 = 固定成本合计；
--   ③ tax_mode：MANUAL=手填税费 / RATE=按率估算（估算值非税务申报依据）；
--   ④ status：OPEN=未月结 / LOCKED=已锁定；锁定后禁止改税费（后端强约束）。
--
-- 权限：仅老板 micro_pro（lite/员工不授权，§21 验收4 + 计划 Assumptions）
-- 幂等：CREATE TABLE IF NOT EXISTS（数据表不 DROP，保护税费/锁定数据）+ 先删后插菜单
--       + JSON_CONTAINS 守卫追加 + INSERT IGNORE 补录
-- 前置：[45]M15_06（小程序菜单 7000~7499 + 老板小程序根 7200）
--       + [152]M36_01（activity_settlement 结算数据源）
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 月度经营月结表 monthly_operating_closing（T38.4）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `monthly_operating_closing` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     bigint        NOT NULL                COMMENT '租户ID',
  `month`         varchar(7)    NOT NULL                COMMENT '月份（yyyy-MM，如 2026-10）',
  `tax_amount`    decimal(12,2) DEFAULT NULL            COMMENT '税费金额（元）',
  `tax_rate`      decimal(6,2)  DEFAULT NULL            COMMENT '税率（%，如 3 表示 3%；手填税费时可空）',
  `tax_mode`      varchar(20)   DEFAULT 'MANUAL'        COMMENT '计税方式：MANUAL=手填 / RATE=按率估算',
  `status`        varchar(20)   NOT NULL DEFAULT 'OPEN' COMMENT '月结状态：OPEN=未月结 / LOCKED=已锁定',
  `lock_time`     datetime      DEFAULT NULL            COMMENT '锁定时间',
  `lock_by`       bigint        DEFAULT NULL            COMMENT '锁定操作人（sys_user.user_id）',
  `remark`        varchar(500)  DEFAULT NULL            COMMENT '备注',

  `create_dept`   bigint        DEFAULT NULL            COMMENT '创建部门',
  `create_by`     varchar(64)   DEFAULT NULL            COMMENT '创建者',
  `create_time`   datetime      DEFAULT NULL            COMMENT '创建时间',
  `update_by`     varchar(64)   DEFAULT NULL            COMMENT '更新者',
  `update_time`   datetime      DEFAULT NULL            COMMENT '更新时间',
  `del_flag`      char(1)       DEFAULT '0'             COMMENT '删除标志（0-正常；2-已删除）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_month` (`tenant_id`, `month`, `del_flag`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='月度经营月结（税费+锁定，§20，租户级）';

-- =====================================================================
-- 2. 菜单：M38 月度经营报表（7380~7382，小程序·老板端 mini）
--    父节点 7200（小程序·老板，M15_06 [45]），与 7201 经营报表同级
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7380 AND 7384;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7380 AND 7384;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7380, '月度经营报表', 7200, 4, 'operating', 'miniBoss/operating', 1, 0, 'C', '0', '0', 'micro:report:operating', 'mini', '#', 'admin', NOW(), 'M38 T38.3 月报（月毛利/净利/税费/保本线，仅 micro_pro）'),
(7381, '税费保存',     7380, 1, '',           NULL,                 1, 0, 'F', '0', '0', 'micro:report:closing',   'mini', '#', 'admin', NOW(), 'M38 T38.4 手填税费/按率估算'),
(7382, '月结锁定',     7380, 2, '',           NULL,                 1, 0, 'F', '0', '0', 'micro:report:closing',   'mini', '#', 'admin', NOW(), 'M38 T38.4 锁定后禁止改税费');

-- =====================================================================
-- 3. role_template：boss（仅 micro_pro）追加 M38 报表权限键
--    ★lite 不授权（§21 验收4：三张报表 + 月报仅老板 micro_pro）
-- =====================================================================
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:operating')
WHERE `role_code` = 'boss' AND `version_type` = 'micro_pro' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:operating"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:closing')
WHERE `role_code` = 'boss' AND `version_type` = 'micro_pro' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:closing"');

-- micro:report:export 已由 M15_06/M15_07 存在（菜单 7144），此处幂等确保 micro_pro boss 含导出权限
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:export')
WHERE `role_code` = 'boss' AND `version_type` = 'micro_pro' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:export"');

-- =====================================================================
-- 4. 存量 sys_role_menu 补录（仅 micro_pro 老板克隆角色）
-- =====================================================================
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro'
  AND m.`menu_id` BETWEEN 7380 AND 7382;

-- 超管 role_id=1 补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` BETWEEN 7380 AND 7382;

-- =====================================================================
-- 5. 验收查询（执行后人工核对）
-- =====================================================================
-- 5.1 表就绪
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_operating_closing';
-- 预期：1 行
--
-- 5.2 菜单 7380~7382 就绪
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu
--  WHERE menu_id BETWEEN 7380 AND 7382 ORDER BY menu_id;
-- 预期：3 行（7380 C-mini micro:report:operating / 7381~7382 F micro:report:closing）
--
-- 5.3 role_template boss micro_pro 含权限、micro_lite 不含
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:report:operating"') AS has_operating,
--        JSON_CONTAINS(menu_keys,'"micro:report:closing"')   AS has_closing
--   FROM role_template WHERE role_code = 'boss' AND version_type IN ('micro_pro','micro_lite');
-- 预期：micro_pro=1/1；micro_lite=0/0
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
