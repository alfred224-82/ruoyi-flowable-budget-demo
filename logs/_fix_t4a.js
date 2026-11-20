/**
 * Task 4 · 第一批：母文档 M5 / M6 / M8 / M10 状态一致性修复
 * 全部改写均以 logs/_facts_*.txt 实测证据为依据
 */
const fs = require('fs');
const F = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
let t = fs.readFileSync(F, 'utf8');
const eol = t.includes('\r\n') ? '\r\n' : '\n';
const log = [];
let fail = 0;

function rep(tag, find, to) {
  const n = t.split(find).length - 1;
  if (n !== 1) { log.push('  !! [' + tag + '] 命中 ' + n + ' 次（应为 1），跳过'); fail++; return; }
  t = t.replace(find, to);
  log.push('  ok [' + tag + ']');
}

// ============================================================ M5
rep('M5-note头',
  '> **v1.16 对账结果（✅ 15 / 🔄 0 / 🟡 4 / 🆕 0）**：下移改造已**分两类完成**——',
  '> **v1.19 对账结果（✅ 17 / 🔄 0 / 🟡 2 / 🆕 0）**：下移改造已**分两类完成**——');

rep('M5-note尾',
  '> - **降为 🟡（2 项）**：T5.17 报销管控——`ExpenseBillController` 已标 `FEAT_EXPENSE_CONTROL` 类级注解，但通知未统一走 Provider（T13.3 仍为桩）；T5.18 规则引擎/发票验真——内嵌报销流程受 `FEAT_EXPENSE_CONTROL` 保护，但反哺规则库未实现（匹配链路为内置规则+兜底确认）。T5.9 隐形成本升为 ✅（`FeeProjectSharingController` 已补 `FEAT_HIDDEN_COST`）。T5.19 基础版边界维持 🟡。',
  [
    '> - **升为 ✅（2 项，v1.19 实测翻转）**：',
    '>   - **T5.9 隐形成本**——四项计提全栈齐备（`FeeProjectSharingServiceImpl` 208 行 + `FeeAssetServiceImpl` / `FeeLoanServiceImpl` / `FeeStaffSalaryServiceImpl`）；`FeeAccrualJob`（85 行）**一个类承载三个 `@XxlJob` Handler**：`salaryAccrualJob`(L41) / `assetDepreciationJob`(L53) / `loanInterestJob`(L65)；四个 Controller（`FeeAssetController` L35 / `FeeLoanController` L31 / `FeeProjectSharingController` L31 / `FeeStaffSalaryController` L30）**类级标注 `@RequireFeature("FEAT_HIDDEN_COST")`**；三个 ServiceImpl 均有「T6.8 ③：仅对开通隐形成本的租户计提，基础版租户跳过（无执行记录）」的开关前置校验（`FeeAssetServiceImpl` L157 / `FeeLoanServiceImpl` L112 / `FeeStaffSalaryServiceImpl` L117）。验收项「每月 1 号计提任务准确」三任务齐备。',
    '>   - **T5.17 报销管控**——大类额度硬拦截 + **85%🟡/95%🟠/100%🔴 三级预警**已由 `CategoryBudgetServiceImpl` 落地（L32 类注释「大类额度硬拦截 + 85%🟡/95%🟠/100%🔴 三级预警（设计文档 2.2 #4）」+ L127「三级预警：每单触发一次消息，通知老板+财务」），且**已统一走 Provider**（`CategoryBudgetServiceImpl` 在引用 `IMessageService` 的 16 个类清单内）；追溯时间线由 `ExpenseBillServiceImpl` L153（T6.4 命中规则快照，开关关闭时同样保留便于审计）+ L316（T6.2 打卡记录入追溯时间线）+ `TrackControlController`（121 行，类级 `@RequireFeature("FEAT_EXPENSE_CONTROL")`，`/config/list`、`/check`、`/type`、`/config/batchSet` 四端点）承载。**★v1.15 note 的 🟡 理由「通知未统一走 Provider（T13.3 仍为桩）」为误判**——T13.3 三实现为真实 HTTP 发送（见 M13 v1.19 对账）。',
    '> - **维持 🟡（2 项）**：',
    '>   - **T5.18 规则引擎/发票验真**——发票验真全栈**已交付且开关已标**（`InvoiceVerifyController` 87 行，L34 类级 `@RequireFeature("FEAT_INVOICE_VERIFY")` + `IInvoiceVerifyService` 46 行 + `InvoiceVerifyServiceImpl` 81 行；`ExpenseBillServiceImpl` L218 `if (featureChecker.isFeatureEnabled("FEAT_INVOICE_VERIFY"))` 条件验真），**但验收项「财务确认/纠偏结果反哺规则库（越用越准）」未实现**——`MappingRule` 全栈 7 个类（含 `MappingRuleServiceImpl` 346 行、`MappingRuleController` 138 行）对 `反哺`/`feedback`/`confirmRule`/`hitCount`/`hit_count` 关键字**零命中**，匹配链路仍为内置规则 + 人工兜底确认，无「越用越准」的命中计数与规则回写。',
    '>   - **T5.19 基础版边界**——①~④（短路跳过 / 项目级自定义科目 / 极简确认单级审批 / 轻量收支概览）已落地，**⑤《基础版能力边界说明书》未产出**（文档类缺口）。',
  ].join(eol));

rep('M5-T5.9', '| 🟡 T5.9 | 隐形成本', '| ✅ T5.9 | 隐形成本');
rep('M5-T5.17', '| 🟡 T5.17 | 报销管控与预警', '| ✅ T5.17 | 报销管控与预警');
rep('M5-T5.18', '| ✅ T5.18 | 规则引擎匹配链与发票验真', '| 🟡 T5.18 | 规则引擎匹配链与发票验真');

// ============================================================ M6
rep('M6-note残留缺口',
  '主体工作完成。残留缺口：① 通知未统一走 Provider（T13.3 仍为桩）；② 规则引擎反哺规则库未实现（匹配链路为内置规则+兜底确认）。',
  '主体工作完成。**残留缺口（v1.19 由 2 项收窄为 1 项）**：规则引擎反哺规则库未实现（`MappingRule` 全栈零反哺信号，匹配链路为内置规则 + 人工兜底确认，归 T5.18 🟡）。~~① 通知未统一走 Provider（T13.3 仍为桩）~~ **本条 v1.19 作废**——`WechatTemplateMessageProvider`(131 行) / `WechatKfMessageProvider`(118 行) / `WecomAppMessageProvider`(122 行) 各含 7 处真实 HTTP 调用、0 处 TODO，`MessageServiceImpl`(103 行) 已被 16 个类引用（含 `BudgetPreparationServiceImpl` / `ExpenseApprovalServiceImpl` / `CategoryBudgetServiceImpl` / `BillingRemindJob`），T13.3 已翻 ✅。');

// ============================================================ M8
rep('M8-note头',
  '> **v1.15 对账结果（✅ 4 / 🔄 2 / 🟡 3 / 🆕 2）**：`ruoyi-app/pages` 共 16 个 `.vue`（`login`/`store/select`/`tab-home`/`tab-work`/`tab-approve`/`tab-mine`/`project/*` 4 页/`expense/*` 3 页/`approval/*` 2 页/`dashboard/category`），`utils` 含 `features.js`/`permission.js`/`roles.js`/`store.js`。',
  '> **v1.19 对账结果（✅ 4 / 🔄 2 / 🟡 3 / 🆕 2，本轮零翻转）**：`ruoyi-app/pages` **企业版 16 个 `.vue`**（`login`/`store/select`/`tab-home`/`tab-work`/`tab-approve`/`tab-mine`/`project/*` 4 页/`expense/*` 3 页/`approval/*` 2 页/`dashboard/category`）+ **小微版 `pages/micro/*` 18 个（v1.19 实测落盘，任务归 M15）**，合计 **34 个**，与 `pages.json` 注册的 **34 条零漂移**；`utils` 含 `features.js`/`permission.js`/`roles.js`/`store.js`。**M8 零翻转的依据**：18 个 micro 页面为小微版三视图（`bossHome`/`staffHome`/`customerHome` 及其子页），**不含任何高级版/专业版增量页面**（提交打卡流程 / 信用分卡片 / 审批证据等级标记 / 登录选择组织 / 大额人脸核身），故 T8.5 维持 🆕、T8.1/T8.4 维持 🔄、T8.11 维持 🆕。');

// ============================================================ M10
rep('M10-note头',
  '> **v1.15 对账结果（✅ 2 / 🔄 0 / 🟡 4 / 🆕 2）**：M10 在 v1.14 统计表中为 🆕 0/8，实测**后端与 SQL 已大面积落地**——',
  '> **v1.19 对账结果（✅ 3 / 🔄 0 / 🟡 3 / 🆕 2）**：M10 在 v1.14 统计表中为 🆕 0/8，实测**后端、SQL 与 PC 管理页均已落地**——');

rep('M10-升为✅计数', '> - **升为 ✅（2 项）**：T10.1 租户三字段全栈', '> - **升为 ✅（3 项）**：T10.1 租户三字段全栈');

rep('M10-T10.7追加',
  '`/system/edition/features` 返回 `{edition,bizCode,accessChannel,features,copy}`）。',
  [
    '`/system/edition/features` 返回 `{edition,bizCode,accessChannel,features,copy}`）；',
    '>   **T10.7 版本编码兼容（v1.19 由 🟡 翻 ✅）**——v1.15 note 的三条 🟡 理由**全部 stale**：① 「单测未覆盖全部别名」不成立，`VersionIsolationTest` L253-303 有 **5 个 T10.7 专项单测**（`pro`/`PROFESSIONAL`/`ENTERPRISE` 返回同一功能集、`adv` 别名、`base`/`null`/未知值降级、大小写不敏感、`editionLevel` 排序），`LicenseServiceTest` 亦覆盖；② 「`LicenseGenerateTool` 未同步接受两套编码」不成立，L166-169 以 `switch(toUpperCase)` + `case ENTERPRISE` 穿透同时接受两套编码；③ 「前端下拉未同步」不成立，`views/system/edition/index.vue`（327 行）为**数据驱动只读档位台**，对 `ENTERPRISE`/`PROFESSIONAL`/`professional`/`normalize` 字面量**零命中**（档位来自后端接口）。另实测 `normalizeEdition` 虽仅 4 个类直接调用，但已在**唯一出口收口**——`EditionFeatureConfig#getFeaturesByEdition()`(L204) 与 `#editionLevel()`(L221) 内部即 `switch (normalizeEdition(edition))`，调用方无需各自归一化，不构成缺口。',
  ].join(eol));

rep('M10-降为🟡',
  '> - **降为 🟡（4 项）**：T10.2 `BizBundleServiceImpl` + `M1_05` 三业态包已落地，但**无管理页面**（`ruoyi-ui` 无 `views/system/bizBundle`）、缓存变更失效未验证；T10.3 `IFeatureFlagService`/`FeatureFlagController` + 33 项初始数据已落表，但**无开关管理页面**，`EditionFeatureConfig` 仍为主用常量集（表与常量不一致告警未实现）；T10.4 `tenant_biz_config` 建表 + `module_overrides` 读取已落地，但**无覆盖配置页面、无 JSON Schema 校验**；T10.7 `normalizeEdition` 唯一入口 + `@Deprecated ENTERPRISE_FEATURES`/`EDITION_ENTERPRISE` 已落地，但**单测未覆盖全部别名**、`LicenseGenerateTool` 与前端下拉未同步接受两套编码（归口 T3.15）。',
  [
    '> - **维持 🟡（3 项，v1.19 理由全部按实测重写）**：',
    '>   - **T10.2 `biz_bundle` 业态包引擎**——v1.15 理由「无管理页面」**已作废**：`views/system/bizBundle/index.vue` 已落盘，`BizBundleController`（77 行）4 个端点 + `system:bizBundle:*` 4 处 perms + 刷新缓存端点，菜单 `1320~1322` 已由 `M10_01` 登记并授予超管（v1.19 补入四个部署包）。`BizBundleServiceImpl`（154 行）以 `ConcurrentHashMap` 做本地缓存（7 处）+ `refresh`/`clear`/`evict` 5 处 + `getBundle`/`getFeatureFlags`/`copy` 文案读取齐备。**残留缺口**：① `schema_blocks` 与 `engine_strategy` 两个字段**零读取**（业态区块声明与引擎策略未生效，联动 T10.6 🆕 / T11.4）；② 缓存为**进程内 `ConcurrentHashMap`**，无 `@Cacheable`/`@CacheEvict`（0 命中）、无 Caffeine，集群多实例下变更失效不同步，仅靠手动刷新端点。',
    '>   - **T10.3 `feature_flag` 表驱动开关**——v1.15 理由「无开关管理页面」「不一致告警未实现」**均已作废**：`views/system/featureFlag/index.vue` 已落盘，`FeatureFlagController`（72 行）4 个端点 + `system:featureFlag:*` 4 处 perms，菜单 `1330~1332` 已由 `M10_01` 登记；`FeatureFlagServiceImpl`（144 行）引用 `EditionFeatureConfig` 8 处做兜底并有 1 处不一致 `log.warn` 告警。**残留缺口**：① **未按 `edition` 从表读默认开启集**（0 命中），默认集仍取自 `EditionFeatureConfig` 常量（该常量被 11 个类引用，仍为主用来源）；② 无 `@Cacheable`/`@CacheEvict`，开关启停依赖手动刷新缓存而非即时生效。',
    '>   - **T10.4 `tenant_biz_config` 单店覆盖**——后端齐备（`TenantBizConfigController` / `TenantBizConfigServiceImpl` / `TenantBizConfig` / `TenantBizConfigVo` / `TenantBizConfigMapper` / `ITenantBizConfigService` 六件套，`module_overrides` 被 11 个类引用，含 `MicroShopConfigServiceImpl` 的「租户覆盖 > 业态包默认」合并）。**残留缺口**：① **无 PC 覆盖配置页面**（`views/system/tenantBizConfig` 与 `views/system/bizConfig` 均 0 命中）；② **`module_overrides` 无 JSON Schema 校验**——全仓 6 处 Schema 校验命中（`BudgetValidationRule` / `BudgetValidationRuleServiceImpl` / `ValidationResultVo` 等）**全部属预算校验规则域，与本任务无关**，非法 JSON 不会被拦截。',
  ].join(eol));

rep('M10-T10.7表', '| 🟡 T10.7 | **版本编码兼容**', '| ✅ T10.7 | **版本编码兼容**');

fs.writeFileSync(F, t, 'utf8');
log.push('');
log.push('母文档第一批写入完成，失败 ' + fail + ' 处，总行数 ' + t.split(eol).length);
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t4a_result.txt', log.join('\n'), 'utf8');
console.log(log.join('\n'));
