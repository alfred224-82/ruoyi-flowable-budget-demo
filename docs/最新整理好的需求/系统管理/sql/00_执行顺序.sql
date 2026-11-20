-- =====================================================================
-- 系统管理（平台底座 / 租户与授权 / License / 开关体系）SQL 执行顺序索引 · v1.32
-- 日期：2026-09-19
-- 依据：《资金管控系统开发任务文档》v1.24（一体化母文档，含 T15.33 行业模板种子数据）
--       《资金管控系统开发任务文档（系统管理）》v1.31
-- v1.32 变更：新增阶段七 [28] M18_10 / [29] M18_11 / [30] M18_12（v1.27 M18 图片合规与全局敏感词，全版本通用·平台级合规底线）：
--             ① M18_10 建 sensitive_word / sensitive_word_hit_log / image_compliance_config 三表（平台级合规底座，tenant_id=0=全局底库）；
--             ② feature_flag 追加第 36/37 项 FEAT_IMAGE_COMPLIANCE / FEAT_SENSITIVE_WORD（组 F=平台合规底线，basic/advanced/pro/micro 四列均 enabled=1，开关总数 35→37）；
--             ③ M18_11 V3.8 附录 A 八大类约 200 条全局底库 INSERT IGNORE 幂等导入 + 旧表 sensitive_word_library→sensitive_word 幂等迁移；
--             ④ M18_12 Admin 平台合规菜单 1370~1380（compliance:sensitiveWord/imageConfig/alert，超管 role_id=1，挂顶级目录与 1300 并列）。
-- v1.31 变更：新增 [26-4] M10_05 feature_flag 补 micro_enabled 列（T10.3 缺口封闭：
--             功能开关管理页面缺少小微版列，补 micro_enabled TINYINT DEFAULT 0 列，
--             组 E 开关（FEAT_PROMOTION_DISTRIBUTION / FEAT_RATING）回填 micro_enabled=1，
--             组 A~D 企业版开关保持 micro_enabled=0；幂等存储过程，重复执行不报 1060）
-- v1.30 变更：新增 [22-5] M15_22 行业模板种子数据填充（T15.33 v1.24 新增：
--             将 industry_template 启用行业从 4 个扩展至 23 个（休闲娱乐 6 / 生活服务 7 /
--             运动健身 7 / 专业工作室 3）+ 69 条 industry_role_map + 每行业 preset_config
--             （category/service_categories/settle_rule/units/projects[5~7 条大众点评上海真实团购]）；
--             INSERT IGNORE + UPDATE 双重幂等；market SET status='1' 停用保留兼容既有租户；
--             修复管理员手册 §2.2 「新建租户行业模板下拉仅 4 项」与实际业务 23 行业不匹配问题）
-- v1.29 变更：新增 [26-3] M10_03 biz_copy 业态文案映射表（T10.5 v1.29 新增：
--             系统级共享表，一行一 biz_code×i18n_key×lang，配合 biz_bundle.copy(JSON)
--             实现多语言精细化覆盖；已纳入 MybatisPlusConfig.IGNORE_TENANT_TABLES；
--             三维合并优先级：租户 module_overrides.copy > biz_copy > biz_bundle.copy > 前端硬编码兜底；
--             修复 BizCopyController/BizCopyServiceImpl 查询报 Table 'biz_copy' doesn't exist）
-- v1.28 变更：新增 [22-4] M3_07 sys_tenant 移除 scale 列（T3.2 v1.28 变更：
--             租户分类完全由 tenant_type + version_type 决定，不再需要规模维度；
--             幂等存储过程 INFORMATION_SCHEMA 条件判断，重复执行不报错）
-- v1.25 变更：新增 [26-2] M10_02 平台运营菜单补建（§5.4 单店覆盖 + §5.5 访问通道 +
--             §5.7 计费/合规菜单收纳到 1300；修复管理手册第五章菜单不可见）
-- v1.24 变更：新增 [12-2] M15_09 sys_role 补 tenant_type_scope 列（从 M15_09 提取，
--             新建租户依赖；修复 Unknown column 'tenant_type_scope'）
-- 口径：本目录为《系统管理》分册承载的 M1 / M2 / M3 / M3.5 / M4 / M9 / M10 共 46 项任务
--       的【自包含可独立部署】脚本集，是平台侧（admin / platform_admin）最小部署单元。
-- 说明：
--   1. 全部脚本幂等，可重复执行；严格按序号执行，不可颠倒；
--   2. 建表一次建齐，版本靠菜单模板（M2_02 / M2_10）+ 功能开关（M1_05 feature_flag）区分，
--      故本包含 M1_03（高级/专业版增量表）——表结构全版本共用，不代表基础版开放这些能力；
--   3. M9（升级流程与防串验收）为验证类里程碑，交付物是演练报告与测试报告，无 SQL 脚本；
--   4. 业态功能（M5 预算域 / M6 高级增量 / M7 专业版增量 / M11~M14 业态与商业化）
--      与小微版（M15 / M16）脚本不在本包，分别见《高级版》《专业版》《小微版》目录；
--   5. ★版本谱系（v1.16）：小微版（MICRO，micro_lite / micro_pro）为独立产品线，
--      企业版谱系为 BASIC / ADVANCED / PROFESSIONAL（存量 ENTERPRISE 由 normalizeEdition 归一化）。
--   6. ★硬规定（v1.27 新增，不可覆盖规则）：
--      (1) 本索引中所有 M 开头脚本均不可覆盖此前已部署版本的数据和表结构；
--      (2) 建表必须使用 CREATE TABLE IF NOT EXISTS 或幂等存储过程；
--      (3) 数据插入必须使用 INSERT IGNORE / INSERT ... ON DUPLICATE KEY UPDATE / 条件 INSERT；
--      (4) ALTER TABLE 必须使用 INFORMATION_SCHEMA 条件判断列/索引是否已存在；
--      (5) 禁止在任何脚本中出现无条件的 DROP TABLE / DROP COLUMN / TRUNCATE / DELETE 全表；
--      (6) 先删后建（DROP + CREATE）仅允许在初始化建表脚本中对自身创建的表使用，
--          不得删除其他脚本或版本建立的表或数据。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 阶段一：数据库底座与平台底座建表（M1，T1.1~T1.9）
-- ---------------------------------------------------------------------
-- [01] 存量表补 tenant_id + 索引（幂等存储过程，重复执行不报 1060/1061）
source M1_01_existing_tables_add_tenant_id.sql;
-- [02] 基础版业务表（fee_project / fee_payment / fee_receipt / fee_income_confirmation /
--      fee_project_change / fee_bad_debt / fee_project_link_rule / fee_invoice 等）
source M1_02_create_tables_basic.sql;
-- [03] 高级/专业版增量表主体（fee_employee_credit / fee_credit_rule / fee_clock_record /
--      fee_org / fee_org_budget_version / fee_face_verify_record / fee_rule_version 等）
--      ★建表一次建齐，能力开放由 feature_flag + 菜单模板控制，与本包版本无关
source M1_03_create_tables_advanced_pro.sql;
-- [04] sys_menu 全量菜单（M1_04 v1.1 幂等补 client_type 列，本包不含小微版菜单）
source M1_04_sys_menu.sql;
-- [05] ★平台底座三表：biz_bundle（3 业态包）/ feature_flag（33 项开关，组 A~D）/
--      sys_edition_menu_template 等；T1.7 / T10.2 / T10.3 的数据基础
source M1_05_platform_foundation_tables.sql;
-- [06] sys_tenant 扩展三字段（biz_code / access_channel / wecom_*），T1.8 / T10.1
source M1_06_sys_tenant_biz_channel.sql;
-- [07] sys_tenant 补 tenant_type / version_type 列 + 存量回填 + 索引（T15.25 / T15.26，
--      从 M15_10 提取补列部分，无 role_template 依赖，企业版部署路径独立可执行；
--      与小微版 M15_10 幂等兼容，重复执行不报错）
source M15_15_sys_tenant_type_version_columns.sql;
-- [08] project 统一容器四表（project / project_member / project_task / project_signup），T1.9
source M1_07_project_container_tables.sql;
-- [08-2] 主题预设表 biz_theme_preset（系统级共享 Design Token，T12.2 补建）
source M12_02_biz_theme_preset.sql;
-- [08-3] ★角色模板表 role_template（系统级共享，无 tenant_id，T15.1 / T15.7；
--        从 M15_01 提取单表建表，企业版部署路径必需；CREATE TABLE IF NOT EXISTS 幂等，
--        与小微版 M15_01 DROP+CREATE 兼容；RoleTemplateServiceImpl / PageBlockConfigServiceImpl
--        等多处引用，表不存在会报 Table 'role_template' doesn't exist）
source M15_01_role_template.sql;

-- ---------------------------------------------------------------------
-- 阶段二：角色与授权模板（M2，T2.1~T2.5）
-- ---------------------------------------------------------------------
-- [09] 标准角色集（boss / project_manager / expense_staff / finance / salesman / customer）
source M2_01_roles.sql;
-- [10] ★角色×版本授权模板 sys_edition_menu_template（*_BASIC / *_ADVANCED / *_PROFESSIONAL）
--      六层防串第④层「菜单授权」的数据源，必须早于 M2_05
source M2_02_role_menu_templates.sql;
-- [11] 基础版 sys_role_menu 授权
source M2_03_sys_role_menu_basic.sql;
-- [12] sys_dept / sys_role 补 tenant_id + 索引（幂等存储过程），T2.4 数据隔离
source M2_04_tenant_isolation.sql;
-- [12-2] ★sys_role 补 tenant_type_scope 列（micro/enterprise/both）+ 存量回填 + 索引
--        （T15.25 角色可见性硬隔离；从 M15_09 提取补列部分，无小微版前置依赖，
--         新建租户依赖此列；与小微版 M15_09 幂等兼容，重复执行不报错；
--         修复 MyBatis-Plus 全量 SELECT sys_role 报 Unknown column 'tenant_type_scope'）
--        ★前置：[12]M2_04（sys_role 表已含 tenant_id，标准角色集 [09]M2_01 已建）
source M15_09_sys_role_tenant_type_scope.sql;
-- [13] platform_admin 平台管理员升级
--      ★已知陷阱：本脚本会重建模板表，必须在 M2_02 之后执行，否则摧毁角色×版本键
--        导致业务角色菜单为空
source M2_05_platform_admin_upgrade.sql;
-- [14] 存量用户-租户归属修复
source M2_06_user_tenant_fix.sql;
-- [15] 租户根部门回填
source M2_07_tenant_root_dept_backfill.sql;
-- [16] 租户角色克隆迁移（共享预设角色 -> 租户私有角色）
source M2_08_tenant_role_clone_migration.sql;
-- [17] sys_tenant.max_roles 角色数上限（基础版 2 / 高级版 4-6 / 专业版 NULL 不限）
source M2_09_tenant_max_roles.sql;
-- [19] ★下移菜单模板迁移（11 类资金管控重能力由 *_BASIC 迁至 *_ADVANCED / *_PROFESSIONAL）
source M2_10_downshift_menu_templates.sql;

-- ---------------------------------------------------------------------
-- 阶段三：定时任务与 License 体系（M3 / M3.5，T3.1~T3.15）
-- ---------------------------------------------------------------------
-- [19] XXL-JOB 租户到期检查任务登记（tenantExpireCheckJob）
source M3_01_xxl_job_tenant_expire_check.sql;
-- [20] XXL-JOB 全量任务登记
source M3_02_xxl_job_all_tasks.sql;
-- [21] sys_license 与租户关联（tenant_id NULL=平台级 / 非 NULL=租户级），T3.10 / T3.11
source M3_05_license_tenant_association.sql;
-- [22] License 管理菜单
source M3_06_license_management_menu.sql;
-- [22-2] ★M14 商业化计费与合规表（11 张表 + 四档套餐 + 协议版本 + 平台计费/合规菜单，
--        T14.1~T14.4；管理员操作手册 §5.7 计费套餐/订单/分账/提现/对账/合规全部依赖；
--        从 M14_01 + M14_02 提取，CREATE TABLE IF NOT EXISTS 幂等）
source M14_01_billing_commerce_tables.sql;
-- [22-3] ★M15_02 行业模板表（industry_template + industry_role_map + 4 行业初始数据，
--        T15.2；管理员操作手册 §2.2 新建租户「行业模板」下拉依赖；
--        从 M15_02 提取，CREATE TABLE IF NOT EXISTS 幂等）
source M15_02_industry_template.sql;
-- [22-4] ★M3_07 sys_tenant 移除 scale 列（v1.28 T3.2 变更：租户分类完全由
--        tenant_type + version_type 决定，不再需要规模维度；幂等存储过程，
--        INFORMATION_SCHEMA 条件判断，重复执行不报错；建议先发版再执行本 SQL）
source M3_07_sys_tenant_drop_scale.sql;
-- [22-5] ★M15_22 行业模板种子数据填充（v1.30 新增，T15.33，V3.5 §16 + 初始化项目清单_上海.xlsx）：
--        23 个二级行业（休闲娱乐 6 / 生活服务 7 / 运动健身 7 / 专业工作室 3）× preset_config
--        （category/service_categories/settle_rule/units/projects 5~7 条大众点评上海真实团购）
--        + 69 条 industry_role_map（boss/salesman/customer 三标准角色 + 行业化业务角色显示名）
--        + market（买菜/生鲜）SET status='1' 停用（V3.5 §10 明确排除零售/生鲜），保留数据兼容既有租户
--        ★幂等：INSERT IGNORE + UPDATE 双重（uk_industry_code / uk_industry_bizrole 保护）
--        ★前置：[22-3] M15_02（industry_template + industry_role_map 建表）
source M15_22_industry_template_seed_v35.sql;

-- ---------------------------------------------------------------------
-- 阶段四：版本编码归一化（M4，T4.1~T4.5）
-- ---------------------------------------------------------------------
-- [23] ENTERPRISE -> PROFESSIONAL 历史编码兼容标注（不写迁移、不改存量生效值，
--      归一化统一由 EditionFeatureConfig#normalizeEdition 承担，T10.7）
source M4_01_edition_enterprise_to_professional.sql;

-- ---------------------------------------------------------------------
-- 阶段五：平台底座管理菜单（M10，T10.2 / T10.3 + T15.29 菜单收纳）
-- ---------------------------------------------------------------------
-- ★顺序强约束（不可颠倒）：M15_12 以
--     DELETE FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359
--   开局，而 M10_01 的「业态包管理」(1320~1322) 与「功能开关管理」(1330~1332) 正落在该区段内。
--   必须【先 M15_12 建 1300 一级目录 + 1310~1314 版本档位管理，再 M10_01】，
--   颠倒则 M10_01 建出的菜单会被 M15_12 的 DELETE 清除。
-- [24] 「资金平台管理菜单」一级目录（menu_id 1300）+ 版本档位管理（1310~1314）
--      + 平台自有管理菜单 re-parent（按 perms 匹配，目标不存在即 no-op）。
--      任务归属 M15（T15.29，小微版方案 v10.3 引入），但产出的是【平台级 admin 菜单】，
--      跨全部版本共用，故一并纳入本系统管理包。
source M15_12_fund_platform_menu_edition.sql;
-- [25] ★角色模板管理菜单（7005~7007，挂资金平台管理菜单 1300 下，修复 edition 页 404；
--      admin 定义总体角色配方，租户管理员自行定义映射角色，新建租户表单不展示角色预览）
source M15_03_role_template_menu.sql;
-- [26] 业态包管理（system:bizBundle:*，1320~1322）+ 功能开关管理（system:featureFlag:*，1330~1332）
--      六层防串第①层「版本分配入口」的 PC 管理页（T10.2 / T10.3），全部授予超管 role_id=1。
--      前置：M1_05（biz_bundle / feature_flag 建表）已在阶段一执行。
source M10_01_biz_bundle_feature_flag_menu.sql;
-- [26-2] ★平台运营菜单补建（T10.4/T13/T14 菜单缺口）：
--        §5.4 单店覆盖（1340~1342，system:tenantBizConfig:*，挂 1300 下）+
--        §5.5 访问通道管理（1343~1345，system:message:*，挂 1300 下）+
--        §5.7 计费(6120)/合规(6160)菜单 re-parent 到 1300（M14_01 建顶级，未收纳）
--        ★前置：[24]M15_12（1300 目录已建）+ [22-2]M14_01（6120/6160 菜单已建）
source M10_02_platform_operation_menu.sql;
-- [26-3] ★业态文案映射表 biz_copy（T10.5 v1.29 新增，系统级共享，无 tenant_id）：
--        三维合并优先级：租户 module_overrides.copy > biz_copy(biz_code,lang) > biz_bundle.copy > 前端硬编码兜底；
--        与 biz_bundle.copy(JSON 单语言) 互补，biz_bundle.copy 作 zh-CN 兜底，biz_copy 提供多语言精细化覆盖；
--        内置 fund_control / boardgame / fresh_food 三业态包 zh-CN 初始数据 + fund_control en-US 示范；
--        附带「文案映射管理」菜单 1350~1352（挂资金平台管理菜单 1300 下，perms system:bizCopy:*）。
--        ★前置：[24]M15_12（1300 目录已建）；MybatisPlusConfig.IGNORE_TENANT_TABLES 已含 'biz_copy'
--        修复：BizCopyController / BizCopyServiceImpl 查询报 Table 'biz_copy' doesn't exist
source M10_03_biz_copy_i18n.sql;
-- [26-4] ★feature_flag 补 micro_enabled 列（T10.3 缺口封闭，v1.31 新增）：
--        功能开关管理页面缺少小微版列；补 micro_enabled TINYINT DEFAULT 0 列（AFTER pro_enabled），
--        组 E 开关（FEAT_PROMOTION_DISTRIBUTION / FEAT_RATING）回填 micro_enabled=1，
--        组 A~D 企业版开关保持 micro_enabled=0；幂等存储过程 INFORMATION_SCHEMA 条件判断，重复执行不报 1060。
--        ★前置：[05]M1_05（feature_flag 建表）+ M16_03（第 34 项）+ M17_01（第 35 项，小微版包）
source M10_05_feature_flag_micro_column.sql;

-- ---------------------------------------------------------------------
-- 阶段六：存量租户菜单对齐（M2 收尾，必须在全部菜单模板脚本之后）
-- ---------------------------------------------------------------------
-- [27] 存量基础版租户克隆角色菜单对齐修复（v1.12 / 手册 v2.0 验收）：
--      回收空壳「预算管理」目录 2000 等下移菜单 + 按 *_BASIC 模板补齐 boss/employee 授权。
--      前置：M2_02 / M2_03 / M2_10 均已执行。
source M2_11_basic_boss_menu_realign.sql;

-- ---------------------------------------------------------------------
-- 阶段七：M18 图片合规与全局敏感词（v1.27 新增，全版本通用·平台级合规底线）
-- ---------------------------------------------------------------------
-- [28] T18.1/T18.8 敏感词/命中日志/图片合规配置三表（平台级合规底座，tenant_id=0=全局底库）
--      + feature_flag 追加第 36/37 项 FEAT_IMAGE_COMPLIANCE/FEAT_SENSITIVE_WORD（组 F，四列均 enabled=1，35→37）
--      前置：[05]M1_05（feature_flag 表）+ [26-4]M10_05（micro_enabled 列，存在则回填）；三表走条件查询而非 IGNORE_TENANT_TABLES 白名单
source M18_10_sensitive_word_image_compliance.sql;
-- [29] T18.7 全局敏感词底库初始化（V3.8 附录 A 八大类约 200 条，INSERT IGNORE 幂等）+ 旧表 sensitive_word_library→sensitive_word 幂等迁移（无旧表则 no-op）
--      前置：[28]M18_10（sensitive_word 表）
source M18_11_sensitive_word_init_data.sql;
-- [30] T18.4/T18.5 Admin 平台合规菜单 1370~1380（compliance:sensitiveWord/imageConfig/alert，超管 role_id=1，与 1300 资金平台管理并列顶级目录）
--      ★小微版专属菜单 7680~7690 在企业版无对应角色，INSERT IGNORE no-op，安全
--      前置：[03]M1_04（sys_menu）+ [28]M18_10（三表）
source M18_12_compliance_menu.sql;

-- =====================================================================
-- 验收检查（系统管理）
-- =====================================================================
-- 1) 平台底座表 + 角色模板 + 计费套餐 + 行业模板就绪
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('biz_bundle','feature_flag','tenant_biz_config','role_template',
--     'sys_billing_plan','sys_billing_order','sys_agreement_version',
--     'industry_template','industry_role_map');  -- 预期 >= 9

-- 2) 35 项功能开关落表且分组正确（组 A 11 / 组 B 9 / 组 C 6 / 组 D 7 / 组 E 2）
-- SELECT flag_group, COUNT(*) FROM feature_flag GROUP BY flag_group;  -- 预期 A=11 B=9 C=6 D=7 E=2
-- 2.1 micro_enabled 列已存在且组 E 开关为 1
-- SELECT code, flag_group, micro_enabled FROM feature_flag WHERE flag_group = 'E';
-- 预期：FEAT_PROMOTION_DISTRIBUTION / FEAT_RATING 两行，micro_enabled=1

-- 3) 3 个内置业态包就绪
-- SELECT biz_code, name, status FROM biz_bundle;  -- 预期 fund_control / boardgame / fresh_food

-- 4) ★资金平台管理菜单目录 + 版本档位 + 业态包/开关/单店覆盖/通道/计费/合规齐备
-- SELECT menu_id, menu_name, parent_id, order_num, perms FROM sys_menu
--  WHERE parent_id = 1300 ORDER BY order_num;
-- 预期含：租户管理/标准角色/行业模板/菜单管理/角色模板/版本档位管理/
--        业态包管理(1320)/功能开关管理(1330)/★单店覆盖(1340)/★访问通道(1343)/
--        ★平台计费(6120)/★合规管理(6160)

-- 5) ★六层防串第①层入口可达：超管已获授权
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 1300 AND 1359;
-- 预期 >= 18（1300+1310~1314+1320~1322+1330~1332+1340~1345）

-- 6) 角色×版本授权模板未被 M2_05 摧毁
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template GROUP BY edition;  -- 预期各版本 > 0

-- 7) 版本编码归一化：不存在残留 ENTERPRISE 生效编码
-- SELECT edition, COUNT(*) FROM sys_tenant GROUP BY edition;

-- 8) 角色数上限分层：基础版 2 / 高级版 4-6 / 专业版 NULL（不限）
-- SELECT edition, max_roles, COUNT(*) FROM sys_tenant GROUP BY edition, max_roles;

-- 9) ★v1.28 租户「规模」字段已移除
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'scale';
-- 预期：0

-- 10) ★v1.29 biz_copy 业态文案映射表就绪，三业态包 zh-CN 文案齐备
-- SELECT biz_code, COUNT(*) AS cnt FROM biz_copy WHERE lang='zh-CN' GROUP BY biz_code;
-- 预期：fund_control >= 6, boardgame >= 5, fresh_food >= 5
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 1350 AND 1352;
-- 预期：3 条（文案映射管理 / 文案查询 / 文案编辑）

-- 11) ★v1.30 M15_22 行业模板种子数据已扩展至 23 个（T15.33）
-- SELECT COUNT(*) FROM industry_template WHERE del_flag='0';                     -- 预期 24（23 启用 + 1 停用 market）
-- SELECT COUNT(*) FROM industry_template WHERE del_flag='0' AND status='0';      -- 预期 23
-- SELECT JSON_UNQUOTE(JSON_EXTRACT(preset_config,'$.category')) AS cat, COUNT(*) AS cnt
--   FROM industry_template WHERE status='0' AND del_flag='0' GROUP BY cat ORDER BY cat;
--   -- 预期 4 行：休闲娱乐 6 / 生活服务 7 / 运动健身 7 / 专业工作室 3
-- SELECT COUNT(*) FROM industry_role_map WHERE industry_code<>'market';          -- 预期 ≥69
-- SELECT industry_code, status FROM industry_template WHERE industry_code='market';
--   -- 预期 status='1'（停用），保留数据兼容既有租户 industry_code=market

-- 12) ★v1.32 M18 图片合规与全局敏感词（全版本通用·平台级合规底线，[28]M18_10/[29]M18_11/[30]M18_12）
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('sensitive_word','sensitive_word_hit_log','image_compliance_config');  -- 预期 3
-- SELECT code, flag_group, basic_enabled, advanced_enabled, pro_enabled, micro_enabled, status FROM feature_flag
--   WHERE code IN ('FEAT_IMAGE_COMPLIANCE','FEAT_SENSITIVE_WORD');  -- 预期 2 行，flag_group='F'，四列 enabled 均=1，status='0'
-- SELECT COUNT(*) FROM feature_flag;  -- 预期 37（原 35 + M18 双开关 2）
-- SELECT COUNT(*) FROM sensitive_word WHERE tenant_id=0 AND source='global';  -- 预期 ≥200
-- SELECT category, COUNT(*) FROM sensitive_word WHERE tenant_id=0 GROUP BY category;  -- 预期 8 类（POLITICAL/AD/PORN/VIOLENCE/SUPERSTITION/ABUSE/RUMOR/DIALECT）
-- SELECT violation_type, confidence_threshold FROM image_compliance_config WHERE tenant_id=0;  -- 预期 porn/violence/politics 三行
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 1370 AND 1389;  -- 预期 11（1370 目录 + 3 C 菜单 + 7 F 按钮）
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id=1 AND menu_id BETWEEN 1370 AND 1389;  -- 预期 11

-- =====================================================================
-- 本包不含的里程碑（归属其他分册，避免重复部署）
-- ---------------------------------------------------------------------
-- · M5 预算域 / M6 高级版增量        -> 《高级版/sql/》（基础版部分见《基础版/sql/》）
-- · M7 专业版增量（多组织/多账套/开放 API） -> 《专业版/sql/》
-- · M8 小程序（M8_01 / M8_02）        -> 各版本包均有（引导态门店与租户档案为全版本共用）
-- · M11~M14 项目容器/业态功能/访问通道/商业化 -> 《高级版/sql/》《专业版/sql/》
-- · M15 / M16 小微版专属              -> 《小微版/sql/》
--   （例外：M15_12「资金平台管理菜单」为平台级 admin 菜单，跨版本共用，已纳入本包阶段五；
--    M15_15 sys_tenant 补 tenant_type/version_type 列，企业版部署路径必需，已纳入阶段一 [07]；
--    M15_01 role_template 角色模板表，系统级共享，企业版部署路径必需，已纳入阶段一 [08-3]；
--    M14_01/M14_02 计费合规表（11 张）原属专业版路径，已纳入本包阶段三 [22-2]；
--    M15_02 行业模板表（2 张）原属小微版路径，已纳入本包阶段三 [22-3]）
--    M14_01/M14_02 计费合规表（11 张）原属专业版路径，已纳入本包阶段三 [22-2]；
--    M15_02 行业模板表（2 张）原属小微版路径，已纳入本包阶段三 [22-3]；
--    M15_22 行业模板种子数据填充（T15.33 v1.30 新增，23 行业 + 69 role_map）原属小微版路径，已纳入本包阶段三 [22-5]；
--    M15_03 角色模板管理菜单（7005~7007）原仅小微版 M15_06 定义，系统管理包补建，已纳入阶段五 [25]；
--    M15_09 sys_role 补 tenant_type_scope 列（角色可见性硬隔离），新建租户依赖，已纳入阶段二 [12-2]；
--    M10_02 平台运营菜单补建（单店覆盖/访问通道/计费合规收纳），已纳入阶段五 [26-2]；
--    M10_03 biz_copy 业态文案映射表（T10.5 v1.29 新增，系统级共享），已纳入阶段五 [26-3]）
-- · M9 升级流程与防串验收             -> 无 SQL，交付物为演练报告 / 测试报告
-- =====================================================================
