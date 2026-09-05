-- =====================================================================
-- 高级版（ADVANCED）SQL 执行顺序索引 · v1.12
-- 日期：2026-09-05
-- 依据：《多业态SaaS平台需求规格说明书》v1.0、《资金管控系统开发任务文档》v1.12
-- 口径：高级版 = 基础版全部能力 + 承接 v1.12 下移的 11 类资金管控重能力
--       + 高级版增量（部门预算/打卡/信用/规则自定义/利润报表/自定义模板/证据链）
--       + 企业微信访问通道（access_channel=wecom）。
-- 说明：
--   1. 本目录为「高级版自包含可独立部署」脚本集（累计制：含基础版全部脚本）；
--   2. 全部脚本幂等，可重复执行；严格按序号执行，不可颠倒；
--   3. 升级路径：基础版租户升级高级版时，阶段一~八已执行，仅需补执行阶段九~十二，
--      并由管理员在租户管理执行「版本分配 → ADVANCED」触发授权模板与功能开关刷新（数据零迁移）。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 阶段一~八：与基础版一致（数据库底座 / 平台底座 / 角色授权 / 定时任务·License /
--            审计字段 / 引导态门店 / 全量业态能力 / 计费合规）
--            详见同目录各脚本，执行顺序与《基础版/sql/00_执行顺序.sql》阶段一~八完全相同：
--   M1_01 → M1_02 → M1_04 → M1_05 → M1_06 → M1_07
--   → M2_01 → M2_02 → M2_03 → M2_04 → M2_05 → M2_06 → M2_07 → M2_08 → M2_09 → M2_10
--   → M3_01 → M3_02 → M3_05 → M3_06 → M4_01 → M5_01
--   → M8_01_sys_tenant_store_profile → M8_01_portal_store_search_and_audit
--   → M11_01 → M12_01 → M14_01 → M14_02 → M14_03
-- ---------------------------------------------------------------------
source M1_01_existing_tables_add_tenant_id.sql;
source M1_02_create_tables_basic.sql;
source M1_04_sys_menu.sql;
source M1_05_platform_foundation_tables.sql;
source M1_06_sys_tenant_biz_channel.sql;
source M1_07_project_container_tables.sql;
source M2_01_roles.sql;
source M2_02_role_menu_templates.sql;
source M2_03_sys_role_menu_basic.sql;
source M2_04_tenant_isolation.sql;
source M2_05_platform_admin_upgrade.sql;
source M2_06_user_tenant_fix.sql;
source M2_07_tenant_root_dept_backfill.sql;
source M2_08_tenant_role_clone_migration.sql;
source M2_09_tenant_max_roles.sql;
source M2_10_downshift_menu_templates.sql;
source M3_01_xxl_job_tenant_expire_check.sql;
source M3_02_xxl_job_all_tasks.sql;
source M3_05_license_tenant_association.sql;
source M3_06_license_management_menu.sql;
source M4_01_edition_enterprise_to_professional.sql;
source M5_01_alter_tables_audit_columns.sql;
source M8_01_sys_tenant_store_profile.sql;
source M8_01_portal_store_search_and_audit.sql;
source M11_01_project_container_menu.sql;
source M12_01_activity_theme_live.sql;
source M14_01_billing_settlement.sql;
source M14_02_compliance.sql;
source M14_03_xxl_job_billing_remind.sql;

-- ---------------------------------------------------------------------
-- 阶段九：高级/专业版增量表一次建齐（建表 ≠ 开放，开放由开关控制）
-- ---------------------------------------------------------------------
-- [30] 高级版增量表（fee_employee_credit/fee_credit_rule/fee_clock_record/fee_mapping_audit_log/
--      fee_elastic_quota_config）+ 专业版增量表（fee_org/fee_org_budget_version/fee_face_verify_record/
--      fee_rule_version/fee_org_user）+ 预算表 department_id 双粒度 + 报销表 evidence_level
source M1_03_create_tables_advanced_pro.sql;

-- ---------------------------------------------------------------------
-- 阶段十：预算域增强（v1.12 由基础版下移至此，FEAT_SUBJECT_3LAYER / FEAT_BUDGET_PREPARE）
-- ---------------------------------------------------------------------
-- [31] 费用大类主数据 + 拆分比例 + 财务科目导入建表（约 6 个大类内置）
source M5_02_create_tables_category.sql;
-- [32] 费用大类维护 / 拆分比例配置菜单与授权（3310~3315）
source M5_03_sys_menu_expense_category.sql;
-- [33] 老板大类额度权限（按年月 + 部门维度，含调整下限控制）
source M5_04_boss_category_quota.sql;
-- [34] 财务科目导入与拆分初稿（建表 + 菜单 3316~3322 + 角色授权）
source M5_05_budget_domain_enhance.sql;
-- [35] 租户大类启用表 fee_tenant_category + fee_expense_category 系统级改造
source M5_06_tenant_category_and_system_level.sql;
-- [36] 租户大类勾选菜单（platform_admin）
source M5_07_tenant_category_menu.sql;
-- [37] platform_admin 大类配置菜单
source M5_08_platform_admin_category_menu.sql;
-- [38] platform_admin 大类配置修正
source M5_09_platform_admin_category_fix.sql;
-- [39] 预算年月 + 部门维度升级（budget_preparation 补 department_id/budget_year/budget_month + 索引）
source M5_10_budget_dimension_upgrade.sql;
-- [40] 历史预算记录清理（可选，幂等）
source M5_11_cleanup_historical_budget_records.sql;

-- ---------------------------------------------------------------------
-- 阶段十一：高级版增量能力（打卡/信用/部门预算/规则自定义/利润报表/自定义模板/证据链）
-- ---------------------------------------------------------------------
-- [41] 高级版增量脚本（内置打卡规则/信用规则模板等）
source M6_01_advanced_incremental.sql;
-- [42] 信用分重算定时任务注册（creditScoreCalcJob，每周一 02:00，基础版租户跳过）
source M6_02_xxl_job_credit_score.sql;
-- [43] ★承接 M5 下移能力的菜单迁移（T6.8②：下移菜单从 *_BASIC 迁至 *_ADVANCED/*_PROFESSIONAL，
--      回收基础版租户克隆角色授权；接口层 @RequireFeature 由后端代码落地）
source M6_03_downshift_menu_migration.sql;

-- ---------------------------------------------------------------------
-- 阶段十二：企业微信访问通道（高级版通道，access_channel=wecom）
-- ---------------------------------------------------------------------
-- [44] 访问通道建表 + tenant_payment_config 收款配置 + 企微凭据字段（FEAT_WECOM）
source M13_01_access_channel.sql;

-- =====================================================================
-- 验收检查（高级版）
-- =====================================================================
-- 1) 高级版增量表就绪
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('fee_employee_credit','fee_credit_rule','fee_clock_record',
--                      'fee_mapping_audit_log','fee_elastic_quota_config');  -- 预期：5

-- 2) 预算域增强表就绪（费用大类 / 拆分比例 / 财务科目 / 租户大类）
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('fee_expense_category','fee_category_split_ratio','fee_finance_subject','fee_tenant_category');  -- 预期：4

-- 3) ★高级版模板已含下移菜单（角色保全，预期 > 0）
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template
--  WHERE edition LIKE '%\_ADVANCED' AND menu_id IN (2001,2003,3208,3209,3215,3216,3204,3205,3310,3318)
--  GROUP BY edition;

-- 4) 高级版功能开关（GET /system/edition/features，租户 edition=ADVANCED）
--    预期含组 A 11 项（FEAT_BAD_DEBT/FEAT_LINK_RULE/FEAT_INVOICE_SUPPLEMENT/FEAT_HIDDEN_COST/
--    FEAT_BOSS_DASHBOARD/FEAT_VOUCHER_EXPORT/FEAT_SUBJECT_3LAYER/FEAT_BUDGET_PREPARE/
--    FEAT_EXPENSE_CONTROL/FEAT_RULE_ENGINE_BASE/FEAT_INVOICE_VERIFY）
--    + 组 B 9 项（FEAT_TIME_CLOCK/FEAT_CREDIT_SCORE/FEAT_DEPT_BUDGET/FEAT_ELASTIC_QUOTA/
--    FEAT_RULE_ENGINE_CUSTOM/FEAT_PROFIT_REPORT/FEAT_EXPORT_TEMPLATE_CUSTOM/FEAT_EVIDENCE_CHAIN/FEAT_LINK_RULE_CUSTOM）
--    + FEAT_WECOM（企微通道）

-- 5) 角色数分层：高级版 max_roles=6，platform_admin 可自建角色（含角色管理菜单 101）
-- SELECT id, company_name, edition, max_roles FROM sys_tenant WHERE UPPER(edition) = 'ADVANCED';

-- =====================================================================
-- 高级版能力边界（v1.12）
-- ---------------------------------------------------------------------
-- ● 承接下移的 11 类资金管控重能力（组 A 开关，接口层 @RequireFeature 拦截基础版）：
--   预算编制闭环 / 费用大类三层科目 / 财务科目导入 / 拆分初稿 / 报销大类硬拦截与三级预警 /
--   规则引擎（内置）/ 发票验真 / 烂账 / 项目关联规则 / 无发票后补 / 隐形成本 / 老板看板 / 凭证导出（7 套模板）
-- ● 高级版原生增量（组 B 开关）：部门级预算 + 弹性额度 / 打卡定位 / 信用体系 / 规则引擎租户级自定义 /
--   项目利润报表 / 自定义凭证模板 / 证据链分级 + 批量审核 / 关联规则自定义
-- ● 访问通道：企业微信自建应用 + OAuth 免登 + 通讯录同步（FEAT_WECOM）
-- ○ 不含（专业版）：多组织 / 多账套 / 人脸比对 / 二维导出 / 规则灰度+AI / 成员负载 / 开放平台 API
-- =====================================================================
