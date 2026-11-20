-- =====================================================================
-- M15_30  旅游行业模板补「司机」角色映射（T26.10 背景图加人配套）
-- =====================================================================
-- 需求概述：
--   老板发布活动时在背景画布上「加人」（导游/司机/游客）。导游、司机均为
--   员工（salesman 标准角色）的行业显示名映射，由旅游行业模板定义；
--   M15_28 仅配了「导游」，本脚本为旅游模板补充「司机」映射。
--
-- 变更：
--   1. industry_role_map 新增 1 行：('travel','司机','salesman',0,4)
--      ★default_flag=0：租户级 tenant_role_mapping 受 uk_tenant_role(tenant_id,role_code)
--        约束，salesman 已映射「导游」，司机行不自动落到租户映射（initTenantByIndustry
--        按 role_code 去重跳过），仅作为行业模板层的业务角色定义，供后台
--        「行业模板管理→角色映射」查看/编辑，租户可按需将员工显示名改名为「司机」。
--   2. industry_template(travel).remark 同步补「司机」术语说明。
--   3. ★无菜单/权限/开关/表结构变更，执行后无需刷新权限快照。
--
-- 前置：
--   [121]M15_28（旅游行业模板种子）
--
-- 幂等：INSERT IGNORE（uk_industry_bizrole(industry_code,biz_role_name) 唯一键保护）
--       + remark 更新带 NOT LIKE 守卫
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 行业角色映射补「司机」（INSERT IGNORE 幂等）
--    ★不新增角色 code（开发规范：标准角色集全版本共用，仅做显示名映射）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `industry_role_map` (`industry_code`, `biz_role_name`, `role_code`, `default_flag`, `sort_order`) VALUES
  ('travel', '司机', 'salesman', 0, 4);

-- ---------------------------------------------------------------------
-- 2. 旅游模板 remark 同步（已含「司机」则跳过）
-- ---------------------------------------------------------------------
UPDATE `industry_template`
   SET `remark` = '旅游出行·旅游（房间=车辆/员工=导游/司机/老板=旅行社社长；含 6 个参考项目 + 3 车辆预设）'
 WHERE `industry_code` = 'travel'
   AND (`remark` IS NULL OR `remark` NOT LIKE '%司机%');

-- ---------------------------------------------------------------------
-- 3. 验收查询（执行后人工核对）
-- ---------------------------------------------------------------------
-- 3.1 旅游模板角色映射含司机
-- SELECT biz_role_name, role_code, default_flag, sort_order
--   FROM industry_role_map WHERE industry_code = 'travel' ORDER BY sort_order;
-- 预期：4 行（旅行社社长→boss/1、导游→salesman/1、游客→customer/1、司机→salesman/0）
--
-- 3.2 remark 已同步
-- SELECT remark FROM industry_template WHERE industry_code = 'travel';
-- 预期：含 '司机'
