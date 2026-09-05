-- =====================================================================
-- 基础版（BASIC）SQL 执行顺序索引 · v1.12
-- 日期：2026-09-05
-- 依据：《多业态SaaS平台需求规格说明书》v1.0、《资金管控系统开发任务文档》v1.12
-- 口径：v1.12「折中下移」——基础版 = 轻量记账 + 项目资金流 + 全量业态能力；
--       资金管控 11 类重能力（预算编制/科目三层/报销管控/规则引擎/发票验真/
--       烂账/关联规则/隐形成本/老板看板/凭证导出）已下移至高级版，本目录不含其脚本。
-- 说明：
--   1. 本目录为「基础版自包含可独立部署」脚本集，严格按序号执行，不可颠倒；
--   2. 全部脚本幂等（先删后建 / 先删后插 / 条件 ALTER），可重复执行；
--   3. 平台采用「一套系统 + 建表一次建齐、版本靠菜单模板 + 功能开关区分」策略：
--      高级/专业版增量表由 M1_03 建立（不在本目录），共享部署时在初始化阶段一并建齐；
--      基础版仅关闭对应入口（@RequireFeature 拦截 + 授权模板回收），数据保留不删。
--   4. 执行方式：mysql -u root -p ry-flowable-plus 后逐个 source，或用客户端按序运行。
-- =====================================================================

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

-- ---------------------------------------------------------------------
-- 阶段六：共用小程序租户识别 + 门店公开展示（引导态白名单）
-- ---------------------------------------------------------------------
-- [23] sys_tenant 门店公开列（city/address/经纬度/logo/简介，MyBatis-Plus 全量 SELECT 硬前置）
source M8_01_sys_tenant_store_profile.sql;
-- [24] 门店搜索 + portal_access_log 引导态审计表（系统级，无 tenant_id）
source M8_01_portal_store_search_and_audit.sql;

-- ---------------------------------------------------------------------
-- 阶段七：全量业态能力（基础版标配，v1.12）
-- ---------------------------------------------------------------------
-- [25] 项目容器菜单（业态分组工作台）
source M11_01_project_container_menu.sql;
-- [26] 活动管理 + 4 套主题 Design Token + Logo/海报管线 + 直播公告状态机
source M12_01_activity_theme_live.sql;

-- ---------------------------------------------------------------------
-- 阶段八：商业化计费与合规（全版本，v1.12）
-- ---------------------------------------------------------------------
-- [27] 计费套餐/订单 + 支付分账虚拟账户 + 提现 + 对账（含菜单 6120~6159）
source M14_01_billing_settlement.sql;
-- [28] 合规：一次性订阅消息配额 + 协议版本留痕 + 数据导出注销（含菜单 6160~6179）
source M14_02_compliance.sql;
-- [29] 计费到期提醒任务（BillingRemindJob，到期前 7/3/1 天；严禁自动扣款）
source M14_03_xxl_job_billing_remind.sql;

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
--    AND menu_id IN (2001,2002,2003,3310,3316,3318,3322,3204,3205,3208,3209,3211,3212,3213,3214,3215,3216,
--                    2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2031,2032,2033,2034,2035,
--                    3262,3263,3264,3265,3273,3276,3277,3280,3282,3284,3288,3289);

-- 5) 角色数分层：基础版 max_roles=2（boss + employee）
-- SELECT id, company_name, edition, max_roles FROM sys_tenant WHERE UPPER(edition) = 'BASIC';

-- =====================================================================
-- 基础版能力边界（v1.12，交付客户须知）
-- ---------------------------------------------------------------------
-- ● 保留（不设开关，基础版标配）：
--   - 项目资金流：项目管理(3201)/收入确认三态(3202,3210)/合同变更(3203)/付款单(3206)/收款单(3207)
--   - 轻量记账：报销单仅录入金额/事由/附件/项目，极简单级老板审批，不走科目匹配/额度拦截/发票验真
--   - 项目级自定义科目：fee_project_subject（挂 3201 页内），不依赖系统级三层科目
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
