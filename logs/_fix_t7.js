const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/';

const SYS_MGMT_LINE = '**版本管控硬约束与平台底座**：见《系统管理》分册 `../系统管理/资金管控系统开发任务文档（系统管理）.md`（六层防串 / 三维能力模型 / 双阶段租户识别 / 授权模板机制均以该分册为权威）';

const targets = [
  { name: '高级版', path: R + '高级版/资金管控系统开发任务文档（高级版）.md' },
  { name: '专业版', path: R + '专业版/资金管控系统开发任务文档（专业版）.md' },
  { name: '小微版', path: R + '小微版/资金管控系统开发任务文档（小微版）.md' },
];

targets.forEach(d => {
  let t = fs.readFileSync(d.path, 'utf8');
  
  if (t.includes('版本管控硬约束与平台底座')) {
    console.log(d.name + ': 已存在系统管理指针，跳过');
    return;
  }
  
  // Insert before **完整任务定义与交付证据**
  const anchor = '**完整任务定义与交付证据**';
  if (!t.includes(anchor)) {
    console.log(d.name + ': 未找到完整任务定义锚点，跳过');
    return;
  }
  
  t = t.replace(anchor, SYS_MGMT_LINE + '\n' + anchor);
  fs.writeFileSync(d.path, t, 'utf8');
  console.log(d.name + ': 已补《系统管理》指针');
});

console.log('DONE');
