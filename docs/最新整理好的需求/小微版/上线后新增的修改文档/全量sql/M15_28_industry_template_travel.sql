-- =====================================================================
-- M15_28 行业模板扩展：旅游行业种子（新增行业只配模板，小程序零改动）
-- 版本：v1.0  日期：2026-10-12
-- 依据：《资金管控系统开发任务文档（小微版）》T15.2/T15.10/T15.33/T15.38 行业模板机制
--       《SaaS 平台需求草稿_最终定稿版 V3.5》第十六章「行业模板与初始化机制」
-- 概述：新增「旅游」行业模板（industry_code=travel），行业化术语映射：
--       房间 → 车辆（preset_config.rooms[] = 车辆，space 分类原子资源，元/小时计价）
--       员工 → 导游（industry_role_map：salesman 标准角色显示名=导游）
--       老板 → 旅行社社长（industry_role_map：boss 标准角色显示名=旅行社社长）
--       顾客 → 游客（industry_role_map：customer 标准角色显示名=游客）
-- 变更内容：
--   1. industry_template 新增 1 行：travel（sort_order=24 / status=0 启用 / voice_enabled=1），
--      preset_config 含 category/service_categories/settle_rule/units/projects[6]/rooms[3 车辆]
--   2. industry_role_map 新增 3 行：旅行社社长→boss / 导游→salesman / 游客→customer
-- 前置依赖：M15_02（industry_template + industry_role_map 建表）、
--           M15_22（23 行业种子，sort_order 占用 1~23）、
--           M15_23（rooms[] 结构约定：rooms[{name,capacity,reference_price}]）、
--           M15_25（voice_enabled 列）
-- ★幂等：INSERT IGNORE + uk_industry_code / uk_industry_bizrole 唯一键保护，可重复执行
-- ★零代码改动：行业模板为系统级共享数据（IGNORE_TENANT_TABLES），执行后 admin「行业模板管理」
--   即见「旅游」行；新建小微版租户行业下拉可选；建租户/首次进入时按模板自动初始化
--   角色映射（tenant_role_mapping）与原子资源（service_category/service_item，rooms→车辆），
--   均幂等仅空表补录，不覆盖存量（V3.5 §16.4 脱钩原则）
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 旅游行业模板主体（INSERT IGNORE 幂等）
--    rooms[] = 车辆（房间对应到车辆）：旅游大巴/中型客车/商务车，reference_price 单位元/小时
--    projects[] = 6 个参考服务项目（跟团/包车/导游/定制小团）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `industry_template` (`industry_code`, `industry_name`, `icon`, `preset_config`, `sort_order`, `status`, `voice_enabled`, `create_by`, `remark`) VALUES
  ('travel', '旅游', '🚌', '{"category":"旅游出行","service_categories":["跟团游","包车服务","导游服务","定制小团"],"settle_rule":"按团次周结","units":["次","天","小时"],"projects":[{"name":"市区经典观光·半日游（含导游讲解）","reference_price":98,"duration":"半天","sample_merchant":"春秋旅游(上海人民广场店)","feature":"纯玩无购物"},{"name":"周边古镇一日游·纯玩团","reference_price":198,"duration":"全天","sample_merchant":"携程当地游(上海)","feature":"含往返车费+导游"},{"name":"包车服务·商务车7座·全天","reference_price":688,"duration":"全天","sample_merchant":"首汽租车(上海)","feature":"含司机油费"},{"name":"导游陪同·讲解服务·半天","reference_price":300,"duration":"半天","sample_merchant":"上海国旅","feature":"持证导游"},{"name":"亲子主题乐园专线·含门票","reference_price":358,"duration":"全天","sample_merchant":"春秋旅游(上海人民广场店)","feature":"含门票+车费"},{"name":"定制小团·2-6人·一日","reference_price":888,"duration":"全天","sample_merchant":"携程当地游(上海)","feature":"行程可定制"}],"rooms":[{"name":"旅游大巴","capacity":"45人","reference_price":200},{"name":"中型客车","capacity":"19人","reference_price":150},{"name":"商务车7座","capacity":"6-7人","reference_price":100}]}', 24, '0', 1, 'admin', '旅游出行·旅游（房间=车辆/员工=导游/老板=旅行社社长；含 6 个参考项目 + 3 车辆预设）');

-- ---------------------------------------------------------------------
-- 2. 行业角色映射 3 条（INSERT IGNORE 幂等）
--    老板→旅行社社长(boss) / 员工→导游(salesman) / 顾客→游客(customer)
--    ★不新增角色 code（开发规范：标准角色集全版本共用，仅做显示名映射）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `industry_role_map` (`industry_code`, `biz_role_name`, `role_code`, `default_flag`, `sort_order`) VALUES
  ('travel', '旅行社社长', 'boss',     1, 1),
  ('travel', '导游',       'salesman', 1, 2),
  ('travel', '游客',       'customer', 1, 3);

-- ---------------------------------------------------------------------
-- 3. 验收查询（执行后人工核对）
-- ---------------------------------------------------------------------
-- 3.1 旅游模板就绪且启用
-- SELECT industry_code, industry_name, sort_order, status, voice_enabled FROM industry_template WHERE industry_code = 'travel';
-- 预期：1 行（travel / 旅游 / 24 / 0 / 1）
--
-- 3.2 预置数据完整（6 项目 + 3 车辆）
-- SELECT JSON_LENGTH(preset_config, '$.projects') AS project_cnt,
--        JSON_LENGTH(preset_config, '$.rooms')    AS room_cnt,
--        JSON_UNQUOTE(JSON_EXTRACT(preset_config, '$.category')) AS cat
--   FROM industry_template WHERE industry_code = 'travel';
-- 预期：6 / 3 / 旅游出行
--
-- 3.3 角色映射 3 条（老板=旅行社社长 / 员工=导游 / 顾客=游客）
-- SELECT biz_role_name, role_code, default_flag FROM industry_role_map WHERE industry_code = 'travel' ORDER BY sort_order;
-- 预期：旅行社社长→boss / 导游→salesman / 游客→customer
--
-- 3.4 前端下拉框（新建小微版租户 → 行业模板下拉应含「旅游」）
-- SELECT industry_code, industry_name FROM industry_template WHERE status = '0' AND del_flag = '0' ORDER BY sort_order;
-- 预期：24 行，末行 travel / 旅游
