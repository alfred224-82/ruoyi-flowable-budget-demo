/**
 * Task 1/3 收尾修正（基于 _probe_idx.txt 实测）：
 *  a) 高级版 插入块阶段号 十五 -> 十四
 *  b) 基础版 插入块阶段号 十五 -> 十一
 *  c) 小微版 插入块阶段号 七（补） -> 八
 *  d) 高级版 L32 注释旧名 M8_01_portal_store_search_and_audit -> M8_02_*
 *  e) 专业版 / 高级版 补 M13_02_wecom_menu.sql 的 source 行（孤儿文件收口，T13.2）
 *  f) 3 个 full 包「M1_05 已在阶段一执行」-> 按实测阶段号表述
 * 只改本轮插入块与上述定位点，不触碰既有正文。
 */
const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const IDX = g => path.join(ROOT, g, 'sql', '00_执行顺序.sql');
const log = [];
const say = s => { log.push(s); console.log(s); };

// 各包插入块阶段号修正表（null 表示无需改）
const STAGE_FIX = { '专业版': null, '高级版': ['十五', '十四'], '基础版': ['十五', '十一'], '小微版': ['七（补）', '八'] };

// ---- 探测工具：某 source 行所属阶段标题（真标题形如 "-- 阶段X："，无缩进）
function stageOf(lines, re) {
  let cur = null;
  for (const l of lines) {
    const m = l.match(/^-- (阶段[^：:]+)[：:]/);
    if (m) cur = m[1];
    if (re.test(l)) return cur;
  }
  return null;
}
// ---- 探测工具：某 source 行上方最近的条目编号 [NN]
function itemNoOf(lines, re) {
  let idx = lines.findIndex(l => re.test(l));
  if (idx < 0) return null;
  for (let i = idx; i >= 0 && i > idx - 12; i--) {
    const m = lines[i].match(/^--\s*\[(\d+)\]/);
    if (m) return parseInt(m[1], 10);
  }
  return null;
}

Object.keys(STAGE_FIX).forEach(g => {
  const f = IDX(g);
  const t = fs.readFileSync(f, 'utf8');
  const eol = t.includes('\r\n') ? '\r\n' : '\n';
  let lines = t.split(/\r?\n/);
  const changed = [];

  // ---- a/b/c 阶段号 + 条目号（仅限本轮插入块：平台底座管理菜单）
  const fix = STAGE_FIX[g];
  if (fix) {
    const [oldNo, newNo] = fix;
    lines = lines.map(l => {
      let n = l;
      if (n.startsWith('-- 阶段' + oldNo + '：平台底座管理菜单')) n = n.replace('阶段' + oldNo, '阶段' + newNo);
      n = n.replace(new RegExp('^--\\s*\\[' + oldNo.replace(/[（）]/g, c => '\\' + c) + '-(\\d)\\]', 'g'),
                    m => m.replace('[' + oldNo + '-', '[' + newNo + '-'));
      if (n !== l) changed.push('阶段号 ' + oldNo + '->' + newNo);
      return n;
    });
  }

  // ---- f M1_05 所属阶段实测表述
  const m105Stage = stageOf(lines, /^\s*source\s+M1_05_platform_foundation_tables\.sql/i);
  if (m105Stage) {
    const m = m105Stage.match(/阶段(.+)$/);
    const stageTxt = m ? m[1] : m105Stage;
    lines = lines.map(l => {
      const n = l.replace('前置依赖 M1_05（biz_bundle / feature_flag 建表）已在阶段一执行。',
                          '前置依赖 M1_05（biz_bundle / feature_flag 建表）已在' + stageTxt + '执行。');
      if (n !== l) changed.push('M1_05 前置表述 -> ' + stageTxt);
      return n;
    });
  }

  // ---- d 高级版注释旧名
  if (g === '高级版') {
    lines = lines.map(l => {
      const n = l.replace(/M8_01_portal_store_search_and_audit/g, 'M8_02_portal_store_search_and_audit');
      if (n !== l) changed.push('注释旧名 M8_01_portal -> M8_02_portal');
      return n;
    });
  }

  // ---- e 补 M13_02 source 行（文件存在于包内但索引未引用）
  const dir = path.join(ROOT, g, 'sql');
  if (fs.existsSync(path.join(dir, 'M13_02_wecom_menu.sql')) &&
      !lines.some(l => /^\s*source\s+M13_02_wecom_menu\.sql/i.test(l))) {
    const i1301 = lines.findIndex(l => /^\s*source\s+M13_01_access_channel\.sql/i.test(l));
    if (i1301 >= 0) {
      const no = itemNoOf(lines, /^\s*source\s+M13_01_access_channel\.sql/i);
      const tag = no ? '[' + (no + 1) + '] ' : '';
      const blk = [
        '-- ' + tag + '企微通讯录同步菜单（menu_id 3490~3492，biz:wecom:list/sync/log，FEAT_WECOM 高级版+），',
        '--      T13.2 补充。前置 M13_01（wecom_sync_log 表 + 通道与集成目录 3480）已在上一条执行；',
        '--      授权 boss(9201) / manager(9203)。v1.19 收口：该脚本此前在包内但索引漏引，按索引部署不会执行。',
        'source M13_02_wecom_menu.sql;',
      ];
      lines.splice(i1301 + 1, 0, ...blk);
      changed.push('补 M13_02 source 行（条目 ' + tag.trim() + '，插于 L' + (i1301 + 2) + '）');
    } else {
      changed.push('!! 未找到 M13_01 source 行，M13_02 未插入');
    }
  }

  fs.writeFileSync(f, lines.join(eol), 'utf8');
  say(g + '（总行 ' + lines.length + '）: ' + (changed.length ? changed.join('；') : '无需改动'));
});

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t13idx2_result.txt', log.join('\n'), 'utf8');
