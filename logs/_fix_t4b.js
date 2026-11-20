/**
 * Task 4 · 第二批：母文档 M12 / M13 / M14
 * 核心：v1.15 note 点名的 4 个 *Stub 类中 3 个是虚构类名，真身为真实实现
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

// ============================================================ M12
rep('M12-note头',
  '> **v1.15 对账结果（✅ 1 / 🔄 0 / 🟡 6 / 🆕 1）**：M12 在 v1.14 统计表中为 🆕 0/8，实测**后端主体与 PC 页面（`views/biz/{activity,announcement,theme}/index.vue`）已落地，但图片处理与内容安全均为桩实现**——',
  '> **v1.19 对账结果（✅ 1 / 🔄 0 / 🟡 6 / 🆕 1，计数不变但 T12.4 由表格 ✅ 纠正为 🟡）**：M12 在 v1.14 统计表中为 🆕 0/8，实测**后端主体与 PC 页面（`views/biz/{activity,announcement,theme}/index.vue`）已落地，图片处理确为桩、内容安全为「文本真实 + 图片放行」的降级实现**——');

rep('M12-ContentSecurity真身',
  '**`ContentSecurityStub` 默认全部放行（PASS）**，未接微信真实 `imgSecCheck`；',
  '**内容安全真身为 `ContentSecurityBlacklistChecker`（89 行，`@Component("contentSecurityChecker")`，实现 `IContentSecurity`）——v1.15 note 记载的 `ContentSecurityStub` 类名在全仓不存在，特此更正**：`checkText` 按 `content.security.blacklist` 关键词黑名单逐词匹配、命中即 `REJECT`（**真实生效**，非放行）；但 `checkImage` 仅校验 URL 非空后即 `ContentCheckResult.pass()`（L64 注释「本地无法做图片内容识别」），**未接微信真实 `imgSecCheck`**。故 **T12.4 由表格误标的 ✅ 纠正为 🟡**（海报 750×400/750×1000 裁剪、±5% 容差、≤200KB、透明底拒收、渐变遮罩与主色提取全部走 `ImagePipelineStub`）；');

rep('M12-T12.4表', '| ✅ T12.4 | **海报处理管线 + `imgSecCheck`**', '| 🟡 T12.4 | **海报处理管线 + `imgSecCheck`**');

// ============================================================ M13
rep('M13-note头',
  '> **v1.15 对账结果（✅ 0 / 🔄 0 / 🟡 5 / 🆕 1）**：M13 在 v1.14 统计表中为 🆕 0/6，实测**后端骨架已落地但企微与消息三通道均为桩实现，前端页面全缺**——',
  '> **v1.19 对账结果（✅ 1 / 🔄 0 / 🟡 4 / 🆕 1）**：M13 在 v1.14 统计表中为 🆕 0/6，实测**消息三通道为真实 HTTP 实现（v1.15「全为桩」系误判，且三个 `*MessageStub` 类名在全仓不存在）、企微确为桩、通道管理前端页面仍缺**——');

rep('M13-降为🟡5项',
  '> - **降为 🟡（5 项）**：T13.1 `SysTenant.mpAppId` 字段已建、`PortalController` 已提供 AppID 反查（`/portal/tenant/resolve`）与门店列表（`/portal/stores`）+ 限流，但**公众号自定义菜单配置生成器未实现**、openid → `user_tenant_bind` 的网页授权回写未打通；T13.2 `WecomController` + `WecomIntegrationStub` 已提供 OAuth 免登与通讯录同步接口骨架且已标 `FEAT_WECOM`，但**为桩实现**（未接企微真实 API）；T13.3 `IMessageProvider` + `MessageProviderDispatcher` + 三实现（`WechatTemplateMessageStub`/`WechatKfMessageStub`/`WecomAppMessageStub`）**全为桩**，T5.12/T3.4/T5.17 的通知**未统一改走 Provider**；T13.5 `OpenApiGatewayController` + `OpenApiSignUtils` + 配额审计已落地（与 T7.8 ✅ 同源），但**无通道管理页面**（`ruoyi-ui` 无 `views/billing`/`views/compliance` 同类目录）、`FEAT_FLOW_CUSTOM` 自定义审批流开放 API 触发未实现；T13.6 `TenantPaymentConfigServiceImpl` + `ProfitSharingController`/`SettlementController`/`WithdrawalController` 已落地分账与提现审核骨架，但**结算单与对账报表页面缺失**。',
  [
    '> - **升为 ✅（1 项，v1.19 实测翻转）**：**T13.3 消息 Provider 抽象**——`IMessageProvider`(31 行) + `MessageProviderDispatcher`(90 行) + `MessageContext`(45 行) + `MessageResult`(59 行) 齐备，三实现真身为 **`WechatTemplateMessageProvider`(131 行) / `WechatKfMessageProvider`(118 行) / `WecomAppMessageProvider`(122 行)**，**各含 7 处真实 HTTP 调用、0 处 TODO/未实现/模拟**，各带 2~3 处 `@Value` 配置读取；`MessageServiceImpl`(103 行) 5 处调 Dispatcher、2 处按 `access_channel` 分发、14 处 `MessageLog` 留痕、2 处降级不阻塞主流程。**T5.12/T3.4/T5.17 的通知已统一改走 Provider**——引用消息服务接口的类共 **16 个**，含 `BillingRemindJob`（T3.4/T14.2 到期提醒）、`CategoryBudgetServiceImpl`（T5.17 三级预警）、`ExpenseApprovalServiceImpl`、`BudgetPreparationServiceImpl`、`BudgetAdjustmentAppServiceImpl`、`FeeNotificationServiceImpl`、`FeeIncomeConfirmationServiceImpl`、`FeeProjectServiceImpl`、`OverBudgetApprovalServiceImpl`、`ReservedBudgetServiceImpl`、`MicroNotifyController` 等。**★v1.15 note 记载的 `WechatTemplateMessageStub` / `WechatKfMessageStub` / `WecomAppMessageStub` 三个类名在全仓不存在（虚构），特此更正；由此连带作废 M5/M6/M14 中「T13.3 仍为桩」的全部推论。**',
    '> - **维持 🟡（4 项）**：T13.1 `SysTenant.mpAppId` 字段已建、`PortalController` 已提供 AppID 反查（`/portal/tenant/resolve`）与门店列表（`/portal/stores`）+ 限流，但**公众号自定义菜单配置生成器未实现**、openid → `user_tenant_bind` 的网页授权回写未打通；**T13.2 企微确为桩**——`WecomIntegrationStub`(57 行) 的 `buildAuthUrl` 按企微标准拼接授权链接（`open.weixin.qq.com/connect/oauth2/authorize` + `snsapi_base` + `agentid` + `#wechat_redirect`，**真实可用**），但 `getUserInfo` 以 code 派生 `stub_*` 模拟成员、`setName("企微成员(桩)")`，**不请求企微 API**，可信域名校验与通讯录读取未实现（`WecomSyncServiceImpl` 骨架已在、`M13_02` 菜单 `3490~3492` v1.19 已补入索引）；T13.5 `OpenApiGatewayController` + `OpenApiSignUtils` + 配额审计已落地（与 T7.8 ✅ 同源），但**无通道管理页面**（`ruoyi-ui` 无 `views/billing`/`views/compliance` 同类目录，实测 billing 相关 `.vue`/`.js` **0 命中**）、`FEAT_FLOW_CUSTOM` 自定义审批流开放 API 触发未实现；T13.6 `TenantPaymentConfigServiceImpl` + `ProfitSharingController`/`SettlementController`/`WithdrawalController` 已落地分账与提现审核骨架，但**结算单与对账报表页面缺失**。',
  ].join(eol));

// ============================================================ M14
rep('M14-note头',
  '> **v1.15 对账结果（✅ 0 / 🔄 0 / 🟡 4 / 🆕 1）**：M14 在 v1.14 统计表中为 🆕 0/5，实测**计费后端与合规骨架已落地，前端页面与排期产出全缺**——',
  '> **v1.19 对账结果（✅ 0 / 🔄 0 / 🟡 4 / 🆕 1，计数不变但 T14.2 由表格 ✅ 纠正为 🟡）**：M14 在 v1.14 统计表中为 🆕 0/5，实测**计费后端与合规骨架已落地且比 v1.15 记载更完整（7/3/1 提醒已走 Provider、3 天只读宽限期已实现、订阅消息授权次数已实现），但前端页面与排期产出全缺**——');

rep('M14-T14.2/T14.4理由',
  'T14.2 到期停用复用 T3.4 已通，但 **7/3/1 天提醒未走 Provider**（T13.3 为桩）、**3 天只读宽限期未实现**、小程序 + PC 续费入口缺失；',
  '**T14.2（v1.19 由表格误标的 ✅ 纠正为 🟡）**——到期停用复用 T3.4 已通；**7/3/1 天提醒已走 Provider**（`BillingRemindJob` 102 行，L42 `private static final int[] REMIND_DAYS = {7, 3, 1}`、L52 遍历档位、L82 `bizType="expire_remind"`、L91 `messageService.send(context)`，v1.15「未走 Provider」为误判）；**3 天只读宽限期已实现**（`TenantWriteBlockInterceptor` 54 行，7 处 GRACE 宽限常量 + 3 处 POST/PUT/DELETE 写方法拦截，且已在 `ResourcesConfig` 注册生效，v1.15「未实现」为误判）；后端续费链路齐备（`BillingOrderServiceImpl` 218 行，L105「续费叠加：现有到期未过则从其顺延」、L184 开通/续费回写 `license_type`/`edition`/`expire_date`/`status=NORMAL`，`BillingOrderController` L20 明确「严禁自动扣款，付费套餐须显式 `markPaid` 方激活」）。**唯一残留缺口 = 续费入口前端双缺**：`ruoyi-ui` 全量 `.vue`/`.js` 对「续费/renew」**0 命中**、`views` 下 billing 相关**0 命中**，`ruoyi-app` 对「续费/renew」**0 命中**，故交付物「续费流程」与验收项「续费后功能即时恢复」缺可操作入口（与 T14.1「套餐管理页面前端缺失」同源）；');

rep('M14-T14.4理由',
  '但 `msgSecCheck`/`imgSecCheck` **由 `ContentSecurityStub` 默认全放行**、一次性订阅消息剩余授权次数校验未实现。',
  '**残留缺口按实测重写为 2 项**：① 内容安全**文本侧真实、图片侧放行**——真身 `ContentSecurityBlacklistChecker`(89 行) 的 `checkText` 按黑名单命中即 REJECT，但 `checkImage` 仅校验 URL 非空后 `pass()`，未接微信真实 `imgSecCheck`（v1.15「由 `ContentSecurityStub` 默认全放行」中的类名不存在，且文本侧并非放行）；② **一次性订阅消息剩余授权次数校验「有服务无消费方」**——`SubscribeQuotaServiceImpl`(113 行) 已实现 `grant`/`checkAndConsume`/`remaining`/`queryPageList`，L66 注释「原子条件消费：仅当 `remaining_count>0` 时 -1，否则 0 行 → 授权不足，不发送」+ L72 `setSql("remaining_count = remaining_count - 1")` 杜绝并发超发（v1.15「未实现」为误判），**但 `WechatTemplateMessageProvider` 对 `SubscribeQuota` 零引用**，发送链路未收口该校验，授权不足时仍会尝试发送。');

rep('M14-T14.2表', '| ✅ T14.2 | **试用到期停止 + 续费**', '| 🟡 T14.2 | **试用到期停止 + 续费**');

fs.writeFileSync(F, t, 'utf8');
log.push('');
log.push('batch2 done, fail=' + fail + ', lines=' + t.split(eol).length);
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t4b_result.txt', log.join('\n'), 'utf8');
console.log(log.join('\n'));
