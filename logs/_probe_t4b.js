const fs = require('fs');
const F = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
const lines = fs.readFileSync(F, 'utf8').split(/\r?\n/);
const out = [];
const say = s => out.push(s);

// 1) 全文所有含「合计」的行
say('===== 全文含「合计」的行（' + lines.length + ' 行文档）=====');
lines.forEach((l, i) => { if (l.includes('合计')) say('  L' + (i + 1) + ' | ' + l); });

// 2) 第三章起点与所有 ### / #### 标题
const secEnd = lines.findIndex(l => /^## 三/.test(l));
say('');
say('===== 第三章标题（L' + (secEnd + 1) + ' 起）=====');
for (let i = secEnd; i < lines.length; i++) {
  if (/^#{2,4} /.test(lines[i])) say('  L' + (i + 1) + ' | ' + lines[i]);
  if (/^## 四/.test(lines[i])) break;
}

// 3) M2 note 与 M2 表格
say('');
say('===== M2 段落 =====');
const m2s = lines.findIndex(l => /^### M2\s/.test(l));
const m3s = lines.findIndex(l => /^### M3\s/.test(l));
for (let i = m2s; i < m3s; i++) say('  L' + (i + 1) + ' | ' + lines[i]);

// 4) 所有 note 头部行（> **v1.x 对账结果）
say('');
say('===== 全部 note 头部声明行 =====');
lines.forEach((l, i) => { if (/^> \*\*v1\.\d+ 对账结果/.test(l)) say('  L' + (i + 1) + ' | ' + l.slice(0, 120)); });

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t4_probe2.txt', out.join('\n'), 'utf8');
console.log(out.join('\n'));
