/**
 * Task 4 关键事实深挖：ruoyi-app 19 个 .vue 的真实归属（企业版 M8 vs 小微版 M15）
 *  判定依据（小微版专属标记）：
 *   · micro: 前缀 perms / 接口路径（/micro/**）
 *   · MICRO_TAB_CONFIG / isMicroVersion / version_type / micro_lite / micro_pro
 *   · 小微版业务词：预约 booking / 结算 settlement / 爽约 noShow / 推广码 promotionCode / 顾客 customer
 *  同时核验小微版分册 ✅T15.15 声称的框架层交付物是否真实存在：
 *   utils/roles.js(MICRO_TAB_CONFIG + isMicroVersion) / config/index.js(micro API) / utils/format.js
 *   App.vue + login 登录分流 / pages.json 页面注册数
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
    if (e.isDirectory()) walk(p, pred, acc);
    else if (pred(p, e.name)) acc.push(p);
  });
  return acc;
}
const rel = p => p.replace(/\\/g, '/').replace(APP + '/', '');

// 小微版专属标记
const MICRO_MARKS = [
  ['micro:perms', /['"`]micro:[a-zA-Z]+:[a-zA-Z]+/],
  ['/micro/ 接口', /['"`]\/micro\//],
  ['MICRO_TAB_CONFIG', /MICRO_TAB_CONFIG/],
  ['isMicroVersion', /isMicroVersion/],
  ['version_type', /version_type|versionType/],
  ['micro_lite/pro', /micro_lite|micro_pro/],
  ['booking 预约', /booking|noShow|no_show/],
  ['promotionCode', /promotionCode|promotion_code|推广码/],
];

say('===== A) ruoyi-app/pages 全量 19 个 .vue 归属判定 =====');
const vues = walk(APP + '/pages', (p, n) => /\.vue$/.test(n)).sort();
let microHit = 0;
vues.forEach(p => {
  const t = fs.readFileSync(p, 'utf8');
  const hits = MICRO_MARKS.filter(([, re]) => re.test(t)).map(([k]) => k);
  if (hits.length) microHit++;
  say('  ' + rel(p).padEnd(42) + ' ' + (hits.length ? '★小微标记: ' + hits.join(' / ') : '（无小微标记）'));
});
say('  --- 含小微标记的页面数 = ' + microHit + ' / ' + vues.length);

say('');
say('===== B) 框架层交付物核验（小微版分册 ✅T15.15 的证据声称）=====');
const checks = [
  ['utils/roles.js', 'MICRO_TAB_CONFIG', /MICRO_TAB_CONFIG/],
  ['utils/roles.js', 'isMicroVersion', /isMicroVersion/],
  ['config/index.js', 'micro API 路径', /\/micro\//],
  ['utils/format.js', '文件存在', null],
  ['App.vue', '登录分流/micro', /micro|version_type|versionType/i],
  ['pages.json', 'micro 页面注册', /pages\/micro\//],
];
checks.forEach(([f, what, re]) => {
  const p = path.join(APP, f);
  if (!fs.existsSync(p)) { say('  ✗ ' + f + ' 不存在（声称的 ' + what + ' 无从验证）'); return; }
  const t = fs.readFileSync(p, 'utf8');
  if (!re) { say('  ✓ ' + f + ' 存在（' + t.split(/\r?\n/).length + ' 行）'); return; }
  const n = (t.match(new RegExp(re.source, 'g' + (re.flags.includes('i') ? 'i' : ''))) || []).length;
  say((n ? '  ✓ ' : '  ✗ ') + f + ' -> ' + what + '：命中 ' + n + ' 处');
});

say('');
say('===== C) config/index.js 中 micro API 条目明细 =====');
const cfg = path.join(APP, 'config/index.js');
if (fs.existsSync(cfg)) {
  const lines = fs.readFileSync(cfg, 'utf8').split(/\r?\n/);
  const mi = lines.filter(l => /micro/i.test(l));
  say('  含 micro 的行数 = ' + mi.length + ' / 总行 ' + lines.length);
  mi.slice(0, 25).forEach(l => say('    ' + l.trim().slice(0, 110)));
} else say('  !! config/index.js 不存在');

say('');
say('===== D) utils 目录全量文件 =====');
walk(APP + '/utils', (p, n) => true).forEach(p => say('  ' + rel(p)));

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_facts_app.txt', out.join('\n'), 'utf8');
