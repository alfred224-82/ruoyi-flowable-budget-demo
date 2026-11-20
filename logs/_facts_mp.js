/**
 * Task 4 取证 · MP 赛道（ruoyi-app）实况 + M16 推广分销是否开工
 * 纠正上一轮 bug：glob 'micro/*.vue' 匹配不到 micro/<sub>/<page>.vue，导致误判「零 micro 页面」
 */
const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X';
const APP = ROOT + '/ruoyi-app';
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

// ---------- A. pages.json 注册实况 ----------
say('===== A) pages.json 注册实况 =====');
const pj = fs.readFileSync(path.join(APP, 'pages.json'), 'utf8');
const paths = [...pj.matchAll(/"path"\s*:\s*"([^"]+)"/g)].map(m => m[1]);
say('  pages 注册总数 = ' + paths.length);
const microPaths = paths.filter(p => p.startsWith('pages/micro/'));
say('  其中 pages/micro/ = ' + microPaths.length);
microPaths.forEach(p => say('      ' + p));
const subPackages = [...pj.matchAll(/"root"\s*:\s*"([^"]+)"/g)].map(m => m[1]);
say('  subPackages root = ' + (subPackages.length ? subPackages.join(', ') : '(无)'));
const tabBars = [...pj.matchAll(/"pagePath"\s*:\s*"([^"]+)"/g)].map(m => m[1]);
say('  tabBar pagePath = ' + (tabBars.length ? tabBars.join(', ') : '(无/条件编译)'));
['MICRO_TAB_CONFIG', 'isMicroVersion', 'microRoles', 'conditional'].forEach(k => {
  say('  pages.json 含 "' + k + '" = ' + (pj.includes(k)));
});

// ---------- B. 落盘 .vue 与注册的一致性 ----------
say('');
say('===== B) 落盘 vs 注册 差集 =====');
const disk = walk(APP + '/pages', (p, n) => /\.vue$/.test(n))
  .map(p => 'pages/' + p.replace(/\\/g, '/').split('/pages/')[1].replace(/\.vue$/, ''));
const setReg = new Set(paths);
const unreg = disk.filter(d => !setReg.has(d));
const phantom = paths.filter(p => !disk.includes(p));
say('  落盘 .vue = ' + disk.length + ' / 注册 = ' + paths.length);
say('  落盘但未注册 (' + unreg.length + ') = ' + (unreg.join(', ') || '无'));
say('  注册但未落盘 (' + phantom.length + ') = ' + (phantom.join(', ') || '无'));

// ---------- C. 18 个 micro 页面实质性 ----------
say('');
say('===== C) micro 页面实质性（行数/UI元素/API调用/生命周期/TODO）=====');
const cfg = fs.readFileSync(path.join(APP, 'config/index.js'), 'utf8');
const apiNames = [...new Set((cfg.match(/^\s*([A-Za-z0-9_]+)\s*:\s*['"`]\/micro\//gm) || [])
  .map(s => s.split(':')[0].trim()))];
say('  config/index.js 中 /micro/* API 键名 = ' + apiNames.length);
const UI = ['<view', '<text', '<button', '<input', '<image', '<scroll-view', '<swiper', '<textarea', '<picker'];
let subst = 0, light = 0;
walk(APP + '/pages/micro', (p, n) => /\.vue$/.test(n)).sort().forEach(p => {
  const t = fs.readFileSync(p, 'utf8');
  const lines = t.split(/\r?\n/).length;
  const ui = UI.reduce((s, k) => s + (t.split(k).length - 1), 0);
  const called = apiNames.filter(a => new RegExp('\\b' + a + '\\s*\\(').test(t));
  const life = (t.match(/\b(onLoad|onShow|onReady|onPullDownRefresh|onReachBottom)\s*\(/g) || []).length;
  const todo = (t.match(/TODO|待实现|占位|假数据/g) || []).length;
  const ok = ui >= 10 && (called.length > 0 || life > 0);
  ok ? subst++ : light++;
  say('  ' + (ok ? '✓' : '?') + ' ' + p.replace(/\\/g, '/').split('/pages/')[1].padEnd(36)
    + ' 行' + String(lines).padStart(4) + ' UI' + String(ui).padStart(3)
    + ' API' + String(called.length).padStart(2) + ' 生命周期' + life + ' TODO' + todo);
});
say('  实质实现 = ' + subst + ' / 轻量或空壳 = ' + light);

// ---------- D. 基础设施 ----------
say('');
say('===== D) 基础设施（T15.15 判定）=====');
const roles = fs.readFileSync(path.join(APP, 'utils/roles.js'), 'utf8');
['MICRO_TAB_CONFIG', 'isMicroVersion', 'SALESMAN', 'CUSTOMER', 'BOSS'].forEach(k =>
  say('  utils/roles.js 含 ' + k + ' = ' + (roles.includes(k))));
const appvue = fs.readFileSync(path.join(APP, 'App.vue'), 'utf8');
say('  App.vue 含 micro 分流 = ' + (/micro/i.test(appvue)) + ' (命中 ' + (appvue.match(/micro/gi) || []).length + ' 次)');
const login = fs.readFileSync(path.join(APP, 'pages/login/index.vue'), 'utf8');
say('  pages/login/index.vue 含 micro 重定向 = ' + (/micro/i.test(login)) + ' (命中 ' + (login.match(/micro/gi) || []).length + ' 次)');
const fmt = fs.readFileSync(path.join(APP, 'utils/format.js'), 'utf8');
['BOOKING_STATUS', 'PAY_STATUS'].forEach(k => say('  utils/format.js 含 ' + k + ' = ' + fmt.includes(k)));

// ---------- E. M16 推广分销是否开工 ----------
say('');
say('===== E) M16 推广分销（T16.1~T16.12）=====');
say('  ruoyi-app/pages/promotion 存在 = ' + fs.existsSync(APP + '/pages/promotion'));
if (fs.existsSync(APP + '/pages/promotion')) {
  walk(APP + '/pages/promotion', (p, n) => /\.vue$/.test(n)).forEach(p => say('      ' + p));
}
const javaHits = walk(ROOT, (p, n) => /\.java$/.test(n) && !p.includes('target'))
  .filter(p => /promotion|Promotion/i.test(p));
say('  文件名含 promotion 的 .java = ' + javaHits.length);
javaHits.slice(0, 20).forEach(p => say('      ' + p.replace(/\\/g, '/').split('RuoYi-Flowable-Plus-0.8.X/')[1]));
const uiHits = walk(ROOT + '/ruoyi-ui/src', (p, n) => /\.(vue|js)$/.test(n))
  .filter(p => /promotion/i.test(p));
say('  ruoyi-ui/src 下含 promotion 的文件 = ' + uiHits.length);
uiHits.slice(0, 20).forEach(p => say('      ' + p.replace(/\\/g, '/').split('ruoyi-ui/src/')[1]));

fs.writeFileSync(ROOT + '/logs/_facts_mp.txt', out.join('\n'), 'utf8');
