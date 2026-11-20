const fs = require('fs');
const MASTER = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/sql';
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';

function head(p, n) {
  const L = fs.readFileSync(p, 'utf8').split(/\r?\n/);
  console.log('FILE ' + p + '  totalLines=' + L.length);
  L.slice(0, n).forEach((l, i) => console.log('  ' + (i + 1) + ': ' + l));
  return L;
}

console.log('=========== 1. M13_02_wecom_menu.sql (orphan in adv/pro) ===========');
head(MASTER + '/M13_02_wecom_menu.sql', 24);

console.log('');
console.log('=========== 2. M10_01_biz_bundle_feature_flag_menu.sql ===========');
const m10 = head(MASTER + '/M10_01_biz_bundle_feature_flag_menu.sql', 24);
const ids10 = [...new Set(m10.join('\n').match(/\b1[23]\d\d\b/g) || [])].sort();
console.log('  menu_id-like tokens: ' + ids10.join(', '));

console.log('');
console.log('=========== 3. M15_12_fund_platform_menu_edition.sql menu ids ===========');
const m1512 = fs.readFileSync(ROOT + '/小微版/sql/M15_12_fund_platform_menu_edition.sql', 'utf8');
const ids1512 = [...new Set(m1512.match(/\b1[23]\d\d\b/g) || [])].sort();
console.log('  M15_12 tokens: ' + ids1512.join(', '));
console.log('  OVERLAP with M10_01: ' + ids10.filter(x => ids1512.includes(x)).join(', '));

console.log('');
console.log('=========== 4. master dir: does it contain M5_13 / M15_14? ===========');
const masterFiles = fs.readdirSync(MASTER).filter(x => x.endsWith('.sql')).sort();
console.log('  master count=' + masterFiles.length);
['M5_13', 'M15_14', 'M10_01', 'M13_02'].forEach(k => {
  console.log('  ' + k + ' -> ' + (masterFiles.some(f => f.startsWith(k)) ? 'PRESENT' : '*** MISSING ***'));
});

console.log('');
console.log('=========== 5. MD5 consistency of shared scripts across packages ===========');
const crypto = require('crypto');
const pkgs = ['专业版', '高级版', '基础版', '小微版'];
const md5 = p => crypto.createHash('md5').update(fs.readFileSync(p)).digest('hex');
const all = {};
pkgs.forEach(g => {
  const d = ROOT + '/' + g + '/sql';
  fs.readdirSync(d).filter(x => x.endsWith('.sql') && x !== '00_执行顺序.sql').forEach(f => {
    (all[f] = all[f] || {})[g] = md5(d + '/' + f);
  });
});
// also master
Object.keys(all).forEach(f => { if (fs.existsSync(MASTER + '/' + f)) all[f]['MASTER'] = md5(MASTER + '/' + f); });
let bad = 0;
Object.keys(all).sort().forEach(f => {
  const hs = new Set(Object.values(all[f]));
  if (hs.size > 1) {
    bad++;
    console.log('  !! MD5 DIVERGENT: ' + f + ' -> ' + JSON.stringify(all[f]));
  }
});
console.log('  divergent scripts total = ' + bad + ' / shared names = ' + Object.keys(all).length);

console.log('');
console.log('=========== 6. scripts present in a package but NOT in master ===========');
Object.keys(all).sort().forEach(f => {
  if (!all[f]['MASTER']) console.log('  ' + f + ' exists only in: ' + Object.keys(all[f]).filter(k => k !== 'MASTER').join('/'));
});

console.log('');
console.log('=========== 7. scripts in master but in NO package ===========');
masterFiles.forEach(f => { if (!all[f]) console.log('  ' + f); });
