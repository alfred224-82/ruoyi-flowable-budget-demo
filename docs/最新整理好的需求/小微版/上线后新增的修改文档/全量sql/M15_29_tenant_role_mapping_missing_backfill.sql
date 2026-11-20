-- =====================================================================
-- M15_29 存量租户角色映射缺失角色补录脚本（v1.45 修复）
-- 版本：v1.0  日期：2026-10-12
-- 背景：租户端角色映射自愈（TenantRoleMappingServiceImpl#selfHealFromIndustry，v1.30）
--       原仅在本租户映射**全空**时触发；若租户已有部分映射（典型路径：新增人员时
--       UserTenantRoleServiceImpl#assign 针对性自愈仅先补录了 boss），其余行业角色
--       （salesman/customer）将永远无法补录——如旅游租户角色映射页只见「旅行社社长(boss)」，
--       员工(导游)/顾客(游客) 缺失且无法分配。代码侧 v1.45 已改为「按缺失 role_code 补录」；
--       本脚本对存量库缺口做一次性确定性补录，不依赖是否打开过角色映射页。
--       ★另修复存量数据漂移：标准角色 salesman(9206)/customer(9207) 的 tenant_id 被误置 0
--       （M15_07 §1.1 约定须为 NULL=全局共享），导致 isNull(tenant_id) 口径的行业级联初始化/
--       自愈/可选角色清单静默丢弃这两个角色（只余 boss），一并防御性校正。
-- 依据：industry_role_map（系统级行业角色映射）× sys_tenant.industry
-- 规范：
--   1. 【幂等】INSERT...SELECT + NOT EXISTS 守卫，可重复执行（二次执行 0 新增）
--   2. 【脱钩】仅插入缺失 role_code，不改动已有映射的 display_name/status/sort_order（V3.5 §16.4）
--   3. 【T15.25 禁跨线】JOIN sys_role 校验标准角色存在（tenant_id IS NULL 且未删除）
--      且 tenant_type_scope 匹配（both/空 或等于本租户 tenant_type）
--   4. 【唯一键兜底】uk_tenant_role(tenant_id,role_code) 不含 del_flag →
--      NOT EXISTS 按「任意行（含 del_flag='2'）」判定，避免 Duplicate entry
-- 前置依赖：M15_01（tenant_role_mapping 建表）、M15_02/M15_22/M15_28（industry_role_map 种子）
-- ★零代码改动：纯数据补录，执行后立即生效（无需重启）
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 防御性校正：标准角色 salesman/customer 为全局共享（tenant_id NULL，M15_07 §1.1）
--    ★仅动固定 ID 段 9206/9207（克隆角色为雪花 ID 且 tenant_id=租户ID，不受影响）
-- ---------------------------------------------------------------------
UPDATE `sys_role` SET `tenant_id` = NULL
 WHERE `role_id` IN (9206, 9207)
   AND `tenant_id` IS NOT NULL;

-- ---------------------------------------------------------------------
-- 2. 缺失角色补录（INSERT...SELECT 幂等）
--    显示名/排序/启停口径与 IndustryTemplateServiceImpl#initTenantByIndustry 一致：
--    display_name=biz_role_name、sort_order=行业映射排序、default_flag=1→启用(0) 否则禁用(1)
-- ---------------------------------------------------------------------
INSERT INTO `tenant_role_mapping`
  (`tenant_id`, `role_code`, `display_name`, `sort_order`, `status`, `create_by`, `create_time`, `remark`)
SELECT t.id,
       irm.role_code,
       irm.biz_role_name,
       IFNULL(irm.sort_order, 0),
       CASE WHEN irm.default_flag = 1 THEN '0' ELSE '1' END,
       'system-heal',
       NOW(),
       'M15_29 补录：行业角色映射缺失角色回补（v1.45）'
FROM `sys_tenant` t
JOIN `industry_role_map` irm ON irm.industry_code = t.industry
JOIN `sys_role` r ON r.role_key = irm.role_code
  AND r.tenant_id IS NULL
  AND r.del_flag = '0'
WHERE (r.tenant_type_scope IS NULL OR r.tenant_type_scope = 'both' OR r.tenant_type_scope = t.tenant_type)
  AND NOT EXISTS (
    SELECT 1 FROM `tenant_role_mapping` m
     WHERE m.tenant_id = t.id
       AND m.role_code = irm.role_code
  );

-- ---------------------------------------------------------------------
-- 3. 验收查询（执行后人工核对）
-- ---------------------------------------------------------------------
-- 3.0 标准角色 tenant_id 归位（NULL=全局共享）
-- SELECT role_id, role_key, tenant_id, tenant_type_scope FROM sys_role WHERE role_id IN (9206, 9207);
-- 预期：2 行 tenant_id 均为 NULL（salesman/micro、customer/micro）
--
-- 3.1 旅游租户角色映射完整（老板/员工/顾客 3 条）
-- SELECT t.company_name, m.role_code, m.display_name, m.sort_order, m.status
--   FROM sys_tenant t JOIN tenant_role_mapping m ON m.tenant_id = t.id AND m.del_flag = '0'
--  WHERE t.industry = 'travel' ORDER BY m.sort_order;
-- 预期：杰杰旅游社 3 行（boss/旅行社社长、salesman/导游、customer/游客）
--
-- 3.2 全库缺口清零：不应再存在「行业有映射、租户缺映射」的组合
-- SELECT t.id, t.industry, irm.role_code
--   FROM sys_tenant t
--   JOIN industry_role_map irm ON irm.industry_code = t.industry
--   JOIN sys_role r ON r.role_key = irm.role_code AND r.tenant_id IS NULL AND r.del_flag = '0'
--  WHERE (r.tenant_type_scope IS NULL OR r.tenant_type_scope = 'both' OR r.tenant_type_scope = t.tenant_type)
--    AND NOT EXISTS (SELECT 1 FROM tenant_role_mapping m WHERE m.tenant_id = t.id AND m.role_code = irm.role_code);
-- 预期：0 行
--
-- 3.3 幂等复核：重复执行本脚本后 UPDATE 0 行、INSERT 0 行（观察 affected rows）
