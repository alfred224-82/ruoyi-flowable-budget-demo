const fs = require('fs');
const f = fs.readFileSync('D:\\workspace\\RuoYi-Flowable-Plus-0.8.X\\docs\\最新整理好的需求\\资金管控系统开发任务文档.md', 'utf8');
const lines = f.split('\n');

console.log('=== Final Verification ===\n');

// M5 task table
console.log('--- M5 Tasks ---');
for (let i = 243; i <= 249; i++) {
  const m = lines[i].match(/^\| ([^\s]+) T5\.\d+/);
  if (m) console.log(`Line ${i+1}: ${m[1]} T5.${lines[i].match(/T5\.(\d+)/)[1]}`);
}

// M6 T6.8
console.log('\n--- M6 T6.8 ---');
const m68 = lines[267].match(/^\| ([^\s]+) T6\.8/);
if (m68) console.log(`Line 268: ${m68[1]} T6.8`);

// Statistics table
console.log('\n--- Statistics Table ---');
const m5m = lines[555].match(/\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|/);
if (m5m) console.log(`M5: total=${m5m[1]} done=${m5m[2]} redo=${m5m[3]} partial=${m5m[4]}`);

const m6m = lines[556].match(/\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|/);
if (m6m) console.log(`M6: total=${m6m[1]} done=${m6m[2]} redo=${m6m[3]} partial=${m6m[4]}`);

const totM = lines[567].match(/\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*\s*\|\s*\*\*(\d+)\*\*/);
if (totM) {
  const sum = parseInt(totM[2]) + parseInt(totM[3]) + parseInt(totM[4]) + parseInt(totM[5]);
  console.log(`Total: ${totM[1]} done=${totM[2]} redo=${totM[3]} partial=${totM[4]} new=${totM[5]} sum=${sum}`);
  console.log(sum === parseInt(totM[1]) ? '✓ Sum matches total!' : '✗ SUM MISMATCH!');
}

// M5 note
console.log('\n--- M5 Note ---');
for (let i = 223; i < 229; i++) {
  if (lines[i].includes('v1.1')) console.log(`Line ${i+1}: ${lines[i].substring(0, 80)}`);
}

// M6 note
console.log('\n--- M6 Note ---');
for (let i = 256; i < 258; i++) {
  if (lines[i].includes('v1.1')) console.log(`Line ${i+1}: ${lines[i].substring(0, 80)}`);
}

console.log('\n=== Done ===');
