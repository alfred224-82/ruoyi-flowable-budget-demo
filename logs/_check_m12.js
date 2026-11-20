const fs = require('fs');
const path = require('path');

const docPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
const content = fs.readFileSync(docPath, 'utf8');
const lines = content.split('\n');

// Check M12 line
lines.forEach((l, i) => {
    if (l.startsWith('M12')) {
        console.log('L' + (i+1) + ': [' + l.substring(0, 60) + ']');
        // Show char codes
        const chars = l.substring(0, 30).split('');
        chars.forEach((c, j) => {
            if (c.charCodeAt(0) > 127) {
                console.log('  pos ' + j + ': char=' + c + ' code=' + c.charCodeAt(0).toString(16));
            }
        });
    }
});
