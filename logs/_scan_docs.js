const fs = require('fs');
const path = require('path');

// Check both document paths
const paths = [
    path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', 'newrequest', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md'),
    path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md')
];

for (const p of paths) {
    if (!fs.existsSync(p)) {
        console.log('NOT FOUND: ' + p);
        continue;
    }
    const c = fs.readFileSync(p, 'utf8');
    const lines = c.split('\n');
    console.log('\n=== ' + p + ' (' + lines.length + ' lines) ===');
    
    // Find T2.3, T13.3, T14.2 lines
    lines.forEach((l, i) => {
        if (l.includes('T13.3') || l.includes('T14.2') || l.includes('T2.3')) {
            console.log('L' + (i+1) + ': ' + l.substring(0, 150));
        }
    });
    
    // Find total row
    lines.forEach((l, i) => {
        if (l.includes('**160**') || l.includes('**116**')) {
            console.log('L' + (i+1) + ' [TOTAL]: ' + l.substring(0, 150));
        }
    });
}
