const fs = require('fs');
const p = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
const t = fs.readFileSync(p, 'utf8');
const lines = t.split(/\r?\n/);

// Collect all P0 tasks that are NOT ✅
let p0NotDone = [];
let p0Total = 0;
let p0Done = 0;

lines.forEach((l, i) => {
  // Match task rows in M1-M16 tables: | ✅/🟡/🔄/🆕 T*.* | ... | P0 |
  const m = l.match(/^\|\s*([✅🟡🔄🆕])\s*T(\d+\.\d+)\s*\|/);
  if (m) {
    const isP0 = /\|\s*P0\s*\|?\s*$/.test(l) || /\|\s*P0\s*\|/.test(l);
    if (isP0) {
      p0Total++;
      if (m[1] === '✅') {
        p0Done++;
      } else {
        p0NotDone.push({ status: m[1], task: 'T' + m[2], line: i + 1, text: l.substring(0, 150) });
      }
    }
  }
});

console.log('P0 总计:', p0Total, '已完成:', p0Done, '未完成:', p0NotDone.length);
console.log('\n--- P0 未完成项 ---');
p0NotDone.forEach(x => {
  console.log(x.status + ' ' + x.task + ' (L' + x.line + '): ' + x.text);
});

// Also count 34 防串用例
let caseCount = 0;
lines.forEach((l, i) => {
  if (/^\|\s*\d+\s*\|/.test(l) && i >= 775 && i <= 815) {
    caseCount++;
  }
});
console.log('\n防串用例数:', caseCount);
