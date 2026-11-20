const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/';

// Scan footer pointers in all 5 task documents + 母文档
const docs = [
  { name: '母文档', path: R + '资金管控系统开发任务文档.md' },
  { name: '基础版', path: R + '基础版/资金管控系统开发任务文档（基础版）.md' },
  { name: '高级版', path: R + '高级版/资金管控系统开发任务文档（高级版）.md' },
  { name: '专业版', path: R + '专业版/资金管控系统开发任务文档（专业版）.md' },
  { name: '小微版', path: R + '小微版/资金管控系统开发任务文档（小微版）.md' },
];

docs.forEach(d => {
  if (!fs.existsSync(d.path)) { console.log(d.name + ': NOT FOUND'); return; }
  const t = fs.readFileSync(d.path, 'utf8');
  const lines = t.split(/\r?\n/);
  console.log('\n=== ' + d.name + ' (' + lines.length + ' lines) ===');
  lines.forEach((l, i) => {
    if (/适用范围|SQL 部署|操作手册|完整任务|文档版本|最近变更|见.*系统管理|系统管理.*分册/.test(l)) {
      console.log('L' + (i + 1) + ': ' + l.substring(0, 150));
    }
  });
});
