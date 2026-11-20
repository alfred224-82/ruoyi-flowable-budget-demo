/**
 * Task 1~3 终局校验（修正上一版两处误报逻辑）：
 *  · 阶段标题只认无缩进且带冒号的 "-- 阶段X："，排除缩进正文注释
 *  · M8_01 残留判定排除合法的 M8_01_sys_tenant_store_profile.sql
 * 检查项：阶段号连续无重号 / M8_01+M8_02 一号双脚本已拆 / M15_12 先于 M10_01 /
 *         M13_01 先于 M13_02 / 无悬空引用 / 无孤儿文件 / 条目编号无重号
 */
const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const out = [];
const say = s => { out.push(s); console.log(s); };
const CN = ['一','二','三','四','五','六','七','八','九','十','十一','十二','十三','十四','十五','十六','十七','十八','十九','二十'];
const cnIdx = s => { const i = CN.indexOf(s); return i < 0 ? null : i + 1; };
let bad = 0;
const BAD = m => { bad++; say('  ✗ ' + m); };
const OK = m => say('  ✓ ' + m);

['系统管理', '专业版', '高级版', '基础版', '小微版'].forEach(g => {
  const dir = path.join(ROOT, g, 'sql');
  const f = path.join(dir, '00_执行顺序.sql');
  say('===================== ' + g + ' =====================');
  if (!fs.existsSync(f)) { BAD('索引不存在'); return; }
  const lines = fs.readFileSync(f, 'utf8').split(/\r?\n/);
  const files = fs.readdirSync(dir);

  // 1) 阶段号（真标题）
  const stages = [];
  lines.forEach((l, i) => {
    const m = l.match(/^-- 阶段([^：:]+)[：:]/);
    if (m) stages.push({ no: m[1].trim(), num: cnIdx(m[1].trim()), line: i + 1 });
  });
  const seqTxt = stages.map(s => s.no).join(' → ');
  const dupS = stages.filter((s, i, a) => s.num !== null && a.findIndex(x => x.num === s.num) !== i);
  if (dupS.length) BAD('阶段重号: ' + dupS.map(d => d.no + '@L' + d.line).join(','));
  const nums = stages.map(s => s.num).filter(n => n !== null);
  let jump = null;
  for (let i = 1; i < nums.length; i++) if (nums[i] !== nums[i - 1] + 1) { jump = nums[i - 1] + '→' + nums[i]; break; }
  if (jump) BAD('阶段跳号: ' + jump);
  if (!dupS.length && !jump) OK('阶段号连续无重号 (' + stages.length + ' 段): ' + seqTxt);

  // 2) M8 一号双脚本拆号
  const s81 = lines.filter(l => /^\s*source\s+M8_01_/i.test(l)).map(l => l.trim());
  const s82 = lines.filter(l => /^\s*source\s+M8_02_/i.test(l)).map(l => l.trim());
  const bad81 = s81.filter(l => !/M8_01_sys_tenant_store_profile\.sql/.test(l));
  if (bad81.length) BAD('M8_01 仍指向 portal_store: ' + bad81.join(','));
  else if (s81.length && s82.length) OK('M8 拆号完成: ' + s81[0].replace('source ', '').replace(';', '') + ' + ' + s82[0].replace('source ', '').replace(';', ''));
  else if (!s81.length && !s82.length) OK('本包不含 M8 脚本');
  else BAD('M8 拆号不完整: M8_01=' + s81.length + ' M8_02=' + s82.length);
  // 注释中的旧名残留
  const staleCmt = lines.filter((l, i) => /^\s*--/.test(l) && /M8_01_portal_store/.test(l));
  if (staleCmt.length) BAD('注释仍含旧名 M8_01_portal_store: ' + staleCmt.map(l => l.trim().slice(0, 60)).join(' | '));

  // 3) 顺序强约束
  const pos = re => { const i = lines.findIndex(l => re.test(l)); return i < 0 ? null : i + 1; };
  const p1512 = pos(/^\s*source\s+M15_12_fund_platform_menu_edition\.sql/i);
  const p1001 = pos(/^\s*source\s+M10_01_biz_bundle_feature_flag_menu\.sql/i);
  const p1301 = pos(/^\s*source\s+M13_01_access_channel\.sql/i);
  const p1302 = pos(/^\s*source\s+M13_02_wecom_menu\.sql/i);
  if (p1001 === null) BAD('缺 M10_01 source 行');
  else if (p1512 !== null && p1512 < p1001) OK('M15_12(L' + p1512 + ') 先于 M10_01(L' + p1001 + ')，1300~1359 区段安全');
  else if (p1512 !== null) BAD('顺序颠倒 M10_01(L' + p1001 + ') 在 M15_12(L' + p1512 + ') 之前');
  else OK('M10_01(L' + p1001 + ') 已引用；本包无 M15_12（1300 目录由他包建立）');

  const has1302File = files.includes('M13_02_wecom_menu.sql');
  if (has1302File && p1302 === null) BAD('M13_02 孤儿：文件在包内但索引无 source 行');
  else if (has1302File && p1301 !== null && p1301 < p1302) OK('M13_01(L' + p1301 + ') 先于 M13_02(L' + p1302 + ')，企微菜单 3490~3492 可建');
  else if (has1302File) OK('M13_02(L' + p1302 + ') 已引用');
  else if (p1302 !== null) BAD('索引引用 M13_02 但文件不存在');
  else OK('本包不含 M13_02（企微为高级版+，符合版本边界）');

  // 4) 悬空引用 / 孤儿文件
  const referenced = new Set();
  lines.forEach(l => { const m = l.match(/^\s*source\s+([^;\s]+)\s*;/i); if (m) referenced.add(m[1]); });
  const fset = new Set(files);
  const miss = [...referenced].filter(x => !fset.has(x));
  const orphan = files.filter(x => /\.sql$/i.test(x) && x !== '00_执行顺序.sql' && !referenced.has(x));
  if (miss.length) BAD('悬空引用(' + miss.length + '): ' + miss.join(' | '));
  else OK('无悬空引用（source ' + referenced.size + ' 条）');
  if (orphan.length) BAD('孤儿文件(' + orphan.length + '): ' + orphan.join(' | '));
  else OK('无孤儿文件（目录 ' + files.length + ' 个文件全部被引用）');

  // 5) 条目编号重号（仅纯数字 [NN] 形式）
  const itemNos = {};
  lines.forEach((l, i) => { const m = l.match(/^--\s*\[(\d+)\]/); if (m) { (itemNos[m[1]] = itemNos[m[1]] || []).push(i + 1); } });
  const dupN = Object.keys(itemNos).filter(k => itemNos[k].length > 1);
  if (dupN.length) BAD('条目编号重号: ' + dupN.map(k => '[' + k + ']@L' + itemNos[k].join('/L')).join(', '));
  else OK('条目编号无重号（共 ' + Object.keys(itemNos).length + ' 个 [NN] 条目）');
});

say('');
say(bad === 0 ? '★★★ 全部通过，0 项异常 ★★★' : '★★★ 共 ' + bad + ' 项异常，需修复 ★★★');
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_verify_final_idx.txt', out.join('\n'), 'utf8');
