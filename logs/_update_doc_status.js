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

// ===== 1. M5 任务表：T5.13~T5.18 状态翻转 =====
// T5.13: 🔄 → ✅
replaceOnce(
  '| 🔄 T5.13 |',
  '| ✅ T5.13 |',
  'M5 T5.13 🔄→✅ (费用大类三层角色 @RequireFeature 补齐)'
);

// T5.14: 🔄 → ✅
replaceOnce(
  '| 🔄 T5.14 |',
  '| ✅ T5.14 |',
  'M5 T5.14 🔄→✅ (预算编制闭环 BudgetPreparationController 标注)'
);

// T5.15: 🔄 → ✅
replaceOnce(
  '| 🔄 T5.15 |',
  '| ✅ T5.15 |',
  'M5 T5.15 🔄→✅ (预算调整申请 BudgetAdjustmentAppController 标注)'
);

// T5.16: 🔄 → ✅
replaceOnce(
  '| 🔄 T5.16 |',
  '| ✅ T5.16 |',
  'M5 T5.16 🔄→✅ (科目三层+财务科目导入 FeeFinanceSubjectController 标注)'
);

// T5.17: 🔄 → 🟡
replaceOnce(
  '| 🔄 T5.17 |',
  '| 🟡 T5.17 |',
  'M5 T5.17 🔄→🟡 (报销管控 ExpenseBillController 标注，但通知未统一Provider)'
);

// T5.18: 🔄 → 🟡
replaceOnce(
  '| 🔄 T5.18 |',
  '| 🟡 T5.18 |',
  'M5 T5.18 🔄→🟡 (规则引擎/发票验真内嵌报销受保护，但反哺规则库未实现)'
);

// ===== 2. M6 任务表：T6.8 状态翻转 =====
// T6.8: 🟡 → ✅
replaceOnce(
  '| 🟡 T6.8 |',
  '| ✅ T6.8 |',
  'M6 T6.8 🟡→✅ (预算域5开关@RequireFeature主体完成)'
);

// ===== 3. Section 三统计表 =====
// M5 行: 11/6/2 → 15/2/2
replaceOnce(
  '| M5 基础版功能（重定边界） | 19 | 11 | 6 | 2 | 0 | 18 | 1 | 0 |',
  '| M5 基础版功能（重定边界） | 19 | 15 | 2 | 2 | 0 | 18 | 1 | 0 |',
  '统计表 M5 行: ✅11→15, 🔄6→2'
);

// M6 行: 7/0/1 → 8/0/0
replaceOnce(
  '| M6 高级版增量（承接下移能力） | 8 | 7 | 0 | 1 | 0 | 8 | 0 | 0 |',
  '| M6 高级版增量（承接下移能力） | 8 | 8 | 0 | 0 | 0 | 8 | 0 | 0 |',
  '统计表 M6 行: ✅7→8, 🟡1→0'
);

// 合计行: 77/13/45 → 83/7/47
replaceOnce(
  '| **合计** | **160** | **77** | **13** | **45** | **25** |',
  '| **合计** | **160** | **83** | **7** | **47** | **25** |',
  '统计表合计行: ✅77→83, 🔄13→7, 🟡45→47'
);

// ===== 4. v1.15 变更总览表 (10.1节) =====
// ✅ 行: 77 → 83
replaceOnce(
  '| ✅ 已完成 | 71 | **36** | **36** | **36** | **77** |',
  '| ✅ 已完成 | 71 | **36** | **36** | **36** | **83** |',
  '10.1 ✅行: 77→83'
);

// 🟡 行: 45 → 47
replaceOnce(
  '| 🟡 部分交付 | — | — | — | — | **45** |',
  '| 🟡 部分交付 | — | — | — | — | **47** |',
  '10.1 🟡行: 45→47'
);

// 🔄 行: 13 → 7
replaceOnce(
  '| 🔄 待重新开发 | 0 | **35** | **35** | **35** | **13** |',
  '| 🔄 待重新开发 | 0 | **35** | **35** | **35** | **7** |',
  '10.1 🔄行: 13→7'
);

// ===== 5. M5 note 段：v1.15 对账结果 =====
replaceOnce(
  '> **v1.15 对账结果（✅ 11 / 🔄 6 / 🟡 2 / 🆕 0）**',
  '> **v1.16 对账结果（✅ 15 / 🔄 2 / 🟡 2 / 🆕 0）**',
  'M5 note 对账结果标题: 11/6→15/2'
);

// ===== 6. M6 note 段：v1.15 对账结果标题 =====
replaceOnce(
  '> **v1.15 对账结果（✅ 7 / 🔄 0 / 🟡 1 / 🆕 0）**：**T6.8 降为 🟡**',
  '> **v1.16 对账结果（✅ 8 / 🔄 0 / 🟡 0 / 🆕 0）**：**T6.8 升为 ✅**',
  'M6 note 对账结果标题: 7/0/1→8/0/0, T6.8 🟡→✅'
);

// ===== 7. M6 note 段：详细说明 =====
// 旧文本（关键段落）
const m6OldDetail = '但预算域 5 个开关（`FEAT_SUBJECT_3LAYER`/`FEAT_BUDGET_PREPARE`/`FEAT_EXPENSE_CONTROL`/`FEAT_RULE_ENGINE_BASE`/`FEAT_INVOICE_VERIFY`）接口层 0 标注 + 分成（3204）接口未标 `FEAT_HIDDEN_COST`**，为本任务未完成部分（直接影响 T5.13~T5.18 与 T1.5 的 🔄 保留判定）。';
const m6NewDetail = '预算域 5 个开关（`FEAT_SUBJECT_3LAYER`/`FEAT_BUDGET_PREPARE`/`FEAT_EXPENSE_CONTROL`/`FEAT_RULE_ENGINE_BASE`/`FEAT_INVOICE_VERIFY`）接口层 `@RequireFeature` 已由 7 个 Controller 类级标注补齐（`BudgetPreparationController`/`BudgetSubjectController`/`BudgetAdjustmentAppController`/`FeeTenantCategoryController`/`FeeFinanceSubjectController`/`ExpenseBillController`/`FeeProjectSharingController`），分成（3204）接口已标 `FEAT_HIDDEN_COST`**，主体工作完成。残留缺口：① 通知未统一走 Provider（T13.3 仍为桩）；② 规则引擎反哺规则库未实现（匹配链路为内置规则+兜底确认）。';

replaceOnce(m6OldDetail, m6NewDetail, 'M6 note 详细说明: 5开关补齐 → 主体完成+残留缺口');

// ===== 验证 =====
if (content === original) {
  console.log('\n[WARN] No changes made!');
  process.exit(1);
}

fs.writeFileSync(filePath, content, 'utf8');
console.log('\n[DONE] ' + changes.length + ' replacements applied.');
console.log('File: ' + filePath);
