-- =====================================================================
-- M2_08 租户级克隆角色迁移脚本（T2.5，配合 M2_02 v2.0 角色×版本模板）
-- 版本：v1.0  日期：2026-08-29
-- 背景：
--   1. 此前开通租户时把"版本全量菜单模板"复制给全部共享业务角色（9201~9204），
--      导致老板等角色菜单过多；且共享角色的菜单授权无租户维度，
--      任一租户版本变更都会全局覆盖所有租户的角色菜单。
--   2. 现改为：每个租户拥有克隆的预设角色（sys_role.tenant_id = 租户ID），
--      菜单授权挂在克隆角色上，按《各角色菜单权限文档》v2.4 的角色×版本模板执行。
-- 前置依赖：
--   1. M2_01_roles.sql（共享预设角色 9201~9205 存在，作为克隆源）
--   2. M2_02_role_menu_templates.sql v2.0（角色×版本模板已重建）
-- 规范：先删后插/幂等保护，脚本可重复执行
-- =====================================================================

-- ----------------------------
-- 1. 为尚无克隆角色的租户补建五个预设角色（绑定 tenant_id）
-- ----------------------------
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`,
                        `data_scope`, `menu_check_strictly`, `dept_check_strictly`,
                        `status`, `del_flag`, `create_by`, `create_time`, `remark`)
SELECT (UNIX_TIMESTAMP(NOW(3)) * 1000) + seq.rn,
       seq.tenant_id, seq.role_name, seq.role_key, seq.role_sort,
       seq.data_scope, seq.menu_check_strictly, seq.dept_check_strictly,
       '0', '0', 'system', NOW(),
       CONCAT('租户[', seq.tenant_id, ']克隆预设角色，来源:', seq.src_role_id)
FROM (
    SELECT t.`id` AS tenant_id,
           src.`role_id` AS src_role_id, src.`role_name`, src.`role_key`, src.`role_sort`,
           src.`data_scope`, src.`menu_check_strictly`, src.`dept_check_strictly`,
           ROW_NUMBER() OVER (ORDER BY t.`id`, src.`role_id`) AS rn
    FROM `sys_tenant` t
    CROSS JOIN `sys_role` src
    WHERE src.`role_id` IN (9201, 9202, 9203, 9204, 9205)
      AND NOT EXISTS (
          SELECT 1 FROM `sys_role` ex
          WHERE ex.`tenant_id` = t.`id` AND ex.`role_key` = src.`role_key` AND ex.`del_flag` = '0'
      )
) seq;

-- ----------------------------
-- 2. 按"角色×版本"模板为租户克隆角色授权菜单
--    模板 key = UPPER(role_key) + '_' + UPPER(edition)
--    （platform_admin → PLATFORM_ADMIN_BASIC 等，ENTERPRISE 归一为 PROFESSIONAL）
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, tpl.`menu_id`
FROM `sys_role` r
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
JOIN `sys_edition_menu_template` tpl
  ON tpl.`edition` = CONCAT(UPPER(r.`role_key`), '_',
       UPPER(CASE WHEN t.`edition` = 'ENTERPRISE' THEN 'PROFESSIONAL' ELSE t.`edition` END))
WHERE r.`tenant_id` IS NOT NULL
  AND r.`role_key` IN ('boss', 'finance', 'manager', 'employee', 'platform_admin')
  AND r.`del_flag` = '0'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm
      WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = tpl.`menu_id`
  );

-- ----------------------------
-- 3. 租户用户的角色指针：共享预设角色 → 本租户克隆角色
-- ----------------------------
-- 3.1 改指（跳过已持有克隆角色的用户，避免 sys_user_role 联合主键冲突）
SET SQL_SAFE_UPDATES = 0;
UPDATE `sys_user_role` ur
  JOIN `sys_user` u      ON u.`user_id` = ur.`user_id`
                        AND u.`tenant_id` IS NOT NULL AND u.`tenant_id` <> 0
  JOIN `sys_role` shared ON shared.`role_id` = ur.`role_id`
                        AND shared.`role_id` IN (9201, 9202, 9203, 9204, 9205)
  JOIN `sys_role` clone  ON clone.`tenant_id` = u.`tenant_id`
                        AND clone.`role_key` = shared.`role_key`
                        AND clone.`del_flag` = '0'
SET ur.`role_id` = clone.`role_id`
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT * FROM `sys_user_role`) x
    WHERE x.`user_id` = ur.`user_id` AND x.`role_id` = clone.`role_id`
);
SET SQL_SAFE_UPDATES = 1;

-- 3.2 清理改指后残留的共享角色关联（仅清理已有克隆角色对应的记录）
DELETE ur FROM `sys_user_role` ur
  JOIN `sys_user` u      ON u.`user_id` = ur.`user_id`
                        AND u.`tenant_id` IS NOT NULL AND u.`tenant_id` <> 0
  JOIN `sys_role` shared ON shared.`role_id` = ur.`role_id`
                        AND shared.`role_id` IN (9201, 9202, 9203, 9204, 9205)
  JOIN `sys_role` clone  ON clone.`tenant_id` = u.`tenant_id`
                        AND clone.`role_key` = shared.`role_key`
                        AND clone.`del_flag` = '0';

-- ----------------------------
-- 4. 共享预设角色自身菜单同步为角色×版本模板（admin 预览/克隆源定义保持一致）
--    注意：共享角色不再授权给租户用户，仅保留模板角色用途
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `role_id` = 9201;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9201, `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'BOSS_BASIC';

DELETE FROM `sys_role_menu` WHERE `role_id` = 9202;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9202, `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'FINANCE_BASIC';

DELETE FROM `sys_role_menu` WHERE `role_id` = 9203;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9203, `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'MANAGER_BASIC';

DELETE FROM `sys_role_menu` WHERE `role_id` = 9204;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9204, `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'EMPLOYEE_BASIC';

DELETE FROM `sys_role_menu` WHERE `role_id` = 9205;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9205, `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'PLATFORM_ADMIN_BASIC';

-- =====================================================================
-- 验收查询
-- =====================================================================

-- 每个租户应有 5 个克隆角色（预期缺失行数 = 0）
-- SELECT t.id, t.tenant_code, COUNT(r.role_id) AS cloned_role_cnt
-- FROM sys_tenant t
-- LEFT JOIN sys_role r ON r.tenant_id = t.id AND r.del_flag = '0'
-- GROUP BY t.id, t.tenant_code
-- HAVING cloned_role_cnt < 5;

-- 各租户老板角色菜单数（基础版预期 23；高级版 27；专业版 32）
-- SELECT t.tenant_code, t.edition, r.role_id, COUNT(rm.menu_id) AS menu_cnt
-- FROM sys_role r
-- JOIN sys_tenant t ON t.id = r.tenant_id
-- JOIN sys_role_menu rm ON rm.role_id = r.role_id
-- WHERE r.role_key = 'boss' AND r.tenant_id IS NOT NULL
-- GROUP BY t.tenant_code, t.edition, r.role_id;

-- 租户用户不应再持有共享预设角色（预期 0 行）
-- SELECT ur.user_id, ur.role_id, u.tenant_id
-- FROM sys_user_role ur
-- JOIN sys_user u ON u.user_id = ur.user_id
-- WHERE ur.role_id IN (9201, 9202, 9203, 9204, 9205)
--   AND u.tenant_id IS NOT NULL AND u.tenant_id <> 0;

-- 老板克隆角色菜单明细核对（基础版应为：定总额/建项目/审批项目/审批付款/审批变更/看板/报销审批按钮/消息中心）
-- SELECT m.menu_id, m.menu_name, m.perms
-- FROM sys_role_menu rm
-- JOIN sys_menu m ON m.menu_id = rm.menu_id
-- JOIN sys_role r ON r.role_id = rm.role_id
-- WHERE r.role_key = 'boss' AND r.tenant_id = <目标租户ID>
-- ORDER BY m.menu_id;
