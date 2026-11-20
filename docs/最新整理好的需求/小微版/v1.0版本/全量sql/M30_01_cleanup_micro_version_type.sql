-- =====================================================================
-- M30_01 清理残留的 version_type='micro' 脏数据（v1.73 新增）
-- 版本：v1.0  日期：2026-10-17
-- 根因：M15_07 初始化 role_template 时使用 version_type='micro'，
--       M15_10 引入分档机制（micro_pro / micro_lite）后未清理旧的
--       version_type='micro' 记录，导致 role_template 表残留无效行。
--       后续多个脚本（M18_08/M18_14/M22_06/M26_01/M26_02/M28_01 等）
--       误将 'micro' 列入 WHERE IN 条件，实际命中脏数据。
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. 删除 role_template 中 version_type='micro' 的残留记录
--   2. 防御性校验：确认删除后只剩 micro_pro / micro_lite 两档
-- 规范：
--   1. DELETE 按 version_type 精确匹配，不影响 micro_pro / micro_lite
--   2. 幂等：重复执行安全（DELETE 0 行不报错）
-- 前置依赖：M15_10（分档机制，已引入 micro_pro / micro_lite）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 删除 role_template 中 version_type='micro' 的残留记录
--    ★M15_10 应删而未删的脏数据，本脚本兜底清理
-- ----------------------------
DELETE FROM `role_template` WHERE `version_type` = 'micro';

-- ----------------------------
-- 2. 防御性校验：确认 role_template 小微版只剩 micro_pro / micro_lite
-- ----------------------------
-- 2.1 查看当前 role_template 小微版版本分布
-- SELECT version_type, role_code, JSON_LENGTH(menu_keys) AS menu_count
--   FROM role_template
--  WHERE version_type IN ('micro_pro', 'micro_lite')
--  ORDER BY version_type, role_code;
-- 预期：micro_pro 3 行(boss/salesman/customer) + micro_lite 3 行 = 6 行
--
-- 2.2 确认无 'micro' 残留
-- SELECT COUNT(*) AS stale_micro FROM role_template WHERE version_type = 'micro';
-- 预期：0
--
-- 2.3 确认无其他异常版本
-- SELECT DISTINCT version_type FROM role_template
--  WHERE version_type NOT IN ('micro_pro','micro_lite','basic','advanced','pro')
--    AND version_type IS NOT NULL;
-- 预期：0 行

-- =====================================================================
-- ★执行后请重启后端，清除 role_template 相关缓存
-- =====================================================================
