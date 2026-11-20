/**
 * Task 1/3 终局修正（基于源文件实测）：
 *  1) 措辞：已在一~八执行 -> 已在阶段一~八执行（专业版/高级版）；已在二执行 -> 已在阶段二执行（基础版）
 *  2) 高级版 M13_02 条目号 [45] 与阶段十三 M5_14 的 [45] 重号 -> 改为 [44-1]（M13_01 的补充子项）
 *  3) 小微版 验收检查 9) 期望值随 M10_01 补入而过期（6 行 -> 12 行；parent_id=1300 ≥6 -> ≥8）
 *  4) 专业版/高级版/基础版 各追加一条「平台底座管理菜单就绪」验收检查（此前仅小微版有）
 */
const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const IDX = g => path.join(ROOT, g, 'sql', '00_执行顺序.sql');
const log = [];
const say = s => { log.push(s); console.log(s); };

const ACC = { '专业版': 8, '高级版': 7, '基础版': 9 };

function accBlock(n) {
  return [
    '-- ' + n + ') ★平台底座管理菜单就绪（M15_12 + M10_01，v1.19 补入自包含包）',
    '-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu',
    '--  WHERE menu_id BETWEEN 1300 AND 1359 ORDER BY menu_id;',
    '-- 预期 12 行：1300(资金平台管理菜单目录) + 1310~1314(版本档位管理 5 行)',
    '--            + 1320~1322(业态包管理 3 行) + 1330~1332(功能开关管理 3 行)',
    '-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 1320 AND 1339;',
    '-- 预期 6（六层防串第①层「版本分配入口」超管可达，T10.2 / T10.3 PC 管理页）',
    '',
  ];
}

['专业版', '高级版', '基础版', '小微版'].forEach(g => {
  const f = IDX(g);
  const t = fs.readFileSync(f, 'utf8');
  const eol = t.includes('\r\n') ? '\r\n' : '\n';
  let lines = t.split(/\r?\n/);
  const ch = [];

  // ---- 1) 措辞修正
  const before1 = lines.join('\n');
  lines = lines.map(l => l
    .replace('建表）已在一~八执行。', '建表）已在阶段一~八执行。')
    .replace('建表）已在二执行。', '建表）已在阶段二执行。'));
  if (lines.join('\n') !== before1) ch.push('M1_05 前置措辞补「阶段」前缀');

  // ---- 2) 高级版 M13_02 条目号重号
  if (g === '高级版') {
    let fixed = false;
    lines = lines.map(l => {
      if (/^-- \[45\] 企微通讯录同步菜单/.test(l)) { fixed = true; return l.replace('[45]', '[44-1]'); }
      return l;
    });
    if (fixed) ch.push('M13_02 条目号 [45]->[44-1]（消除与阶段十三 M5_14 的 [45] 重号）');
  }

  // ---- 3) 小微版 验收检查 9) 期望值
  if (g === '小微版') {
    const i = lines.findIndex(l => /^-- 9\) ★资金平台管理菜单目录/.test(l));
    if (i >= 0) {
      // 原 4 行：标题 / SELECT / 预期 / parent_id
      lines.splice(i, 4,
        '-- 9) ★资金平台管理菜单目录 + 版本档位管理 + 业态包/功能开关管理',
        '-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359 ORDER BY menu_id;',
        '-- 预期 12 行：1300(目录) + 1310(版本档位管理) + 1311~1314(4 按钮)',
        '--            + 1320(业态包管理) + 1321~1322(2 按钮) + 1330(功能开关管理) + 1331~1332(2 按钮)',
        '--            （1320~1332 由阶段八 [八-1] M10_01 建出，v1.19 补入）',
        '-- SELECT COUNT(*) FROM sys_menu WHERE parent_id = 1300;  -- 预期 ≥8（re-parent 的租户/角色/行业/菜单/角色模板 + 版本档位管理 + 业态包管理 + 功能开关管理）',
        '-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 1320 AND 1339;  -- 预期 6');
      ch.push('验收检查 9) 期望值 6 行->12 行、parent_id=1300 ≥6->≥8');
    } else ch.push('!! 未定位小微版验收检查 9)');
  }

  // ---- 4) 追加平台底座菜单验收检查（专业版/高级版/基础版）
  if (ACC[g]) {
    const already = lines.some(l => /平台底座管理菜单就绪/.test(l));
    if (!already) {
      const ib = lines.findIndex(l => new RegExp('^-- ' + g + '能力边界').test(l));
      if (ib > 0) {
        // 向上找紧邻的 "-- =====" 分隔线
        let sep = ib - 1;
        while (sep > 0 && !/^-- ={10,}/.test(lines[sep])) sep--;
        if (/^-- ={10,}/.test(lines[sep])) {
          lines.splice(sep, 0, ...accBlock(ACC[g]));
          ch.push('追加验收检查 ' + ACC[g] + ')「平台底座管理菜单就绪」@L' + (sep + 1));
        } else ch.push('!! 未找到 ' + g + ' 能力边界分隔线');
      } else ch.push('!! 未找到 ' + g + '能力边界标题');
    } else ch.push('验收检查已存在，跳过');
  }

  fs.writeFileSync(f, lines.join(eol), 'utf8');
  say(g + '（总行 ' + lines.length + '）: ' + (ch.length ? ch.join('；') : '无需改动'));
});

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t13idx3_result.txt', log.join('\n'), 'utf8');
