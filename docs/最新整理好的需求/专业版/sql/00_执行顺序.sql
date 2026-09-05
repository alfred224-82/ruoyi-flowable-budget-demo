-- =====================================================================
-- 专业版（PROFESSIONAL）SQL 执行顺序索引 · v1.12
-- 日期：2026-09-05
-- 依据：《多业态SaaS平台需求规格说明书》v1.0、《资金管控系统开发任务文档》v1.12
-- 口径：专业版 = 高级版全部能力（含 v1.12 下移的 11 类资金管控重能力）
--       + 专业版增量（多组织 / 多账套 / 人脸比对 / 二维导出 / 规则灰度+AI / 成员负载）
--       + 访问通道「企业微信 + 开放平台 API」。
-- 说明：
--   1. 本目录为「专业版自包含可独立部署」脚本集（累计制：含基础版 + 高级版全部脚本）；
--   2. 全部脚本幂等，可重复执行；严格按序号执行，不可颠倒；
--   3. 升级路径：高级版租户升级专业版时，阶段一~十二已执行，仅需补执行阶段十三，
--      并由管理员在租户管理执行「版本分配 → PROFESSIONAL」触发授权模板与功能开关刷新（数据零迁移）；
--   4. 版本编码归一化：ENTERPRISE / pro / professional 由 normalizeEdition() 统一为 PROFESSIONAL，
--      不写迁移脚本、不改存量（M4_01 仅做历史编码兼容标注）。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 阶段一~八：与基础版一致（数据库底座 / 平台底座 / 角色授权 / 定时任务·License /
--            审计字段 / 引导态门店 / 全量业态能力 / 计费合规）
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
-- [30] 高级版增量表 + 专业版增量表主体（fee_org/fee_org_budget_version/
--      fee_face_verify_record/fee_rule_version）+ 预算表双粒度 + 报销表证据链
source M1_03_create_tables_advanced_pro.sql;

-- ---------------------------------------------------------------------
-- 阶段十：预算域增强（v1.12 由基础版下移，FEAT_SUBJECT_3LAYER / FEAT_BUDGET_PREPARE）
-- ---------------------------------------------------------------------
source M5_02_create_tables_category.sql;
source M5_03_sys_menu_expense_category.sql;
source M5_04_boss_category_quota.sql;
source M5_05_budget_domain_enhance.sql;
source M5_06_tenant_category_and_system_level.sql;
source M5_07_tenant_category_menu.sql;
source M5_08_platform_admin_category_menu.sql;
source M5_09_platform_admin_category_fix.sql;
source M5_10_budget_dimension_upgrade.sql;
source M5_11_cleanup_historical_budget_records.sql;

-- ---------------------------------------------------------------------
-- 阶段十一：高级版增量能力（打卡/信用/部门预算/规则自定义/利润报表/自定义模板/证据链）
-- ---------------------------------------------------------------------
source M6_01_advanced_incremental.sql;
source M6_02_xxl_job_credit_score.sql;
-- ★承接 M5 下移能力的菜单迁移（T6.8②：下移菜单迁至 *_ADVANCED/*_PROFESSIONAL 模板）
source M6_03_downshift_menu_migration.sql;

-- ---------------------------------------------------------------------
-- 阶段十二：企业微信访问通道（高级版通道，access_channel=wecom，FEAT_WECOM）
-- ---------------------------------------------------------------------
source M13_01_access_channel.sql;

-- ---------------------------------------------------------------------
-- 阶段十三：专业版增量能力（T7.1~T7.8，本版本专属）
-- ---------------------------------------------------------------------
-- [45] 专业版增量脚本（T7.1~T7.6）：用户-组织关系表 fee_org_user（FEAT_MULTI_ORG 多组织）
--      + 组织预算版本快照（FEAT_ORG_SNAPSHOT）+ 人脸比对记录（FEAT_FACE_RECOGNITION）
--      + 规则版本灰度（FEAT_RULE_GRAYSCALE）+ 二维导出（FEAT_EXPORT_2D）+ 成员负载（FEAT_MEMBER_LOAD）
--      主体表已在 M1_03 建齐，本脚本补关系表与存量表字段扩展。
source M7_01_incremental_professional.sql;
-- [46] 多账套（T7.7，FEAT_MULTI_ACCOUNT_SET）：账套主表 fee_account_set + 单据归属账套 +
--      合并报表；menu_id 顺延占用 3325~3331；账套为组织（fee_org）的下级维度。
source M7_02_multi_account_set.sql;
-- [47] 开放平台 API（T7.8，FEAT_OPEN_API）：对外 RESTful + AppKey/AppSecret 签名鉴权（HMAC-SHA256）
--      + 日/月配额限流 + IP 白名单 + 全量调用审计 + Webhook 回调；menu_id 顺延占用 3340~3347；
--      凭据密文落库（应用层 AES 加密，同企微密钥策略 T10.1）。
source M7_03_open_api.sql;

-- =====================================================================
-- 验收检查（专业版）
-- =====================================================================
-- 1) 专业版增量表就绪
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('fee_org','fee_org_user','fee_org_budget_version',
--                      'fee_face_verify_record','fee_rule_version');  -- 预期：5

-- 2) 多账套 + 开放平台表就绪
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('fee_account_set','open_api_credential');  -- 预期：≥2

-- 3) ★专业版模板含全部下移菜单 + 专业版增量菜单（角色保全，预期 > 0）
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template
--  WHERE edition LIKE '%\_PROFESSIONAL'
--    AND menu_id IN (2001,2003,3208,3209,3215,3216,3204,3205,3310,3318,3325,3340)
--  GROUP BY edition;

-- 4) 专业版功能开关（GET /system/edition/features，租户 edition=PROFESSIONAL）
--    预期 = 高级版全部（组 A 11 + 组 B 9 + FEAT_WECOM）
--    + 组 D 专业版 7 项（FEAT_MULTI_ORG/FEAT_ORG_SNAPSHOT/FEAT_FACE_RECOGNITION/
--      FEAT_EXPORT_2D/FEAT_RULE_GRAYSCALE/FEAT_MEMBER_LOAD/FEAT_ISOLATED_SCHEMA 预留）
--    + 组 C 通道/多账套/开放 API（FEAT_MULTI_ACCOUNT_SET/FEAT_OPEN_API/FEAT_WECOM）

-- 5) 角色数分层：专业版 max_roles=NULL（不限角色），platform_admin 可自建任意角色
-- SELECT id, company_name, edition, max_roles FROM sys_tenant WHERE UPPER(edition) = 'PROFESSIONAL';

-- 6) 版本编码归一化验证：不存在残留 ENTERPRISE 生效编码（新代码统一走 normalizeEdition）
-- SELECT edition, COUNT(*) FROM sys_tenant GROUP BY edition;

-- =====================================================================
-- 专业版能力边界（v1.12，交付客户须知）
-- ---------------------------------------------------------------------
-- ● 含高级版全部能力（承接下移的 11 类资金管控重能力 + 高级版原生增量 + 企微通道）。
-- ● 专业版专属增量（组 D / 组 C 开关，接口层 @RequireFeature 拦截非专业版）：
--   - 多组织（FEAT_MULTI_ORG）：一个租户挂多个独立核算组织（fee_org），用户按组织授权，登录选主组织
--   - 组织预算版本快照（FEAT_ORG_SNAPSHOT）：按组织 + 版本冻结预算基线，支持跨版本对比
--   - 人脸比对（FEAT_FACE_RECOGNITION）：报销/审批关键环节人脸核身留痕（fee_face_verify_record）
--   - 二维导出（FEAT_EXPORT_2D）：行×列交叉透视导出（部门×科目、项目×月份等）
--   - 规则灰度 + AI（FEAT_RULE_GRAYSCALE）：规则版本化灰度发布（fee_rule_version）+ AI 辅助建议
--   - 成员负载（FEAT_MEMBER_LOAD）：审批人工作量统计与均衡分派
--   - 多账套（FEAT_MULTI_ACCOUNT_SET）：租户内多账套独立核算 + 单据归属账套 + 合并报表
--   - 开放平台 API（FEAT_OPEN_API）：对外 RESTful + 签名鉴权 + 配额限流 + Webhook + 全量审计
--   - 物理隔离预留（FEAT_ISOLATED_SCHEMA）：为大客户独立库/独立 Schema 预留（当前逻辑隔离，标记能力位）
-- ● 访问通道：企业微信自建应用 + 开放平台 API 双通道（access_channel=wecom/open_api）。
-- =====================================================================
