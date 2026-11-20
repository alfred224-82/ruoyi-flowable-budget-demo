/**
 * 校验：①《系统管理》分册第二章任务表 == 其第三章统计表（仅 7 个系统管理里程碑）
 *       ②《系统管理》分册第三章 18 行 == 母文档第三章 18 行（逐行同源）
 *       ③ note 头声明计数 == 分册任务表
 */
const fs = require('fs');
const MA = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
const SB = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/系统管理/资金管控系统开发任务文档（系统管理）.md';
const out = [];
const say = s => out.push(s);
const STAT_RE = /^\|\s*(M[\d.]+)[^|]*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|/;
const TOT_RE = /^\|\s*\*\*合计\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|/;
const NOTE_RE = /^> \*\*v1\.\d+ 对账结果（✅ (\d+) \/ 🔄 (\d+) \/ 🟡 (\d+) \/ 🆕 (\d+)[^）]*）[^*]*\*\*/;

function parse(file, ch2re, ch3re) {
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
  const s2 = lines.findIndex(l => ch2re.test(l));
  const s3 = lines.findIndex(l => ch3re.test(l));
  const s4 = lines.findIndex((l, i) => i > s3 && /^## 四/.test(l));
  // 第二章任务表
  let cur = null; const tally = {}; const order = []; const notes = [];
  for (let i = s2; i < s3; i++) {
    const l = lines[i];
    const m = l.match(/^### (M[\d.]+)\s/);
    if (m) { cur = m[1]; if (!tally[cur]) { tally[cur] = { '✅': 0, '🔄': 0, '🟡': 0, '🆕': 0, P0: 0, P1: 0, P2: 0 }; order.push(cur); } continue; }
    const nm = l.match(NOTE_RE);
    if (nm && cur) notes.push({ k: cur, ln: i + 1, ok: +nm[1], re: +nm[2], part: +nm[3], nw: +nm[4] });
    if (!cur) continue;
    const r = l.match(/^\|\s*(✅|🔄|🟡|🆕)\s*(T[\d.]+)\s*\|/);
    if (r) {
      tally[cur][r[1]]++;
      const p = l.match(/\|\s*(P[012])\s*\|\s*$/);
      if (p) tally[cur][p[1]]++;
      else say('  !! ' + r[2] + ' 行尾无优先级');
    }
  }
  // 第三章统计表
  const stat = {}; const rows = []; let total = null;
  for (let i = s3; i < (s4 < 0 ? lines.length : s4); i++) {
    const l = lines[i];
    const m = l.match(STAT_RE);
    if (m) { stat[m[1]] = { n: +m[2], ok: +m[3], re: +m[4], part: +m[5], nw: +m[6], p0: +m[7], p1: +m[8], p2: +m[9] }; rows.push(l.trim()); continue; }
    const t = l.match(TOT_RE);
    if (t) { total = { n: +t[1], ok: +t[2], re: +t[3], part: +t[4], nw: +t[5], p0: +t[6], p1: +t[7], p2: +t[8] }; rows.push(l.trim()); }
  }
  return { tally, order, stat, rows, total, notes };
}

const ma = parse(MA, /^## 二/, /^## 三/);
const sb = parse(SB, /^## 二/, /^## 三/);

say('===== ①《系统管理》分册第二章任务表（' + sb.order.length + ' 个里程碑）=====');
sb.order.forEach(k => {
  const t = sb.tally[k];
  const n = t['✅'] + t['🔄'] + t['🟡'] + t['🆕'];
  say('  ' + k.padEnd(5) + ' n=' + String(n).padStart(3) + '  ok' + t['✅'] + ' re' + t['🔄'] + ' yl' + t['🟡'] + ' nw' + t['🆕'] + '  | P0=' + t.P0 + ' P1=' + t.P1 + ' P2=' + t.P2);
});

say('');
say('===== ② 分册第二章 vs 分册第三章（7 个系统管理里程碑）=====');
let bad = 0;
sb.order.forEach(k => {
  const t = sb.tally[k], s = sb.stat[k];
  if (!s) { say('  ' + k.padEnd(5) + ' ✗ 第三章无此行'); bad++; return; }
  const n2 = t['✅'] + t['🔄'] + t['🟡'] + t['🆕'];
  const d = [];
  if (s.n !== n2) d.push('任务数 ' + s.n + '≠' + n2);
  if (s.ok !== t['✅']) d.push('ok ' + s.ok + '≠' + t['✅']);
  if (s.re !== t['🔄']) d.push('re ' + s.re + '≠' + t['🔄']);
  if (s.part !== t['🟡']) d.push('yl ' + s.part + '≠' + t['🟡']);
  if (s.nw !== t['🆕']) d.push('nw ' + s.nw + '≠' + t['🆕']);
  if (s.p0 !== t.P0) d.push('P0 ' + s.p0 + '≠' + t.P0);
  if (s.p1 !== t.P1) d.push('P1 ' + s.p1 + '≠' + t.P1);
  if (s.p2 !== t.P2) d.push('P2 ' + s.p2 + '≠' + t.P2);
  if (d.length) bad++;
  say('  ' + k.padEnd(5) + (d.length ? '✗ ' + d.join('; ') : '✓ 一致'));
});

say('');
say('===== ③ 分册第三章 vs 母文档第三章（18 行逐行同源）=====');
if (sb.rows.length !== ma.rows.length) { say('  ✗ 行数 ' + sb.rows.length + ' ≠ ' + ma.rows.length); bad++; }
const max = Math.max(sb.rows.length, ma.rows.length);
for (let i = 0; i < max; i++) {
  const A = ma.rows[i] || '(缺)', B = sb.rows[i] || '(缺)';
  // 分册里程碑名较短，只比对 8 个「纯数字单元格」；不可用 /\d+/g 全文提取——母文档 M4 名为「M4 开关与防串（扩展至 33 项）」，名称内的 33 会造成误报
  const nums = x => x.split('|').map(c => c.trim()).filter(c => /^\*{0,2}\d+\*{0,2}$/.test(c)).map(c => c.replace(/\*/g, '')).join(',');
  const name = x => (x.match(/^\|\s*\*{0,2}(M[\d.]+|合计)/) || [, '?'])[1];
  const ok = nums(A) === nums(B) && name(A) === name(B);
  if (!ok) bad++;
  say('  ' + name(B).padEnd(5) + (ok ? '✓' : '✗ 母[' + nums(A) + '] vs 分[' + nums(B) + ']'));
}

say('');
say('===== ④ 分册 note 头声明计数 vs 分册任务表 =====');
sb.notes.forEach(nt => {
  const t = sb.tally[nt.k];
  const d = [];
  if (nt.ok !== t['✅']) d.push('ok ' + nt.ok + '≠' + t['✅']);
  if (nt.re !== t['🔄']) d.push('re ' + nt.re + '≠' + t['🔄']);
  if (nt.part !== t['🟡']) d.push('yl ' + nt.part + '≠' + t['🟡']);
  if (nt.nw !== t['🆕']) d.push('nw ' + nt.nw + '≠' + t['🆕']);
  if (d.length) bad++;
  say('  ' + nt.k.padEnd(5) + ' L' + nt.ln + ' ' + (d.length ? '✗ ' + d.join('; ') : '✓'));
});
say('  note 覆盖 ' + sb.notes.length + ' / ' + sb.order.length + ' 个里程碑' + (sb.notes.length === sb.order.length ? ' ✓' : ' ✗ 有遗漏'));
if (sb.notes.length !== sb.order.length) bad++;

say('');
say('===== ⑤ 分册合计行自洽 =====');
const T = sb.total;
say('  四态: ' + T.ok + '+' + T.re + '+' + T.part + '+' + T.nw + ' = ' + (T.ok + T.re + T.part + T.nw) + ' vs n=' + T.n + ' ' + ((T.ok + T.re + T.part + T.nw) === T.n ? '✓' : '✗'));
say('  优先级: ' + T.p0 + '+' + T.p1 + '+' + T.p2 + ' = ' + (T.p0 + T.p1 + T.p2) + ' vs n=' + T.n + ' ' + ((T.p0 + T.p1 + T.p2) === T.n ? '✓' : '✗'));
if ((T.ok + T.re + T.part + T.nw) !== T.n) bad++;
if ((T.p0 + T.p1 + T.p2) !== T.n) bad++;

say('');
say('########## TOTAL BAD = ' + bad + ' ##########');
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t4f_verify.txt', out.join('\n'), 'utf8');
