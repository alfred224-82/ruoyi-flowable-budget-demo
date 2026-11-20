const fs = require('fs');
const F = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
const lines = fs.readFileSync(F, 'utf8').split(/\r?\n/);
const out = [];
lines.forEach((l, i) => { if (/^## /.test(l)) out.push('L' + (i + 1) + ' | ' + l); });
out.push('---- 含「变更总览」/「v1.19」/「v1.20」的行 ----');
lines.forEach((l, i) => { if (/变更总览|v1\.19|v1\.20/.test(l)) out.push('L' + (i + 1) + ' | ' + l.slice(0, 150)); });
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t4_head.txt', out.join('\n'), 'utf8');
console.log(out.join('\n'));
