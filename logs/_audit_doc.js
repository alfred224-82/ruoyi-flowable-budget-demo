const fs = require('fs');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const DOC = ROOT + '/资金管控系统开发任务文档.md';
const L = fs.readFileSync(DOC, 'utf8').split(/\r?\n/);
console.log('MASTER DOC lines=' + L.length);

const pats = {
  'M8_01': /M8_01/,
  'M5_12/13/14': /M5_1[234]/,
  'M15_14': /M15_14/,
  'M10_01': /M10_01/,
  'M15_12': /M15_12/,
  'M13_02': /M13_02/,
};
Object.keys(pats).forEach(k => {
  console.log('');
  console.log('===== ' + k + ' =====');
  L.forEach((l, i) => {
    if (pats[k].test(l)) console.log('  L' + (i + 1) + ': ' + l.trim().slice(0, 190));
  });
});

console.log('');
console.log('===== 脚本清单小节定位（含 "建表脚本" / "脚本清单"）=====');
L.forEach((l, i) => {
  if (/脚本清单|建表脚本|SQL 脚本/.test(l)) console.log('  L' + (i + 1) + ': ' + l.trim().slice(0, 160));
});

console.log('');
console.log('===== 章节标题定位 =====');
L.forEach((l, i) => {
  if (/^#{1,3}\s/.test(l)) console.log('  L' + (i + 1) + ': ' + l.trim().slice(0, 120));
});
