/** Task 5/6 取证探针：母文档组 A~E 定义、高级版 sql 数、T7.2 小程序赛道 */
const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/';
const out = [];
const say = s => out.push(s);

// ---------- A. 母文档中「组 A/B/C/D/E」出现处 ----------
const MA = R + 'docs/最新整理好的需求/资金管控系统开发任务文档.md';
const ma = fs.readFileSync(MA, 'utf8').split(/\r?\n/);
say('=== A. 母文档 组A~E 提及行 ===');
ma.forEach((l, i) => {
  if (/组\s*[ABCDE]/.test(l)) say('  L' + (i + 1) + ' | ' + l.trim().slice(0, 300));
});

// ---------- B. 母文档中全部 FEAT_ 开关去重清单 ----------
say('');
say('=== B. 母文档出现的 FEAT_* 去重清单 ===');
const feats = new Set();
ma.forEach(l => (l.match(/FEAT_[A-Z0-9_]+/g) || []).forEach(f => feats.add(f)));
const arr = [...feats].sort();
say('  共 ' + arr.length + ' 项');
arr.forEach(f => say('    ' + f));

// ---------- C. 各分册 sql 脚本数 ----------
say('');
say('=== C. 各分册 sql 脚本数（含 00_执行顺序.sql）===');
['系统管理', '专业版', '高级版', '基础版', '小微版'].forEach(d => {
  const p = R + 'docs/最新整理好的需求/' + d + '/sql';
  try {
    const all = fs.readdirSync(p).filter(f => f.endsWith('.sql'));
    say('  ' + d.padEnd(6) + ' 总 ' + String(all.length).padStart(3) + ' | 索引 ' + (all.includes('00_执行顺序.sql') ? '有' : '无') + ' | 业务 ' + (all.length - (all.includes('00_执行顺序.sql') ? 1 : 0)));
  } catch (e) { say('  ' + d + ' 无 sql 目录'); }
});

// ---------- D. T7.2 人脸比对的小程序/PC 赛道 ----------
say('');
say('=== D. T7.2 人脸比对 前端赛道实测 ===');
function walk(dir, acc, ext) {
  let es;
  try { es = fs.readdirSync(dir, { withFileTypes: true }); } catch (e) { return acc; }
  for (const e of es) {
    const fp = dir + '/' + e.name;
    if (e.isDirectory()) { if (e.name !== 'node_modules' && e.name !== 'unpackage' && e.name !== 'dist') walk(fp, acc, ext); }
    else if (ext.some(x => e.name.endsWith(x))) acc.push(fp);
  }
  return acc;
}
const appFiles = walk(R + 'ruoyi-app', [], ['.vue', '.js']);
const uiFiles = walk(R + 'ruoyi-ui/src', [], ['.vue', '.js']);
say('  ruoyi-app 文件数 ' + appFiles.length + ' | ruoyi-ui/src 文件数 ' + uiFiles.length);
[['ruoyi-app', appFiles], ['ruoyi-ui/src', uiFiles]].forEach(([tag, files]) => {
  const hits = [];
  files.forEach(f => {
    const c = fs.readFileSync(f, 'utf8');
    const lines = c.split(/\r?\n/);
    lines.forEach((l, i) => {
      if (/faceVerify|FaceVerify|人脸|faceCompare|face_verify/i.test(l)) hits.push(f.replace(R, '') + ':' + (i + 1) + ' | ' + l.trim().slice(0, 120));
    });
  });
  say('  --- ' + tag + ' 命中 ' + hits.length + ' 处 ---');
  hits.slice(0, 40).forEach(h => say('    ' + h));
});

// ---------- E. 后端 FaceVerify 全栈 ----------
say('');
say('=== E. 后端 FaceVerify 类清单 ===');
const beFiles = walk(R + 'ruoyi-admin/src', [], ['.java']).concat(walk(R + 'ruoyi-system/src', [], ['.java']), walk(R + 'ruoyi-common/src', [], ['.java']));
beFiles.filter(f => /[Ff]ace/.test(f)).forEach(f => say('  ' + f.replace(R, '') + ' (' + fs.readFileSync(f, 'utf8').split(/\r?\n/).length + ' 行)'));

fs.writeFileSync(R + 'logs/_t56_probe.txt', out.join('\n'), 'utf8');
console.log('lines=' + out.length);
