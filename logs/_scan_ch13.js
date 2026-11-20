const fs = require('fs');
const p = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
const t = fs.readFileSync(p, 'utf8');
const lines = t.split(/\r?\n/);

// Find chapter 13 headings
lines.forEach((l, i) => {
  if (/^#{1,4}\s.*13/.test(l) || /^#{1,4}\s.*缺口|^#{1,4}\s.*防串|^#{1,4}\s.*排期|^#{1,4}\s.*判定/.test(l)) {
    console.log('L' + (i + 1) + ': ' + l.substring(0, 150));
  }
});

// Also find section 13.4 area
console.log('\n--- 13.4 区域 ---');
let inSection = false;
lines.forEach((l, i) => {
  if (/13\.4/.test(l) && /^#/.test(l)) {
    inSection = true;
  }
  if (inSection) {
    console.log('L' + (i + 1) + ': ' + l.substring(0, 150));
    if (i > 0 && /^## /.test(l) && !/13\.4/.test(l)) {
      inSection = false;
    }
  }
});
