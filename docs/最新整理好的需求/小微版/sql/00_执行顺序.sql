-- =====================================================================
-- 小微版（MICRO）SQL 执行顺序索引 · v1.86
-- 日期：2026-10-17
-- v1.86 变更：新增 [106] M26_04_h5_qrcode_rename.sql（「微信推广码」改名为「H5推广码」）：
--             ① 菜单 7156 改名：微信推广码 → H5推广码，remark 更新为 H5 说明；
--             ② 子按钮菜单 7157/7158 文案同步更新（去掉"微信"前缀）；
--             ③ 前端 wechatQrcode/index.vue 改用 /micro/promo/code/myQrcode 接口（H5 二维码，与 app 端 boss 推广码一致）；
--             ★前置：[70]M19_09（菜单 7156~7158 已建立）
--             ★幂等：UPDATE 操作天然幂等，可重复执行
--             ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- v1.85 变更：新增 [105] M26_03_resource_price_pricing_type.sql（资源价格设置升级：人员定价支持 4 种计价模式）：
--             ① staff_pricing 表新增 pricing_type + extra_config 两列（计时/套餐/计量/一口价）；
--             ② 前端页面重新设计：去掉房间 Tab（已由房间/空间管理统一管理），人员定价改为 jjadmin 四档模式；
--             ③ 后端 Controller 去掉房间定价端点，只保留人员定价；
--             ★前置：[87]M21_03（staff_pricing 建表 + 资源价格设置菜单）
--             ★幂等：INFORMATION_SCHEMA 条件 ALTER + UPDATE WHERE 守卫，可重复执行
-- v1.84 变更：新增 [104] M26_02_sensitive_word_permission_fix.sql（租户敏感词 F 按钮权限落地）：
--             ① TenantSensitiveWordController 检查 micro:sensitiveWord:list/add/edit/remove/query/hitLog，
--               但菜单 7610 的 perms 只有 micro:compliance:config:sensitiveWord，权限键完全不匹配；
--             ② 补建 6 个 F 按钮菜单（7611~7616）+ role_template 追加 6 权限键 + 存量角色补录；
--             ★前置：[68]M18_13 + [103]M26_01
--             ★执行后请重启后端 + 退出重新登录刷新 Sa-Token 权限快照
-- v1.83 变更：新增 [103] M26_01_sensitive_word_activation.sql（敏感词功能激活与旧表数据补迁）：
--             ① 前端敏感词管理页已切换至新接口 /compliance/tenant/sensitiveWord/*（读写 sensitive_word 表），
--               但 FEAT_SENSITIVE_WORD 功能开关可能处于关闭态 + 旧表 sensitive_word_library 中
--               M18_11 执行后通过旧接口添加的词未迁移到新表，导致功能未生效；
--             ② 修复：幂等重assert FEAT_SENSITIVE_WORD 开关（status='0' + micro_enabled=1）+
--               补迁旧表尚未迁入的有效词（INSERT IGNORE + NOT EXISTS 幂等）+
--               补录 biz_bundle 业态包 feature_flags 确保含 FEAT_SENSITIVE_WORD（防升级拦截）；
--             ★前置：[54]M18_10 + [55]M18_11
--             ★幂等：UPDATE WHERE 条件守卫 + INSERT IGNORE + NOT JSON_CONTAINS，可重复执行
--             ★执行后请重启后端清除 SensitiveWordEngine 内存缓存
-- v1.82 变更：新增 [102] M25_02_fund_platform_menu_final_fix.sql（「资金平台管理菜单」终极兜底修复）：
--             ① 用户反馈执行完 00_执行顺序.sql 后 admin 侧边栏仍看不到 menu 1300「资金平台管理菜单」，
--               经实时核查数据库：1300 本体存在、role_menu (1,1300) 存在、14 子菜单齐全，但存在 3 处隐蔽缺陷；
--             ② R1 根因：menu 7260「投诉监管」path='' 空字符串（M25_01 遗留）→ 前端 vue-router
--               遇到 path='' 的 M 目录子路由时整棵 1300 子树渲染异常 → 侧边栏消失；
--             ③ R2 根因：menu 6160「合规管理」path='compliance2' → 命中前端 utils/feature.js
--               ROUTE_FEATURE_MAP['compliance']='FEE_COMPLIANCE_CUSTOM' → admin 无该特性时被过滤，
--               叠加 filterRoutesByFeature line 162「children=0 && Layout」判断可能级联移除 1300；
--             ④ R3 根因：M15_12 re-parent 到 1300 的 4 个既有菜单（6001/101/102/3130）未补
--               sys_role_menu (1, ...) 授权（尾部 INSERT 只覆盖 1300~1359 区段）；
--             ⑤ R4 隐患：menu 1300 icon='component' 非若依标准图标 key → svg-icon 渲染为空，视觉缺失；
--             修复：① 幂等重建 menu 1300（ON DUPLICATE KEY UPDATE，icon→money）；
--                   ② UPDATE menu 7260 path '' → 'complaint'；
--                   ③ UPDATE menu 6160 path 'compliance2' → 'regulatory'（完全避开 compliance 关键字）；
--                   ④ INSERT IGNORE role_id=1 对 1300 全部子菜单+孙菜单授权（含 6001/101/102/3130）；
--                   ⑤ 强制 1300 全部子菜单 visible='0'/status='0'/client_type='pc'；
--             ★前置：[27]M15_12 + [九-1]M10_01 + [九-2]M10_02 + [79]M20_09 + [101]M25_01
--             ★幂等：ON DUPLICATE KEY UPDATE + UPDATE WHERE 条件守卫 + INSERT IGNORE，可重复执行
--             ★执行后必做：① 重启 ruoyi-admin 后端（清 Sa-Token 快照）；② 浏览器 Ctrl+F5 硬刷新；
--                          ③ 退出重登 admin 账号触发 getRouters 重新拉取
-- v1.81 变更：新增 [97] M22_04_service_item_extra_config.sql（service_item 补 extra_config TEXT 列，
--             存储业态差异配置 JSON，T11 报名流程依赖；INFORMATION_SCHEMA 幂等 ALTER）；
--             新增 [98] M22_05_schedule_change_log_audit_fix.sql（schedule_change_log 补 BaseEntity 四审计字段
--             create_by/create_time/update_by/update_time，修复 Unknown column 运行时错误；幂等 ALTER）；
--             新增 [99] M22_06_activity_pricing_enhance.sql（活动费用与参与增强：promotion_activity 补 4 列
--             custom_price/auto_calculated_price/participant_count/is_locked + project_signup 补 pay_status +
--             菜单 7159 锁定活动 F 按钮 + role_template boss 追加 micro:promo:lock + activityPayReminderJob 定时任务）；
--             ★前置：[93]M22_01（staff_schedule 建表）+ [87]M21_03（staff_pricing）+ [26]M15_11（promotion_activity）
--             ★幂等：条件 ALTER + 先删后插 + JSON_CONTAINS 守卫，可重复执行
-- v1.80 变更：新增 [93] M22_01_staff_schedule.sql（T22.1/T22.6 员工排班管理建表+菜单+功能开关 FEAT_STAFF_SCHEDULE）：
--             ① 新建 staff_schedule（UNIQUE(tenant_id, staff_id, schedule_date, shift_type) 防重复排班）+ schedule_change_log（修正留痕）两表；
--             ② feature_flag 第 39 项 FEAT_STAFF_SCHEDULE（组 E，micro_enabled=1，micro_lite 不授权）+ biz_bundle 全业态包幂等补录；
--             ③ sys_menu 7230~7235（员工排班 C + 批量创建/发布/修正 3 F + 员工 my / 顾客 store 各 1 F，避让 7210~7213）；
--             ④ role_template micro/micro_pro 的 boss(+4)/salesman(my)/customer(store) 追加权限键 + 存量 sys_role_menu 补录；
--             ★前置：[25]M15_10 + M15_06 + [85]M21_01（参考模式）；★后置：[94]M22_02（staff_schedule 审计字段修复）依赖本表；
--             ★序号调整：原 [93]M22_02 → [94]，原 [94]M21_08 → [95]（M22_01 为 M22_02 前置，须先执行）；
--             ★幂等：DROP IF EXISTS 建表 + 先删后插 + NOT JSON_CONTAINS 守卫，可重复执行
-- v1.79 变更：新增 [95] M21_08_resource_inuse_job.sql（T21.8 资源状态自动流转定时任务）：
--             ① 补全 M21 资源三态中此前未落地的 reserved→in_use（服务开始）与 in_use→idle（结束）写入触发点；
--             ② 新增 resourceInUseFlowJob（每 10 分钟，@TenantJob(versions={"micro"})），按预约时间窗 +
--               订单状态推进助教/房间资源状态并写 resource_status_log；幂等（当前状态==目标状态则跳过）；
--             ③ 前置：[86]M21_02（resource_status 列 + resource_status_log 表）+ advanceResourceStatusByTime() + Handler Bean；
--             ★幂等：先删后插（按 executor_handler），可重复执行
-- v1.78 变更：新增 [94] M22_02_fix_audit_columns_bigint_to_varchar.sql（staff_pricing/staff_schedule 审计字段类型修复，M22）。
-- v1.77 变更：新增 [92] M21_07_self_booking_biz_bundle_fix.sql（FEAT_SELF_BOOKING 业态包能力开关补全）：
--             ① 根因（与 M20_05 FEAT_RATING 升级拦截同源）：三维交集 License ∩ feature_flag ∩ biz_bundle，
--               biz_bundle 三业态包 feature_flags 缺 M21_01 新增的 FEAT_SELF_BOOKING → applyBizFilter 第三维剔除 →
--               助教状态管理/房间状态管理/资源价格设置（类级 @RequireFeature("FEAT_SELF_BOOKING")）
--               报「小微版无此功能权限」（micro_pro 付费版不应拦截；后端实现齐备，纯数据缺口）；
--             ② 修复：biz_bundle 全部业态包幂等补录 FEAT_SELF_BOOKING + 防御性重补 M20_05 四个小微开关；
--             ③ feature_flag FEAT_SELF_BOOKING 先删后插兜底（防 M21_01 漏执行）；
--             ★幂等：先删后插 + NOT JSON_CONTAINS 守卫，可重复执行
--             ★执行后请重启后端（清除 BizBundle/FeatureFlag/License 三层内存缓存）
-- v1.76 变更：新增 [91] fix_boss_remove_tenant_admin_menu.sql（boss 角色误关联「租户管理菜单」+ sys_user_role 双表不同步修复）：
--             ① 部分小微版租户 boss 克隆角色仍持有 platform_admin 专属菜单 7500~7509（「租户管理菜单」目录），
--                导致 boss 登录后侧边栏同时出现「租户管理菜单」和「小微运营」，违反五角色菜单隔离原则；
--             ② 根因：assign() 此前只写 user_tenant_role，但登录时 SysPermissionService.getRolePermission()
--                通过 sys_user_role → sys_role 读取角色权限，两表不一致导致用户拿到错误角色的菜单；
--             修复：① 从 boss 克隆角色的 sys_role_menu 中移除 7500~7509；
--                   ② 按 user_tenant_role 的实际分配重写 sys_user_role 关联（存量数据修复）；
--                   ③ Java 修复 UserTenantRoleServiceImpl.assign() 新增 syncSysUserRole()（防止复发）；
--             ★幂等：DELETE + INSERT 幂等，可重复执行
-- v1.75 变更：新增 [89] M21_05_boss_oss_permission_fix.sql（存量 boss OSS 权限补录）：
--             部分存量小微版租户（如 AIA桌游店 micro_pro）的 boss 角色 sys_role_menu
--             缺少 1130/1131/1133（OSS 查询/上传/删除），导致发布活动上传图片报 403；
--             根因：租户创建时 role_template 尚未含 OSS 权限，后续模板更新未同步存量 sys_role_menu；
--             修复：为所有小微版 boss 补录 1130/1131/1133（F 类型按钮，不污染侧边栏）。
--             ★幂等：INSERT IGNORE + NOT EXISTS，可重复执行
-- v1.74 变更：新增 [88] M21_04_fix_audit_columns.sql（BaseEntity 审计字段补全，M21，v1.74 修复）：
--             ① 新建 staff_pricing 表（tenant_id/user_id/hourly_rate DECIMAL(10,2)/enabled/remark）
--                UNIQUE(tenant_id, user_id)，存储助教小时单价；
--             ② sys_menu 新增 7207~7209（资源价格设置 C+F+F，client_type=pc，挂 7100 小微运营）；
--             ③ role_template micro_pro + micro_lite boss 追加 3 权限键（micro:resourcePrice:view/staff:set/room:set）；
--                存量 boss 角色 sys_role_menu 补录。
--             ★房间价格复用 service_item.price（已有字段，通过 IServiceItemService.changePrice 管理），无需新建表。
-- v1.72 变更：新增 [85] M21_01_self_booking.sql（T21.1~T21.4 自助预约菜单+功能开关 FEAT_SELF_BOOKING）：
--             ① feature_flag 新增第 38 项 FEAT_SELF_BOOKING（组 E，micro_enabled=1，仅小微版 micro_pro 启用）；
--             ② sys_menu 新增 7200~7202（自助预约 C+F+F，client_type=mini，挂 7400 顾客小程序）；
--             ③ role_template micro_pro customer 追加 3 权限键；存量 customer 角色 sys_role_menu 补录。
--             新增 [86] M21_02_resource_status.sql（T21.5~T21.6 资源状态流转+Boss PC 状态管理）：
--             ① service_item 补 resource_status 列（idle/reserved/in_use 三态）；
--             ② 新建 resource_status_log 表（资源状态变更日志）；
--             ③ sys_menu 新增 7203~7206（助教/房间状态管理 C+C+F+F，client_type=pc，挂 7100 小微运营）；
--             ④ role_template boss 追加 4 权限键；存量 boss 角色 sys_role_menu 补录。
-- v1.71 变更：新增 [84] M20_14_customer_role_permission_fix.sql（顾客小程序权限 403 修复）：
--             根因：①顾客角色(9207)在 sys_role 中存在但 sys_role_menu 无任何记录；
--                   ②registerByPhone() 自动注册顾客用户时未分配 sys_user_role；
--                   ③selectMenuPermsByUserId 权限链 sys_user_role→sys_role_menu→sys_menu 双层断裂，
--                   @SaCheckPermission("micro:customer:shop") 校验必失败（403）。
--             修复：①为顾客角色(9207)补录 sys_role_menu（取 micro_pro customer 权限集 + M 祖先目录 7400）；
--                   ②为无 sys_user_role 的小微版顾客用户补录角色关联；
--                   ③【Java】registerByPhone() 自动注册时自动分配 customer 角色(9207)。
--             ★执行后请退出重新登录，刷新 Sa-Token 权限快照。
-- v1.70 变更：新增 [83] M20_13_activity_end_job.sql（活动自动结束定时任务）：
--             根因：活动 endTime 过期后无自动机制将 status 从 PUBLISHED 改为 ENDED，
--                   导致已结束的活动仍出现在小程序「最新活动」浏览页。
--             修复：① listPublished 增加 endTime 过滤（浏览页不展示 endTime 已过的活动）；
--                   ② 新增 activityEndJob 定时任务（每 6 小时），将 endTime 已过且 PUBLISHED 的活动置为 ENDED；
--                   ③ 仅处理 PUBLISHED 状态，Boss 手动取消（DRAFT）或已结束（ENDED）的活动不受影响。
-- v1.69 变更：新增 [82] M20_12_biz_bundle_fund_control_menus_fix.sql（业态包菜单声明修复）：
--             根因：biz_bundle.fund_control.menus 包含企业版旧 ID [2000,3000,3201,...]，
--                   与小微版 7100+ 菜单无交集 → applyBizFilter 过滤异常 → menus 仅返回极少量菜单。
--             修复：fund_control 是小微版默认业态包，menus 置 NULL 跳过业态过滤（兼容存量设计）。
--             ★执行后需重启后端清除 BizBundleService 内存缓存 menuCache。
-- v1.68 变更：新增 [81] M20_11_tenant_version_type_null_fix.sql（小微版租户 version_type 为 NULL 修复）：
--             根因：部分小微版租户 sys_tenant.version_type 为 NULL（创建时未正确回填），导致：
--               ① 登录响应不含 versionType → 前端 isPro=false → 经营驾驶舱不渲染；
--               ② copyMenuTemplateForMicro 查 role_template 不匹配 → sys_role_menu 缺 M/C 菜单 → menus=[]。
--             修复：批量补设 version_type='micro_pro' + role_template 配方补录 sys_role_menu + M 祖先目录补全。
-- v1.67 变更：新增 [80] M20_10_exception_resolve_permission.sql（异常处理中心「处理」按钮权限补全）：
--             ① 根因：@SaCheckPermission(micro:exception:resolve) 在 sys_menu 无对应 F 按钮菜单行
--               （仅 7698 C 菜单承载 micro:exception:list），role_template 亦无此键，
--               权限串永远无法落地为菜单授权 → PUT /micro/exceptionCenter/resolve 必 403；
--             ② 新增 F 按钮菜单 7699（异常处理，parent=7698，perms=micro:exception:resolve，侧边栏不渲染）；
--             ③ role_template 对已含 micro:exception:list 的模板幂等追加 micro:exception:resolve；
--             ④ 超管 role_id=1 + 存量已授权 7698 的角色补录 sys_role_menu；
--             ⑤ 前端无需改动（处理按钮无 v-hasPermi 拦截，F 类型不污染侧边栏）。
-- v1.66 变更：新增 [79] M20_09_pc_menu_flatten_single_child.sql（V3.7 PC 菜单拍平·保留一层）：
--             目录下仅一个可见子菜单时提升到上级，消除「目录 → 唯一子菜单」无意义点击层，共 5 个：
--             ① 6000 租户管理→6001 提至 1300(path=tenant)，删空目录 6000；
--             ② 7020 业务角色管理→7021 提至 7010(path=tenantRole)，删空目录 7020；
--             ③ 7050 基础数据配置→7058 提至 7010(path=shop)，7050 隐藏保留（含隐藏子页 7051/7055）；
--             ④ 7110 预约/订单管理→7111 提至 7100(path=bookingList)，7110 隐藏保留（含隐藏子页 7116）；
--             ⑤ 7195 固定支出管理→7196 提至 7190(path=cost)，F 按钮 7197~7199 改挂 7196，删空目录 7195。
--             ★前端联动：7111 路由 /microOps/booking/list→/microOps/bookingList，已同步更新 ruoyi-ui 4 处硬编码引用
--               （router/index.js activeMenu、micro/dashboard/index.vue 两处快捷入口、micro/order/detail.vue 返回按钮）；
--             ★role_template 不受影响：被删/隐藏 M 目录 perms 均为 NULL，menu_keys 按 perms 字符串授权。
-- v1.65 变更：新增 [78] M20_08_voice_compliance_result.sql（T18 增强语音合规记录补 result 列 + 触发校验流程）：
--             ① voice_compliance_hit 补 VARCHAR(10) 列 result（默认 PASS，存量数据更新为 FAIL）；
--             ② VoiceComplianceHit/VoiceComplianceHitVo 实体补 result 字段；
--             ③ IVoiceAsrProvider 接口 + StubVoiceAsrProvider 桩实现（V1 返回固定测试文本）；
--             ④ IVoiceComplianceService 新增 checkVoiceCompliance() 方法（ASR 转写 + 敏感词检测 + 写记录）；
--             ⑤ MicroActivitySettingServiceImpl.saveSetting() 保存语音前触发合规校验，违规则打回重录。
-- v1.64 变更：新增 [77] M20_07_voice_compliance_max_duration.sql（T18 增强语音合规规则补 max_voice_duration 列）：
--             ① voice_compliance_rule 补 INT 列 max_voice_duration（默认 180 秒=3 分钟，0=不限）；
--             ② 后端 voiceEnabled 接口扩展返回 { enabled, maxVoiceDuration }；
--             ③ 前端合规配置页规则弹窗新增“最大语音时长”输入框（分钟）；
--             ④ 前端 promo/index.vue + activity/setting/index.vue 上传前用 Web Audio API 校验时长不超限。
-- v1.63 变更：新增 [75] M20_05_micro_pro_feature_bundle_and_menu_fix.sql（业态包能力开关补全 + 推广分销/固定支出菜单重构）：
--             ① 【根因·升级拦截】biz_bundle 三业态包(boardgame/fresh_food/fund_control) feature_flags 幂等补齐
--               FEAT_PROMOTION_DISTRIBUTION/FEAT_RATING/FEAT_SENSITIVE_WORD/FEAT_IMAGE_COMPLIANCE 四个小微版开关，
--               修复 applyBizFilter 第三维剔除导致 @RequireFeature 抛「当前版本不支持此功能，请升级License」（micro_pro 付费版不应再有升级拦截）；
--             ② 菜单 7101「经营概览」改名「经营驾驶舱」（V3.7 §三 老板首页）；
--             ③ 推广分销子树重构对齐 V3.7「推广管理」：7180 改回 M 目录，7181→推广数据看板(index)，
--               7182→业务员奖励记录(salesman/records)，7183→顾客奖励记录(customer/records)，7184 拉新奖励配置归位；
--               删除死链菜单 7186(firstOrder)/7187(code)/7188(toggleCode)——组件不存在；
--             ④ 重建固定支出管理菜单 7195~7199（挂 7190 数据与报表，M15_21 定义但库中缺失、M20_04 补录空转）；
--             ⑤ role_template micro_pro boss menu_keys 移除死链键(data/attribution/firstOrderConfig/code)、
--               新增 salesman/customer，39→37 权限键；存量 micro_pro boss sys_role_menu 补录新菜单 + 通用 F 按钮补授权；
--             ⑥ 【前端】distribution/index.vue 两张卡片跳转修正为 /microOps/distribution/salesmanRecords|customerRecords（原为死链 /micro/distribution/data）。
-- v1.62 变更：新增 [74] M20_04_micro_pro_boss_permission_and_menu_fix.sql（micro_pro boss 权限补全 + 推广分销菜单类型修正）：
--             ① role_template micro_pro boss 追加 8 个缺失权限键（活动 CRUD 4 个 + 固定支出 4 个），31→39 权限键；
--             ② 菜单 7180 从 M 目录改为 C 类型（component=micro/distribution/index，推广分销概览页可直接导航）；
--             ③ 存量 micro_pro 租户 boss sys_role_menu 补录活动按钮(7152~7155) + 固定支出(7195~7199) + 7180(C 类型)；
--             ④ 【Java】LicenseServiceImpl.getTenantActiveFeatures() 增加 sys_tenant.version_type 回退逻辑，
--               小微版租户（micro_lite/micro_pro）即使无 License 或 edition 不匹配也使用 MICRO_FEATURES，
--               修复评价管理 @RequireFeature("FEAT_RATING") 报 license 错误。
-- v1.61 变更：新增 [73] M20_03_remove_my_promo_menu_and_shared_permissions.sql（移除「我的推广码」菜单 + 公用功能去除权限校验）：
--             ① 删除菜单 7159（我的推广码）+ role_template 移除 micro:myPromo:view；
--             ② 【Java】MicroActivitySettingController 全部端点移除 @SaCheckPermission（微信推广码/选择器公用）；
--             ③ 【Java】MicroRewardConfigController GET/PUT 移除 @SaCheckPermission（拉新奖励配置公用）；
--             ④ 【Java】MicroPromotionCodeController generate/my 移除 SaMode.OR（菜单已删，仅保留功能开关）。
-- v1.60 变更：新增 [72] M20_02_booking_page_permission_and_menu_fix.sql（预约列表页权限 403 + 订单详情菜单隐藏）：
--             ① 【Java】ServiceItemController.onSale() → SaMode.OR(micro:service:list, micro:booking:list)；
--             ② 【Java】UserRoleController 新增 /staff 端点 → SaMode.OR(micro:booking:list, micro:settle:list)；
--             ③ 【前端】booking/settle/history 页面改调 listStaff 替代 listUserRole；
--             ④ 【SQL】sys_menu 7116(订单详情) visible '0'→'1'（隐藏，仅 router.push 访问）。
-- v1.59 变更：新增 [71] M20_01_boss_pro_menu_fix.sql（micro_pro boss 角色菜单 V3.7 §6.5 完整对齐）：
--             ① 补建缺失菜单 7159(我的推广码)/7650(员工业绩榜)/7698(异常处理中心)；
--             ② 新建「数据与报表」M 目录(7190)，收纳评价管理/顾客管理/员工业绩榜/语音合规记录/合规命中记录 + 收支记录/报表中心降级为子目录；
--             ③ role_template micro_pro boss 31 权限键重写（对齐 V3.7 完整菜单骨架）；
--             ④ sys_role_menu 清理+重授（含 8 个 M 类型目录显式补入，修复此前 M 目录完全缺失导致侧边栏无法展示树形结构的问题）。
-- v1.58 变更：新增 [70] M19_09_wechat_qrcode_rename.sql（「活动设置」改名为「微信推广码」）：
--             ① sys_menu 7156 改名：活动设置 → 微信推广码，component 改为 micro/activity/wechatQrcode/index；
--             ② 权限 perms 改名：micro:activity:setting:* → micro:wechat:qrcode:*；
--             ③ role_template boss 权限同步更新；
--             ④ 前端新增 wechatQrcode/index.vue（活动选择 + 二维码展示 + 手机扫码说明）；
--             ⑤ 后端 MicroActivitySettingController 权限注解同步更新。
-- v1.57 变更：M18_13_voice_compliance_menu.sql 扁平化（v1.0→v1.1）：
--             ① 租户管理员「语音与合规配置」(7610) 从 M 目录+3 子菜单扁平化为 C 类型单菜单
--                （内含合规规则/录音留存/敏感词三个 Tab，组件 micro/compliance/config/index）；
--             ② 清理同级重复的「敏感词管理」(7680~7683，M18_12 规划但未交付)；
--             ③ role_template platform_admin 权限从 3 key 精简为 1 key（micro:compliance:config:rule）；
--                ★菜单 7610 perms 补齐三 Tab 对应全部权限（rule/policy/sensitiveWord 逗号分隔），
--                修复录音留存/敏感词 Tab API 403 问题；
--             ④ M18_15 同步更新：去掉 7680~7683 补录 + 去掉 7610 M 类型目录手动补录 +
--                删除 5.2 孤立 micro:sensitiveWord:* 权限段（对应菜单已删）。
-- v1.56 变更：新增 [69] M19_08_boss_my_promo_qrcode_menu.sql（Boss PC「我的推广码」菜单）：
--             boss PC「推广发布」目录下新增「我的推广码」页面（menu_id 7159，C 类型，client_type=pc），
--             展示门店/个人推广二维码供客户手机扫描进入微信小程序；
--             新增 perms micro:myPromo:view，role_template micro/micro_pro/micro_lite boss 均追加；
--             存量租户 boss sys_role_menu 补录 7159 + M 祖先目录；
--             后端 MicroPromotionCodeController generate/my 端点放宽为 SaMode.OR 兼容免费版 boss。
--             ★免费版和付费版均需加上此菜单。
--             租户敏感词管理 7680~7683 + 老板合规命中记录 7690 + role_template 三角色追加）；
--             ② 新增 [56.7] M18_15_m18_final_role_template_patch.sql（T18.20 M18 最终汇总补丁）：
--             修复 M18_08 缺陷（role_key='micro_pro' 不存在→'boss'）+
--             role_template admin/platform_admin/boss 三角色 M18 权限完整性兜底 +
--             sys_role_menu 存量租户全量补录（7600~7699 + 1370~1379 全覆盖）。
-- v1.54 变更：新增 [56.6] M18_14_system_setting_menu.sql（T18.11 租户管理员系统设置菜单注册）：
--             T18.11 后端（MicroSystemSettingController）+ 前端（systemSetting/index.vue）已交付，
--             唯一缺口=sys_menu 入口未注册 + role_template 未更新。
--             本脚本使用 7695~7697 区段（避让 7680~7690 合规/敏感词区段）：
--             系统设置页面(7695, micro:systemSetting:list) + 企业信息保存按钮(7696, micro:systemSetting:edit)；
--             role_template platform_admin 追加 micro:systemSetting:list/edit + sys_role_menu 存量补录。
-- v1.53 变更：新增 [56.5] M18_13_voice_compliance_menu.sql（T18.1~T18.4 语音合规全链路菜单注册）：
--             语音合规 BE+API+Vue 已由 M18_01+早期开发交付，唯一缺口=sys_menu 入口未注册。
--             本脚本使用 7600~7619 预留区段：Admin 语音合规(7600~7604 挂 1370)+
--             租户管理员配置(7610 单 C 菜单挂 7500，内含三 Tab)+ 老板只读记录(7618 挂 7100)；
--             perms 与 Controller @SaCheckPermission 严格对齐；
--             role_template boss/platform_admin 追加新权限 + sys_role_menu 存量补录。
-- v1.52 变更：新增 [68] M19_07_activity_setting_enhance.sql（T19.1~T19.5 活动设置增强数据模型）：
--             依据《活动功能需求草稿》v0.1，promotion_activity 补 8 列
--             （auto_title/bg_image_url/voice_url/qrcode_url/recommend_scene/
--              selected_project_id/selected_room_ids/selected_staff_ids）；
--             活动设置菜单 7156~7158（C+F+F，挂 7150 活动管理目录下）；
--             role_template boss 追加 micro:activity:setting:list/edit 权限；
--             存量租户 boss sys_role_menu 补录 7156~7158 + M 祖先目录。
--             ★背景图仅对当前活动生效，不跨活动复用（对齐需求草稿 §3.6）。
-- v1.51 变更：新增 [67] M19_06_boss_oss_query_remove_permission.sql（boss OSS 图片回显/删除权限补全）：
--             问题：boss 打开「活动管理」时前端 ImageUpload/promo resolveOssIds() 调 GET /system/oss/listByIds 回显图片，
--                   报 403「无此权限：system:oss:list」；删除图片调 DELETE /system/oss/{id} 需 system:oss:remove。
--             根因：system:oss:list 只挂在 C 类型菜单 118(文件管理) 上，授予 boss 会渲染出「系统管理」，违反菜单隔离。
--             修复：① 【Java】SysOssController.listByIds 放宽为 SaMode.OR(system:oss:list, system:oss:query)，兼容 admin/企业版；
--                   ② role_template micro/micro_pro/micro_lite boss 追加 system:oss:query + system:oss:remove；
--                   ③ 所有小微版 boss sys_role_menu 补录 1130(文件查询,F) + 1133(文件删除,F)——均 F 类型不污染侧边栏。
-- v1.50 变更：★根治「系统管理」菜单反复出现（micro_lite 免费版 boss 侧边栏仅经营概览 + 活动管理）：
--             ① 【Java 根因修复】SysTenantServiceImpl.copyMenuTemplateForMicro：角色重建（clonePresetRoles /
--                reCopyMenuTemplateForMicro）补全 M 祖先目录时跳过 F 类型菜单——此前会为 1131(文件上传,F,
--                perms=system:oss:upload) 沿 1131→118→1 补出顶层目录「系统管理」(menu_id=1) 并拖入 boss 的
--                sys_role_menu，导致免费版老板侧边栏反复冒出「系统管理」；
--             ② 【SQL 修订】[63]M19_02 / [64]M19_03 / [66]M19_05 统一改为「只补/保留 1131(F 按钮，仅承载权限、
--                不在侧边栏渲染)，删除 1(系统管理,M) / 118(文件管理,C)」；M19_05 role_template 覆盖 micro/micro_pro/micro_lite。
--             ★关键原理：selectMenuTreeByUserId 只渲染 M/C 类型（侧边栏），selectMenuPermsByUserId 只需菜单本身携带
--               perms 即可放行——故补 F 类型 1131 既能上传又不污染侧边栏，父级 1/118 完全不需要。
-- v1.49 变更：新增 [66] M19_05_boss_oss_upload_role_template_fix.sql（boss OSS 上传权限彻底修复）：
--             ① role_template micro/micro_pro/micro_lite boss menu_keys 追加 system:oss:upload（修复根因：M19_02 仅改 sys_role_menu 未改模板源头）；
--             ② 所有小微版 boss（含 micro_lite）sys_role_menu 只补 1131(F 按钮) + 删除误补的 1/118（v1.50 修订，原为补录 1/118/1131）。
-- v1.48 变更：新增 [65] M19_04_micro_lite_boss_promo_crud_permission.sql（micro_lite 免费版 boss 活动增删改查权限补全）：
--             ① role_template micro_lite boss menu_keys 追加 micro:promo:add/edit/remove/poster；
--             ② 存量小微版免费版租户 boss 克隆角色补录 sys_role_menu（7152/7153/7154/7155）。
-- v1.47 变更：新增 [64] M19_03_micro_lite_boss_menu_simplify.sql（micro_lite 免费版 boss 菜单精简）：
--             ① role_template micro_lite boss menu_keys 精简为仅含 micro:dashboard:view + micro:promo:list；
--             ② 存量小微版免费版租户 boss 克隆角色清理 sys_role_menu（删除系统管理 1/118/1131 + 7000~7499 非目标菜单，
--                重新插入 7101 经营概览 + 7151 活动管理 + M 祖先目录）；
--             ③ 免费版 boss 侧边栏仅保留「经营概览」和「活动管理」，无系统管理，首页显示经营概览。
-- v1.45 变更：新增 [62] M19_01_micro_lite_boss_promotion_menu.sql（micro_lite 免费版 boss 推广管理菜单授权）：
--             依据 V3.7 §八 免费版老板菜单范围：推广管理（拉新奖励配置+推广数据看板）+ 活动管理（活动列表）；
--             修复：role_template micro_lite boss 追加 micro:distribution:rewardConfig + micro:distribution:*；
--             存量小微版免费版租户 boss 克隆角色补写 sys_role_menu（递归 CTE 补 M 祖先目录）。
-- v1.42 变更：新增 [59] M15_26_boss_role_menu_cleanup.sql（boss 克隆角色菜单清理）：
--             ① 删除 boss 克隆角色中残留的 platform_admin 菜单关联（7500~7519/7620~7639）；
--             ② 确保 boss 克隆角色拥有 M 类型祖先目录 7100（小微运营）；
--             ③ 配合 index.vue 增加 isBoss 分支，boss 登录后显示经营驾驶舱首页。
-- v1.41 变更：新增 [58] M15_25_boss_lite_dashboard_fix.sql（micro_lite 老板首页修复）：
--             ① role_template micro_lite boss 追加 micro:dashboard:view（经营概览首页）；
--             ② 存量小微版免费版租户 boss 克隆角色补写 sys_role_menu（7101 经营概览 + 7100 小微运营目录）；
--             ③ PC 前端 micro/dashboard/index.vue 适配：lite 档显示简化首页（推广统计+升级提示），pro 档保持完整经营驾驶舱。
-- v1.40 变更：新增 [57] M15_24_service_item_capacity.sql（T15.38 修复）：
--             ① service_item 补 capacity 列（INT，可容纳人数，房间/空间用，项目为 NULL）；
--             ② 存量房间数据从 description「可容纳：X-Y人」中提取最大人数回填 capacity（幂等）；
--             ③ 代码侧 IndustryTemplateServiceImpl.initTenantResources 房间创建时解析 capacity 数字写入；
--             ④ 修复 service_item.category_id NOT NULL 约束导致项目初始化失败（categoryId 哨兵值 0L）。
-- v1.39 变更：新增 [54] M18_10 / [55] M18_11 / [56] M18_12
-- 依据：《小微版完整方案》v10.5（增量 v10.1~v10.5）、《推广分销与激励方案》v1.0、
--       《多业态SaaS平台需求规格说明书》v1.0、
--       《资金管控系统开发任务文档》v1.31（M15 里程碑 T15.1~T15.37 + M16 里程碑 T16.1~T16.12）
--       + v1.24（M17 里程碑 T17.1~T17.5 通用评价与点赞体系 / T15.33~T15.34 行业模板种子数据）
--       + v1.29（M18 里程碑 T18.1/T18.9/T18.10/T18.12~T18.15/T18.20 V3.7 菜单骨架缺口补齐）
-- v1.39 变更：新增 [54] M18_10 / [55] M18_11 / [56] M18_12（v1.27 M18 图片合规与全局敏感词，全版本通用·平台级合规底线，任务总数不变）：
--             ① M18_10 建 sensitive_word（全局底库 tenant_id=0 + 租户自定义，UNIQUE(tenant_id,word,category)）/ sensitive_word_hit_log（三路 text/voice_ocr/image_ocr 命中留痕）/ image_compliance_config（porn/violence/politics 三类 × OCR+视觉两路开关，tenant_id=0 全局默认三行）三表；
--             ② feature_flag 追加第 36/37 项 FEAT_IMAGE_COMPLIANCE / FEAT_SENSITIVE_WORD（组 F=平台合规底线，basic/advanced/pro/micro 四列均 enabled=1，开关总数 35→37）；
--             ③ M18_11 依 V3.8 附录 A 八大类 INSERT IGNORE 幂等导入约 200 条全局底库（POLITICAL/AD/PORN/VIOLENCE/SUPERSTITION/ABUSE/RUMOR/DIALECT）+ 旧表 sensitive_word_library→sensitive_word 幂等迁移（不动旧表原数据）；
--             ④ M18_12 Admin 平台合规菜单 1370~1380（compliance:sensitiveWord/imageConfig/alert）+ 租户敏感词管理 7680~7686（micro:sensitiveWord:*）+ 老板合规命中记录 7690（micro:boss:complianceHit:list）+ role_template boss/platform_admin 追加。
-- v1.38 变更：新增 [53] M15_23_atomic_resource_project_room.sql（T15.38 platform_admin 原子资源管理·项目管理 + 房间/空间管理）：
--             ① service_category 补 category_type 列（service 默认 / space 房间·空间分类），房间=space 分类下的 service_item（零新表）；
--             ② industry_template.preset_config 补 rooms[] 预设（23 行业各 2~3 间，条件 JSON_SET 仅补空，兑现 T15.33「每行业 ≥2 房间」验收）；
--             ③ 菜单「原子资源管理」目录 7510 + 项目管理 7511~7513 + 房间/空间管理 7514~7516，7504(角色映射)/7507(人员管理) re-parent 到 7510；
--             ④ role_template micro_lite/micro_pro platform_admin 追加 6 perms（micro:platform:project[:query/:edit] + micro:platform:room[:query/:edit]）；
--             ⑤ 数据自愈：与角色映射同模式，租户首次进入/建租户时从 admin 行业模板 preset_config.projects/rooms 初始化落库（幂等仅空表补录，V3.5 §16.4 脱钩）。
-- v1.37 变更：新增 [52] M18_09_platform_admin_role_menu_fix.sql（platform_admin 克隆角色 + 用户角色分配 + 完整菜单授权修复）：
--             ① 为缺少 platform_admin 克隆角色的小微版租户补建克隆角色（tenant_id*10+5 生成唯一 role_id）；
--             ② 为无角色的 platform_admin 用户补建 sys_user_role 关联（解决登录后无任何菜单的根因）；
--             ③ 为所有 platform_admin 克隆角色补全完整 sys_role_menu
--                （含 M 类型目录 7500/7620 手动补录 + C/F 类型 7501~7508 role_template 匹配 + M18 菜单 7621~7628/7630）；
--             ④ 更新 role_template 补入 M18 新权限（micro:tagConfig:list/micro:periodRule:list/micro:bookingPolicy:list）。
-- v1.36 变更：新增 [51] _fix_platform_admin_menu_v136.sql（紧急修复 platform_admin 登录无菜单 + 权限隔离修正）：
--             ① role_template 移除 admin 级别 perms（system:tenant:list 等），仅保留 micro:platform:*（与 admin 完全隔离）；
--             ② 补录 M 类型目录 7500 到 sys_role_menu（perms 为空不会被 menu_keys→perms 匹配，
--                导致 selectMenuTreeByUserId 找不到根节点，子菜单 7501~7508 成为孤儿，侧边栏空白）；
--             ③ 清理存量 platform_admin 克隆角色中错误的 admin 菜单关联 + 预设角色 9205 同步修正；
--             ④ 补录 M18 新增菜单（7620~7639）到 platform_admin 克隆角色。
--             配套代码修复：BizCopyController.java /map /map-all /langs 三个 i18n 只读端点
--             移除 @SaCheckPermission("system:bizCopy:query")（登录时 GetInfo→bizCopy/switchBizCode 触发，
--             platform_admin 无此权限导致报错）。
-- v1.35 变更：新增 [38.5] M15_22 行业模板种子数据（T15.33，V3.5 §16 + 初始化项目清单_上海.xlsx，
--             23 个二级行业 × 132 个真实服务项目 × 69 条 industry_role_map；
--             INSERT IGNORE + UPDATE 双重幂等；market 停用不删除保留兼容既有租户；
--             修复「租户新增时可选行业仅 4 个」问题，四大类分布：休闲娱乐 6 / 生活服务 7 /
--             运动健身 7 / 专业工作室 3；preset_config 含 category/service_categories/settle_rule/
--             units/projects 五段结构，projects 每行业取 Excel 前 5~7 条大众点评上海公开团购数据）
-- 口径：小微版 = 面向个体门店/小微商户的轻量经营 SaaS（桌游店/买菜生鲜/健身房/汤泉美容）；
--       手机号即身份、一店一租户、老板 + 业务员 + 顾客三视图，PC 管理端 + 微信小程序双端；
--       ★V1 零在线支付合规：仅「到店付」，无在线收款/无支付分账/无顾客在线付款菜单。
--       ★v1.14 新增：小微版分档（micro_lite 免费·仅推广 / micro_pro 收费·全经营）+
--       角色可见性硬隔离（tenant_type_scope）+ 资金平台管理菜单收纳（menu_id 1300~1359）+
--       版本档位管理（system:edition:*）+ lite 档活动浏览页（micro:activity:browse）+
--       防爽约/黑名单机制（customer_credit/no_show_record/noShowCheckJob）。
--       ★v1.14 前端交付配套（M15_14）：ruoyi-ui 小微版 PC 页面交付后三方核对
--       （后端 @SaCheckPermission ↔ sys_menu.perms ↔ sys_menu.component ↔ .vue 落盘路径），
--       补建「页面装修」菜单（micro:page:*，此前完全无入口）+ 修正 5 个指向不存在 .vue 的菜单
--       + 收敛版本档位管理写按钮（后端为只读聚合端点）。
--       ★v1.16 新增（M16 推广分销与激励，仅小微版）：推广码（门店码+个人码，一人一码永久有效）+
--       扫码归因（谁最后扫码业绩归谁，交接点锁定不回溯）+ 拉新奖励（首消触发仅一次，员工现金/顾客优惠券，
--       系统自动发放，V1 零支付走「标记已线下发放」）+ 首单识别（手机号唯一维度，按活动独立判断，与归属解耦）+
--       功能开关 FEAT_PROMOTION_DISTRIBUTION（basic/advanced/pro 均 disabled，仅小微版启用，四校验点统一走开关）。
--       ★M16 为小微版专属，基础版/高级版/专业版不部署、不开放入口（母目录 docs/newrequest/sql 同步留档）。
--       ★v1.24 新增（M17 通用评价与点赞体系，仅小微版付费版 micro_pro）：
--       评价表 user_rating（1~5 星 + 内容 + 图片 + 场景 + 回复 + 屏蔽）+ 点赞表 user_like（toggle 切换）
--       + 评价管理菜单 7160~7164（micro:rating:*，老板 PC）+ 功能开关 FEAT_RATING（第 35 项，组 E）
--       ★M17 仅 micro_pro 启用；micro_lite/basic/advanced/pro 均不生效；全部评价接口走 @RequireFeature("FEAT_RATING")
--       ★M15_15 存量修复：clonePresetRoles 此前未适配小微版 role_template 路径，
--       致小微版租户克隆角色 sys_role_menu=0，租户管理员登录无菜单无角色。
--       ★M15_16 platform_admin 角色模板补全：role_template 补 micro_lite/micro_pro 两档
--       platform_admin 配方（含 system:license:manage 上传 License 升级版本），
--       License管理(3130) re-parent 到资金平台管理(1300)，存量租户 sys_role_menu 补写。
--       ★v1.30 新增（T15.33~T15.37，M15_17~M15_21）：
--       T15.33 platform_admin 租户管理员菜单三件套（版本升级/角色映射/人员管理，menu_id 7500~7509，
--         与 admin 的 1300 资金平台管理完全隔离）；
--       T15.34 推广分销按渠道拆分（业务员推广+顾客推广，奖励记录按 reward_channel 拆为两独立页面，
--         新增菜单 7189/7190）；
--       T15.35 活动图片上传+背景主题（promotion_activity 补 logo_url/theme_key 两列，4 套预设主题）；
--       T15.36 小程序全局主题+首页欢迎语（sys_tenant 补 app_theme_key，主题下发链路 PC→tenant_config→小程序）；
--       T15.37 首页盈亏指标+每月固定支出（monthly_fixed_cost 表，menu_id 7195~7199，
--         盈亏公式：本月盈利=本月收入-(流水支出+Σ本月固定支出)）。
--       ★v1.34 新增（M18 里程碑，v1.29 任务文档，menu_id 7600~7699 区段）：
--       T18.1 语音合规四表（voice_compliance_rule / voice_recording_policy / voice_compliance_hit /
--         sensitive_word_library，混合表 tenant_id=NULL=平台级、非NULL=租户自定义，M18_01）；
--       T18.9 固定支出标签+周期规则（fixed_cost_tag / period_rule + 菜单 7620~7628，M18_02）；
--       T18.10 预约策略配置（booking_policy UNIQUE(tenant_id) upsert + 菜单 7630，M18_03）；
--       T18.12 拉新奖励配置扩展（promotion_reward_config ALTER 补 trigger_condition/reward_limit_type/
--         reward_limit_value 三列 + 菜单 7640，M18_04）；
--       T18.13 老板员工业绩榜菜单（复用 booking_order+user_rating+sys_user，不建表，菜单 7650，M18_05）；
--       T18.14 License 模板管理与使用报表（sys_license_template 平台级 + 4 条预置模板 + 菜单 7660~7661，M18_06）；
--       T18.15 Admin 运维审计补齐（sys_alert_rule 平台级 + 4 条预置告警规则 + 菜单 7670~7671，M18_07）；
--       T18.20 M18 汇总补丁（role_template boss/platform_admin 追加 M18 新权限 + sys_role_menu 存量租户补录，M18_08）。
--       ★v1.31 新增/v1.33 收敛（对齐《多业态SaaS平台需求规格说明书》V1.0，T15.35§六/T15.36§五§六，M15_19b/M15_20b）：
--       T15.35§六 图片校验与渲染管线（★复用 T12.x 既有扩展点，不新建平行设施：内容安全审核留痕复用
--         biz_content_security_log（provider=wechat_imgSecCheck）、衍生图任务复用 biz_image_task、成品 URL 复用
--         tenant_theme.logo_rendered_url/rendered_poster_url；@Primary WechatImgSecCheckSecurity 覆盖桩真调微信
--         wxa/img_sec_check，@Primary OssRenderImagePipeline 覆盖桩做 Logo 衬底 80x80 / 海报顶部渐变遮罩 /
--         k-means(k=3) 主色提取；M15_19b 唯一增量=tenant_theme 补 poster_dominant_color/poster_accent_color 2 列）；
--       T15.36§五 主题显示名映射（sys_tenant 补 theme_display_name_map json + default_logo_url，
--         营销名「暗夜桌游/暖光聚会/清新日系/霓虹赛博」与代码名双口径，theme_key 稳定不变）；
--       T15.36§六 多租户选择流程（对齐说明书 11.4，user_tenant_role≥2 条弹「我的店铺」列表，
--         新增跨角色公共页 pages/micro/tenant/select.vue，pages.json 18→19；★纯前端+登录响应体扩展，无建表，
--         user_tenant_bind 语义由既有 user_tenant_role 承接，不新建表）。
-- 说明：
--   1. 本目录为「小微版自包含可独立部署」脚本集，严格按序号执行，不可颠倒；
--   2. 全部脚本幂等（先删后建 / 先删后插 / 存储过程 + INFORMATION_SCHEMA 条件 ALTER），可重复执行；
--   3. 平台采用「一套系统 + 建表一次建齐、版本靠菜单模板 + 功能开关区分」策略：
--      小微版复用平台底座（sys_menu/sys_role/多租户/License/计费合规），
--      增量表由 M15_01~M15_05 建立、M15_19b 补 tenant_theme 主色/辅色 2 列、M15_21 补 monthly_fixed_cost、M16 推广分销表由 M16_01~M16_03 建立、
--      M17 评价点赞表由 M17_01 建立、M18 语音合规/标签周期/预约策略/License模板/告警规则表由 M18_01~M18_07 建立；
--      小微版菜单占用 7000~7509 独立区段（企业版 2000~6179 不冲突）；
--      ★M18 菜单占用 7600~7699 独立区段（语音合规 7600~7619 预留 / 固定支出配置 7620~7629 / 预约策略 7630~7639 /
--        拉新奖励配置 7640~7649 / 员工业绩榜 7650~7659 / License 模板 7660~7669 / 运维审计 7670~7679 /
--        敏感词管理 7680~7683（v1.57 已清理）+ 老板命中记录 7690 / 系统设置 7695~7697）；
--      ★资金平台管理菜单占用 1300~1359 区段（与若依原生 1000~1230 按钮不冲突）；
--      ★M16 推广分销管理占用 7180~7190（老板 PC）、7340~7342（员工小程序）、7440~7442（顾客小程序），
--        避让 M15 既有 7170~7179（顾客管理）/7330/7430（活动浏览）；
--      ★固定支出管理占用 7195~7199（boss PC 端）；
--      ★评价管理占用 7160~7164（boss PC 端，M17_01）；
--      ★platform_admin 管理菜单占用 7500~7509（与 admin 1300 区段完全隔离）；
--   4. ★小微版不含企业版资金管控重能力（预算编制/科目三层/报销管控/规则引擎/发票验真/项目容器等），
--      故本目录不含 M1_01/M1_02/M1_03/M1_07、M2_02/M2_03/M2_05~M2_11、M5/M6/M7/M11/M12/M13 脚本；
--   5. 执行方式：mysql -u root -p ry-flowable-plus 后逐个 source，或用客户端按序运行。
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
-- 阶段一：数据库底座（全量菜单 + 多业态平台底座）
-- ---------------------------------------------------------------------
-- [01] 全量菜单一次建齐（三版本企业菜单 + 按钮 + 隐藏页；建菜单 ≠ 开放，开放由模板控制）
--      ★须先执行：M15_06 为 sys_menu 补 client_type 列，依赖 sys_menu 表已存在
source M1_04_sys_menu.sql;
-- [02] 平台底座建表：biz_bundle 业态包 / feature_flag 功能开关 / tenant_biz_config 单店覆盖 /
--      tenant_theme 主题 / user_tenant_bind openid 多租户归属 + 内置业态包与开关
source M1_05_platform_foundation_tables.sql;
-- [03] sys_tenant 扩展：biz_code / access_channel / wecom_* / mp_app_id（小微版走 wechat 通道）
source M1_06_sys_tenant_biz_channel.sql;

-- ---------------------------------------------------------------------
-- 阶段二：角色与租户隔离（全版本共用一张 sys_role 表）
-- ---------------------------------------------------------------------
-- [04] 五角色初始化（boss/finance/manager/employee/platform_admin，role_id 9201~9205）
--      ★须先执行：M15_07 补充 salesman(9206)/customer(9207) 依赖本脚本既有角色
source M2_01_roles.sql;
-- [05] 租户隔离校正（sys_role 补 tenant_id 列，共享标准角色 tenant_id=NULL 跨租户可见）
--      ★须先于 M15_07：M15_07 防御性校正 salesman/customer 的 tenant_id 依赖本列已补
source M2_04_tenant_isolation.sql;

-- ---------------------------------------------------------------------
-- 阶段三：定时任务 / License / 版本编码
-- ---------------------------------------------------------------------
-- [06] 租户到期检测任务注册
source M3_01_xxl_job_tenant_expire_check.sql;
-- [07] 全量定时任务注册（幂等；小微版专属任务由 M15_08 追加）
source M3_02_xxl_job_all_tasks.sql;
-- [08] License-租户关联（sys_license 加 tenant_id）
source M3_05_license_tenant_association.sql;
-- [09] License 管理菜单增强
source M3_06_license_management_menu.sql;
-- [09.5] sys_tenant 移除 scale 列（v1.28 T3.2 变更，租户分类由 tenant_type+version_type 决定）
--      ★幂等：INFORMATION_SCHEMA 条件判断，列不存在也不报错
source M3_07_sys_tenant_drop_scale.sql;
-- [10] 版本编码迁移（ENTERPRISE→PROFESSIONAL 存量兼容；小微版走 normalizeEdition 归一化 MICRO）
source M4_01_edition_enterprise_to_professional.sql;

-- ---------------------------------------------------------------------
-- 阶段四：共用小程序租户识别 + 门店公开展示（引导态白名单）
-- ---------------------------------------------------------------------
-- [11] sys_tenant 门店公开列（city/address/经纬度/logo/简介，MyBatis-Plus 全量 SELECT 硬前置）
--      ★小微版门店主页/搜索强依赖本列
source M8_01_sys_tenant_store_profile.sql;
-- [12] 门店搜索 + portal_access_log 引导态审计表（系统级，无 tenant_id）
source M8_02_portal_store_search_and_audit.sql;

-- ---------------------------------------------------------------------
-- 阶段五：商业化计费与合规（全版本）
-- ---------------------------------------------------------------------
-- [13] 计费套餐/订单 + 支付分账虚拟账户 + 提现 + 对账（含菜单 6120~6159）
source M14_01_billing_settlement.sql;
-- [14] 合规：一次性订阅消息配额 + 协议版本留痕 + 数据导出注销（含菜单 6160~6179）
source M14_02_compliance.sql;
-- [15] 计费到期提醒任务（BillingRemindJob，到期前 7/3/1 天；严禁自动扣款）
source M14_03_xxl_job_billing_remind.sql;

-- ---------------------------------------------------------------------
-- 阶段六：小微版核心（M15 里程碑，本版本专属）
-- ---------------------------------------------------------------------
-- [16] T15.1 身份与角色映射核心表：user_tenant_role（一人多租户多角色）
--      + tenant_role_mapping（平台管理员挑选/改名/启停）+ role_template（系统级无 tenant_id，
--        version_type×role_code×menu_keys×block_keys，须入 MybatisPlusConfig.IGNORE_TENANT_TABLES）
source M15_01_micro_identity_role_tables.sql;
-- [17] T15.2 行业模板：industry_template + industry_role_map（均系统级无 tenant_id）
--      内置 board_game(桌游店)/market(买菜生鲜)/gym(健身房)/spa(汤泉美容) 四行业角色映射
source M15_02_industry_template.sql;
-- [18] T15.3 模块化页面 + 分享场景：page_block_config（单一页面壳 + 10 区块组件库，
--      tenant_id=0 系统默认模板，module_type=micro）+ share_scene（扫码带租户号）
source M15_03_page_block_share_scene.sql;
-- [19] T15.4 服务定价：service_category / service_item（price DECIMAL(12,2)，
--      pricing_type=duration/weight/times/person/portion）/ price_change_log（改价留痕）
source M15_04_service_pricing.sql;
-- [20] T15.5 预约结算与现金流：booking_order（★pay_status 仅 PENDING_PAY/RECEIVED，无在线支付态）
--      + staff_settlement（★pay_status 仅 PENDING/PAID_OFFLINE 线下发放）+ cash_flow（纯记账）
--      + micro_daily_revenue（每日营收快照，UNIQUE(tenant_id,revenue_date) 幂等 upsert）
source M15_05_booking_settlement_cashflow.sql;
-- [21] T15.6 sys_menu.client_type 端过滤列（pc/mini/both，存量 menu_id<7000 回填 pc）
--      + 小微版全量菜单 7000~7499：admin 行业/角色模板(7000~) + 平台管理员店铺配置(7010~)
--      + 老板 PC 运营(7100~) + 老板小程序(7200~) + 业务员小程序(7300~) + 顾客小程序(7400~，★无 pay)
--      ★须先执行 [01]M1_04（sys_menu 表存在）
source M15_06_sys_menu_client_type_micro.sql;
-- [22] T15.7 标准角色集补充（salesman=9206/customer=9207，先删后插 + 防御性校正 tenant_id=NULL）
--      + 小微版角色模板三配方（version_type='micro'）：boss/salesman/customer 的 menu_keys + block_keys
--      ★前置：[04]M2_01 + [05]M2_04 + [16]M15_01 + [21]M15_06 均须已执行
source M15_07_micro_role_template_init.sql;
-- [23] T15.22 小微版 XXL-JOB 三任务注册（microDailyRevenueJob/bookingExpireJob/customerTagJob）
--      ★一个 Handler 服务全量租户（executor_param 留空），@TenantJob(versions={"micro"}) 版本过滤
--      ★前置：须先部署 TenantJobAspect 切面 + 三个 Handler Bean；依赖 [07]M3_02 + [20]M15_05
source M15_08_xxl_job_micro_tasks.sql;
-- [24] T15.26 角色可见性硬隔离：sys_role 补 tenant_type_scope 列（micro/enterprise/both）
--      + 存量回填（boss/platform_admin=both；salesman/customer=micro；manager/employee/finance=enterprise）
--      + 索引 idx_role_scope_status；可选角色列表接口 SQL 层按 tenant_type 数据源过滤
source M15_09_micro_role_scope_hard_isolation.sql;
-- [25] T15.26+T15.27 小微版分档机制：sys_tenant 补 tenant_type/version_type 列 + 存量回填
--      + role_template micro_lite/micro_pro 分档配方（6 行：lite 3 + pro 3）
--      + sys_billing_plan 补 version_type 列 + 5 档分档套餐（micro_lite_free / micro_pro 四档）
--      ★前置：[16]M15_01（role_template 表）+ [22]M15_07（micro 配方）+ [13]M14_01（billing_plan）
source M15_10_micro_edition_tier_role_template.sql;
-- [26] T15.28 lite 档活动浏览页：promotion_activity 表（租户级，推广内容只读数据源）
--      + 菜单 7330（业务员小程序）/7430（顾客小程序），共用 perms micro:activity:browse，client_type=mini
--      ★lite 档 salesman/customer 唯一菜单；无需登录可浏览（走 security.excludes 白名单）
source M15_11_promotion_activity_browse.sql;
-- [27] T15.29 资金平台管理菜单 + 版本档位管理：一级目录「资金平台管理菜单」(menu_id=1300)
--      + 版本档位管理(1310~1314，system:edition:*) + 平台自有管理菜单 re-parent 到 1300
--      ★menu_id 1300~1359（避开若依原生 1000~1230 按钮区段）；新建租户级联为 Java 接口
source M15_12_fund_platform_menu_edition.sql;
-- [28] T15.31+T15.32 防爽约/黑名单：customer_credit 表（UNIQUE(tenant_id,user_id)，version 乐观锁）
--      + no_show_record 表 + booking_order 扩展 expire_time/NO_SHOW 态/扫描索引
--      + 顾客管理菜单 7170~7172（micro:customer:list/unblacklist/creditDetail）
--      + noShowCheckJob 注册（@TenantJob(versions={"micro"},versionTypes={"micro_pro"})，每 10 分钟）
--      ★仅 micro_pro 生效；满 3 次自动拉黑（原子操作）；老板手动移出为唯一解除出口
source M15_13_customer_credit_no_show.sql;
-- [29] T15.6+T15.16/17+T15.29/30 前端菜单缺口幂等修补（★ruoyi-ui 交付后必执行，否则页面装修无入口）
--      + 补建「页面装修」菜单 7063~7066（parent=7010 店铺配置，URL /microConfig/pageDecorate，
--        perms micro:page:list/edit/add/remove，对应 PageBlockConfigController /micro/page）
--      + 7012 用户列表 re-point 到 system/user/index（手机号为全局身份锚点，用户池共享，
--        无 /micro/user 控制器），7013~7015 按钮 re-point 到 system:user:*，7016 隐藏
--      + 7040 套餐管理 / 7051 员工档案 / 7055 结算规则 / 7135 支出预警 置 visible='1'
--        （菜单与后端双缺，避免生产点菜单白屏；perms 保留待后端补齐）
--      + 1312~1314 隐藏（SysEditionController 仅 4 个只读端点，档位配方写入口收敛到 7005 角色模板管理）
--      ★前置：[21]M15_06 + [27]M15_12 + [28]M15_13 均须已执行；★不建表、不改表结构
source M15_14_frontend_menu_gap_patch.sql;
-- [30] T15.29 补漏：存量小微版租户克隆角色菜单授权修复
--      ★clonePresetRoles() 此前仅走 sys_edition_menu_template（企业版路径），小微版无 BOSS_MICRO 等键，
--      致克隆角色 sys_role_menu=0。本脚本按 role_template(version_type×role_code) 补写 sys_role_menu。
--      ★前置：[22]M15_07 + [25]M15_10（role_template 配方）+ [21]M15_06（sys_menu 7000~7499）
--      ★幂等：INSERT IGNORE + NOT EXISTS，可重复执行
source M15_15_fix_existing_micro_tenant_menu.sql;
-- [30.5] platform_admin 角色模板补全 + License 菜单修复（v1.18 新增）
--      ★role_template 此前无 platform_admin 条目（M15_07/M15_10 只建了 boss/salesman/customer），
--      致 M15_15 Step 2 JOIN 匹配不到 → sys_role_menu 为空，platform_admin 登录无菜单。
--      修复：License管理(3130) re-parent 到资金平台管理(1300) +
--      micro_lite/micro_pro 两档 platform_admin 配方（含 system:license:manage 上传升级）+
--      存量租户 sys_role_menu 补写 + 预设角色 9205 同步修复。
--      ★前置：[25]M15_10 + [27]M15_12 + [30]M15_15
--      ★幂等：先删后插 + INSERT IGNORE，可重复执行
source M15_16_platform_admin_role_template_license.sql;

-- ---------------------------------------------------------------------
-- 阶段七：租户管理员菜单 + 推广分销拆分 + 主题/图片 + 盈亏指标（T15.33~T15.37，v1.30 新增）
-- ---------------------------------------------------------------------
-- [34] T15.33 platform_admin 租户管理员菜单三件套：版本升级(7501~7503)/角色映射(7504~7506)/
--      人员管理(7507)/小程序主题(7508)，一级目录 7500。
--      + role_template micro_lite/micro_pro platform_admin 追加新 perms + 存量补写 sys_role_menu
--      ★menu_id 7500~7509（避让既有 7000~7499 小微版菜单），与 admin 的 1300 完全隔离
--      ★前置：[21]M15_06 + [25]M15_10 + [30.5]M15_16
source M15_17_platform_admin_menu.sql;
-- [35] T15.34 boss 推广分销管理菜单拆分：新增业务员奖励记录(7189)+顾客奖励记录(7190)，
--      挂在既有 7181/7185 目录下；micro_pro boss role_template 追加 distribution perms
--      ★前置：[31]M16_01（推广分销菜单 7180~7188 已建立）+ [25]M15_10
source M15_18_boss_distribution_menu_split.sql;
-- [36] T15.35 活动图片上传+背景主题：promotion_activity 补 logo_url/theme_key 两列
--      （poster_url 已由 M15_11 建立，4 套预设主题 theme_default/warm/fresh/elegant）
--      ★前置：[26]M15_11（promotion_activity 表）
source M15_19_activity_image_theme.sql;
-- [36.5] T15.35§六 图片校验与渲染管线（v1.31 新增 / v1.33 收敛）：★复用 T12.x 既有扩展点，
--      仅 tenant_theme 补 poster_dominant_color/poster_accent_color 2 列（k-means 主色/辅色落库）；
--      审核留痕复用 biz_content_security_log、任务状态复用 biz_image_task、成品 URL 复用 logo_rendered_url/rendered_poster_url，
--      不新建 img_sec_check_log 表、不加 promotion_activity 冗余列
--      ★前置：[02]M1_05（tenant_theme 表，含 logo_rendered_url/rendered_poster_url）
source M15_19b_activity_image_pipeline.sql;
-- [37] T15.36 小程序全局主题+首页欢迎语：sys_tenant 补 app_theme_key 列
--      （platform_admin 选定主题后写入该列，小程序冷启动读取）
--      ★前置：[21]M15_06（sys_tenant 表）
source M15_20_platform_admin_theme_welcome.sql;
-- [37.5] T15.36§五 主题显示名映射 + 默认 Logo（v1.31 新增）：sys_tenant 补 2 列
--      （theme_display_name_map json + default_logo_url varchar(500)）
--      ★前置：[21]M15_06 + [37]M15_20（sys_tenant 表及 app_theme_key 已就绪）
source M15_20b_tenant_theme_display_map.sql;
-- [38] T15.37 每月固定支出表+管理菜单：monthly_fixed_cost 表（tenant_id/name/category/amount/...）
--      + 固定支出管理菜单 7195~7199（micro:pro:cost:*，boss PC 端）
--      + micro_pro boss role_template 追加 cost perms（覆盖 M15_18 版本）+ 存量补写
--      ★前置：[25]M15_10（role_template 分档）
source M15_21_monthly_fixed_cost.sql;
-- [38.5] ★T15.33 行业模板种子数据填充（v1.35 新增，M15_22，修复「新建租户行业下拉仅 4 项」）
--      23 个二级行业（休闲娱乐 6 / 生活服务 7 / 运动健身 7 / 专业工作室 3）× preset_config
--      （category / service_categories[] / settle_rule / units[] / projects[5~7 条大众点评上海真实团购]）
--      + 69 条 industry_role_map（23 × boss/salesman/customer）
--      + market（买菜/生鲜）SET status='1' 停用（V3.5 §10 明确排除零售/生鲜），保留数据兼容既有租户
--      ★幂等：INSERT IGNORE + UPDATE 双重（uk_industry_code / uk_industry_bizrole 保护）
--      ★前置：[17]M15_02（industry_template + industry_role_map 建表）
source M15_22_industry_template_seed_v35.sql;
-- [39] ★T1.7/T18.1 行业模板补 voice_enabled 列（三级语音控制总开关，v1.33 新增）
--      industry_template 补 voice_enabled TINYINT DEFAULT 1（0否 1是，admin→platform_admin→boss）
--      存量数据回填 voice_enabled=1（兼容既有行业）
--      ★前置：[17]M15_02（industry_template 建表）
--      ★幂等：INFORMATION_SCHEMA 条件 ALTER + NULL 回填
source M15_25_industry_template_voice_enabled.sql;

-- ---------------------------------------------------------------------
-- 阶段八：推广分销与激励（M16 里程碑，★仅小微版启用 FEAT_PROMOTION_DISTRIBUTION）
-- ---------------------------------------------------------------------
-- [39] T16.1/T16.5/T16.6/T16.10/T16.11 推广码与归因：promotion_code（门店码+个人码，一人一码
--      UNIQUE(tenant_id,user_id,code_type)，永久有效无过期字段，scene≤32 字符）+ promotion_attribution
--      （谁最后扫码业绩归谁，UNIQUE(tenant_id,customer_user_id)，last_scan_time 交接点锁定不回溯）
--      + 推广分销菜单：老板 PC 7180~7188（micro:distribution:*）+ 员工小程序 7340~7342 + 顾客小程序 7440~7442
--      ★前置：[21]M15_06（菜单 7000~7499）；★menu_id 避让 7170~7179（M15_13）/7330/7430（M15_11）
source M16_01_promotion_code_attribution.sql;
-- [40] T16.2/T16.3/T16.7 拉新奖励与优惠券：promotion_reward_config（奖励内容唯一可变，trigger_rule 固定=拉新）
--      + promotion_reward_record（首消触发仅一次 UNIQUE(tenant_id,customer_user_id)，现金走「标记已线下发放」）
--      + coupon（站内券模板）+ coupon_record（source 区分 promotion 拉新发放/activity 活动领取）
--      ★V1 零支付：员工现金奖励不调任何支付接口；顾客奖励为站内优惠券
--      ★前置：[20]M15_05（staff_settlement/cash_flow 记账口径）+ [39]M16_01（归因关系）
source M16_02_promotion_reward_coupon.sql;
-- [41] T16.4/T16.8/T16.12 首单识别 + 功能开关：first_order_record（手机号唯一识别，
--      UNIQUE(tenant_id,activity_id,customer_phone)，按活动独立判断，与推广归属解耦）
--      + feature_flag 追加第 34 项 FEAT_PROMOTION_DISTRIBUTION（组 E，basic/advanced/pro 均 0，仅小微版启用）
--      ★前置：[02]M1_05（feature_flag 33 项，本脚本仅追加不动前 33）+ [26]M15_11（promotion_activity 活动表）
source M16_03_first_order_feature_flag.sql;

-- [42] T17.1~T17.5 通用评价与点赞：user_rating（1~5 星 + 内容 + 图片 + 场景 + 回复 + 屏蔽，
--      UNIQUE(tenant_id,target_user_id,author_user_id,scene,order_id) 防重 + 禁止自评）
--      + user_like（点赞 toggle，UNIQUE(tenant_id,target_user_id,author_user_id,scene)）
--      + 评价管理菜单 7160~7164（micro:rating:list/reply/shield/detail/ranking，老板 PC，挂 7100 小微运营下）
--      + feature_flag 第 35 项 FEAT_RATING（组 E，basic/advanced/pro 均 0，仅 micro_pro 启用）
--      ★前置：[02]M1_05（feature_flag 33 项）+ [41]M16_03（第 34 项；本脚本追加第 35 项）+ [21]M15_06（菜单 7000~7499）
source M17_01_user_rating_like.sql;

-- [42.5] feature_flag 补 micro_enabled 列（小微版功能开关列，v1.31 T10.3 缺口封闭）
--       幂等 ALTER（INFORMATION_SCHEMA 守卫，列已存在则跳过）；
--       组 E（FEAT_PROMOTION_DISTRIBUTION / FEAT_RATING）回填 micro_enabled=1；
--       前置：[02]M1_05（feature_flag 建表）+ [41]M16_03（第 34 项）+ [42]M17_01（第 35 项）
source M10_05_feature_flag_micro_column.sql;

-- ---------------------------------------------------------------------
-- 阶段九-a：M18 里程碑（v1.29 新增，menu_id 7600~7679 区段，V3.7 菜单骨架缺口补齐）
-- ---------------------------------------------------------------------
-- [43] T18.1 语音合规规则与记录：voice_compliance_rule（租户级 activity_type + sensitive_word_override）
--      + voice_recording_policy（租户级 UNIQUE tenant_id，max_retention_days ≤ Admin 全局上限）
--      + voice_compliance_hit（合规命中明细，租户级）
--      + sensitive_word_library（混合表：tenant_id=NULL=平台级 / 非NULL=租户自定义）+ 8 条平台敏感词初始数据
--      ★menu_id 预留 7600~7619（本脚本不建菜单，菜单由 M18_02~M18_07 各自建立）
--      ★前置：[02]M1_05（平台底座）+ [21]M15_06（sys_menu.client_type）
source M18_01_voice_compliance_tables.sql;

-- [44] T18.9 固定支出标签 + 周期规则：fixed_cost_tag（房租/水电/工资等，UNIQUE(tenant_id,tag_name)）
--      + period_rule（数字 + 时间单位 DAY/WEEK/MONTH/QUARTER/YEAR）
--      + 菜单 7620~7628（platform_admin PC，固定支出配置目录 + 标签管理 + 周期规则管理 + 6 按钮）
--      ★前置：[21]M15_06（sys_menu.client_type）+ [38]M15_21（monthly_fixed_cost 表）
source M18_02_tag_period_rule_tables.sql;

-- [45] T18.10 预约策略配置：booking_policy（租户级 UNIQUE(tenant_id) upsert 模式，
--      customer_self_booking/advance_booking_days/cancel_policy/no_show_threshold/auto_confirm/max_daily_bookings）
--      + 菜单 7630（platform_admin PC，挂 7500 租户管理员目录下）
--      ★前置：[21]M15_06 + [44]M18_02（同属 platform_admin 菜单区段，序号连续）
source M18_03_booking_policy_tables.sql;

-- [46] T18.12 拉新奖励配置扩展：promotion_reward_config ALTER 幂等补 3 列
--      （trigger_condition NEW_CUSTOMER_BIND/NEW_CUSTOMER_FIRST_ORDER + reward_limit_type NONE/DAILY/MONTHLY
--       + reward_limit_value），复用 M16_02 既有表，不重建
--      + 菜单 7640（老板 PC，挂 7180 推广分销管理目录下）+ 更新 7184 component 路径
--      ★前置：[39]M16_01（菜单 7180~7188）+ [40]M16_02（promotion_reward_config 表）
source M18_04_reward_config_extend.sql;

-- [47] T18.13 老板员工业绩榜菜单：menu_id 7650（micro:staffRanking:list，接单数/营收/评分排行）
--      ★无新建表，复用 booking_order + user_rating + sys_user
--      ★前置：[42]M17_01（user_rating 表）+ [20]M15_05（booking_order 表）
source M18_05_staff_ranking_menu.sql;

-- [48] T18.14 License 模板管理与使用报表：sys_license_template（平台级无 tenant_id，
--      UNIQUE(edition,license_type)）+ 4 条预置模板（BASIC/ADVANCED/PROFESSIONAL 正式 + BASIC 试用 7 天）
--      + 菜单 7660~7661（system:licenseTemplate:list / system:licenseReport:list，Admin 专属，挂 100 系统管理下）
--      ★前置：sys_license / sys_tenant（既有）+ [09]M3_06（License 管理菜单增强）
source M18_06_license_template_tables.sql;

-- [49] T18.15 Admin 运维审计补齐：sys_alert_rule（平台级无 tenant_id）
--      + 4 条预置告警规则（LICENSE_EXPIRE/COMPLIANCE_HIT/TENANT_DISABLED/SERVICE_DOWN）
--      + 菜单 7670~7671（system:voiceAuditLog:list / system:alertRule:list，Admin 专属，挂 100 系统管理下）
--      ★前置：[43]M18_01（voice_compliance_hit 表，供合规命中告警查询）
source M18_07_admin_audit_tables.sql;

-- [50] T18.20 M18 汇总补丁：role_template 更新
--      ① boss(micro/micro_pro) 追加 micro:distribution:rewardConfig + micro:staffRanking:list
--      ② platform_admin(micro/micro_pro) 追加 micro:tagConfig:list + micro:periodRule:list + micro:bookingPolicy:list + micro:bookingPolicy:edit（按钮级 F，menu_id=7631，对齐 MicroBookingPolicyController.savePolicy 的 @SaCheckPermission）
--      + sys_role_menu 存量租户克隆角色关联补录（platform_admin/micro_pro/admin，INSERT IGNORE 幂等）
--      ★前置：[43]~[49] M18_01~M18_07 全部执行完毕；★幂等：JSON_CONTAINS 判断后再追加
source M18_08_menu_role_template_consolidate.sql;

-- ---------------------------------------------------------------------
-- 阶段九-b：平台底座管理菜单补齐（T10.2 / T10.3，v1.19 补入）
-- ---------------------------------------------------------------------
-- ★顺序强约束（不可颠倒）：M15_12 以
--     DELETE FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359
--   开局，而 M10_01 的「业态包管理」(1320~1322) 与「功能开关管理」(1330~1332) 正落在该区段内。
--   必须【先 M15_12 建 1300 一级目录 + 1310~1314 版本档位管理，再 M10_01】，
--   颠倒则 M10_01 建出的菜单会被 M15_12 的 DELETE 清除，T10.2/T10.3 的 PC 入口消失。
-- [九-1] 业态包管理（system:bizBundle:*）+ 功能开关管理（system:featureFlag:*），
--       menu_id 1320~1339，六层防串第①层「版本分配入口」的 PC 管理页（T10.2 / T10.3），
--       全部授予超管 role_id=1。前置：M15_12 已在阶段六建出 1300 一级目录，M1_05 已建 biz_bundle / feature_flag。
source M10_01_biz_bundle_feature_flag_menu.sql;
-- [九-2] 平台运营菜单补建（T10.4 / T13.1~T13.6 / §5.7 计费合规收纳）
--       单店覆盖(1340~1342) + 访问通道管理(1343~1345) + re-parent 6120/6160 到 1300，
--       全部授予超管 role_id=1。
--       前置：[27]M15_12（1300 目录已建）+ [九-1]M10_01 + M14_01（6120/6160 已建）
source M10_02_platform_operation_menu.sql;

-- ---------------------------------------------------------------------
-- 阶段十：紧急修复补丁（v1.36 新增）
-- ---------------------------------------------------------------------
-- [51] ★紧急修复：platform_admin 登录无菜单 + 权限隔离修正（v1.36，2026-09-18）
--      根因：① role_template 中 platform_admin 错误包含 admin 级别 perms（违反隔离原则）
--            ② M 类型目录 7500 的 perms 为空，不会被 menu_keys→perms 匹配进入 sys_role_menu，
--               导致 selectMenuTreeByUserId 找不到根节点，子菜单全部成为孤儿
--            ③ BizCopyController i18n 只读端点权限过严（代码层面已修复）
--      修复：修正 role_template + 清理错误关联 + 补录 M 目录 7500 + 补录 M18 菜单
--      ★前置：[34]M15_17（三件套菜单 7500~7509）+ [44]M18_02（7620~7629）+ [45]M18_03（7630~7639）
--      ★幂等：先删后插 + INSERT IGNORE，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source _fix_platform_admin_menu_v136.sql;

-- [52] ★platform_admin 克隆角色 + 用户角色分配 + 完整菜单授权修复（v1.37，2026-09-18）
--      根因：① 部分小微版租户缺少 platform_admin 克隆角色（sys_role 无记录）
--            ② 租户用户在 sys_user_role 中无任何角色分配 → 登录后 selectMenuTreeByUserId 查不到菜单
--            ③ M 类型目录 7500/7620 perms 为空，不会被 role_template.menu_keys→perms 匹配
--      修复：补建克隆角色 + 补建用户角色关联 + 补全 sys_role_menu（7500 目录体系 + M18 菜单）
--      ★前置：[34]M15_17（三件套菜单 7500~7509）+ [44]M18_02（7620~7628）+ [45]M18_03（7630）+ [51]_fix_v136
--      ★幂等：INSERT IGNORE + NOT EXISTS，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M18_09_platform_admin_role_menu_fix.sql;

-- [53] ★T15.38 platform_admin 原子资源管理：项目管理 + 房间/空间管理（v1.38，2026-09-18）
--      依据：V3.7 §4.2「原子资源管理」（Project/Room/Role/Staff 四原子），补齐此前缺失的项目管理、房间/空间管理，
--            与既有 角色映射(7504)=角色管理、人员管理(7507)=员工库 一并归入「原子资源管理」目录(7510)。
--      内容：① service_category 补 category_type 列（service 默认/space 房间·空间）；
--            ② industry_template.preset_config 补 rooms[] 预设（23 行业各 2~3 间，条件 JSON_SET 仅补空）；
--            ③ 菜单 7510 目录 + 项目管理 7511~7513 + 房间/空间管理 7514~7516，7504/7507 re-parent 到 7510；
--            ④ role_template micro_lite/micro_pro platform_admin 追加 6 perms（JSON_CONTAINS 守卫）；
--            ⑤ sys_role_menu role_id=1 全量 + 存量租户 platform_admin 克隆角色补录（含 M 目录 7510）。
--      ★数据来源：与角色映射自愈同模式，租户首次进入或建租户时从 admin 行业模板 preset_config.projects/rooms
--        自动初始化落库（IndustryTemplateServiceImpl.initTenantResources/selfHealCurrentTenantResources，幂等仅空表补录）。
--      ★前置：[19]M15_04（service_category/service_item）+ [34]M15_17（7500~7509）+ [38.5]M15_22（23 行业 preset_config）
--      ★幂等：条件 ALTER（INFORMATION_SCHEMA 守卫）+ JSON_EXTRACT IS NULL 守卫 + 先删后插 + INSERT IGNORE
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M15_23_atomic_resource_project_room.sql;

-- ---------------------------------------------------------------------
-- 阶段十一：M18 图片合规与全局敏感词（v1.27 新增，全版本通用·平台级合规底线）
-- ---------------------------------------------------------------------
-- [54] T18.1/T18.8 敏感词/命中日志/图片合规配置三表 + feature_flag 双开关（35→37）
--      前置：[02]M1_05（feature_flag 表）；三表 tenant_id=0=全局底库，走条件查询而非 IGNORE_TENANT_TABLES 白名单
source M18_10_sensitive_word_image_compliance.sql;
-- [55] T18.7 全局敏感词底库初始化（V3.8 附录 A 八大类约 200 条，INSERT IGNORE 幂等）
--      + 旧表 sensitive_word_library→sensitive_word 幂等迁移（不动旧表原数据）
--      前置：[54]M18_10（sensitive_word 表）+ [43]M18_01（旧表 sensitive_word_library，存在则迁移，不存在则 no-op）
source M18_11_sensitive_word_init_data.sql;
-- [56] T18.4/T18.5 Admin 平台合规菜单 1370~1379 + 租户敏感词管理 7680~7683 + 老板命中记录 7690
--      + role_template admin/boss/platform_admin 三角色追加权限
--      前置：[01]M1_04（sys_menu）+ [21]M15_06（client_type）+ [54]M18_10（三表）+ [34]M15_17（7500 目录）
--      ★v1.55 补建：原脚本缺失，本次创建（Admin 平台合规目录 1370~1379 为 M18_13 语音合规菜单的父级目录）
source M18_12_compliance_menu.sql;
-- [56.5] T18.1~T18.4 语音合规全链路菜单注册（v1.53，T18.1 建表+T18.2 Admin+T18.3 租户管理员+T18.4 老板）
--      BE/API/Vue 已全部交付，本脚本补齐 sys_menu 入口（7600~7619 预留区段）+ role_template + sys_role_menu
--      ★前置：[43]M18_01（四张合规表）+ [56]M18_12（1370 平台合规目录）+ [34]M15_17（7500 目录）
--      ★幂等：先删后插 + INSERT IGNORE + JSON_CONTAINS 守卫
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M18_13_voice_compliance_menu.sql;
-- [56.6] T18.11 租户管理员系统设置菜单注册（v1.54，T18.11 后端+前端已交付，补齐菜单+role_template）
--      菜单 7695~7696（micro:systemSetting:list/edit，挂 7500 租户管理员目录下）
--      + role_template platform_admin 追加 micro:systemSetting:list/edit
--      + sys_role_menu 存量租户 platform_admin 克隆角色补录
--      ★前置：[34]M15_17（7500 目录）+ [21]M15_06（client_type）
--      ★幂等：先删后插 + JSON_CONTAINS 守卫 + INSERT IGNORE
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M18_14_system_setting_menu.sql;
-- [56.7] ★T18.20 M18 最终汇总补丁（v1.55，必须在 M18 全部子脚本执行完毕后运行）：
--      ① 修复 M18_08 缺陷：role_key='micro_pro' 不存在→'boss'（7640~7659 拉新奖励+员工业绩榜）；
--      ② role_template admin/platform_admin/boss 三角色 M18 权限完整性兜底（幂等追加）；
--      ③ sys_role_menu 存量租户全量补录（7600~7699 + 1370~1379 全覆盖）；
--      ④ platform_admin 克隆角色补录 M18_13/M18_14 新增菜单（语音合规 7610~7617 + 系统设置 7695~7696）
--      ★前置：[43]~[56.6] M18_01~M18_14 全部执行完毕
--      ★幂等：JSON_CONTAINS 守卫 + INSERT IGNORE，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M18_15_m18_final_role_template_patch.sql;

-- ---------------------------------------------------------------------
-- 阶段十二：T15.38 原子资源修复（v1.40 新增）
-- ---------------------------------------------------------------------
-- [57] service_item 补 capacity 列 + 存量数据回填
--      前置：[19]M15_04（service_item 表）+ [53]M15_23（房间数据）
--      ★幂等：条件 ALTER（INFORMATION_SCHEMA 守卫）+ 仅 NULL 时 UPDATE
source M15_24_service_item_capacity.sql;

-- ---------------------------------------------------------------------
-- 阶段十三：micro_lite 老板首页修复（v1.41 新增）
-- ---------------------------------------------------------------------
-- [58] ★micro_lite(免费版) boss 经营驾驶舱首页修复（v1.41，2026-09-19）
--      根因：role_template micro_lite boss 缺少 micro:dashboard:view，
--            导致 boss 无 7101（经营概览）菜单，登录后无默认首页。
--      修复：role_template 追加 micro:dashboard:view + 存量 sys_role_menu 补写（7100+7101）
--      ★前置：[21]M15_06（菜单 7000~7499）+ [25]M15_10（micro_lite 分档配方）
--      ★幂等：JSON_CONTAINS 守卫 + INSERT IGNORE，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M15_25_boss_lite_dashboard_fix.sql;

-- ---------------------------------------------------------------------
-- 阶段十四：boss 克隆角色菜单清理（v1.42 新增）
-- ---------------------------------------------------------------------
-- [59] ★boss 克隆角色菜单清理：移除残留的 platform_admin 菜单（v1.42，2026-09-19）
--      根因：历史修复脚本部分 boss 克隆角色被错误关联了 platform_admin 菜单（7500~7509/7620~7639），
--            导致 boss 登录后侧边栏出现租户管理员菜单，违反 V3.7 角色隔离原则。
--      修复：删除 boss 克隆角色中 7500~7519/7620~7639 菜单关联 + 补录 M 目录 7100。
--      ★前置：[21]M15_06（菜单 7000~7499）+ [34]M15_17（7500~7509）+ [52]M18_09（7620~7639）
--      ★幂等：DELETE WHERE EXISTS 幂等，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M15_26_boss_role_menu_cleanup.sql;

-- ---------------------------------------------------------------------
-- 阶段十五：老板用户角色分配修复（v1.43 新增）
-- ---------------------------------------------------------------------
-- [60] ★紧急修复：老板用户被错误分配 platform_admin 角色（v1.43，2026-09-19）
--      根因：用户在 sys_user_role 中被错误关联到 platform_admin 克隆角色，而非 boss 克隆角色，
--            导致登录看到平台管理菜单（租户管理、版本升级等）而非经营驾驶舱。
--      修复：删除错误的 platform_admin 关联 + 补建正确的 boss 关联。
--      ★前置：[53]M15_23（原子资源管理）+ [58]M15_25（lite 档首页）+ [59]M15_26（菜单清理）
--      ★幂等：INSERT IGNORE + NOT EXISTS，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source _fix_boss_role_assignment.sql;

-- ---------------------------------------------------------------------
-- 阶段十六：小微版租户 boss 克隆角色补建 + 用户角色错配修复（v1.44 新增）
-- ---------------------------------------------------------------------
-- [61] ★根因修复：存量小微版租户缺 boss 克隆角色 + 老板账号被挂 platform_admin（v1.44，2026-09-19）
--      根因：①存量租户 sys_role 仅有 platform_admin 克隆（M18_09 补建），boss 克隆从未创建；
--            ②M18_09 Step 2 把无角色用户统一挂 platform_admin → 老板账号拿到租户管理员角色；
--            ③[60] 依赖 boss 克隆存在（INSERT 0 行）且删掉了 admin_user_id 的 platform_admin。
--      修复：按 role_template 配方补建 boss/salesman/customer 等克隆角色 + 补写 sys_role_menu
--            （递归 CTE 补 M 祖先目录）+ admin_user_id 回补 platform_admin + 非 admin 用户改挂 boss。
--      ★前置：[21]M15_06（菜单 7000~7499）+ [52]M18_09（platform_admin 克隆）
--      ★幂等：NOT EXISTS + INSERT IGNORE，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M15_27_micro_tenant_boss_role_rebuild.sql;

-- ---------------------------------------------------------------------
-- 阶段十七：micro_lite(免费版) boss 推广管理菜单授权（v1.45 新增）
-- ---------------------------------------------------------------------
-- [62] ★micro_lite(免费版) boss 推广管理菜单授权（v1.45，2026-09-19）
--      依据 V3.7 §八 免费版老板菜单范围：推广管理（拉新奖励配置+推广数据看板）+ 活动管理（活动列表）
--      修复：role_template micro_lite boss 追加 micro:distribution:rewardConfig + micro:distribution:*
--      ★前置：[25]M15_10（micro_lite 分档配方）+ [48]M18_08（micro_pro 已含同权限）
--      ★幂等：JSON_CONTAINS 守卫 + INSERT IGNORE，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M19_01_micro_lite_boss_promotion_menu.sql;

-- ---------------------------------------------------------------------
-- 阶段十八：小微版 boss 补录 system:oss:upload 权限（v1.46 新增）
-- ---------------------------------------------------------------------
-- [63] ★小微版 boss 角色补录 OSS 上传权限（v1.46，2026-09-19）
--      问题：boss 调用 POST /system/oss/upload 报 403「无此权限：system:oss:upload」
--      根因：boss 的 role_template.menu_keys 仅含 micro:* 系列，sys_role_menu 无 menu_id=1131
--      修复：为所有小微版 boss 克隆角色补录 menu_id 1(系统管理) + 118(文件管理) + 1131(文件上传)
--      ★前置：[61]M15_27（boss 克隆角色已补建）
--      ★幂等：INSERT IGNORE + NOT EXISTS，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M19_02_boss_oss_upload_permission.sql;

-- ---------------------------------------------------------------------
-- 阶段十九：micro_lite(免费版) boss 菜单精简（v1.47 新增）
-- ---------------------------------------------------------------------
-- [64] ★micro_lite(免费版) boss 菜单精简（v1.47，2026-09-19）
--      需求：免费版 boss 侧边栏只保留「经营概览」和「活动管理」，首页显示经营概览
--      修复：① role_template micro_lite boss menu_keys 精简为 micro:dashboard:view + micro:promo:list；
--            ② 存量小微版免费版租户 boss 克隆角色清理 sys_role_menu（删除系统管理 1/118/1131 + 非目标菜单，重新插入目标菜单 + M 祖先目录）
--      ★前置：[25]M15_10（micro_lite 分档配方）+ [58]M15_25（经营概览菜单）+ [62]M19_01（推广管理菜单）
--      ★幂等：JSON_SET 覆盖 + DELETE + INSERT IGNORE，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M19_03_micro_lite_boss_menu_simplify.sql;

-- [65] ★micro_lite(免费版) boss 活动增删改查权限补全（v1.48，2026-09-19）
--      需求：免费版 boss 活动管理需完整增删改查，M19_03 仅保留 list 导致操作按钮全部消失
--      修复：① role_template micro_lite boss menu_keys 追加 micro:promo:add/edit/remove/poster；
--            ② 存量 boss 克隆角色补录 7152/7153/7154/7155 到 sys_role_menu
--      ★前置：[64]M19_03（micro_lite boss 菜单精简）
--      ★幂等：JSON_ARRAY 覆盖 + INSERT IGNORE，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M19_04_micro_lite_boss_promo_crud_permission.sql;

-- ---------------------------------------------------------------------
-- 阶段二十：小微版 boss OSS 上传权限彻底修复（v1.49 新增）
-- ---------------------------------------------------------------------
-- [66] ★小微版 boss OSS 上传权限彻底修复 + 系统管理菜单根治（v1.49/v1.50，2026-09-19）
--      问题：M19_02 仅补 sys_role_menu（运行表），未更新 role_template（模板源头），
--            角色重建（版本变更 reCopyMenuTemplateForMicro / 新建租户 clonePresetRoles）
--            从 role_template.menu_keys 读取权限，模板不含 system:oss:upload → 修复丢失；
--            且 copyMenuTemplateForMicro 会为 F 类型 1131 补 M 祖先，把「系统管理」(1) 拖入 boss 侧边栏
--      根因：role_template 是权限天花板（source of truth），仅改 sys_role_menu = 治标不治本；
--            Java 层祖先补全未区分菜单类型，F 按钮不应触发 M 目录补全
--      修复：① role_template micro/micro_pro/micro_lite boss menu_keys 追加 system:oss:upload；
--            ② 所有小微版 boss（含 micro_lite）sys_role_menu 删除 1(系统管理,M)/118(文件管理,C) + 只补 1131(F 按钮)；
--            ③ 配套 Java 修复 SysTenantServiceImpl.copyMenuTemplateForMicro（F 类型菜单不补 M 祖先）
--      ★前置：[63]M19_02（首次补丁）+ [64]M19_03（micro_lite 精简）+ [65]M19_04（活动 CRUD）
--      ★幂等：JSON_CONTAINS 守卫 + 条件 DELETE + INSERT IGNORE + NOT EXISTS，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M19_05_boss_oss_upload_role_template_fix.sql;

-- [67] ★小微版 boss OSS 图片回显/删除权限补全（v1.51，2026-09-19）
--      问题：boss 打开「活动管理」列表/编辑页，前端 ImageUpload 组件 + promo/index.vue resolveOssIds()
--            调 GET /system/oss/listByIds/{ossId} 回显图片，报 403「无此权限：system:oss:list」；
--            删除已上传图片调 DELETE /system/oss/{ossId}（delOss）需 system:oss:remove
--      根因：system:oss:list 只挂在 C 类型菜单 118(文件管理) 上，授予 boss 会渲染「系统管理 > 文件管理」，
--            违反 micro_lite「仅经营概览+活动管理」隔离要求；M19_05 只补了 1131(upload)，未覆盖回显/删除
--      修复：① 【Java】SysOssController.listByIds 放宽为 SaMode.OR(system:oss:list, system:oss:query)；
--            ② role_template micro/micro_pro/micro_lite boss 追加 system:oss:query + system:oss:remove；
--            ③ 所有小微版 boss sys_role_menu 补录 1130(文件查询,F) + 1133(文件删除,F)
--      ★前置：[66]M19_05（boss OSS 上传权限 + 系统管理菜单根治）
--      ★幂等：JSON_CONTAINS 守卫 + INSERT IGNORE + NOT EXISTS + 条件 DELETE，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M19_06_boss_oss_query_remove_permission.sql;

-- ---------------------------------------------------------------------
-- 阶段二十一：活动设置增强——数据模型 + 菜单 + 权限（v1.52 新增）
-- ---------------------------------------------------------------------
-- [68] ★T19.1~T19.5 活动设置增强数据模型（v1.52，2026-09-21）
--      依据：《活动功能需求草稿》v0.1
--      内容：① promotion_activity 补 8 列（auto_title/bg_image_url/voice_url/
--               qrcode_url/recommend_scene/selected_project_id/selected_room_ids/selected_staff_ids）；
--            ② 活动设置菜单 7156(活动设置,C)/7157(查看设置,F)/7158(保存设置,F)，挂 7150 活动管理目录下；
--            ③ role_template boss(micro/micro_pro/micro_lite) 追加 micro:activity:setting:list/edit；
--            ④ 存量租户 boss sys_role_menu 补录 7156~7158 + M 祖先目录（递归 CTE，F 按钮不触发祖先补全）。
--      ★背景图仅对当前活动生效，不跨活动复用（对齐需求草稿 §3.6）。
--      ★语音为单向播放（约 3 分钟），不做语音互动，仅活动介绍。
--      ★前置：[26]M15_11（promotion_activity 表）+ [36]M15_19（logo_url/theme_key）+ [62]M19_01（boss 推广菜单）
--      ★幂等：条件 ALTER（INFORMATION_SCHEMA 守卫）+ INSERT IGNORE + JSON_CONTAINS 守卫
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M19_07_activity_setting_enhance.sql;

-- ---------------------------------------------------------------------
-- 阶段二十二：Boss PC「我的推广码」菜单（v1.56 新增）
-- ---------------------------------------------------------------------
-- [69] ★Boss PC「我的推广码」菜单（v1.56，2026-09-25）
--      需求：boss PC 菜单增加「我的推广码」页面，展示门店/个人推广二维码，
--            供手机扫描进入微信小程序。免费版和付费版都需要加上。
--      内容：① sys_menu 新增 menu_id 7159（我的推广码，C 类型，parent=7150 推广发布，
--               perms=micro:myPromo:view，client_type=pc，component=micro/promo/myQrcode/index）；
--            ② role_template micro/micro_pro/micro_lite boss menu_keys 追加 micro:myPromo:view；
--            ③ 存量租户 boss sys_role_menu 补录 7159 + M 祖先目录（递归 CTE）；
--            ④ 后端 MicroPromotionCodeController generate/my 端点放宽为 SaMode.OR
--               （micro:distribution:* OR micro:myPromo:view），免费版 boss 无需完整 distribution 权限即可访问。
--      ★前置：[21]M15_06（菜单 7000~7499 + client_type）+ [68]M19_07（活动设置菜单 7156~7158，避让）
--      ★幂等：INSERT IGNORE + JSON_CONTAINS 守卫 + NOT EXISTS，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M19_08_boss_my_promo_qrcode_menu.sql;

-- ---------------------------------------------------------------------
-- 阶段二十三：「活动设置」改名为「微信推广码」+ 页面简化（v1.57 新增）
-- ---------------------------------------------------------------------
-- [70] ★「活动设置」改名为「微信推广码」（v1.57，2026-09-25）
--      需求：将「活动设置」页面改名为「微信推广码」，页面只展示微信二维码，
--            供手机扫码进入活动页面。
--      内容： sys_menu 7156 改名：活动设置 → 微信推广码，component 改为 micro/activity/wechatQrcode/index；
--            ② 权限 perms 改名：micro:activity:setting:* → micro:wechat:qrcode:*；
--            ③ role_template boss 权限同步更新；
--             前端新增 wechatQrcode/index.vue（活动选择 + 二维码展示 + 手机扫码说明）。
--      ★前置：[68]M19_07（活动设置菜单 7156~7158）
--      ★幂等：UPDATE 操作天然幂等，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M19_09_wechat_qrcode_rename.sql;

-- ---------------------------------------------------------------------
-- 阶段二十四：micro_pro boss 角色菜单 V3.7 完整对齐（v1.59 新增）
-- ---------------------------------------------------------------------
-- [71] ★micro_pro boss 角色菜单修复（v1.59，2026-09-26）
--      需求：V3.7 §6.5 定义 boss PC 菜单骨架（经营驾驶舱/推广管理/活动预约管理/数据与报表/我的），
--            但实际数据库中：① role_template 使用过时权限字符串导致 boss 只能看到 7 个菜单；
--            ② sys_role_menu 完全没有 M 类型目录菜单导致侧边栏无法展示树形结构；
--            ③ 7159/7650/7698 三个菜单从未创建；
--            ④ 评价管理/员工业绩榜/语音合规记录等直接挂在 7100 顶级而非「数据与报表」目录下。
--      内容：补建 3 个缺失菜单 + 新建 7190 数据与报表 M 目录 + 移动 5 个散落菜单至 7190 下 +
--            role_template 31 权限键重写 + sys_role_menu 清理重授（含 8 个 M 目录显式补入）。
--      ★前置：[68]M19_07（活动设置菜单）+ [69]M19_08（我的推广码 7159 前置 DELETE）
--      ★幂等：DELETE+INSERT + UPDATE 操作天然幂等，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M20_01_boss_pro_menu_fix.sql;

-- [72] ★预约列表页权限 403 修复 + 订单详情菜单隐藏（v1.60，2026-09-26）
--      问题：① 预约列表页 created() 调 onSaleService() 报 403「无此权限：micro:service:list」；
--            ② 调 listUserRole() 报 403「无此权限：micro:userRole:list」；
--             订单详情（7116）visible='0' 出现在侧边栏，应仅通过 router.push 跳转访问。
--      根因：M20_01 重写 role_template 31 权限键时遗漏 micro:service/list + micro:userRole:list；
--            订单详情菜单 visible 未设为隐藏。
--      修复：① 【Java】ServiceItemController.onSale() → SaMode.OR(micro:service:list, micro:booking:list)；
--            ② 【Java】UserRoleController 新增 /staff 端点 → SaMode.OR(micro:booking:list, micro:settle:list)；
--            ③ 【前端】booking/settle 页面改调 listStaff 替代 listUserRole；
--            ④ 【SQL】sys_menu 7116 visible '0'→'1'（隐藏）。
--      ★前置：[71]M20_01（micro_pro boss 菜单对齐）
--      ★幂等：UPDATE ... WHERE visible='0' 天然幂等，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M20_02_booking_page_permission_and_menu_fix.sql;

-- ---------------------------------------------------------------------
-- 阶段二十五：移除「我的推广码」菜单 + 公用功能去除权限校验（v1.61 新增）
-- ---------------------------------------------------------------------
-- [73] ★移除「我的推广码」菜单 + 免费版/付费版公用功能去除权限校验（v1.61，2026-09-26）
--      需求：① 「我的推广码」菜单不需要，移除；
--            ② 免费版和付费版公用的功能去除权限码校验，避免反复 403。
--      内容：① 删除菜单 7159（我的推广码）+ sys_role_menu 关联；
--            ② role_template 各版本 boss 移除 micro:myPromo:view；
--            ③ MicroActivitySettingController 全部端点移除 @SaCheckPermission（公用：微信推广码/选择器）；
--            ④ MicroRewardConfigController GET/PUT 移除 @SaCheckPermission（公用：拉新奖励配置）；
--            ⑤ MicroPromotionCodeController generate/my 移除 SaMode.OR（菜单已删）；
--            ⑥ 删除前端 myQrcode/index.vue。
--      ★前置：[71]M20_01（micro_pro boss 菜单对齐，含 7159 重建）
--      ★幂等：DELETE WHERE + JSON_REMOVE + 条件判断，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M20_03_remove_my_promo_menu_and_shared_permissions.sql;

-- ---------------------------------------------------------------------
-- 阶段二十六：micro_pro boss 权限补全 + 推广分销菜单类型修正 + License 小微版识别兜底（v1.62 新增）
-- ---------------------------------------------------------------------
-- [74] ★micro_pro boss 权限补全 + 推广分销菜单类型修正（v1.62，2026-09-26）
--      问题：① 活动管理页面无「发布活动」按钮（role_template 缺 micro:promo:add/edit/remove/poster）；
--            ② 推广分销管理点击无反应（菜单 7180 为 M 目录无 component，点击仅展开/折叠）；
--            ③ 固定支出菜单不可见（role_template 缺 micro:pro:cost:list/add/edit/remove）；
--            ④ 评价管理报 license 错误（小微版付费版应能使用 FEAT_RATING）。
--      修复：① 【SQL】role_template micro_pro boss menu_keys 追加 8 个缺失权限键（31→39）；
--            ② 【SQL】sys_menu 7180 从 M 目录改为 C 类型（component=micro/distribution/index）；
--            ③ 【SQL】存量 micro_pro 租户 boss sys_role_menu 补录活动按钮+固定支出+7180；
--            ④ 【Java】LicenseServiceImpl.getTenantActiveFeatures() 增加 sys_tenant.version_type 回退，
--               小微版无 License 或 edition 不匹配时使用 MICRO_FEATURES；
--            ⑤ 【Java】LicenseServiceImpl.getTenantLicenseInfo() 同步适配小微版无 License 场景。
--            ⑥ 【前端】distribution/index.vue 路由路径修正（salesman→data, customer→rewardConfig）。
--      ★前置：[71]M20_01（micro_pro boss 菜单对齐）、[73]M20_03（移除我的推广码）
--      ★幂等：UPDATE + INSERT IGNORE，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M20_04_micro_pro_boss_permission_and_menu_fix.sql;

-- ---------------------------------------------------------------------
-- 阶段二十七：micro_pro 业态包能力开关补全 + 推广分销/固定支出菜单重构（v1.63 新增）
-- ---------------------------------------------------------------------
-- [75] ★biz_bundle 能力开关补全（根因）+ 推广分销/固定支出菜单重构（v1.63，2026-09-27）
--      问题：① micro_pro 付费版老板打开「推广分销管理」弹「功能未授权：当前版本不支持此功能，请升级License」；
--            ② 菜单不正确：7101 命名、推广分销子树多个死链菜单、固定支出管理菜单缺失。
--      根因：LicenseServiceImpl.getTenantActiveFeatures() 三维交集中，biz_bundle.feature_flags（第三维 applyBizFilter）
--            未声明小微版 FEAT_*，导致 License 已含的 FEAT_PROMOTION_DISTRIBUTION 被剔除 → @RequireFeature 拦截。
--      修复：① 【SQL根因】biz_bundle 三业态包 feature_flags 幂等补齐 4 个 micro FEAT_*（JSON_ARRAY_APPEND + NOT JSON_CONTAINS 守卫）；
--            ② 【SQL】sys_menu 7101 改名经营驾驶舱；7180 改回 M 目录，7181/7182/7183 重构为看板/业务员记录/顾客记录（指向真实组件）；
--               删除死链 7186/7187/7188；重建固定支出 7195~7199（挂 7190）；
--            ③ 【SQL】role_template micro_pro boss menu_keys 39→37（移除 data/attribution/firstOrderConfig/code，新增 salesman/customer）；
--            ④ 【SQL】存量 micro_pro boss sys_role_menu 补录新菜单 + 通用 F 按钮补授权（7100~7199 区间）；
--            ⑤ 【前端】distribution/index.vue 卡片跳转修正为 /microOps/distribution/salesmanRecords|customerRecords。
--      ★前置：[71]M20_01（7190 数据与报表目录）、[74]M20_04（7180 类型/role_template 39 键）
--      ★幂等：UPDATE + DELETE/INSERT + INSERT IGNORE + JSON_CONTAINS 守卫，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照 + 前端重新拉取路由
source M20_05_micro_pro_feature_bundle_and_menu_fix.sql;

-- ---------------------------------------------------------------------
-- 阶段二十八：固定支出表修复与增强（v1.64 新增）
-- ---------------------------------------------------------------------
-- [76] ★monthly_fixed_cost 修复 create_by/update_by 类型（bigint→varchar(64)）
--      + 新增 period_rule_id 列（关联周期规则，boss 选择标签后自动带出周期规则按周期分摊）
--      根因：BaseEntity.createBy 为 String 类型，MetaObjectHandler 填充用户名，
--            原 bigint 列导致 "Incorrect integer value: 'lijie' for column 'create_by'"
--      ★前置：[38]M15_21（monthly_fixed_cost 建表）+ [44]M18_02（period_rule 建表）
--      ★幂等：INFORMATION_SCHEMA 条件判断，可重复执行
source M20_06_monthly_fixed_cost_fix_and_enhance.sql;

-- ---------------------------------------------------------------------
-- 阶段二十九：语音合规增强——最大语音时长配置（v1.64 新增）
-- ---------------------------------------------------------------------
-- [77] ★T18 增强 voice_compliance_rule 补 max_voice_duration 列（M20_07，v1.64，2026-09-30）
--      租户管理员可配置活动语音最大时长（秒，默认 180=3 分钟，0=不限）；
--      Boss 发布/编辑活动时前端用 Web Audio API 校验音频时长不超限；
--      voiceEnabled 接口扩展返回 { enabled, maxVoiceDuration }。
--      ★前置：[43]M18_01（voice_compliance_rule 建表）
--      ★幂等：INFORMATION_SCHEMA 条件 ALTER，可重复执行
source M20_07_voice_compliance_max_duration.sql;

-- ---------------------------------------------------------------------
-- 阶段三十：语音合规增强——完整合规校验流程（v1.65 新增）
-- ---------------------------------------------------------------------
-- [78] ★T18 增强 voice_compliance_hit 补 result 列 + 触发校验流程（M20_08，v1.65，2026-09-30）
--      语音上传后需进行合规校验，且所有语音合规记录都应记录是否合规（PASS/FAIL）；
--      流程：① ASR 转写 → ② 敏感词检测 → ③ 写 voice_compliance_hit 记录（含 result 列）；
--      Boss 发布/编辑活动时后端自动触发校验，违规则拒绝保存并提示重新录制。
--      ★前置：[43]M18_01（voice_compliance_hit 建表）+ [77]M20_07（max_voice_duration）
--      ★幂等：INFORMATION_SCHEMA 条件 ALTER，可重复执行
source M20_08_voice_compliance_result.sql;

-- ---------------------------------------------------------------------
-- 阶段三十一：PC 菜单拍平·保留一层（v1.66 新增）
-- ---------------------------------------------------------------------
-- [79] ★V3.7 PC 菜单拍平：目录下仅一个可见子菜单时提升到上级（M20_09，v1.66，2026-09-30）
--      消除「目录 → 唯一子菜单」无意义点击层，共 5 个候选（6000/7020/7050/7110/7195）；
--      含隐藏子页的目录（7050/7110）隐藏保留不删（保既有路由），空目录（6000/7020/7195）删除。
--      ★前置：[75]M20_05（7195~7199 固定支出菜单重建）、[71]M20_01（7190 数据与报表目录）
--      ★前端联动：7111 path 改 bookingList，已同步更新 ruoyi-ui 4 处硬编码 /microOps/booking/list→/microOps/bookingList
--      ★幂等：全部 UPDATE/DELETE 按 menu_id 定位，重复执行结果一致
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照 + 前端重新拉取路由
source M20_09_pc_menu_flatten_single_child.sql;

-- ---------------------------------------------------------------------
-- 阶段三十二：异常处理中心「处理」按钮权限补全（v1.67 新增）
-- ---------------------------------------------------------------------
-- [80] ★异常处理中心「处理」按钮 403 修复（M20_10，v1.67，2026-10-02）
--      问题：PUT /micro/exceptionCenter/resolve 报「无此权限：micro:exception:resolve」。
--      根因：@SaCheckPermission 所用权限串无对应 sys_menu.perms 行（仅 7698 C 菜单承载
--            micro:exception:list），role_template menu_keys 亦无此键 → 权限永远无法授予 → 必 403。
--      修复：新增 F 按钮 7699（parent=7698）+ role_template 幂等追加键 + 超管/存量角色补录 sys_role_menu。
--      ★前置：[71]M20_01（7698 异常处理中心菜单）
--      ★幂等：DELETE+INSERT / INSERT IGNORE / JSON_CONTAINS 守卫，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M20_10_exception_resolve_permission.sql;

-- ---------------------------------------------------------------------
-- 阶段三十三：小微版租户 version_type 为 NULL 修复（v1.68 新增）
-- ---------------------------------------------------------------------
-- [81] ★小微版租户 version_type 为 NULL 修复（M20_11，v1.68，2026-10-02）
--      问题：租户登录后 menus=[] 且首页不显示经营驾驶舱。
--      根因：sys_tenant.version_type 为 NULL → 登录响应无 versionType → 前端 isPro=false；
--            同时 copyMenuTemplateForMicro 查 role_template 不匹配 → sys_role_menu 缺 M/C 菜单。
--      修复：① 批量补设 version_type='micro_pro'（tenant_type='micro' 且 version_type 为 NULL）；
--            ② role_template.menu_keys → sys_menu.perms 补录 C/F 类型菜单；
--            ③ 保底 M 类型根目录 7100 + 中间 M 祖先目录补全。
--      ★前置：[71]M20_01（micro_pro boss 菜单骨架）+ [75]M20_05（role_template 37 权限键）
--      ★幂等：UPDATE WHERE NULL + INSERT IGNORE，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M20_11_tenant_version_type_null_fix.sql;

-- ---------------------------------------------------------------------
-- 阶段三十四：业态包菜单声明修复（v1.69 新增）
-- ---------------------------------------------------------------------
-- [82] ★biz_bundle.fund_control.menus 业态菜单声明修复（M20_12，v1.69，2026-10-02）
--      问题：boss 登录后 menus 仅含 MiniBoss/Report，缺少 7100+ 区段经营驾驶舱等菜单。
--      根因：biz_bundle.fund_control.menus 包含企业版旧 ID [2000,3000,3201,...]，
--            与小微版 7100+ 菜单无交集 → applyBizFilter 过滤异常。
--      修复：fund_control 是小微版默认业态包，menus 置 NULL 跳过业态过滤。
--      ★前置：[81]M20_11（version_type NULL 修复）
--      ★幂等：UPDATE 幂等
--      ★执行后请重启后端（清除 BizBundleService 内存缓存 menuCache）
source M20_12_biz_bundle_fund_control_menus_fix.sql;

-- [83] M20_13_activity_end_job.sql（活动自动结束定时任务，T15.28，v1.70）
--      问题：活动 endTime 过期后无自动结束机制，已结束的活动仍出现在小程序「最新活动」浏览页。
--      修复：① listPublished 增加 endTime 过滤；② activityEndJob 每 6 小时扫描，PUBLISHED→ENDED；
--            ③ 仅处理 PUBLISHED 状态，Boss 手动取消/已结束的活动不受影响。
--      ★前置：PromotionActivityServiceImpl.autoEndActivities() 方法已交付
--      ★幂等：DELETE+INSERT 幂等
source M20_13_activity_end_job.sql;

-- [84] M20_14_customer_role_permission_fix.sql（顾客小程序权限 403 修复，M20_14，v1.71）
--      问题：GET /micro/customer/shop 报「无此权限：micro:customer:shop」。
--      根因：顾客角色(9207)无 sys_role_menu + 顾客用户无 sys_user_role，权限链双层断裂。
--      修复：①补录 sys_role_menu(9207→micro_pro customer menu_keys + M 目录 7400)；
--            ②补录小微版无角色用户的 sys_user_role(→9207)；
--            ③Java registerByPhone() 自动注册时自动分配 customer 角色。
--      ★前置：[22]M15_07（customer 角色 9207 已创建）+ [25]M15_10（micro_pro customer role_template）
--      ★幂等：INSERT IGNORE 幂等
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M20_14_customer_role_permission_fix.sql;

-- [85] M21_01_self_booking.sql（自助预约菜单+功能开关，M21，v1.72）
--      T21.1~T21.4 顾客小程序自助预约（项目/房间/助教三类入口+可选关联资源+提前一天限制）。
--      ① feature_flag 第 38 项 FEAT_SELF_BOOKING（组 E，micro_enabled=1，仅 micro_pro）；
--      ② sys_menu 7200~7202（C+F+F，client_type=mini，挂 7400）；
--      ③ role_template micro_pro customer 追加 3 权限键 + 存量 sys_role_menu 补录。
--      ★前置：[22]M15_07 + [25]M15_10
--      ★幂等：先删后插 + INSERT IGNORE
source M21_01_self_booking.sql;

-- [86] M21_02_resource_status.sql（资源状态流转+Boss PC 状态管理，M21，v1.72）
--      T21.5~T21.6 资源状态三态流转（idle→reserved→in_use→idle）+ Boss PC 管理页。
--      ① service_item 补 resource_status 列（幂等 ALTER TABLE）；
--      ② 新建 resource_status_log 表；
--      ③ sys_menu 7203~7206（C+C+F+F，client_type=pc，挂 7100）；
--      ④ role_template boss 追加 4 权限键 + 存量 sys_role_menu 补录。
--      ★前置：[85]M21_01
--      ★幂等：ALTER TABLE 幂等 + 先删后插 + INSERT IGNORE
source M21_02_resource_status.sql;

-- [87] M21_03_staff_pricing.sql（助教价格表+资源价格设置菜单，M21，v1.73）
--      T21.7 Boss PC 资源价格设置（助教+房间小时单价，顾客可见）。
--      ① 新建 staff_pricing 表（UNIQUE(tenant_id, user_id)）；
--      ② sys_menu 7207~7209（C+F+F，client_type=pc，挂 7100）；
--      ③ role_template boss 追加 3 权限键 + 存量 sys_role_menu 补录。
--      ★房间价格复用 service_item.price，无需新建表。
--      ★前置：[85]M21_01（功能开关 FEAT_SELF_BOOKING）
--      ★幂等：DROP IF EXISTS + CREATE TABLE + INSERT IGNORE
source M21_03_staff_pricing.sql;

-- [88] M21_04_fix_audit_columns.sql（BaseEntity 审计字段补全，M21，v1.74 修复）
--      修复 service_item / resource_status_log 缺少 update_by/update_time 导致 Unknown column 报错。
--      ① service_item 幂等补 update_by/update_time + create_by 类型兜底；
--      ② resource_status_log 修复 create_by 类型（BIGINT→VARCHAR(64)）+ 幂等补 update_by/update_time。
--      ★前置：[86]M21_02（resource_status_log 建表）+ M15_04（service_item 建表）
--      ★幂等：ALTER TABLE INFORMATION_SCHEMA 幂等保护
source M21_04_fix_audit_columns.sql;

-- [89] M21_05_boss_oss_permission_fix.sql（存量 boss OSS 上传/查询/删除权限补录，M21，v1.75）
--      问题：部分存量小微版租户 boss 角色 sys_role_menu 缺少 1130/1131/1133，
--            发布活动上传图片报 403「无此权限：system:oss:upload」。
--      根因：租户创建时 role_template 尚未含 OSS 权限，后续 M19_05/M19_06/M20_01 更新了模板，
--            但存量 sys_role_menu 未同步补录。
--      修复：为所有小微版 boss 补录 1130(文件查询)/1131(文件上传)/1133(文件删除)。
--      ★前置：[66]M19_05（role_template 源头含 OSS 权限）
--      ★幂等：INSERT IGNORE + NOT EXISTS，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M21_05_boss_oss_permission_fix.sql;

-- [90] M21_06_fix_room_status_duplicate.sql（房间状态管理重复数据清理，M21，v1.75 修复）
--      问题：Boss PC 房间状态管理页面房间重复显示 2 次。
--      原因：自愈多次触发或手动操作可能产生同名房间记录。
--      修复：①后端 listRoomResourceStatus() 按名称去重（防御性）；
--            ②SQL 清理 service_item 表重复房间记录（保留 MIN(id)）。
--      ★前置：[86]M21_02（resource_status 列）+ M15_04（service_item 建表）
--      ★幂等：DELETE 使用 MIN(id) 保留最早记录
source M21_06_fix_room_status_duplicate.sql;

-- [91] fix_boss_remove_tenant_admin_menu.sql（boss 菜单隔离 + sys_user_role 双表同步修复，v1.76）
--      问题：① boss 登录后侧边栏同时出现「租户管理菜单」和「小微运营」；
--            ② 新增人员被分配 boss 后，登录时仍看到 platform_admin 的菜单。
--      根因：① M15_26 清理脚本在部分环境下未完全生效；
--            ② assign() 只写 user_tenant_role，但登录时 getRolePermission() 回退查 sys_user_role，两表不一致。
--      修复： 从 boss 克隆角色的 sys_role_menu 中移除 7500~7509；
--            ② 按 user_tenant_role 的实际分配重写 sys_user_role 关联；
--            ③ Java 修复 UserTenantRoleServiceImpl.assign() 新增 syncSysUserRole()。
--      ★前置：[59]M15_26（首次清理尝试）+ [34]M15_17（7500~7509 菜单定义）
--      ★幂等：DELETE + INSERT 幂等，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source fix_boss_remove_tenant_admin_menu.sql;

-- [92] M21_07_self_booking_biz_bundle_fix.sql（FEAT_SELF_BOOKING 业态包能力开关补全，M21，v1.77）
--      问题：助教状态管理/房间状态管理/资源价格设置三菜单在 micro_pro 付费版报「小微版无此功能权限」。
--      根因：biz_bundle 三业态包 feature_flags 缺 FEAT_SELF_BOOKING（M21_01 新增开关时未同步补录），
--            applyBizFilter 第三维剔除 → FeatureInterceptor MICRO 分支抛异常（与 M20_05 FEAT_RATING 同源）。
--      修复：① biz_bundle 全部业态包幂等补录 FEAT_SELF_BOOKING；
--            ② 防御性重补 M20_05 四个小微开关（覆盖新建自定义业态包）；
--            ③ feature_flag FEAT_SELF_BOOKING 先删后插兜底。
--      ★前置：[85]M21_01（功能开关 FEAT_SELF_BOOKING）+ [75]M20_05（业态包补录模式）
--      ★幂等：先删后插 + NOT JSON_CONTAINS 守卫，可重复执行
--      ★执行后请重启后端（清除 BizBundle/FeatureFlag/License 三层内存缓存）
source M21_07_self_booking_biz_bundle_fix.sql;

-- [93] M22_01_staff_schedule.sql（员工排班管理建表+菜单+功能开关，M22，v1.80）
--      T22.1/T22.6 Boss 按周/月编排员工排班（早班 morning / 晚班 evening，起止时间 Boss 自定），
--      草稿 draft → 发布 published，发布后仍可修正并写 schedule_change_log 留痕；员工/顾客仅可见 published。
--      ① 新建 staff_schedule 表（UNIQUE(tenant_id, staff_id, schedule_date, shift_type) 防重复排班）；
--      ② 新建 schedule_change_log 表（发布后修正留痕）；
--      ③ feature_flag 新增第 39 项 FEAT_STAFF_SCHEDULE（组 E，micro_enabled=1，仅小微版；micro_lite 不授权）；
--      ④ biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_STAFF_SCHEDULE（与 M21_07 同模式）；
--      ⑤ sys_menu 7230~7235（员工排班 C + 3 F 按钮 + 员工/顾客小程序各 1 F 按钮，避让 7210~7213）；
--      ⑥ role_template micro/micro_pro 的 boss(+4)/salesman(my)/customer(store) 追加权限键 + 存量 sys_role_menu 补录。
--      ★前置：[25]M15_10（role_template 配方）+ M15_06（sys_menu 7000~7499 区段）+ [85]M21_01（FEAT_SELF_BOOKING 参考模式）
--      ★幂等：DROP IF EXISTS 建表 + 先删后插（菜单/开关）+ NOT JSON_CONTAINS 守卫（模板追加），可重复执行
--      ★执行后请重启后端（清除 BizBundle/FeatureFlag/License 三层内存缓存）+ 退出重新登录刷新权限快照
source M22_01_staff_schedule.sql;

-- [94] M22_02_fix_audit_columns_bigint_to_varchar.sql（staff_pricing/staff_schedule 审计字段类型修复，M22，v1.78 修复）
--      问题：保存助教价格报 Incorrect integer value: 'lijie' for column 'create_by'。
--      根因：两表实体继承 TenantEntity→BaseEntity，create_by/update_by 应为 VARCHAR(64)（存用户名），
--            但建表脚本（M21_03/M22_01）误定义为 BIGINT，写入用户名字符串报错（与 M21_04 同源缺陷）。
--      修复：staff_pricing + staff_schedule 的 create_by/update_by 由 BIGINT 修正为 VARCHAR(64)。
--      ★前置：[87]M21_03（staff_pricing 建表）+ [93]M22_01（staff_schedule 建表）
--      ★幂等：ALTER TABLE INFORMATION_SCHEMA 类型守卫，可重复执行
source M22_02_fix_audit_columns_bigint_to_varchar.sql;

-- [95] M21_08_resource_inuse_job.sql（资源状态自动流转定时任务，M21，v1.79）
--      T21.8：补全 M21 资源三态中此前未落地的 reserved→in_use（服务开始）与 in_use→idle（结束）写入触发点。
--      新增 resourceInUseFlowJob（每 10 分钟，@TenantJob(versions={"micro"})）：扫描当天预约，
--      按时间窗（bookingTime + remark.bookingTimeSlot，缺失兜底 2 小时）+ 订单状态推进资源状态并写 resource_status_log；
--      幂等：仅 reserved→in_use 与 in_use→idle，当前状态==目标状态则跳过；助教状态由最新日志推导。
--      ★前置：[86]M21_02（resource_status 列 + resource_status_log 表）+ advanceResourceStatusByTime() + resourceInUseFlowJob Handler Bean
--      ★幂等：先删后插（按 executor_handler），可重复执行
source M21_08_resource_inuse_job.sql;

-- [96] M22_03_staff_schedule_perm_fix.sql（员工排班·按钮权限与功能开关兜底修复，M22 补漏）
--      修复①：EditionFeatureConfig.MICRO_FEATURES 代码侧补 FEAT_STAFF_SCHEDULE（三维交集第①维）；
--              本脚本兜底 feature_flag(micro_enabled=1) + biz_bundle 两维数据。
--      修复②：boss 角色 sys_role_menu 补 7231/7232/7233 F 按钮（前端 v-hasPermi 依赖），
--              解决排班页缺「批量创建/发布本期/班次时间设定」按钮问题。
--      ★前置：[93]M22_01（菜单 7230~7235 已建）
--      ★幂等：INSERT IGNORE / NOT EXISTS / NOT JSON_CONTAINS 守卫，可重复执行
--      ★执行后请重启后端（清 FeatureFlag/BizBundle/License 缓存）+ 退出重新登录刷新权限快照
source M22_03_staff_schedule_perm_fix.sql;

-- [97] M22_04_service_item_extra_config.sql（service_item 补 extra_config 列，M22，v1.81）
--      service_item 补 TEXT 列 extra_config（业态差异配置 JSON：quota/tableCount/perTable 等，T11 报名流程依赖）。
--      promotion_activity.selected_project_id 指向 service_item.id，报名需读取 extra_config 中的配额/桌数等。
--      ★前置：[19]M15_04（service_item 建表）
--      ★幂等：INFORMATION_SCHEMA 条件 ALTER，可重复执行
source M22_04_service_item_extra_config.sql;

-- [98] M22_05_schedule_change_log_audit_fix.sql（schedule_change_log 审计字段修复，M22，v1.81）
--      修复 schedule_change_log 缺失 BaseEntity 四审计字段（create_by/create_time/update_by/update_time），
--      导致 INSERT 时 Unknown column 'create_by' 运行时错误。
--      ★前置：[93]M22_01（schedule_change_log 建表）
--      ★幂等：INFORMATION_SCHEMA 条件 ALTER，可重复执行
source M22_05_schedule_change_log_audit_fix.sql;

-- [99] M22_06_activity_pricing_enhance.sql（活动费用与参与增强，M22，v1.81）
--      T22 Boss 创建活动费用模型：房间单价×时长 + 助教单价×时长 = 总价，Boss 可自定义价格覆盖自动计算值；
--      总价 / 设定人数 = 每人应付金额；开团前 30 分钟系统消息通知每人应付金额（到店支付）；
--      Boss 可锁定活动（锁定后不可报名/取消）；顾客/助教活动列表扩展为展示所有参与的活动。
--      ① promotion_activity 补 4 列（custom_price/auto_calculated_price/participant_count/is_locked）；
--      ② project_signup 补 pay_status 列（PENDING_PAY/PAID/REMIND_SENT）；
--      ③ sys_menu 新增 7159（锁定活动 F 按钮，micro:promo:lock，挂 7150 活动管理目录下）；
--      ④ role_template boss 追加 micro:promo:lock + 存量 sys_role_menu 补录；
--      ⑤ xxl_job_info 注册 activityPayReminderJob（每 5 分钟，@TenantJob(versions={"micro"})）。
--      ★前置：[26]M15_11（promotion_activity 表）+ [87]M21_03（staff_pricing）+ [93]M22_01（staff_schedule）
--      ★幂等：条件 ALTER + 先删后插 + JSON_CONTAINS 守卫，可重复执行
--      ★执行后请退出重新登录，刷新 Sa-Token 权限快照
source M22_06_activity_pricing_enhance.sql;

-- [100] M24_01_check_in.sql（员工打卡建表+菜单+功能开关，M24，v1.82）
--      T24.1/T24.6 员工打卡：H5 拍照+时间+地点+水印+迟到判定+三级功能开关链路。
--      ① 新建 check_in_record 表（UNIQUE(tenant_id,activity_id,staff_user_id,check_in_time) 防重复）；
--      ② 新建 check_in_config 表（UNIQUE(tenant_id)）；
--      ③ industry_template 补 check_in_enabled 列（三级开关之平台行业级）；
--      ④ sys_tenant 补 check_in_enabled 列（三级开关之租户级）；
--      ⑤ feature_flag 新增第 40 项 FEAT_CHECK_IN（组 E，micro_enabled=1，仅小微版；micro_lite 不授权）；
--      ⑥ biz_bundle 全部业态包幂等补录 FEAT_CHECK_IN；
--      ⑦ sys_menu 7250~7254（打卡记录 C + 补录/导出 F + 员工小程序 submit/my F）；
--      ⑧ role_template micro/micro_pro boss(+3)/salesman(+2) 追加权限键 + 存量 sys_role_menu 补录。
--      ★前置：[25]M15_10（role_template 配方）+ M15_06（sys_menu 7000~7499 区段）+ [93]M22_01（FEAT_STAFF_SCHEDULE 参考模式）
--      ★幂等：DROP IF EXISTS 建表 + 先删后插（菜单/开关）+ NOT JSON_CONTAINS 守卫（模板追加），可重复执行
--      ★执行后请重启后端（清除 BizBundle/FeatureFlag/License 三层内存缓存）+ 退出重新登录刷新权限快照
source M24_01_check_in.sql;

-- [101] M25_01_complaint_supervision.sql（投诉与平台监管建表+菜单+功能开关，M25，v1.82）
--      T25.1/T25.7 投诉与平台监管：顾客双渠道投诉+合规协议签署+平台 Admin 监管+活动留痕。
--      ① 新建 complaint_record 表（投诉记录，渠道 BOSS/PLATFORM，状态 PENDING/PROCESSING/RESOLVED/CLOSED）；
--      ② 新建 compliance_agreement 表（UNIQUE(tenant_id,agreement_version) 防重复签署）；
--      ③ feature_flag 新增第 41 项 FEAT_COMPLAINT（组 E，micro_enabled=1，全版本可用含 micro_lite）；
--      ④ biz_bundle 全部业态包幂等补录 FEAT_COMPLAINT；
--      ⑤ sys_menu 7260~7271（投诉监管 M 目录 + Boss PC 投诉列表/处理 + platform_admin 监管台/停止/协议/留痕）；
--      ⑥ role_template platform_admin(+6)/boss(+2，含 micro_lite) 追加权限键 + 存量 sys_role_menu 补录。
--      ★前置：[25]M15_10（role_template 配方）+ M15_06（sys_menu 7000~7499 区段）+ [100]M24_01（FEAT_CHECK_IN 参考模式）
--      ★幂等：DROP IF EXISTS 建表 + 先删后插（菜单/开关）+ NOT JSON_CONTAINS 守卫（模板追加），可重复执行
--      ★执行后请重启后端（清除 BizBundle/FeatureFlag/License 三层内存缓存）+ 退出重新登录刷新权限快照
source M25_01_complaint_supervision.sql;

-- [102] M25_02_fund_platform_menu_final_fix.sql（「资金平台管理菜单」终极兜底修复，M25，v1.82）
--      用户反馈：执行完 00_执行顺序.sql 后 admin 侧边栏仍看不到 menu 1300「资金平台管理菜单」。
--      数据库实时核查：1300 本体存在、role_menu (1,1300) 存在、14 子菜单齐全，
--      但发现 3 处隐蔽数据缺陷 + 1 处授权空洞足以破坏前端渲染。
--      修复：① 幂等重建 menu 1300（icon 'component'→'money'）；
--            ② menu 7260 path ''→'complaint'（根因 R1：空 path 破坏 vue-router 树）；
--            ③ menu 6160 path 'compliance2'→'regulatory'（根因 R2：避开前端 filterRoutesByFeature）；
--            ④ 补齐 role_id=1 对 1300 全部子菜单+孙菜单授权（根因 R3：re-parent 菜单未授权）；
--            ⑤ 强制 1300 子菜单 visible='0'/status='0'/client_type='pc'。
--      ★前置：[27]M15_12 + [九-1]M10_01 + [九-2]M10_02 + [79]M20_09 + [101]M25_01
--      ★幂等：ON DUPLICATE KEY UPDATE + UPDATE WHERE 条件守卫 + INSERT IGNORE，可重复执行
--      ★执行后必做：① 重启后端；② 浏览器 Ctrl+F5 硬刷新；③ 退出重登 admin
source M25_02_fund_platform_menu_final_fix.sql;

-- [103] M26_01_sensitive_word_activation.sql（敏感词功能激活与旧表数据补迁，M26，v1.83）
--      前端敏感词管理页已切换至新接口 /compliance/tenant/sensitiveWord/*（读写 sensitive_word 表），
--      但存在两处数据缺口导致功能未生效：
--      ① FEAT_SENSITIVE_WORD 功能开关可能处于关闭态；
--      ② 旧表 sensitive_word_library 中 M18_11 执行后通过旧接口添加的词未迁移到新表。
--      修复：① 幂等重assert FEAT_SENSITIVE_WORD 开关（status='0' + micro_enabled=1）；
--            ② 补迁 sensitive_word_library 中尚未迁入的有效词（INSERT IGNORE + NOT EXISTS 幂等）；
--            ③ 补录 biz_bundle 业态包 feature_flags 确保含 FEAT_SENSITIVE_WORD（防升级拦截）。
--      ★前置：[54]M18_10 + [55]M18_11
--      ★幂等：UPDATE WHERE 条件守卫 + INSERT IGNORE + NOT JSON_CONTAINS，可重复执行
--      ★执行后请重启后端清除 SensitiveWordEngine 内存缓存
source M26_01_sensitive_word_activation.sql;

-- [104] M26_02_sensitive_word_permission_fix.sql（租户敏感词 F 按钮权限落地，M26，v1.83 修复）
--      TenantSensitiveWordController 检查 micro:sensitiveWord:list/add/edit/remove/query/hitLog，
--      但菜单 7610 的 perms 只有 micro:compliance:config:sensitiveWord，权限键完全不匹配。
--      M18_13 v1.57 扁平化时删除了 7680~7683（原计划的 F 按钮），导致权限链断裂。
--      修复：① 补建 6 个 F 按钮菜单（7611~7616）perms 与 Controller 严格对齐；
--            ② role_template platform_admin（micro_pro + micro_lite）追加 6 权限键；
--            ③ 存量 platform_admin 克隆角色 sys_role_menu 补录 7611~7616。
--      ★前置：[68]M18_13（菜单 7610）+ [103]M26_01（功能开关激活）
--      ★幂等：先删后插 + NOT JSON_CONTAINS 守卫 + INSERT IGNORE，可重复执行
--      ★执行后请重启后端 + 退出重新登录刷新 Sa-Token 权限快照
source M26_02_sensitive_word_permission_fix.sql;

-- [105] M26_03_resource_price_pricing_type.sql（资源价格设置升级：人员定价 4 种计价模式，M26，v1.85）
--      人员定价对齐 jjadmin 四档计价模式（计时/套餐/计量/一口价）。
--      ① staff_pricing 表新增 pricing_type VARCHAR(16) + extra_config TEXT 两列；
--      ② 存量数据迁移：hourly_rate > 0 的记录默认 pricing_type='duration'；
--      ③ 前端页面重新设计：去掉房间 Tab，人员定价支持 4 种计价模式动态表单；
--      ④ 后端 Controller 去掉房间定价端点，只保留人员定价。
--      ★保留：hourly_rate 列（预约计费兼容，作为基准价）+ 资源价格设置菜单 7207~7209。
--      ★前置：[87]M21_03（staff_pricing 建表 + 资源价格设置菜单）
--      ★幂等：INFORMATION_SCHEMA 条件 ALTER + UPDATE WHERE 守卫，可重复执行
source M26_03_resource_price_pricing_type.sql;

-- [106] M26_04_h5_qrcode_rename.sql（「微信推广码」改名为「H5推广码」+ 改用 H5 二维码，M26，v1.86）
--      ① 菜单 7156 改名：微信推广码 → H5推广码，remark 更新；
--      ② 子按钮菜单 7157/7158 文案同步更新；
--      ③ 前端改用 /micro/promo/code/myQrcode 接口（H5 二维码，与 app 端 boss 推广码一致）。
--      ★前置：[70]M19_09（菜单 7156~7158 已建立）
--      ★幂等：UPDATE 操作天然幂等，可重复执行
source M26_04_h5_qrcode_rename.sql;

-- =====================================================================
-- 验收检查（小微版）
-- =====================================================================
-- ★注：验收项 1~20 覆盖 M15+M16；验收项 18b/19b 覆盖 v1.31 图片管线/主题映射增量；验收项 21 覆盖 M17（v1.32 新增）；验收项 22~26 覆盖 M18（v1.34 新增）；验收项 27 覆盖 M15_22 行业模板种子（v1.35 新增）；验收项 28 覆盖 platform_admin 菜单修复（v1.36 新增）；验收项 29 覆盖 platform_admin 克隆角色+用户角色分配+完整菜单授权修复（v1.37 新增）；验收项 30 覆盖 platform_admin 原子资源管理·项目/房间空间（v1.38 新增）；验收项 31 覆盖 M18 图片合规与全局敏感词（v1.39 新增）；验收项 32 覆盖 micro_lite 免费版 boss 推广管理菜单授权（v1.45 新增）；验收项 33 覆盖 boss 补录 system:oss:upload 权限（v1.46 新增）；验收项 34 覆盖 micro_lite 免费版 boss 菜单精简（v1.47 新增）；验收项 35 覆盖 micro_lite 免费版 boss 活动增删改查权限补全（v1.48 新增）；验收项 36 覆盖 boss OSS 上传权限彻底修复——role_template 源头+sys_role_menu 双层修复（v1.49 新增）；验收项 37 覆盖 micro_lite 免费版 boss 最终态总验收——侧边栏仅经营概览+活动管理、活动 CRUD 齐备、无系统管理菜单（v1.50 新增）；验收项 38 覆盖 boss OSS 图片回显/删除权限补全——listByIds OR 放行 + 1130/1133 F 按钮（v1.51 新增）；验收项 39 覆盖活动设置增强数据模型——promotion_activity 8 列+菜单 7156~7158+role_template boss 活动设置权限（v1.52 新增）；验收项 40 覆盖 M18 最终汇总补丁——M18_12 补建+M18_08 缺陷修复+role_template 三角色完整性+sys_role_menu 全覆盖（v1.55 新增）；验收项 41 覆盖 Boss PC「我的推广码」菜单——menu_id 7159+role_template boss micro:myPromo:view+后端 OR 权限放宽（v1.56 新增）；验收项 42 覆盖「活动设置」改名为「微信推广码」——menu_id 7156 改名+权限改名+页面简化（v1.57 新增）；验收项 43 覆盖 micro_pro boss 角色菜单 V3.7 完整对齐——补建 7159/7650/7698+新建 7190 数据与报表 M 目录+role_template 31 权限键重写+sys_role_menu 8 个 M 目录显式补入（v1.59 新增）
-- 1) 小微版核心表就绪（身份/行业/页面/定价/结算 五域 + 防爽约两表 + 推广活动 + 固定支出 + 评价点赞两表）
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('user_tenant_role','tenant_role_mapping','role_template',
--                      'industry_template','industry_role_map','page_block_config','share_scene',
--                      'service_category','service_item','price_change_log',
--                      'booking_order','staff_settlement','cash_flow','micro_daily_revenue',
--                      'customer_credit','no_show_record','promotion_activity','monthly_fixed_cost',
--                      'user_rating','user_like');
-- 预期：20

-- 2) sys_menu.client_type 端过滤列已补 + 小微版菜单占用 7000~7509 独立区段
-- SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'client_type';         -- 预期：1
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7000 AND 7509;      -- 预期：> 0（小微版菜单）
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id < 7000 AND client_type <> 'pc';  -- 预期：0（存量回填 pc）

-- 3) ★标准角色集共用一张 sys_role 表（不存在「小微版角色表」与「企业版角色表」两张表）
-- SELECT role_id, role_name, role_key FROM sys_role
--  WHERE role_key IN ('boss','salesman','customer') ORDER BY role_id;   -- 预期含 9206/9207
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('micro_role','enterprise_role');                  -- 预期：0

-- 4) ★共享标准角色 tenant_id=NULL（跨租户可见），小微版分档配方完整（micro_lite + micro_pro）
-- SELECT role_key, tenant_id FROM sys_role WHERE role_key IN ('salesman','customer');  -- tenant_id 均为 NULL
-- SELECT version_type, role_code, JSON_LENGTH(menu_keys) menus, JSON_LENGTH(block_keys) blocks
--   FROM role_template WHERE version_type IN ('micro','micro_lite','micro_pro');
-- 预期：micro 3 行 + micro_lite 4 行 + micro_pro 4 行 = 11 行（含 platform_admin）

-- 5) ★V1 零在线支付合规：顾客模板无在线支付 perms，预约/结算无在线支付态
-- SELECT JSON_CONTAINS(menu_keys, '"micro:customer:pay"') FROM role_template
--  WHERE version_type='micro' AND role_code='customer';                  -- 预期：0
-- SELECT COUNT(*) FROM sys_menu WHERE perms = 'micro:customer:pay';       -- 预期：0（该 perms 不存在）

-- 6) 行业模板四业态就绪（系统级共享，无 tenant_id）
-- SELECT COUNT(*) FROM industry_template;                                -- 预期：≥4
-- SELECT COUNT(*) FROM industry_role_map;                               -- 预期：≥12（四行业×三角色）

-- 7) ★小微版七定时任务注册成功（trigger_status=1，glue_remark 标注版本过滤）
-- SELECT executor_handler, schedule_conf, glue_remark, trigger_status FROM xxl_job_info
--  WHERE executor_handler IN ('microDailyRevenueJob','bookingExpireJob','customerTagJob','noShowCheckJob','activityEndJob','resourceInUseFlowJob','activityPayReminderJob');
-- 预期：7 行，glue_remark 均含 @TenantJob(versions={"micro"})；noShowCheckJob 额外含 versionTypes={"micro_pro"}

-- 8) ★角色可见性硬隔离：sys_role.tenant_type_scope 已补列且存量回填正确
-- SELECT role_key, tenant_type_scope FROM sys_role WHERE role_key IN ('boss','salesman','customer','platform_admin','manager','employee','finance');
-- 预期：boss/platform_admin=both；salesman/customer=micro；manager/employee/finance=enterprise

-- 9) ★资金平台管理菜单目录 + 版本档位管理 + 业态包/功能开关管理
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359 ORDER BY menu_id;
-- 预期 12 行：1300(目录) + 1310(版本档位管理) + 1311~1314(4 按钮)
--            + 1320(业态包管理) + 1321~1322(2 按钮) + 1330(功能开关管理) + 1331~1332(2 按钮)
--            （1320~1332 由阶段九 [九-1] M10_01 建出，v1.19 补入）
-- SELECT COUNT(*) FROM sys_menu WHERE parent_id = 1300;  -- 预期 ≥8（re-parent 的租户/角色/行业/菜单/角色模板 + 版本档位管理 + 业态包管理 + 功能开关管理）
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 1320 AND 1339;  -- 预期 6

-- 10) ★分档计费套餐完整
-- SELECT plan_code, name, version_type, price, days FROM sys_billing_plan WHERE plan_code LIKE 'micro\_%' ORDER BY version_type, price;
-- 预期：5 行（micro_lite_free + micro_pro 四档）

-- 11) ★防爽约闭环表 + 菜单 + 任务
-- SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('customer_credit','no_show_record');  -- 预期 2 行
-- SELECT menu_id, perms FROM sys_menu WHERE menu_id BETWEEN 7170 AND 7179;  -- 预期 3 行
-- SELECT executor_handler, trigger_status FROM xxl_job_info WHERE executor_handler='noShowCheckJob';  -- 预期 1 行，trigger_status=1

-- 12) ★lite 档活动浏览页
-- SELECT menu_id, menu_name, perms, client_type FROM sys_menu WHERE menu_id IN (7330,7430);
-- 预期：2 行，perms=micro:activity:browse，client_type=mini

-- 13) ★前端菜单缺口已修补（M15_14，三层控制模型第②层排版入口就绪）
-- SELECT menu_id, menu_name, parent_id, path, component, perms, visible FROM sys_menu
--  WHERE menu_id BETWEEN 7063 AND 7069 ORDER BY menu_id;
-- 预期：4 行，7063(component=micro/pageDecorate/index, perms=micro:page:list, parent=7010, visible=0)
--      + 7064(micro:page:edit) + 7065(micro:page:add) + 7066(micro:page:remove)
-- SELECT CONCAT((SELECT path FROM sys_menu WHERE menu_id=7010), '/', path) FROM sys_menu WHERE menu_id=7063;
-- 预期：/microConfig/pageDecorate（与 roleTemplate 页「前往页面装修」硬编码跳转一致）
-- SELECT menu_id, component, perms, visible FROM sys_menu WHERE menu_id IN (7012,7040,7051,7055,7135,1312,1313,1314);
-- 预期：7012(system/user/index, system:user:list, visible=0)；其余 7 项 visible=1
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id=1 AND menu_id BETWEEN 7063 AND 7069;  -- 预期：4
-- ★装饰性 perms 未被误改（role_template 天花板依赖）
-- SELECT menu_id, perms FROM sys_menu WHERE perms IN ('micro:dashboard:view','micro:dashboard:today',
--   'micro:order:detail','micro:activity:browse','micro:budgetAlert:edit');  -- 预期 6 行(7101/7202/7116/7330/7430/7135)

-- 14) ★推广分销与激励（M16，仅小微版 FEAT_PROMOTION_DISTRIBUTION）
-- 14.1 推广分销 7 张表就绪
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('promotion_code','promotion_attribution','promotion_reward_config',
--                      'promotion_reward_record','coupon','coupon_record','first_order_record');  -- 预期 7
-- 14.2 推广码一人一码 + 永久有效（无过期字段）
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_code' AND INDEX_NAME='uk_tenant_user_type';  -- 预期 uk_tenant_user_type
-- SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_code' AND COLUMN_NAME IN ('expire_time','valid_until');  -- 预期 0
-- 14.3 拉新奖励仅一次 + 首单按手机号+活动唯一
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_reward_record' AND INDEX_NAME='uk_tenant_customer';  -- 预期 uk_tenant_customer
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='first_order_record' AND INDEX_NAME='uk_tenant_activity_phone';  -- 预期 uk_tenant_activity_phone
-- 14.4 ★功能开关 34 项（组 E=1，FEAT_PROMOTION_DISTRIBUTION 三版本均 disabled）
-- SELECT COUNT(*) FROM feature_flag;  -- 预期 34
-- SELECT basic_enabled,advanced_enabled,pro_enabled FROM feature_flag WHERE code='FEAT_PROMOTION_DISTRIBUTION';  -- 预期 0/0/0
-- 14.5 推广分销菜单（老板 PC 7180~7188 + 员工小程序 7340~7342 + 顾客小程序 7440~7442）
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7180 AND 7188;  -- 预期 9
-- SELECT menu_id,perms,client_type FROM sys_menu WHERE menu_id IN (7340,7440);  -- 预期 client_type=mini

-- 15) ★存量小微版租户菜单授权已修复（M15_15，v1.17 补漏）
-- 15.1 小微版租户克隆角色菜单数 > 0
-- SELECT t.id, t.company_name, t.version_type,
--        r.role_key, r.role_id,
--        COUNT(rm.menu_id) AS menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key IN ('boss','finance','manager','employee','platform_admin')
--   LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, t.version_type, r.role_key, r.role_id
--  ORDER BY t.id, r.role_key;
-- 预期：每个克隆角色的 menu_count > 0（boss 最多，platform_admin 次之）
-- 15.2 ★platform_admin 角色菜单数 > 0（此前为 0 的根因症状）
-- SELECT r.role_id, r.role_key, COUNT(rm.menu_id) AS menu_count
--   FROM sys_role r LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE r.tenant_id = (SELECT id FROM sys_tenant WHERE tenant_type='micro' LIMIT 1)
--    AND r.role_key = 'platform_admin'
--  GROUP BY r.role_id, r.role_key;  -- 预期：menu_count > 0

-- 16) ★租户管理员菜单三件套（T15.33，M15_17，v1.30 新增）
-- 16.1 platform_admin 管理菜单目录 + 子菜单
-- SELECT menu_id, menu_name, parent_id, perms, menu_type FROM sys_menu WHERE menu_id BETWEEN 7500 AND 7509 ORDER BY menu_id;
-- 预期：10 行（7500 目录 + 7501~7508 四菜单 + 7502/7503/7505/7506 按钮）
-- 16.2 role_template 包含新 perms
-- SELECT JSON_CONTAINS(menu_keys, '"micro:platform:upgrade"') AS has_upgrade,
--        JSON_CONTAINS(menu_keys, '"micro:platform:theme"') AS has_theme
--   FROM role_template WHERE version_type='micro_lite' AND role_code='platform_admin';
-- 预期：1/1
-- 16.3 存量租户 platform_admin 菜单数 > 0
-- SELECT t.id, t.company_name, COUNT(rm.menu_id) AS menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin'
--   LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name;
-- 预期：menu_count >= 16

-- 17) ★推广分销菜单拆分（T15.34，M15_18，v1.30 新增）
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id IN (7189, 7190);
-- 预期：2 行（7189 业务员奖励记录 parent=7181，7190 顾客奖励记录 parent=7185）
-- SELECT JSON_CONTAINS(menu_keys, '"micro:distribution:salesman:records"') FROM role_template
--  WHERE version_type='micro_pro' AND role_code='boss';  -- 预期：1

-- 18) ★活动图片+背景主题（T15.35，M15_19，v1.30 新增）
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
--    AND COLUMN_NAME IN ('logo_url', 'theme_key');
-- 预期：2 行（logo_url varchar(500), theme_key varchar(32) default theme_default）

-- 18b) ★图片校验与渲染管线（T15.35§六，M15_19b，v1.31 新增 / v1.33 收敛）
-- ★复用 T12.x 既有扩展点：审核留痕→biz_content_security_log、任务→biz_image_task、成品 URL→tenant_theme.logo_rendered_url/rendered_poster_url
-- 18b.1 唯一新增：tenant_theme 补 k-means 主色/辅色 2 列
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenant_theme'
--    AND COLUMN_NAME IN ('poster_dominant_color','poster_accent_color');
-- 预期：2 行（均 varchar(16)）
-- 18b.2 复用的既有基础设施就绪（不由 M15_19b 创建，此处仅校验存在）
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('biz_content_security_log','biz_image_task','tenant_theme');  -- 预期：3
-- 18b.3 ★不再新建 img_sec_check_log 表（已由 biz_content_security_log 承接留痕）
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'img_sec_check_log';  -- 预期：0

-- 19) ★小程序全局主题+欢迎语（T15.36，M15_20，v1.30 新增）
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'app_theme_key';
-- 预期：1 行（app_theme_key varchar(32) default theme_default）
-- SELECT id, company_name, app_theme_key FROM sys_tenant WHERE tenant_type='micro' LIMIT 5;
-- 预期：app_theme_key 均为 theme_default

-- 19b) ★主题显示名映射 + 默认 Logo（T15.36§五，M15_20b，v1.31 新增）
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant'
--    AND COLUMN_NAME IN ('theme_display_name_map','default_logo_url');
-- 预期：2 行（theme_display_name_map json, default_logo_url varchar(500)）

-- 20) ★首页盈亏+每月固定支出（T15.37，M15_21，v1.30 新增 / v1.64 增强）
-- 20.1 monthly_fixed_cost 表就绪
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost';  -- 预期：1
-- 20.2 表结构完整（v1.64 新增 period_rule_id 列，create_by/update_by 改为 varchar(64)）
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost';  -- 预期：17 列
-- 20.3 菜单已建
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id BETWEEN 7195 AND 7199;
-- 预期：5 行（1 目录 + 1 列表 + 3 按钮）
-- 20.4 boss role_template 包含 cost perms
-- SELECT JSON_CONTAINS(menu_keys, '"micro:pro:cost:list"') FROM role_template
--  WHERE version_type='micro_pro' AND role_code='boss';  -- 预期：1

-- 21) ★通用评价与点赞（M17，仅小微版付费版 FEAT_RATING）
-- 21.1 评价/点赞两张表就绪
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('user_rating','user_like');  -- 预期 2
-- 21.2 评价表唯一约束（防重 + 禁止自评）
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='user_rating' AND INDEX_NAME='uk_tenant_target_author_scene_order';  -- 预期存在
-- 21.3 点赞表唯一约束（toggle 防重）
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='user_like' AND INDEX_NAME='uk_tenant_target_author_scene';  -- 预期存在
-- 21.4 ★FEAT_RATING 落表且三版本均 disabled
-- SELECT code, basic_enabled, advanced_enabled, pro_enabled FROM feature_flag WHERE code='FEAT_RATING';
--   -- 预期 0/0/0
-- 21.5 ★功能开关总数 35 项（组 E=2）
-- SELECT COUNT(*) FROM feature_flag;  -- 预期 35
-- SELECT flag_group, COUNT(*) FROM feature_flag GROUP BY flag_group;  -- 预期 A=11/B=9/C=6/D=7/E=2
-- 21.6 评价管理菜单（老板 PC 7160~7164）
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7160 AND 7164;  -- 预期 5
-- SELECT menu_id, perms FROM sys_menu WHERE menu_id BETWEEN 7160 AND 7164;
--   -- 预期 7160(micro:rating:list) + 7161(reply) + 7162(shield) + 7163(detail) + 7164(ranking)

-- 22) ★M18 语音合规与敏感词库（M18_01，T18.1，v1.34 新增）
-- 22.1 四张合规表均含 tenant_id 列
-- SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND COLUMN_NAME='tenant_id'
--   AND TABLE_NAME IN ('voice_compliance_rule','voice_recording_policy','voice_compliance_hit','sensitive_word_library');
--   -- 预期 4
-- 22.2 voice_compliance_rule/voice_recording_policy/sensitive_word_library 唯一约束
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='voice_compliance_rule'   AND INDEX_NAME='uk_tenant_activity_type';  -- 预期存在
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='voice_recording_policy'  AND INDEX_NAME='uk_tenant_id';              -- 预期存在
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='sensitive_word_library'  AND INDEX_NAME='uk_tenant_word';            -- 预期存在
-- 22.3 平台级敏感词初始数据（tenant_id IS NULL）
-- SELECT COUNT(*) FROM sensitive_word_library WHERE tenant_id IS NULL;  -- 预期 ≥8

-- 23) ★M18 固定支出标签/周期规则/预约策略（M18_02~M18_03，T18.9/T18.10）
-- 23.1 三张表就绪
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('fixed_cost_tag','period_rule','booking_policy');  -- 预期 3
-- 23.2 booking_policy 每租户一条 UNIQUE
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='booking_policy' AND INDEX_NAME='uk_tenant_id';  -- 预期存在
-- 23.3 菜单 7620~7630 已落库
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7620 AND 7639;  -- 预期 ≥10（1 目录+2 列表+6 按钮+1 预约策略）

-- 24) ★M18 拉新奖励扩展 + 员工业绩榜（M18_04~M18_05，T18.12/T18.13）
-- 24.1 promotion_reward_config 幂等补 3 列
-- SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_reward_config'
--   AND COLUMN_NAME IN ('trigger_condition','reward_limit_type','reward_limit_value');  -- 预期 3 行
-- 24.2 菜单 7640/7650 已落库
-- SELECT menu_id, perms FROM sys_menu WHERE menu_id IN (7640, 7650);
--   -- 预期 7640(micro:distribution:rewardConfig) + 7650(micro:staffRanking:list)

-- 25) ★M18 License 模板与 Admin 运维审计（M18_06~M18_07，T18.14/T18.15）
-- 25.1 两张平台级表就绪
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('sys_license_template','sys_alert_rule');  -- 预期 2
-- 25.2 预置数据：4 条 License 模板 + 4 条告警规则
-- SELECT COUNT(*) FROM sys_license_template;  -- 预期 ≥4
-- SELECT COUNT(*) FROM sys_alert_rule;         -- 预期 ≥4
-- 25.3 菜单 7660~7671（Admin 专属）
-- SELECT menu_id, perms FROM sys_menu WHERE menu_id BETWEEN 7660 AND 7679 ORDER BY menu_id;
--   -- 预期 4 行（system:licenseTemplate:list / system:licenseReport:list / system:voiceAuditLog:list / system:alertRule:list）

-- 26) ★M18 汇总补丁（M18_08，T18.20）
-- 26.1 role_template boss(micro/micro_pro) 含 M18 新权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:distribution:rewardConfig"') AS has_reward,
--        JSON_CONTAINS(menu_keys, '"micro:staffRanking:list"')          AS has_ranking
--   FROM role_template WHERE version_type IN ('micro','micro_pro') AND role_code='boss';
--   -- 预期 1/1
-- 26.2 role_template platform_admin(micro/micro_pro) 含 M18 新权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:tagConfig:list"')     AS has_tag,
--        JSON_CONTAINS(menu_keys, '"micro:periodRule:list"')    AS has_period,
--        JSON_CONTAINS(menu_keys, '"micro:bookingPolicy:list"') AS has_booking_list,
--        JSON_CONTAINS(menu_keys, '"micro:bookingPolicy:edit"') AS has_booking_edit
--   FROM role_template WHERE version_type IN ('micro','micro_pro') AND role_code='platform_admin';
--   -- 预期 1/1/1/1
-- 26.3 M18 菜单总数 + 存量租户关联
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7600 AND 7679;  -- 预期 ≥14
-- SELECT m.menu_id, m.menu_name, COUNT(rm.role_id) AS role_count
--   FROM sys_menu m LEFT JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
--  WHERE m.menu_id BETWEEN 7620 AND 7679 AND m.menu_type IN ('M','C')
--  GROUP BY m.menu_id, m.menu_name ORDER BY m.menu_id;  -- 预期每个菜单 role_count ≥1

-- 27) ★M15_22 行业模板种子数据（T15.33，V3.5 §16，v1.35 新增）
-- 27.1 行业模板总数（23 启用 + 1 停用 market）
-- SELECT COUNT(*) FROM industry_template WHERE del_flag='0';                    -- 预期 24
-- SELECT COUNT(*) FROM industry_template WHERE del_flag='0' AND status='0';     -- 预期 23
-- 27.2 四大类分布（休闲娱乐 6 / 生活服务 7 / 运动健身 7 / 专业工作室 3）
-- SELECT JSON_UNQUOTE(JSON_EXTRACT(preset_config,'$.category')) AS cat, COUNT(*) AS cnt
--   FROM industry_template WHERE status='0' AND del_flag='0' GROUP BY cat ORDER BY cat;
--   -- 预期 4 行；cnt 分别为 6/7/7/3（合计 23）
-- 27.3 每行业 preset_config.projects 5~7 条
-- SELECT industry_code, industry_name, JSON_LENGTH(preset_config,'$.projects') AS project_count
--   FROM industry_template WHERE status='0' AND del_flag='0' ORDER BY sort_order;
--   -- 预期 23 行；每行 project_count BETWEEN 5 AND 7
-- 27.4 行业角色映射完整（23 × 3 = 69，market 保留旧数据不计入）
-- SELECT COUNT(*) FROM industry_role_map WHERE industry_code<>'market';         -- 预期 ≥69
-- SELECT industry_code, COUNT(*) AS role_cnt FROM industry_role_map
--  WHERE industry_code<>'market' GROUP BY industry_code HAVING role_cnt<3;      -- 预期 0 行
-- 27.5 market 已停用但数据保留（V3.5 §10 排除零售/生鲜，兼容既有租户 industry_code=market）
-- SELECT industry_code, industry_name, status FROM industry_template WHERE industry_code='market';
--   -- 预期 status='1'，remark 含 'V3.5 停用'
-- 27.6 前端下拉框验证（新建租户页面 → 行业模板下拉应显示 23 项）
-- SELECT COUNT(*) FROM industry_template WHERE status='0' AND del_flag='0';      -- 预期 23

-- 28) ★platform_admin 菜单权限隔离修正（v1.36 新增，[51]_fix_platform_admin_menu_v136）
-- 28.1 role_template 不再包含 admin 级别 perms
-- SELECT JSON_CONTAINS(menu_keys, '"system:tenant:list"') AS has_admin_perm,
--        JSON_CONTAINS(menu_keys, '"micro:platform:upgrade"') AS has_micro_perm
--   FROM role_template WHERE version_type='micro_lite' AND role_code='platform_admin';
--   -- 预期 has_admin_perm=0, has_micro_perm=1
-- 28.2 M 类型目录 7500 已关联到 platform_admin 克隆角色
-- SELECT COUNT(*) FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id
--   JOIN sys_tenant t ON r.tenant_id = t.id
--  WHERE r.role_key = 'platform_admin' AND t.tenant_type = 'micro' AND rm.menu_id = 7500;
--   -- 预期 > 0（每个小微版租户 1 条）
-- 28.3 无 admin 菜单泄漏
-- SELECT COUNT(*) AS admin_menu_leak
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id
--   JOIN sys_tenant t ON r.tenant_id = t.id
--  WHERE r.role_key = 'platform_admin' AND t.tenant_type = 'micro'
--    AND rm.menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'system:tenant:%');
--   -- 预期 0

-- 29) ★platform_admin 克隆角色 + 用户角色分配 + 完整菜单授权修复（v1.37 新增，[52]M18_09）
-- 29.1 所有小微版租户均有 platform_admin 克隆角色
-- SELECT t.id, t.company_name, r.role_id, r.role_key
--   FROM sys_tenant t
--   LEFT JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin' AND r.del_flag = '0'
--  WHERE t.tenant_type = 'micro';
--   -- 预期：每行 role_id 非 NULL
-- 29.2 所有 platform_admin 用户均有角色分配
-- SELECT u.user_id, u.user_name, u.tenant_id, ur.role_id
--   FROM sys_user u
--   JOIN sys_tenant t ON u.tenant_id = t.id AND t.tenant_type = 'micro'
--   LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id
--   LEFT JOIN sys_role r ON ur.role_id = r.role_id AND r.role_key = 'platform_admin'
--  WHERE u.status = '0' AND u.del_flag = '0';
--   -- 预期：每行 role_id 非 NULL
-- 29.3 每个克隆角色菜单数 >= 19
-- SELECT r.role_id, r.tenant_id, COUNT(rm.menu_id) AS menu_count
--   FROM sys_role r
--   LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE r.role_key = 'platform_admin' AND r.tenant_id IS NOT NULL AND r.del_flag = '0'
--  GROUP BY r.role_id, r.tenant_id;
--   -- 预期：menu_count >= 19（7500+7501~7508+7620+7621~7628+7630+7631 = 20）
-- 29.4 ★预约策略按钮级菜单 7631 已落库（MicroBookingPolicyController.savePolicy 依赖）
-- SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id = 7631;
--   -- 预期：1 行，menu_type='F'，perms='micro:bookingPolicy:edit'

-- 30) ★platform_admin 原子资源管理·项目管理 + 房间/空间管理（v1.38 新增，[53]M15_23）
-- 30.1 service_category.category_type 列已建立
-- SELECT COLUMN_NAME, DATA_TYPE, COLUMN_DEFAULT FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='service_category' AND COLUMN_NAME='category_type';
--   -- 预期：1 行（varchar / service）
-- 30.2 23 行业 preset_config 均含 rooms 数组（≥2 间）
-- SELECT industry_code, JSON_LENGTH(JSON_EXTRACT(preset_config,'$.rooms')) AS room_count
--   FROM industry_template WHERE status='0' ORDER BY sort_order;
--   -- 预期：23 行，room_count >= 2
-- 30.3 原子资源管理菜单目录 + 子菜单
-- SELECT menu_id, menu_name, parent_id, perms, menu_type FROM sys_menu WHERE menu_id BETWEEN 7510 AND 7516 ORDER BY menu_id;
--   -- 预期：7 行（7510 目录 + 7511/7514 两 C 菜单 + 7512/7513/7515/7516 四 F 按钮）
-- 30.4 7504(角色管理)/7507(员工库) 已 re-parent 到 7510
-- SELECT menu_id, menu_name, parent_id, order_num FROM sys_menu WHERE menu_id IN (7504,7507,7508) ORDER BY menu_id;
--   -- 预期：7504 parent=7510 / 7507 parent=7510 / 7508 parent=7500
-- 30.5 role_template 含新 perms
-- SELECT JSON_CONTAINS(menu_keys,'"micro:platform:project"') AS has_project,
--        JSON_CONTAINS(menu_keys,'"micro:platform:room"') AS has_room
--   FROM role_template WHERE version_type='micro_lite' AND role_code='platform_admin';
--   -- 预期：1/1
-- 30.6 存量租户 platform_admin 已关联 7510~7516
-- SELECT t.id, t.company_name, COUNT(rm.menu_id) AS atomic_menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='platform_admin'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id AND rm.menu_id BETWEEN 7510 AND 7516
--  WHERE t.tenant_type='micro' GROUP BY t.id, t.company_name;
--   -- 预期：atomic_menu_count = 7

-- 30.5) ★M18 语音合规全链路菜单注册（v1.53 新增，[56.5]M18_13，T18.1~T18.4）
-- 30.5.1 Admin 语音合规菜单（7600~7604）
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7600 AND 7609;  -- 预期 5
-- 30.5.2 租户管理员语音合规菜单（7610~7617）
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7610 AND 7617;  -- 预期 8
-- 30.5.3 老板语音合规记录菜单（7618）
-- SELECT menu_id, perms FROM sys_menu WHERE menu_id = 7618;  -- 预期 micro:compliance:records
-- 30.5.4 role_template 含新权限
-- SELECT JSON_CONTAINS(menu_keys,'"micro:compliance:config:rule"') AS pa_rule,
--        JSON_CONTAINS(menu_keys,'"micro:compliance:records"')      AS boss_records
--   FROM role_template WHERE version_type='micro_pro' AND role_code IN ('platform_admin','boss');
--   -- platform_admin 行 pa_rule=1；boss 行 boss_records=1

-- 31) ★M18 图片合规与全局敏感词（v1.39 新增，[54]M18_10/[55]M18_11/[56]M18_12）
-- 31.1 三表建成
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('sensitive_word','sensitive_word_hit_log','image_compliance_config');  -- 预期 3
-- 31.2 feature_flag 双开关落表，四列均 enabled=1，开关总数 35→37
-- SELECT code, basic_enabled, advanced_enabled, pro_enabled, status FROM feature_flag
--   WHERE code IN ('FEAT_IMAGE_COMPLIANCE','FEAT_SENSITIVE_WORD');  -- 预期 2 行，三列均=1，status='0'
-- SELECT COUNT(*) FROM feature_flag;  -- 预期 37
-- 31.3 全局底库≥200 条，八大类分布
-- SELECT COUNT(*) FROM sensitive_word WHERE tenant_id=0 AND source='global';  -- 预期 ≥200
-- SELECT category, COUNT(*) FROM sensitive_word WHERE tenant_id=0 GROUP BY category;  -- 预期 8 类
-- 31.4 image_compliance_config 全局默认三行（tenant_id=0）
-- SELECT violation_type, confidence_threshold FROM image_compliance_config WHERE tenant_id=0;  -- 预期 porn/violence/politics
-- 31.5 Admin 平台合规菜单 + 租户/老板菜单入口存在
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 1370 AND 1389;  -- 预期 11
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7680 AND 7690;  -- 预期 8

-- 32) ★micro_lite(免费版) boss 推广管理菜单授权（v1.45 新增，[62]M19_01）
-- 32.1 role_template micro_lite boss 含推广管理权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:distribution:rewardConfig"') AS has_reward_config,
--        JSON_CONTAINS(menu_keys, '"micro:distribution:*"') AS has_distribution,
--        JSON_CONTAINS(menu_keys, '"micro:promo:list"') AS has_promo_list
--   FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
--   -- 预期 1/1/1
-- 32.2 存量租户 boss 菜单授权完整
-- SELECT t.id, t.company_name, COUNT(rm.menu_id) AS menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--  GROUP BY t.id, t.company_name;
--   -- 预期：每个租户 menu_count >= 10（含推广管理相关菜单）

-- 33) ★小微版 boss 补录 system:oss:upload 权限（v1.46 新增，[63]M19_02）
-- 33.1 boss 克隆角色已包含 OSS 上传权限
-- SELECT t.id, t.company_name, r.role_id,
--        SUM(CASE WHEN rm.menu_id = 1131 THEN 1 ELSE 0 END) AS has_oss_upload
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, r.role_id;
--   -- 预期：每个租户 has_oss_upload = 1

-- 34) ★micro_lite(免费版) boss 菜单精简（v1.47 新增，[64]M19_03）
-- 34.1 role_template micro_lite boss 权限键（★最终态：M19_03 精简 2 键 → M19_04 补 CRUD → M19_05 补 OSS）
-- SELECT menu_keys FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
--   -- 预期（最终态 7 键）：["micro:dashboard:view","micro:promo:list","micro:promo:add","micro:promo:edit","micro:promo:remove","micro:promo:poster","system:oss:upload"]
-- 34.2 存量租户 boss 菜单精简后仅含目标菜单 + 祖先目录
-- SELECT t.id, t.company_name, m.menu_id, m.menu_name, m.menu_type
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--    AND m.menu_type IN ('M', 'C')
--  ORDER BY t.id, m.menu_id;
--   -- 预期：每个租户 3~4 行（7100 小微运营 + 7101 经营概览 + 7150 推广发布 + 7151 活动管理）
-- 34.3 boss 侧边栏（仅 M/C 渲染）无多余菜单
-- SELECT m.menu_id, m.menu_name
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--    AND m.menu_type IN ('M', 'C')
--    AND m.menu_id NOT IN (7100, 7101, 7150, 7151);
--   -- 预期：0 行（★F 按钮 7152~7155/1131 不在侧边栏渲染，已被 menu_type 过滤，不算多余）

-- 35) ★micro_lite(免费版) boss 活动增删改查权限补全（v1.48 新增，[65]M19_04）
-- 35.1 role_template micro_lite boss 含活动 CRUD 全量权限（★最终态含 M19_05 追加的 system:oss:upload）
-- SELECT menu_keys FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
--   -- 预期：7 项 JSON 数组（dashboard:view + promo:list/add/edit/remove/poster + system:oss:upload）
-- 35.2 存量租户 boss 已包含活动操作按钮
-- SELECT t.id, t.company_name,
--        SUM(CASE WHEN rm.menu_id = 7152 THEN 1 ELSE 0 END) AS has_add,
--        SUM(CASE WHEN rm.menu_id = 7153 THEN 1 ELSE 0 END) AS has_edit,
--        SUM(CASE WHEN rm.menu_id = 7154 THEN 1 ELSE 0 END) AS has_remove,
--        SUM(CASE WHEN rm.menu_id = 7155 THEN 1 ELSE 0 END) AS has_poster
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--  GROUP BY t.id, t.company_name;
--   -- 预期：每个租户 has_add/has_edit/has_remove/has_poster 均为 1

-- 36) ★小微版 boss OSS 上传权限彻底修复 + 系统管理菜单根治（v1.49/v1.50 新增，[66]M19_05 + Java 修复）
-- 36.1 role_template micro/micro_pro/micro_lite boss 已包含 system:oss:upload
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"system:oss:upload"') AS has_oss_upload
--   FROM role_template
--  WHERE version_type IN ('micro', 'micro_pro', 'micro_lite') AND role_code = 'boss';
--   -- 预期：micro=1, micro_pro=1, micro_lite=1
-- 36.2 所有小微版 boss 已包含 menu_id=1131（OSS 上传 F 按钮）
-- SELECT t.id, t.company_name, t.version_type, r.role_id,
--        SUM(CASE WHEN rm.menu_id = 1131 THEN 1 ELSE 0 END) AS has_oss_upload
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, t.version_type, r.role_id;
--   -- 预期：每个租户 has_oss_upload = 1
-- 36.3 ★所有小微版 boss 均无系统管理可见菜单 1/118（侧边栏不再冒出「系统管理」——本次根治核心验收）
-- SELECT COUNT(*) AS system_menu_leak
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND rm.menu_id IN (1, 118);
--   -- 预期：0

-- 37) ★micro_lite(免费版) boss 最终态总验收（v1.50 新增，本次「系统管理反复出现」根治，一次性核对）
--     需求：执行完 00_执行顺序.sql 后，免费版 boss 侧边栏仅「经营概览 + 活动管理」，活动管理具备增删改查，且无「系统管理」
-- 37.1 侧边栏可见菜单（M/C）= 且仅 = 7100 小微运营 / 7101 经营概览 / 7150 推广发布 / 7151 活动管理
-- SELECT t.id, t.company_name, GROUP_CONCAT(m.menu_id ORDER BY m.menu_id) AS sidebar_menu_ids
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.menu_type IN ('M', 'C') AND m.status = '0'
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--  GROUP BY t.id, t.company_name;
--   -- 预期：每租户 sidebar_menu_ids = '7100,7101,7150,7151'（★无 1/118 系统管理）
-- 37.2 活动管理增删改查 + OSS 上传按钮齐备（F 类型，不在侧边栏渲染但决定按钮显隐/上传能力）
-- SELECT t.id, t.company_name,
--        SUM(CASE WHEN rm.menu_id = 7152 THEN 1 ELSE 0 END) AS has_add,
--        SUM(CASE WHEN rm.menu_id = 7153 THEN 1 ELSE 0 END) AS has_edit,
--        SUM(CASE WHEN rm.menu_id = 7154 THEN 1 ELSE 0 END) AS has_remove,
--        SUM(CASE WHEN rm.menu_id = 7155 THEN 1 ELSE 0 END) AS has_poster,
--        SUM(CASE WHEN rm.menu_id = 1131 THEN 1 ELSE 0 END) AS has_oss_upload
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--  GROUP BY t.id, t.company_name;
--   -- 预期：每租户 has_add/has_edit/has_remove/has_poster/has_oss_upload 均为 1

-- 38) ★小微版 boss OSS 图片回显/删除权限补全（v1.51 新增，[67]M19_06 + Java listByIds OR 放行）
-- 38.1 role_template micro/micro_pro/micro_lite boss 均含 system:oss:query + system:oss:remove
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"system:oss:query"')  AS has_oss_query,
--        JSON_CONTAINS(menu_keys, '"system:oss:remove"') AS has_oss_remove
--   FROM role_template
--  WHERE version_type IN ('micro', 'micro_pro', 'micro_lite') AND role_code = 'boss';
--   -- 预期：三档均 has_oss_query=1, has_oss_remove=1
-- 38.2 所有小微版 boss 已包含 1130(文件查询,F) + 1133(文件删除,F)
-- SELECT t.id, t.company_name, t.version_type, r.role_id,
--        SUM(CASE WHEN rm.menu_id = 1130 THEN 1 ELSE 0 END) AS has_oss_query,
--        SUM(CASE WHEN rm.menu_id = 1133 THEN 1 ELSE 0 END) AS has_oss_remove
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, t.version_type, r.role_id;
--   -- 预期：每个租户 has_oss_query=1, has_oss_remove=1
-- 39) ★活动设置增强数据模型（v1.52 新增，[68]M19_07，T19.1~T19.5）
-- 39.1 promotion_activity 新增 8 列就绪
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
--    AND COLUMN_NAME IN ('auto_title','bg_image_url','voice_url','qrcode_url',
--        'recommend_scene','selected_project_id','selected_room_ids','selected_staff_ids');
-- 预期：8 行
-- 39.2 活动设置菜单已落库
-- SELECT menu_id, menu_name, parent_id, menu_type, perms
--  FROM sys_menu WHERE menu_id BETWEEN 7156 AND 7158;
-- 预期：3 行（7156=C, 7157=F, 7158=F）
-- 39.3 role_template boss 含活动设置权限
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"micro:activity:setting:list"') AS has_list,
--        JSON_CONTAINS(menu_keys, '"micro:activity:setting:edit"') AS has_edit
--  FROM role_template
--  WHERE role_code = 'boss' AND version_type IN ('micro','micro_pro','micro_lite');
-- 预期：3 行，has_list=1, has_edit=1
-- 39.4 存量租户 boss 已关联活动设置菜单
-- SELECT t.id, t.company_name,
--        SUM(CASE WHEN rm.menu_id = 7156 THEN 1 ELSE 0 END) AS has_setting,
--        SUM(CASE WHEN rm.menu_id = 7157 THEN 1 ELSE 0 END) AS has_query,
--        SUM(CASE WHEN rm.menu_id = 7158 THEN 1 ELSE 0 END) AS has_edit
--  FROM sys_tenant t
--  JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--  JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name;
-- 预期：每行 has_setting/has_query/has_edit 均为 1

-- 40) ★M18 最终汇总补丁（v1.55 新增，[56.7]M18_15，T18.20）
-- 40.1 M18_12 Admin 平台合规目录已建立
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 1370 AND 1379;  -- 预期 4
-- 40.2 老板合规命中记录（M18_12）
-- SELECT menu_id, perms FROM sys_menu WHERE menu_id = 7690;  -- 预期 micro:boss:complianceHit:list
-- ★注：租户敏感词管理 7680~7683 已由 M18_13 v1.57 清理
-- 40.3 ★M18_08 缺陷修复：7640~7659 已授予 boss 克隆角色
-- SELECT COUNT(*) AS boss_7640 FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id=r.role_id
--   JOIN sys_tenant t ON r.tenant_id=t.id AND t.tenant_type='micro'
--  WHERE r.role_key='boss' AND rm.menu_id BETWEEN 7640 AND 7649;  -- 预期 > 0
-- 40.4 role_template M18 权限完整性
-- SELECT JSON_CONTAINS(menu_keys, '"system:compliance:sensitiveWord"') AS admin_sw
--   FROM role_template WHERE version_type IS NULL AND role_code='admin';  -- 预期 1
-- SELECT JSON_CONTAINS(menu_keys, '"micro:compliance:config:rule"') AS pa_vc,
--        JSON_CONTAINS(menu_keys, '"micro:systemSetting:list"') AS pa_ss
--   FROM role_template WHERE version_type='micro_pro' AND role_code='platform_admin';  -- 预期 1/1
-- SELECT JSON_CONTAINS(menu_keys, '"micro:boss:complianceHit:list"') AS boss_hit,
--        JSON_CONTAINS(menu_keys, '"micro:distribution:rewardConfig"') AS boss_reward,
--        JSON_CONTAINS(menu_keys, '"micro:compliance:records"') AS boss_voice
--   FROM role_template WHERE version_type='micro_pro' AND role_code='boss';  -- 预期 1/1/1
-- 40.5 ★M18 全部菜单总数（7600~7699）
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7600 AND 7699;  -- 预期 ≥30

-- 41) ★Boss PC「我的推广码」菜单（v1.56 新增，[69]M19_08）
-- 41.1 菜单已落库
-- SELECT menu_id, menu_name, parent_id, perms, menu_type, client_type
--  FROM sys_menu WHERE menu_id = 7159;
-- 预期：1 行，C 类型，client_type=pc，parent_id=7150
-- 41.2 role_template 含新权限（付费版 + 免费版均含）
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"micro:myPromo:view"') AS has_my_promo
--   FROM role_template
--  WHERE role_code = 'boss' AND version_type IN ('micro', 'micro_pro', 'micro_lite');
-- 预期：3 行，has_my_promo 均为 1
-- 41.3 存量租户 boss 已关联
-- SELECT t.id, t.company_name,
--        SUM(CASE WHEN rm.menu_id = 7159 THEN 1 ELSE 0 END) AS has_my_qrcode
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name;
-- 预期：每行 has_my_qrcode = 1
-- 41.4 ★micro_lite 免费版 boss 侧边栏含 7159（挂在 7150 推广发布目录下）
-- SELECT t.id, GROUP_CONCAT(m.menu_id ORDER BY m.menu_id) AS sidebar_menu_ids
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.menu_type IN ('M', 'C') AND m.status = '0'
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--  GROUP BY t.id;
--   -- 预期：每租户 sidebar_menu_ids 含 7159

-- 43) ★micro_pro boss 角色菜单 V3.7 完整对齐（v1.59 新增，[71]M20_01）
-- 43.1 缺失菜单已补建
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id IN (7159, 7650, 7698);
-- 预期：3 行
-- 43.2 数据与报表 M 目录已建
-- SELECT menu_id, menu_name, parent_id, menu_type FROM sys_menu WHERE menu_id = 7190;
-- 预期：1 行，M 类型，parent_id=7100
-- 43.3 role_template 权限键数
-- SELECT JSON_LENGTH(menu_keys) FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：31
-- 43.4 boss sys_role_menu 含全部 M 目录
-- SELECT COUNT(*) FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='boss' AND r.del_flag='0'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id
--  WHERE t.tenant_type='micro' AND t.version_type='micro_pro'
--    AND rm.menu_id IN (7100,7110,7120,7130,7140,7150,7180,7190);
-- 预期：8（每租户 8 个 M 目录全部关联）
-- 43.5 boss 菜单总数 >= 35
-- SELECT t.company_name, COUNT(rm.menu_id) AS cnt FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='boss' AND r.del_flag='0'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id
--  WHERE t.tenant_type='micro' AND t.version_type='micro_pro'
--  GROUP BY t.company_name;
-- 预期：cnt >= 35

-- 44) ★预约列表页权限 403 修复 + 订单详情菜单隐藏（v1.60 新增，[72]M20_02）
-- 44.1 订单详情菜单已隐藏
-- SELECT menu_id, menu_name, visible FROM sys_menu WHERE menu_id = 7116;
-- 预期：visible = '1'
-- 44.2 侧边栏不再出现「订单详情」
-- SELECT menu_id, menu_name, parent_id, visible FROM sys_menu
--  WHERE parent_id = 7110 AND visible = '0';
-- 预期：仅 7111（预约列表），不含 7116
-- 44.3 Java 层 OR 权限已生效（代码层验证，无需 SQL）
-- ServiceItemController.onSale() → @SaCheckPermission(value={"micro:service:list","micro:booking:list"}, mode=SaMode.OR)
-- UserRoleController.staff() → @SaCheckPermission(value={"micro:booking:list","micro:settle:list"}, mode=SaMode.OR)

-- 45) ★micro_pro boss 权限补全 + 推广分销菜单类型修正 + License 小微版识别兜底（v1.62 新增，[74]M20_04）
-- 45.1 role_template 权限键数 = 39
-- SELECT JSON_LENGTH(menu_keys) FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：39
-- 45.2 role_template 包含活动 CRUD + 固定支出
-- SELECT
--   JSON_CONTAINS(menu_keys, '"micro:promo:add"')    AS has_promo_add,
--   JSON_CONTAINS(menu_keys, '"micro:promo:edit"')   AS has_promo_edit,
--   JSON_CONTAINS(menu_keys, '"micro:pro:cost:list"') AS has_cost_list,
--   JSON_CONTAINS(menu_keys, '"micro:pro:cost:add"')  AS has_cost_add
-- FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：全部 1
-- 45.3 菜单 7180 已改为 C 类型
-- SELECT menu_id, menu_name, menu_type, component FROM sys_menu WHERE menu_id = 7180;
-- 预期：menu_type='C', component='micro/distribution/index'
-- 45.4 boss sys_role_menu 含活动按钮 + 固定支出
-- SELECT t.company_name,
--   GROUP_CONCAT(CASE WHEN m.menu_id IN (7152,7153,7154,7155) THEN m.menu_id END ORDER BY m.menu_id) AS promo_btns,
--   GROUP_CONCAT(CASE WHEN m.menu_id BETWEEN 7195 AND 7199 THEN m.menu_id END ORDER BY m.menu_id) AS cost_menus
-- FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='boss' AND r.del_flag='0'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id
--   JOIN sys_menu m ON m.menu_id=rm.menu_id
-- WHERE t.tenant_type='micro' AND t.version_type='micro_pro'
-- GROUP BY t.company_name;
-- 预期：每租户 promo_btns='7152,7153,7154,7155' 且 cost_menus='7195,7196,7197,7198,7199'
-- 45.5 Java 层 LicenseServiceImpl 小微版识别兜底已生效（代码层验证）
-- getTenantActiveFeatures() 检查 sys_tenant.version_type，微版租户无 License 时返回 MICRO_FEATURES
-- getTenantLicenseInfo() 同步适配小微版无 License 场景（status=MICRO_BUILTIN）

-- 46) ★micro_pro 业态包能力开关补全（根因）+ 推广分销/固定支出菜单重构（v1.63 新增，[75]M20_05）
-- 46.1 三业态包 feature_flags 均含 4 个 micro FEAT_*（升级拦截根因已消除）
-- SELECT biz_code,
--   JSON_CONTAINS(feature_flags,'"FEAT_PROMOTION_DISTRIBUTION"') AS f_promo,
--   JSON_CONTAINS(feature_flags,'"FEAT_RATING"')                 AS f_rating,
--   JSON_CONTAINS(feature_flags,'"FEAT_SENSITIVE_WORD"')         AS f_word,
--   JSON_CONTAINS(feature_flags,'"FEAT_IMAGE_COMPLIANCE"')       AS f_image
-- FROM biz_bundle WHERE biz_code IN ('boardgame','fresh_food','fund_control');
-- 预期：3 行，每行 f_promo/f_rating/f_word/f_image 均为 1
-- 46.2 7101 已改名经营驾驶舱
-- SELECT menu_id, menu_name FROM sys_menu WHERE menu_id=7101;   -- 预期：经营驾驶舱
-- 46.3 推广管理子树（7180=M；7181/7182/7183/7184=C 且 component 均存在；无 7186/7187/7188）
-- SELECT menu_id, parent_id, menu_name, menu_type, path, component, perms
-- FROM sys_menu WHERE menu_id BETWEEN 7180 AND 7188 ORDER BY menu_id;
-- 预期：7180(M,推广管理) 7181(C,micro/distribution/index,micro:distribution:stat)
--      7182(C,micro/distribution/salesman/records,micro:distribution:salesman)
--      7183(C,micro/distribution/customer/records,micro:distribution:customer)
--      7184(C,micro/distribution/rewardConfig) 7185(F)；7186/7187/7188 已删除
-- 46.4 固定支出菜单重建（5 行 7195~7199，7195 parent=7190）
-- SELECT menu_id, parent_id, menu_name, menu_type, component, perms
-- FROM sys_menu WHERE menu_id BETWEEN 7195 AND 7199 ORDER BY menu_id;
-- 预期：7195(M,parent=7190) 7196(C,micro/cost/index,micro:pro:cost:list) 7197/7198/7199(F,add/edit/remove)
-- 46.5 role_template micro_pro boss 权限键（预期 37，含 salesman/customer，不含死链 data/code）
-- SELECT JSON_LENGTH(menu_keys) AS cnt,
--   JSON_CONTAINS(menu_keys,'"micro:distribution:salesman"') AS has_salesman,
--   JSON_CONTAINS(menu_keys,'"micro:distribution:customer"') AS has_customer,
--   JSON_CONTAINS(menu_keys,'"micro:distribution:data"')     AS has_dead_data
-- FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 预期：cnt=37，has_salesman=1，has_customer=1，has_dead_data=0
-- 47) ★V3.7 PC 菜单拍平·保留一层（v1.66 新增，[79]M20_09）
-- 47.1 被提升的 5 个菜单已挂到新父级且路径正确
-- SELECT menu_id, parent_id, path FROM sys_menu WHERE menu_id IN (6001,7021,7058,7111,7196) ORDER BY menu_id;
-- 预期：6001→1300/tenant；7021→7010/tenantRole；7058→7010/shop；7111→7100/bookingList；7196→7190/cost
-- 47.2 空目录已删除、含隐藏子页目录已隐藏
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id IN (6000,7020,7195);   -- 预期：0（已删除）
-- SELECT menu_id, visible FROM sys_menu WHERE menu_id IN (7050,7110); -- 预期：均 visible=1（隐藏）
-- 47.3 固定支出 F 按钮已改挂 7196
-- SELECT menu_id, parent_id FROM sys_menu WHERE menu_id IN (7197,7198,7199); -- 预期：parent_id 均为 7196
-- 47.4 无「仅一个可见子菜单」的残留 M 目录
-- SELECT p.menu_id FROM sys_menu p JOIN sys_menu c ON c.parent_id=p.menu_id AND c.menu_type IN ('C','M') AND c.visible='0'
--  WHERE p.menu_type='M' AND p.visible='0' AND p.client_type='pc' GROUP BY p.menu_id HAVING COUNT(c.menu_id)=1; -- 预期：空集

-- 50) ★biz_bundle.fund_control.menus 业态菜单声明修复（v1.69 新增，[82]M20_12）
-- 50.1 fund_control 业态包 menus 已置空
-- SELECT biz_code, menus FROM biz_bundle WHERE biz_code = 'fund_control';
-- 预期：menus = NULL
-- 50.2 登录响应 menus 包含 7100+ 区段菜单（需重启后端+重新登录验证）

-- 49) ★小微版租户 version_type 为 NULL 修复（v1.68 新增，[81]M20_11）
-- 49.1 无 version_type 为 NULL 的小微版租户
-- SELECT COUNT(*) AS null_count FROM sys_tenant
--  WHERE tenant_type='micro' AND (version_type IS NULL OR version_type='');
-- 预期：0
-- 49.2 每个小微版 boss 的 M/C 菜单数 > 0
-- SELECT t.id, t.company_name, t.version_type, COUNT(DISTINCT m.menu_id) AS mc_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='boss' AND r.del_flag='0'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id
--   JOIN sys_menu m ON m.menu_id=rm.menu_id
--  WHERE t.tenant_type='micro' AND m.menu_type IN ('M','C') AND m.status='0'
--  GROUP BY t.id, t.company_name, t.version_type;
-- 预期：每行 mc_count > 0

-- 48) ★异常处理中心「处理」按钮权限补全（v1.67 新增，[80]M20_10）
-- 48.1 F 按钮菜单 7699 存在（预期 1 行，F 类型，parent=7698，perms=micro:exception:resolve）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu WHERE menu_id = 7699;
-- 48.2 role_template micro_pro boss 含 resolve 键（预期 1）
-- SELECT JSON_CONTAINS(menu_keys, '"micro:exception:resolve"') AS has_resolve
--   FROM role_template WHERE version_type='micro_pro' AND role_code='boss';
-- 48.3 存量角色 7698/7699 成对授权（预期每行 has_resolve_menu=1）
-- SELECT r.role_id, r.role_key,
--        MAX(rm.menu_id = 7698) AS has_list_menu,
--        MAX(rm.menu_id = 7699) AS has_resolve_menu
--   FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE rm.menu_id IN (7698, 7699)
--  GROUP BY r.role_id, r.role_key;

-- 51) ★boss 角色不含 platform_admin 菜单 7500~7509（v1.76 新增，[91]fix_boss_remove_tenant_admin_menu）
-- 51.1 boss 克隆角色不应持有 7500~7509 任何菜单（预期 0 行）
-- SELECT r.role_id, t.company_name, rm.menu_id
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--    AND rm.menu_id BETWEEN 7500 AND 7509;
--   -- 预期：0 行
-- 51.2 platform_admin 克隆角色仍持有 7500 目录（预期每租户 1 行）
-- SELECT r.role_id, t.company_name, rm.menu_id
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--    AND rm.menu_id = 7500;
--   -- 预期：每个租户 1 行（menu_id=7500）

-- 52) ★FEAT_SELF_BOOKING 业态包补全（v1.77 新增，[92]M21_07_self_booking_biz_bundle_fix）
-- 52.1 开关落表且 micro_enabled=1（预期 1 行：E / 1 / 0 / 0）
-- SELECT code, flag_group, micro_enabled, basic_enabled, status FROM feature_flag WHERE code = 'FEAT_SELF_BOOKING';
-- 52.2 所有 feature_flags 非 NULL 业态包均含 5 个小微开关（预期每行 5 个 1）
-- SELECT biz_code,
--   JSON_CONTAINS(feature_flags,'"FEAT_SELF_BOOKING"')           AS f_self_booking,
--   JSON_CONTAINS(feature_flags,'"FEAT_PROMOTION_DISTRIBUTION"') AS f_promo,
--   JSON_CONTAINS(feature_flags,'"FEAT_RATING"')                 AS f_rating,
--   JSON_CONTAINS(feature_flags,'"FEAT_SENSITIVE_WORD"')         AS f_word,
--   JSON_CONTAINS(feature_flags,'"FEAT_IMAGE_COMPLIANCE"')       AS f_image
--   FROM biz_bundle;
-- 52.3 端到端：micro_pro boss 重启后端并重新登录后，助教状态管理/房间状态管理/资源价格设置
--      三页面列表接口返回 200，不再抛「小微版无此功能权限」

-- 53) ★FEAT_STAFF_SCHEDULE 员工排班（v1.80 新增，[93]M22_01_staff_schedule）
-- 53.1 两表就绪 + 防重复排班唯一键（预期 2 行）
-- SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('staff_schedule','schedule_change_log');   -- 预期 2 行
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='staff_schedule' AND INDEX_NAME='uk_tenant_staff_date_shift';   -- 预期 1 行
-- 53.2 开关落表且 micro_enabled=1（预期 1 行：E / 1 / 0 / 0）
-- SELECT code, flag_group, micro_enabled, basic_enabled, status FROM feature_flag WHERE code = 'FEAT_STAFF_SCHEDULE';
-- 53.3 所有 feature_flags 非 NULL 业态包均含 FEAT_STAFF_SCHEDULE（预期每行 1）
-- SELECT biz_code, JSON_CONTAINS(feature_flags,'"FEAT_STAFF_SCHEDULE"') AS f_staff_schedule FROM biz_bundle;
-- 53.4 菜单 7230~7235 就绪（预期 6 行：7230 C/pc + 7231~7233 F/pc + 7234 F/mini + 7235 F/mini）
-- SELECT menu_id, parent_id, menu_name, menu_type, perms, client_type FROM sys_menu
--  WHERE menu_id BETWEEN 7230 AND 7235 ORDER BY menu_id;
-- 53.5 role_template：micro_pro boss 含 4 键 + salesman 含 my + customer 含 store；micro_lite 三配方均不含
-- SELECT role_code,
--   JSON_CONTAINS(menu_keys,'"micro:staffSchedule:view"')    AS has_view,
--   JSON_CONTAINS(menu_keys,'"micro:staffSchedule:create"')  AS has_create,
--   JSON_CONTAINS(menu_keys,'"micro:staffSchedule:publish"') AS has_publish,
--   JSON_CONTAINS(menu_keys,'"micro:staffSchedule:edit"')    AS has_edit,
--   JSON_CONTAINS(menu_keys,'"micro:staffSchedule:my"')      AS has_my,
--   JSON_CONTAINS(menu_keys,'"micro:staffSchedule:store"')   AS has_store
--   FROM role_template WHERE version_type='micro_pro' AND role_code IN ('boss','salesman','customer');
-- 预期：boss 行 has_view/create/publish/edit=1；salesman 行 has_my=1；customer 行 has_store=1
-- SELECT JSON_CONTAINS(menu_keys,'"micro:staffSchedule:view"') AS lite_leak
--   FROM role_template WHERE version_type='micro_lite';   -- 预期：全部 0（免费版不启用）
-- 53.6 端到端：micro_pro boss 重启后端+重新登录后，员工排班页批量创建→发布→修正全链路 200；
--      员工小程序「我的排班」仅见本人 published；顾客小程序「门店排班」+ 自助预约助教数据源来自 staff_schedule

-- 54) ★service_item 补 extra_config 列（v1.81 新增，[97]M22_04）
-- 54.1 service_item 含 extra_config 列（预期 1 行）
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='service_item' AND COLUMN_NAME='extra_config';
-- 预期：1 行，TEXT 类型
-- 54.2 端到端：Boss 发布活动（selected_project_id 指向 service_item）→ 顾客报名 200，
--      不再抛「项目容器不存在」；getSignupDetail 返回 quota/teamTarget 正常

-- 55) ★schedule_change_log 审计字段修复（v1.81 新增，[98]M22_05）
-- 55.1 schedule_change_log 四审计字段齐备（预期 4 行）
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='schedule_change_log'
--    AND COLUMN_NAME IN ('create_by','create_time','update_by','update_time');
-- 预期：4 行；create_by/update_by 为 varchar(64)，create_time/update_time 为 datetime
-- 55.2 端到端：发布排班后 schedule_change_log INSERT 成功，不再报 Unknown column

-- 56) ★活动费用与参与增强（v1.81 新增，[99]M22_06）
-- 56.1 promotion_activity 新增 4 列就绪（预期 4 行）
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='promotion_activity'
--    AND COLUMN_NAME IN ('custom_price','auto_calculated_price','participant_count','is_locked');
-- 预期：4 行
-- 56.2 project_signup 新增 pay_status 列就绪（预期 1 行）
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='project_signup' AND COLUMN_NAME='pay_status';
-- 预期：1 行，DEFAULT='PENDING_PAY'
-- 56.3 菜单 7159 存在（预期 1 行，F 类型，perms=micro:promo:lock）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu WHERE menu_id = 7159;
-- 56.4 role_template boss 含锁定权限（预期 3 行，has_lock=1）
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"micro:promo:lock"') AS has_lock
-- FROM role_template WHERE role_code = 'boss' AND version_type IN ('micro','micro_pro','micro_lite');
-- 56.5 activityPayReminderJob 定时任务已注册（预期 1 行，trigger_status=1）
-- SELECT executor_handler, schedule_conf, trigger_status FROM xxl_job_info
--  WHERE executor_handler = 'activityPayReminderJob';

-- 46.6 boss 无残留死链菜单关联（预期 0 行）
-- SELECT rm.* FROM sys_role_menu rm
-- JOIN sys_role r ON r.role_id=rm.role_id AND r.role_key='boss'
-- JOIN sys_tenant t ON t.id=r.tenant_id AND t.version_type='micro_pro'
-- WHERE rm.menu_id IN (7186,7187,7188);

-- 38.3 ★补录 1130/1133 后侧边栏仍无系统管理（F 类型不渲染）+ micro_lite 侧边栏仍仅 4 项
-- SELECT COUNT(*) AS system_menu_leak
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND rm.menu_id IN (1, 118);   -- 预期：0
-- SELECT t.id, GROUP_CONCAT(m.menu_id ORDER BY m.menu_id) AS sidebar_menu_ids
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.menu_type IN ('M', 'C') AND m.status = '0'
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--  GROUP BY t.id;   -- 预期：每租户 sidebar_menu_ids = '7100,7101,7150,7151'

-- =====================================================================
-- 小微版能力边界（v1.37，交付客户须知）
-- ---------------------------------------------------------------------
-- ● 保留（小微版标配）：
--   - 手机号即身份：一手机号 = 一 sys_user（跨端跨租户唯一），user_tenant_role 支持一人多租户多角色
--   - 一店一租户：老板(boss) + 业务员(salesman) + 顾客(customer) 三视图；PC 管理端 + 微信小程序双端
--   - ★分档机制（v1.14 新增）：micro_lite（免费·仅推广发布/店铺管理/活动浏览） / micro_pro（收费·全经营）；
--     升级 lite→pro = UPDATE tenant.version_type + 刷新菜单缓存，零数据迁移
--   - ★角色可见性硬隔离（v1.14 新增）：sys_role.tenant_type_scope=micro/enterprise/both，
--     可选角色列表 SQL 层过滤，跨产品线角色在查询阶段排除
--   - ★资金平台管理菜单（v1.14 新增）：一级目录 1300 统一收纳（租户管理/标准角色/行业模板/
--     菜单管理/角色模板/版本档位管理），与若依原生「系统管理」并列
--   - ★版本档位管理（v1.14 新增）：system:edition:list，维护 micro_lite/micro_pro/basic/advanced/pro 五档
--   - ★新建租户级联（v1.14 新增）：选产品线(tenant_type) → 级联版本档位(version_type) → 级联行业模板 → 预览默认角色
--   - ★三层控制模型（v1.14 新增）：admin 角色模板（权限天花板）∩ platform_admin 页面装修（排版）= 最终渲染
--     ★第①层入口 = 资金平台管理菜单/角色模板管理(system:roleTemplate:*)；
--       第②层入口 = 店铺配置/页面装修(micro:page:*，M15_14 补建，URL /microConfig/pageDecorate)；
--       第③层 = 后端 @SaCheckPermission 接口级兜底；天花板为空则不过滤（向后兼容）
--   - ★lite 档活动浏览页（v1.14 新增）：promotion_activity 租户级表 + micro:activity:browse 只读菜单，
--     lite 档 salesman/customer 唯一菜单，无需登录可浏览
--   - ★防爽约/黑名单（v1.14 新增，仅 micro_pro）：customer_credit（累计爽约 + 黑名单）+ no_show_record（明细）
--     + noShowCheckJob（每 10 分钟扫描）+ 满 3 次自动拉黑 + 老板手动移出为唯一解除出口
--   - ★推广分销与激励（v1.16 新增，仅小微版 FEAT_PROMOTION_DISTRIBUTION）：推广码（门店码+个人码，一人一码永久有效）
--     + 扫码归因（谁最后扫码业绩归谁，交接点锁定不回溯）+ 拉新奖励（首消触发仅一次，员工现金「标记已线下发放」/顾客站内优惠券）
--     + 首单识别（手机号唯一维度，按活动独立判断，与归属解耦）+ 老板 PC 推广管理（数据/归因/天周月统计/奖励配置/首单配置/码启停）
--     + 员工与顾客小程序「我的推广码」页；★基础版/高级版/专业版不部署、不开放入口
--   - ★推广分销按渠道拆分（v1.30 新增）：业务员推广（现金）+ 顾客推广（优惠券），
--     奖励记录按 reward_channel 拆为两独立页面（7189/7190）
--   - ★租户管理员菜单（v1.30 新增，T15.33）：platform_admin 三件套（版本升级/角色映射/人员管理）
--     + 小程序主题选择，menu_id 7500~7509，与 admin 的 1300 完全隔离
--   - ★原子资源管理（v1.38 新增，T15.38，V3.7 §4.2）：platform_admin「原子资源管理」目录(7510)
--     统一收纳 项目管理(7511~7513)/房间·空间管理(7514~7516)/角色管理(7504)/员工库(7507) 四原子；
--     ★房间/空间 = service_category.category_type='space' 分类下的 service_item（元/小时按时长计价，零新表，
--     天然兼容 booking_order.service_item_id/price_change_log 改价留痕）；数据源与角色映射自愈同模式，
--     租户首次进入/建租户时从 admin 行业模板 preset_config.projects/rooms 自动初始化落库（幂等仅空表补录，V3.5 §16.4 脱钩）
--   - ★活动图片上传+背景主题（v1.30 新增，T15.35）：promotion_activity 补 logo_url/theme_key，
--     4 套预设主题（theme_default/warm/fresh/elegant）
--   - ★图片校验与渲染管线（v1.31 新增 / v1.33 收敛，T15.35§六，对齐说明书 5.2/8）：四重上传校验（MIME/750x400头图&750x1000海报±5%/≤200KB/微信imgSecCheck）
--     + Logo 透明底自动衬底（圆形/圆角+毛玻璃）80x80 归一化 + 海报顶部渐变遮罩 + k-means(k=3) 主色提取推导卡片配色
--     + 三层渲染融合（预设色打底→海报带遮罩→内容卡片毛玻璃）；★复用 T12.x 既有扩展点落地：
--     @Primary WechatImgSecCheckSecurity 覆盖桩真调微信 wxa/img_sec_check（留痕 biz_content_security_log）、
--     @Primary OssRenderImagePipeline 覆盖桩渲染并回写（任务 biz_image_task、成品 URL tenant_theme.logo_rendered_url/rendered_poster_url）；
--     M15_19b 唯一增量=tenant_theme 补 poster_dominant_color/poster_accent_color 2 列（不新建 img_sec_check_log、不加 promotion_activity 冗余列）；
--     ★V1 采用同步契约（submit 内联渲染+回写，单次<1s），biz_image_task.status/result_url/retry_count 已为异步化预留；海报大小口径统一 ≤200KB（原 500KB 废弃）
--   - ★小程序全局主题+首页欢迎语（v1.30 新增，T15.36）：sys_tenant.app_theme_key，
--     主题下发链路 PC→tenant_config→/page/config→App.vue→theme.js
--   - ★主题显示名映射+多租户选择（v1.31 新增，T15.36§五§六，对齐说明书 5.2/11.4）：sys_tenant 补 theme_display_name_map（营销名「暗夜桌游/暖光聚会/清新日系/霓虹赛博」与代码名双口径，theme_key 稳定不变）+ default_logo_url（活动 logo 空时兜底）（M15_20b）；
--     多租户选择流程：user_tenant_role≥2 条弹「我的店铺」列表（pages/micro/tenant/select.vue，pages.json 18→19），登录响应体扩展 tenants 数组，user_tenant_bind 语义由既有 user_tenant_role 承接不新建表
--   - ★首页盈亏指标+每月固定支出（v1.30 新增，T15.37）：monthly_fixed_cost 表，
--     盈亏公式：本月盈利=本月收入-(流水支出+Σ本月固定支出)，菜单 7195~7199
--   - ★通用评价与点赞（v1.24 新增，M17，T17.1~T17.5，仅 micro_pro）：
--     user_rating（1~5 星 + 内容 + 图片 + 场景 + 回复 + 屏蔽）+ user_like（点赞 toggle），
--     评价管理菜单 7160~7164（micro:rating:*），功能开关 FEAT_RATING（第 35 项，组 E），
--     ★micro_lite/basic/advanced/pro 均不生效；全部评价接口走 @RequireFeature("FEAT_RATING")
--   - ★M18 V3.7 菜单骨架缺口补齐（v1.29 任务文档 / v1.34 归档，menu_id 7600~7679）：
--     语音合规四表（voice_compliance_rule/voice_recording_policy/voice_compliance_hit/sensitive_word_library，
--     敏感词库分层：tenant_id=NULL 平台级 + 非NULL 租户自定义）；
--     固定支出标签 + 周期规则（fixed_cost_tag/period_rule，租户管理员 PC 7620~7628）；
--     预约策略配置（booking_policy UNIQUE(tenant_id) upsert，租户管理员 PC 7630 + 保存按钮 7631）；
--     拉新奖励配置扩展（promotion_reward_config 补 trigger_condition/reward_limit_type/reward_limit_value 三列，老板 PC 7640）；
--     老板员工业绩榜（复用 booking_order+user_rating+sys_user，不建表，老板 PC 7650）；
--     License 模板管理与使用报表（sys_license_template 平台级 + 4 条预置模板，Admin PC 7660~7661）；
--     Admin 运维审计补齐（sys_alert_rule 平台级 + 4 条预置告警规则，Admin PC 7670~7671）；
--     M18_08 汇总补丁：role_template boss/platform_admin 追加 M18 新权限 + sys_role_menu 存量租户关联补录
--   - ★行业模板开箱（v1.35 T15.33 扩展至 23 行业）：
--     休闲娱乐 6（桌游/剧本杀/棋牌室/KTV/电竞馆/轰趴馆）+
--     生活服务 7（足浴/推拿/SPA/美容美发/美甲美睫/洗衣洗鞋/宠物美容）+
--     运动健身 7（健身房/瑜伽馆/普拉提/羽毛球馆/网球馆/游泳馆/舞蹈室）+
--     专业工作室 3（摄影写真/理发工作室/家政保洁团队）；
--     每行业预置角色映射（boss/salesman/customer 三标准角色 + 行业化业务角色显示名）
--     + 服务分类 + 结算规则 + 计量单位 + 5~7 条真实项目预设（源自大众点评上海公开团购）；
--     market（买菜/生鲜）V3.5 §10 排除，SET status='1' 停用保留数据兼容既有租户
--   - 角色映射机制：平台管理员在 tenant_role_mapping 挑选/改名/启停（如「助教/配送员/教练/技师」），
--     权限永远由 admin 在 role_template 预置（平台管理员不可授权，只可映射改名）
--   - 模块化页面：单一页面壳 + 10 区块组件库（营收/待收/员工照片/活动 banner/快捷预约/我的排班/
--     打卡/店铺信息/公告栏），page_block_config 按 role_code×page_key 装配，tenant_id=0 为系统模板
--   - 服务定价：service_item 五种计价方式（时长/重量/次数/人数/份），改价留痕 price_change_log
--   - 预约结算闭环：顾客扫码预约(booking_order) → 到店服务 → 老板确认收款 → 员工业绩结算(staff_settlement)
--     ★booking_order 增 NO_SHOW 态 + expire_time 列（v1.14 防爽约判定基准）
--   - 轻量记账：cash_flow 收入/支出流水 + micro_daily_revenue 每日营收快照（毛利/支出/净利）
--   - 分享裂变：share_scene 扫码带租户号，跨店引流
--   - 共用小程序引导态：门店选择/搜索、?tenant= 与 AppID 反查（M8，仅公开字段）
--   - 计费与合规：套餐/试用到期停止/订阅消息配额（M14）+ ★分档计费 5 档套餐（v1.14）
--   - ★clonePresetRoles 小微版适配（v1.17 修复）：新建小微版租户时，克隆预设角色自动按
--     role_template(version_type×role_code) 授权菜单，不再依赖 sys_edition_menu_template
-- ○ 不含（企业版专属，小微版不部署对应脚本、不开放入口）：
--   - ★V1 零在线支付：无在线收款/无支付分账/无顾客在线付款（micro:customer:pay 不存在），仅「到店付」
--   - 预算编制闭环 / 费用大类三层科目 / 报销管控 / 规则引擎 / 发票验真（企业版 M5/M6）
--   - 项目容器 / 项目资金流 / 活动组队引擎 / 主题海报管线 / 直播公告（企业版 M1_02/M1_07/M11/M12）
--   - 多组织 / 多账套 / 人脸比对 / 开放平台 API / 企业微信通道（专业版 M7/M13）
-- =====================================================================
