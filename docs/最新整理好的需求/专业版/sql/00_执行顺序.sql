-- =====================================================================
-- 专业版（PROFESSIONAL）SQL 执行顺序索引 · v1.19
-- 日期：2026-09-19
-- 依据：《多业态SaaS平台需求规格说明书》v1.0、《资金管控系统开发任务文档》v1.16
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
--   5. ★版本谱系（v1.16）：平台新增「小微版（MICRO）」独立产品线（面向个体门店/小微商户，
--      手机号即身份 + 老板/业务员/顾客三视图 + V1 零在线支付 + ★分档 micro_lite/micro_pro），
--      其脚本见《小微版/sql/00_执行顺序.sql》（M15 里程碑 T15.1~T15.32 + M16 推广分销 T16.1~T16.12，32 脚本）；
--      小微版菜单占用 7000~7499 独立区段 + 资金平台管理菜单 1300~1359 区段，
--      与本目录企业版菜单（2000~6179）互不冲突；
--      专业版为「企业版谱系」最高档，与小微版分属两条产品线，不互相部署。
--   7. ★M16 推广分销与激励（推广码/扫码归因/拉新奖励/首单识别，FEAT_PROMOTION_DISTRIBUTION）
--      为「小微版」专属能力，脚本 M16_01~M16_03 仅归入《小微版/sql/》；本专业版目录不部署、
--      不建表、不开放入口（feature_flag 该开关 basic/advanced/pro 三列均 = 0，接口层 @RequireFeature 拦截）。
--   6. ★硬规定（v1.27 新增，不可覆盖规则）：
--      (1) 本索引中所有 M 开头脚本均不可覆盖此前已部署版本的数据和表结构；
--      (2) 建表必须使用 CREATE TABLE IF NOT EXISTS 或幂等存储过程；
--      (3) 数据插入必须使用 INSERT IGNORE / INSERT ... ON DUPLICATE KEY UPDATE / 条件 INSERT；
--      (4) ALTER TABLE 必须使用 INFORMATION_SCHEMA 条件判断列/索引是否已存在；
--      (5) 禁止在任何脚本中出现无条件的 DROP TABLE / DROP COLUMN / TRUNCATE / DELETE 全表；
--      (6) 先删后建（DROP + CREATE）仅允许在初始化建表脚本中对自身创建的表使用，
--          不得删除其他脚本或版本建立的表或数据。
--   7. ★M16/M17 不部署声明：M16 推广分销与激励为小微版专属，M17 通用评价与点赞仅小微版付费版启用（FEAT_RATING 在专业版 disabled），本专业版目录不部署对应脚本、不开放入口。
--   8. ★v1.19 新增（M18 图片合规与全局敏感词，全版本通用·平台级合规底线）：阶段十六新增 M18_10/M18_11/M18_12 三脚本（sensitive_word/sensitive_word_hit_log/image_compliance_config 三表 + feature_flag 双开关 FEAT_IMAGE_COMPLIANCE/FEAT_SENSITIVE_WORD（pro_enabled=1）+ 约 200 条全局底库 + Admin 平台合规菜单 1370~1380）；M18 为平台级合规底线，不受版本谱系隔离，与 M16/M17 不同。

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
-- ★sys_tenant 补 tenant_type/version_type 列（T15.25/T15.26，从 M15_10 提取补列部分，
--   无 role_template 依赖，企业版部署路径独立可执行；与小微版 M15_10 幂等兼容）
source M15_15_sys_tenant_type_version_columns.sql;
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
-- ★基础版阶段五 [23] 累计同步：报销单表结构补齐（V5.4 多币种/补助/合规校验 + cost_method
--   + 申请人-审批人审计列，修复 Unknown column 'currency_code' in 'field list'，幂等条件 ALTER）
source M5_12_fee_expense_bill_sync_columns.sql;
-- 注：基础版阶段五 [24] M5_13「大类↔预算科目映射下放」为『基础版专属补齐』——专业版已由
--     阶段十 M5_02 + M5_06 提供 fee_expense_category / fee_tenant_category / 映射B，
--     故专业版阶段一~八不重复引入 M5_13（避免与 M5_02 重复建表）。
source M8_01_sys_tenant_store_profile.sql;
source M8_02_portal_store_search_and_audit.sql;
source M11_01_project_container_menu.sql;
source M12_01_activity_theme_live.sql;
source M12_02_biz_theme_preset.sql;
source M14_01_billing_settlement.sql;
source M14_02_compliance.sql;
source M14_03_xxl_job_billing_remind.sql;
-- ★基础版阶段九 [32] 累计同步：存量基础版租户克隆角色菜单对齐修复（v1.12 / 手册 v2.0 验收，
--   回收空壳「预算管理」目录 2000 等下移菜单 + 按 *_BASIC 模板补齐 boss/employee 授权；
--   前置：M2_02 v2.2 / M2_03 v1.2 / M2_10 v1.1 已在上方执行）
source M2_11_basic_boss_menu_realign.sql;

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
-- 企微通讯录同步菜单（menu_id 3490~3492，biz:wecom:list/sync/log，FEAT_WECOM 高级版+），
--      T13.2 补充。前置 M13_01（wecom_sync_log 表 + 通道与集成目录 3480）已在上一条执行；
--      授权 boss(9201) / manager(9203)。v1.19 收口：该脚本此前在包内但索引漏引，按索引部署不会执行。
source M13_02_wecom_menu.sql;

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

-- ---------------------------------------------------------------------
-- 阶段十四：存量租户主数据初始化补齐（v1.12，2026-09-06 增补，三版同步）
-- ---------------------------------------------------------------------
-- [48] 预算科目租户初始化：存量科目提升为 tenant_id=0 模板 + 三级克隆到每个无科目租户
--      + 无配置租户补齐 fee_tenant_category（修复新租户立项/编制科目联动「无数据」，幂等；
--      Java 侧 SysTenantServiceImpl#initTenantDefaultData 同步补克隆，覆盖未来新租户）
source M5_14_budget_subject_tenant_init.sql;

-- ---------------------------------------------------------------------
-- 阶段十五：平台底座管理菜单（T10.2 / T10.3 / T15.29，v1.19 补入自包含包）
-- ---------------------------------------------------------------------
-- ★顺序强约束（不可颠倒）：M15_12 以
--     DELETE FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359
--   开局，而 M10_01 的「业态包管理」(1320~1322) 与「功能开关管理」(1330~1332) 正落在该区段内。
--   必须【先 M15_12 建 1300 一级目录 + 1310~1314 版本档位管理，再 M10_01】，
--   颠倒则 M10_01 建出的菜单会被 M15_12 的 DELETE 清除，T10.2/T10.3 的 PC 入口消失。
-- [十五-1] 资金平台管理菜单收纳 + 版本档位管理（menu_id 1300 一级目录 / 1310~1314，
--       T15.29）。re-parent 的 UPDATE 全部按 perms/menu_name 匹配，目标不存在即 no-op，
--       本包无 M15_06（小微版菜单）与 M15_10（version_type 分档）亦安全。
source M15_12_fund_platform_menu_edition.sql;
-- [十五-2] 业态包管理（system:bizBundle:*）+ 功能开关管理（system:featureFlag:*），
--       menu_id 1320~1339，六层防串第①层「版本分配入口」的 PC 管理页（T10.2 / T10.3），
--       全部授予超管 role_id=1；前置依赖 M1_05（biz_bundle / feature_flag 建表）已在阶段一~八执行。
source M10_01_biz_bundle_feature_flag_menu.sql;

-- ---------------------------------------------------------------------
-- 阶段十六：M18 图片合规与全局敏感词（v1.27 新增，全版本通用·平台级合规底线）
-- ---------------------------------------------------------------------
-- [十六-1] T18.1/T18.8 敏感词/命中日志/图片合规配置三表 + feature_flag 双开关（FEAT_IMAGE_COMPLIANCE/FEAT_SENSITIVE_WORD，组 F，pro_enabled=1）
--      前置：阶段一~八 M1_05（feature_flag 表）；三表 tenant_id=0=全局底库，走条件查询而非 IGNORE_TENANT_TABLES 白名单
source M18_10_sensitive_word_image_compliance.sql;
-- [十六-2] T18.7 全局敏感词底库初始化（V3.8 附录 A 八大类约 200 条，INSERT IGNORE 幂等）+ 旧表 sensitive_word_library→sensitive_word 幂等迁移（专业版无旧表则 no-op）
--      前置：[十六-1]M18_10（sensitive_word 表）
source M18_11_sensitive_word_init_data.sql;
-- [十六-3] T18.4/T18.5 Admin 平台合规菜单 1370~1380（compliance:sensitiveWord/imageConfig/alert，超管 role_id=1）
--      ★小微版专属菜单 7680~7690 在企业版无对应角色，INSERT IGNORE no-op，安全
--      前置：阶段一~八 M1_04（sys_menu）+ [十六-1]M18_10（三表）
source M18_12_compliance_menu.sql;

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

-- 7) ★预算科目租户初始化就绪（M5_14：每个租户均有科目数据与大类启用配置）
-- SELECT st.id, (SELECT COUNT(*) FROM budget_subject b WHERE b.tenant_id = st.id) AS subj_cnt,
--        (SELECT COUNT(*) FROM fee_tenant_category f WHERE f.tenant_id = st.id) AS cat_cnt
--   FROM sys_tenant st;   -- 预期：subj_cnt > 0 且 cat_cnt = 6

-- 8) ★平台底座管理菜单就绪（M15_12 + M10_01，v1.19 补入自包含包）
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu
--  WHERE menu_id BETWEEN 1300 AND 1359 ORDER BY menu_id;
-- 预期 12 行：1300(资金平台管理菜单目录) + 1310~1314(版本档位管理 5 行)
--            + 1320~1322(业态包管理 3 行) + 1330~1332(功能开关管理 3 行)
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 1320 AND 1339;
-- 预期 6（六层防串第①层「版本分配入口」超管可达，T10.2 / T10.3 PC 管理页）

-- 9) ★M18 图片合规与全局敏感词（v1.19 新增，全版本通用·平台级合规底线）
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('sensitive_word','sensitive_word_hit_log','image_compliance_config');  -- 预期 3
-- SELECT code, pro_enabled, status FROM feature_flag WHERE code IN ('FEAT_IMAGE_COMPLIANCE','FEAT_SENSITIVE_WORD');  -- 预期 2 行，pro_enabled=1
-- SELECT COUNT(*) FROM sensitive_word WHERE tenant_id=0 AND source='global';  -- 预期 ≥200
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 1370 AND 1389;  -- 预期 11（Admin 平台合规菜单）

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
