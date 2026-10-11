-- =====================================================================
-- M4_01 版本编码统一迁移脚本：ENTERPRISE → PROFESSIONAL（T4.1）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控系统开发任务文档》T4.1 / 《01_ADR架构决策清单》一套系统+版本分级
-- 规范：
--   1. UPDATE 天然幂等，脚本可重复执行（重复执行影响 0 行）
--   2. 版本变更日志先于 UPDATE 写入（基于 ENTERPRISE 存量筛选），
--      重复执行时已无 ENTERPRISE 记录，不会重复写日志
--   3. 同档映射：ENTERPRISE 与 PROFESSIONAL 功能等级相同（均为最高档），
--      不涉及功能增减，无需菜单/开关补偿
-- =====================================================================

-- ----------------------------
-- 1. 租户表：版本编码映射（含变更留痕）
-- ----------------------------
-- 1.1 先写版本变更日志（仅针对仍为 ENTERPRISE 的租户）
INSERT INTO `sys_tenant_version_log` (`tenant_id`, `from_edition`, `to_edition`, `change_type`, `operator`, `remark`, `create_by`, `create_time`)
SELECT t.`id`, 'ENTERPRISE', 'PROFESSIONAL', 'UPGRADE', 'system-migration',
       'T4.1版本编码统一映射：ENTERPRISE→PROFESSIONAL（同档等价，功能与授权无变化）',
       'system', NOW()
  FROM `sys_tenant` t
 WHERE t.`edition` = 'ENTERPRISE'
   AND NOT EXISTS (SELECT 1 FROM `sys_tenant_version_log` l
                    WHERE l.`tenant_id` = t.`id`
                      AND l.`remark` LIKE 'T4.1版本编码统一映射%');

SET SQL_SAFE_UPDATES = 0;
-- 1.2 执行编码映射
UPDATE `sys_tenant`
   SET `edition` = 'PROFESSIONAL',
       `update_by` = 'system-migration',
       `update_time` = NOW()
 WHERE `edition` = 'ENTERPRISE';

-- ----------------------------
-- 2. 版本增量初始化数据标记表：编码映射
-- ----------------------------
UPDATE `fee_edition_init_data`
   SET `edition` = 'PROFESSIONAL'
 WHERE `edition` = 'ENTERPRISE';
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 3. sys_license 说明（重要：不直接 UPDATE）
-- ----------------------------
-- sys_license 含 RSA 数字签名（signature 覆盖 edition 字段），直接改库会导致
-- 验签失败进入 TAMPERED 状态。存量 ENTERPRISE License 处理策略：
--   ① 运行时代码已做兼容映射：EditionFeatureConfig.getFeaturesByEdition()
--      与 SysTenantServiceImpl.getEditionLevel() 均将 ENTERPRISE 等价
--      PROFESSIONAL，存量 License 无需立即重签即可正常运行；
--   ② 如需彻底统一，请用 LicenseGenerateTool 以 PROFESSIONAL 重新签发
--      并替换旧 License（注意 license_version 递增，防回滚校验）。

-- ----------------------------
-- 4. 验收查询（迁移完成后执行，预期均为 0）
-- ----------------------------
-- SELECT COUNT(*) FROM sys_tenant WHERE edition = 'ENTERPRISE';
-- SELECT COUNT(*) FROM fee_edition_init_data WHERE edition = 'ENTERPRISE';
