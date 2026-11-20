-- =====================================================================
-- M15_31  旅游行业「司机」映射落库租户角色映射（同 role_code 多业务角色名支持）
-- =====================================================================
-- 需求概述：
--   旅游行业角色映射应含「司机」，与「导游」同为员工角色（salesman）的行业业务
--   角色名映射（不新建角色 code）。M15_30 已在 industry_role_map 补
--   ('travel','司机','salesman',0,4)，但 tenant_role_mapping 唯一键
--   uk_tenant_role(tenant_id,role_code) 与自愈/初始化的 role_code 去重口径使
--   同 code 第二个业务名永远无法落库 → 租户端「角色映射」页只见 3 行缺「司机」。
--
-- 变更：
--   1. ★唯一键放宽：uk_tenant_role(tenant_id,role_code)
--      → uk_tenant_role_code_name(tenant_id,role_code,display_name)。
--      同 role_code 允许并存多个业务角色名行（导游/司机）；显示名仍租户内唯一。
--   2. industry_role_map(travel,'司机') default_flag 0→1：落库租户映射时启用
--      （status='0'），与行业默认启用角色口径一致。
--   3. 存量租户补录（INSERT...SELECT 幂等，位序对账口径与
--      IndustryTemplateServiceImpl#initTenantByIndustry v1.46 一致）：
--      同 role_code 行业行按 sort_order 排位 1..N，租户该 code 已有 M 行时
--      补第 M+1..N 位行（M=N 时不补 → 租户改名后不会复活旧名，V3.5 §16.4 脱钩）。
--   4. ★无菜单/权限/开关/表结构（除唯一键）变更；执行后无需刷新权限快照。
--
-- 前置：
--   [121]M15_28（旅游行业模板种子）+ [133]M15_30（industry_role_map 司机行）
--
-- 幂等：唯一键变更带 INFORMATION_SCHEMA 守卫；补录 NOT EXISTS（pair 级，
--       含 del_flag='2' 行防 uk 冲突）+ 位序计数守卫，可重复执行
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 唯一键放宽：(tenant_id,role_code) → (tenant_id,role_code,display_name)
--    ★存量数据每租户每 code 仅 1 行，pair 级必然无重复，ALTER 安全
-- ---------------------------------------------------------------------
SELECT COUNT(*) INTO @uk_old FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenant_role_mapping'
   AND INDEX_NAME = 'uk_tenant_role';
SELECT COUNT(*) INTO @uk_new FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenant_role_mapping'
   AND INDEX_NAME = 'uk_tenant_role_code_name';
SET @ddl = CASE
    WHEN @uk_old > 0 THEN 'ALTER TABLE `tenant_role_mapping` DROP INDEX `uk_tenant_role`, ADD UNIQUE KEY `uk_tenant_role_code_name` (`tenant_id`, `role_code`, `display_name`)'
    WHEN @uk_new = 0 THEN 'ALTER TABLE `tenant_role_mapping` ADD UNIQUE KEY `uk_tenant_role_code_name` (`tenant_id`, `role_code`, `display_name`)'
    ELSE 'SELECT 1' END;
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. 司机行置为行业默认启用（落库租户映射 status='0'）
-- ---------------------------------------------------------------------
UPDATE `industry_role_map`
   SET `default_flag` = 1
 WHERE `industry_code` = 'travel'
   AND `biz_role_name` = '司机'
   AND `default_flag` <> 1;

-- ---------------------------------------------------------------------
-- 3. 存量租户补录同 code 附加业务角色名行（位序对账）
--    位序 pos = 同行业同 role_code 按 (sort_order,id) 升序的 1 基序号；
--    仅当租户该 code 现有行数 < pos 时补该行（改名占位不复活旧名）
-- ---------------------------------------------------------------------
INSERT INTO `tenant_role_mapping`
  (`tenant_id`, `role_code`, `display_name`, `sort_order`, `status`, `create_by`, `create_time`, `remark`)
SELECT t.`id`,
       irm.`role_code`,
       irm.`biz_role_name`,
       IFNULL(irm.`sort_order`, 0),
       CASE WHEN irm.`default_flag` = 1 THEN '0' ELSE '1' END,
       'system-heal',
       NOW(),
       'M15_31 补录：同 role_code 附加业务角色名（如旅游 司机）'
FROM `sys_tenant` t
JOIN `industry_template` it ON it.`del_flag` = '0'
  AND (t.`industry` = it.`industry_code` OR t.`industry` = it.`industry_name`)
JOIN `industry_role_map` irm ON irm.`industry_code` = it.`industry_code`
JOIN `sys_role` r ON r.`role_key` = irm.`role_code`
  AND r.`tenant_id` IS NULL
  AND r.`del_flag` = '0'
WHERE (r.`tenant_type_scope` IS NULL OR r.`tenant_type_scope` = 'both' OR r.`tenant_type_scope` = t.`tenant_type`)
  -- 位序守卫：租户该 code 行数 < 本行位序 才补
  AND (SELECT COUNT(*) FROM `tenant_role_mapping` m
        WHERE m.`tenant_id` = t.`id` AND m.`role_code` = irm.`role_code` AND m.`del_flag` = '0')
      < (1 + (SELECT COUNT(*) FROM `industry_role_map` c
               WHERE c.`industry_code` = irm.`industry_code` AND c.`role_code` = irm.`role_code`
                 AND (c.`sort_order` < irm.`sort_order`
                      OR (c.`sort_order` = irm.`sort_order` AND c.`id` < irm.`id`))))
  -- pair 级幂等 + uk 兜底（含软删除行占位）
  AND NOT EXISTS (
      SELECT 1 FROM `tenant_role_mapping` x
       WHERE x.`tenant_id` = t.`id`
         AND x.`role_code` = irm.`role_code`
         AND x.`display_name` = irm.`biz_role_name`
  );

-- ---------------------------------------------------------------------
-- 4. 验收查询（执行后人工核对）
-- ---------------------------------------------------------------------
-- 4.1 唯一键已放宽（预期：uk_tenant_role_code_name 1 行，uk_tenant_role 0 行）
-- SELECT INDEX_NAME FROM information_schema.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenant_role_mapping'
--    AND INDEX_NAME IN ('uk_tenant_role', 'uk_tenant_role_code_name') GROUP BY INDEX_NAME;
--
-- 4.2 司机行默认启用（预期：司机/salesman/1/4）
-- SELECT biz_role_name, role_code, default_flag, sort_order FROM industry_role_map
--  WHERE industry_code = 'travel' ORDER BY sort_order;
-- 预期：4 行（旅行社社长→boss/1、导游→salesman/1、游客→customer/1、司机→salesman/1）
--
-- 4.3 旅游租户角色映射含司机（预期每租户 4 行且 status 全 0）
-- SELECT t.id, m.role_code, m.display_name, m.sort_order, m.status
--   FROM sys_tenant t
--   JOIN tenant_role_mapping m ON m.tenant_id = t.id AND m.del_flag = '0'
--  WHERE t.industry = 'travel' ORDER BY t.id, m.sort_order;
--
-- 4.4 幂等复核：重复执行本脚本后 4.3 行数不变
