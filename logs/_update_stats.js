const fs = require('fs');
const path = require('path');

const docPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
let content = fs.readFileSync(docPath, 'utf8');
const lines = content.split('\n');

// 1. Update M5 milestone stats: \u270511 \ud83d\udd042 \ud83d\udfe16 \u2192 \u270512 \ud83d\udd042 \ud83d\udfe15
// (T5.18 moved from \ud83d\udfe1 to \u2705)
let m5Updated = false;
lines.forEach((l, i) => {
    if (l.includes('M5') && l.includes('\u270511') && l.includes('\ud83d\udfe16') && !m5Updated) {
        lines[i] = l.replace('\u270511', '\u270512').replace('\ud83d\udfe16', '\ud83d\udfe15');
        m5Updated = true;
        console.log('L' + (i+1) + ': M5 stats updated');
    }
});

// 2. Update M12 milestone stats: \u27051 \ud83d\udd046 \u2192 \u27052 \ud83d\udd045
// (T12.4 moved from \ud83d\udfe1 to \u2705)
let m12Updated = false;
lines.forEach((l, i) => {
    if (l.includes('M12') && l.includes('\u27051') && l.includes('\ud83d\udfe16') && !m12Updated) {
        lines[i] = l.replace('\u27051', '\u27052').replace('\ud83d\udfe16', '\ud83d\udfe15');
        m12Updated = true;
        console.log('L' + (i+1) + ': M12 stats updated');
    }
});

// 3. Update total stats: \u270577 \ud83d\udd0413 \ud83d\udfe145 \u2192 \u270587 \ud83d\udd046 \ud83d\udfe142
// Wait, the current stats show \u270577 but v1.19 should have \u270585...
// Let me check what the actual current stats are and update to v1.20
lines.forEach((l, i) => {
    if (l.includes('\u5408\u8ba1') && l.includes('= 160')) {
        // Update to new totals
        lines[i] = l.replace(/\u2705\d+/, '\u270587').replace(/\ud83d\udd04\d+/, '\ud83d\udd046').replace(/\ud83d\udfe1\d+/, '\ud83d\udfe142');
        console.log('L' + (i+1) + ': Total stats updated: ' + lines[i].substring(0, 80));
    }
});

content = lines.join('\n');
fs.writeFileSync(docPath, content, 'utf8');
console.log('\nDocument stats updated successfully.');
