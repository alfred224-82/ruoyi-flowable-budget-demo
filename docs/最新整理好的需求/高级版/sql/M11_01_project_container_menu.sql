-- =====================================================================
-- M11_01 项目容器统一 · 菜单与成员角色映射脚本（T11.1~T11.5）
-- 版本：v1.2  日期：2026-09-06
-- v1.2 变更：基础版仅 boss/employee 两角色且 boss 兼任财务/经理，故补齐预设 boss(9201) 的
--      企业任务全量（add/edit/remove/assign 3412~3415）与活动报名操作（add/remove/checkin
--      3422/3424/3425）授权，避免 boss 能进「企业任务/活动报名」页却因缺按钮权限无法操作；
--      经 §4.5 派生进 BOSS_BASIC/ADVANCED/PROFESSIONAL 模板、§4.6 对齐存量克隆 boss
-- v1.1 变更：补「角色×版本」授权模板登记（BOSS_/FINANCE_/MANAGER_/EMPLOYEE_ × 三版本，
--      由预设角色授权派生）+ 存量租户克隆角色菜单对齐（INSERT IGNORE 幂等）；
--      修复 v1.0 仅登记版本全量键（BASIC/ADVANCED/PROFESSIONAL）导致开通租户的克隆
--      boss/manager 等角色侧边栏无「项目协作」菜单的问题（克隆授权唯一依据为
--      sys_edition_menu_template 的 roleKey_edition 键，见 SysTenantServiceImpl.getRoleTemplateKey）
-- 依据：《资金管控系统开发任务文档》v1.12 M11（项目容器统一）
--       《多业态SaaS平台需求规格说明书》v1.0 §6（项目即容器）
--       《资金管控系统设计文档》v1.3 §1.8.5（项目即容器）
-- 前置依赖：
--   M1_07_project_container_tables.sql（project / project_member / project_task / project_signup 已建）
--   M1_05_platform_foundation_tables.sql（biz_bundle 业态包 menus 声明）
--   M2_01_roles.sql（角色 9201 boss / 9202 finance / 9203 manager / 9204 employee）
-- 规范：
--   1. 先删后插，脚本可重复执行（幂等）
--   2. menu_id 占用 3400~3439（此前未使用，避免与 2000/3000/3200/3300/6000 段冲突）
--   3. 项目容器为【基础版标配】（业态能力全版本可用，M11 与 M5 并行），三版本模板均登记
--   4. 菜单可见性四层叠加：版本模板 → biz_bundle.menus 业态过滤 → feature_flag∩License → 单店覆盖
--   5. project_member.role 五枚举与 sys_role 解耦；存量 fee_project_member VIEWER→member（只读）
-- =====================================================================

-- ----------------------------
-- 1. 菜单：项目协作容器（目录 3400，parent_id=0）
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 3400 AND 3439;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
-- 目录
(3400, '项目协作', 0, 5, 'biz', NULL, NULL, 1, 0, 'M', '0', '0', '', 'component', 'admin', NOW(), '', NULL, '项目即容器：企业任务与活动报名共用 project 容器（M11）'),
-- 页面：项目容器（T11.1）
(3401, '项目容器', 3400, 1, 'container', 'biz/project/container/index', NULL, 1, 0, 'C', '0', '0', 'biz:project:list', 'guide', 'admin', NOW(), '', NULL, 'project 统一容器 CRUD + 业态模板（extra_config）'),
-- 页面：企业任务（T11.3，type=general）
(3410, '企业任务', 3400, 2, 'task', 'biz/project/task/index', NULL, 1, 0, 'C', '0', '0', 'biz:task:list', 'list', 'admin', NOW(), '', NULL, 'project_task 分表：指派/进度/截止/附件'),
-- 页面：活动报名（T11.3，type=activity/team_buy）
(3420, '活动报名', 3400, 3, 'signup', 'biz/project/signup/index', NULL, 1, 0, 'C', '0', '0', 'biz:signup:list', 'peoples', 'admin', NOW(), '', NULL, 'project_signup 分表：人数/名额/候补/签到'),
-- 页面：项目成员（T11.2，五枚举）
(3430, '项目成员', 3400, 4, 'member', 'biz/project/member/index', NULL, 1, 0, 'C', '0', '0', 'biz:member:list', 'user', 'admin', NOW(), '', NULL, 'project_member 五枚举 owner/manager/member/participant/signup');

-- ----------------------------
-- 2. 按钮权限（F 类）
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
-- 项目容器按钮
(3402, '容器查询', 3401, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:project:query',   '#', 'admin', NOW(), '', NULL, ''),
(3403, '容器新增', 3401, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:project:add',     '#', 'admin', NOW(), '', NULL, '按 type=activity/team_buy/general 建容器'),
(3404, '容器编辑', 3401, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:project:edit',    '#', 'admin', NOW(), '', NULL, '含 extra_config 业态差异字段'),
(3405, '容器删除', 3401, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:project:remove',  '#', 'admin', NOW(), '', NULL, ''),
(3406, '容器发布', 3401, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:project:publish', '#', 'admin', NOW(), '', NULL, 'DRAFT→PUBLISHED，触发组队引擎策略'),
-- 企业任务按钮
(3411, '任务查询', 3410, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:task:query',   '#', 'admin', NOW(), '', NULL, ''),
(3412, '任务新增', 3410, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:task:add',     '#', 'admin', NOW(), '', NULL, ''),
(3413, '任务编辑', 3410, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:task:edit',    '#', 'admin', NOW(), '', NULL, ''),
(3414, '任务删除', 3410, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:task:remove',  '#', 'admin', NOW(), '', NULL, ''),
(3415, '任务指派', 3410, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:task:assign',  '#', 'admin', NOW(), '', NULL, '指派 + 进度更新'),
-- 活动报名按钮
(3421, '报名查询', 3420, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:signup:query',   '#', 'admin', NOW(), '', NULL, ''),
(3422, '报名登记', 3420, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:signup:add',     '#', 'admin', NOW(), '', NULL, '满员进候补 is_waitlist'),
(3423, '报名编辑', 3420, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:signup:edit',    '#', 'admin', NOW(), '', NULL, ''),
(3424, '报名取消', 3420, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:signup:remove',  '#', 'admin', NOW(), '', NULL, ''),
(3425, '报名签到', 3420, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:signup:checkin', '#', 'admin', NOW(), '', NULL, 'NOT_SIGNED→SIGNED/ABSENT'),
-- 项目成员按钮
(3431, '成员查询', 3430, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:member:query',  '#', 'admin', NOW(), '', NULL, ''),
(3432, '成员编辑', 3430, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:member:edit',   '#', 'admin', NOW(), '', NULL, '分配五枚举角色'),
(3433, '成员移除', 3430, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:member:remove', '#', 'admin', NOW(), '', NULL, '');

-- ----------------------------
-- 3. 三版本授权模板登记（项目容器为基础版标配，三版本均含）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` BETWEEN 3400 AND 3439;
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT e.edition, m.menu_id
  FROM (SELECT 'BASIC' AS edition UNION ALL SELECT 'ADVANCED' UNION ALL SELECT 'PROFESSIONAL') e
  CROSS JOIN (SELECT menu_id FROM `sys_menu` WHERE menu_id BETWEEN 3400 AND 3439) m;

-- ----------------------------
-- 4. 五角色授权（幂等：先删后插）
--    boss(9201)/manager(9203)：容器全量 + 成员 + 任务/报名查看；
--    finance(9202)：容器查看（资金视图关联）；employee(9204)：仅报名（participant/signup）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 3400 AND 3439 AND `role_id` IN (9201, 9202, 9203, 9204);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
-- 老板：目录 + 容器全量 + 成员全量 + 任务全量 + 报名全量
-- （基础版仅 boss/employee 两角色、boss 兼任财务/经理，故 boss 需可操作企业任务与活动报名，不能只读）
(9201, 3400), (9201, 3401), (9201, 3402), (9201, 3403), (9201, 3404), (9201, 3405), (9201, 3406),
(9201, 3410), (9201, 3411), (9201, 3412), (9201, 3413), (9201, 3414), (9201, 3415),
(9201, 3420), (9201, 3421), (9201, 3422), (9201, 3424), (9201, 3425),
(9201, 3430), (9201, 3431), (9201, 3432), (9201, 3433),
-- 财务：目录 + 容器查看
(9202, 3400), (9202, 3401), (9202, 3402),
-- 项目经理：目录 + 容器查看/编辑 + 任务全量 + 报名查看 + 成员查看
(9203, 3400), (9203, 3401), (9203, 3402), (9203, 3404),
(9203, 3410), (9203, 3411), (9203, 3412), (9203, 3413), (9203, 3414), (9203, 3415),
(9203, 3420), (9203, 3421), (9203, 3430), (9203, 3431),
-- 报销人/员工：目录 + 报名（登记/查看/签到）
(9204, 3400), (9204, 3420), (9204, 3421), (9204, 3422), (9204, 3425);

-- ----------------------------
-- 4.5 「角色×版本」授权模板登记（v1.1：克隆角色授权唯一依据，由预设角色授权派生）
--     键格式 roleKey_edition（BOSS_BASIC 等），三版本累计制内容一致（业态能力全版本可用）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` BETWEEN 3400 AND 3439
  AND `edition` IN ('BOSS_BASIC','BOSS_ADVANCED','BOSS_PROFESSIONAL',
                    'FINANCE_BASIC','FINANCE_ADVANCED','FINANCE_PROFESSIONAL',
                    'MANAGER_BASIC','MANAGER_ADVANCED','MANAGER_PROFESSIONAL',
                    'EMPLOYEE_BASIC','EMPLOYEE_ADVANCED','EMPLOYEE_PROFESSIONAL');
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT CONCAT(UPPER(r.`role_key`), '_', e.`edition`), rm.`menu_id`
  FROM `sys_role` r
  JOIN `sys_role_menu` rm ON rm.`role_id` = r.`role_id`
  CROSS JOIN (SELECT 'BASIC' AS edition UNION ALL SELECT 'ADVANCED' UNION ALL SELECT 'PROFESSIONAL') e
 WHERE r.`tenant_id` IS NULL
   AND r.`role_key` IN ('boss', 'finance', 'manager', 'employee')
   AND rm.`menu_id` BETWEEN 3400 AND 3439;

-- ----------------------------
-- 4.6 存量租户克隆角色菜单对齐（v1.1：开通时按旧模板复制的克隆角色补齐项目容器菜单）
--     角色保全：仅按 CONCAT(UPPER(role_key),'_',UPPER(tenant.edition)) 模板补本角色菜单，不跨角色授权
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, tp.`menu_id`
  FROM `sys_role` r
  JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
  JOIN `sys_edition_menu_template` tp
    ON tp.`edition` = CONCAT(UPPER(r.`role_key`), '_', UPPER(t.`edition`))
 WHERE r.`tenant_id` IS NOT NULL
   AND r.`role_key` IN ('boss', 'finance', 'manager', 'employee')
   AND tp.`menu_id` BETWEEN 3400 AND 3439;

-- ----------------------------
-- 5. biz_bundle.menus 追加项目容器菜单（业态过滤层，幂等 JSON 合并）
--    fund_control/boardgame/fresh_food 三包均声明项目容器菜单（项目即容器为全业态底座能力）
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;
UPDATE `biz_bundle`
   SET `menus` = JSON_MERGE_PRESERVE(
        COALESCE(`menus`, JSON_ARRAY()),
        JSON_ARRAY(3400, 3401, 3410, 3420, 3430))
 WHERE `biz_code` IN ('fund_control', 'boardgame', 'fresh_food')
   AND NOT JSON_CONTAINS(COALESCE(`menus`, JSON_ARRAY()), '3401');
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 6. 存量 fee_project_member → project_member 角色映射（T11.2，仅对已关联 project_id 的 fee_project 生效）
--    MANAGER→manager / MEMBER→member / VIEWER→member（只读）；不迁移存量 fee_project（不合并策略）
--    幂等：uk_project_user 唯一约束 + INSERT IGNORE
-- ----------------------------
INSERT IGNORE INTO `project_member` (`tenant_id`, `project_id`, `user_id`, `role`, `join_time`)
SELECT fp.`tenant_id`, fp.`project_id`, fpm.`user_id`,
       CASE UPPER(fpm.`role`)
            WHEN 'MANAGER' THEN 'manager'
            WHEN 'MEMBER'  THEN 'member'
            WHEN 'VIEWER'  THEN 'member'
            ELSE 'member'
       END,
       NOW()
  FROM `fee_project_member` fpm
  JOIN `fee_project` fp ON fp.`id` = fpm.`project_id`
 WHERE fp.`project_id` IS NOT NULL;

-- ----------------------------
-- 7. 验收查询（执行后人工核对）
-- ----------------------------
-- 7.1 菜单落库
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3400 AND 3439 ORDER BY menu_id;
-- 7.2 三版本模板登记数量一致（各版本应含相同的项目容器菜单集）
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template WHERE menu_id BETWEEN 3400 AND 3439 GROUP BY edition;
-- 7.3 角色授权
-- SELECT role_id, COUNT(*) FROM sys_role_menu WHERE menu_id BETWEEN 3400 AND 3439 GROUP BY role_id;
-- 7.5 角色×版本模板登记（v1.1：boss/finance/manager/employee × 3 版本，预期 12 行键）
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template WHERE menu_id BETWEEN 3400 AND 3439 GROUP BY edition;
-- 7.6 存量克隆 boss 角色已含项目协作目录 3400（预期各租户克隆 boss 均 1 行）
-- SELECT r.role_id, r.role_key, r.tenant_id FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id
--  WHERE r.tenant_id IS NOT NULL AND r.role_key = 'boss' AND rm.menu_id = 3400;
-- 7.4 业态包 menus 已含 3401
-- SELECT biz_code, JSON_CONTAINS(menus, '3401') AS has_container FROM biz_bundle;
