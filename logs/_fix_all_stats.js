const fs = require('fs');
const path = require('path');

const docPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
let content = fs.readFileSync(docPath, 'utf8');
const lines = content.split('\n');

let changes = 0;

lines.forEach((l, i) => {
    // M5 stats: \u270511 \ud83d\udfe12 \u2192 \u270512 \ud83d\udfe11
    if (l.startsWith('M5') && l.includes('\u270511') && l.includes('\ud83d\udfe12')) {
        lines[i] = l.replace('\u270511', '\u270512').replace('\ud83d\udfe12', '\ud83d\udfe11');
        changes++;
        console.log('L' + (i+1) + ': M5 \u2192 ' + lines[i].substring(0, 40));
    }
    // M12 stats: \u27051 \ud83d\udfe16 \u2192 \u27052 \ud83d\udfe15
    if (l.startsWith('M12') && l.includes('\u27051') && l.includes('\ud83d\udfe16')) {
        lines[i] = l.replace('\u27051', '\u27052').replace('\ud83d\udfe16', '\ud83d\udfe15');
        changes++;
        console.log('L' + (i+1) + ': M12 \u2192 ' + lines[i].substring(0, 40));
    }
    // Total stats: \u5408\u8ba1 \u270577 \ud83d\udfe145 \ud83d\udd0413 \u2192 \u5408\u8ba1 \u270587 \ud83d\udfe142 \ud83d\udd046
    if (l.includes('\u5408\u8ba1') && l.includes('= 160')) {
        lines[i] = l
            .replace(/\u270577/, '\u270587')
            .replace(/\ud83d\udfe145/, '\ud83d\udfe142')
            .replace(/\ud83d\udd0413/, '\ud83d\udd046');
        changes++;
        console.log('L' + (i+1) + ': Total \u2192 ' + lines[i].substring(0, 80));
    }
});

content = lines.join('\n');
fs.writeFileSync(docPath, content, 'utf8');
console.log('\nTotal changes: ' + changes);

// Verify
const verify = fs.readFileSync(docPath, 'utf8');
const vlines = verify.split('\n');
vlines.forEach((l, i) => {
    if (l.startsWith('M5 ') || l.startsWith('M12') || (l.includes('\u5408\u8ba1') && l.includes('= 160'))) {
        console.log('Verify L' + (i+1) + ': ' + l.substring(0, 80));
    }
});
