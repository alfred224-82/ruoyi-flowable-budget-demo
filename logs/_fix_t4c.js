/**
 * Task 4 · 第三批：母文档 M15 + 第三章统计表 + v1.19 状态分布说明
 * 依据：logs/_facts_mp.txt / _facts_mp_bug.txt / _facts_m15mp.txt
 *   ruoyi-app 实测 34 个 .vue（micro 18）＝pages.json 注册 34 条，零漂移
 *   config/index.js 43 个 /micro/* API 键；18 页面消费 79 个合法键，0 拼写漂移
 */
const fs = require('fs');
const F = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
let t = fs.readFileSync(F, 'utf8');
const eol = t.includes('\r\n') ? '\r\n' : '\n';
const log = [];
let fail = 0;
function rep(tag, find, to) {
  const n = t.split(find).length - 1;
  if (n !== 1) { log.push('  !! [' + tag + '] hit=' + n + ' (expect 1) SKIP'); fail++; return; }
  t = t.replace(find, to);
  log.push('  ok [' + tag + ']');
}

// ============================================================ M15 note
rep('M15-note头',
  '> **v1.15 对账结果（✅ 14 / 🔄 0 / 🟡 14 / 🆕 4）**：M15 在 v1.14 统计表中为 🆕 **0/32**（本文档**系统性偏差最严重的里程碑**，亦即本次对账的直接起因）。实测：**SQL 14 个脚本（`M15_01`~`M15_14`）+ 后端 20 个 `micro/*` Controller + `SysEditionController` + PC 前端 28 个 `views/micro/*` 页面已全量交付**，唯一整体缺失的赛道是**小程序端**（`ruoyi-app/pages` 全量 16 个 `.vue` 中无任何小微版页面）。',
  '> **v1.19 对账结果（✅ 17 / 🔄 0 / 🟡 15 / 🆕 0）**：M15 在 v1.14 统计表中为 🆕 **0/32**（本文档**系统性偏差最严重的里程碑**，亦即历次对账的直接起因）。实测：**SQL 14 个脚本（`M15_01`~`M15_14`）+ 后端 20 个 `micro/*` Controller + `SysEditionController` + PC 前端 28 个 `views/micro/*` 页面 + 小程序端 18 个 `pages/micro/*.vue` 已全量交付，四条赛道（SQL/BE/PC/MP）全部开工，🆕 清零**。**★v1.15「唯一整体缺失的赛道是小程序端（`ruoyi-app/pages` 全量 16 个 `.vue` 中无任何小微版页面）」为探测缺陷导致的误判**——当时的 glob `micro/*.vue` 匹配不到 `micro/activity/browse.vue` 这类二级目录页面；实测 `ruoyi-app` 共 **34 个 `.vue`**（企业版 16 + 小微版 **18**），与 `pages.json` 注册的 **34 条零漂移**（其中 micro 18 条），`config/index.js` 含 **43 个 `/micro/*` API 键**，18 个页面共消费 **79 个合法 `config.api.*` 键、0 处拼写漂移**。');

rep('M15-降为🟡计数', '> - **降为 🟡（14 项）**：**T15.8**', '> - **维持 🟡（13 项，T15.12 本轮翻 ✅ 已移出；另有 T15.15/T15.20 由 🆕 翻入，合计 15 项）**：**T15.8**');

rep('M15-T15.8理由',
  '**T15.8** 手机号统一登录——后端按 `client_type` 过滤菜单已通，但小程序 `wx.login` + `getPhoneNumber` 一键登录页未开工；',
  '**T15.8** 手机号统一登录——后端按 `client_type` 过滤菜单已通；小程序侧微信一键登录**已落盘**（`pages/login/index.vue` L96 `uni.login` 取 code、L84 `post(config.api.xcxLogin, { xcxCode: code, tenantId: this.store.tenantId })`、L59 账号登录同样携带选定门店 `tenantId` 防跨门店串号），**残留缺口收窄为 `getPhoneNumber` 手机号一键授权全仓 0 命中**——当前身份锚点为「账号密码 + 微信 code」，与核心约束 ②「手机号为唯一身份锚点」尚未对齐；');

rep('M15-T15.12/T15.14理由',
  '**T15.12 / T15.14** 预约管理与收支报表——老板 PC 页（`micro/booking`、`order/detail`、`income`、`expense`、`dashboard`、`report/*`）已落盘，**顾客小程序端未开工**；',
  '**T15.14** 收支记录与报表——老板 PC 页（`micro/income`、`micro/expense`、`micro/dashboard`、`micro/report/*`）+ **小程序报表页（`pages/micro/report/index.vue`、`report/trend.vue`）均已落盘**（周报/月报/资金水位/现金流键齐备），**残留缺口收窄为「小程序老板端改报价」未落地**（与 T15.20 同源）；');

rep('M15-T15.16理由',
  '**T15.16** 模块化页面架构——后端 `/page/config` 已交付（含 `filterByCeiling` 交集），**小程序区块组件库与页面壳未开工**；',
  '**T15.16** 模块化页面架构——后端 `/page/config` 已交付（含 `filterByCeiling` 交集），小程序侧 `config/index.js` L126 已备 `microPageConfig: \'/page/config\'` 键，**但 0 个页面调用该键、区块组件库（`<component is="{{item.blockKey}}" />` 动态渲染）0 命中**；且实际落盘的是 `bossHome/`、`staffHome/`、`customerHome/` **三套独立页面壳**，与核心约束 ⑥「小程序页面 = 单一页面壳 + 区块配置，不存在 boss/home、salesman/home 等多套页面」及本任务交付物 ④ **直接冲突**——这是 T15.15/T15.20/T15.28 不能全翻 ✅ 的共同根因；');

rep('M15-T15.28理由',
  '**T15.28** 活动浏览页——`PromotionActivityServiceImpl` + `M15_11` + 菜单 `micro:activity:browse` 已落地，**小程序只读页未开工**；',
  '**T15.28** 活动浏览页——`PromotionActivityServiceImpl` + `M15_11` + 菜单 `micro:activity:browse` + **小程序只读页 `pages/micro/activity/browse.vue` 均已落地**（L91 `options.tenantId` 解析、L4/L70 缺租户号时显示提示而非空白、L107 按 `tenantId` 拉数据，符合「无需登录可浏览」），**残留缺口收窄为「升级 pro 后同页自动多出经营入口（页面壳不变）」未达成**——三套独立壳使 lite→pro 扩围需换页而非同页增量；');

rep('M15-T15.30理由',
  '**T15.30** 三层控制模型——第①层（`role_template`）+ 第②层（页面装修）+ `filterByCeiling` 已落地，**第③层小程序最终渲染未联调验证**；',
  '**T15.30** 三层控制模型——第①层（`role_template`）+ 第②层（页面装修）+ 后端 `filterByCeiling` 已落地，**第③层小程序最终渲染未实现**（v1.19 实测：`ruoyi-app` 全量对 `filterByCeiling`/`ceiling` 关键字 **0 命中**，前端无「天花板 ∩ 排版」交集逻辑）；');

rep('M15-T15.32理由',
  '**T15.32** 防爽约判定 + 黑名单——`noShowCheckJob`（`versionTypes={"micro_pro"}`）+ `micro/customer/index` + 「顾客管理」菜单已交付，**小程序侧「预约按钮置灰 + 请联系商家提示」与推送名单过滤未落地**。',
  '**T15.32** 防爽约判定 + 黑名单——`noShowCheckJob`（`versionTypes={"micro_pro"}`）+ `micro/customer/index` + 「顾客管理」菜单已交付，**小程序侧「预约按钮置灰 + 请联系商家提示」与推送名单过滤未落地**（v1.19 实测：`ruoyi-app` 全量对 `is_blacklisted`/`blacklisted` **0 命中**，「请联系商家」文案 **0 命中**，`pages/micro/customerBook/submit.vue` 提交前无黑名单校验）。');

rep('M15-保留🆕4项',
  '> - **保留 🆕（4 项）**：**T15.15** 小程序框架（单包多角色 + `client_type` 过滤）、**T15.18** 业务员小程序页面、**T15.19** 顾客小程序页面、**T15.20** 老板小程序页面——四项均以小程序页面为主交付物，`ruoyi-app` 本轮未开工，**从未实现**。',
  [
    '> - **升为 ✅（3 项，v1.19 小程序端落盘翻转）**：',
    '>   - **T15.12 预约管理（顾客小程序端）**——`pages/micro/customerHome/index.vue`（浏览店铺）+ `customerBook/submit.vue`（选服务/选时间/提交预约，L55 解析 `options.serviceName`）+ `customerBook/list.vue`（我的预约 + 取消）+ `customerBook/detail.vue`（取消预约 + 查看收款方式提示）齐备，对应 perms（浏览店铺/提交预约/我的预约/取消预约/收款方式提示）逐项命中 2~3 处；**零支付自证**——`ruoyi-app` 全量对 `requestPayment`/支付 SDK **0 命中**，`config/index.js` 中 `paymentList`/`paymentApprove`/`paymentReject` 三键属**企业版付款单审批**（`pages/approval/detail.vue` 消费），与顾客在线支付无关，符合核心约束 ⑤ V1 零支付。',
    '>   - **T15.18 业务员小程序页面**——`pages/micro/staffHome/index.vue`（我的排班 + 打卡提交）+ `staffIncome/index.vue`（本周/本月收入）+ `staffIncome/detail.vue`（每单明细）齐备，`micro:staff:schedule`/`micro:staff:income`/`micro:staff:checkin`/`micro:staff:checkinSubmit` 四 perms 在 `config/index.js` 与页面双向命中（3/6/2 处），无管理类菜单入口。',
    '>   - **T15.19 顾客小程序页面**——`customerHome/index.vue` + `customerBook/{submit,list,detail}.vue` + `customerMine/index.vue` 齐备，`micro:customer:shop`/`micro:customer:book`/`micro:customer:myBook` 逐 perms 命中；`customerBook/detail.vue` 与 `customerMine/index.vue` 的「收款方式提示」为纯文案展示，**顾客端零支付代码**（无 `micro:customer:pay`）。',
    '> - **由 🆕 翻 🟡（2 项）**：',
    '>   - **T15.15 小程序框架**——18 个 `pages/micro/*.vue` + `pages.json` 34 条注册（零漂移）+ `utils/roles.js`（`MICRO_TAB_CONFIG` + `isMicroVersion`）+ `config/index.js`（43 个 `/micro/*` 键）+ `App.vue`/`login` 登录分流 + 租户号带入（`activity/browse.vue` L91 解析 `options.tenantId`、`store/select.vue` 多绑定显式选店、`login/index.vue` L59/L84 携带 `tenantId`）**均已落盘**，验收项「扫码/分享链接正确带入 tenant_id」「多租户时弹出选择页」「业务代码无 appid 判断」达成；**残留缺口 = 验收项「菜单/perms 按 `client_type=mini` 过滤（后端返回 mini/both 菜单）」未在小程序侧打通**——`client_type`/`clientType` 在 `ruoyi-app` 全量仅 1 处命中且为 `utils/roles.js` L102 的**注释文本**，menu 树/perms 拉取 **0 命中**，Tab 与页面权限实际由本地静态 `MICRO_TAB_CONFIG` 决定而非后端菜单树，故「老板小程序只返回报表+定价菜单」由前端硬编码保证、后端过滤形同虚设。',
    '>   - **T15.20 老板小程序页面**——`pages/micro/bossHome/index.vue`（今日概览）+ `report/index.vue`（周报/月报/资金水位）+ `report/trend.vue`（趋势）+ `mine/index.vue`（消息提醒）齐备，`micro:report:weekly`/`micro:notify:list` 命中；**本轮已修复 1 个运行期缺陷**——`bossHome/index.vue` L105 引用了未定义变量 `weeklyRes`（上方解构名为 `trendRes`），该 `ReferenceError` 被 `try/catch` 吞掉后导致首页趋势与待处理提醒**静默失效**（已改正）。**残留缺口 = 定价管理快改未落地**：`config/index.js` L118/L119 已备 `microServiceOnSale`/`microServiceChangePrice` 两键，但 `microServiceChangePrice` **0 个页面消费**，`pages/micro/shop/config.vue`（61 行）仅调 `microShopConfig`、**不含价格编辑**，故验收项「改价只暴露价格字段」「改价确认写入 `price_change_log`」在小程序侧未达成（`pages/micro/booking/add.vue` L66 仅**读**在售服务列表）。',
    '> - **🆕 清零（4 → 0）**：T15.15/T15.18/T15.19/T15.20 四项均以小程序页面为主交付物，v1.15 判定「`ruoyi-app` 本轮未开工，从未实现」**已被实测推翻**（18 个 micro 页面 + 34 条注册 + 43 个 API 键），其中 2 项翻 ✅、2 项翻 🟡，M15 不再有 🆕 项。',
  ].join(eol));

// ============================================================ M15 表格
rep('表-T15.12', '| 🟡 T15.12 | **预约管理（老板 PC + 顾客小程序）**', '| ✅ T15.12 | **预约管理（老板 PC + 顾客小程序）**');
rep('表-T15.15', '| 🆕 T15.15 | **小程序框架（单包多角色 + client_type 过滤）**', '| 🟡 T15.15 | **小程序框架（单包多角色 + client_type 过滤）**');
rep('表-T15.18', '| 🆕 T15.18 | **业务员小程序页面（salesman 视图）**', '| ✅ T15.18 | **业务员小程序页面（salesman 视图）**');
rep('表-T15.19', '| 🆕 T15.19 | **顾客小程序页面（customer 视图）**', '| ✅ T15.19 | **顾客小程序页面（customer 视图）**');
rep('表-T15.20', '| 🆕 T15.20 | **老板小程序页面（boss 视图，轻量：只看/改）**', '| 🟡 T15.20 | **老板小程序页面（boss 视图，轻量：只看/改）**');

// ============================================================ M15 v1.14 PC 前端交付进度表
rep('进度表-引言',
  '已同步交付并通过 `npm run build:prod`；**小程序端（ruoyi-app）本轮未开工**。逐任务口径：',
  '已同步交付并通过 `npm run build:prod`；**小程序端（ruoyi-app）在 v1.14 时点未开工，已于 v1.19 实测确认落盘 18 个 `pages/micro/*.vue`**（下表「小程序」列为 v1.19 复核结果）。逐任务口径：');

rep('进度表-T15.11', '| ❌ 7210~7213 定价快改 |', '| ⚠️ `microServiceChangePrice` 键已备但 0 页面消费 |');
rep('进度表-T15.16',
  '| ❌ 区块组件库 + 页面壳 | 🆕 小程序侧待开工 |',
  '| ⚠️ 三套独立壳（`bossHome`/`staffHome`/`customerHome`），区块组件库 0 命中 | 🟡 与核心约束 ⑥ 冲突，待改造为单一页面壳 |');
rep('进度表-T15.30',
  '| ❌ 第③层渲染 | 🆕 待小程序侧联调验证 |',
  '| ❌ 第③层渲染（`ceiling`/`filterByCeiling` 0 命中） | 🟡 待小程序侧实现天花板 ∩ 排版交集 |');
rep('进度表-T15.32',
  '| ❌ 预约按钮置灰 | ⚠️ PC 侧完成；小程序侧「按钮置灰 + 请联系商家提示」待补 |',
  '| ❌ 预约按钮置灰（`is_blacklisted` 0 命中） | 🟡 PC 侧完成；小程序侧「按钮置灰 + 请联系商家提示」与提交前黑名单校验待补 |');

rep('进度表-尾注',
  '小程序侧（T15.15 / T15.18 / T15.19 / T15.20）本轮未开工。',
  '小程序侧（T15.15 / T15.18 / T15.19 / T15.20）**已于 v1.19 实测落盘**：`bossHome`/`staffHome`/`staffIncome`/`customerHome`/`customerBook`/`customerMine`/`report`/`activity`/`shop`/`mine` 共 18 个 `.vue`，T15.18/T15.19 翻 ✅，T15.15（`client_type` 菜单过滤未打通）/T15.20（定价快改未落地）翻 🟡。');

// ============================================================ 第三章统计表
rep('统计-M5', '| M5 基础版功能（重定边界） | 19 | 15 | 0 | 4 | 0 | 18 | 1 | 0 |',
  '| M5 基础版功能（重定边界） | 19 | 17 | 0 | 2 | 0 | 18 | 1 | 0 |');
rep('统计-M10', '| M10 多业态平台底座 | 8 | 2 | 0 | 4 | 2 | 5 | 3 | 0 |',
  '| M10 多业态平台底座 | 8 | 3 | 0 | 3 | 2 | 5 | 3 | 0 |');
rep('统计-M14', '| M14 商业化计费与合规 | 5 | 1 | 0 | 3 | 1 | 2 | 2 | 1 |',
  '| M14 商业化计费与合规 | 5 | 0 | 0 | 4 | 1 | 2 | 2 | 1 |');
rep('统计-M15', '| M15 小微版专属功能 | 32 | 14 | 0 | 14 | 4 | 27 | 5 | 0 |',
  '| M15 小微版专属功能 | 32 | 17 | 0 | 15 | 0 | 27 | 5 | 0 |');
rep('统计-合计', '| **合计** | **160** | **85** | **6** | **44** | **25** | **120** | **33** | **7** |',
  '| **合计** | **160** | **90** | **6** | **43** | **21** | **120** | **33** | **7** |');

// ============================================================ v1.19 状态分布说明（追加在 v1.16 说明之后）
rep('v1.19说明',
  'M16 **仅小微版启用**，不影响基础版/高级版/专业版任何任务状态。',
  [
    'M16 **仅小微版启用**，不影响基础版/高级版/专业版任何任务状态。',
    '',
    '> **v1.19 状态分布说明（小程序端落盘 + 桩实现真身更正 + 部署包收口后口径）**：任务总数 **160 不变**、P0/P1/P2 = **120/33/7 不变**、里程碑 **17 不变**；四态由 **85/6/44/25** 变为 **✅90 / 🔄6 / 🟡43 / 🆕21**（净 +5✅ / −1🟡 / −4🆕）。逐条翻转与依据：',
    '>',
    '> | 任务 | v1.16 | v1.19 | 翻转依据（实测） |',
    '> |------|:-----:|:-----:|------------------|',
    '> | T5.9 隐形成本 | 🟡 | ✅ | `FeeAccrualJob` 一类三 `@XxlJob`（L41/L53/L65）+ 4 Controller 类级 `FEAT_HIDDEN_COST` + 3 ServiceImpl 开关前置校验 |',
    '> | T5.17 报销管控 | 🟡 | ✅ | `CategoryBudgetServiceImpl` L127 三级预警每单发消息 + `TrackControlController` 4 端点 + `ExpenseBillServiceImpl` L153/L316 时间线；原 🟡 理由「T13.3 仍为桩」已作废 |',
    '> | T5.18 规则引擎/发票验真 | ✅ | 🟡 | 验真全栈已交付且开关已标，但 `MappingRule` 全栈 7 类对反哺关键字**零命中**，验收项「确认反哺生效」未达成 |',
    '> | T10.7 版本编码兼容 | 🟡 | ✅ | `VersionIsolationTest` L253-303 五个专项单测 + `LicenseGenerateTool` L166-169 双编码穿透 + `edition/index.vue` 数据驱动零硬编码；`normalizeEdition` 已在 `getFeaturesByEdition`(L204)/`editionLevel`(L221) 唯一出口收口 |',
    '> | T12.4 海报管线 + imgSecCheck | ✅ | 🟡 | 真身 `ContentSecurityBlacklistChecker`：`checkText` 黑名单真实 REJECT，`checkImage` 仅校验 URL 非空即放行；裁剪/容差/体积/遮罩/主色全走 `ImagePipelineStub` |',
    '> | T13.3 消息 Provider | 🟡 | ✅ | 三 Provider 真身各 7 处真实 HTTP、0 TODO；`MessageServiceImpl` 被 16 个类引用；**v1.15 记载的三个 `*MessageStub` 类名系虚构** |',
    '> | T14.2 到期停止 + 续费 | ✅ | 🟡 | 7/3/1 提醒已走 Provider（`BillingRemindJob` L42/L91）、3 天只读宽限期已实现（`TenantWriteBlockInterceptor` 已注册），但**续费入口 PC + 小程序双缺（0 命中）** |',
    '> | T15.12 预约管理 | 🟡 | ✅ | 顾客小程序 `customerHome` + `customerBook/{submit,list,detail}` 落盘，`requestPayment` 0 命中符合 V1 零支付 |',
    '> | T15.15 小程序框架 | 🆕 | 🟡 | 18 页 + 34 条注册零漂移 + 租户号带入齐备；但 `client_type` 在 `ruoyi-app` 仅 1 处**注释**命中，菜单过滤未打通 |',
    '> | T15.18 业务员小程序 | 🆕 | ✅ | `staffHome` + `staffIncome/{index,detail}`，四 perms 双向命中 |',
    '> | T15.19 顾客小程序 | 🆕 | ✅ | `customerHome` + `customerBook/*` + `customerMine`，顾客端零支付代码 |',
    '> | T15.20 老板小程序 | 🆕 | 🟡 | `bossHome`/`report/*`/`mine` 齐备（本轮修复 L105 `weeklyRes` 未定义变量导致的静默失效）；但定价快改 0 页面消费 |',
    '>',
    '> **★本轮同时更正 v1.15 note 的 4 处事实性错误**（不改判定口径，仅改事实）：① 三个 `*MessageStub` 与 `ContentSecurityStub` **四个类名在全仓不存在**，真身为 `Wechat*MessageProvider`/`WecomAppMessageProvider`/`ContentSecurityBlacklistChecker`；② `ImagePipelineStub`/`WecomIntegrationStub` **确为桩**（前者 `rendered=stub` 占位 URL，后者 `getUserInfo` 返回 `stub_*` 模拟成员），v1.15 判定正确；③ 「`ruoyi-app` 无任何小微版页面」系 glob 深度缺陷（`micro/*.vue` 匹配不到 `micro/activity/browse.vue`）导致的误判，实际 18 个；④ 3 天只读宽限期、7/3/1 提醒走 Provider、订阅消息授权次数校验三项 v1.15 记为「未实现」的能力**实际均已实现**（其中订阅次数为「有服务无消费方」：`SubscribeQuotaServiceImpl` 已备但 `WechatTemplateMessageProvider` 零引用）。',
    '> **★13.4 残留缺口清单（v1.15 口径）中涉及上述 12 项的条目以本说明为准**，逐条重写见第十五章。',
  ].join(eol));

fs.writeFileSync(F, t, 'utf8');
log.push('');
log.push('batch3 done, fail=' + fail + ', lines=' + t.split(eol).length);
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t4c_result.txt', log.join('\n'), 'utf8');
console.log(log.join('\n'));
