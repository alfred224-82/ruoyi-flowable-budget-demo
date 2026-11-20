/**
 * Task 4 核心取证：ruoyi-app 19 个页面是「实质实现」还是「空壳」
 *  判定维度（每页）：
 *   · 总行数 / <template> 内 UI 元素数（view/text/button/input/image/scroll-view）
 *   · 从 config/index.js 引入并实际调用的 micro API 名（api.xxx( 调用点计数）
 *   · 是否有 onLoad/onShow/methods 生命周期与业务方法
 *   · 是否有 TODO / 待实现 / 占位 标记
 *  另单独取证 pages/promotion/my-code.vue —— 若为实质实现，则母文档 L518
 *  「M16 全仓无推广码业务代码，前端页面 0 命中」为 stale，M16 的 12 项全 🆕 需重判。
 */
const fs = require('fs');
const path = require('path');
const APP = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/ruoyi-app';
const out = [];
const say = s => { out.push(s); console.log(s); };
function walk(dir, pred, acc) {
  acc = acc || [];
  if (!fs.existsSync(dir)) return acc;
  fs.readdirSync(dir, { withFileTypes: true }).forEach(e => {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, pred, acc); else if (pred(p, e.name)) acc.push(p);
  });
  return acc;
}
const rel = p => p.replace(/\\/g, '/').replace(APP + '/', '');

// config/index.js 导出的 micro API 名清单
const cfg = fs.readFileSync(path.join(APP, 'config/index.js'), 'utf8');
const apiNames = [...new Set((cfg.match(/^\s*([A-Za-z0-9_]+)\s*:\s*['"`]\/micro\//gm) || [])
  .map(s => s.split(':')[0].trim()))];
say('config/index.js 中 /micro/* API 键名 ' + apiNames.length + ' 个: ' + apiNames.join(', '));
say('');

const UI = ['<view', '<text', '<button', '<input', '<image', '<scroll-view', '<swiper', '<textarea', '<picker'];
say('===== 逐页取证 =====');
say('页面 | 行数 | UI元素 | micro API 调用 | 生命周期/方法 | TODO占位');
const vues = walk(APP + '/pages', (p, n) => /\.vue$/.test(n)).sort();
const detail = [];
vues.forEach(p => {
  const t = fs.readFileSync(p, 'utf8');
  const lines = t.split(/\r?\n/).length;
  const ui = UI.reduce((s, k) => s + (t.split(k).length - 1), 0);
  const called = apiNames.filter(a => new RegExp('\\b' + a + '\\s*\\(').test(t));
  const life = (t.match(/\b(onLoad|onShow|onReady|onPullDownRefresh|onReachBottom)\s*\(/g) || []).length;
  const methods = (t.match(/^\s{4,}([a-zA-Z0-9_]+)\s*\([^)]*\)\s*\{/gm) || []).length;
  const todo = (t.match(/TODO|待实现|占位|mock|假数据|示例数据/gi) || []).length;
  say('  ' + rel(p).padEnd(34) + ' | ' + String(lines).padStart(4) + ' | ' + String(ui).padStart(3)
    + ' | ' + (called.length ? called.join(',') : '—') + ' | ' + life + '/' + methods + ' | ' + todo);
  detail.push({ p: rel(p), lines, ui, called, life, methods, todo });
});

say('');
say('===== 汇总判定 =====');
const subst = detail.filter(d => d.ui >= 10 && (d.called.length > 0 || d.methods >= 3));
const shell = detail.filter(d => !subst.includes(d));
say('  实质实现页（UI≥10 且 有 micro API 调用或 ≥3 业务方法）= ' + subst.length + ' / ' + detail.length);
subst.forEach(d => say('      ✓ ' + d.p + '  (UI ' + d.ui + ', API ' + d.called.length + ', 方法 ' + d.methods + ')'));
if (shell.length) { say('  疑似空壳/轻量页 = ' + shell.length); shell.forEach(d => say('      ? ' + d.p + '  (UI ' + d.ui + ', 行 ' + d.lines + ')')); }
const withTodo = detail.filter(d => d.todo > 0);
say('  含 TODO/占位/mock 标记的页 = ' + withTodo.length + (withTodo.length ? '：' + withTodo.map(d => d.p + '(' + d.todo + ')').join(', ') : ''));

say('');
say('===== pages/promotion/my-code.vue 专项（M16 T16.11 判定依据）=====');
const mc = path.join(APP, 'pages/promotion/my-code.vue');
if (fs.existsSync(mc)) {
  const t = fs.readFileSync(mc, 'utf8');
  say('  行数 = ' + t.split(/\r?\n/).length);
  say('  推广相关关键字命中: ');
  [['promotionCode', /promotionCode/g], ['推广码', /推广码/g], ['qrCode', /qrCode|qr_code/g],
   ['归因/attribution', /attribution/g], ['奖励/reward', /reward/g], ['拉新', /拉新/g],
   ['保存到相册', /saveImageToPhotosAlbum|保存/g], ['coupon', /coupon/g]].forEach(([k, re]) => {
    const n = (t.match(re) || []).length;
    say('      ' + k.padEnd(20) + ' = ' + n);
  });
  say('  --- 全文前 60 行 ---');
  t.split(/\r?\n/).slice(0, 60).forEach((l, i) => say('  ' + (i + 1) + '| ' + l));
} else say('  !! 文件不存在');

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_facts_app2.txt', out.join('\n'), 'utf8');
