-- =====================================================================
-- M20_10  异常处理中心「处理」按钮权限补全（v1.67 新增）
-- 版本：v1.1  日期：2026-10-02
-- 依据：用户反馈「PUT /micro/exceptionCenter/resolve 报 无此权限：micro:exception:resolve」
-- ---------------------------------------------------------------------
-- 根因：
--   MicroExceptionCenterController.resolve() 使用 @SaCheckPermission("micro:exception:resolve")，
--   但 sys_menu 中不存在 perms=micro:exception:resolve 的 F 按钮菜单行
--   （仅 7698 C 菜单承载 micro:exception:list），role_template menu_keys 亦无此键 →
--   该权限串永远无法落地为菜单授权，任何角色（含 micro_pro boss）校验必失败（403）。
--   （同 M20 通配符权限事故模式：@SaCheckPermission 所用权限串必须有对应 sys_menu.perms 行。）
-- 修复：
--   1. 新增 F 按钮菜单 7699（异常处理，parent=7698，perms=micro:exception:resolve）；
--   2. role_template 对已含 micro:exception:list 的模板幂等追加 micro:exception:resolve；
--   3. 超管 role_id=1 + 存量已授权 7698 的角色补录 sys_role_menu；
--   4. F 类型不在侧边栏渲染，前端无需改动（处理按钮本就无 v-hasPermi 拦截）。
-- 前置依赖：[71]M20_01（7698 异常处理中心菜单）
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- ★幂等：DELETE+INSERT / INSERT IGNORE / JSON_CONTAINS 守卫，可重复执行
-- ---------------------------------------------------------------------
-- v1.1 变更（2026-10-02，v1.68 同批）：
--   末尾嵌套 source [81]M20_11_exception_ack_table.sql（异常处理中心「已处理却还存在」修复：
--   新建租户级确认表 micro_exception_ack，「已处理」按 租户+类型+当月 持久化）。
--   ★登记方式说明：M20_11 经本脚本嵌套 source 挂入执行链，用户执行 00_执行顺序.sql
--     至 [80] 时自动连带执行 M20_11，无需单独手动执行（满足「只跑索引即全量变更」硬规定）。
-- =====================================================================

SET NAMES utf8mb4;

-- ─── 1. 新增 F 按钮菜单 7699（异常处理） ─────────────────────────
DELETE FROM sys_role_menu WHERE menu_id = 7699;
DELETE FROM sys_menu      WHERE menu_id = 7699;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, client_type, icon, create_by, create_time, remark)
VALUES (7699, '异常处理', 7698, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'micro:exception:resolve', 'pc', '#', 'admin', NOW(),
        '★M20_10 异常处理中心「处理」按钮（对齐 MicroExceptionCenterController.resolve 的 @SaCheckPermission）');

-- 超管授权
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 7699);

-- ─── 2. role_template：已含 micro:exception:list 的模板追加 resolve 键 ─────
-- JSON_CONTAINS + NOT JSON_CONTAINS 双重守卫保证幂等（已追加则跳过）
UPDATE role_template
SET menu_keys = JSON_ARRAY_APPEND(menu_keys, '$', 'micro:exception:resolve')
WHERE JSON_CONTAINS(menu_keys, '"micro:exception:list"')
  AND NOT JSON_CONTAINS(menu_keys, '"micro:exception:resolve"');

-- ─── 3. 存量角色补录：已授权 7698 的角色同步授权 7699 ─────────────
-- （能看到异常列表的角色即应能标记处理；INSERT IGNORE 保证幂等）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 7699
FROM sys_role_menu rm
WHERE rm.menu_id = 7698;

-- ─── 3.5 v1.68 连带执行：[81] 业务异常已处理确认表（M20_11） ─────────────
-- 修复「固定支出异常点已处理后刷新仍存在」：派生异常无物理来源记录，
-- 需按 租户+异常类型+当月 持久化确认标记（micro_exception_ack）。
-- ★嵌套 source：执行 00_执行顺序.sql 至 [80] 时自动连带执行，幂等可重复
source M20_11_exception_ack_table.sql;

-- ─── 4. 验收查询（执行后人工核对） ─────────────────────────────────
-- 4.1 菜单 7699 存在且为 F 类型（预期 1 行，parent=7698）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu WHERE menu_id = 7699;
--
-- 4.2 role_template micro_pro boss 含 resolve 键（预期 1）
-- SELECT JSON_CONTAINS(menu_keys, '"micro:exception:resolve"') AS has_resolve
--   FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
--
-- 4.3 存量角色 7698/7699 成对授权（预期每行 has_resolve_menu=1）
-- SELECT r.role_id, r.role_key,
--        MAX(rm.menu_id = 7698) AS has_list_menu,
--        MAX(rm.menu_id = 7699) AS has_resolve_menu
--   FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE rm.menu_id IN (7698, 7699)
--  GROUP BY r.role_id, r.role_key;
--
-- 4.4（v1.68）micro_exception_ack 表已连带创建（预期 1 行 + 唯一索引存在）
-- SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='micro_exception_ack' AND COLUMN_NAME='tenant_id';
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='micro_exception_ack' AND INDEX_NAME='uk_tenant_type_key';
