const fs = require('fs');
const f = fs.readFileSync('D:\\workspace\\RuoYi-Flowable-Plus-0.8.X\\docs\\最新整理好的需求\\资金管控系统开发任务文档.md', 'utf8');
const lines = f.split('\n');

// Find M5 note with "对账结果"
console.log('=== Searching M5 note area (lines 220-232) ===');
for (let i = 219; i < 232; i++) {
  const line = lines[i];
  if (line.includes('v1.1') || line.includes('M5') || line.includes('对账') || line.includes('基础版')) {
    console.log(`Line ${i+1}: ${line.substring(0, 120)}`);
  }
}

// Find the text "v1.16 对账结果" or "v1.15 对账结果" anywhere
console.log('\n=== All "对账结果" occurrences ===');
for (let i = 0; i < lines.length; i++) {
  if (lines[i].includes('对账结果') && lines[i].includes('v1.1')) {
    console.log(`Line ${i+1}: ${lines[i].substring(0, 120)}`);
  }
}
