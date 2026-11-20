/** v1.21 校验：第二章标记 ↔ 第三章统计表 ↔ note 计数 ↔ 合计算术 */
const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/';
const P = R + 'docs/最新整理好的需求/资金管控系统开发任务文档.md';
const a = fs.readFileSync(P, 'utf8').split(/\r?\n/);
const out = [];
const say = s => out.push(s);

// ---- 1. 定位第二章各里程碑表格区间 ----
const msIdx = [];
a.forEach((l, i) => {
  const m = l.match(/^### M(\d+(?:\.\d+)?)\s/);
  if (m) msIdx.push({ ms: 'M' + m[1], start: i });
});
const ch2End = a.findIndex(l => /^## 三、任务统计/.test(l));
msIdx.forEach((o, k) => { o.end = k + 1 < msIdx.length ? msIdx[k + 1].start : ch2End; });

const MARK = { OK: '\u2705', RO: '\uD83D\uDD04', YE: '\uD83D\uDFE1', NE: '\uD83C\uDD95' };
function count(seg) {
  const c = { OK: 0, RO: 0, YE: 0, NE: 0, tasks: [] };
  seg.forEach(l => {
    const m = l.match(/^\|\s*(\u2705|\uD83D\uDD04|\uD83D\uDFE1|\uD83C\uDD95)\s*(T[\d.]+)\s*\|/);
    if (!m) return;
    const key = m[1] === MARK.OK ? 'OK' : m[1] === MARK.RO ? 'RO' : m[1] === MARK.YE ? 'YE' : 'NE';
    c[key]++;
    c.tasks.push(m[2] + '=' + m[1]);
  });
  return c;
}

say('=== 1. 第二章逐里程碑实测计数 ===');
const act = {};
msIdx.forEach(o => {
  const c = count(a.slice(o.start, o.end));
  act[o.ms] = c;
  say(o.ms.padEnd(6) + ' total=' + (c.OK + c.RO + c.YE + c.NE) + '  OK=' + c.OK + ' RO=' + c.RO + ' YE=' + c.YE + ' NE=' + c.NE);
});

// ---- 2. 第三章统计表 ----
const t3 = a.findIndex(l => /^## 三、任务统计/.test(l));
say('');
say('=== 2. 第三章统计表 vs 第二章实测 ===');
let BAD = 0;
const docSum = { total: 0, OK: 0, RO: 0, YE: 0, NE: 0, P0: 0, P1: 0, P2: 0 };
const ORDER = ['M1', 'M2', 'M3', 'M3.5', 'M4', 'M5', 'M6', 'M7', 'M8', 'M9', 'M10', 'M11', 'M12', 'M13', 'M14', 'M15', 'M16'];
ORDER.forEach(ms => {
  const ln = a.findIndex((l, i) => i > t3 && new RegExp('^\\| ' + ms.replace('.', '\\.') + ' ').test(l));
  if (ln < 0) { say('BAD: 统计表缺 ' + ms); BAD++; return; }
  const cells = a[ln].split('|').map(s => s.trim()).filter(s => s !== '');
  // cells: [里程碑, 任务数, OK, RO, YE, NE, P0, P1, P2]
  const d = cells.slice(1).map(Number);
  const c = act[ms] || { OK: 0, RO: 0, YE: 0, NE: 0 };
  const real = c.OK + c.RO + c.YE + c.NE;
  const ok = d[0] === real && d[1] === c.OK && d[2] === c.RO && d[3] === c.YE && d[4] === c.NE;
  if (!ok) BAD++;
  say((ok ? 'OK  ' : 'BAD ') + ms.padEnd(6) + ' 表[' + d.join('/') + '] 实测[' + real + ',' + c.OK + ',' + c.RO + ',' + c.YE + ',' + c.NE + ']');
  d.forEach((v, i) => { const k = ['total', 'OK', 'RO', 'YE', 'NE', 'P0', 'P1', 'P2'][i]; docSum[k] += v; });
});

// ---- 3. 合计行 ----
const sumLn = a.findIndex((l, i) => i > t3 && /^\|\s*\*\*合计\*\*/.test(l));
const sc = a[sumLn].split('|').map(s => s.trim()).filter(s => s !== '').slice(1)
  .map(s => Number(s.replace(/\*/g, '')));
const keys = ['total', 'OK', 'RO', 'YE', 'NE', 'P0', 'P1', 'P2'];
say('');
say('=== 3. 合计行 vs 17 行累加 ===');
keys.forEach((k, i) => {
  const ok = sc[i] === docSum[k];
  if (!ok) BAD++;
  say((ok ? 'OK  ' : 'BAD ') + k.padEnd(6) + ' 合计行=' + sc[i] + ' 累加=' + docSum[k]);
});
say('四态和=' + (sc[1] + sc[2] + sc[3] + sc[4]) + ' 应=' + sc[0] + ' -> ' + ((sc[1] + sc[2] + sc[3] + sc[4]) === sc[0] ? 'OK' : 'BAD'));
if ((sc[1] + sc[2] + sc[3] + sc[4]) !== sc[0]) BAD++;
say('P 级和=' + (sc[5] + sc[6] + sc[7]) + ' 应=' + sc[0] + ' -> ' + ((sc[5] + sc[6] + sc[7]) === sc[0] ? 'OK' : 'BAD'));
if ((sc[5] + sc[6] + sc[7]) !== sc[0]) BAD++;

// ---- 4. note 计数 vs 表格 ----
say('');
say('=== 4. note 头计数 vs 第二章实测 ===');
const reNote = /v1\.\d+ 对账结果（\s*(?:✅|OK)\s*(\d+)\s*\/\s*🔄\s*(\d+)\s*\/\s*🟡\s*(\d+)\s*\/\s*🆕\s*(\d+)/;
msIdx.forEach(o => {
  const seg = a.slice(o.start, o.end);
  let last = null;
  seg.forEach((l, j) => { const m = l.match(reNote); if (m) last = { ln: o.start + j + 1, m }; });
  if (!last) { say('INFO ' + o.ms.padEnd(6) + ' 无对账 note'); return; }
  const c = act[o.ms];
  const n = last.m.slice(1).map(Number);
  const ok = n[0] === c.OK && n[1] === c.RO && n[2] === c.YE && n[3] === c.NE;
  if (!ok) BAD++;
  say((ok ? 'OK  ' : 'BAD ') + o.ms.padEnd(6) + ' note(L' + last.ln + ')[' + n.join('/') + '] 实测[' + c.OK + ',' + c.RO + ',' + c.YE + ',' + c.NE + ']');
});

// ---- 5. 残留 🔄 清单核对 ----
say('');
say('=== 5. 全文 🔄 任务清单 ===');
const roAll = [];
Object.keys(act).forEach(ms => act[ms].tasks.forEach(t => { if (t.endsWith(MARK.RO)) roAll.push(ms + '/' + t.split('=')[0]); }));
say('🔄 共 ' + roAll.length + ' 项: ' + roAll.join(', '));
const yeAll = [];
Object.keys(act).forEach(ms => act[ms].tasks.forEach(t => { if (t.endsWith(MARK.YE)) yeAll.push(t.split('=')[0]); }));
say('🟡 共 ' + yeAll.length + ' 项');
const neAll = [];
Object.keys(act).forEach(ms => act[ms].tasks.forEach(t => { if (t.endsWith(MARK.NE)) neAll.push(t.split('=')[0]); }));
say('🆕 共 ' + neAll.length + ' 项');

// ---- 6. 悬空指针 / 版本号 ----
say('');
say('=== 6. 指针与版本号 ===');
const bad15 = [];
a.forEach((l, i) => { if (l.includes('第十五章') && !l.includes('悬空指针')) bad15.push('L' + (i + 1)); });
say(bad15.length === 0 ? 'OK   无未标注的「第十五章」悬空引用' : 'BAD  悬空引用: ' + bad15.join(','));
if (bad15.length) BAD++;
say('L3  ' + a[2]);
say('L4  ' + a[3]);
const tail = a.filter(l => /^\*\*文档版本\*\*/.test(l));
say('文末 ' + (tail.length ? tail[tail.length - 1].trim() : '缺失'));
if (!tail.length || !tail[tail.length - 1].includes('v1.21')) { say('BAD 文末版本号非 v1.21'); BAD++; }

// ---- 7. 第三章 v1.21 说明存在性 ----
say('');
say('=== 7. v1.21 说明块 ===');
say(a.some(l => l.includes('v1.21 状态分布说明')) ? 'OK   第三章含 v1.21 状态分布说明' : 'BAD 缺 v1.21 状态分布说明');
if (!a.some(l => l.includes('v1.21 状态分布说明'))) BAD++;
say(a.some(l => l.includes('v1.19 / v1.20 / v1.21 变更提示')) ? 'OK   10.1 含 v1.21 提示' : 'BAD 10.1 缺 v1.21 提示');
if (!a.some(l => l.includes('v1.19 / v1.20 / v1.21 变更提示'))) BAD++;
say(a.some(l => l.includes('v1.21 补注（不改上表标记）')) ? 'OK   10.2 含 v1.21 补注' : 'BAD 10.2 缺 v1.21 补注');
if (!a.some(l => l.includes('v1.21 补注（不改上表标记）'))) BAD++;

say('');
say('TOTAL BAD = ' + BAD);
say('lines=' + a.length);
fs.writeFileSync(R + 'logs/_v121_verify.txt', out.join('\n'), 'utf8');
console.log('done BAD=' + BAD);
