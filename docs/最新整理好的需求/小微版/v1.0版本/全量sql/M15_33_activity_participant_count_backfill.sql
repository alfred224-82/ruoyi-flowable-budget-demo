-- =====================================================================
-- M15_33 修复：活动参与人数存量回填（BUG-04，v1.3 问题清单）
-- ---------------------------------------------------------------------
-- 背景：
--   推广活动「参与人数」（promotion_activity.participant_count）由报名/取消
--   流程实时写入。但 v1.3 前 customerSignup/cancelSignup 只落 project_signup
--   明细，未回写活动维度计数，导致：
--     · 活动详情、我的活动列表、Boss 首页数据看板长期显示 0 人参与
--     · 人均费用 perPersonPrice 因分母为 0 无法计算
--
-- 修复范围：
--   1. 应用层：PromotionActivityServiceImpl 已在 customerSignup/cancelSignup
--      末尾调用 refreshParticipantCount()，新数据自动汇总（详见 Java 修改）。
--   2. 数据层：本脚本对存量活动一次性回填 participant_count，
--      来源 = project_signup 按 project_id 汇总 SUM(people_count WHERE is_waitlist=0)。
--
-- 幂等：
--   UPDATE 语句天然幂等，重复执行结果一致；
--   仅回填 selected_project_id 非空的活动（座位布局活动无报名容器，保持原值）。
--
-- 无表结构 / 菜单 / 权限 / 开关变更，纯数据修复。
-- =====================================================================

-- ① 回填 participant_count = SUM(project_signup.people_count WHERE is_waitlist IN (0, NULL))
UPDATE promotion_activity pa
LEFT JOIN (
    SELECT ps.project_id,
           COALESCE(SUM(CASE WHEN ps.is_waitlist = 1 THEN 0 ELSE COALESCE(ps.people_count, 1) END), 0) AS total_people
      FROM project_signup ps
     WHERE ps.del_flag = '0'
     GROUP BY ps.project_id
) s ON s.project_id = pa.selected_project_id
   SET pa.participant_count = COALESCE(s.total_people, 0),
       pa.update_time       = NOW(),
       pa.update_by         = 'M15_33_backfill'
 WHERE pa.del_flag = '0'
   AND pa.selected_project_id IS NOT NULL
   AND (pa.participant_count IS NULL
        OR pa.participant_count <> COALESCE(s.total_people, 0));

-- =====================================================================
-- 验收检查
-- =====================================================================
-- 1) 回填后所有已选项目的活动 participant_count 与 project_signup 汇总一致
-- SELECT pa.id, pa.title, pa.selected_project_id, pa.participant_count,
--        COALESCE(s.total_people, 0) AS expect_count
--   FROM promotion_activity pa
--   LEFT JOIN (
--       SELECT project_id,
--              SUM(CASE WHEN is_waitlist = 1 THEN 0 ELSE COALESCE(people_count, 1) END) AS total_people
--         FROM project_signup WHERE del_flag = '0' GROUP BY project_id
--   ) s ON s.project_id = pa.selected_project_id
--  WHERE pa.del_flag = '0' AND pa.selected_project_id IS NOT NULL
--    AND COALESCE(pa.participant_count, 0) <> COALESCE(s.total_people, 0);
-- 预期：0 行
--
-- 2) 有报名的活动 participant_count > 0
-- SELECT pa.id, pa.title, pa.participant_count
--   FROM promotion_activity pa
--   JOIN project_signup ps ON ps.project_id = pa.selected_project_id AND ps.del_flag = '0'
--  WHERE pa.del_flag = '0'
--  GROUP BY pa.id, pa.title, pa.participant_count
-- HAVING pa.participant_count = 0;
-- 预期：0 行
-- =====================================================================
