const fs = require('fs');
const p = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/资金管控系统开发任务文档.md';
const t = fs.readFileSync(p, 'utf8');
const lines = t.split(/\r?\n/);

// Check specific lines
[147, 165, 180, 185, 192, 200, 232, 253, 254, 256, 290].forEach(n => {
  const l = lines[n - 1];
  if (l) {
    // Try to extract task ID
    const m = l.match(/T(\d+\.\d+)/);
    const status = l.match(/([✅🟡🔄🆕])/);
    console.log('L' + n + ': status=' + (status ? status[1] : 'NONE') + ' task=' + (m ? 'T' + m[1] : 'NONE') + ' first50=' + l.substring(0, 50));
  }
});

// Also count all lines containing T*.* pattern in chapter 2
let count = 0;
let inCh2 = false;
lines.forEach((l, i) => {
  if (/^## 二、/.test(l)) inCh2 = true;
  if (/^## 三、/.test(l)) inCh2 = false;
  if (inCh2 && /T\d+\.\d+/.test(l) && l.startsWith('|')) {
    count++;
  }
});
console.log('\nChapter 2 task rows (loose):', count);

// Count by looking for each status emoji
let counts2 = { '✅': 0, '🟡': 0, '🔄': 0, '🆕': 0 };
inCh2 = false;
lines.forEach((l, i) => {
  if (/^## 二、/.test(l)) inCh2 = true;
  if (/^## 三、/.test(l)) inCh2 = false;
  if (inCh2 && l.startsWith('|') && /T\d+\.\d+/.test(l)) {
    for (const s of ['✅', '🟡', '🔄', '🆕']) {
      if (l.includes(s)) { counts2[s]++; break; }
    }
  }
});
console.log('Status counts:', JSON.stringify(counts2));
