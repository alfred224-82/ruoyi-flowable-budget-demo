/**
 * 侦察：母目录 70 个脚本的命名规律 + 各脚本注释中的 T 编号 + 分册包归属
 * 同时定位母文档中「交付清单 / 部署索引 / 母目录」相关行，为 Task2 脚本清单更新做准备
 */
const fs = require('fs');
const path = require('path');
const MASTER = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/sql';
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const GROUPS = ['系统管理', '专业版', '高级版', '基础版', '小微版'];
const DOC = path.join(ROOT, '资金管控系统开发任务文档.md');
const out = [];
const say = s => { out.push(s); console.log(s); };

const pkgs = {};
GROUPS.forEach(g => { pkgs[g] = new Set(fs.readdirSync(path.join(ROOT, g, 'sql'))); });

const files = fs.readdirSync(MASTER).filter(f => /\.sql$/i.test(f)).sort();
say('===== 母目录脚本（' + files.length + ' 个）=====');
say('脚本名 | 里程碑 | 注释T编号 | 归属包');
files.forEach(f => {
  const mm = f.match(/^(M\d+(?:\.\d+)?)_/i);
  const ms = mm ? mm[1] : '(非M命名)';
  const txt = fs.readFileSync(path.join(MASTER, f), 'utf8');
  const cmt = txt.split(/\r?\n/).filter(l => /^\s*--/.test(l)).join('\n');
  const ts = [...new Set((cmt.match(/T\d+\.\d+/g) || []))];
  // 按数字排序 T 编号
  ts.sort((a, b) => parseFloat(a.slice(1)) - parseFloat(b.slice(1)));
  const own = GROUPS.filter(g => pkgs[g].has(f));
  say(f + ' | ' + ms + ' | ' + (ts.length ? ts.join(',') : '(无)') + ' | ' + (own.length ? own.join('/') : '★仅母目录'));
});

say('');
say('===== 非 M 命名脚本 =====');
files.filter(f => !/^M\d/i.test(f)).forEach(f => say('  ' + f));

say('');
say('===== 母文档相关行定位 =====');
const dl = fs.readFileSync(DOC, 'utf8').split(/\r?\n/);
dl.forEach((l, i) => {
  if (/交付清单|部署索引|母目录|newrequest.sql|00_执行顺序/.test(l)) {
    say('  L' + (i + 1) + '| ' + l.trim().slice(0, 120));
  }
});

say('');
say('===== 小微版索引计数（用于修正 L506 stale 数值）=====');
const wx = fs.readFileSync(path.join(ROOT, '小微版', 'sql', '00_执行顺序.sql'), 'utf8').split(/\r?\n/);
say('  source 条数 = ' + wx.filter(l => /^\s*source\s+/i.test(l)).length);
say('  验收检查项数 = ' + wx.filter(l => /^-- \d+\)/.test(l)).length);

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_probe_master_sql.txt', out.join('\n'), 'utf8');
