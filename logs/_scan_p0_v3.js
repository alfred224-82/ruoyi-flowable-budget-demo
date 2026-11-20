const fs = require('fs');
const p = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
const t = fs.readFileSync(p, 'utf8');
const lines = t.split(/\r?\n/);

let inCh2 = false;
let p0NotDone = [];
let p0Done = 0;
let allNonDone = [];

lines.forEach((l, i) => {
  if (/^## 二、/.test(l)) inCh2 = true;
  if (/^## 三、/.test(l)) inCh2 = false;
  if (!inCh2 || !l.startsWith('|') || !/T\d+\.\d+/.test(l)) return;

  const tm = l.match(/T(\d+\.\d+)/);
  if (!tm) return;
  const task = 'T' + tm[1];
  const isP0 = /\|\s*P0\s*\|?\s*$/.test(l) || /\|\s*P0\s*\|/.test(l);
  
  let status = '?';
  if (l.includes('🆕')) status = '🆕';
  else if (l.includes('🔄')) status = '🔄';
  else if (l.includes('🟡')) status = '🟡';
  else if (l.includes('✅')) status = '✅';
  
  if (isP0) {
    if (status === '✅') {
      p0Done++;
    } else {
      p0NotDone.push({ status, task, line: i + 1 });
    }
  }
  
  if (status !== '✅') {
    allNonDone.push({ status, task, line: i + 1, isP0 });
  }
});

console.log('P0 已完成:', p0Done, 'P0 未完成:', p0NotDone.length);
console.log('\n--- P0 非✅ 清单（按状态分组）---');
['🔄', '🟡', '🆕'].forEach(s => {
  const items = p0NotDone.filter(x => x.status === s);
  if (items.length > 0) {
    console.log('\n' + s + ' (' + items.length + ' 项):');
    items.forEach(x => console.log('  ' + x.task + ' (L' + x.line + ')'));
  }
});

console.log('\n\n--- 全部非✅ 任务 (' + allNonDone.length + ' 项) ---');
['🔄', '🟡', '🆕'].forEach(s => {
  const items = allNonDone.filter(x => x.status === s);
  console.log(s + ': ' + items.length + ' 项 → ' + items.map(x => x.task).join(', '));
});
