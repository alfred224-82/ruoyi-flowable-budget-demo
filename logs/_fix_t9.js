const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/';

// === 1. 高级版操作手册 v2.0 → v2.1 ===
{
  const p = R + '高级版/02_高级版操作手册.md';
  let t = fs.readFileSync(p, 'utf8');
  t = t.replace(
    '**版本**：v2.0（对齐 v1.12「折中下移」口径）',
    '**版本**：v2.1（对齐 v1.21 · 状态回填与 MP 赛道首次核对）'
  );
  t = t.replace(
    '**日期**：2026-09-05',
    '**日期**：2026-09-11'
  );
  t = t.replace(
    '**文档版本**：v2.0（对齐 v1.12「折中下移」口径）',
    '**文档版本**：v2.1（对齐 v1.21 · 状态回填与 MP 赛道首次核对）'
  );
  // Add change note if not present
  if (!t.includes('最近变更') && !t.includes('v2.1')) {
    // Already replaced above, check for change note
  }
  if (!t.includes('v2.1 变更')) {
    t = t.replace(
      /(\*\*文档版本\*\*：v2\.1[^\n]*\n)/,
      '$1**最近变更**：2026-09-11（对齐母文档 v1.21：T13.3 桩实现真身更正、T12.4/T14.4/T11.2 理由按实测重写、版本号统一为 v2.1）\n'
    );
  }
  fs.writeFileSync(p, t, 'utf8');
  console.log('高级版操作手册: v2.0 → v2.1');
}

// === 2. 专业版操作手册 v2.0 → v2.1 ===
{
  const p = R + '专业版/03_专业版操作手册.md';
  let t = fs.readFileSync(p, 'utf8');
  t = t.replace(
    '**版本**：v2.0（对齐 v1.12「折中下移」口径）',
    '**版本**：v2.1（对齐 v1.21 · 状态回填与 MP 赛道首次核对）'
  );
  t = t.replace(
    '**日期**：2026-09-05',
    '**日期**：2026-09-11'
  );
  t = t.replace(
    '**文档版本**：v2.0（对齐 v1.12「折中下移」口径）',
    '**文档版本**：v2.1（对齐 v1.21 · 状态回填与 MP 赛道首次核对）'
  );
  if (!t.includes('v2.1 变更')) {
    t = t.replace(
      /(\*\*文档版本\*\*：v2\.1[^\n]*\n)/,
      '$1**最近变更**：2026-09-11（对齐母文档 v1.21：T7.2 MP 人脸采集缺口声明、T7.6↔T8.11 依赖声明、版本号统一为 v2.1）\n'
    );
  }
  fs.writeFileSync(p, t, 'utf8');
  console.log('专业版操作手册: v2.0 → v2.1');
}

// === 3. 小微版操作手册 v1.2 → v1.3 ===
{
  const p = R + '小微版/04_小微版操作手册.md';
  let t = fs.readFileSync(p, 'utf8');
  t = t.replace(
    '**版本**：v1.2（对齐 v1.16「小微版 M16 推广分销与激励里程碑（T16.1~T16.12）」口径）',
    '**版本**：v1.3（对齐 v1.21 · 状态回填与 MP 赛道首次核对）'
  );
  t = t.replace(
    '**日期**：2026-09-10',
    '**日期**：2026-09-11'
  );
  t = t.replace(
    '**文档版本**：v1.2（对齐 v1.16「小微版 M16 推广分销与激励里程碑（T16.1~T16.12）」口径）',
    '**文档版本**：v1.3（对齐 v1.21 · 状态回填与 MP 赛道首次核对）'
  );
  if (!t.includes('v1.3 变更')) {
    t = t.replace(
      /(\*\*文档版本\*\*：v1\.3[^\n]*\n)/,
      '$1**最近变更**：2026-09-11（对齐母文档 v1.21：小程序 18 页面 + 34 脚本确认、版本号统一为 v1.3）\n'
    );
  }
  fs.writeFileSync(p, t, 'utf8');
  console.log('小微版操作手册: v1.2 → v1.3');
}

console.log('DONE');
