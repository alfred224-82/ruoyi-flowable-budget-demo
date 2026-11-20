/**
 * Task 2 收尾校验：母目录 vs 5 个分册包的同名脚本 MD5 一致性
 *  规则（项目既定归档策略）：母目录为超集/源数据，分册包是副本；
 *  同一脚本在各处副本内容必须一致（MD5 相同），否则按索引部署会出现版本漂移。
 * 输出：
 *  A) MD5 分叉清单（同名文件内容不一致）
 *  B) 母目录缺失清单（分册包有、母目录无 -> 需回填）
 *  C) 分册包缺失但按版本边界应有的（仅统计，不判定对错）
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const MASTER = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/sql';
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const GROUPS = ['系统管理', '专业版', '高级版', '基础版', '小微版'];
const md5 = p => crypto.createHash('md5').update(fs.readFileSync(p)).digest('hex');
const out = [];
const say = s => { out.push(s); console.log(s); };

// 收集：文件名 -> { 位置: md5 }
const map = new Map();
function scan(dir, tag) {
  if (!fs.existsSync(dir)) { say('!! 目录不存在: ' + tag); return; }
  fs.readdirSync(dir).filter(f => /\.sql$/i.test(f) && f !== '00_执行顺序.sql').forEach(f => {
    if (!map.has(f)) map.set(f, {});
    map.get(f)[tag] = md5(path.join(dir, f));
  });
}
scan(MASTER, '母目录');
GROUPS.forEach(g => scan(path.join(ROOT, g, 'sql'), g));

let fork = 0, missMaster = 0;
say('===== A) MD5 分叉（同名脚本内容不一致）=====');
[...map.entries()].forEach(([f, where]) => {
  const hashes = [...new Set(Object.values(where))];
  if (hashes.length > 1) {
    fork++;
    say('  ✗ ' + f);
    Object.keys(where).forEach(k => say('      ' + k.padEnd(6, ' ') + ' = ' + where[k].slice(0, 12)));
  }
});
if (!fork) say('  ✓ 无分叉，全部同名脚本 MD5 一致');

say('');
say('===== B) 母目录缺失（分册包有、母目录无）=====');
[...map.entries()].forEach(([f, where]) => {
  if (!where['母目录']) {
    missMaster++;
    const only = Object.keys(where).filter(k => k !== '母目录').join('/');
    say('  ✗ ' + f + '  仅存在于: ' + only);
  }
});
if (!missMaster) say('  ✓ 母目录为完整超集，无缺失');

say('');
say('===== C) 各包脚本数统计 =====');
const cnt = tag => [...map.entries()].filter(([, w]) => w[tag]).length;
say('  母目录   : ' + cnt('母目录'));
GROUPS.forEach(g => say('  ' + g.padEnd(6, ' ') + ': ' + cnt(g)));
say('  全部去重脚本种类: ' + map.size);

say('');
say(fork === 0 && missMaster === 0 ? '★★★ Task2 校验通过 ★★★' : '★★★ 异常: 分叉 ' + fork + ' / 母目录缺失 ' + missMaster + ' ★★★');
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_verify_md5.txt', out.join('\n'), 'utf8');
