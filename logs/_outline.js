/** 母文档章节骨架 + 第十章定位 */
const fs = require('fs');
const P = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
const a = fs.readFileSync(P, 'utf8').split(/\r?\n/);
const out = [];
a.forEach((l, i) => {
  if (/^#{1,4}\s/.test(l)) out.push('L' + (i + 1) + ' | ' + l.slice(0, 100));
});
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_outline.txt', out.join('\n'), 'utf8');
console.log('headings=' + out.length);
