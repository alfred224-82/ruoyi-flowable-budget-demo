const fs = require('fs');
const path = require('path');

const docPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
const content = fs.readFileSync(docPath, 'utf8');
const lines = content.split('\n');

// Find all \ud83d\udd04 and \ud83d\udfe1 tasks
console.log('=== \ud83d\udd04 Rework tasks ===');
lines.forEach((l, i) => {
    if (l.match(/^\| \ud83d\udd04 T\d+/)) {
        console.log('L' + (i+1) + ': ' + l.substring(0, 160));
    }
});

console.log('\n=== \ud83d\udfe1 Partial tasks (backend only, excluding mini-program/frontend) ===');
lines.forEach((l, i) => {
    if (l.match(/^\| \ud83d\udfe1 T\d+/)) {
        // Exclude mini-program tasks (T8.x with \u5c0f\u7a0b\u5e8f)
        if (l.includes('\u5c0f\u7a0b\u5e8f') && !l.includes('PC')) {
            return;
        }
        console.log('L' + (i+1) + ': ' + l.substring(0, 180));
    }
});
