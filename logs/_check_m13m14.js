const fs = require('fs');
const path = require('path');

const docPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
let content = fs.readFileSync(docPath, 'utf8');
const lines = content.split('\n');

// Find all M13 and M14 rows to check for issues
console.log('=== Checking M13/M14 rows ===');
for (let i = 0; i < lines.length; i++) {
    if (lines[i].includes('M13') && lines[i].includes('|') && !lines[i].includes('T13')) {
        console.log('L' + (i+1) + ': ' + lines[i].substring(0, 150));
    }
    if (lines[i].includes('M14') && lines[i].includes('|') && !lines[i].includes('T14')) {
        console.log('L' + (i+1) + ': ' + lines[i].substring(0, 150));
    }
}

// Also check the total row
console.log('\n=== Checking total rows ===');
for (let i = 0; i < lines.length; i++) {
    if (lines[i].includes('**160**') && lines[i].includes('**85**')) {
        console.log('L' + (i+1) + ': ' + lines[i].substring(0, 200));
    }
}
