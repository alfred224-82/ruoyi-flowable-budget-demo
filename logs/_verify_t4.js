/**
 * 校验：母文档第二章任务表逐行状态计数 == 第三章统计表
 */
const fs = require('fs');
const F = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
const lines = fs.readFileSync(F, 'utf8').split(/\r?\n/);
const out = [];
const say = s => out.push(s);

// 1) 第二章任务表：定位每个 ### M 段落，统计 | 状态 T编号 | 行
const secStart = lines.findIndex(l => /^## 二/.test(l));
const secEnd = lines.findIndex(l => /^## 三/.test(l));
let cur = null;
const tally = {};
const order = [];
for (let i = secStart; i < secEnd; i++) {
  const l = lines[i];
  const m = l.match(/^### (M[\d.]+)\s/);
  if (m) { cur = m[1]; if (!tally[cur]) { tally[cur] = { '✅': 0, '🔄': 0, '🟡': 0, '🆕': 0, P0: 0, P1: 0, P2: 0, ids: [] }; order.push(cur); } continue; }
  if (!cur) continue;
  const r = l.match(/^\|\s*(✅|🔄|🟡|🆕)\s*(T[\d.]+)\s*\|/);
  if (r) {
    tally[cur][r[1]]++;
    tally[cur].ids.push(r[2] + r[1]);
    const p = l.match(/\|\s*(P[012])\s*\|\s*$/);
    if (p) tally[cur][p[1]]++;
    else say('  !! ' + r[2] + ' 行尾无优先级: ' + l.slice(-40));
  }
}
say('===== 第二章任务表实测（' + order.length + ' 个里程碑）=====');
let g = { '✅': 0, '🔄': 0, '🟡': 0, '🆕': 0, P0: 0, P1: 0, P2: 0, n: 0 };
order.forEach(k => {
  const t = tally[k];
  const n = t['✅'] + t['🔄'] + t['🟡'] + t['🆕'];
  g['✅'] += t['✅']; g['🔄'] += t['🔄']; g['🟡'] += t['🟡']; g['🆕'] += t['🆕'];
  g.P0 += t.P0; g.P1 += t.P1; g.P2 += t.P2; g.n += n;
  say('  ' + k.padEnd(5) + ' n=' + String(n).padStart(3) + '  ✅' + t['✅'] + ' 🔄' + t['🔄'] + ' 🟡' + t['🟡'] + ' 🆕' + t['🆕'] + '  | P0=' + t.P0 + ' P1=' + t.P1 + ' P2=' + t.P2);
});
say('  合计  n=' + g.n + '  ✅' + g['✅'] + ' 🔄' + g['🔄'] + ' 🟡' + g['🟡'] + ' 🆕' + g['🆕'] + '  | P0=' + g.P0 + ' P1=' + g.P1 + ' P2=' + g.P2);

// 2) 第三章统计表
say('');
say('===== 第三章统计表 =====');
const stat = {};
let totalRow = null;
for (let i = secEnd; i < lines.length; i++) {
  const l = lines[i];
  const m = l.match(/^\|\s*(M[\d.]+)[^|]*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|/);
  if (m) { stat[m[1]] = { n: +m[2], ok: +m[3], re: +m[4], part: +m[5], nw: +m[6], p0: +m[7], p1: +m[8], p2: +m[9] }; continue; }
  const tt = l.match(/^\|\s*\*\*合计\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|/);
  if (tt) totalRow = { n: +tt[1], ok: +tt[2], re: +tt[3], part: +tt[4], nw: +tt[5], p0: +tt[6], p1: +tt[7], p2: +tt[8] };
  if (/^### 3\.1/.test(l)) break;
}
Object.keys(stat).forEach(k => {
  const s = stat[k], t = tally[k];
  if (!t) { say('  !! 统计表有 ' + k + ' 但第二章无该段落'); return; }
  const n2 = t['✅'] + t['🔄'] + t['🟡'] + t['🆕'];
  const bad = [];
  if (s.n !== n2) bad.push('任务数 ' + s.n + '≠' + n2);
  if (s.ok !== t['✅']) bad.push('✅ ' + s.ok + '≠' + t['✅']);
  if (s.re !== t['🔄']) bad.push('🔄 ' + s.re + '≠' + t['🔄']);
  if (s.part !== t['🟡']) bad.push('🟡 ' + s.part + '≠' + t['🟡']);
  if (s.nw !== t['🆕']) bad.push('🆕 ' + s.nw + '≠' + t['🆕']);
  if (s.p0 !== t.P0) bad.push('P0 ' + s.p0 + '≠' + t.P0);
  if (s.p1 !== t.P1) bad.push('P1 ' + s.p1 + '≠' + t.P1);
  if (s.p2 !== t.P2) bad.push('P2 ' + s.p2 + '≠' + t.P2);
  say('  ' + k.padEnd(5) + (bad.length ? '✗ ' + bad.join('; ') : '✓ 一致'));
});
const sumStat = Object.values(stat).reduce((a, s) => ({ n: a.n + s.n, ok: a.ok + s.ok, re: a.re + s.re, part: a.part + s.part, nw: a.nw + s.nw, p0: a.p0 + s.p0, p1: a.p1 + s.p1, p2: a.p2 + s.p2 }), { n: 0, ok: 0, re: 0, part: 0, nw: 0, p0: 0, p1: 0, p2: 0 });
say('  统计表分行求和: n=' + sumStat.n + ' ✅' + sumStat.ok + ' 🔄' + sumStat.re + ' 🟡' + sumStat.part + ' 🆕' + sumStat.nw + ' P0=' + sumStat.p0 + ' P1=' + sumStat.p1 + ' P2=' + sumStat.p2);
say('  合计行:        n=' + totalRow.n + ' ✅' + totalRow.ok + ' 🔄' + totalRow.re + ' 🟡' + totalRow.part + ' 🆕' + totalRow.nw + ' P0=' + totalRow.p0 + ' P1=' + totalRow.p1 + ' P2=' + totalRow.p2);
const cmp = (a, b, tag) => say('  ' + tag + ': ' + (a === b ? '✓' : '✗ ' + a + ' vs ' + b));
say('');
say('===== 三方交叉校验（任务表 / 统计表分行和 / 合计行）=====');
cmp(g.n, sumStat.n, '任务总数(表vs分行和)'); cmp(g.n, totalRow.n, '任务总数(表vs合计行)');
cmp(g['✅'], sumStat.ok, '✅(表vs分行和)'); cmp(g['✅'], totalRow.ok, '✅(表vs合计行)');
cmp(g['🔄'], sumStat.re, '🔄(表vs分行和)'); cmp(g['🔄'], totalRow.re, '🔄(表vs合计行)');
cmp(g['🟡'], sumStat.part, '🟡(表vs分行和)'); cmp(g['🟡'], totalRow.part, '🟡(表vs合计行)');
cmp(g['🆕'], sumStat.nw, '🆕(表vs分行和)'); cmp(g['🆕'], totalRow.nw, '🆕(表vs合计行)');
cmp(g.P0, sumStat.p0, 'P0(表vs分行和)'); cmp(g.P0, totalRow.p0, 'P0(表vs合计行)');
cmp(g.P1, sumStat.p1, 'P1(表vs分行和)'); cmp(g.P1, totalRow.p1, 'P1(表vs合计行)');
cmp(g.P2, sumStat.p2, 'P2(表vs分行和)'); cmp(g.P2, totalRow.p2, 'P2(表vs合计行)');
say('  四态自洽(合计行): ' + ((totalRow.ok + totalRow.re + totalRow.part + totalRow.nw) === totalRow.n ? '✓' : '✗'));
say('  优先级自洽(合计行): ' + ((totalRow.p0 + totalRow.p1 + totalRow.p2) === totalRow.n ? '✓' : '✗'));

// 3) note 头部声明的四态计数 vs 表格
say('');
say('===== note 头部声明计数 vs 任务表 =====');
for (let i = secStart; i < secEnd; i++) {
  const m = lines[i].match(/^> \*\*v1\.\d+ 对账结果（✅ (\d+) \/ 🔄 (\d+) \/ 🟡 (\d+) \/ 🆕 (\d+)[^）]*）[^*]*\*\*/);
  if (!m) continue;
  let k = null;
  for (let j = i; j >= secStart; j--) { const mm = lines[j].match(/^### (M[\d.]+)\s/); if (mm) { k = mm[1]; break; } }
  if (!k) continue;
  const t = tally[k];
  const bad = [];
  if (+m[1] !== t['✅']) bad.push('✅ ' + m[1] + '≠' + t['✅']);
  if (+m[2] !== t['🔄']) bad.push('🔄 ' + m[2] + '≠' + t['🔄']);
  if (+m[3] !== t['🟡']) bad.push('🟡 ' + m[3] + '≠' + t['🟡']);
  if (+m[4] !== t['🆕']) bad.push('🆕 ' + m[4] + '≠' + t['🆕']);
  say('  ' + k.padEnd(5) + ' L' + (i + 1) + ' ' + (bad.length ? '✗ ' + bad.join('; ') : '✓'));
}
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t4_verify.txt', out.join('\n'), 'utf8');
console.log(out.join('\n'));
