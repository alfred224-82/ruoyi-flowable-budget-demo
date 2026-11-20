-- =====================================================================
-- 全量 SQL 执行顺序索引（系统管理 + 小微版 合并） · v1.0
-- 日期：2026-10-12
-- 说明：
--   1. 本目录合并「系统管理」与「小微版」两个分册的全部 SQL 脚本，
--      提供统一的 00_执行顺序.sql，一次执行完成全部数据库变更。
--   2. 全部脚本幂等，可重复执行；严格按序号执行，不可颠倒。
--   3. 阶段一~五 = 系统管理底座（M1~M4/M10/M14/M15 共用部分）
--      阶段六~三十二 = 小微版专属（M8/M15~M25 + M18 合规 + M19~M20 修复）
--   4. ★硬规定（v1.27）：建表用 IF NOT EXISTS / 幂等存储过程；
--      ALTER 用 INFORMATION_SCHEMA 条件判断；禁止无条件 DROP。
--   5. ★执行后请退出重新登录，刷新 Sa-Token 权限快照。
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 阶段一：数据库底座与平台底座建表（M1，T1.1~T1.9）
-- =====================================================================
-- [01] 存量表补 tenant_id + 索引（幂等存储过程）
source M1_01_existing_tables_add_tenant_id.sql;
-- [02] 基础版业务表（fee_project / fee_payment / fee_receipt 等）
source M1_02_create_tables_basic.sql;
-- [03] 高级/专业版增量表主体（fee_employee_credit / fee_credit_rule 等）
source M1_03_create_tables_advanced_pro.sql;
-- [04] sys_menu 全量菜单（M1_04 v1.1 幂等补 client_type 列）
source M1_04_sys_menu.sql;
-- [05] ★平台底座三表：biz_bundle / feature_flag / sys_edition_menu_template
source M1_05_platform_foundation_tables.sql;
-- [06] sys_tenant 扩展三字段（biz_code / access_channel / wecom_*）
source M1_06_sys_tenant_biz_channel.sql;
-- [07] sys_tenant 补 tenant_type / version_type 列 + 存量回填（企业版提取，无 role_template 依赖）
source M15_15_sys_tenant_type_version_columns.sql;
-- [08] project 统一容器四表（project / project_member / project_task / project_signup）
source M1_07_project_container_tables.sql;
-- [09] 主题预设表 biz_theme_preset（系统级共享 Design Token）
source M12_02_biz_theme_preset.sql;
-- [10] ★角色模板表 role_template（系统级共享，无 tenant_id；CREATE TABLE IF NOT EXISTS 幂等）
source M15_01_role_template.sql;

-- =====================================================================
-- 阶段二：角色与授权模板（M2，T2.1~T2.5）
-- =====================================================================
-- [11] 标准角色集（boss / finance / manager / employee / salesman / customer）
source M2_01_roles.sql;
-- [12] ★角色×版本授权模板 sys_edition_menu_template
source M2_02_role_menu_templates.sql;
-- [13] 基础版 sys_role_menu 授权
source M2_03_sys_role_menu_basic.sql;
-- [14] sys_dept / sys_role 补 tenant_id + 索引（幂等存储过程）
source M2_04_tenant_isolation.sql;
-- [15] ★sys_role 补 tenant_type_scope 列（micro/enterprise/both）+ 存量回填 + 索引
source M15_09_sys_role_tenant_type_scope.sql;
-- [16] platform_admin 平台管理员升级（★必须在 M2_02 之后）
source M2_05_platform_admin_upgrade.sql;
-- [17] 存量用户-租户归属修复
source M2_06_user_tenant_fix.sql;
-- [18] 租户根部门回填
source M2_07_tenant_root_dept_backfill.sql;
-- [19] 租户角色克隆迁移（共享预设角色 -> 租户私有角色）
source M2_08_tenant_role_clone_migration.sql;
-- [20] sys_tenant.max_roles 角色数上限
source M2_09_tenant_max_roles.sql;
-- [21] 下移菜单模板迁移（*_BASIC -> *_ADVANCED / *_PROFESSIONAL）
source M2_10_downshift_menu_templates.sql;

-- =====================================================================
-- 阶段三：定时任务 / License / 计费 / 行业模板 / 版本编码
-- =====================================================================
-- [22] 租户到期检测任务注册
source M3_01_xxl_job_tenant_expire_check.sql;
-- [23] 全量定时任务注册
source M3_02_xxl_job_all_tasks.sql;
-- [24] License-租户关联（sys_license 加 tenant_id）
source M3_05_license_tenant_association.sql;
-- [25] License 管理菜单增强
source M3_06_license_management_menu.sql;
-- [26] ★商业化计费与合规表（11 张表 + 四档套餐 + 菜单 6120~6159，小微版完整路径）
source M14_01_billing_settlement.sql;
-- [27] 合规：订阅消息配额 + 协议版本留痕 + 菜单 6160~6179
source M14_02_compliance.sql;
-- [28] 计费到期提醒任务
source M14_03_xxl_job_billing_remind.sql;
-- [29] 行业模板表（industry_template + industry_role_map）
source M15_02_industry_template.sql;
-- [30] 行业模板种子数据填充（23 行业 + 69 role_map）
source M15_22_industry_template_seed_v35.sql;
-- [31] sys_tenant 移除 scale 列（幂等）
source M3_07_sys_tenant_drop_scale.sql;
-- [32] 版本编码迁移（ENTERPRISE→PROFESSIONAL）
source M4_01_edition_enterprise_to_professional.sql;

-- =====================================================================
-- 阶段四：资金平台管理菜单（M15_12 + M10 平台底座菜单）
-- =====================================================================
-- ★顺序强约束：M15_12 DELETE 1300~1359 开局，M10_01/M10_02 落在该区段内，
--   必须先 M15_12 建 1300 目录，再 M10_01/M10_02。
-- [33] 「资金平台管理菜单」一级目录(1300) + 版本档位管理(1310~1314) + re-parent
source M15_12_fund_platform_menu_edition.sql;
-- [34] ★角色模板管理菜单（7005~7007，挂 1300 下；★必须在 M15_12 之后）
source M15_03b_role_template_menu.sql;
-- [35] 业态包管理(1320~1322) + 功能开关管理(1330~1332)
source M10_01_biz_bundle_feature_flag_menu.sql;
-- [36] 平台运营菜单补建：单店覆盖(1340~1342) + 访问通道(1343~1345) + re-parent 6120/6160
source M10_02_platform_operation_menu.sql;
-- [37] 业态文案映射表 biz_copy + 菜单 1350~1352
source M10_03_biz_copy_i18n.sql;
-- [37.5] Schema 页面引擎区块定义
source M10_04_schema_engine.sql;
-- [38] feature_flag 补 micro_enabled 列
source M10_05_feature_flag_micro_column.sql;

-- =====================================================================
-- 阶段五：共用小程序租户识别 + 门店公开展示
-- =====================================================================
-- [39] sys_tenant 门店公开列（city/address/logo/简介）★小微版门店主页强依赖
source M8_01_sys_tenant_store_profile.sql;
-- [40] 门店搜索 + portal_access_log 引导态审计表
source M8_02_portal_store_search_and_audit.sql;

-- =====================================================================
-- 阶段六：小微版核心（M15 里程碑）
-- =====================================================================
-- [41] T15.1 身份与角色映射核心表：user_tenant_role / tenant_role_mapping
source M15_01_micro_identity_role_tables.sql;
-- [42] T15.3 模块化页面 + 分享场景：page_block_config / share_scene
source M15_03_page_block_share_scene.sql;
-- [43] T15.4 服务定价：service_category / service_item / price_change_log
source M15_04_service_pricing.sql;
-- [44] T15.5 预约结算与现金流：booking_order / staff_settlement / cash_flow / micro_daily_revenue
source M15_05_booking_settlement_cashflow.sql;
-- [45] T15.6 sys_menu.client_type 端过滤列 + 小微版全量菜单 7000~7499
source M15_06_sys_menu_client_type_micro.sql;
-- [46] T15.7 标准角色集补充 + 小微版角色模板三配方（micro）
source M15_07_micro_role_template_init.sql;
-- [47] T15.22 小微版 XXL-JOB 三任务注册
source M15_08_xxl_job_micro_tasks.sql;
-- [48] T15.26 角色可见性硬隔离（sys_role 补 tenant_type_scope）
source M15_09_micro_role_scope_hard_isolation.sql;
-- [49] T15.26+T15.27 小微版分档机制 + role_template micro_lite/micro_pro 配方
source M15_10_micro_edition_tier_role_template.sql;
-- [50] T15.28 lite 档活动浏览页：promotion_activity 表 + 菜单 7330/7430
source M15_11_promotion_activity_browse.sql;
-- [51] T15.31+T15.32 防爽约/黑名单：customer_credit / no_show_record + 7170~7172
source M15_13_customer_credit_no_show.sql;
-- [52] T15.6+T15.16/17 前端菜单缺口幂等修补
source M15_14_frontend_menu_gap_patch.sql;
-- [53] T15.29 补漏：存量小微版租户克隆角色菜单授权修复
source M15_15_fix_existing_micro_tenant_menu.sql;
-- [54] platform_admin 角色模板补全 + License 菜单修复
source M15_16_platform_admin_role_template_license.sql;

-- =====================================================================
-- 阶段七：租户管理员菜单 + 推广分销拆分 + 主题/图片 + 盈亏指标
-- =====================================================================
-- [55] T15.33 platform_admin 租户管理员菜单三件套(7500~7509)
source M15_17_platform_admin_menu.sql;
-- [56] T15.34 boss 推广分销管理菜单拆分(7189/7190)
source M15_18_boss_distribution_menu_split.sql;
-- [57] T15.35 活动图片上传+背景主题：promotion_activity 补 logo_url/theme_key
source M15_19_activity_image_theme.sql;
-- [58] T15.35§六 图片校验与渲染管线
source M15_19b_activity_image_pipeline.sql;
-- [59] T15.36 小程序全局主题+首页欢迎语：sys_tenant 补 app_theme_key
source M15_20_platform_admin_theme_welcome.sql;
-- [60] T15.36§五 主题显示名映射 + 默认 Logo
source M15_20b_tenant_theme_display_map.sql;
-- [61] T15.37 每月固定支出表+管理菜单(7195~7199)
source M15_21_monthly_fixed_cost.sql;
-- [62] T15.38 原子资源管理：项目管理+房间/空间管理(7510~7516)
source M15_23_atomic_resource_project_room.sql;
-- [63] service_item 补 capacity 列 + 存量回填
source M15_24_service_item_capacity.sql;
-- [64] T1.7/T18.1 行业模板补 voice_enabled 列
source M15_25_industry_template_voice_enabled.sql;

-- =====================================================================
-- 阶段八：推广分销与激励（M16 里程碑，★仅小微版启用）
-- =====================================================================
-- [65] T16.1/T16.5 推广码与归因：promotion_code + promotion_attribution(7180~7188)
source M16_01_promotion_code_attribution.sql;
-- [66] T16.2/T16.3 拉新奖励与优惠券：promotion_reward_config / coupon
source M16_02_promotion_reward_coupon.sql;
-- [67] T16.4/T16.8 首单识别 + FEAT_PROMOTION_DISTRIBUTION
source M16_03_first_order_feature_flag.sql;
-- [68] T17.1~T17.5 通用评价与点赞：user_rating / user_like(7160~7164)
source M17_01_user_rating_like.sql;

-- =====================================================================
-- 阶段八-b：M18 合规建表系列（语音/标签/预约/奖励/排名/License/审计/汇总）
-- =====================================================================
-- [68.1] 语音合规规则与记录表
source M18_01_voice_compliance_tables.sql;
-- [68.2] 固定支出标签+周期规则表
source M18_02_tag_period_rule_tables.sql;
-- [68.3] 预约策略配置表
source M18_03_booking_policy_tables.sql;
-- [68.4] 拉新奖励配置扩展
source M18_04_reward_config_extend.sql;
-- [68.5] 老板员工业绩榜菜单
source M18_05_staff_ranking_menu.sql;
-- [68.6] License 模板管理表
source M18_06_license_template_tables.sql;
-- [68.7] Admin 运维审计表
source M18_07_admin_audit_tables.sql;
-- [68.8] M18 汇总补丁（role_template + menu 整合）
source M18_08_menu_role_template_consolidate.sql;

-- =====================================================================
-- 阶段九：平台底座管理菜单补齐（T10.2/T10.3）
-- =====================================================================
-- ★强约束：M15_12 先建 1300 → 再 M10_01 → 再 M10_02（已在阶段四完成）

-- =====================================================================
-- 阶段十~十一：紧急修复补丁 + M18 图片合规与全局敏感词
-- =====================================================================
-- [69] ★紧急修复：platform_admin 登录无菜单 + 权限隔离修正(v1.36)
source _fix_platform_admin_menu_v136.sql;
-- [70] ★platform_admin 克隆角色 + 用户角色分配 + 菜单授权修复(v1.37)
source M18_09_platform_admin_role_menu_fix.sql;
-- [70.5] ★修复 M18_09 Step 2 缺陷：platform_admin 误挂非 admin 用户（如老板李杰）
--        根因：M18_09 Step 2 给所有无角色用户分配 platform_admin，导致 boss 账号泄漏「租户管理菜单」
--        修复：① 清理非 admin_user_id 用户的 platform_admin（sys_user_role + user_tenant_role）
--              ② admin_user_id 补建 platform_admin 关联 ③ 同步两表一致性
--        ★幂等：DELETE WHERE + INSERT IGNORE + NOT EXISTS，可重复执行
--        ★前置：[70]M18_09
source M32_01_fix_m18_09_platform_admin_misassignment.sql;
-- [71] T18.1/T18.8 敏感词/命中日志/图片合规配置三表 + FEAT 36/37
source M18_10_sensitive_word_image_compliance.sql;
-- [72] T18.7 全局敏感词底库初始化（~200 条）+ 旧表迁移
source M18_11_sensitive_word_init_data.sql;
-- [73] T18.4/T18.5 Admin 平台合规菜单 1370~1380
source M18_12_compliance_menu.sql;
-- [74] T18.1~T18.4 语音合规全链路菜单注册(7600~7619)
source M18_13_voice_compliance_menu.sql;
-- [75] T18.11 租户管理员系统设置菜单(7695~7696)
source M18_14_system_setting_menu.sql;
-- [76] ★T18.20 M18 最终汇总补丁（role_template + sys_role_menu 全覆盖）
source M18_15_m18_final_role_template_patch.sql;

-- =====================================================================
-- 阶段十二~十三：micro_lite 老板首页修复 + boss 克隆角色菜单清理
-- =====================================================================
-- [77] ★micro_lite boss 经营驾驶舱首页修复
source M15_25_boss_lite_dashboard_fix.sql;
-- [78] ★boss 克隆角色菜单清理：移除残留的 platform_admin 菜单
source M15_26_boss_role_menu_cleanup.sql;
-- [79] ★紧急修复：老板/租户管理员角色错配（v2.0：非 admin 用户改挂 boss + admin 移除 boss 误挂）
source _fix_boss_role_assignment.sql;
-- [80] ★根因修复：存量小微版租户缺 boss 克隆角色 + 补建
source M15_27_micro_tenant_boss_role_rebuild.sql;

-- =====================================================================
-- 阶段十四~二十：boss 权限修复系列（M19/M20）
-- =====================================================================
-- [81] micro_lite boss 推广管理菜单授权
source M19_01_micro_lite_boss_promotion_menu.sql;
-- [82] boss 补录 system:oss:upload 权限
source M19_02_boss_oss_upload_permission.sql;
-- [83] micro_lite boss 菜单精简
source M19_03_micro_lite_boss_menu_simplify.sql;
-- [84] micro_lite boss 活动增删改查权限补全
source M19_04_micro_lite_boss_promo_crud_permission.sql;
-- [85] boss OSS 上传 role_template 修复
source M19_05_boss_oss_upload_role_template_fix.sql;
-- [86] boss OSS 查询移除权限
source M19_06_boss_oss_query_remove_permission.sql;
-- [87] 活动设置增强
source M19_07_activity_setting_enhance.sql;
-- [88] boss 我的推广二维码菜单
source M19_08_boss_my_promo_qrcode_menu.sql;
-- [89] 微信二维码重命名
source M19_09_wechat_qrcode_rename.sql;
-- [90] boss 专业版菜单修复
source M20_01_boss_pro_menu_fix.sql;
-- [91] 预约页面权限+菜单修复
source M20_02_booking_page_permission_and_menu_fix.sql;
-- [92] 移除我的推广菜单+共享权限
source M20_03_remove_my_promo_menu_and_shared_permissions.sql;
-- [93] micro_pro boss 权限+菜单修复
source M20_04_micro_pro_boss_permission_and_menu_fix.sql;
-- [94] micro_pro 功能包+菜单修复
source M20_05_micro_pro_feature_bundle_and_menu_fix.sql;
-- [95] 每月固定支出修复+增强
source M20_06_monthly_fixed_cost_fix_and_enhance.sql;
-- [96] 语音合规最大时长
source M20_07_voice_compliance_max_duration.sql;
-- [97] 语音合规结果表
source M20_08_voice_compliance_result.sql;
-- [98] PC 菜单扁平化（单子节点提升）
source M20_09_pc_menu_flatten_single_child.sql;
-- [99] 异常处理权限 + 异常确认表(nested source M20_11_exception_ack_table)
source M20_10_exception_resolve_permission.sql;
-- [100] tenant version_type NULL 修复
source M20_11_tenant_version_type_null_fix.sql;
-- [101] biz_bundle + 资金管控菜单修复
source M20_12_biz_bundle_fund_control_menus_fix.sql;
-- [102] 活动结束任务
source M20_13_activity_end_job.sql;
-- [103] 顾客角色权限修复
source M20_14_customer_role_permission_fix.sql;

-- =====================================================================
-- 阶段二十四：自助预约与资源管理（M21）
-- =====================================================================
-- [104] 自助预约
source M21_01_self_booking.sql;
-- [105] 资源状态
source M21_02_resource_status.sql;
-- [106] 员工定价
source M21_03_staff_pricing.sql;
-- [107] 审计列修复
source M21_04_fix_audit_columns.sql;
-- [108] boss OSS 权限修复
source M21_05_boss_oss_permission_fix.sql;
-- [109] 房间状态重复修复
source M21_06_fix_room_status_duplicate.sql;
-- [110] 自助预约 biz_bundle 修复
source M21_07_self_booking_biz_bundle_fix.sql;
-- [111] 资源使用中任务
source M21_08_resource_inuse_job.sql;

-- =====================================================================
-- 阶段二十五：员工排班（M22）
-- =====================================================================
-- [112] 员工排班
source M22_01_staff_schedule.sql;
-- [113] 审计列 BIGINT→VARCHAR 修复
source M22_02_fix_audit_columns_bigint_to_varchar.sql;
-- [114] 排班权限修复
source M22_03_staff_schedule_perm_fix.sql;
-- [115] 服务项额外配置
source M22_04_service_item_extra_config.sql;
-- [116] 排班变更日志审计修复
source M22_05_schedule_change_log_audit_fix.sql;
-- [117] 活动定价增强
source M22_06_activity_pricing_enhance.sql;

-- =====================================================================
-- 阶段三十一~三十二：签到 + 投诉与平台监管（M24/M25）
-- =====================================================================
-- [118] 签到(T24.1)
source M24_01_check_in.sql;
-- [119] 投诉与平台监管（T25.1~T25.7）：complaint_record / compliance_agreement + 菜单 7260~7271
source M25_01_complaint_supervision.sql;
-- [119a] 「资金平台管理菜单」(menu 1300) 终极兜底修复（修复执行完 1300 一级目录侧边栏不显示）
--        根因：M25_01 建的 7260「投诉监管」path='' 空字符串破坏前端 vue-router 树 → 1300 整体不渲染；
--        修复：7260 path→'complaint' + 6160 path→'regulatory' + 重建 1320~1345 子菜单 + re-parent 6120/6160/7000/7005 + 补 role_id=1 授权。
--        ★幂等：INSERT IGNORE / ON DUPLICATE KEY UPDATE / 守卫 UPDATE，可重复执行（依赖 M25_01）
source M25_02_fund_platform_menu_final_fix.sql;
-- [119b] complaint_record / compliance_agreement 审计列补全（修复 Unknown column 'create_by'）
source M25_03_fix_complaint_audit_columns.sql;
-- [119c] 租户管理员菜单隔离修复 + 合规协议签署守卫权限：
--        ① 清理 M25_01 v1.0 误授 platform_admin 的平台侧监管菜单（1300/6000 整棵子树，含 7260~7271/7000）；
--        ② role_template 移除 system:supervision:* 误加键、追加 micro:complianceAgreement:view/sign；
--        ③ 新建 7520/7521 F 按钮（T25.5 登录签署守卫，修复租户管理员调用 403、签署弹窗不出现问题）。
--        ★幂等：DELETE+INSERT / JSON_CONTAINS 守卫 / 递归 CTE 清理，可重复执行
source M25_04_fix_tenant_admin_menu_isolation_and_agreement_sign.sql;

-- =====================================================================
-- 阶段九-b（收尾）：存量租户菜单对齐
-- =====================================================================
-- [120] 存量基础版租户克隆角色菜单对齐修复
source M2_11_basic_boss_menu_realign.sql;

-- =====================================================================
-- 阶段三十三：行业模板扩展（新增行业只配模板，小程序零改动）
-- =====================================================================
-- [121] 旅游行业模板种子（房间=车辆/员工=导游/老板=旅行社社长）
source M15_28_industry_template_travel.sql;

-- =====================================================================
-- 阶段三十四：角色映射缺失补录（v1.45 修复）
-- =====================================================================
-- [122] 存量租户角色映射缺失角色补录（幂等 NOT EXISTS；如旅游租户缺 salesman/customer）
source M15_29_tenant_role_mapping_missing_backfill.sql;

-- =====================================================================
-- 阶段三十六：租户管理员角色错配 + boss 平台菜单泄漏修复（v1.84）
-- =====================================================================
-- [123] ★租户管理员双角色修复（admin 移除 boss 误挂）+ 投诉菜单 7261 改挂 7100
--        + 非超管角色清除 1300/6000 平台目录授权（修复侧边栏泄漏「资金平台管理菜单」）
source M25_05_fix_admin_role_and_boss_platform_menu_leak.sql;
-- [123a] complaint_record 表新增 staff_id / staff_name 列（投诉可关联员工 + 活动不限状态）
source M25_06_complaint_staff_fields.sql;

-- =====================================================================
-- 阶段三十七：M26 房间座位布局与选座（V3.9 T26.1~T26.3 / T26.7 / T26.8）
-- =====================================================================
-- [124] T26.1 room_seat_layout / room_seat / room_seat_layout_template 三表
--        + T26.2 service_item 补 has_seat_layout / seat_capacity 房间二态列
--        + T26.3 上传背景图菜单 7280（房间布局配置 C，暂隐藏）/ 7281（micro:room:uploadBg F）
--        + role_template boss(micro_pro) 追加权限键 + 存量 sys_role_menu 补录（micro_lite 不授）
source M26_01_room_seat_layout.sql;

-- [125] T26.8 活动座位占用表 activity_seat_assignment + 操作日志表 activity_seat_operation_log
--        + T26.7 顾客小程序选座菜单 7290（perms=micro:booking:pickSeat, client_type=mini）
--        + role_template customer(micro_pro/micro_lite) 追加权限键 + 存量 sys_role_menu 补录
--        ★T26.1 room_seat_layout / room_seat 表由 M26_01（[124]）建立
source M26_03_seat_assignment_and_pick_menu.sql;

-- [126] T26.4~T26.6 FEAT_ROOM_SEAT_LAYOUT 功能开关（feature_flag 第 41 项，组 E，micro_pro 专属）
--        + biz_bundle 全部业态包 feature_flags 幂等补录（三维交集根因防御，参考 M21_07 模式）
--        + sys_menu 新增 7282（复制布局 F）/ 7283（存为模板 F）/ 7285（默认房间模板 C，parent=7510）
--          / 7286（新增/编辑/删除 F，perms 逗号分隔多 key 覆盖 Controller 三处 @SaCheckPermission）
--        + sys_menu 7280（房间布局配置 C）visible='1' → '0'（T26.4 前端页面同批交付置显）
--        + role_template boss(micro_pro) 追加 micro:room:copyLayout / micro:room:saveTemplate（micro_lite 不授）
--        + role_template platform_admin(micro_pro/micro_lite) 追加 system:platformRoomTemplate:view / edit
--        + 存量 sys_role_menu 补录（boss 排除 micro_lite；platform_admin 全档；超管 role_id=1 兜底）
--        ★前置：[124]M26_01（三表 + 菜单 7280/7281）+ [125]M26_03（activity_seat_assignment + 菜单 7290）
--               + [75]M20_05（biz_bundle 补录模式）+ [85]M21_01（feature_flag 组 E 参考）
--               + [92]M21_07（biz_bundle 三维交集根因防御模式）+ [24]M15_23（原子资源管理目录 7510）
source M26_02_room_layout_template_and_menu.sql;

-- =====================================================================
-- 阶段三十八：M27 主题风格命名对齐（V3.9 T27.1）
-- =====================================================================
-- [127] T27.1 sys_tenant.app_theme_key 存量值迁移（theme_default→theme_fresh / theme_warm→theme_vivid
--        / theme_elegant→theme_dark / NULL→theme_fresh）+ 列默认值改 theme_fresh
--        + biz_theme_preset 补 theme_handdrawn Design Token 记录（幂等）
--        ★代码联动（BE PRESET_THEMES 常量 / MP theme.js / PC 主题管理页）同批交付，无需 SQL
--        ★前置：[59]M15_20（app_theme_key 补列）+ [09]M12_02（biz_theme_preset 建表）
source M27_01_theme_key_rename_and_handdrawn.sql;

-- =====================================================================
-- 阶段三十九：M28 人员编排与活动锁定（V3.9 T28.1 / T28.4 / T28.5）
-- =====================================================================
-- [128] T28.1 promotion_activity.status 列 COMMENT 扩展 LOCKED 值域（运行时锁定以 is_locked=1 为权威标志）
--        + T28.5 FEAT_ACTIVITY_LOCK 功能开关（feature_flag 第 42 项，组 E，micro_lite + micro_pro 通用）
--          + biz_bundle 全部业态包 feature_flags 幂等补录（三维交集根因防御，参考 M21_07 / M26_02 模式）
--        + sys_menu 新增 7287（锁定活动 F, micro:activity:lock）/ 7288（解锁活动 F, micro:activity:unlock）
--          / 7289（编排座位 F, micro:seat:assign），parent=7151 活动管理 C，client_type=pc
--        + role_template boss 追加 micro:activity:lock / micro:activity:unlock（两档）+ micro:seat:assign（micro_pro，micro_lite 不授）
--        + 存量 sys_role_menu 补录（boss 7287/7288 全档；7289 排除 micro_lite；超管 role_id=1 兜底）
--        ★T28.4 操作日志切面（@SeatOperation + SeatOperationLogAspect）为纯 Java 交付，无 DDL；
--          activity_seat_operation_log 表已由 [125]M26_03 建立
--        ★前置：[26]M15_11（promotion_activity 表）+ [99]M22_06（is_locked 列）
--               + [125]M26_03（activity_seat_assignment / activity_seat_operation_log 两表）
--               + [02]M1_05（feature_flag / biz_bundle）+ [24]M15_06（sys_menu 7150/7151）
source M28_01_activity_lock_and_seat_log.sql;

-- =====================================================================
-- 阶段四十：M26 房间布局配置菜单 platform_admin 授权修复（V3.9 T26.4 补遗）
-- =====================================================================
-- [129] T26.4 补遗：菜单 7280（房间布局配置 C）挂在 7100（boss 专属目录）下，
--        platform_admin 无 7100 访问权限 → 看不到该菜单。
--        ★尊重 M15_26 角色隔离原则（boss ≠ platform_admin 菜单互不可见），
--          不 re-parent 7280，而是新建 7291（同组件）挂 7510（原子资源管理）下。
--        + sys_menu 新增 7291（房间布局配置 C，parent=7510）/ 7292（上传背景图 F）
--          / 7293（复制布局 F）/ 7294（存为模板 F），perms 与 7280~7283 一致
--        + role_template platform_admin(micro_pro/micro_lite) 追加
--          micro:room:layout / micro:room:uploadBg / micro:room:copyLayout / micro:room:saveTemplate
--        + 存量 sys_role_menu 补录（platform_admin 7291~7294；超管 role_id=1 兜底）
--        ★前置：[124]M26_01（7280/7281 + boss 授权）+ [126]M26_02（7280 置显 + 7282/7283）
--               + [24]M15_23（原子资源管理目录 7510）+ [62]M15_26（角色隔离原则）
source M26_04_room_layout_menu_fix.sql;

-- =====================================================================
-- 阶段四十一：M26 默认房间模板菜单归属修正（platform_admin → admin，T26.6 修正）
-- =====================================================================
-- [130] T26.6 修正：菜单 7285/7286（默认房间模板）原挂 7510（原子资源管理，
--        platform_admin 专属）下，但"默认房间模板"是平台级兜底配置
--        （tenant_id=1 + is_industry_default=1，跨租户共享），应由 admin 统一维护。
--        + sys_menu 7285/7286 parent_id 从 7510 改为 7000（行业模板管理，admin 专属）
--        + role_template platform_admin 两档移除 system:platformRoomTemplate:view/edit
--        + 存量 sys_role_menu 清理 platform_admin 克隆角色对 7285/7286 的授权
--        ★前置：[126]M26_02（7285/7286 首建）+ [129]M26_04（7291~7294 补遗）
source M26_06_default_room_template_admin_only.sql;

-- =====================================================================
-- 阶段四十二：M21 菜单术语对齐（助教状态管理 → 员工状态管理）
-- =====================================================================
-- [131] 菜单 7203 改名：助教状态管理 → 员工状态管理（仅显示名与 remark，
--        perms/path/component 不变，无需 role_template / sys_role_menu 变更）
--        + PC 页面 micro/resourceStatus/staff/index.vue 文案同批对齐（助教→员工）
--        ★幂等：UPDATE 天然幂等，可重复执行；执行后重新登录刷新菜单名称缓存
--        ★前置：[86]M21_02（菜单 7203~7206）
source M21_09_staff_status_menu_rename.sql;

-- =====================================================================
-- 阶段四十三：V3.9 §一.3 活动海报多图（1~6 张横滑）cover_url 扩列
-- =====================================================================
-- [132] V3.9 §一.3 交付补全：PC 推广发布「活动图片」limit 1→6，
--        cover_url 存逗号分隔多值（ossId 串或编辑回显后的 URL 串），
--        原 VARCHAR(500) 在 6 条 URL 拼接场景可能超限 → 扩为 VARCHAR(2000)。
--        ★INFORMATION_SCHEMA 守卫幂等；无菜单/权限/开关变更
--        ★前置：[26]M15_11（promotion_activity 表）
source M26_07_activity_poster_multi.sql;

-- =====================================================================
-- 阶段四十四：旅游行业模板补「司机」角色映射（T26.10 背景图加人配套）
-- =====================================================================
-- [133] Boss 发布/编辑活动时可在房间座位布局背景画布上「加人」（＋号选人、
--        头像上座）：候选人为租户下全部员工（导游/司机）+ 注册游客；
--        导游、司机均为 salesman 标准角色的行业显示名映射，M15_28 旅游模板
--        仅配「导游」，本脚本 industry_role_map 补('travel','司机','salesman')。
--        ★INSERT IGNORE 幂等；无菜单/权限/开关/表结构变更（画布保存复用
--        activity_seat_assignment 既有结构 + 7289 micro:seat:assign 权限）
--        ★前置：[121]M15_28（旅游行业模板种子）
source M15_30_industry_role_travel_driver.sql;

-- =====================================================================
-- 阶段四十五：旅游行业「司机」映射落库租户角色映射（同 role_code 多业务角色名）
-- =====================================================================
-- [134] 租户端「角色映射」页缺「司机」行：司机与导游同为 salesman 的行业业务
--        角色名映射（不新建角色 code），但 tenant_role_mapping 唯一键
--        uk_tenant_role(tenant_id,role_code) 与自愈/初始化 role_code 去重口径使
--        同 code 第二业务名永远无法落库。本脚本：①唯一键放宽为
--        uk_tenant_role_code_name(tenant_id,role_code,display_name)；
--        ②industry_role_map(travel,'司机') default_flag 0→1；③存量租户按
--        「位序对账」补录附加业务名行（租户改名占位不复活旧名，V3.5 §16.4 脱钩）。
--        ★代码配套 v1.46：IndustryTemplateServiceImpl#initTenantByIndustry 同口径
--        位序对账（新建租户/自愈自动含司机）；roleDisplayNameMap/可选角色清单加
--        sort_order 确定性排序（同 code 多行时取首位业务名）。
--        ★无菜单/权限/开关变更；执行后无需刷新权限快照
--        ★前置：[121]M15_28 + [133]M15_30
source M15_31_tenant_role_mapping_multi_bizname.sql;

-- =====================================================================
-- 阶段四十六：人员管理角色下拉按映射行呈现（记录所选映射行）
-- =====================================================================
-- [135] 「人员管理」新增/修改弹窗角色下拉原硬编码 3 个标准角色（boss/salesman/
--        customer），未走租户角色映射显示名；[134]M15_31 后同 code 存在多业务名行
--        （导游/司机），下拉应按映射行呈现 4 项并记录所选行。本脚本：
--        user_tenant_role 加列 role_mapping_id（分配时选中的 tenant_role_mapping.id）
--        + 存量按 (tenant_id,role_code) 首映射行回填（sort_order,id 升序，仅补 NULL）。
--        ★代码配套：UserTenantRoleServiceImpl#assign 落库并校验 roleMappingId、
--          #fillDisplay 优先按映射ID取显示名（空回退 role_code 首名）；前端人员管理
--          下拉改为本租户角色映射启用行数据源（映射名+编码）
--        ★无菜单/权限/开关变更；执行后需重启后端
--        ★前置：[134]M15_31
source M15_32_user_tenant_role_mapping_id.sql;

-- =====================================================================
-- 阶段四十七：PROD 修复——OSS 公网域名启用 + 存量 127.0.0.1 URL 批量替换
-- =====================================================================
-- [136] 真机「localhost 嫌疑验证」命中（测试表 Sheet3 第 1~2 行）：sys_oss_config
--        minio 行 domain 为空 → OssClient.getUrl() 回退用 endpoint=127.0.0.1:9000
--        拼 URL，真机把 127.0.0.1 当手机自己 → 图片/语音全挂。
--        本脚本：① minio/image 行 domain='xiaozhangben.com.cn/oss'（endpoint 保留供
--        服务端 SDK 直连本机 MinIO，不可改）；② 幂等存储过程扫全库字符串列，
--        把 http(s)://127.0.0.1:9000 / localhost:9000 前缀替换为 https://xiaozhangben.com.cn/oss。
--        ★执行后必须重启后端或在「文件存储配置」页切换一次启用（Redis 缓存）；
--          前置：Nginx /oss 反代 MinIO（proxy_pass 尾斜杠）+ MinIO 桶匿名读
--        ★纯数据修复，无表结构/菜单/权限变更；幂等可重复执行
source PROD_02_oss_public_domain_and_url_repair.sql;

-- =====================================================================
-- 阶段四十七-B：PROD 域名启用——IP 118.89.133.114 → xiaozhangben.com.cn 全量迁移
-- =====================================================================
-- [136b] 域名已审批通过，将生产数据库中所有残留的 IP 地址替换为域名：
--        ① sys_oss_config domain 从 '118.89.133.114/oss' 改为 'xiaozhangben.com.cn/oss'；
--        ② 幂等存储过程扫全库字符串列，把存量 http://118.89.133.114/oss 替换为 https://xiaozhangben.com.cn/oss。
--        ★执行后必须重启后端 + 刷新「文件存储配置」Redis 缓存
--        ★前置：[136]PROD_02 + Nginx server_name 已含域名 + SSL 证书已部署
--        ★纯数据修复，无表结构/菜单/权限变更；幂等可重复执行
source PROD_03_ip_to_domain_migration.sql;

-- =====================================================================
-- 阶段四十八：v1.3 问题清单 BUG-04 修复——活动参与人数存量回填
-- =====================================================================
-- [137] v1.3 BUG-04：活动已有游客报名但参与人数显示仍为 0
--        根因：customerSignup/cancelSignup 仅落 project_signup 明细，
--              未同步回写 promotion_activity.participant_count。
--        本脚本：对存量活动一次性回填 participant_count = SUM(project_signup.people_count
--              WHERE is_waitlist IN (0, NULL))，新数据由应用层 refreshParticipantCount() 实时维护。
--        ★纯数据修复，无表结构 / 菜单 / 权限 / 开关变更；幂等可重复执行
source M15_33_activity_participant_count_backfill.sql;

-- =====================================================================
-- 阶段四十九：v1.3 需求清单 REQ-01/02/03（M29 三脚本，均幂等）
-- =====================================================================
-- [138] REQ-01 自助预约三类资源控制：booking_policy 幂等加 6 开关列
--        （project/room/staff × enabled/required）；默认 enabled=1、required=0；
--        旅游/按摩/足浴行业由后端 savePolicy 硬校验强制项目必选，本脚本仅加列。
source M29_01_booking_policy_resource_switches.sql;
-- [139] REQ-02 活动收益拆应收/实收：promotion_activity 幂等加 3 列
--        （receivable_amount / received_amount / received_confirmed_time）；
--        应收=首次报名一次锁定 COALESCE(custom_price, auto_calculated_price)；
--        实收=活动结束后回写（人均应付×已支付报名人数）；含存量 participant_count>0 补跑。
source M29_02_promotion_activity_receivable_received.sql;
-- [140] REQ-03 员工提成比例配置（仅 PC）：新建 staff_commission 表（唯一键 tenant_id+staff_user_id+del_flag）
--        + 菜单 7350~7354（目录/列表/add/edit/remove/export，挂 7500 租户管理下，platform_admin 专属）
--        + role_template platform_admin 两档授权 + 存量克隆角色 sys_role_menu 补录（INSERT IGNORE）。
source M29_03_staff_commission.sql;

-- =====================================================================
-- 阶段五十：活动海报不限张数（cover_url 扩为 TEXT）
-- =====================================================================
-- [141] 老板 PC 创建活动海报上传不再限 6 张：前端 image-upload :limit 999，
--        promotion_activity.cover_url 由 VARCHAR(2000) 扩为 TEXT，避免多张 URL 拼接超限。
--        ★无菜单/权限/开关变更，执行后无需刷新权限快照。
source M29_04_activity_cover_url_unlimited.sql;

-- =====================================================================
-- 阶段五十一：清理残留的 version_type='micro' 脏数据（M15_07 历史遗留）
-- =====================================================================
-- [142] 清理 role_template 中 M15_10 分档后未删除的 version_type='micro' 残留记录
--        小微版只有 micro_pro / micro_lite 两个版本，'micro' 为 M15_07 历史脏数据
--        + 修复 M18_08/M18_14/M22_06/M26_01/M26_02/M28_01 等脚本中误引用 'micro' 的 WHERE 条件
source M30_01_cleanup_micro_version_type.sql;

-- =====================================================================
-- 阶段五十二：敏感词功能激活与旧表数据补迁
-- =====================================================================
-- [143] 前端敏感词管理页已切换至新接口 /compliance/tenant/sensitiveWord/*，
--        但 FEAT_SENSITIVE_WORD 功能开关可能处于关闭态 + 旧表 sensitive_word_library
--        中 M18_11 执行后通过旧接口添加的词未迁移到新表，导致功能未生效。
--        修复：① 幂等重assert FEAT_SENSITIVE_WORD 开关（status='0' + micro_enabled=1）；
--              ② 补迁 sensitive_word_library 中尚未迁入的有效词（INSERT IGNORE + NOT EXISTS）；
--              ③ 补录 biz_bundle 业态包 feature_flags 确保含 FEAT_SENSITIVE_WORD。
--        ★前置：[54]M18_10 + [55]M18_11
--        ★幂等：UPDATE WHERE 条件守卫 + INSERT IGNORE + NOT JSON_CONTAINS，可重复执行
--        ★执行后请重启后端清除 SensitiveWordEngine 内存缓存
source M31_01_sensitive_word_activation.sql;
-- [144] 租户敏感词 F 按钮权限落地（修复 micro:sensitiveWord:* 权限链断裂，403 修复）
source M31_02_sensitive_word_permission_fix.sql;

-- =====================================================================
-- 阶段五十三：Boss 房间布局配置菜单隐藏（M31_03）
-- =====================================================================
-- [145] Boss 侧边栏不应显示「房间布局配置」菜单（7280）：
--        M26_02 将 7280 从 visible='1' 改为 '0'，但房间布局配置为 platform_admin 功能
--        （平行菜单 7291 挂 7510），Boss 不应看到 7280。改回 visible='1'（隐藏）。
--        ★同步修正 M26_02 源脚本，防止重新执行时再次置显。
--        ★执行后请退出重新登录，刷新菜单缓存
source M31_03_boss_room_layout_menu_hide.sql;

-- =====================================================================
-- 阶段五十四：「微信推广码」改名为「H5推广码」+ 改用 H5 二维码（M26_04）
-- =====================================================================
-- [146] 菜单 7156 改名：微信推广码 → H5推广码，子按钮 7157/7158 文案同步更新；
--        前端 wechatQrcode/index.vue 改用 /micro/promo/code/myQrcode 接口
--        （H5 二维码，与 app 端 boss 推广码共用同一后端方法）。
--        ★幂等：UPDATE 天然幂等，可重复执行
--        ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M26_04_h5_qrcode_rename.sql;

-- =====================================================================
-- 阶段五十五：boss「消息提醒」权限修复（micro:notify:list 403）
-- =====================================================================
-- [147] boss 登录小程序调 /micro/notify/list（notice-bar 通知栏 / bossTodo 预约失败
--        通知卡 / mine 消息列表）报「无此权限：micro:notify:list」，根因有二：
--        ① [78]M15_26 历史清理曾按 7200 目录树整树删除租户 boss 授权（含 7220）；
--        ② micro_lite 档 role_template boss 配方仅含 micro:notify:config 未含 notify:list。
--        Step1: micro_lite boss 配方补 micro:notify:list（JSON_CONTAINS 守卫）；
--        Step2: 存量小微租户 boss 角色（含共享预设 boss）按 perms 补写 sys_role_menu。
--        ★幂等可重复执行；前置：[45]M15_06（菜单 7220）+ [49]M15_10（分档配方）
--        ★执行后必须退出重新登录小程序，刷新 Sa-Token 权限快照
source M15_20_fix_boss_notify_menu_grant.sql;

-- =====================================================================
-- 阶段五十六：活动应收金额发布时锁定（存量数据回填，M29_05）
-- =====================================================================
-- [148] 应收金额语义调整：活动发布时即锁定 receivable_amount = COALESCE(custom_price, auto_calculated_price)，
--        不再等首次报名才锁定。本脚本回填存量已发布活动的 receivable_amount，
--        同时后端 changeStatus/insert 已改为发布时立即写入。
--        ★幂等：仅 receivable_amount IS NULL 且 status=PUBLISHED 时更新，可重复执行
--        ★前置：[139]M29_02 + [99]M22_06
source M29_05_receivable_amount_publish_backfill.sql;

-- =====================================================================
-- 阶段五十七：资源价格设置升级——staff_pricing 补计价模式列（生产 Unknown column 修复）
-- =====================================================================
-- [149] 生产报错 Unknown column 'pricing_type' in 'field list' 修复：
--        Java 实体 StaffPricing 已声明 pricingType / extraConfig 两字段（人员定价 4 档计价模式），
--        但建表脚本 [106]M21_03 未含这两列，StaffPricingServiceImpl.listAll() 查询即报错。
--        本脚本幂等 ALTER 补 pricing_type VARCHAR(20) NOT NULL DEFAULT 'duration'
--        + extra_config TEXT（存量行自动回填 duration，无需 UPDATE）。
--        ★幂等：INFORMATION_SCHEMA 守卫条件 ALTER，可重复执行；无菜单/权限/开关变更
--        ★前置：[106]M21_03（staff_pricing 建表）
source M21_10_staff_pricing_pricing_type.sql;

-- =====================================================================
-- 阶段五十八：M31 T31.5 隐私分级基座（小账本 H5 界面开发任务文档 v1.1）
-- =====================================================================
-- [150] T31.5 隐私分级基座：
--        ① 新建 staff_contact_public 表（员工联系方式公开开关，租户级 UNIQUE(tenant_id,staff_id)），
--          新入职默认全关（show_phone=0, show_wechat=0）；
--        ② sys_tenant 补 contact_wechat 列（L1 门店客服微信号，「联系我们」长按识别二维码）；
--        ③ 菜单 7059（员工联系方式公开开关，挂 7051 员工档案下，perms=micro:staff:manage）
--          + 7063（门店联系方式，挂 7058 店铺信息设置下，perms=micro:store:manage）；
--        ④ role_template boss（micro_pro/micro_lite）+ platform_admin 追加两个权限键；
--          存量小微版克隆角色 sys_role_menu 补录。
--        ★幂等：CREATE TABLE IF NOT EXISTS + INFORMATION_SCHEMA 守卫 ALTER + 先删后插菜单 +
--                JSON_CONTAINS 守卫追加 + INSERT IGNORE 补录，可重复执行
--        ★前置：[45]M15_06（菜单 7000~7499） + [46]M15_07（micro 角色模板）
--                + [49]M15_10（micro_lite/micro_pro 分档配方）
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
source M31_04_staff_contact_public_and_store_wechat.sql;

-- =====================================================================
-- 阶段五十九：M35 T35.1 结算分账机制（小账本 H5 界面开发任务文档 v1.2）
-- =====================================================================
-- [151] T35.1 员工分成规则 + 活动分成快照 + 活动其他成本：
--        ① 新建 staff_commission_rule 表（三种类型 FIXED_AMOUNT/PERCENTAGE/TIERED，UNIQUE(tenant_id,staff_id)），
--          含 include_addon_in_commission / base_salary / is_active；
--        ② 新建 activity_staff_snapshot 表（历史账不可变，UNIQUE(activity_id,staff_id)），
--          snapshot_value JSON 快照 + calculated_amount 结算时回写；
--        ③ promotion_activity 补 other_cost 列（定价页自动算账用，员工成本走快照不在此列）；
--        ④ 菜单 7064~7068（分成规则 CRUD 五按钮，挂 7051 员工档案下）
--          + 7069（预估vs实际报表 C 级页，挂 7100 老板 PC 首页下）；
--        ⑤ role_template boss（micro_pro/micro_lite）+ platform_admin 追加 6 个权限键；
--          存量小微版克隆角色 sys_role_menu 补录。
--        ★幂等：CREATE TABLE IF NOT EXISTS + INFORMATION_SCHEMA 守卫 ALTER + 先删后插菜单 +
--                JSON_CONTAINS 守卫追加 + INSERT IGNORE 补录，可重复执行
--        ★前置：[45]M15_06（菜单 7000~7499） + [46]M15_07（micro 角色模板）
--                + [150]M31_04（占用 7059/7063）
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
source M35_01_staff_commission_rule_and_snapshot.sql;

-- =====================================================================
-- 阶段六十：M36 活动结算单 + M37 推广码与海报（小账本 H5 界面开发任务文档 v1.2）
-- =====================================================================
-- [152] T36.1/T36.2 活动结算单三表 + 行业结算模板表 + 菜单 7360~7372：
--        ① activity_settlement（结算单主表，状态机 DRAFT/SUBMITTED/LOCKED，含调整单标记）；
--        ② settlement_income_item（收入项明细，TICKET/ADDON/COLLECTED 三类，代收不计入利润）；
--        ③ settlement_cost_item（成本项明细，EMPLOYEE 由快照自动生成 / OTHER 可手填）；
--        ④ industry_settlement_template（行业收入项模板，桌游/旅游/按摩三行业种子数据）；
--        ⑤ 菜单 7360~7364（结算单 C + 保存/提交/导出/调整 F）+ 7370~7372（海报 C + 员工/门店 F）；
--        ⑥ role_template boss（micro_pro/micro_lite）+ platform_admin 追加权限键；
--        ⑦ 存量小微版克隆角色 sys_role_menu 补录（INSERT IGNORE）。
--        ★幂等：CREATE TABLE IF NOT EXISTS + 先删后插菜单 + JSON_CONTAINS 守卫追加 + INSERT IGNORE 补录
--        ★前置：[26]M15_11（promotion_activity）+ [140]M29_03（staff_commission）
--                + [151]M35_01（activity_staff_snapshot 快照表，员工成本自动生成依赖）
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
source M36_01_activity_settlement_tables.sql;

-- =====================================================================
-- 阶段六十一：M34 T34.5 员工联系方式公开开关 PC 配置页菜单（小账本 H5 界面开发任务文档 v1.2）
-- =====================================================================
-- [153] T34.5 员工联系方式公开：老板 PC 端配置页菜单：
--        ① C 级页面菜单 7070（员工联系方式公开，挂 7100 小微运营下，
--          component=micro/staff/contactPublic/index，perms=micro:staff:manage）；
--        ② role_template boss + platform_admin 幂等确保含 micro:staff:manage（[150] 已追加，守卫跳过）；
--        ③ 存量小微版克隆角色 sys_role_menu 补录（INSERT IGNORE）。
--        ★表 staff_contact_public + F 按钮 7059 已在 [150]M31_04 交付，本脚本仅补可见的 C 级页面菜单。
--        ★幂等：先删后插菜单 + JSON_CONTAINS 守卫追加 + INSERT IGNORE 补录，可重复执行
--        ★前置：[45]M15_06（菜单 7000~7499，含 7100） + [150]M31_04（表 + micro:staff:manage 授权）
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照与前端路由
source M34_01_staff_contact_public_pc_menu.sql;

-- =====================================================================
-- 阶段六十二：M38 月度经营核算 + M39 三张报表（小账本 H5 界面开发任务文档 v1.3）
-- =====================================================================
-- [154] T38.4 月度经营月结表 monthly_operating_closing + 菜单 7380~7382：
--        ① monthly_operating_closing（月度税费 + 月结锁定状态，UNIQUE(tenant_id,month,del_flag)）；
--        ② 菜单 7380（月度经营报表 C-mini，micro:report:operating）+ 7381/7382（税费保存/月结锁定 F，micro:report:closing）；
--        ③ role_template boss（仅 micro_pro）追加权限键 + micro:report:export 幂等确保；
--        ④ 存量 micro_pro 老板克隆角色 sys_role_menu 补录（INSERT IGNORE）+ role_id=1 补录。
--        ★口径：月净利=月毛利−固定成本合计−当月税费；月保本毛利=固定成本合计；数据源 activity_settlement.profit。
--        ★权限：仅老板 micro_pro，lite/员工不授权（§21 验收4）。
--        ★幂等：CREATE TABLE IF NOT EXISTS（数据表不 DROP）+ 先删后插菜单 + JSON_CONTAINS 守卫 + INSERT IGNORE 补录
--        ★前置：[45]M15_06（小程序菜单 7000~7499 + 老板小程序根 7200） + [152]M36_01（activity_settlement 结算数据源）
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
source M38_01_monthly_operating_closing.sql;

-- [155] T39.1~T39.5 三张报表菜单 7390~7393（报表中心 + 活动明细/日报/对账）：
--        ① 菜单 7390（报表中心 C-mini，micro:report:activityDetail）+ 7391（活动明细 F）
--          + 7392（经营日报 F，micro:report:daily）+ 7393（对账校验 F，micro:report:reconcile）；
--        ② role_template boss（仅 micro_pro）追加 activityDetail/daily/reconcile 权限键；
--        ③ 存量 micro_pro 老板克隆角色 sys_role_menu 补录（INSERT IGNORE）+ role_id=1 补录。
--        ★对账铁律（T39.5）：日报月累计毛利 = 活动明细同月合计 = 月报月毛利（三者同源 settlement.profit）。
--        ★月报/月结权限（micro:report:operating/closing）在 [154]M38_01 交付，本脚本不重复。
--        ★权限：仅老板 micro_pro，lite/员工不授权（§21 验收4）。
--        ★幂等：先删后插菜单 + JSON_CONTAINS 守卫追加 + INSERT IGNORE 补录
--        ★前置：[45]M15_06 + [152]M36_01 + [154]M38_01
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
source M39_01_report_center_menus.sql;

-- =====================================================================
-- 阶段六十三：M33 §九 老板/员工权限分工——锁定申请审批（小账本 H5 界面开发任务文档 v1.8）
-- =====================================================================
-- [156] §九 活动锁定申请审批表 activity_lock_request：
--        ① activity_lock_request（员工「申请锁定」→ 老板「审批锁定申请」落地表，
--          状态机 PENDING/APPROVED/REJECTED，含活动标题/申请人/审批人快照）；
--        ② approve/reject 复用既有 micro:activity:lock（菜单 7287，[128]M28_01 已授 boss），
--          list 同权限守卫；员工申请端 /micro/staff/lockApply 仅需登录，无新增菜单/权限键；
--        ③ 对应 BE：ActivityLockRequest 实体/Mapper/Service + MicroBossLockRequestController
--          （/micro/boss/lockRequest/list|approve/{id}|reject/{id}）+ MicroStaffController.lockApply。
--        ★幂等：CREATE TABLE IF NOT EXISTS（数据表不 DROP，可重复执行）
--        ★前置：[26]M15_11（promotion_activity）+ [99]M22_06（is_locked）+ [128]M28_01（micro:activity:lock + FEAT_ACTIVITY_LOCK）
--        ★执行后请重启后端（无菜单变更，无需重新登录刷新权限快照）
source M33_01_activity_lock_request.sql;

-- =====================================================================
-- 阶段六十四：M37_01 租户管理员菜单层级重构（v4.0）
-- =====================================================================
-- [157] 租户管理员(platform_admin)菜单层级重构（v4.0）：
--         新建 6 个 M 类型中间目录：业务运营(7540)/人员与权限(7541)/成本分摊管理(7542)/
--          系统运维(7543)/小程序配置(7544)/AI语音合规(7545)；
--        ② 删除旧中间目录：原子资源管理(7510)/固定支出配置(7620)；
--         删除员工业绩榜(7650)——属 boss 菜单，租户管理员不需要；
--        ④ 复用旧菜单：7610→语音配置（改名+迁移到7545，保留三Tab页面）
--          7630 策略开关保持直属7500（与合规无关，独立菜单），保留子按钮7631；
--        ⑤ 重命名/迁移既有菜单：角色映射→角色权限配置 / 房间/空间管理→房间资源管理 /
--          标签管理→支出标签管理 / 周期规则管理→分摊周期规则 / 小程序主题→小程序主题设置；
--        ⑥ 7291 房间布局配置从 7510 迁移到 7540(业务运营)；
--        ⑦ 7500 根目录 icon people→office；版本升级/系统设置 改挂系统运维(7543)下；
--        ⑧ 新建员工提成管理(7551)叶子菜单；
--         role_template platform_admin 追加 micro:staffCommission:list。
--        ★幂等：先删后插（DELETE + INSERT），操作区段 7500~7559 / 7650~7659
--        ★前置：[4]M15_17(7500目录) + [30]M15_23(7510原子资源) + [43]M18_02(7620固定支出)
--              + [44]M18_03(7630策略开关) + [47]M18_05(7650业绩榜) + [56.5]M18_13(7610合规)
--              + [75]M18_14(7695系统设置) + [129]M26_04(7291房间布局)
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
source M37_01_tenant_admin_menu_restructure.sql;

-- =====================================================================
-- 阶段六十五：M40 platform_admin 标签/周期规则/员工提成 F 按钮权限全面补建
-- =====================================================================
-- [158] M37_01 重构删除旧 7620~7629（含 F 按钮 7622~7624/7626~7628），
--        但仅重建 C 菜单 7547/7550（:list），未补建 F 按钮菜单；
--        M29_03 旧菜单 7350~7354（员工提成）未清理，与 7551 重复；
--        M15_17 旧菜单 7508（小程序主题）未清理，与 7552（小程序主题设置）重复；
--        role_template 缺 tagConfig:add/edit + periodRule:add/edit +
--          staffCommission:add/edit/remove → Controller 403。
--        本脚本：①清理旧菜单 7350~7354（M29_03 遗留）+ 7508（M15_17 遗留）；
--              ②新建 F 按钮 7548/7549(标签)+7553/7554(周期规则)+7557~7559(提成)；
--              ③清理多余 7555(periodRule:remove，Controller 已改用 :edit)；
--              ④role_template platform_admin 追加 7 权限键（JSON_CONTAINS 守卫）；
--              ⑤存量 platform_admin 克隆角色 + 超管 role_id=1 补写 sys_role_menu。
--        配套代码：MicroTagPeriodRuleController.java
--          removeTag() tagConfig:remove → tagConfig:edit（编辑涵盖删除）
--          removeRule() periodRule:remove → periodRule:edit（编辑涵盖删除）
--        ★幂等：先删后插 + JSON_CONTAINS 守卫 + INSERT IGNORE，可重复执行
--        ★前置：[157]M37_01（菜单层级重构）+ [140]M29_03（员工提成原始菜单）
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
source M40_01_platform_admin_tag_period_button_perms.sql;

-- =====================================================================
-- 阶段六十六：M41 投诉记录 complainant_phone 存量回填（生产修复）
-- =====================================================================
-- [159] M41_01 投诉记录 complainant_phone/name 存量回填：
--        根因：前端 complaint.vue 提交时未传 complainantPhone/complainantName，
--        后端 submitComplaint 直接取 BO 字段 → 落库 NULL → myComplaints() 按手机号
--        匹配失败 → "我的投诉记录"为空。代码已改兜底取 LoginHelper，本脚本回填存量。
--        ★幂等：仅 WHERE complainant_phone IS NULL 时更新，可重复执行
source M41_01_complaint_phone_backfill.sql;

-- =====================================================================
-- 阶段六十七：M42 游客扫码登录身份锚点改为「手机号 + 顾客角色」（生产修复）
-- =====================================================================
-- [161] M42_01 游客扫码登录：身份锚点 phone_last4 → 完整手机号，并固定顾客角色：
--        根因：VisitorLoginController 建 sys_user 时从不写 sys_user_role（游客无角色，权限链断），
--        且按 temp_visitor_account(tenant_id + phone_last4) 复用 + 该表唯一键为后 4 位，
--        同租户不同手机号后 4 位相同即碰撞误复用。代码已改为按手机号+顾客角色定位/创建，
--        本脚本切换唯一键为 (tenant_id, phone) 并为存量无角色游客补挂 customer(9207)。
--        ★幂等：DDL 用 information_schema 守卫 + INSERT IGNORE / WHERE NOT EXISTS，可重复执行
--        ★前置：[160]M31_05（建表）+ M15_07（customer 角色 9207）
source M42_01_visitor_customer_role_and_phone_identity.sql;

-- =====================================================================
-- 附录：一次性修复/诊断脚本（不在执行顺序中，按需手动执行）
-- ---------------------------------------------------------------------
-- _cleanup_boss_role_users.sql          — 临时清理：删除所有 boss 克隆角色（不可逆！）
-- _diag_boss_tenant_menu.sql            — 诊断：boss 角色为何仍显示租户管理菜单
-- _diag_prod_missing_migrations.sql      — 诊断：生产库 [138]~[156] 增量脚本缺跑清单（只读，部署新 jar 后报 Unknown column 时先跑它）
-- _fix_myadmin_no_menu.sql              — 一次性修复：特定租户无菜单问题
-- fix_boss_remove_tenant_admin_menu.sql  — 一次性修复：boss 移除租户管理菜单
-- =====================================================================

-- =====================================================================
-- 验收检查（全量）
-- =====================================================================
-- 1) 平台底座表 + 角色模板 + 计费套餐 + 行业模板就绪
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('biz_bundle','feature_flag','tenant_biz_config','role_template',
--     'sys_billing_plan','sys_billing_order','industry_template','industry_role_map');
-- 预期 >= 8
--
-- 2) 资金平台管理菜单目录 + 子菜单完整
-- SELECT menu_id, menu_name, parent_id FROM sys_menu
--  WHERE parent_id = 1300 ORDER BY order_num;
-- 预期含：租户管理/标准角色/行业模板/菜单管理/角色模板/版本档位/
--        业态包(1320)/功能开关(1330)/单店覆盖(1340)/访问通道(1343)/
--        平台计费(6120)/合规管理(6160)
--
-- 3) 小微版菜单 7000~7499 完整
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7000 AND 7499;
-- 预期 > 50
--
-- 4) 投诉监管菜单(7260)存在且挂 1300 下
-- SELECT menu_id, menu_name, parent_id FROM sys_menu WHERE menu_id = 7260;
-- 预期：7260, 投诉监管, 1300
--
-- 5) 旅游行业模板就绪（M15_28，[121]）
-- SELECT industry_code, industry_name, sort_order, status FROM industry_template WHERE industry_code = 'travel';
-- 预期：1 行（travel / 旅游 / 24 / 0）
-- SELECT biz_role_name, role_code FROM industry_role_map WHERE industry_code = 'travel' ORDER BY sort_order;
-- 预期：旅行社社长→boss / 导游→salesman / 游客→customer
--
-- 6) 旅游租户角色映射完整（M15_29 补录，[122]）
-- SELECT m.role_code, m.display_name FROM sys_tenant t
--   JOIN tenant_role_mapping m ON m.tenant_id = t.id AND m.del_flag = '0'
--  WHERE t.industry = 'travel' ORDER BY m.sort_order;
-- 预期：3 行（boss/旅行社社长、salesman/导游、customer/游客）
--        ★注：[134]M15_31 会再补一行 salesman/司机，全量执行后为 4 行，以 14.3 为准。
--
-- 7) ★M26 房间座位布局全链路（[124]M26_01 + [125]M26_03 + [126]M26_02 + [129]M26_04）
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('room_seat_layout','room_seat','room_seat_layout_template',
--                      'activity_seat_assignment','activity_seat_operation_log');
-- 预期：5 行
-- SELECT code, flag_group, micro_enabled FROM feature_flag WHERE code = 'FEAT_ROOM_SEAT_LAYOUT';
-- 预期：1 行（E / micro_enabled=1）
-- SELECT menu_id, menu_name, parent_id, menu_type, visible, perms FROM sys_menu
--  WHERE menu_id IN (7280,7281,7282,7283,7285,7286,7290,7291,7292,7293,7294) ORDER BY menu_id;
-- 预期：11 行（7280 C visible=0 parent=7100 / 7281~7283 F parent=7280 / 7285 C parent=7000 / 7286 F
--          / 7290 F client_type=mini / 7291 C visible=0 parent=7510 / 7292~7294 F parent=7291）
--
-- 8) ★M27 主题命名对齐（[127]M27_01）
-- 8.1 app_theme_key 无残留旧值（预期 0 行）
-- SELECT id, app_theme_key FROM sys_tenant WHERE app_theme_key IN ('theme_default','theme_warm','theme_elegant');
-- 8.2 列默认值已改 theme_fresh
-- SELECT COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'app_theme_key';
-- 预期：theme_fresh
-- 8.3 biz_theme_preset 含 theme_handdrawn（预期 1 行）
-- SELECT preset_key, name, sort FROM biz_theme_preset WHERE preset_key = 'theme_handdrawn';
--
-- 9) ★M28 活动锁定与座位编排（[128]M28_01）
-- 9.1 promotion_activity.status COMMENT 已含 LOCKED 值域
-- SELECT COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity' AND COLUMN_NAME = 'status';
-- 预期：COMMENT 含 'LOCKED'
-- 9.2 FEAT_ACTIVITY_LOCK 落表且 micro_enabled=1（预期 1 行：E / micro_enabled=1 / basic_enabled=0 / status=0）
-- SELECT code, flag_group, micro_enabled, basic_enabled, status FROM feature_flag WHERE code = 'FEAT_ACTIVITY_LOCK';
-- 9.3 菜单 7287/7288/7289 存在且挂 7151 下（预期 3 行）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu
--  WHERE menu_id IN (7287,7288,7289) ORDER BY menu_id;
-- 预期：7287 F micro:activity:lock pc / 7288 F micro:activity:unlock pc / 7289 F micro:seat:assign pc
-- 9.4 role_template boss 权限键就绪（micro_pro 含 3 键，micro_lite 仅 lock/unlock）
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:activity:lock"')   AS has_lock,
--        JSON_CONTAINS(menu_keys,'"micro:activity:unlock"') AS has_unlock,
--        JSON_CONTAINS(menu_keys,'"micro:seat:assign"')     AS has_assign
--   FROM role_template WHERE role_code='boss' AND version_type IN ('micro_pro','micro_lite');
-- 预期：micro_pro 为 1/1/1；micro_lite 为 1/1/0
--
-- 10) ★M26_04 房间布局配置 platform_admin 授权修复（[129]M26_04）
-- 10.1 新菜单 7291~7294 存在且挂在 7510 下（预期 4 行）
-- SELECT menu_id, menu_name, parent_id, menu_type, visible, perms FROM sys_menu
--  WHERE menu_id IN (7291,7292,7293,7294) ORDER BY menu_id;
-- 预期：7291 C parent=7510 visible=0 / 7292~7294 F parent=7291
-- 10.2 原菜单 7280 仍在 7100 下（boss 专属，未改动）
-- SELECT menu_id, parent_id FROM sys_menu WHERE menu_id = 7280;
-- 预期：parent_id = 7100
-- 10.3 role_template platform_admin 两档均含房间布局权限（预期 2 行均 1/1/1/1）
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:room:layout"')    AS has_layout,
--        JSON_CONTAINS(menu_keys,'"micro:room:uploadBg"')   AS has_upload,
--        JSON_CONTAINS(menu_keys,'"micro:room:copyLayout"') AS has_copy,
--        JSON_CONTAINS(menu_keys,'"micro:room:saveTemplate"') AS has_save
--   FROM role_template WHERE role_code = 'platform_admin' AND status = '0';
-- 10.4 boss 不应有 7291~7294 或 7510（M15_26 隔离原则，预期 0 行）
-- SELECT rm.menu_id FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'boss' AND r.del_flag = '0'
--  WHERE rm.menu_id IN (7291,7292,7293,7294,7510);
--
-- 10) ★M26_06 默认房间模板菜单归属修正（[130]M26_06）
-- 10.1 菜单 7285/7286 已迁移到 7000（行业模板管理，admin 专属）下
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu WHERE menu_id IN (7285, 7286);
-- 预期：parent_id=7000
-- 10.2 platform_admin role_template 不再含平台模板权限（两档均 0/0）
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"system:platformRoomTemplate:view"') AS has_view,
--        JSON_CONTAINS(menu_keys,'"system:platformRoomTemplate:edit"') AS has_edit
--   FROM role_template WHERE role_code = 'platform_admin' AND status = '0';
-- 预期：两档均 0/0
-- 10.3 存量 platform_admin 克隆角色不再持有 7285/7286（预期 0 行）
-- SELECT COUNT(*) AS remaining FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'platform_admin'
--  WHERE rm.menu_id IN (7285, 7286);
-- 预期：0
-- 10.4 admin role_id=1 仍持有 7285/7286（预期 2 行）
-- SELECT menu_id FROM sys_role_menu WHERE role_id = 1 AND menu_id IN (7285, 7286) ORDER BY menu_id;
--
-- 11) ★M21_09 菜单改名：助教状态管理 → 员工状态管理（[131]M21_09）
-- 11.1 菜单 7203 已改名且 perms/path/component 不变
-- SELECT menu_id, menu_name, parent_id, path, component, perms, client_type FROM sys_menu WHERE menu_id = 7203;
-- 预期：7203 | 员工状态管理 | 7100 | resourceStatus/staff
--       | micro/resourceStatus/staff/index | micro:resourceStatus:staff:view | pc
-- 11.2 旧菜单名无残留（预期 0 行）
-- SELECT menu_id, menu_name FROM sys_menu WHERE menu_name = '助教状态管理';
--
-- 12) ★V3.9 §一.3 活动海报多图扩列（[132]M26_07）
-- 12.1 cover_url 列宽已扩至 2000 且 COMMENT 含多图语义
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity' AND COLUMN_NAME = 'cover_url';
-- 预期：varchar(2000)，COMMENT 含 '1~6 张'
--
-- 13) ★旅游行业模板补司机映射（[133]M15_30）
-- 13.1 industry_role_map 旅游模板含司机行
-- SELECT biz_role_name, role_code, default_flag FROM industry_role_map
--  WHERE industry_code = 'travel' ORDER BY sort_order;
-- 预期：4 行（旅行社社长→boss/1、导游→salesman/1、游客→customer/1、司机→salesman/0）
--        ★注：M15_30 落库时司机 default_flag=0；后续 [134]M15_31 会将其置 1，
--          全量执行后以 14.2 的 default_flag=1 为准。
--
-- 14) ★旅游租户角色映射含司机（[134]M15_31）
-- 14.1 唯一键已放宽（预期仅 uk_tenant_role_code_name）
-- SELECT INDEX_NAME FROM information_schema.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenant_role_mapping'
--    AND INDEX_NAME IN ('uk_tenant_role', 'uk_tenant_role_code_name') GROUP BY INDEX_NAME;
-- 14.2 司机行默认启用（预期 4 行，司机 default_flag=1）
-- SELECT biz_role_name, role_code, default_flag, sort_order FROM industry_role_map
--  WHERE industry_code = 'travel' ORDER BY sort_order;
-- 14.3 旅游租户角色映射 4 行且全启用
-- SELECT t.id, m.role_code, m.display_name, m.sort_order, m.status
--   FROM sys_tenant t
--   JOIN tenant_role_mapping m ON m.tenant_id = t.id AND m.del_flag = '0'
--  WHERE t.industry = 'travel' ORDER BY t.id, m.sort_order;
-- 预期：每租户 4 行（boss/旅行社社长、salesman/导游、customer/游客、salesman/司机，status 全 0）
--
-- 15) ★人员管理记录所选角色映射（[135]M15_32）
-- 15.1 user_tenant_role.role_mapping_id 列存在
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_tenant_role'
--    AND COLUMN_NAME = 'role_mapping_id';
-- 预期：role_mapping_id | bigint
-- 15.2 存量分配均已命中映射行（预期 0 行）
-- SELECT COUNT(*) AS unmapped FROM user_tenant_role utr
--  WHERE utr.del_flag = '0' AND utr.role_mapping_id IS NULL
--    AND EXISTS (SELECT 1 FROM tenant_role_mapping m
--                 WHERE m.tenant_id = utr.tenant_id AND m.role_code = utr.role_code
--                   AND m.del_flag = '0');
--
-- 16) ★PROD_02 OSS 公网域名与存量 URL 修复（[136]PROD_02）
-- 16.1 domain 已启用且 endpoint 未动
-- SELECT config_key, endpoint, domain FROM sys_oss_config WHERE config_key IN ('minio','image');
-- 预期：domain = xiaozhangben.com.cn/oss，endpoint 仍为 127.0.0.1:9000
-- 16.2 sys_oss 无 localhost / 127 残留 URL（预期 0）
-- SELECT COUNT(*) FROM sys_oss WHERE url LIKE '%//127.0.0.1:9000%' OR url LIKE '%//localhost:9000%';
--
-- 17) ★v1.3 BUG-04 活动参与人数存量回填（[137]M15_33）
-- 17.1 已选项目的活动 participant_count 与 project_signup 汇总一致（预期 0 行）
-- SELECT pa.id, pa.title, pa.participant_count,
--        COALESCE(s.total_people, 0) AS expect_count
--   FROM promotion_activity pa
--   LEFT JOIN (SELECT project_id,
--                     SUM(CASE WHEN is_waitlist = 1 THEN 0 ELSE COALESCE(people_count, 1) END) AS total_people
--                FROM project_signup WHERE del_flag = '0' GROUP BY project_id) s
--     ON s.project_id = pa.selected_project_id
--  WHERE pa.del_flag = '0' AND pa.selected_project_id IS NOT NULL
--    AND COALESCE(pa.participant_count, 0) <> COALESCE(s.total_people, 0);
--
-- 18) ★REQ-01 自助预约三类资源开关（[138]M29_01）
-- 18.1 booking_policy 6 开关列均存在（预期 6 行）
-- SELECT COLUMN_NAME, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking_policy'
--    AND COLUMN_NAME IN ('project_booking_enabled','project_booking_required',
--                        'room_booking_enabled','room_booking_required',
--                        'staff_booking_enabled','staff_booking_required');
-- 预期：6 行（enabled 列默认 1，required 列默认 0）
--
-- 19) ★REQ-02 活动应收/实收拆分（[139]M29_02 + [148]M29_05 发布时锁定应收）
-- 19.1 promotion_activity 3 列均存在（预期 3 行）
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
--    AND COLUMN_NAME IN ('receivable_amount','received_amount','received_confirmed_time');
-- 19.2 所有已发布活动应收均已回填（预期 0 行，M29_05 回填后）
-- SELECT COUNT(*) FROM promotion_activity
--  WHERE del_flag = '0' AND status = 'PUBLISHED' AND receivable_amount IS NULL;
--
-- 20) ★REQ-03 员工提成配置表 + 菜单 + 授权（[140]M29_03）
-- 20.1 staff_commission 表存在
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_commission';
-- 20.2 菜单 7350~7354 存在且挂 7500 下（预期 5 行）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--  WHERE menu_id BETWEEN 7350 AND 7354 ORDER BY menu_id;
-- 预期：7350 C parent=7500 component=micro/staffCommission/index perms=micro:staffCommission:list
--       / 7351~7354 F add/edit/remove/export
-- 20.3 role_template platform_admin 两档均含提成权限（预期 2 行均 1）
-- SELECT version_type, JSON_CONTAINS(menu_keys,'"micro:staffCommission:list"') AS has_list
--   FROM role_template WHERE role_code = 'platform_admin' AND status = '0';
-- 20.4 boss 角色不应持有 7350~7354（M15_26 隔离原则，预期 0 行）
-- SELECT rm.menu_id FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'boss' AND r.del_flag = '0'
--  WHERE rm.menu_id BETWEEN 7350 AND 7354;
--
-- 21) ★活动海报不限张数（[141]M29_04）
-- 21.1 cover_url 已扩为 TEXT（预期 1 行，DATA_TYPE=text）
-- SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
--    AND COLUMN_NAME = 'cover_url';
-- 预期：DATA_TYPE=text，COMMENT 含 '不限张数'
--
-- 22) ★boss 消息提醒权限修复（[147]M15_20_fix_boss_notify_menu_grant）
-- 22.1 micro_lite boss 配方已含 notify:list（预期 1）
-- SELECT JSON_CONTAINS(menu_keys,'"micro:notify:list"') AS has_notify_list
--   FROM role_template WHERE version_type = 'micro_lite' AND role_code = 'boss';
-- 22.2 小微租户 boss 角色无 7220 授权缺口（预期 0 行）
-- SELECT t.id FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--  WHERE t.tenant_type = 'micro'
--    AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm
--                     WHERE rm.role_id = r.role_id AND rm.menu_id = 7220);
-- 22.3 运行时验证：boss 退出重新登录 → 首页/待办不再报「无此权限：micro:notify:list」
--
-- 23) ★活动应收金额发布时锁定（[148]M29_05）
-- 23.1 所有已发布活动 receivable_amount 非空（预期 0 行）
-- SELECT COUNT(*) FROM promotion_activity
--  WHERE del_flag = '0' AND status = 'PUBLISHED' AND receivable_amount IS NULL;
-- 23.2 驾驶舱应收/实收卡片不再显示 ¥0.00（运行时验证：boss 小程序首页）
--
-- 24) ★M31 T31.5 隐私分级基座（[150]M31_04）
-- 24.1 staff_contact_public 表存在且含关键字段
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_contact_public'
--    AND COLUMN_NAME IN ('show_phone','show_wechat','wechat_qrcode_url');
-- 预期：3 行（tinyint(1)/tinyint(1)/varchar(500)）
-- 24.2 uk_tenant_staff 唯一键已建
-- SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_contact_public'
--    AND INDEX_NAME = 'uk_tenant_staff' GROUP BY INDEX_NAME;
-- 预期：1 行
-- 24.3 sys_tenant.contact_wechat 列已加
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'contact_wechat';
-- 预期：1 行 varchar(50)
-- 24.4 菜单 7059 / 7063 存在且挂载正确
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--  WHERE menu_id IN (7059, 7063) ORDER BY menu_id;
-- 预期：7059 F parent=7051 perms=micro:staff:manage / 7063 F parent=7058 perms=micro:store:manage
-- 24.5 role_template boss + platform_admin 均含两个权限键
-- SELECT role_code, version_type,
--        JSON_CONTAINS(menu_keys,'"micro:staff:manage"') AS has_staff,
--        JSON_CONTAINS(menu_keys,'"micro:store:manage"') AS has_store
--   FROM role_template
--  WHERE role_code IN ('boss','platform_admin') AND status = '0'
--    AND version_type IN ('micro','micro_pro','micro_lite');
-- 预期：全部行 has_staff=1 且 has_store=1
-- 24.6 存量小微版 boss + platform_admin 克隆角色已关联 7059 / 7063
-- SELECT r.role_key, COUNT(DISTINCT r.role_id) AS granted_roles
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key IN ('boss','platform_admin') AND r.del_flag = '0'
--   JOIN sys_tenant t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
--  WHERE rm.menu_id IN (7059, 7063)
--  GROUP BY r.role_key;
-- 预期：boss / platform_admin 各 >= 小微版租户数量
--
-- 25) ★M37_01 租户管理员菜单层级重构（[157]）
-- 25.1 新目录结构完整（预期 6 个 M 类型目录）
-- SELECT menu_id, menu_name, parent_id, icon, menu_type
--   FROM sys_menu WHERE menu_id IN (7540,7541,7542,7543,7544,7545);
--   -- 预期 6 行，全部 menu_type='M'，parent_id=7500
-- 25.2 旧菜单已清理（7510/7620/7650 预期 0 行）
-- SELECT COUNT(*) AS old_remaining FROM sys_menu
--   WHERE menu_id IN (7510,7620,7650);
-- 25.2b 7610 已改名+迁移到 7545 下；7630 保持直属 7500（预期 3 行）
-- SELECT menu_id, menu_name, parent_id, icon FROM sys_menu
--   WHERE menu_id IN (7610,7630,7631) ORDER BY menu_id;
--   -- 预期：7610 | 语音配置 | 7545 | document
--   --       7630 | 策略开关 | 7500 | date
--   --       7631 | 策略保存 | 7630 | #
-- 25.3 7500 根目录 icon 已更新
-- SELECT menu_id, menu_name, icon FROM sys_menu WHERE menu_id = 7500;
--   -- 预期 icon='office'
-- 25.4 房间布局配置 7291 已迁移到业务运营(7540)下
-- SELECT menu_id, menu_name, parent_id, icon FROM sys_menu WHERE menu_id = 7291;
--   -- 预期：7291 | 房间布局配置 | 7540 | layout
-- 25.5 role_template 含新权限
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:compliance:config:rule"') AS has_rule,
--        JSON_CONTAINS(menu_keys,'"micro:staffCommission:list"')   AS has_commission
--   FROM role_template WHERE role_code='platform_admin' AND status='0';
--   -- 预期：各档全部 1
--
-- 26) ★M40_01 platform_admin 标签/周期规则/员工提成 F 按钮权限全面补建（[158]）
-- 26.1 F 按钮菜单已落库（预期 7 行，全部 menu_type='F'）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms
--   FROM sys_menu WHERE menu_id IN (7548,7549,7553,7554,7557,7558,7559) ORDER BY menu_id;
--   -- 预期：7548/7549 挂 7547 / 7553/7554 挂 7550 / 7557~7559 挂 7551
-- 26.2 旧菜单 7350~7354/7508/7555 已清理（预期 0 行）
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id IN (7350,7351,7352,7353,7354,7508,7555);
-- 26.3 role_template 含全部按钮权限
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:tagConfig:add"')          AS has_tag_add,
--        JSON_CONTAINS(menu_keys,'"micro:tagConfig:edit"')         AS has_tag_edit,
--        JSON_CONTAINS(menu_keys,'"micro:periodRule:add"')         AS has_rule_add,
--        JSON_CONTAINS(menu_keys,'"micro:periodRule:edit"')        AS has_rule_edit,
--        JSON_CONTAINS(menu_keys,'"micro:staffCommission:add"')    AS has_comm_add,
--        JSON_CONTAINS(menu_keys,'"micro:staffCommission:edit"')   AS has_comm_edit,
--        JSON_CONTAINS(menu_keys,'"micro:staffCommission:remove"') AS has_comm_remove
--   FROM role_template WHERE role_code='platform_admin' AND status='0';
--   -- 预期：micro_pro / micro_lite 各 1 行，全部 1
-- 26.4 存量 platform_admin 克隆角色已关联新按钮
-- SELECT t.id, COUNT(DISTINCT rm.menu_id) AS new_btn_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND rm.menu_id IN (7548,7549,7553,7554,7557,7558,7559)
--  GROUP BY t.id;
--   -- 预期：每个小微版租户 1 行，new_btn_count = 7
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

-- 27) ★M41_01 投诉记录 complainant_phone 回填（[159]）
-- 27.1 有效投诉记录无 NULL 手机号（预期 0 行）
-- SELECT COUNT(*) AS null_phone_count FROM complaint_record
--  WHERE complainant_phone IS NULL AND del_flag = '0';
-- 预期：0
-- 27.2 重新提交投诉 → 小程序"我的投诉记录"应显示刚提交的记录

-- =====================================================================
-- 阶段六十七：M31 临时游客账号表 + 游客信息补全表 + 菜单 + 功能开关（T31.3）
-- =====================================================================
-- [160] T31.3 临时游客账号表 temp_visitor_account + 游客信息补全表 visitor_profile
--        + 菜单 7300~7305（Boss 海报生成 + 游客信息管理）
--        + FEAT_VISITOR_SCAN_LOGIN 功能开关（第 44 项，组 E，小微版启用）
--        + role_template boss / platform_admin 追加 M31 权限键
--        + 存量 sys_role_menu 补录
--        ★幂等：CREATE TABLE IF NOT EXISTS + 先删后插菜单 + INSERT IGNORE 功能开关/角色授权
--        ★前置：[41]M15_01（sys_user 表）+ [45]M15_06（sys_menu 7000~7499）
--               + [46]M15_07（micro 角色模板）+ [49]M15_10（分档配方）
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
source M31_05_temp_visitor_and_profile.sql;

-- =====================================================================
-- 验收检查（M31 新增）
-- =====================================================================
-- 28) ★M31 T31.3 临时游客账号 + 游客信息补全（[160]M31_05）
-- 28.1 两表已创建
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME IN ('temp_visitor_account','visitor_profile');
-- 预期：2 行
-- 28.2 菜单 7300~7305 存在
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--  WHERE menu_id BETWEEN 7300 AND 7305 ORDER BY menu_id;
-- 预期：6 行
-- 28.3 FEAT_VISITOR_SCAN_LOGIN 功能开关已注册
-- SELECT code, flag_group, micro_enabled FROM feature_flag WHERE code = 'FEAT_VISITOR_SCAN_LOGIN';
-- 预期：1 行（E / micro_enabled=1）

-- =====================================================================
-- 阶段六十八：PROD_04 重复房间数据清理（生产修复）
-- =====================================================================
-- [161] PROD_04 重复房间（空间分类 service_item）清理：同租户+同分类同名去重保留 1 条并改指 FK
--        ★背景：并发自愈初始化（selfHealCurrentTenantResources 判空→补录非原子、无锁）
--          导致 preset 房间双插，房间资源管理/房间布局配置/自助预约选房间同名×2；
--          代码侧已加 租户级初始化锁 + 名称级幂等 + 读侧去重防御 + 房间同名禁建
--        ★策略：重复组保留 1 条——①组内含被 JSON 引用 id（booking_order.remark /
--          promotion_activity.selected_room_ids）时保留 MIN(被引用 id)；②否则 MIN(id)；
--          简单 FK（booking_order.service_item_id / price_change_log.item_id /
--          room_seat_layout.room_id / resource_status_log.resource_id）改指保留 id；
--          被弃行逻辑删除（del_flag='2'）可回滚
--        ★幂等：临时表驱动，二次执行无重复组零变更
--        ★前置：[19]M15_04（service_category/service_item）+ [53]M15_23（space 分类/rooms 预设）
source PROD_04_duplicate_room_cleanup.sql;

-- =====================================================================
-- 验收检查（PROD_04 新增）
-- =====================================================================
-- 29) ★PROD_04 重复房间清理（[161]）
-- 29.1 剩余重复组 = 0
-- SELECT COUNT(*) AS dup_groups FROM (
--   SELECT 1 FROM service_item si
--     JOIN service_category sc ON sc.id = si.category_id AND sc.category_type = 'space'
--    WHERE si.del_flag = '0'
--    GROUP BY si.tenant_id, si.category_id, si.name
--   HAVING COUNT(*) > 1) t;
-- 预期：0
-- 29.2 房间资源管理 / 房间布局配置 / 自助预约选房间 / 活动选房间入口均无同名房间
-- 29.3 重启后端后并发打开「房间资源管理」+「房间布局配置」两页签，房间数不再增长

-- =====================================================================
-- 阶段六十九：M38/M39 PC 端经营报表菜单（日报/月报/活动明细）
-- =====================================================================
-- [162] M38M39_01 PC 端经营报表菜单 7395~7399：
--        ① 菜单 7395（经营报表 M 目录，parent=7100 小微版 PC 端）
--        ② 7396（日报 C，micro/operating/daily，micro:report:daily）
--        ③ 7397（月报 C，micro/operating/monthly，micro:report:operating）
--        ④ 7398（活动明细 C，micro/operating/activityDetail，micro:report:activityDetail）
--        ⑤ 7399（导出 CSV F，micro:report:export）
--        ★避让 H5 端 7380~7382（M38_01）+ 7390~7393（M39_01）
--        ★幂等：先删后插 + INSERT IGNORE，可重复执行
--        ★前置：[152]M36_01（activity_settlement 结算数据源）
--               + [154]M38_01（H5 端月报菜单 7380~7382）
--               + [155]M39_01（H5 端报表中心菜单 7390~7393）
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
source M38M39_01_operating_report_menus.sql;

-- =====================================================================
-- 阶段七十：M43_01 Boss PC 菜单树重构（v4.1 新菜单树）
-- =====================================================================
-- [163] M43_01 按用户设计的新菜单树重构 boss PC 端菜单结构：
--        ① 新建 4 个 M 目录：资源管理(7263)/员工管理(7264)/售后客诉(7265)/资金台账(7266)
--        ② 改名：7187→H5推广码 / 7189→业务员推广记录 / 7395→经营分析
--        ③ 提升为 7100 直属：7151活动管理 / 7180营销推广
--        ④ 删除 16 个废弃菜单（含 7141~7144 旧报表中心子页面）
--        ⑤ 补建 7070 员工联系方式公开页（M34_01 未执行，本脚本代建）
--        ⑥ role_template micro_pro/micro_lite boss 完整重写
--        ⑦ sys_role_menu 全量重建
--        ★不含图标（icon 全部置空）
--        ★幂等：先删后插 + UPDATE 天然幂等，可重复执行
--        ★前置：[162]M38M39_01（经营报表菜单 7395~7399，本脚本会删除并重建为“经营分析”）
--               + 全部 M15~M42 系列脚本
--        ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
source M43_01_boss_pc_menu_restructure.sql;

-- =====================================================================
-- 阶段七十一：PROD_fix 统一重建所有角色 sys_role_menu（生产修复）
-- =====================================================================
-- [164] PROD_fix_all_role_menus：
--        统一重建小微版所有角色（boss/salesman/customer × micro_pro/micro_lite）
--        的 sys_role_menu，从 role_template menu_keys 同步。
--        涵盖此前分散的三个修复：
--        ① 预约操作按钮权限（micro:booking:edit/cancel/complete）
--        ② 座位编排权限（micro:seat:assign）
--        ③ salesman 角色权限同步（micro:staff:schedule 等）
--        ★幂等：先删后插 + INSERT IGNORE，可重复执行
--        ★前置：[163]M43_01（boss 菜单树重构 + role_template 重写）
--        ★执行后请退出重新登录，刷新 Sa-Token 权限快照
--        ★后续权限修复直接追加到 PROD_fix_all_role_menus.sql 中
source PROD_fix_all_role_menus.sql;

-- =====================================================================
-- 阶段七十二：boss 角色菜单泄漏修复（7100 树外脏数据清理）
-- =====================================================================
-- [165] fix_boss_menu_leak：
--        修复 boss 角色 sys_role_menu 中 7100（小微运营）树之外的脏数据。
--        根因：[164] PROD_fix_all_role_menus.sql 步骤 1.3 只清理了 7000~7099
--              和 7500~7699 区间，漏掉了 menu_id=1（系统管理）和 menu_id=7400
--              （小程序·顾客）等不在这些区间内的根级菜单，导致 boss 侧边栏
--              出现系统管理/租户管理菜单/店铺配置/小程序·顾客等多余菜单。
--        策略：用递归 CTE 收集 7100 子树所有 menu_id，删除不在此集合的记录。
--        ★幂等：可重复执行
--        ★前置：[164] PROD_fix_all_role_menus.sql
--        ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source fix_boss_menu_leak.sql;

-- =====================================================================
-- 阶段七十三：业务员排班菜单恢复（micro:staff:schedule 权限链修复）
-- =====================================================================
-- [166] M44_01_restore_salesman_schedule_menu：
--        修复业务员(salesman)登录 H5 后 /micro/staff/schedule 与
--        /micro/staff/myActivities 报 403「无此权限：micro:staff:schedule」，
--        导致首页「我参与的活动（未结束的）」空白。
--        根因：M31_05 复用 menu_id 7300~7302（覆盖为 Boss PC 海报/游客信息菜单），
--              原 7301(micro:staff:schedule)/7302(micro:staff:scheduleDetail) 被抹除，
--              sys_menu 再无该 perms 行；role_template 仍引用该 perm，
--              导致 [164] PROD_fix 按 perms 匹配重建时匹配不到 → 业务员永远缺权限。
--        策略：用新空闲 menu_id 7236/7237 重建两个 client_type=mini 排班菜单，
--              授权给 micro/micro_pro 租户克隆 salesman + 共享预设 salesman（不含 micro_lite）。
--        ★幂等：先删后插（仅删 7236/7237）+ INSERT IGNORE，可重复执行
--        ★前置：[164] PROD_fix_all_role_menus.sql
--        ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M44_01_restore_salesman_schedule_menu.sql;

-- =====================================================================
-- 验收检查（阶段七十二）
-- =====================================================================
-- 32) ★boss 角色菜单泄漏修复（[165]）
-- 32.1 boss 角色根级菜单应仅剩 7100（小微运营）
-- SELECT rm.menu_id, m.menu_name
--   FROM sys_role_menu rm
--   JOIN sys_menu m ON rm.menu_id = m.menu_id
--   JOIN sys_role r ON rm.role_id = r.role_id
--  WHERE r.role_key = 'boss' AND r.del_flag = '0' AND r.tenant_id IS NOT NULL
--    AND m.menu_type = 'M' AND m.parent_id = 0
--  ORDER BY rm.menu_id;
-- 预期：仅 1 行，menu_id=7100, menu_name=小微运营
-- 32.2 boss 角色菜单总数应 = 7100 子树内菜单数
-- 32.3 7100 树外菜单数应为 0

-- =====================================================================
-- 阶段七十一 验收检查（续）
-- =====================================================================
-- 30) ★boss 角色权限完整（[164]）
-- 30.1 boss role_template 含 micro:booking:edit/cancel/complete + micro:seat:assign
-- SELECT JSON_CONTAINS(menu_keys, '"micro:booking:edit"') AS has_edit,
--        JSON_CONTAINS(menu_keys, '"micro:seat:assign"') AS has_seat
--   FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：两列均为 1
-- 30.2 预约列表页「标记已收款」「取消」「完成服务」按钮可见（非 ACTIVITY 订单）
-- 30.3 活动编辑弹窗选择大巴房间后「画布加人」区域可见

-- 31) ★salesman 角色权限同步（[164]）
-- 31.1 salesman 登录后 /getInfo 的 permissions 应含 micro:staff:schedule
-- 31.2 /micro/staff/schedule 和 /micro/staff/myActivities 接口不再报 403
