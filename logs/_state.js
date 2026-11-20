/** 当前工作区状态盘点：logs 下探针文件 + 六份文档的版本/行数/字符数 */
const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/';
const out = [];
const say = s => out.push(s);

// A. logs 下 _ 开头的文件（按 mtime 倒序）
const ls = fs.readdirSync(R + 'logs').filter(f => f.startsWith('_'))
  .map(f => { const st = fs.statSync(R + 'logs/' + f); return { f, m: st.mtimeMs, s: st.size }; })
  .sort((a, b) => b.m - a.m);
say('=== A. logs/_* （' + ls.length + ' 个，按 mtime 倒序） ===');
ls.forEach(x => say(new Date(x.m).toISOString().slice(5, 16).replace('T', ' ') + '  ' + String(x.s).padStart(7) + '  ' + x.f));

// B. 六份文档
say('');
say('=== B. 母文档 + 五分册 ===');
const docs = [
  ['母文档', 'docs/最新整理好的需求/资金管控系统开发任务文档.md'],
  ['系统管理', 'docs/最新整理好的需求/系统管理/资金管控系统开发任务文档（系统管理）.md'],
  ['基础版', 'docs/最新整理好的需求/基础版/资金管控系统开发任务文档（基础版）.md'],
  ['小微版', 'docs/最新整理好的需求/小微版/资金管控系统开发任务文档（小微版）.md'],
  ['高级版', 'docs/最新整理好的需求/高级版/资金管控系统开发任务文档（高级版）.md'],
  ['专业版', 'docs/最新整理好的需求/专业版/资金管控系统开发任务文档（专业版）.md']
];
for (const [tag, p] of docs) {
  let txt;
  try { txt = fs.readFileSync(R + p, 'utf8'); } catch (e) { say(tag + ' | MISSING ' + p); continue; }
  const a = txt.split(/\r?\n/);
  const head = a.slice(0, 10).map((l, i) => 'L' + (i + 1) + ':' + l.trim()).filter(l => /版本|日期|来源/.test(l)).join(' || ');
  const tail = a.map((l, i) => [i + 1, l]).filter(([i, l]) => /文档版本|变更记录/.test(l)).slice(-2)
    .map(([i, l]) => 'L' + i + ':' + l.trim().slice(0, 60)).join(' || ');
  say('--- ' + tag + ' | lines=' + a.length + ' chars=' + txt.length);
  say('    HEAD ' + head);
  say('    TAIL ' + tail);
}
fs.writeFileSync(R + 'logs/_state.txt', out.join('\n'), 'utf8');
console.log('ok');
