/**
 * Task 4 前置事实核验（状态判定必须基于代码证据，不沿用文档旧结论）
 *  F1) ruoyi-ui/src/views/system/ 下是否存在 bizBundle / featureFlag / edition 页面（T10.2 / T10.3 / T15.29 的 PC 赛道）
 *  F2) ruoyi-ui/src/api/system/ 下对应 api 文件
 *  F3) ruoyi-app/pages 全量 .vue 计数 + micro 子目录计数 + pages.json 注册数（M8 / M15 的 MP 赛道）
 *  F4) FEAT_SUBJECT_3LAYER（T5.16）与 FEAT_INVOICE_VERIFY（T5.18）的 @RequireFeature 标注实况
 *  F5) ruoyi-ui/src/views/micro 页面计数（M15 PC 赛道）
 *  F6) micro/* Controller 计数（M15 BE 赛道）
 */
const fs = require('fs');
const path = require('path');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X';
const out = [];
const say = s => { out.push(s); console.log(s); };

function walk(dir, pred, acc) {
  acc = acc || [];
  if (!fs.existsSync(dir)) return acc;
  fs.readdirSync(dir, { withFileTypes: true }).forEach(e => {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, pred, acc);
    else if (pred(p, e.name)) acc.push(p);
  });
  return acc;
}
const rel = p => p.replace(/\\/g, '/').replace(R + '/', '');

// ---- F1 / F2
say('===== F1) ruoyi-ui/src/views/system 子目录 =====');
const VSYS = R + '/ruoyi-ui/src/views/system';
if (fs.existsSync(VSYS)) fs.readdirSync(VSYS, { withFileTypes: true }).forEach(e => {
  if (e.isDirectory()) {
    const vs = fs.readdirSync(path.join(VSYS, e.name)).filter(f => /\.vue$/.test(f));
    say('  ' + e.name + '/  (' + vs.length + ' vue) ' + vs.join(','));
  }
});
else say('  !! 目录不存在');
['bizBundle', 'featureFlag', 'edition'].forEach(k => {
  const d = path.join(VSYS, k);
  say('  ★ views/system/' + k + ' : ' + (fs.existsSync(d) ? '存在 -> ' + fs.readdirSync(d).join(',') : '★不存在'));
});

say('');
say('===== F2) ruoyi-ui/src/api/system 相关文件 =====');
const ASYS = R + '/ruoyi-ui/src/api/system';
if (fs.existsSync(ASYS)) {
  const hits = fs.readdirSync(ASYS).filter(f => /bizBundle|featureFlag|edition/i.test(f));
  say('  命中: ' + (hits.length ? hits.join(', ') : '★无'));
} else say('  !! 目录不存在');

// ---- F3
say('');
say('===== F3) ruoyi-app 小程序页面实况（MP 赛道）=====');
const APP = R + '/ruoyi-app';
const appVues = walk(APP + '/pages', (p, n) => /\.vue$/.test(n));
say('  pages 全量 .vue = ' + appVues.length);
const microVues = appVues.filter(p => /[\\/]pages[\\/]micro[\\/]/i.test(p));
say('  pages/micro/*.vue = ' + microVues.length);
microVues.forEach(p => say('      ' + rel(p)));
const others = {};
appVues.forEach(p => { const m = p.match(/[\\/]pages[\\/]([^\\/]+)[\\/]/i); if (m) others[m[1]] = (others[m[1]] || 0) + 1; });
say('  按一级目录分布: ' + Object.keys(others).map(k => k + '=' + others[k]).join(', '));
const pj = path.join(APP, 'pages.json');
if (fs.existsSync(pj)) {
  const t = fs.readFileSync(pj, 'utf8');
  const paths = t.match(/"path"\s*:\s*"([^"]+)"/g) || [];
  const microReg = paths.filter(x => /pages\/micro\//.test(x));
  say('  pages.json 注册总数 = ' + paths.length + '，其中 pages/micro/ = ' + microReg.length);
  microReg.forEach(x => say('      ' + x.replace(/"path"\s*:\s*"/, '').replace(/"$/, '')));
} else say('  !! pages.json 不存在');

// ---- F4
say('');
say('===== F4) @RequireFeature 标注实况（T5.16 / T5.18）=====');
const javas = walk(R, (p, n) => /\.java$/.test(n) && !/[\\/]target[\\/]/.test(p));
say('  扫描 .java 文件数 = ' + javas.length + '（已排除 target）');
['FEAT_SUBJECT_3LAYER', 'FEAT_INVOICE_VERIFY', 'FEAT_EXPENSE_CONTROL', 'FEAT_BUDGET_PREPARE',
 'FEAT_RULE_ENGINE_BASE', 'FEAT_BAD_DEBT', 'FEAT_HIDDEN_COST', 'FEAT_BOSS_DASHBOARD',
 'FEAT_VOUCHER_EXPORT', 'FEAT_LINK_RULE', 'FEAT_INVOICE_SUPPLEMENT'].forEach(code => {
  const hits = [];
  javas.forEach(p => {
    const t = fs.readFileSync(p, 'utf8');
    if (t.includes('@RequireFeature') && t.includes(code)) hits.push(path.basename(p, '.java'));
  });
  say('  ' + code.padEnd(26) + ' -> ' + (hits.length ? hits.length + ' 个类: ' + hits.join(', ') : '★0 命中'));
});

// ---- F5 / F6
say('');
say('===== F5) ruoyi-ui/src/views/micro（M15 PC 赛道）=====');
const vm = walk(R + '/ruoyi-ui/src/views/micro', (p, n) => /\.vue$/.test(n));
say('  views/micro/*.vue = ' + vm.length);
say('===== F6) micro Controller（M15 BE 赛道）=====');
const mc = javas.filter(p => /[\\/]micro[\\/]/.test(p) && /Controller\.java$/.test(p));
say('  micro/*Controller.java = ' + mc.length);
say('  全仓 Controller 总数 = ' + javas.filter(p => /Controller\.java$/.test(p)).length);

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_facts_t4.txt', out.join('\n'), 'utf8');
