-- =====================================================================
-- M5_14 预算科目租户初始化补齐脚本（tenant_id=0 模板克隆模式，三版同步）
-- 版本：v1.12  日期：2026-09-06
-- 背景：
--   budget_subject 按 ADR-007 随租户隔离，但租户开通初始化（SysTenantServiceImpl
--   #initTenantDefaultData） historically 仅克隆部门树/项目关联规则/凭证导出模板，
--   遗漏预算科目：科目种子数据停留在 tenant_id=1（M1_01 补列默认值），
--   新租户（雪花ID）在租户过滤下 /system/subject/all 返回空，
--   项目立项(3201)等页面预算科目联动「无数据」（M5_13 映射B 回填本身已成功）。
-- 修复（对齐 fee_export_template / fee_project_link_rule 的 tenant_id=0 模板克隆模式）：
--   0. uk_subject_code 全局唯一键降级为租户级唯一 (tenant_id, subject_code)
--      （科目随租户隔离后，编码唯一性应为租户内唯一，对齐 fee_tenant_category.uk_tenant_category 口径）；
--   1. 存量科目数据（取最小非零租户）提升为 tenant_id=0 系统模板，
--      逐级重映射 parent_id（科目树 parent_id 基于 ID，ancestors/parent_code 基于编码）；
--   2. 为 sys_tenant 中无任何科目数据的租户按三级克隆模板（parent_id 按 subject_code 重映射）；
--   3. 顺带为无大类启用配置的租户补齐 fee_tenant_category（INSERT IGNORE + NOT EXISTS 幂等）；
--   4. Java 侧 initTenantDefaultData 同步增加相同克隆逻辑，覆盖未来新租户。
-- 幂等：模板创建以「tenant_id=0 计数=0」为守卫；租户克隆以「执行前快照的待初始化租户
--       临时表」为守卫；重复执行不产生重复数据。
-- =====================================================================

-- ----------------------------
-- 0. 唯一键降级：uk_subject_code(全局) → uk_tenant_subject(tenant_id, subject_code)
--    科目随租户隔离 + 模板克隆后，编码唯一性必须为租户内唯一，否则克隆报 Duplicate entry
-- ----------------------------
DROP PROCEDURE IF EXISTS `m5_14_fix_subject_uk`;
DELIMITER $$
CREATE PROCEDURE `m5_14_fix_subject_uk`()
BEGIN
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budget_subject'
                 AND INDEX_NAME = 'uk_subject_code') THEN
        ALTER TABLE `budget_subject` DROP INDEX `uk_subject_code`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budget_subject'
                     AND INDEX_NAME = 'uk_tenant_subject') THEN
        ALTER TABLE `budget_subject` ADD UNIQUE KEY `uk_tenant_subject` (`tenant_id`, `subject_code`);
    END IF;
END$$
DELIMITER ;

CALL m5_14_fix_subject_uk();
DROP PROCEDURE IF EXISTS `m5_14_fix_subject_uk`;

-- ----------------------------
-- 1. 系统模板（tenant_id=0）：自最小非零租户提升，三级重映射 parent_id
-- ----------------------------
SET @src_tenant = (SELECT MIN(tenant_id) FROM budget_subject WHERE tenant_id IS NOT NULL AND tenant_id <> 0);
SET @tpl_cnt    = (SELECT COUNT(*) FROM budget_subject WHERE tenant_id = 0);

-- 一级（顶级 parent_id=0）
INSERT INTO budget_subject (tenant_id, subject_code, subject_name, parent_id, parent_code, level, ancestors,
                            is_leaf, sort_order, is_active, description, create_by, create_time, subject_type, category_code)
SELECT 0, subject_code, subject_name, 0, parent_code, level, ancestors,
       is_leaf, sort_order, is_active, description, 'system-migration', NOW(), subject_type, category_code
  FROM budget_subject
 WHERE @tpl_cnt = 0 AND @src_tenant IS NOT NULL
   AND tenant_id = @src_tenant AND level = 1;

-- 二级（parent_id 按 subject_code 重映射为模板一级新 ID）
INSERT INTO budget_subject (tenant_id, subject_code, subject_name, parent_id, parent_code, level, ancestors,
                            is_leaf, sort_order, is_active, description, create_by, create_time, subject_type, category_code)
SELECT 0, t.subject_code, t.subject_name, p.id, t.parent_code, t.level, t.ancestors,
       t.is_leaf, t.sort_order, t.is_active, t.description, 'system-migration', NOW(), t.subject_type, t.category_code
  FROM budget_subject t
  JOIN budget_subject p ON p.tenant_id = 0 AND p.subject_code = t.parent_code
 WHERE @tpl_cnt = 0 AND @src_tenant IS NOT NULL
   AND t.tenant_id = @src_tenant AND t.level = 2;

-- 三级（及更深层级，父级须已在上一pass 入模板）
INSERT INTO budget_subject (tenant_id, subject_code, subject_name, parent_id, parent_code, level, ancestors,
                            is_leaf, sort_order, is_active, description, create_by, create_time, subject_type, category_code)
SELECT 0, t.subject_code, t.subject_name, p.id, t.parent_code, t.level, t.ancestors,
       t.is_leaf, t.sort_order, t.is_active, t.description, 'system-migration', NOW(), t.subject_type, t.category_code
  FROM budget_subject t
  JOIN budget_subject p ON p.tenant_id = 0 AND p.subject_code = t.parent_code
 WHERE @tpl_cnt = 0 AND @src_tenant IS NOT NULL
   AND t.tenant_id = @src_tenant AND t.level >= 3;

-- ----------------------------
-- 2. 待初始化租户快照（执行前无任何科目数据的租户；临时表会话级）
-- ----------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_subject_init_tenant;
CREATE TEMPORARY TABLE tmp_subject_init_tenant (
  tenant_id BIGINT NOT NULL,
  PRIMARY KEY (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='M5_14 待初始化科目租户快照（会话临时）';

INSERT INTO tmp_subject_init_tenant (tenant_id)
SELECT st.id FROM sys_tenant st
 WHERE NOT EXISTS (SELECT 1 FROM budget_subject x WHERE x.tenant_id = st.id);

-- ----------------------------
-- 3. 模板克隆到各待初始化租户（三级重映射 parent_id）
-- ----------------------------
INSERT INTO budget_subject (tenant_id, subject_code, subject_name, parent_id, parent_code, level, ancestors,
                            is_leaf, sort_order, is_active, description, create_by, create_time, subject_type, category_code)
SELECT st.tenant_id, t.subject_code, t.subject_name, 0, t.parent_code, t.level, t.ancestors,
       t.is_leaf, t.sort_order, t.is_active, t.description, 'system-migration', NOW(), t.subject_type, t.category_code
  FROM tmp_subject_init_tenant st
  CROSS JOIN budget_subject t
 WHERE t.tenant_id = 0 AND t.level = 1;

INSERT INTO budget_subject (tenant_id, subject_code, subject_name, parent_id, parent_code, level, ancestors,
                            is_leaf, sort_order, is_active, description, create_by, create_time, subject_type, category_code)
SELECT st.tenant_id, t.subject_code, t.subject_name, p.id, t.parent_code, t.level, t.ancestors,
       t.is_leaf, t.sort_order, t.is_active, t.description, 'system-migration', NOW(), t.subject_type, t.category_code
  FROM tmp_subject_init_tenant st
  CROSS JOIN budget_subject t
  JOIN budget_subject p ON p.tenant_id = st.tenant_id AND p.subject_code = t.parent_code
 WHERE t.tenant_id = 0 AND t.level = 2;

INSERT INTO budget_subject (tenant_id, subject_code, subject_name, parent_id, parent_code, level, ancestors,
                            is_leaf, sort_order, is_active, description, create_by, create_time, subject_type, category_code)
SELECT st.tenant_id, t.subject_code, t.subject_name, p.id, t.parent_code, t.level, t.ancestors,
       t.is_leaf, t.sort_order, t.is_active, t.description, 'system-migration', NOW(), t.subject_type, t.category_code
  FROM tmp_subject_init_tenant st
  CROSS JOIN budget_subject t
  JOIN budget_subject p ON p.tenant_id = st.tenant_id AND p.subject_code = t.parent_code
 WHERE t.tenant_id = 0 AND t.level >= 3;

DROP TEMPORARY TABLE IF EXISTS tmp_subject_init_tenant;

-- ----------------------------
-- 4. 租户大类启用配置补齐（无配置的租户启用全部内置大类；与 M5_06/M5_13 口径一致）
-- ----------------------------
INSERT IGNORE INTO fee_tenant_category (tenant_id, category_code, category_name, is_enabled, create_by)
SELECT st.id, c.category_code, c.category_name, 1, 'system-migration'
  FROM sys_tenant st
  CROSS JOIN fee_expense_category c
 WHERE c.is_builtin = 1 AND c.is_active = 1
   AND NOT EXISTS (SELECT 1 FROM fee_tenant_category f WHERE f.tenant_id = st.id);

-- ----------------------------
-- 验收查询
-- ----------------------------
-- SELECT tenant_id, COUNT(*) AS cnt FROM budget_subject GROUP BY tenant_id;
--   预期：tenant_id=0 模板 57 行 + 每个租户各 57 行（存量租户保留原数据）
-- SELECT b.tenant_id, COUNT(*) AS travel_cnt FROM budget_subject b
--   JOIN sys_tenant st ON st.id = b.tenant_id
--  WHERE b.category_code = 'TRAVEL' AND b.is_active = 1 GROUP BY b.tenant_id;
--   预期：每租户 4 行（项目立项「差旅招待」联动数据源）
-- SELECT st.id, (SELECT COUNT(*) FROM fee_tenant_category f WHERE f.tenant_id = st.id) AS cat_cnt
--   FROM sys_tenant st;   预期：每租户 6
-- 树完整性抽查：SELECT COUNT(*) FROM budget_subject b
--   WHERE b.level >= 2 AND NOT EXISTS (SELECT 1 FROM budget_subject p
--     WHERE p.tenant_id = b.tenant_id AND p.id = b.parent_id);   预期 0
-- SHOW INDEX FROM budget_subject WHERE Key_name = 'uk_tenant_subject';   预期存在（tenant_id, subject_code）
