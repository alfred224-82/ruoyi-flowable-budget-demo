const fs = require('fs');
const path = require('path');

const filePath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X',
  'docs', '最新整理好的需求', '资金管控系统开发任务文档.md');

let content = fs.readFileSync(filePath, 'utf8');
const original = content;
let changes = [];

function replaceOnce(old, replacement, label) {
  const idx = content.indexOf(old);
  if (idx === -1) {
    console.log('[MISS] ' + label);
    return;
  }
  content = content.substring(0, idx) + replacement + content.substring(idx + old.length);
  changes.push(label);
  console.log('[OK]   ' + label);
}

// ===== 1. M5 note 详细说明：更新 "保留 🔄（6 项）" 段落 =====
const m5OldDetail = '> - **保留 🔄（6 项）**：T5.13~T5.18 预算域 5 个开关（`FEAT_BUDGET_PREPARE` / `FEAT_SUBJECT_3LAYER` / `FEAT_EXPENSE_CONTROL` / `FEAT_RULE_ENGINE_BASE` / `FEAT_INVOICE_VERIFY`）**全仓只在 `EditionFeatureConfig` 常量集中出现，无任何 Controller 标注 `@RequireFeature`**（`BudgetPreparationController`/`BudgetSubjectController`/`BudgetAdjustmentAppController`/`FeeExpenseCategoryController`/`FeeTenantCategoryController`/`FeeFinanceSubjectController` 均未标注，`ExpenseBillController` 仅有 `FEAT_EVIDENCE_CHAIN`）——**基础版租户目前仍能调通全部预算域接口，属防串第②层实质缺口**，改造完成前不得视为交付。';
const m5NewDetail = '> - **升为 ✅（4 项）**：T5.13/T5.14/T5.15/T5.16 预算域 5 个开关中 `FEAT_SUBJECT_3LAYER`/`FEAT_BUDGET_PREPARE` 已由 5 个 Controller 类级标注补齐（`BudgetPreparationController`/`BudgetSubjectController`/`BudgetAdjustmentAppController`/`FeeTenantCategoryController`/`FeeFinanceSubjectController`），`FeeExpenseCategoryController` 已有标注，防串第②层缺口已封闭。';

replaceOnce(m5OldDetail, m5NewDetail, 'M5 note: 保留🔄6项 → 升为✅4项');

// ===== 2. M5 note 详细说明：更新 "降为 🟡（2 项）" 段落 =====
const m5OldYellow = '> - **降为 🟡（2 项）**：T5.9 隐形成本——`FeeAssetController`/`FeeStaffSalaryController`/`FeeLoanController` 已标 `FEAT_HIDDEN_COST`，但 **`FeeProjectSharingController`（分成 3204）未标注**；T5.19 基础版轻量记账边界——`FeeLightOverviewController`（基础版标配、故意不设开关）+ `fee_project_subject`（`M1_02`）+ 《基础版能力边界说明书》已交付，但「报销单在基础版租户下短路跳过科目/额度/发票校验」因上述 5 开关未落地而无法生效。';
const m5NewYellow = '> - **降为 🟡（2 项）**：T5.17 报销管控——`ExpenseBillController` 已标 `FEAT_EXPENSE_CONTROL` 类级注解，但通知未统一走 Provider（T13.3 仍为桩）；T5.18 规则引擎/发票验真——内嵌报销流程受 `FEAT_EXPENSE_CONTROL` 保护，但反哺规则库未实现（匹配链路为内置规则+兜底确认）。T5.9 隐形成本升为 ✅（`FeeProjectSharingController` 已补 `FEAT_HIDDEN_COST`）。T5.19 基础版边界维持 🟡。';

replaceOnce(m5OldYellow, m5NewYellow, 'M5 note: 降为🟡2项 → 更新为T5.17/T5.18');

// ===== 3. v1.15 状态分布说明：更新统计数字 =====
// 3a. ✅ 77 项 → 83 项
replaceOnce(
  '> - ✅ **77 项**',
  '> - ✅ **83 项**',
  '状态说明: ✅77→83'
);

// 3b. 🟡 45 项 → 47 项
replaceOnce(
  '> - 🟡 **45 项（v1.15 新增状态）**',
  '> - 🟡 **47 项（v1.15 新增状态）**',
  '状态说明: 🟡45→47'
);

// 3c. 🔄 13 项 → 7 项
replaceOnce(
  '> - 🔄 **13 项**：v1.12 改造**主体未完成**，改造完成前不得视为交付——T1.5、T2.3、T4.5、T5.13~T5.18（预算域 6 项）、T8.1、T8.4、T9.1、T9.2。**核心风险：预算域 5 个开关接口层 0 标注**',
  '> - 🔄 **7 项**：v1.12 改造**主体未完成**，改造完成前不得视为交付——T1.5、T2.3、T4.5、T8.1、T8.4、T9.1、T9.2。**预算域 5 个开关接口层 `@RequireFeature` 已由 7 个 Controller 类级标注补齐（v1.18 封闭防串第②层实质缺口）**',
  '状态说明: 🔄13→7 + 移除预算域6项 + 更新核心风险'
);

// 3d. 已程序化计数校验: 77+13+45+13=148 → 83+7+47+13=150... 
// Actually total is 160 (with M16 12 items). Let me check: 83+7+47+25=162? No...
// 83+7+47+25 = 162, that's wrong. Let me recalculate.
// Wait: original was 77+13+45+25=160. New: 83+7+47+25=162. That's +2 too many!
// Hmm, let me recount: T5.13 🔄→✅ (+1✅,-1🔄), T5.14 🔄→✅ (+1✅,-1🔄), 
// T5.15 🔄→✅ (+1✅,-1🔄), T5.16 🔄→✅ (+1✅,-1🔄), T5.17 🔄→🟡 (+1🟡,-1🔄),
// T5.18 🔄→🟡 (+1🟡,-1🔄), T6.8 🟡→✅ (+1✅,-1🟡)
// Net: ✅+5, 🔄-6, 🟡+2-1=+1
// New: ✅77+5=82, 🔄13-6=7, 🟡45+1=46, 🆕25
// Wait that doesn't match what I put in the table (83/7/47/25)!
// Let me recount more carefully:
// T5.13: 🔄→✅: ✅+1, 🔄-1
// T5.14: 🔄→✅: ✅+1, 🔄-1
// T5.15: 🔄→✅: ✅+1, 🔄-1
// T5.16: 🔄→✅: ✅+1, 🔄-1
// T5.17: 🔄→🟡: 🟡+1, 🔄-1
// T5.18: 🔄→🟡: 🟡+1, 🔄-1
// T6.8:  🟡→✅: ✅+1, 🟡-1
// Total: ✅+5, 🔄-6, 🟡+2-1=+1
// So: ✅77+5=82, 🔄13-6=7, 🟡45+1=46, 🆕25
// Sum: 82+7+46+25=160 ✓
// But I put 83/7/47/25 in the table! That sums to 162!
// ERROR! The table should be 82/7/46/25!

// Hmm wait, but the script already wrote 83/7/47/25 to the file. Let me check if
// that's actually wrong or if my counting is off.
// 
// Actually, I think I need to also count T5.9 and T1.5 changes:
// From the memory: "T5.9: 🟡 → ✅" was done in a PREVIOUS session (v1.17)
// And "T1.5: 🟡 → ✅" was done in a PREVIOUS session (v1.16 system管理)
// These were already counted in the base numbers (77/13/45/25)?
// 
// Actually no - the base numbers 77/13/45/25 are from the v1.15/v1.16 document
// BEFORE this session's changes. The memory says "✅77" was the count at v1.15.
// But the system管理 memory says "✅85" which is a different document.
//
// The document we're editing is the MAIN document (资金管控系统开发任务文档.md).
// Its statistics table showed 77/13/45/25 before our changes.
//
// So the correct new numbers should be:
// ✅: 77 + 5 (T5.13~T5.16 + T6.8) = 82
// 🔄: 13 - 6 (T5.13~T5.18) = 7
// 🟡: 45 + 2 (T5.17 + T5.18) - 1 (T6.8) = 46
// 🆕: 25 (unchanged)
// Sum: 82 + 7 + 46 + 25 = 160 ✓
//
// I made an error in the first script! The table should be 82/7/46/25, not 83/7/47/25!
// I need to fix this.

console.log('\n[ERROR DETECTED] Statistics miscalculated! Fixing...');

// Fix the statistics table
replaceOnce(
  '| **合计** | **160** | **83** | **7** | **47** | **25** |',
  '| **合计** | **160** | **82** | **7** | **46** | **25** |',
  'FIX 统计表合计行: 83→82, 47→46'
);

// Fix M5 row (should be 11+4=15 ✅, 6-4=2 🔄, 2+0=2 🟡 ... wait)
// M5 before: ✅11 🔄6 🟡2
// Changes in M5: T5.13 🔄→✅, T5.14 🔄→✅, T5.15 🔄→✅, T5.16 🔄→✅, T5.17 🔄→🟡, T5.18 🔄→🟡
// M5 after: ✅11+4=15, 🔄6-6=0, 🟡2+2=4
// Wait that gives 15+0+4=19 ✓ but I had 15/2/2 in the table!
// The M5 row should be 15/0/4, not 15/2/2!
replaceOnce(
  '| M5 基础版功能（重定边界） | 19 | 15 | 2 | 2 | 0 |',
  '| M5 基础版功能（重定边界） | 19 | 15 | 0 | 4 | 0 |',
  'FIX M5 row: 🔄2→0, 🟡2→4'
);

// Fix M6 row: T6.8 🟡→✅
// M6 before: ✅7 🟡1
// M6 after: ✅8 🟡0
// This is already correct in the table (8/0/0). ✓

// Fix 10.1 table
replaceOnce(
  '| ✅ 已完成 | 71 | **36** | **36** | **36** | **83** |',
  '| ✅ 已完成 | 71 | **36** | **36** | **36** | **82** |',
  'FIX 10.1 ✅行: 83→82'
);

replaceOnce(
  '| 🟡 部分交付 | — | — | — | — | **47** |',
  '| 🟡 部分交付 | — | — | — | — | **46** |',
  'FIX 10.1 🟡行: 47→46'
);

// Fix the 状态说明 ✅ 83 → 82
replaceOnce(
  '> - ✅ **83 项**',
  '> - ✅ **82 项**',
  'FIX 状态说明: ✅83→82'
);

// Fix the 状态说明 🟡 47 → 46
replaceOnce(
  '> - 🟡 **47 项（v1.15 新增状态）**',
  '> - 🟡 **46 项（v1.15 新增状态）**',
  'FIX 状态说明: 🟡47→46'
);

// Also fix the M5 note header: should be ✅15/🔄0/🟡4
replaceOnce(
  '> **v1.16 对账结果（✅ 15 / 🔄 2 / 🟡 2 / 🆕 0）**',
  '> **v1.16 对账结果（✅ 15 / 🔄 0 / 🟡 4 / 🆕 0）**',
  'FIX M5 note header: 🔄2→0, 🟡2→4'
);

// ===== Verify =====
if (content === original) {
  console.log('\n[WARN] No changes made!');
  process.exit(1);
}

fs.writeFileSync(filePath, content, 'utf8');
console.log('\n[DONE] ' + changes.length + ' replacements applied.');
