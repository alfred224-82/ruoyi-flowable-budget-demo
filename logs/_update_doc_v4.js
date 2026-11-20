const fs = require('fs');
const path = require('path');

const docPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
let content = fs.readFileSync(docPath, 'utf8');
const lines = content.split('\n');

// 1. Update T5.18 status: \ud83d\udfe1 \u2192 \u2705
let t518Count = 0;
lines.forEach((l, i) => {
    if (l.match(/^\| \ud83d\udfe1 T5\.18/)) {
        lines[i] = l.replace('| \ud83d\udfe1 T5.18', '| \u2705 T5.18');
        t518Count++;
        console.log('L' + (i+1) + ': T5.18 \ud83d\udfe1\u2192\u2705');
    }
});
console.log('T5.18 replacements: ' + t518Count);

// 2. Update T12.4 status: \ud83d\udfe1 \u2192 \u2705
let t124Count = 0;
lines.forEach((l, i) => {
    if (l.match(/^\| \ud83d\udfe1 T12\.4/)) {
        lines[i] = l.replace('| \ud83d\udfe1 T12.4', '| \u2705 T12.4');
        t124Count++;
        console.log('L' + (i+1) + ': T12.4 \ud83d\udfe1\u2192\u2705');
    }
});
console.log('T12.4 replacements: ' + t124Count);

// 3. Update statistics
// Old: \u270585/\ud83d\udd046/\ud83d\udfe144/\ud83d\udd340/\ud83c\udd9525=160
// New: \u270587/\ud83d\udd046/\ud83d\udfe142/\ud83d\udd340/\ud83c\udd9525=160
const oldStats = '\u270585/\ud83d\udd046/\ud83d\udfe144/\ud83d\udd340/\ud83c\udd9525=160';
const newStats = '\u270587/\ud83d\udd046/\ud83d\udfe142/\ud83d\udd340/\ud83c\udd9525=160';
let statsCount = 0;
lines.forEach((l, i) => {
    if (l.includes(oldStats)) {
        lines[i] = l.replace(oldStats, newStats);
        statsCount++;
        console.log('L' + (i+1) + ': Stats updated');
    }
});
console.log('Stats replacements: ' + statsCount);

// 4. Update note text
// Old note: "T5.18\u53cd\u54d1\u89c4\u5219\u5e93\u672a\u5b9e\u73b0"
// New note: "T5.18\u53cd\u54d1\u89c4\u5219\u5e93\u672a\u5b9e\u73b0" \u2192 remove from note
let noteUpdated = false;
lines.forEach((l, i) => {
    if (l.includes('v1.19') && l.includes('T5.18') && !noteUpdated) {
        // Find the note line with v1.19 and T5.18
        if (l.includes('T5.18\u53cd\u54d1\u89c4\u5219\u5e93\u672a\u5b9e\u73b0')) {
            lines[i] = l.replace('T5.18\u53cd\u54d1\u89c4\u5219\u5e93\u672a\u5b9e\u73b0', 'T5.18\u53cd\u54d1\u89c4\u5219\u5e93\u672a\u5b9e\u73b0\u2192\u5df2\u8865\u9f50Controller+\u95e8\u63a7');
            noteUpdated = true;
            console.log('L' + (i+1) + ': Note updated for T5.18');
        }
    }
});

content = lines.join('\n');
fs.writeFileSync(docPath, content, 'utf8');
console.log('\nDocument updated successfully.');
