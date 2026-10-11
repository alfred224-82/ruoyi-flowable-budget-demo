-- =====================================================================
-- 基础版（BASIC）SQL 执行顺序索引 · v1.20
-- 日期：2026-09-19
-- 依据：《资金管控系统开发任务文档》v1.24
-- 口径：基础版 = 轻量记账 + 项目资金流 + 全量业态能力（不含高级版增量 / 小微版专属）
-- 说明：
--   1. 本目录为「基础版自包含可独立部署」脚本集（累计制）；
--   2. 全部脚本幂等，可重复执行；严格按序号执行，不可颠倒；
--   6. ★硬规定（v1.27 新增，不可覆盖规则）：
--      (1) 本索引中所有 M 开头脚本均不可覆盖此前已部署版本的数据和表结构；
--      (2) 建表必须使用 CREATE TABLE IF NOT EXISTS 或幂等存储过程；
--      (3) 数据插入必须使用 INSERT IGNORE / INSERT ... ON DUPLICATE KEY UPDATE / 条件 INSERT；
--      (4) ALTER TABLE 必须使用 INFORMATION_SCHEMA 条件判断列/索引是否已存在；
--      (5) 禁止在任何脚本中出现无条件的 DROP TABLE / DROP COLUMN / TRUNCATE / DELETE 全表；
--      (6) 先删后建（DROP + CREATE）仅允许在初始化建表脚本中对自身创建的表使用，
--          不得删除其他脚本或版本建立的表或数据。
--   7. ★M16/M17 不部署声明：M16 推广分销与激励为小微版专属，M17 通用评价与点赞仅小微版付费版启用，本基础版目录不部署对应脚本、不开放入口。

-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 阶段一：数据库底座（多租户 + 基础业务表 + 全量菜单）
-- ---------------------------------------------------------------------
-- [01] 存量表补齐 tenant_id（幂等）
source M1_01_existing_tables_add_tenant_id.sql;
-- [02] 基础版业务表（项目资金流域：fee_project/fee_payment/fee_receipt/... 19 张 + 内置数据）
source M1_02_create_tables_basic.sql;
-- [03] 全量菜单一次建齐（三版本菜单 + 按钮 + 隐藏页；建菜单 ≠ 开放，开放由模板控制）
source M1_04_sys_menu.sql;

-- ---------------------------------------------------------------------
-- 阶段二：多业态平台底座（v1.12 新增，三维能力模型前提）
-- ---------------------------------------------------------------------
-- [04] 平台底座建表：biz_bundle 业态包 / feature_flag 33 项开关 / tenant_biz_config 单店覆盖 /
--      tenant_theme 主题 / user_tenant_bind openid 多租户归属 + 内置 3 业态包与 33 项开关
source M1_05_platform_foundation_tables.sql;
-- [05] sys_tenant 扩展：biz_code / access_channel / wecom_* / mp_app_id（存量回填 fund_control + wechat）
source M1_06_sys_tenant_biz_channel.sql;
-- ★sys_tenant 补 tenant_type/version_type 列（T15.25/T15.26，从 M15_10 提取补列部分，
--   无 role_template 依赖，企业版部署路径独立可执行；与小微版 M15_10 幂等兼容）
source M15_15_sys_tenant_type_version_columns.sql;
-- [06] 项目容器建表：project / project_member / project_task / project_signup（fee_project 补 project_id）
source M1_07_project_container_tables.sql;

-- ---------------------------------------------------------------------
-- 阶段三：角色与分版本授权（2 / 4-6 / 不限）
-- ---------------------------------------------------------------------
-- [07] 五角色初始化（boss/finance/manager/employee/platform_admin）
source M2_01_roles.sql;
-- [08] 角色×版本菜单授权模板（防串第④层，累计制）
source M2_02_role_menu_templates.sql;
-- [09] 基础版共享预览角色 sys_role_menu 授权
source M2_03_sys_role_menu_basic.sql;
-- [10] 租户隔离校正
source M2_04_tenant_isolation.sql;
-- ★sys_role 补 tenant_type_scope 列（micro/enterprise/both）+ 存量回填 + 索引
--   （T15.25 角色可见性硬隔离；从 M15_09 提取补列部分，无小微版前置依赖，
--    企业版部署路径独立可执行；与小微版 M15_09 幂等兼容，重复执行不报错；
--    修复 MyBatis-Plus 全量 SELECT sys_role 报 Unknown column 'tenant_type_scope'）
--   ★前置：[10]M2_04（sys_role 表已含 tenant_id，标准角色集 [07]M2_01 已建）
source M15_09_sys_role_tenant_type_scope.sql;
-- [11] platform_admin 升级 + 版本全量模板（BASIC/ADVANCED/PROFESSIONAL）
source M2_05_platform_admin_upgrade.sql;
-- [12] 用户-租户绑定修复
source M2_06_user_tenant_fix.sql;
-- [13] 租户根部门回填
source M2_07_tenant_root_dept_backfill.sql;
-- [14] 租户级克隆角色迁移（版本变更仅影响本租户）
source M2_08_tenant_role_clone_migration.sql;
-- [15] sys_tenant.max_roles 角色数分层（BASIC=2 / ADVANCED=6 / PROFESSIONAL=NULL）
source M2_09_tenant_max_roles.sql;
-- [16] ★授权模板下移重排：将 11 类下移能力菜单从 *_BASIC 模板迁出（基础版收敛为轻量入口）
source M2_10_downshift_menu_templates.sql;

-- ---------------------------------------------------------------------
-- 阶段四：定时任务 / License / 版本编码
-- ---------------------------------------------------------------------
-- [17] 租户到期检测任务注册
source M3_01_xxl_job_tenant_expire_check.sql;
-- [18] 全量定时任务注册（幂等）
source M3_02_xxl_job_all_tasks.sql;
-- [19] License-租户关联（sys_license 加 tenant_id）
source M3_05_license_tenant_association.sql;
-- [20] License 管理菜单增强
source M3_06_license_management_menu.sql;
-- [21] 版本编码迁移（ENTERPRISE→PROFESSIONAL 存量兼容；新代码走 normalizeEdition 归一化）
source M4_01_edition_enterprise_to_professional.sql;

-- ---------------------------------------------------------------------
-- 阶段五：基础版业务表审计字段修正（M1_02 增量）
-- ---------------------------------------------------------------------
-- [22] 基础版业务表补审计字段 + fee_expense_bill 补 project_id/invoice_defer（幂等）
source M5_01_alter_tables_audit_columns.sql;
-- [23] 报销单表结构补齐：V5.4 多币种/补助/合规校验 + cost_method + 申请人-审批人审计列
--      （修复 Unknown column 'currency_code' in 'field list'，幂等条件 ALTER）
source M5_12_fee_expense_bill_sync_columns.sql;
-- [24] ★基础版补齐「费用大类↔预算科目对应关系」(映射B 下放)：fee_expense_category(6大类,系统级共享)
--      + fee_tenant_category(租户启用) + budget_subject 补 category_code 列并按大类关键字回填；
--      项目立项(3201)按大类分配预算依赖，前后端零改动，与高级版 M5_02/M5_06 结构幂等一致
source M5_13_basic_category_subject_mapping.sql;

-- ---------------------------------------------------------------------
-- 阶段六：共用小程序租户识别 + 门店公开展示（引导态白名单）
-- ---------------------------------------------------------------------
-- [25] sys_tenant 门店公开列（city/address/经纬度/logo/简介，MyBatis-Plus 全量 SELECT 硬前置）
source M8_01_sys_tenant_store_profile.sql;
-- [26] 门店搜索 + portal_access_log 引导态审计表（系统级，无 tenant_id）
source M8_02_portal_store_search_and_audit.sql;

-- ---------------------------------------------------------------------
-- 阶段七：全量业态能力（基础版标配，v1.12）
-- ---------------------------------------------------------------------
-- [27] 项目容器菜单（业态分组工作台）
source M11_01_project_container_menu.sql;
-- [28] 活动管理 + 4 套主题 Design Token + Logo/海报管线 + 直播公告状态机
source M12_01_activity_theme_live.sql;
-- [28-2] 主题预设表 biz_theme_preset（系统级共享 Design Token，T12.2 补建）
source M12_02_biz_theme_preset.sql;

-- ---------------------------------------------------------------------
-- 阶段八：商业化计费与合规（全版本，v1.12）
-- ---------------------------------------------------------------------
-- [29] 计费套餐/订单 + 支付分账虚拟账户 + 提现 + 对账（含菜单 6120~6159）
source M14_01_billing_settlement.sql;
-- [30] 合规：一次性订阅消息配额 + 协议版本留痕 + 数据导出注销（含菜单 6160~6179）
source M14_02_compliance.sql;
-- [31] 计费到期提醒任务（BillingRemindJob，到期前 7/3/1 天；严禁自动扣款）
source M14_03_xxl_job_billing_remind.sql;

-- ---------------------------------------------------------------------
-- 阶段九：存量租户授权修复（v1.12 基础版 boss 菜单对齐，2026-09-06 增补）
-- ---------------------------------------------------------------------
-- [32] 克隆角色菜单对齐：回收下移残留（含 2000 空壳目录）+ 按最新 *_BASIC 模板补齐
--      （存量环境须先重放 [08]M2_02 → [09]M2_03 → [16]M2_10 再执行本脚本）
source M2_11_basic_boss_menu_realign.sql;

-- ---------------------------------------------------------------------
-- 阶段十：存量租户主数据初始化补齐（v1.12，2026-09-06 增补，三版同步）
-- ---------------------------------------------------------------------
-- [33] 预算科目租户初始化：存量科目提升为 tenant_id=0 模板 + 三级克隆到每个无科目租户
--      + 无配置租户补齐 fee_tenant_category（修复新租户立项/编制科目联动「无数据」，幂等；
--      Java 侧 SysTenantServiceImpl#initTenantDefaultData 同步补克隆，覆盖未来新租户）
source M5_14_budget_subject_tenant_init.sql;

-- ---------------------------------------------------------------------
-- 阶段十一：平台底座管理菜单（T10.2 / T10.3 / T15.29，v1.19 补入自包含包）
-- ---------------------------------------------------------------------
-- ★顺序强约束（不可颠倒）：M15_12 以
--     DELETE FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359
--   开局，而 M10_01 的「业态包管理」(1320~1322) 与「功能开关管理」(1330~1332) 正落在该区段内。
--   必须【先 M15_12 建 1300 一级目录 + 1310~1314 版本档位管理，再 M10_01】，
--   颠倒则 M10_01 建出的菜单会被 M15_12 的 DELETE 清除，T10.2/T10.3 的 PC 入口消失。
-- [十一-1] 资金平台管理菜单收纳 + 版本档位管理（menu_id 1300 一级目录 / 1310~1314，
--       T15.29）。re-parent 的 UPDATE 全部按 perms/menu_name 匹配，目标不存在即 no-op，
--       本包无 M15_06（小微版菜单）与 M15_10（version_type 分档）亦安全。
source M15_12_fund_platform_menu_edition.sql;
-- [十一-2] 业态包管理（system:bizBundle:*）+ 功能开关管理（system:featureFlag:*），
--       menu_id 1320~1339，六层防串第①层「版本分配入口」的 PC 管理页（T10.2 / T10.3），
--       全部授予超管 role_id=1；前置依赖 M1_05（biz_bundle / feature_flag 建表）已在阶段二执行。
source M10_01_biz_bundle_feature_flag_menu.sql;

-- ---------------------------------------------------------------------
-- 阶段十二：M18 图片合规与全局敏感词（v1.27 新增，全版本通用·平台级合规底线）
-- ---------------------------------------------------------------------
-- [十二-1] T18.1/T18.8 敏感词/命中日志/图片合规配置三表 + feature_flag 双开关（FEAT_IMAGE_COMPLIANCE/FEAT_SENSITIVE_WORD，组 F，basic_enabled=1）
--      前置：阶段二 M1_05（feature_flag 表）；三表 tenant_id=0=全局底库，走条件查询而非 IGNORE_TENANT_TABLES 白名单
source M18_10_sensitive_word_image_compliance.sql;
-- [十二-2] T18.7 全局敏感词底库初始化（V3.8 附录 A 八大类约 200 条，INSERT IGNORE 幂等）+ 旧表 sensitive_word_library→sensitive_word 幂等迁移（旧表不存在则 no-op）
--      前置：[十二-1]M18_10（sensitive_word 表）
source M18_11_sensitive_word_init_data.sql;
-- [十二-3] T18.4/T18.5 Admin 平台合规菜单 1370~1380（compliance:sensitiveWord/imageConfig/alert，超管 role_id=1）
--      ★本包为小微版专属菜单 7680~7690 无对应角色，INSERT IGNORE no-op，安全
--      前置：阶段一 M1_04（sys_menu）+ [十二-1]M18_10（三表）
source M18_12_compliance_menu.sql;

-- =====================================================================
-- 验收检查（基础版）
-- =====================================================================
-- 1) 全部 fee_* 业务表含 tenant_id
-- SELECT TABLE_NAME FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE 'fee_%' AND COLUMN_NAME = 'tenant_id';

-- 2) 平台底座表就绪（业态包 / 开关 / 项目容器）
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('biz_bundle','feature_flag','tenant_biz_config','tenant_theme',
--                      'user_tenant_bind','project','project_member','project_task','project_signup');
-- 预期：9

-- 3) 33 项功能开关落表（组 A 11 + 组 B 9 + 组 C 6 + 组 D 7）
-- SELECT COUNT(*) FROM feature_flag;  -- 预期：33

-- 4) ★防串第④层：基础版模板已回收全部下移菜单（预期 0）
-- SELECT COUNT(*) FROM sys_edition_menu_template
--  WHERE (edition = 'BASIC' OR edition LIKE '%\_BASIC')
--   AND menu_id IN (2000,2001,2002,2003,3310,3316,3318,3322,3204,3205,3208,3209,3211,3212,3213,3214,3215,3216,
--                    2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2031,2032,2033,2034,2035,
--                    3262,3263,3264,3265,3273,3276,3277,3280,3282,3284,3288,3289);

-- 5) 角色数分层：基础版 max_roles=2（boss + employee）
-- SELECT id, company_name, edition, max_roles FROM sys_tenant WHERE UPPER(edition) = 'BASIC';

-- 6) 报销单表结构与实体对齐（13 列，预期 13）
-- SELECT COUNT(*) FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_expense_bill'
--    AND COLUMN_NAME IN ('currency_code','original_amount','exchange_rate','subsidy_amount','travel_days',
--                        'cost_method','compliance_status','applicant_id','applicant_name','approver_id',
--                        'approve_time','reject_reason','reject_time');

-- 7) ★大类↔预算科目对应关系就绪（M5_13，支撑项目立项 3201 按大类分配预算）
-- SELECT COUNT(*) FROM fee_expense_category WHERE is_builtin = 1;                               -- 预期 6（tenant_id 均为 0）
-- SELECT COUNT(*) FROM fee_tenant_category WHERE tenant_id = 1;                                 -- 预期 6
-- SELECT COUNT(*) FROM budget_subject WHERE category_code IS NULL AND subject_type = 'EXPENSE'; -- 预期 0（费用类全部归类）

-- 8) ★预算科目租户初始化就绪（M5_14：每个租户均有科目数据与大类启用配置）
-- SELECT st.id, (SELECT COUNT(*) FROM budget_subject b WHERE b.tenant_id = st.id) AS subj_cnt,
--        (SELECT COUNT(*) FROM fee_tenant_category f WHERE f.tenant_id = st.id) AS cat_cnt
--   FROM sys_tenant st;   -- 预期：subj_cnt > 0 且 cat_cnt = 6
-- SELECT COUNT(*) FROM budget_subject b WHERE b.level >= 2 AND NOT EXISTS
--   (SELECT 1 FROM budget_subject p WHERE p.tenant_id = b.tenant_id AND p.id = b.parent_id);  -- 预期 0（树完整）

-- 9) ★平台底座管理菜单就绪（M15_12 + M10_01，v1.19 补入自包含包）
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu
--  WHERE menu_id BETWEEN 1300 AND 1359 ORDER BY menu_id;
-- 预期 12 行：1300(资金平台管理菜单目录) + 1310~1314(版本档位管理 5 行)
--            + 1320~1322(业态包管理 3 行) + 1330~1332(功能开关管理 3 行)
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 1320 AND 1339;
-- 预期 6（六层防串第①层「版本分配入口」超管可达，T10.2 / T10.3 PC 管理页）

-- 10) ★M18 图片合规与全局敏感词（v1.20 新增，全版本通用·平台级合规底线）
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('sensitive_word','sensitive_word_hit_log','image_compliance_config');  -- 预期 3
-- SELECT code, basic_enabled, status FROM feature_flag WHERE code IN ('FEAT_IMAGE_COMPLIANCE','FEAT_SENSITIVE_WORD');  -- 预期 2 行，basic_enabled=1
-- SELECT COUNT(*) FROM feature_flag;  -- 预期 35（基础版 33 + M18 双开关 2）
-- SELECT COUNT(*) FROM sensitive_word WHERE tenant_id=0 AND source='global';  -- 预期 ≥200
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 1370 AND 1389;  -- 预期 11（Admin 平台合规菜单）

-- =====================================================================
-- 基础版能力边界（v1.12，交付客户须知）
-- ---------------------------------------------------------------------
-- ● 保留（不设开关，基础版标配）：
--   - 项目资金流：项目管理(3201)/收入确认三态(3202,3210)/合同变更(3203)/付款单(3206)/收款单(3207)
--   - 轻量记账：报销单仅录入金额/事由/附件/项目，极简单级老板审批，不走科目匹配/额度拦截/发票验真
--   - 项目级自定义科目：fee_project_subject（挂 3201 页内），不依赖系统级三层科目
--   - 项目立项按大类分配预算：大类主数据 fee_expense_category + 映射B(budget_subject.category_code)
--     由 M5_13 下放补齐（仅「大类 + 扁平科目」联动，不含三层科目预算编制闭环）
--   - 轻量收支概览：本月收入/支出/结余三卡片（不含大类下钻看板）
--   - 全量业态能力：活动管理/组队引擎/4 套主题/Logo·海报管线/直播公告（M11/M12）
--   - 共用小程序引导态：门店选择/搜索、?tenant= 与 AppID 反查（M8，仅公开字段）
--   - 计费与合规：四档套餐/试用到期停止/订阅消息配额（M14）
-- ○ 不含（已下移高级版，入口关闭、接口 @RequireFeature 拦截）：
--   - 预算编制闭环 / 费用大类三层科目 / 财务科目导入 / 拆分初稿（FEAT_BUDGET_PREPARE / FEAT_SUBJECT_3LAYER）
--   - 报销大类硬拦截与三级预警（FEAT_EXPENSE_CONTROL）
--   - 规则引擎 / 发票验真（FEAT_RULE_ENGINE_BASE / FEAT_INVOICE_VERIFY）
--   - 烂账(3208) / 项目关联规则(3216) / 无发票后补 / 隐形成本(3204/3205/3211~3214)
--   - 老板看板(3215) / 凭证导出(3209)
-- =====================================================================
