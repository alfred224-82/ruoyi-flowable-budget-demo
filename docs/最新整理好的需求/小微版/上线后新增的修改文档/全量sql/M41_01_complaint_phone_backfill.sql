-- =====================================================================
-- M41_01 投诉记录 complainant_phone/name 存量回填（生产修复）
-- =====================================================================
-- 根因：submitComplaint 原代码直接取 bo.complainantPhone / bo.complainantName，
--       但前端 complaint.vue 提交时未传这两个字段 → 落库为 NULL。
--       myComplaints() 按 LoginHelper.getUsername()（手机号）匹配 complainant_phone，
--       NULL 永远匹配不上 → "我的投诉记录"显示为空。
-- 修复：
--   ① 代码侧（MicroComplaintServiceImpl.java）已改为兜底取 LoginHelper 值；
--   ② 本脚本对存量 NULL 记录按 create_by（提交用户ID）回填手机号+昵称。
-- ★幂等：仅 WHERE complainant_phone IS NULL 时更新，可重复执行
-- ★前置：无（纯数据修复）
-- =====================================================================

SET NAMES utf8mb4;

--  存量投诉记录：按 create_by（用户名）回填 complainant_phone / complainant_name
--  ★注意： complaint_record 继承 TenantEntity（无 del_flag），不可加 del_flag 条件
--         ② BaseEntity.createBy 是 String 类型存用户名，不是 user_id，需用 user_name 匹配
UPDATE complaint_record cr
  INNER JOIN sys_user u ON u.user_name = cr.create_by
   SET cr.complainant_phone = u.phonenumber,
       cr.complainant_name  = COALESCE(cr.complainant_name, u.nick_name)
 WHERE cr.complainant_phone IS NULL;

-- ② 验收：不应再有 complainant_phone 为 NULL 的有效投诉记录
SELECT COUNT(*) AS null_phone_count
  FROM complaint_record
 WHERE complainant_phone IS NULL;
-- 预期：0
