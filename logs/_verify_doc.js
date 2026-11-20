const fs = require('fs');
const f = fs.readFileSync('D:\\workspace\\RuoYi-Flowable-Plus-0.8.X\\docs\\最新整理好的需求\\资金管控系统开发任务文档.md', 'utf8');
const lines = f.split('\n');

// Check M5 task table (T5.13~T5.18)
console.log('=== M5 Task Table ===');
for (let i = 243; i <= 248; i++) {
  console.log(`Line ${i+1}: ${lines[i].substring(0, 15)}`);
}

// Check M6 T6.8
console.log('\n=== M6 T6.8 ===');
console.log(`Line 268: ${lines[267].substring(0, 15)}`);

// Check statistics table
console.log('\n=== Statistics Table ===');
console.log(`Line 556 (M5): ${lines[555]}`);
console.log(`Line 557 (M6): ${lines[556]}`);
console.log(`Line 568 (合计): ${lines[567]}`);

// Check 10.1 table
console.log('\n=== 10.1 Change Overview ===');
for (let i = 750; i <= 753; i++) {
  console.log(`Line ${i+1}: ${lines[i].substring(0, 60)}`);
}

// Check M5 note
console.log('\n=== M5 Note ===');
// Find the v1.15/v1.16 对账结果 line
for (let i = 225; i < 235; i++) {
  if (lines[i].includes('对账结果')) {
    console.log(`Line ${i+1}: ${lines[i].substring(0, 80)}`);
  }
}

// Check M6 note
console.log('\n=== M6 Note ===');
for (let i = 253; i < 260; i++) {
  if (lines[i].includes('对账结果')) {
    console.log(`Line ${i+1}: ${lines[i].substring(0, 100)}`);
  }
}

// Count totals
const allText = f;
const doneCount = (allText.match(/\| ✅ T/g) || []).length;
const redoCount = (allText.match(/\| 🔄 T/g) || []).length;
const partialCount = (allText.match(/\| 🟡 T/g) || []).length;
const newCount = (allText.match(/\| 🆕 T/g) || []).length;
console.log('\n=== Actual Emoji Counts in Task Tables ===');
console.log(`✅ tasks: ${doneCount}`);
console.log(`🔄 tasks: ${redoCount}`);
console.log(`🟡 tasks: ${partialCount}`);
console.log(`🆕 tasks: ${newCount}`);
console.log(`Total: ${doneCount + redoCount + partialCount + newCount}`);
