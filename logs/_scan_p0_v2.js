const fs = require('fs');
const p = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
const t = fs.readFileSync(p, 'utf8');
const lines = t.split(/\r?\n/);

// Collect ALL task rows from chapter 2 (M1-M16)
let allTasks = [];
let inChapter2 = false;

lines.forEach((l, i) => {
  if (/^## 二、任务明细/.test(l)) inChapter2 = true;
  if (/^## 三、/.test(l)) inChapter2 = false;
  
  if (inChapter2) {
    // Match: | status T*.* | ...
    const m = l.match(/^\|\s*([✅🟡🔄🆕])\s*T(\d+\.\d+)\s*\|/);
    if (m) {
      // Find priority - look for P0/P1/P2 at end
      const pm = l.match(/\|\s*(P[012])\s*\|?\s*$/);
      const priority = pm ? pm[1] : '?';
      allTasks.push({
        status: m[1],
        task: 'T' + m[2],
        priority: priority,
        line: i + 1
      });
    }
  }
});

// Count by status
const counts = {};
allTasks.forEach(t => {
  counts[t.status] = (counts[t.status] || 0) + 1;
});
console.log('总任务数:', allTasks.length, '状态分布:', JSON.stringify(counts));

// P0 non-✅
const p0NotDone = allTasks.filter(t => t.priority === 'P0' && t.status !== '✅');
console.log('\nP0 非✅ 数量:', p0NotDone.length);
console.log('\n--- P0 非✅ 清单 ---');
p0NotDone.forEach(x => {
  console.log(x.status + ' ' + x.task + ' (L' + x.line + ')');
});

// P0 ✅ count
const p0Done = allTasks.filter(t => t.priority === 'P0' && t.status === '✅');
console.log('\nP0 ✅ 数量:', p0Done.length);
console.log('P0 总计:', p0NotDone.length + p0Done.length);
