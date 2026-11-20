const fs = require('fs');
const p = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/操作手册/01_基础版操作手册.md';
const t = fs.readFileSync(p, 'utf8');
const lines = t.split(/\r?\n/);
console.log('Lines:', lines.length);
console.log('First 5 lines:');
lines.slice(0, 5).forEach((l, i) => console.log('L' + (i + 1) + ': ' + l.substring(0, 120)));
console.log('\nLast 5 lines:');
lines.slice(-5).forEach((l, i) => console.log('L' + (lines.length - 4 + i) + ': ' + l.substring(0, 120)));

// Check if same as current location
const current = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/基础版/01_基础版操作手册.md';
if (fs.existsSync(current)) {
  const ct = fs.readFileSync(current, 'utf8');
  console.log('\nSame as current?', t === ct);
  console.log('Current lines:', ct.split(/\r?\n/).length);
} else {
  console.log('\nCurrent location NOT FOUND');
}
