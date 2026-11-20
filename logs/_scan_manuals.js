const fs = require('fs');

const manuals = [
  // Old location (docs/newrequest/操作手册/)
  { path: 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/操作手册/00_管理员操作手册.md', loc: 'OLD' },
  { path: 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/操作手册/02_高级版操作手册.md', loc: 'OLD' },
  { path: 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/操作手册/03_专业版操作手册.md', loc: 'OLD' },
  // Current location (docs/最新整理好的需求/)
  { path: 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/00_管理员操作手册.md', loc: 'ROOT' },
  { path: 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/系统管理/00_管理员操作手册.md', loc: 'SYSMGMT' },
  { path: 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/高级版/02_高级版操作手册.md', loc: 'ADV' },
  { path: 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/专业版/03_专业版操作手册.md', loc: 'PRO' },
  { path: 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/小微版/04_小微版操作手册.md', loc: 'MICRO' },
];

manuals.forEach(m => {
  if (!fs.existsSync(m.path)) {
    console.log(m.loc + ': NOT FOUND - ' + m.path);
    return;
  }
  const t = fs.readFileSync(m.path, 'utf8');
  const lines = t.split(/\r?\n/);
  const ver = lines.find(l => /\*\*版本\*\*|版本：|v1\.\d+/.test(l));
  const date = lines.find(l => /\*\*日期\*\*|日期：|更新.*2026/.test(l));
  console.log(m.loc + ' (' + lines.length + 'L): ' + (ver || 'NO VER').substring(0, 80));
  if (date) console.log('  date: ' + date.substring(0, 80));
  
  // Check for stale version references
  const staleRefs = [];
  lines.forEach((l, i) => {
    if (/v1\.\d+/.test(l)) {
      const vm = l.match(/v1\.(\d+)/);
      if (vm && parseInt(vm[1]) < 16) {
        staleRefs.push({ line: i + 1, ver: 'v1.' + vm[1], text: l.substring(0, 80) });
      }
    }
  });
  if (staleRefs.length > 0) {
    console.log('  stale refs (' + staleRefs.length + '):');
    staleRefs.slice(0, 5).forEach(s => console.log('    L' + s.line + ' ' + s.ver + ': ' + s.text));
  }
  console.log('');
});
