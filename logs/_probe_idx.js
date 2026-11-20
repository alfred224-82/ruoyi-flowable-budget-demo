/**
 * 侦察：
 *  A) 各索引中所有含 M8_01 / M8_02 的原始行（带行号、原样输出，用于定位改名未命中的写法）
 *  B) 各索引中所有「阶段」标题原始行（带行号）
 *  C) 各索引中含 M13_01 / M13_02 的原始行
 */
const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const out = [];
const say = s => { out.push(s); console.log(s); };

['专业版', '高级版', '基础版', '小微版'].forEach(g => {
  const f = path.join(ROOT, g, 'sql', '00_执行顺序.sql');
  const lines = fs.readFileSync(f, 'utf8').split(/\r?\n/);
  say('===================== ' + g + '  (总行 ' + lines.length + ') =====================');
  say('--- A) M8_01 / M8_02 原始行 ---');
  lines.forEach((l, i) => { if (/M8_0[12]/.test(l)) say('  L' + (i + 1) + '| ' + JSON.stringify(l)); });
  say('--- B) 阶段标题原始行 ---');
  lines.forEach((l, i) => { if (/^\s*--\s*阶段/.test(l)) say('  L' + (i + 1) + '| ' + l.trim()); });
  say('--- C) M13_01 / M13_02 原始行 ---');
  lines.forEach((l, i) => { if (/M13_0[12]/.test(l)) say('  L' + (i + 1) + '| ' + l.trim()); });
  say('');
});

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_probe_idx.txt', out.join('\n'), 'utf8');
