/**
 * 复核 _fix_idx2 的写入结果：
 *  1) 各包「M1_05 前置表述」句子现状（stageOf 去掉「阶段」前缀导致语句不通，需复核）
 *  2) 专业版 / 高级版 M13_01~M13_02 前后 12 行上下文（确认条目编号格式与是否重号）
 *  3) 各包平台底座管理菜单块 8 行上下文（确认阶段号与条目号已同步）
 */
const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const IDX = g => path.join(ROOT, g, 'sql', '00_执行顺序.sql');
const out = [];
const say = s => { out.push(s); console.log(s); };

function ctx(lines, re, before, after, title) {
  const i = lines.findIndex(l => re.test(l));
  say('  --- ' + title + (i < 0 ? '  !! 未命中' : '  @L' + (i + 1)) + ' ---');
  if (i < 0) return;
  for (let k = Math.max(0, i - before); k <= Math.min(lines.length - 1, i + after); k++) {
    say('  ' + (k + 1 === i + 1 ? '>' : ' ') + ' L' + (k + 1) + '| ' + lines[k]);
  }
}

['专业版', '高级版', '基础版', '小微版'].forEach(g => {
  const lines = fs.readFileSync(IDX(g), 'utf8').split(/\r?\n/);
  say('===================== ' + g + ' (总行 ' + lines.length + ') =====================');
  // 1) M1_05 前置表述句
  lines.forEach((l, i) => { if (/前置依赖 M1_05/.test(l)) say('  [M1_05句] L' + (i + 1) + '| ' + l.trim()); });
  // 2) M13_02 上下文
  ctx(lines, /^\s*source\s+M13_02_wecom_menu\.sql/i, 8, 3, 'M13_02 上下文');
  // 3) 平台底座块
  ctx(lines, /^\s*source\s+M10_01_biz_bundle_feature_flag_menu\.sql/i, 12, 2, '平台底座管理菜单块');
  say('');
});

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_probe_idx2.txt', out.join('\n'), 'utf8');
