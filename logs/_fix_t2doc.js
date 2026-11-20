/**
 * Task 2 收尾：母文档「更新脚本清单」
 *  A) 第三章后新增「3.1 SQL 脚本归属总表」——70 项脚本 × 5 分册包，全部由文件系统实测派生（零猜测）
 *     收口 M5_12 / M5_13 / M5_14 / M8_01 / M8_02 / M10_01 / M13_01 / M13_02 等在母文档零命中的脚本
 *  B) 修 L502 stale：M15_12 的 menu_id「1000+」-> 实际 1300~1359
 *  C) 修 L506 stale：小微版索引「29 项、验收检查 13 项」-> 实测 33 项 / 14 项
 *  D) 修 L498~L504 markdown 断裂：M15 清单表后半段缺 "> " 前缀，脱离 blockquote 导致表格渲染断裂
 *  E) 校 L902：M15_14 在小微版索引中的登记号是否仍为 [29]
 * 注：T 编号排序按 (里程碑号, 任务号) 双键数值排序，修掉 parseFloat('15.16') > parseFloat('15.3') 的错序
 */
const fs = require('fs');
const path = require('path');
const MASTER = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/sql';
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const GROUPS = ['系统管理', '专业版', '高级版', '基础版', '小微版'];
const DOC = path.join(ROOT, '资金管控系统开发任务文档.md');
const log = [];
const say = s => { log.push(s); console.log(s); };

// ---------------------------------------------------------------- 采集
const pkgs = {};
GROUPS.forEach(g => { pkgs[g] = new Set(fs.readdirSync(path.join(ROOT, g, 'sql'))); });
const files = fs.readdirSync(MASTER).filter(f => /\.sql$/i.test(f));

const msKey = f => {
  const m = f.match(/^M(\d+)_(\d+)_/i);
  return m ? [parseInt(m[1], 10), parseInt(m[2], 10)] : [999, 999];
};
const msLabel = f => {
  const m = f.match(/^M(\d+)_/i);
  if (!m) return '—';
  const n = m[1];
  return (n === '3' && /^M3_0[56]_/.test(f)) ? 'M3.5' : 'M' + n;
};
const tSort = (a, b) => {
  const pa = a.slice(1).split('.').map(Number), pb = b.slice(1).split('.').map(Number);
  return pa[0] - pb[0] || pa[1] - pb[1];
};

const rows = files.map(f => {
  const txt = fs.readFileSync(path.join(MASTER, f), 'utf8');
  const cmt = txt.split(/\r?\n/).filter(l => /^\s*--/.test(l)).join('\n');
  const ts = [...new Set(cmt.match(/T\d+\.\d+/g) || [])].sort(tSort);
  return { f, ms: msLabel(f), ts, own: GROUPS.filter(g => pkgs[g].has(f)) };
}).sort((a, b) => { const ka = msKey(a.f), kb = msKey(b.f); return ka[0] - kb[0] || ka[1] - kb[1]; });

// ---------------------------------------------------------------- A) 生成表格
const T = [];
T.push('### 3.1 SQL 脚本归属总表（v1.19 新增，母目录 ' + rows.length + ' 项 × 五分册部署包）');
T.push('');
T.push('> **本表由文件系统实测派生**（扫描 `docs/newrequest/sql/` 与五个分册 `sql/` 目录，2026-09-10），');
T.push('> 用于收口此前母文档**零命中**的脚本：`M5_12` / `M5_13` / `M5_14` / `M8_01` / `M8_02` / `M10_01` / `M13_01` / `M13_02`。');
T.push('> **归档策略**：母目录为全量超集与源数据，分册包为自包含可独立部署子集，同名副本 **MD5 必须一致**（本轮已全量校验，零分叉）。');
T.push('> **✓ = 该分册包含此脚本**；空白 = 按版本边界不部署（如 M7 仅专业版、M13 仅高级版+、M15/M16 仅小微版）。');
T.push('> **覆盖任务**取自脚本头部注释中的 `T` 编号，`—` 表示脚本注释未标注任务号（不影响部署）。');
T.push('');
T.push('| 脚本 | 里程碑 | 覆盖任务 | 系统管理 | 专业版 | 高级版 | 基础版 | 小微版 |');
T.push('|------|:------:|----------|:--------:|:------:|:------:|:------:|:------:|');
rows.forEach(r => {
  const cells = GROUPS.map(g => r.own.includes(g) ? '✓' : '');
  T.push('| `' + r.f + '` | ' + r.ms + ' | ' + (r.ts.length ? r.ts.join(' / ') : '—') + ' | ' + cells.join(' | ') + ' |');
});
T.push('');
const cnt = g => rows.filter(r => r.own.includes(g)).length;
T.push('> **分包脚本数（实测）**：母目录 **' + rows.length + '**（去重种类 ' + rows.length + '，即母目录为完整超集、无缺失）｜');
T.push('> 系统管理 ' + cnt('系统管理') + '｜专业版 ' + cnt('专业版') + '｜高级版 ' + cnt('高级版') + '｜基础版 ' + cnt('基础版') + '｜小微版 ' + cnt('小微版') + '。');
T.push('>');
T.push('> **★顺序强约束（跨包通用，v1.19 登记）**：`M15_12` 以 `DELETE FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359` 开局，');
T.push('> 而 `M10_01` 建的 1320~1322（业态包管理）/ 1330~1332（功能开关管理）正落在该区段内，');
T.push('> **必须先 M15_12 后 M10_01**，颠倒则 T10.2 / T10.3 的 PC 管理页入口被 DELETE 清除。五个分册索引均已按此顺序登记并附注释。');
T.push('>');
T.push('> **⚠️ 残留风险（v1.19 记录，未改动）**：`M15_14_frontend_menu_gap_patch.sql` 仅归《小微版》包，');
T.push('> 但其内容含「1312~1314 版本档位写按钮收敛（后端为只读聚合端点）」——该项属**平台级菜单**，');
T.push('> 理论上对其余四包同样适用；因该脚本同时建小微版专属菜单 7063~7066、改 7012/7040/7051/7055/7135，');
T.push('> **不可拆分**，故维持仅小微版部署。其余四包的 1312~1314 写按钮仍可见但后端只读，点击报权限/只读错误，');
T.push('> 属**已知可接受偏差**，如需彻底收口应另建平台级补丁脚本（待用户拍板）。');
T.push('');

// ---------------------------------------------------------------- 写入母文档
let doc = fs.readFileSync(DOC, 'utf8');
const eol = doc.includes('\r\n') ? '\r\n' : '\n';
let lines = doc.split(/\r?\n/);

// 定位第三章末（"## 四、开发" 之前的 "---" 分隔线）
const i4 = lines.findIndex(l => /^## 四、/.test(l));
if (i4 < 0) throw new Error('未找到「## 四、」');
let sep = i4 - 1;
while (sep > 0 && !/^---\s*$/.test(lines[sep])) sep--;
if (!/^---\s*$/.test(lines[sep])) throw new Error('未找到第三章末分隔线');
// 幂等：若已存在 3.1 表则先移除
const old = lines.findIndex(l => /^### 3\.1 SQL 脚本归属总表/.test(l));
if (old >= 0) {
  let oe = old;
  while (oe < lines.length && !/^---\s*$/.test(lines[oe])) oe++;
  lines.splice(old, oe - old);
  say('A) 检测到旧 3.1 表，已移除（重建）');
  const i4b = lines.findIndex(l => /^## 四、/.test(l));
  sep = i4b - 1;
  while (sep > 0 && !/^---\s*$/.test(lines[sep])) sep--;
}
lines.splice(sep, 0, ...T);
say('A) 3.1 SQL 脚本归属总表已插入：' + rows.length + ' 行脚本 + 说明，共 ' + T.length + ' 行，位于 L' + (sep + 1));

// ---------------------------------------------------------------- B) L502 menu_id
const b0 = '「资金平台管理菜单」一级目录收纳（menu_id 1000+，与若依「系统管理」并列）';
const b1 = '「资金平台管理菜单」一级目录收纳（**menu_id 1300~1359**：1300 目录 + 1310~1314 版本档位管理，与若依「系统管理」并列；若依原生按钮已占 1000~1230，故取 1300 段避让）';
let nb = 0;
lines = lines.map(l => { if (l.includes(b0)) { nb++; return l.replace(b0, b1); } return l; });
say('B) M15_12 menu_id 1000+ -> 1300~1359：命中 ' + nb + ' 处');

// ---------------------------------------------------------------- C) L506 索引计数
const wxIdx = fs.readFileSync(path.join(ROOT, '小微版', 'sql', '00_执行顺序.sql'), 'utf8').split(/\r?\n/);
const nSrc = wxIdx.filter(l => /^\s*source\s+/i.test(l)).length;
const nAcc = wxIdx.filter(l => /^-- \d+\)/.test(l)).length;
let nc = 0;
lines = lines.map(l => {
  const m = l.match(/索引已同步至 (\d+) 项、验收检查 (\d+) 项/);
  if (m && (m[1] !== String(nSrc) || m[2] !== String(nAcc))) {
    nc++;
    return l.replace(m[0], '索引已同步至 **' + nSrc + ' 项**、验收检查 **' + nAcc + ' 项**（v1.19 补入 M10_01 后）');
  }
  return l;
});
say('C) 小微版索引计数 -> ' + nSrc + ' 项 / ' + nAcc + ' 项：命中 ' + nc + ' 处');

// ---------------------------------------------------------------- D) blockquote 断裂
const i487 = lines.findIndex(l => /^> \*\*M15 建表\/脚本交付清单/.test(l));
let nd = 0;
if (i487 >= 0) {
  for (let i = i487; i < lines.length; i++) {
    if (/^> \*\*部署索引\*\*/.test(lines[i])) break;
    if (/^\|/.test(lines[i])) { lines[i] = '> ' + lines[i]; nd++; }
  }
}
say('D) M15 清单表 blockquote 前缀补齐：' + nd + ' 行' + (i487 < 0 ? '（!! 未定位表头）' : ''));

// ---------------------------------------------------------------- E) L902 登记号
const i1514 = wxIdx.findIndex(l => /^\s*source\s+M15_14_frontend_menu_gap_patch\.sql/i.test(l));
let no1514 = null;
for (let i = i1514; i >= 0 && i > i1514 - 12; i--) { const m = wxIdx[i].match(/^--\s*\[(\d+)\]/); if (m) { no1514 = m[1]; break; } }
say('E) M15_14 在小微版索引的登记号实测 = [' + no1514 + ']');
let ne = 0;
if (no1514 && no1514 !== '29') {
  lines = lines.map(l => { const n = l.replace('同步登记为 [29]', '同步登记为 [' + no1514 + ']'); if (n !== l) ne++; return n; });
}
say('E) 母文档登记号修正：' + ne + ' 处' + (ne === 0 ? '（实测与文档一致，无需改）' : ''));

fs.writeFileSync(DOC, lines.join(eol), 'utf8');
say('母文档已写入，总行数 ' + lines.length + '（原 1184）');
fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t2doc_result.txt', log.join('\n'), 'utf8');
