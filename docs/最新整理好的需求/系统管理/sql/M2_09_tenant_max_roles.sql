-- =====================================================================
-- M2_09 角色数分层授权脚本（T2.5，v1.12 新增）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》v1.12 T2.5
--       《资金管控系统设计文档》v1.3 §1.6（角色体系按版本分层：基础版 2 / 高级版 4-6 / 专业版不限）
--       《多业态SaaS平台需求规格说明书》v1.0 §4.1（版本矩阵·角色数）
-- 前置依赖：M2_01_roles.sql（boss/finance/manager/employee/platform_admin 五角色）
-- 规范：
--   1. 基于 information_schema 条件判断，可安全重复执行（幂等）
--   2. 角色数量上限写入 sys_tenant.max_roles，由新增角色接口(SysRoleController#add)校验（代码层，见 T2.5④）
--   3. max_roles 语义：NULL=不限（专业版）；数值=业务角色上限（不含平台级 platform_admin，设计文档 1.6 口径说明）
--   4. 存量租户按 edition 回填：BASIC=2 / ADVANCED=6 / PROFESSIONAL(含 ENTERPRISE)=NULL
--      版本编码统一走大写全名比较，兼容 pro/professional/ENTERPRISE 存量（T4.1/T10.7 归一化，不改存量数据）
-- =====================================================================

-- ----------------------------
-- 1. sys_tenant 增加 max_roles 列（幂等）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m2_09_add_tenant_max_roles`;
DELIMITER $$
CREATE PROCEDURE `m2_09_add_tenant_max_roles`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'max_roles') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `max_roles` INT DEFAULT NULL COMMENT '业务角色数量上限(NULL=不限;基础版2/高级版6/专业版不限;不含平台级platform_admin)' AFTER `max_users`;
    END IF;
END$$
DELIMITER ;
CALL m2_09_add_tenant_max_roles();
DROP PROCEDURE IF EXISTS `m2_09_add_tenant_max_roles`;

-- ----------------------------
-- 2. 存量租户按版本回填 max_roles（天然幂等；版本编码大小写不敏感，兼容 ENTERPRISE/pro）
-- ----------------------------
-- 2.1 基础版 = 2（boss + employee）
UPDATE `sys_tenant` SET `max_roles` = 2
 WHERE UPPER(`edition`) = 'BASIC';

-- 2.2 高级版 = 6（boss/finance/manager/employee 四角色，platform_admin 可按业态包扩展至 6）
UPDATE `sys_tenant` SET `max_roles` = 6
 WHERE UPPER(`edition`) = 'ADVANCED';

-- 2.3 专业版 = 不限（NULL）；兼容存量 PROFESSIONAL / ENTERPRISE / pro / professional
UPDATE `sys_tenant` SET `max_roles` = NULL
 WHERE UPPER(`edition`) IN ('PROFESSIONAL', 'ENTERPRISE', 'PRO');

-- ----------------------------
-- 3. 验收查询（执行后人工核对）
-- ----------------------------
-- 3.1 各版本 max_roles 分布
-- SELECT UPPER(edition) AS edition, max_roles, COUNT(*) FROM sys_tenant GROUP BY UPPER(edition), max_roles;
-- 预期：BASIC→2；ADVANCED→6；PROFESSIONAL/ENTERPRISE→NULL
--
-- 3.2 列已建立
-- SHOW CREATE TABLE sys_tenant;
--
-- 3.3 防串校验（代码层）：基础版租户新建第 3 个业务角色被拒（提示升级）；
--     高级版超过 6 个被拒；专业版无上限；platform_admin 平台级角色不计入配额（见 SysRoleServiceImpl 校验）
