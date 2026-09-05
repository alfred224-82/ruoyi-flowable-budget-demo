-- ============================================================
-- M5_11 清理历史编制单数据（年月+部门维度改造后清理旧数据）
-- 说明：
--   1. BG-202607-001 / BG-202608-001 属于运维部门(dept_id=107)的已审批历史数据
--   2. boss 角色属于总公司，应与运维部门完全隔离
--   3. 清理后 boss 保存大类额度时会按部门维度新建独立的编制单
-- ============================================================

-- 1. 删除关联的编制明细（先删明细再删主单）
DELETE FROM budget_preparation_detail
WHERE sheet_id IN (
    SELECT id FROM budget_preparation
    WHERE sheet_no IN ('BG-202607-001', 'BG-202608-001')
);

-- 2. 删除编制单主记录
DELETE FROM budget_preparation
WHERE sheet_no IN ('BG-202607-001', 'BG-202608-001');

-- 验证：确认已清理
-- SELECT COUNT(*) AS remaining FROM budget_preparation WHERE sheet_no IN ('BG-202607-001', 'BG-202608-001');
