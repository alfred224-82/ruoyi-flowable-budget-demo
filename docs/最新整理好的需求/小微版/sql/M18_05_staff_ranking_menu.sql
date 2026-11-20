-- =====================================================================
-- M18_05 员工业绩榜菜单脚本（T18.13，v1.29 新增）
-- 版本：v1.0  日期：2026-09-17
-- 依据：《资金管控系统开发任务文档（小微版）》v1.29 M18
--       T18.13 老板员工业绩榜（BE+PC）
-- 前置依赖：M17_01（user_rating 表）、M16（booking_order 表）
-- 规范：
--   1. T18.13 无新建表，复用 booking_order + user_rating + sys_user
--   2. menu_id 占用 7650~7659 区段
--   3. 先删后建幂等
-- =====================================================================

SET NAMES utf8mb4;

-- ===== 菜单 SQL（老板 PC 端员工业绩榜，menu_id 7650~7659）=====

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7650 AND 7659;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7650 AND 7659;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
(7650, '员工业绩榜', 7500, 30, 'staffRanking', 'micro/staffRanking/index', NULL, 1, 0, 'C', '0', '0', 'micro:staffRanking:list', 'pc', 'chart', 'admin', NOW(), '★T18.13 老板员工业绩排行（接单数/营收/评分）');

-- 角色菜单关联（micro_pro 角色）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE m.menu_id BETWEEN 7650 AND 7659
  AND r.role_key = 'micro_pro';

-- ===== 验证 =====
-- SELECT * FROM sys_menu WHERE menu_id BETWEEN 7650 AND 7659;
