const fs = require('fs');
const F = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
let s = fs.readFileSync(F, 'utf8');
const before = s.length;
const log = [];
let fail = 0;

function rep(tag, find, to) {
  const n = s.split(find).length - 1;
  if (n !== 1) { log.push('  [FAIL ' + n + '] ' + tag); fail++; return; }
  s = s.replace(find, to);
  log.push('  [ok] ' + tag);
}

// ---------- 1. 文档头版本号 ----------
rep('1 头部版本 v1.16→v1.20',
  '\n**版本**：v1.16',
  '\n**版本**：v1.20');

// ---------- 2. M1 note 头 ----------
rep('2 M1 note 头 ✅8/🔄1 → ✅9/🔄0',
  '> **v1.15 对账结果（✅ 8 / 🔄 1 / 🟡 0 / 🆕 0）**：T1.3 下移重排已由',
  '> **v1.20 对账结果（✅ 9 / 🔄 0 / 🟡 0 / 🆕 0）**：T1.3 下移重排已由');

// ---------- 3. M1 note 尾：T1.5 🔄 → ✅ ----------
rep('3 M1 note T1.5 翻 ✅',
  '**仅 T1.5 保留 🔄**：`FEAT_SUBJECT_3LAYER` 接口层开关未标注（归口 T6.8）。',
  '**T1.5 由 v1.15 的 🔄 纠正为 ✅（v1.20 实测，与《系统管理》分册对齐）**：v1.15「`FEAT_SUBJECT_3LAYER` 接口层开关未标注（归口 T6.8）」**已过期**——实测 `FeeExpenseCategoryController` **L31 类级 `@RequireFeature("FEAT_SUBJECT_3LAYER")` 已存在**，同开关另有 4 处类级标注（`FeeFinanceSubjectController` L38 / `FeeTenantCategoryController` L28 / `MappingRuleController` L36 / `BudgetSubjectController` L38），基础版租户调用费用大类全部接口返回「当前版本不支持此功能」，防串第②层无缺口；建表侧由 `M5_02_create_tables_category.sql` + `M5_06_tenant_category_and_system_level.sql` 交付（含约 6 个大类内置数据与映射B，`fee_expense_category` 系统级共享、`fee_tenant_category` 绑定 `tenant_id`）。SQL / BE 两赛道齐备，按规范 17 ② 标 ✅。');

// ---------- 4. M1 表格标记 ----------
rep('4 M1 表格 T1.5 🔄→✅',
  '| 🔄 T1.5 | 建表：费用大类主数据（',
  '| ✅ T1.5 | 建表：费用大类主数据（');

// ---------- 5. M3.5 note 头 ----------
rep('5 M3.5 note 头补「理由改写」',
  '> **v1.15 对账结果（✅ 6 / 🔄 0 / 🟡 2 / 🆕 0）**：**T3.12 升为 ✅**',
  '> **v1.20 对账结果（✅ 6 / 🔄 0 / 🟡 2 / 🆕 0，计数不变但 T3.11/T3.15 的 🟡 理由按实测改写）**：**T3.12 升为 ✅**');

// ---------- 6. M3.5 note 尾：T3.11 + T3.15 理由改写 ----------
rep('6 M3.5 note T3.11/T3.15 理由改写',
  '**T3.11 降为 🟡**：导入接口与 `expire_date`/`edition` 同步已落地，但 License JSON 的 `bizCode` 字段与租户业态包匹配校验未实现。**T3.15 降为 🟡**：`LicenseGenerateTool` 已有租户ID 入参与 `ENTERPRISE` 兼容，但无 `--biz-code`、无套餐类型入参，`getDefaultFeatures` 为简化版未真正引用 `EditionFeatureConfig`。',
  '**T3.11 维持 🟡（v1.20 改写理由，计数不变）**：v1.15 的 🟡 理由「License JSON 的 `bizCode` 字段与租户业态包匹配校验未实现」**事实有误**——实测 `LicenseServiceImpl#importLicenseForTenant` **L784-791 已有完整校验**（读 License 的 `bizCode`，非空时与租户业态包（空则回落 `BizBundleServiceImpl.DEFAULT_BIZ_CODE`）做 `equalsIgnoreCase` 比对，不匹配抛 `ServiceException("License业态包[..]与租户业态包[..]不匹配，请使用对应业态包的License")`）；核心链路亦齐备——L734 hard 模式 RSA-SHA256 验签、L771 旧证置 `REPLACED`、L834-839 回写租户 `expire_date`/`edition`/`license_type`、L843 `evictTenantCache`、L845-849 写 `sys_tenant_version_log`（`LICENSE_IMPORT`/`LICENSE_REPLACE`）。**真实 🟡 缺口为版本编码未归一化（违反开发规范第 4 条）**：L803 `newLicense.setEdition(...)` 与 L837 `tenantUpdate.setEdition(...)` 均将 License JSON 的 `edition` **原值直写**，未走 `EditionFeatureConfig.normalizeEdition()` 入口——v1.12 T3.11 要求「接受 `pro` 短码**并归一化为** `PROFESSIONAL`」，实测仅达成「接受」未达成「归一化」（导入 `pro` License 后 `sys_license.edition` 与 `sys_tenant.edition` 落库为 `pro`）；功能计算侧不受影响（L816 `getFeaturesByEdition()` 内部已收口归一化），但连带 L878 `getTenantSelector` 仅硬编码 `if ("ENTERPRISE".equalsIgnoreCase(edition)) edition = "PROFESSIONAL"`、**不兼容 `pro`**，该类租户会在 T3.17 租户选择器中落入未渲染分组而丢失（关联防串用例 12）。**T3.15 维持 🟡（分册标 ✅ 过早）**：`LicenseGenerateTool` 已有租户ID 入参（L94 交互式 prompt）与 `ENTERPRISE` 兼容（L169），但 ① 无 `--biz-code` 命令行参数（L95 仅交互式 prompt）、② 无套餐类型（trial/first_month/monthly/yearly）入参、③ L167-169 switch 仅 `PROFESSIONAL`/`ENTERPRISE`、**无 `case "pro"`**、④ L113/L165 注释自承「简化，实际应引用 `EditionFeatureConfig`」，`getDefaultFeatures` 为硬编码简化版而非真正引用配置类。');

// ---------- 7. M4 note 头 ----------
rep('7 M4 note 头 ✅3/🟡1 → ✅4/🟡0',
  '> **v1.15 对账结果（✅ 3 / 🔄 1 / 🟡 1 / 🆕 0）**：T4.1 已由',
  '> **v1.20 对账结果（✅ 4 / 🔄 1 / 🟡 0 / 🆕 0）**：T4.1 已由');

// ---------- 8. M4 note 尾：T4.3 🟡 → ✅ ----------
rep('8 M4 note T4.3 翻 ✅',
  '**T4.3 降为 🟡**：`/system/edition/features` 已返回三维交集（含 `bizCode`/`accessChannel`/`copy`），但 `ruoyi-ui` 全量 **无 i18n**（无 `vue-i18n`、无 `$t(`），业态文案映射未落地（归口 T10.5）。',
  '**T4.3 由 v1.15 的 🟡 纠正为 ✅（v1.20 实测，与《系统管理》分册对齐）**：v1.15 的 🟡 理由为「`ruoyi-ui` 全量无 i18n（无 `vue-i18n`、无 `$t(`），业态文案映射未落地（归口 T10.5）」——该缺口**已明确归口 T10.5，不应在 T4.3 重复扣分**（规范 17 ②「一项缺口只在其归口任务上登记」），T10.5 维持 🆕 不受影响。T4.3 自身验收要点实测达成：① `/system/edition/features` 返回三维交集（含 `bizCode`/`accessChannel`/`copy`）；② 前端按开关动态隐藏——`ruoyi-ui/src/utils/feature.js` L113 `featureVisible()`（L101 `isAdmin()` 超管豁免）+ L129 `getRequiredFeature()`（`ROUTE_FEATURE_MAP` 路由→开关映射）+ **L144 `filterRoutesByFeature()`**（L160 递归过滤子路由，L162-164 剔除子节点全被过滤掉的空 `Layout` 父节点），已由 `store/modules/permission.js` L7 引入、L39 `const filteredData = filterRoutesByFeature(res.data)` 在**路由生成期**接入，未解锁页面根本不进入 vue-router；③ 小程序侧 `ruoyi-app/utils/features.js` L3 同源接入，`api/system/edition.js` L12 提供端点封装。**★登记实现偏差（不改判定）**：任务描述写「直接输入路由访问时组件内二次校验并跳转 403」，实测采用「路由未注册即不可达」——被过滤的路由在 vue-router 中不存在，浏览器直敲命中 **404 兜底页**而非 403。实际拦截效果**等价且更早更彻底**（不加载任何组件代码 + `FeatureInterceptor` 接口层二次拦截），但**第五章防串用例 4 的预期须按「404 或 403 均视为拦截成功」口径执行**，用例文案在下沉时同步。');

// ---------- 9. M4 表格标记 ----------
rep('9 M4 表格 T4.3 🟡→✅',
  '| 🟡 T4.3 | 前端能力下发与渲染：',
  '| ✅ T4.3 | 前端能力下发与渲染：');

// ---------- 10~12. 第三章统计表 ----------
rep('10 统计表 M1 行',
  '| M1 数据库与菜单（含平台底座建表） | 9 | 8 | 1 | 0 | 0 | 8 | 1 | 0 |',
  '| M1 数据库与菜单（含平台底座建表） | 9 | 9 | 0 | 0 | 0 | 8 | 1 | 0 |');
rep('11 统计表 M4 行',
  '| M4 开关与防串（扩展至 33 项） | 5 | 3 | 1 | 1 | 0 | 4 | 1 | 0 |',
  '| M4 开关与防串（扩展至 33 项） | 5 | 4 | 1 | 0 | 0 | 4 | 1 | 0 |');
rep('12 统计表合计行',
  '| **合计** | **160** | **90** | **6** | **43** | **21** | **120** | **33** | **7** |',
  '| **合计** | **160** | **92** | **5** | **42** | **21** | **120** | **33** | **7** |');

// ---------- 13. 插入 v1.20 状态分布说明 ----------
const v120 = [
  '',
  '> **v1.20 状态分布说明（分册冲突实测裁定 + note 与表格对齐）**：任务总数 **160 不变**、P0/P1/P2 = **120/33/7 不变**、里程碑 **17 不变**；四态由 **90/6/43/21** 变为 **✅92 / 🔄5 / 🟡42 / 🆕21**（净 +2✅ / −1🔄 / −1🟡）。本轮起因是《系统管理》分册（v1.16）与母文档在 **4 项任务**上判定相反，逐项以**代码实测**裁定（分册 note 与历史记忆均不作为依据）：',
  '>',
  '> | 任务 | v1.19 | v1.20 | 裁定依据（实测） |',
  '> |------|:-----:|:-----:|------------------|',
  '> | T1.5 费用大类主数据 | 🔄 | **✅** | `FeeExpenseCategoryController` **L31 类级 `@RequireFeature("FEAT_SUBJECT_3LAYER")` 已存在**，同开关另有 4 处类级标注（`FeeFinanceSubjectController` L38 / `FeeTenantCategoryController` L28 / `MappingRuleController` L36 / `BudgetSubjectController` L38）；建表侧由 `M5_02` + `M5_06` 交付。v1.15「接口层开关未标注」已过期 → **分册正确、母文档 stale** |',
  '> | T4.3 前端能力下发与渲染 | 🟡 | **✅** | `feature.js` L144 `filterRoutesByFeature` + `permission.js` L39 路由生成期接入，L113 `featureVisible` / L129 `getRequiredFeature` / L101 `isAdmin` 齐备 → **分册正确、母文档 stale**；原 🟡 理由（i18n）归口 T10.5，不重复扣分 |',
  '> | T2.3 数据范围过滤 | 🔄 | **✅** | `SysDataScopeServiceImpl` L69/L74/L80 双表并查 + L101/L107 新枚举过滤，已按五枚举重写 → **表格 v1.15 起即为 ✅，本轮仅纠正 M2 note，不影响计数** |',
  '> | T3.11 导入 License | 🟡 | 🟡（**理由改写**） | **分册标 ✅ 过早**；母文档 v1.15 的 🟡 理由（bizCode 校验未实现）**事实错误**（`LicenseServiceImpl` L784-791 已实现）。真实缺口＝L803/L837 `edition` 原值直写未走 `normalizeEdition`（违反规范 4）+ L878 `getTenantSelector` 不兼容 `pro`。计数不变 |',
  '> | T3.15 签发工具增强 | 🟡 | 🟡（**维持**） | **分册标 ✅ 过早**：L113/L165 注释自承「简化，实际应引用 `EditionFeatureConfig`」；L167-169 无 `case "pro"`；无套餐类型入参；`bizCode` 仅 L95 交互式 prompt 而非 `--biz-code`。计数不变 |',
  '> | T10.2 / T10.3 / T10.4 | 🟡 | 🟡（**维持**） | **分册标 ✅（M10 ✅6）过早**：T10.2 任务描述要求加载 `menus`/`copy`/`schema_blocks`/`engine_strategy`/`feature_flags` 五项，后两项零读取；T10.3 验收要求「表与常量不一致时**以表为准**并告警」，实测以常量为准（方向相反）；T10.4 交付物要求「覆盖配置页面」与「JSON Schema 校验」，两者全仓零命中。**母文档 M10 = ✅3 / 🟡3 / 🆕2 为准** |',
  '>',
  '> **★本轮确立的裁定原则（作为规范 17 ⑦ 的执行口径）**：分册 note、历史 note、外部记忆三者与**代码实测**冲突时，**一律以实测为准**；「分册比母文档新」不构成母文档跟随升级的理由，必须先取证再改。本轮 4 项冲突中 **2 项分册正确（T1.5 / T4.3）、2 项分册过早（T3.11 / T3.15）**，恰好各半，印证该原则必要性。另**「grep 零命中」不等于「功能未实现」**——T2.3 误判即源于以「枚举类零引用」为据，实际实现用了字符串字面量。',
  '>',
  '> **★连带修正**：① M2 note 头由 `v1.15（✅3/🔄1/🟡1）` 改为 `v1.20（✅4/🔄0/🟡1）` 与表格对齐；② 第三章 v1.15 / v1.16 两段历史「状态分布说明」已加「⚠️ 历史口径」标注（规范 17 ⑥）；③ T4.3 翻 ✅ 附带登记「路由未注册 → 命中 404 兜底而非组件内 403」的实现偏差，**第五章防串用例 4 的预期须按「404 或 403 均视为拦截成功」口径执行**；④ 10.1 变更总览与 10.2 历史快照追加 v1.20 补注，表内标记原样保留；⑤ 文档头与文末版本号补齐为 v1.20（v1.19 未曾回填版本号）。'
].join('\n');
rep('13 插入 v1.20 状态分布说明',
  '> **★13.4 残留缺口清单（v1.15 口径）中涉及上述 12 项的条目以本说明为准**，逐条重写见第十五章。',
  '> **★13.4 残留缺口清单（v1.15 口径）中涉及上述 12 项的条目以本说明为准**，逐条重写见第十五章。\n' + v120);

// ---------- 14. 10.1 变更总览追加 v1.19/v1.20 提示 ----------
rep('14 10.1 追加 v1.19/v1.20 提示',
  'M16 仅小微版生效，**不影响基础版/高级版/专业版任何任务状态与口径**。',
  'M16 仅小微版生效，**不影响基础版/高级版/专业版任何任务状态与口径**。\n\n> **⚠️ v1.19 / v1.20 变更提示（状态对账续作，上表仍未含这两列）**：两版**均不增删任务、不改任务定义与优先级**，任务总数恒为 **160**、里程碑恒为 **17**、开关恒为 **34**、防串用例恒为 **34 条**、P0/P1/P2 恒为 **120/33/7**。四态演进：**v1.16 = 77/13/45/25 → v1.19 = ✅90 / 🔄6 / 🟡43 / 🆕21 → v1.20 = ✅92 / 🔄5 / 🟡42 / 🆕21**。v1.19 翻转 12 项（小程序端 18 个 micro 页面落盘 + 桩实现真身更正 + 部署包收口），v1.20 翻转 2 项（T1.5 / T4.3）并改写 2 项理由（T3.11 / T3.15）、纠正 1 处 note 与表格冲突（T2.3）。逐条依据见第三章「v1.19 / v1.20 状态分布说明」与各里程碑 note。');

// ---------- 15. 10.2 历史快照追加 v1.20 补注 ----------
rep('15 10.2 追加 v1.20 补注',
  '**最新任务状态以第二章与第十三章为准**，本节仅用于追溯「v1.12 当时为什么要改」。',
  '**最新任务状态以第二章与第十三章为准**，本节仅用于追溯「v1.12 当时为什么要改」。\n>\n> **⚠️ v1.20 补注（不改上表标记）**：上述「残留 13 项 🔄」清单中，**T1.5 / T2.3 / T5.13 / T5.14 / T5.15 / T5.16 / T5.17 共 7 项已在后续版本翻 ✅**，**T5.18 翻 🟡**，故 v1.20 现状残留 🔄 仅 **5 项**：**T4.5**（防串用例仅 21 条且口径 stale，未扩至 34 条）、**T8.1**（小程序角色分流仍为四 Tab 静态渲染）、**T8.4**（报销人端未按开关渲染）、**T9.1**（防串回归未执行，验证类）、**T9.2**（升级降级演练报告未产出，验证类）。T1.5 的翻转依据见 M1 v1.20 note，T4.3 虽不在「残留 🔄」清单内（v1.15 记为 🟡）亦于本轮翻 ✅，依据见 M4 v1.20 note。');

// ---------- 16. 文末版本号 + 变更记录 ----------
rep('16a 文末文档版本 v1.16→v1.20',
  '**文档版本**：v1.16',
  '**文档版本**：v1.20');
rep('16b 变更记录前置 v1.20 / v1.19 条目',
  '**变更记录**：v1.16 推广分销与激励整合（2026-09-10）：',
  '**变更记录**：v1.20 分册冲突实测裁定与 note 对齐（2026-09-10）：**不增删任务、不改任务定义与优先级**，任务总数 160 / 里程碑 17 / 开关 34 / 防串用例 34 条 / P0-P1-P2 = 120-33-7 全部不变；四态由 v1.19 的 **90/6/43/21** 变为 **✅92 / 🔄5 / 🟡42 / 🆕21**。起因是《系统管理》分册（v1.16）与母文档在 4 项任务上判定相反，逐项以**代码实测**裁定：**T1.5 🔄→✅**（`FeeExpenseCategoryController` L31 类级 `@RequireFeature("FEAT_SUBJECT_3LAYER")` 已存在，另 4 处同标；建表侧 `M5_02`+`M5_06`）、**T4.3 🟡→✅**（`feature.js` L144 `filterRoutesByFeature` + `permission.js` L39 路由生成期过滤 + L113 `featureVisible`/L129 `getRequiredFeature`/L101 `isAdmin` 齐备；原 🟡 理由 i18n 已归口 T10.5，不重复扣分）、**T2.3 note 由 🔄 纠正为 ✅**（`SysDataScopeServiceImpl` L69/L74/L80 双表并查 + L101/L107 新枚举过滤已重写，v1.15 系「grep 枚举类零引用」误判；表格 v1.15 起即为 ✅，不影响计数）、**T3.11 维持 🟡 但理由改写**（v1.15「bizCode 校验未实现」事实错误，`LicenseServiceImpl` L784-791 已实现；真实缺口为 L803/L837 `edition` 原值直写未走 `normalizeEdition`（违反规范 4）+ L878 `getTenantSelector` 不兼容 `pro`）、**T3.15 维持 🟡**（分册标 ✅ 过早：L113/L165 自承简化、无 `case "pro"`、无套餐入参、`bizCode` 仅交互式 prompt）、**T10.2/T10.3/T10.4 维持 🟡**（分册标 ✅ 过早，母文档 M10 = ✅3/🟡3/🆕2 为准）。同步：第三章统计表 M1 行 `9|8|1|0|0` → `9|9|0|0|0`、M4 行 `5|3|1|1|0` → `5|4|1|0|0`、合计行 `90|6|43|21` → `92|5|42|21`；M1/M3.5/M4 note 头升为 v1.20；第三章 v1.15/v1.16 两段历史说明加「⚠️ 历史口径」标注（规范 17 ⑥）；新增第三章「v1.20 状态分布说明」承载 6 行裁定表 + 裁定原则 + 连带修正；10.1 追加 v1.19/v1.20 变更提示、10.2 追加 v1.20 补注（历史快照标记原样保留）；文档头与文末版本号补齐为 v1.20（v1.19 未曾回填）。**登记实现偏差 1 项**：T4.3 路由拦截采用「未注册即不可达」（直敲命中 **404 兜底**）而非任务描述的「组件内二次校验跳转 403」，拦截效果等价且更早，**第五章防串用例 4 预期须按「404 或 403 均视为拦截成功」口径执行**。**确立裁定原则**：分册 note / 历史 note / 外部记忆与代码实测冲突时一律以实测为准，「分册比母文档新」不构成母文档跟随升级的理由；且「grep 零命中」不等于「功能未实现」。 v1.19 小程序端落盘与桩实现真身更正（2026-09-10）：四态由 v1.16 的 **77/13/45/25** 重算为 **✅90 / 🔄6 / 🟡43 / 🆕21**（任务总数 160 / 里程碑 17 / P0-P1-P2 = 120-33-7 不变），翻转 12 项（T5.9 / T5.17 / T10.7 / T13.3 / T15.12 / T15.18 / T15.19 升 ✅，T5.18 / T12.4 / T14.2 / T15.15 / T15.20 记 🟡，T8.5 改判 🆕）；更正 v1.15 note 的 4 处事实性错误（三个 `*MessageStub` 与 `ContentSecurityStub` 四个类名在全仓不存在、真身为 `Wechat*MessageProvider`/`WecomAppMessageProvider`/`ContentSecurityBlacklistChecker`；`ruoyi-app` 实有 18 个 micro 页面而非零页面；3 天只读宽限期与 7/3/1 提醒走 Provider 均已实现）；新增第三章 3.1「SQL 脚本归属总表」（母目录 70 项 × 五分册部署包矩阵 + 分包脚本数 + `M15_12` 先于 `M10_01` 的顺序强约束 + `M15_14` 仅归小微版的残留风险登记）；SQL 部署包收口（`M10_01` 补进四个分册包并为《系统管理》新建 `sql/` 目录 25 脚本、`M5_12`~`M5_14`/`M15_14` 回填母目录、`M8_01` 一号双脚本拆为 `M8_01`/`M8_02`、`M13_02` 孤儿修复、四个 `00_执行顺序.sql` 同步）。 v1.16 推广分销与激励整合（2026-09-10）：');

fs.writeFileSync(F, s, 'utf8');
log.push('');
log.push('RESULT: fail=' + fail + ' | bytes ' + before + ' -> ' + s.length + ' (' + (s.length - before) + ')');
log.push('lines: ' + s.split('\n').length);
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t4d_result.txt', log.join('\n'), 'utf8');
console.log(log.join('\n'));
