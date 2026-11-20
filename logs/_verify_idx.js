/**
 * 校验 4 个既有索引 + 新建系统管理索引：
 *  1) 各包既有「阶段N」编号序列 -> 检查新插入的「阶段十五」是否重号/跳号
 *  2) M8_01 残留引用（应为 0）、M8_02 引用（应为 1）
 *  3) M15_12 / M10_01 的 source 行相对顺序（M15_12 必须在 M10_01 之前）
 *  4) 每个 source 行指向的文件是否真实存在于该包（悬空引用检测）
 */
const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const out = [];
const say = s => { out.push(s); console.log(s); };

const CN = ['一','二','三','四','五','六','七','八','九','十','十一','十二','十三','十四','十五','十六','十七','十八','十九','二十'];
function cnIdx(s) { const i = CN.indexOf(s); return i < 0 ? null : i + 1; }

['系统管理', '专业版', '高级版', '基础版', '小微版'].forEach(g => {
  const dir = path.join(ROOT, g, 'sql');
  const f = path.join(dir, '00_执行顺序.sql');
  say('===================== ' + g + ' =====================');
  if (!fs.existsSync(f)) { say('  !! 索引不存在'); return; }
  const t = fs.readFileSync(f, 'utf8');
  const lines = t.split(/\r?\n/);

  // 1) 阶段号序列
  const stages = [];
  lines.forEach((l, i) => {
    const m = l.match(/^--\s*阶段([^\s：:（(]+)/);
    if (m) stages.push({ no: m[1], num: cnIdx(m[1]), line: i + 1, txt: l.trim().slice(0, 70) });
  });
  const seq = stages.map(s => s.no + '(' + s.num + ')');
  say('  阶段序列: ' + seq.join(' -> '));
  const dup = stages.filter((s, i, a) => s.num !== null && a.findIndex(x => x.num === s.num) !== i);
  if (dup.length) say('  !! 阶段重号: ' + dup.map(d => d.no + ' @L' + d.line).join(', '));
  // 跳号检测
  const nums = stages.map(s => s.num).filter(n => n !== null);
  for (let i = 1; i < nums.length; i++) if (nums[i] !== nums[i - 1] + 1) say('  !! 阶段跳号: ' + nums[i - 1] + ' -> ' + nums[i] + ' (@L' + stages[i].line + ')');

  // 2) M8_01 / M8_02
  const n81 = lines.filter(l => /^\s*source\s+M8_01/i.test(l)).length;
  const n82 = lines.filter(l => /^\s*source\s+M8_02/i.test(l)).length;
  say('  source M8_01=' + n81 + '  M8_02=' + n82 + (n81 === 0 ? '  OK' : '  !! M8_01 残留'));

  // 3) M15_12 / M10_01 相对顺序
  let p1512 = -1, p1001 = -1;
  lines.forEach((l, i) => {
    if (/^\s*source\s+M15_12_fund_platform_menu_edition\.sql/i.test(l)) { if (p1512 < 0) p1512 = i + 1; }
    if (/^\s*source\s+M10_01_biz_bundle_feature_flag_menu\.sql/i.test(l)) { if (p1001 < 0) p1001 = i + 1; }
  });
  say('  M15_12 @L' + p1512 + '   M10_01 @L' + p1001);
  if (p1001 > 0 && p1512 > 0) say(p1512 < p1001 ? '  OK: M15_12 先于 M10_01' : '  !! 顺序颠倒：M10_01 在 M15_12 之前，菜单会被 DELETE 清除');
  if (p1001 < 0) say('  !! 缺 M10_01 source 行');

  // 4) 悬空引用
  const files = new Set(fs.readdirSync(dir));
  const miss = [];
  let srcCount = 0;
  lines.forEach((l, i) => {
    const m = l.match(/^\s*source\s+([^;\s]+)\s*;/i);
    if (m) { srcCount++; if (!files.has(m[1])) miss.push(m[1] + ' @L' + (i + 1)); }
  });
  say('  source 总数=' + srcCount + '  目录文件数=' + files.size);
  if (miss.length) say('  !! 悬空引用(' + miss.length + '): ' + miss.join(' | '));
  else say('  OK: 无悬空引用');
  // 未被引用的文件（孤儿）
  const referenced = new Set();
  lines.forEach(l => { const m = l.match(/^\s*source\s+([^;\s]+)\s*;/i); if (m) referenced.add(m[1]); });
  const orphan = [...files].filter(x => /\.sql$/i.test(x) && x !== '00_执行顺序.sql' && !referenced.has(x));
  if (orphan.length) say('  ⚠ 孤儿文件(存在但无 source 行)(' + orphan.length + '): ' + orphan.join(' | '));
});

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_verify_idx.txt', out.join('\n'), 'utf8');
