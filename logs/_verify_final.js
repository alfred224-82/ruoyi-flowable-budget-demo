const fs = require('fs');
const path = require('path');

const docPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
const content = fs.readFileSync(docPath, 'utf8');
const lines = content.split('\n');

// Count actual task statuses
let ok = 0, yl = 0, rd = 0, nw = 0;
lines.forEach((l) => {
    if (l.match(/^\| \u2705 T\d+/)) ok++;
    if (l.match(/^\| \ud83d\udfe1 T\d+/)) yl++;
    if (l.match(/^\| \ud83d\udd04 T\d+/)) rd++;
    if (l.match(/^\| \ud83c\udd95 T\d+/)) nw++;
});
const total = ok + yl + rd + nw;
console.log('Actual count: \u2705' + ok + ' \ud83d\udfe1' + yl + ' \ud83d\udd04' + rd + ' \ud83c\udd95' + nw + ' = ' + total);

// Check stats table
lines.forEach((l, i) => {
    if (l.includes('\u5408\u8ba1') && l.includes('= 160')) {
        console.log('Stats table L' + (i+1) + ': ' + l.substring(0, 80));
    }
});

// Check T5.18 and T12.4 status
lines.forEach((l, i) => {
    if (l.match(/T5\.18/) && l.match(/^\|/)) {
        console.log('T5.18 L' + (i+1) + ': ' + l.substring(0, 40));
    }
    if (l.match(/T12\.4/) && l.match(/^\|/)) {
        console.log('T12.4 L' + (i+1) + ': ' + l.substring(0, 40));
    }
});
