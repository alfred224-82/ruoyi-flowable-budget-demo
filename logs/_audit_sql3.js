const fs = require('fs');
const MASTER = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/sql';
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const DOC = ROOT + '/资金管控系统开发任务文档.md';

console.log('=== 1. M15_12 全文（判断 1300 目录创建可否复用）===');
fs.readFileSync(ROOT + '/小微版/sql/M15_12_fund_platform_menu_edition.sql', 'utf8')
  .split(/\r?\n/).forEach((l, i) => console.log((i + 1) + ': ' + l));

console.log('');
console.log('=== 2. M5_02 diff: MASTER vs 高级版 ===');
const a = fs.readFileSync(MASTER + '/M5_02_create_tables_category.sql', 'utf8').split(/\r?\n/);
const b = fs.readFileSync(ROOT + '/高级版/sql/M5_02_create_tables_category.sql', 'utf8').split(/\r?\n/);
console.log('  MASTER lines=' + a.length + '  ADV lines=' + b.length);
const max = Math.max(a.length, b.length);
let d = 0;
for (let i = 0; i < max; i++) {
  if (a[i] !== b[i]) {
    d++;
    if (d <= 25) {
      console.log('  L' + (i + 1) + ' MASTER: ' + (a[i] === undefined ? '<EOF>' : a[i].slice(0, 150)));
      console.log('  L' + (i + 1) + ' ADV    : ' + (b[i] === undefined ? '<EOF>' : b[i].slice(0, 150)));
    }
  }
}
console.log('  total diff lines=' + d);
// header version hints
console.log('  MASTER head: ' + a.slice(0, 6).join(' | ').slice(0, 300));
console.log('  ADV head   : ' + b.slice(0, 6).join(' | ').slice(0, 300));
