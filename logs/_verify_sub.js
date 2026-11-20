/** 五分册 + 母文档统一校验：note 头四态计数 ↔ 表格标记实测 ↔ 版本号一致性 ↔ stale 引用 */
const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/';
const DOCS = [
  { k: '母文档', p: R + '资金管控系统开发任务文档.md' },
  { k: '系统管理', p: R + '系统管理/资金管控系统开发任务文档（系统管理）.md' },
  { k: '基础版', p: R + '基础版/资金管控系统开发任务文档（基础版）.md' },
  { k: '高级版', p: R + '高级版/资金管控系统开发任务文档（高级版）.md' },
  { k: '专业版', p: R + '专业版/资金管控系统开发任务文档（专业版）.md' },
  { k: '小微版', p: R + '小微版/资金管控系统开发任务文档（小微版）.md' }
];
const out = [];
const say = s => out.push(s);
let BAD = 0;
const E = { OK: '\u2705', RO: '\uD83D\uDD04', YE: '\uD83D\uDFE1', NE: '\uD83C\uDD95' };
const RE_ROW = /^\|\s*(\u2705|\uD83D\uDD04|\uD83D\uDFE1|\uD83C\uDD95)\s*(T[\d.]+)\s*\|/;
const RE_NOTE = /v1\.\d+ 对账结果（\s*✅\s*(\d+)\s*\/\s*🔄\s*(\d+)\s*\/\s*🟡\s*(\d+)\s*\/\s*🆕\s*(\d+)/;

DOCS.forEach(d => {
  const a = fs.readFileSync(d.p, 'utf8').split(/\r?\n/);
  say('');
  say('################ ' + d.k + '  (' + a.length + ' 行) ################');

  // --- 里程碑区间 ---
  const ms = [];
  a.forEach((l, i) => { const m = l.match(/^### M(\d+(?:\.\d+)?)\s/); if (m) ms.push({ name: 'M' + m[1], s: i }); });
  // 章节终点：第一个 ## 三、 或 ## 之后的任意二级标题
  let end = a.length;
  for (let i = (ms[0] ? ms[0].s : 0); i < a.length; i++) { if (/^## [三四五六七八九十]/.test(a[i])) { end = i; break; } }
  ms.forEach((o, k) => { o.e = k + 1 < ms.length ? ms[k + 1].s : end; });

  say('--- 逐里程碑：表格实测 vs note 头 ---');
  const tot = { OK: 0, RO: 0, YE: 0, NE: 0 };
  const roList = [];
  ms.forEach(o => {
    const c = { OK: 0, RO: 0, YE: 0, NE: 0 };
    for (let i = o.s; i < o.e; i++) {
      const m = a[i].match(RE_ROW);
      if (!m) continue;
      const key = m[1] === E.OK ? 'OK' : m[1] === E.RO ? 'RO' : m[1] === E.YE ? 'YE' : 'NE';
      c[key]++;
      if (key === 'RO') roList.push(m[2]);
    }
    Object.keys(tot).forEach(k => { tot[k] += c[k]; });
    let note = null, nl = 0;
    for (let i = o.s; i < o.e; i++) { const m = a[i].match(RE_NOTE); if (m) { note = m.slice(1).map(Number); nl = i + 1; } }
    const n = c.OK + c.RO + c.YE + c.NE;
    if (!note) { say('INFO ' + o.name.padEnd(6) + ' total=' + n + ' [' + c.OK + '/' + c.RO + '/' + c.YE + '/' + c.NE + ']  无标准 note 头'); return; }
    const ok = note[0] === c.OK && note[1] === c.RO && note[2] === c.YE && note[3] === c.NE;
    if (!ok) BAD++;
    say((ok ? 'OK  ' : 'BAD ') + o.name.padEnd(6) + ' total=' + n + ' 表[' + c.OK + '/' + c.RO + '/' + c.YE + '/' + c.NE + '] note(L' + nl + ')[' + note.join('/') + ']');
  });
  const S = tot.OK + tot.RO + tot.YE + tot.NE;
  say('合计 ' + S + ' 项 = ✅' + tot.OK + ' / 🔄' + tot.RO + ' / 🟡' + tot.YE + ' / 🆕' + tot.NE);
  say('🔄 清单: ' + (roList.length ? roList.join(', ') : '(无)'));

  // --- 版本号一致性 ---
  const hv = (a[2] || '').match(/v1\.\d+/);
  const fv = a.filter(l => /^\*\*文档版本\*\*/.test(l));
  const fvm = fv.length ? fv[fv.length - 1].match(/v1\.\d+/) : null;
  const vok = hv && fvm && hv[0] === fvm[0];
  if (!vok) BAD++;
  say((vok ? 'OK  ' : 'BAD ') + '版本号 头(L3)=' + (hv ? hv[0] : '缺') + ' 文末=' + (fvm ? fvm[0] : '缺'));

  // --- stale 母文档版本引用 ---
  const stale = [];
  a.forEach((l, i) => {
    if (!hv) return;
    const re = /母文档》?\s*v?1?\.*|《资金管控系统开发任务文档》\s*\**\s*(v1\.\d+)/g;
    let m; while ((m = re.exec(l)) !== null) { if (m[1] && m[1] !== hv[0]) stale.push('L' + (i + 1) + ':' + m[1]); }
  });
  if (stale.length) { BAD++; say('BAD  stale 母文档版本引用: ' + stale.join(', ')); }
  else say('OK   无 stale 母文档版本引用');

  // --- 已知事实错误关键词 ---
  const WRONG = [
    ['FEAT_INVOICE_VERIFY 无专属 Controller', /FEAT_INVOICE_VERIFY`? ?无专属 ?Controller/],
    ['当前未标注', /★当前未标注/],
    ['ContentSecurityStub', /ContentSecurityStub/],
    ['MessageStub', /\w*MessageStub/],
    ['SysDataScopeServiceImpl 未引用', /SysDataScopeServiceImpl`? ?未引用/],
    ['ruoyi-app 无 micro 页面', /(小程序端.{0,12}零 ?micro|无任何 ?micro ?页面)/],
    ['四 Tab 静态渲染', /四 ?Tab ?静态渲染/],
    ['后续无需全系统扫描', /后续无需全系统扫描/],
    ['第十五章', /第十五章/]
  ];
  WRONG.forEach(w => {
    const hits = [];
    a.forEach((l, i) => { if (w[1].test(l) && !/(不成立|事实错误|作废|予以更正|予以删除|悬空指针|误述|有误|不存在|系误判|系虚构|特此更正|已纠正|已作废|v1\.\d+.*(?:不存在|移出|仍为|残留|误判|更正))/.test(l)) hits.push('L' + (i + 1)); });
    if (hits.length) { BAD++; say('BAD  仍含「' + w[0] + '」: ' + hits.join(',')); }
  });
  say('OK   事实错误关键词扫描完成');
});

say('');
say('================ TOTAL BAD = ' + BAD + ' ================');
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_sub_verify.txt', out.join('\n'), 'utf8');
console.log('done BAD=' + BAD);
