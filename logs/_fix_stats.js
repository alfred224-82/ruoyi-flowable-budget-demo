const fs = require('fs');
const path = require('path');

const docPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
let content = fs.readFileSync(docPath, 'utf8');
const lines = content.split('\n');

// 1. Update M5: \u270511 \ud83d\udfe12 \u2192 \u270512 \ud83d\udfe11
let m5Updated = false;
lines.forEach((l, i) => {
    if (l.startsWith('M5') && l.includes('\u270511') && l.includes('\ud83d\udfe12') && !m5Updated) {
        lines[i] = l.replace('\u270511', '\u270512').replace('\ud83d\udfe12', '\ud83d\udfe11');
        m5Updated = true;
        console.log('L' + (i+1) + ': M5 updated: ' + lines[i].substring(0, 50));
    }
});
console.log('M5 updated: ' + m5Updated);

// 2. Check M12 was updated correctly
lines.forEach((l, i) => {
    if (l.startsWith('M12')) {
        console.log('L' + (i+1) + ': M12 current: ' + l.substring(0, 50));
    }
});

// 3. Check total
lines.forEach((l, i) => {
    if (l.includes('\u5408\u8ba1') && l.includes('= 160')) {
        console.log('L' + (i+1) + ': Total current: ' + l.substring(0, 80));
    }
});

content = lines.join('\n');
fs.writeFileSync(docPath, content, 'utf8');
console.log('\nDone.');
