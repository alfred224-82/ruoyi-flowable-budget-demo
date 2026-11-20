/** T11.2 取证：SysDataScopeServiceImpl 是否处理 participant/signup；VIEWER→member 映射是否落库 */
const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/';
const out = [];
const say = s => out.push(s);

function walk(dir, acc, ext) {
  let es; try { es = fs.readdirSync(dir, { withFileTypes: true }); } catch (e) { return acc; }
  for (const e of es) {
    const fp = dir + '/' + e.name;
    if (e.isDirectory()) { if (!['node_modules', 'unpackage', 'dist', 'target', '.git'].includes(e.name)) walk(fp, acc, ext); }
    else if (ext.some(x => e.name.endsWith(x))) acc.push(fp);
  }
  return acc;
}

// A. SysDataScopeServiceImpl 全文
say('=== A. SysDataScopeServiceImpl 全文 ===');
const be = walk(R + 'ruoyi-system/src', [], ['.java']).concat(walk(R + 'ruoyi-admin/src', [], ['.java']), walk(R + 'ruoyi-common/src', [], ['.java']));
const dss = be.filter(f => /DataScope/.test(f));
dss.forEach(f => {
  say('--- ' + f.replace(R, '') + ' ---');
  fs.readFileSync(f, 'utf8').split(/\r?\n/).forEach((l, i) => say('  ' + (i + 1) + ' | ' + l));
});

// B. ProjectMemberRole 枚举全文
say('');
say('=== B. ProjectMemberRole 枚举 ===');
be.filter(f => /ProjectMemberRole/.test(f)).forEach(f => {
  say('--- ' + f.replace(R, '') + ' ---');
  fs.readFileSync(f, 'utf8').split(/\r?\n/).forEach((l, i) => say('  ' + (i + 1) + ' | ' + l));
});

// C. VIEWER 映射是否落库（SQL 全仓）
say('');
say('=== C. SQL 中 VIEWER / member 映射 ===');
const sqls = walk(R + 'docs/newrequest/sql', [], ['.sql']).concat(walk(R + 'sql', [], ['.sql']));
say('  扫描 sql 文件数 ' + sqls.length);
sqls.forEach(f => {
  fs.readFileSync(f, 'utf8').split(/\r?\n/).forEach((l, i) => {
    if (/VIEWER/i.test(l)) say('  ' + f.replace(R, '') + ':' + (i + 1) + ' | ' + l.trim().slice(0, 160));
  });
});

// D. participant / signup 在数据范围相关代码中的命中
say('');
say('=== D. participant|signup 在后端的命中（排除 Controller/Service 业务命名）===');
be.forEach(f => {
  fs.readFileSync(f, 'utf8').split(/\r?\n/).forEach((l, i) => {
    if (/"participant"|"signup"|PARTICIPANT|SIGNUP/.test(l)) say('  ' + f.replace(R, '') + ':' + (i + 1) + ' | ' + l.trim().slice(0, 160));
  });
});

fs.writeFileSync(R + 'logs/_t112_probe.txt', out.join('\n'), 'utf8');
console.log('lines=' + out.length);
