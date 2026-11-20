const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const pkgs = ['专业版', '高级版', '基础版', '小微版', '系统管理'];

function readUtf8(p) { return fs.readFileSync(p, 'utf8'); }

console.log('=== A. 各 00_执行顺序.sql 中真实 source 行（排除注释）===');
for (const g of pkgs) {
  const f = path.join(ROOT, g, 'sql', '00_执行顺序.sql');
  if (!fs.existsSync(f)) { console.log(g + ' -> NO sql dir'); continue; }
  const lines = readUtf8(f).split(/\r?\n/);
  const srcs = [];
  lines.forEach((l, i) => {
    const t = l.trim();
    if (/^source\s+/i.test(t)) srcs.push({ n: i + 1, s: t.replace(/;$/, '').replace(/^source\s+/i, '') });
  });
  const names = srcs.map(x => x.s);
  const hasM10 = names.some(n => /M10_01/.test(n));
  const hasM513 = names.some(n => /M5_13/.test(n));
  const hasM1514 = names.some(n => /M15_14/.test(n));
  const m801 = names.filter(n => /M8_01/.test(n));
  console.log('--- ' + g + ' | sourceLines=' + srcs.length + ' | M10_01=' + hasM10 + ' M5_13=' + hasM513 + ' M15_14=' + hasM1514);
  if (m801.length) console.log('    M8_01 dup: ' + JSON.stringify(m801));
  // dangling refs: source line points to a file not in the dir
  const dir = path.dirname(f);
  const missing = names.filter(n => !fs.existsSync(path.join(dir, n)));
  if (missing.length) console.log('    !! DANGLING source refs: ' + JSON.stringify(missing));
}

console.log('');
console.log('=== B. 注释中提及 M5_13 / M15_14 / M10_01 的行（说明性文字）===');
for (const g of pkgs) {
  const f = path.join(ROOT, g, 'sql', '00_执行顺序.sql');
  if (!fs.existsSync(f)) continue;
  readUtf8(f).split(/\r?\n/).forEach((l, i) => {
    if (/M5_13|M15_14|M10_01/.test(l) && !/^\s*source/i.test(l.trim())) {
      console.log(g + ' L' + (i + 1) + ': ' + l.trim());
    }
  });
}

console.log('');
console.log('=== C. 各包实际 .sql 文件 vs source 引用差集 ===');
for (const g of pkgs) {
  const d = path.join(ROOT, g, 'sql');
  if (!fs.existsSync(d)) { console.log(g + ' -> NO sql dir'); continue; }
  const files = fs.readdirSync(d).filter(x => x.endsWith('.sql') && x !== '00_执行顺序.sql').sort();
  const idx = readUtf8(path.join(d, '00_执行顺序.sql')).split(/\r?\n/)
    .map(l => l.trim()).filter(t => /^source\s+/i.test(t))
    .map(t => t.replace(/;$/, '').replace(/^source\s+/i, ''));
  const notRefed = files.filter(x => !idx.includes(x));
  const notExist = idx.filter(x => !files.includes(x));
  console.log('--- ' + g + ' | files=' + files.length + ' refs=' + idx.length);
  if (notRefed.length) console.log('    未被引用(孤儿文件): ' + JSON.stringify(notRefed));
  if (notExist.length) console.log('    引用但不存在: ' + JSON.stringify(notExist));
}
