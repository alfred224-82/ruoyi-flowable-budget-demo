-- =====================================================================
-- M15_14 小微版 PC 前端菜单缺口幂等修补脚本（T15.6 / T15.16 / T15.17 / T15.29 / T15.30 配套）
-- 版本：v1.0  日期：2026-09-08
-- 依据：《资金管控系统开发任务文档》v1.14 T15.6（菜单+client_type）/ T15.16·T15.17（页面装修）
--       / T15.29（资金平台管理菜单+版本档位管理）/ T15.30（三层控制模型）
--       《小微版完整方案》v10.5 §5.4（单一页面壳 + page_block_config）
-- 前置依赖：M15_06（micro 菜单全表 7000~7422）、M15_10（role_template 分档配方）、
--           M15_11（活动浏览页 7330/7430）、M15_12（资金平台菜单 1300~1314）、M15_13（顾客管理 7170~7172）
-- 规范：
--   1. 本脚本为【前端交付配套修补】，不建表、不改表结构，只动 sys_menu / sys_role_menu
--   2. 新增区段 7063~7069 先删后插幂等；存量菜单修正一律用 UPDATE ... WHERE menu_id=?（可重复执行）
--   3. ★所有 UPDATE 均带 WHERE menu_id 精确定位，不使用 LIKE / 区间，避免误伤存量企业版菜单
--   4. 修正口径来自三方核对：后端 @SaCheckPermission ↔ sys_menu.perms ↔ sys_menu.component ↔ ruoyi-ui .vue 落盘路径
-- =====================================================================
-- 【核对结论摘要】
--   M15_06/09~13 登记的 PC 菜单 component 共 25 个：
--     ✔ 一致（19 个，.vue 已落盘且路径完全匹配）：
--        micro/{industry,roleTemplate,userRole,tenantRole,category,service,priceLog,shop,payInfo,notify}/index
--        micro/{dashboard/index,order/detail,booking/index,settle/index,settle/history,income/index,expense/index}
--        micro/{report/weekly,report/monthly,report/profit,report/export,report/cashFlow}
--        micro/{promo/index,shopMgr/toggle,shopMgr/hours,customer/index} + system/edition/index
--     ✘ 指向不存在的 .vue（5 个，点菜单必白屏）：7012 / 7040 / 7051 / 7055 / 7135  → §3 修正
--     ✘ 完全无菜单登记（1 个，功能已交付但无入口）：micro/pageDecorate/index      → §2 补建
--     － 装饰性 perms（后端零命中，由 role_template 天花板与菜单可见性使用）        → §5 登记说明，不改
-- =====================================================================

-- ----------------------------
-- 1. 幂等清理（本脚本自有区段 7063~7069）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7063 AND 7069;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 7063 AND 7069;

-- ----------------------------
-- 2. 补「页面装修」菜单（T15.16 / T15.17，menu_id 7063~7066）
--    ★缺口：ruoyi-ui/src/views/micro/pageDecorate/index.vue 已交付，
--      后端 PageBlockConfigController(/micro/page) 四个 perms（list/edit/add/remove）齐备，
--      但 M15_06~M15_13 无任何菜单登记 → 平台管理员无入口，三层控制模型第②层（排版）无法配置。
--    ★挂载：parent=7010「店铺配置」(path='microConfig')，path='pageDecorate'
--      → 最终 URL /microConfig/pageDecorate，与 micro/roleTemplate/index.vue 的 gotoPageDecorate()
--        硬编码跳转路径一致（第①层天花板 → 第②层排版的联动入口）。
--    ★★父节点类型硬约束：SysMenuServiceImpl.buildMenus 仅当 menu.getMenuType()=='M'(目录) 时才递归处理子节点，
--      C 类父节点下的 C 类子菜单【不会生成路由】（点菜单 404）。已核实 7010 menu_type='M'，故 7063 可正常生成路由；
--      7064~7066 为 'F'(按钮) 类，不参与 buildMenus，仅由 selectMenuPermsByUserId（不按 menu_type 过滤）提供 perms。
--      ⚠ 后续若把 7010 改为 'C' 类，必须同步把 7063 提到 M 类目录下，或改在 ruoyi-ui router/index.js 的
--        dynamicRoutes 中静态注册（验收见 §6.8）。
--    ★perms 对应后端端点：
--      micro:page:list   → GET /micro/page/list（装修面板）、GET /micro/page/{id}（区块详情）
--      micro:page:edit   → POST /micro/page/decorate（整页全量覆盖）、POST /micro/page/resetDefault（恢复默认）、PUT /micro/page
--      micro:page:add    → POST /micro/page（新增单区块）
--      micro:page:remove → DELETE /micro/page/{ids}（page_block_config 无 del_flag，物理删除）
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7063, '页面装修', 7010, 6, 'pageDecorate', 'micro/pageDecorate/index', NULL, 1, 0, 'C', '0', '0', 'micro:page:list', 'pc', 'build', 'admin', NOW(), '★三层控制模型第②层：page_block_config 区块勾选+拖拽排序+参数+恢复默认；可选区块上限由第①层 role_template.block_keys 天花板决定'),
(7064, '保存装修/恢复默认', 7063, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:page:edit', 'pc', '#', 'admin', NOW(), 'decorate 整页全量覆盖(先删后插)；resetDefault 从 tenant_id=0 系统模板克隆到目标租户'),
(7065, '新增区块', 7063, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:page:add', 'pc', '#', 'admin', NOW(), '超管默认写入系统模板(tenant_id=0)；老板写入本租户'),
(7066, '删除区块', 7063, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:page:remove', 'pc', '#', 'admin', NOW(), '物理删除，可批量');

-- 授予超管 role_id=1（业务角色授权由 role_template 驱动，见 M15_07 / M15_10）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 7063 AND 7069;

-- ----------------------------
-- 3. 修正指向不存在 .vue 的 PC 菜单（点菜单必白屏/404）
-- ----------------------------
-- 3.1 「用户列表」7012~7016：无 /micro/user 控制器、无 micro/user/index.vue、无 micro:user:* 后端 perms
--     ★设计口径（v10.5 §5.1）：手机号为全局身份锚点，用户池全局共享（sys_user），小微版不另建用户体系；
--       一人多租户多角色由 user_tenant_role 承载，租户内「用户→业务角色」绑定已由
--       7017 角色分配（micro/userRole/index，perms micro:userRole:*）承担且已交付。
--     → 7012 re-point 到若依原生 system/user/index；7013~7015 按钮 perms 同步 re-point 到 system:user:*；
--       7016「禁用用户」隐藏（原生用户管理的 status 开关已覆盖，且与 7014 同为 system:user:edit 会重复）。
--     ★安全性：micro:user:* 未出现在 M15_07 / M15_10 任何 role_template.menu_keys 配方中，re-point 无天花板影响。
UPDATE `sys_menu` SET `component` = 'system/user/index', `perms` = 'system:user:list',
       `remark` = '★M15_14 re-point：原 component=micro/user/index、perms=micro:user:list 均无后端；用户池全局共享(手机号身份锚点)，复用若依原生用户管理；租户内角色绑定见同级「角色分配」'
 WHERE `menu_id` = 7012;
UPDATE `sys_menu` SET `perms` = 'system:user:add',
       `remark` = '★M15_14 re-point：原 micro:user:add 无后端'
 WHERE `menu_id` = 7013;
UPDATE `sys_menu` SET `perms` = 'system:user:edit',
       `remark` = '★M15_14 re-point：原 micro:user:edit 无后端；含用户状态启停'
 WHERE `menu_id` = 7014;
UPDATE `sys_menu` SET `perms` = 'system:user:resetPwd',
       `remark` = '★M15_14 re-point：原 micro:user:resetPwd 无后端'
 WHERE `menu_id` = 7015;
UPDATE `sys_menu` SET `visible` = '1',
       `remark` = '★M15_14 隐藏：禁用用户已并入「编辑用户」(system:user:edit 的 status 开关)，避免与 7014 perms 重复'
 WHERE `menu_id` = 7016;

-- 3.2 四个「菜单+后端双缺」模块：置 visible='1' 隐藏 + remark 标注，避免生产环境点菜单白屏
--     ★安全性：micro:package:* / micro:staff:list|add|edit|disable / micro:settleRule:* / micro:budgetAlert:edit
--       中，仅 micro:budgetAlert:edit 出现在 role_template 配方（M15_07/M15_10 boss 档），其余均无；
--       隐藏菜单不影响天花板 JSON 内容（天花板是独立数据），后端补齐后把 visible 改回 '0' 即可恢复。
UPDATE `sys_menu` SET `visible` = '1',
       `remark` = CONCAT(IFNULL(`remark`, ''), '｜★M15_14 隐藏·待后端补齐：无 ServicePackageController、无 micro/package/index.vue')
 WHERE `menu_id` = 7040;
UPDATE `sys_menu` SET `visible` = '1',
       `remark` = CONCAT(IFNULL(`remark`, ''), '｜★M15_14 隐藏·待后端补齐：MicroStaffController 仅小程序侧(排班/收入/打卡)，无 staff:list|add|edit|disable、无 micro/staff/index.vue')
 WHERE `menu_id` = 7051;
UPDATE `sys_menu` SET `visible` = '1',
       `remark` = CONCAT(IFNULL(`remark`, ''), '｜★M15_14 隐藏·待后端补齐：无 SettleRuleController、无 micro/settleRule/index.vue；当前结算规则内置于 StaffSettlementService')
 WHERE `menu_id` = 7055;
UPDATE `sys_menu` SET `visible` = '1',
       `remark` = CONCAT(IFNULL(`remark`, ''), '｜★M15_14 隐藏·待后端补齐：无 BudgetAlertController、无 micro/budgetAlert/index.vue；perms 保留(role_template 配方引用)')
 WHERE `menu_id` = 7135;

-- ----------------------------
-- 4. 版本档位管理 1310~1314（T15.29）：component 已交付，写操作按钮收敛
--    ★核对：SysEditionController 全类无 @SaCheckPermission，仅提供 4 个只读端点
--      （/system/edition/features、/version-options、/industry-options、/default-role-preview），
--      即 system:edition:list|query|add|edit|bindMenu 五个 perms 后端命中数均为 0，属【菜单可见性 perms】。
--    ★前端处置：ruoyi-ui/src/views/system/edition/index.vue 已交付为【只读五档聚合台】
--      （档位清单 + 功能开关矩阵 + 计费套餐 + 行业模板 + 每档默认角色配方预览），页面内 alert 已明示只读边界，
--      且全页无 v-hasPermi（不依赖 1312~1314 按钮 perms），故隐藏 1312~1314 无任何前端副作用。
--    ★配方写入口统一收敛到 7005「角色模板管理」(system:roleTemplate:add / :bindMenu，后端齐备)。
UPDATE `sys_menu` SET `remark` = CONCAT(IFNULL(`remark`, ''), '｜★M15_14：前端已交付 system/edition/index（只读五档聚合台）；档位配方写入口见「角色模板管理」')
 WHERE `menu_id` = 1310;
UPDATE `sys_menu` SET `remark` = '★M15_14：对应只读端点 /system/edition/features、/version-options、/industry-options、/default-role-preview'
 WHERE `menu_id` = 1311;
UPDATE `sys_menu` SET `visible` = '1',
       `remark` = '★M15_14 隐藏·后端无写端点：SysEditionController 无 @SaCheckPermission 且无 add 接口；档位为枚举常量(EditionFeatureConfig BASIC/ADVANCED/PROFESSIONAL + micro_lite/micro_pro)，不支持新增'
 WHERE `menu_id` = 1312;
UPDATE `sys_menu` SET `visible` = '1',
       `remark` = '★M15_14 隐藏·后端无写端点：功能开关集由 EditionFeatureConfig 常量定义、features 由 sys_tenant.edition 派生，不支持逐档编辑'
 WHERE `menu_id` = 1313;
UPDATE `sys_menu` SET `visible` = '1',
       `remark` = '★M15_14 隐藏·写入口收敛：该档位解锁的菜单集合(role_template.menu_keys)请到「资金平台管理菜单/角色模板管理」维护(perms system:roleTemplate:bindMenu)'
 WHERE `menu_id` = 1314;

-- ----------------------------
-- 5. 装饰性 perms 登记说明（★本节仅注释，不执行任何变更）
--    以下 perms 后端 @SaCheckPermission 命中数为 0，但被 role_template.menu_keys 配方引用、
--    承担【菜单可见性天花板】职责（三层控制模型第①层），故一律保留原值，不得改写：
--      · micro:dashboard:view (7101) / micro:dashboard:today (7202)
--          → micro/dashboard/index.vue 已交付，实际调 /micro/report/today|weekly|trend(micro:report:weekly)
--            与 /micro/report/cashFlow(micro:report:cashFlow)；同角色必然同时获授 7141/7145，故页面可用
--      · micro:order:detail (7116)
--          → micro/order/detail.vue 已交付，实际调 /micro/booking/{id}(micro:booking:list)
--            与 cancel/complete/receive(micro:booking:cancel|complete|edit)；同角色必然同时获授 7111~7115
--      · micro:activity:browse (7330 / 7430)
--          → MicroActivityBrowseController 刻意只用 @SaCheckLogin + security.excludes 白名单
--            （v10.5 §5.5：lite 档活动浏览页对未登录游客开放），perms 仅作菜单可见性用
--      · micro:customer:book (7410) / selectService (7411) / selectTime (7412)
--          → 小程序顾客端赛道（本轮 PC 交付不含 ruoyi-app），MicroCustomerController 仅对
--            submitBook/myBook/cancelBook/payInfo/shop 五个端点做 perms 校验，选项目/选时间为纯前端步骤
--      · micro:budgetAlert:edit (7135) → 见 §3.2，菜单已隐藏、perms 保留待后端补齐
-- ----------------------------

-- ----------------------------
-- 6. 验收查询（执行后人工核对）
-- ----------------------------
-- 6.1 页面装修菜单已补建且挂到「店铺配置」下
-- SELECT menu_id, menu_name, parent_id, path, component, perms, client_type FROM sys_menu
--  WHERE menu_id BETWEEN 7063 AND 7069 ORDER BY menu_id;
-- 预期 4 行：7063(micro:page:list, component=micro/pageDecorate/index, parent=7010)
--          + 7064(micro:page:edit) + 7065(micro:page:add) + 7066(micro:page:remove)
--
-- 6.2 ★URL 拼接校验：7010.path + '/' + 7063.path 必须 = /microConfig/pageDecorate
-- SELECT CONCAT((SELECT path FROM sys_menu WHERE menu_id=7010), '/', path) AS full_url FROM sys_menu WHERE menu_id=7063;
--
-- 6.3 ★全库不再有 PC 菜单指向不存在的 .vue（人工比对 ruoyi-ui/src/views 下同名文件）
-- SELECT menu_id, menu_name, component, visible FROM sys_menu
--  WHERE client_type='pc' AND menu_type='C' AND menu_id BETWEEN 7000 AND 7499 ORDER BY menu_id;
-- 预期：7040/7051/7055/7135 的 visible='1'；7012 的 component='system/user/index'；其余 visible='0' 且 .vue 均存在
--
-- 6.4 用户列表 re-point 结果
-- SELECT menu_id, menu_name, component, perms, visible FROM sys_menu WHERE menu_id BETWEEN 7012 AND 7016 ORDER BY menu_id;
-- 预期：7012(system/user/index, system:user:list, visible=0) / 7013(system:user:add) / 7014(system:user:edit)
--      / 7015(system:user:resetPwd) / 7016(visible=1)
--
-- 6.5 版本档位管理写按钮已收敛
-- SELECT menu_id, menu_name, perms, visible FROM sys_menu WHERE menu_id BETWEEN 1310 AND 1314 ORDER BY menu_id;
-- 预期：1310/1311 visible='0'；1312/1313/1314 visible='1'
--
-- 6.6 ★装饰性 perms 未被误改（三层控制模型第①层天花板依赖）
-- SELECT menu_id, perms FROM sys_menu WHERE perms IN
--   ('micro:dashboard:view','micro:dashboard:today','micro:order:detail','micro:activity:browse','micro:budgetAlert:edit');
-- 预期 6 行：7101 / 7202 / 7116 / 7330 / 7430 / 7135
--
-- 6.7 ★超管已获授页面装修菜单
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id=1 AND menu_id BETWEEN 7063 AND 7069;  -- 预期 4
--
-- 6.8 ★父节点必须为 M 类，否则 7063 路由不会生成（buildMenus 不递归 C 类父节点）
-- SELECT menu_id, menu_name, menu_type FROM sys_menu WHERE menu_id IN (7010, 7063);
-- 预期：7010 menu_type='M' 且 7063 menu_type='C'；若 7010 非 'M'，页面装修菜单将出现在侧边栏但点击 404
--
-- 6.9 ★运行时验证（三层控制模型联动）：
--   ① 平台管理员登录 → 「店铺配置 / 页面装修」可点开（URL /microConfig/pageDecorate），选角色+页面 → 区块列表加载
--   ② 勾选区块 + 拖拽排序 + 设参数 → 保存装修 → page_block_config 整页覆盖；点「恢复默认」→ 从 tenant_id=0 克隆
--   ③ 「资金平台管理菜单 / 角色模板管理」→ 配方详情 → 点「前往页面装修」→ 携带 roleCode 跳转 ①
--   ④ 「资金平台管理菜单 / 版本档位管理」→ 只读五档聚合台（无新增/编辑/绑定按钮）→ 点「角色模板」跳 7005
--   ⑤ 小程序端 /page/config?pageKey=xxx 返回 = page_block_config ∩ 当前登录角色 perms（已剔除无权限区块）
