const fs = require('fs');
const path = require('path');

const docPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
const content = fs.readFileSync(docPath, 'utf8');
const lines = content.split('\n');

// Find lines containing M12
lines.forEach((l, i) => {
    if (l.includes('M12') && l.includes('= 8')) {
        console.log('L' + (i+1) + ': ' + JSON.stringify(l.substring(0, 80)));
    }
});
