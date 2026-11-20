const fs = require('fs');
const path = require('path');

const docPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '\u6700\u65b0\u6574\u7406\u597d\u7684\u9700\u6c42', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
let content = fs.readFileSync(docPath, 'utf8');
let changes = 0;

function replace(label, old, nw) {
    if (content.includes(old)) {
        content = content.replace(old, nw);
        changes++;
        console.log('[OK] ' + label);
    } else {
        console.log('[SKIP] ' + label);
    }
}

// Fix L564: M13 row with -1 (Section 3 stats)
// Should be: 6 total, 1 done, 0 partial, 4 rework, 1 todo (T13.3 flipped from \ud83d\udfe1\u2192\u2705)
// Original was: 6|0|0|5|1, now should be: 6|1|0|4|1
replace('M13 Section3 fix',
    '| M13 \u8bbf\u95ee\u901a\u9053 | 6 | 1 | -1 | 5 | 1 |',
    '| M13 \u8bbf\u95ee\u901a\u9053 | 6 | 1 | 0 | 4 | 1 |');

// Fix L565: M14 row with -1 (Section 3 stats)
// Original was: 5|0|0|4|1, now should be: 5|1|0|3|1 (T14.2 flipped from \ud83d\udfe1\u2192\u2705)
replace('M14 Section3 fix',
    '| M14 \u5546\u4e1a\u5316\u8ba1\u8d39\u4e0e\u5408\u89c4 | 5 | 1 | -1 | 4 | 1 |',
    '| M14 \u5546\u4e1a\u5316\u8ba1\u8d39\u4e0e\u5408\u89c4 | 5 | 1 | 0 | 3 | 1 |');

// Fix L1029: "Old total stats" was wrongly replaced
// Current (wrong column order): | **\u5408\u8ba1** | **160** | **85** | **44** | **6** | **25** |
// Correct column order: | **\u5408\u8ba1** | **160** | **85** | **6** | **44** | **25** |
replace('L1029 total fix',
    '| **\u5408\u8ba1** | **160** | **85** | **44** | **6** | **25** | \u2014 |',
    '| **\u5408\u8ba1** | **160** | **85** | **6** | **44** | **25** | \u2014 |');

// Also fix M13/M14 detailed stats at L1025/L1026 area
// L1025: M13 was 6|0|4|0|1, should be 6|1|3|0|1 (T13.3 \ud83d\udfe1\u2192\u2705)
// L1026: M14 was 5|0|3|0|1, should be 5|1|2|0|1 (T14.2 \ud83d\udfe1\u2192\u2705)
// Check if they were already updated by the dynamic match
const lines = content.split('\n');
for (let i = 0; i < lines.length; i++) {
    // Fix M13 detailed row if it wasn't updated
    if (lines[i].includes('M13 \u8bbf\u95ee\u901a\u9053') && lines[i].includes('6 | 0 | 4 | 0 | 1')) {
        lines[i] = lines[i].replace('6 | 0 | 4 | 0 | 1', '6 | 1 | 3 | 0 | 1');
        changes++;
        console.log('[OK] M13 detailed row fix at L' + (i+1));
    }
    // Fix M14 detailed row if it wasn't updated
    if (lines[i].includes('M14 \u5546\u4e1a\u5316\u8ba1\u8d39') && lines[i].includes('5 | 0 | 3 | 0 | 1')) {
        lines[i] = lines[i].replace('5 | 0 | 3 | 0 | 1', '5 | 1 | 2 | 0 | 1');
        changes++;
        console.log('[OK] M14 detailed row fix at L' + (i+1));
    }
}
content = lines.join('\n');

fs.writeFileSync(docPath, content, 'utf8');
console.log('\n=== Total fixes: ' + changes + ' ===');
