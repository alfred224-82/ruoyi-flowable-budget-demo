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
        console.log('[SKIP] ' + label + ' - not found');
    }
}

// === Task status flips ===
// T2.3: \ud83d\udd04 \u2192 \u2705
replace('T2.3 main table',
    '| \ud83d\udd04 T2.3 |',
    '| \u2705 T2.3 |');

// T13.3: \ud83d\udfe1 \u2192 \u2705
replace('T13.3 main table',
    '| \ud83d\udfe1 T13.3 |',
    '| \u2705 T13.3 |');

// T14.2: \ud83d\udfe1 \u2192 \u2705
replace('T14.2 main table',
    '| \ud83d\udfe1 T14.2 |',
    '| \u2705 T14.2 |');

// === Statistics section (Section 3) ===
// Total row: \u2705+3(82\u219285), \ud83d\udfe1-2(46\u219244), \ud83d\udd04-1(7\u21926)
replace('Total stats row',
    '| **\u5408\u8ba1** | **160** | **82** | **7** | **46** | **25** |',
    '| **\u5408\u8ba1** | **160** | **85** | **6** | **44** | **25** |');

// M2 row: T2.3 \ud83d\udd04\u2192\u2705: \u2705+1(3\u21924), \ud83d\udd04-1(1\u21920)
replace('M2 stats row',
    '| M2 \u89d2\u8272\u4e0e\u5206\u7248\u672c\u5206\u5c42\u6388\u6743 | 5 | 3 | 1 | 1 | 0 |',
    '| M2 \u89d2\u8272\u4e0e\u5206\u7248\u672c\u5206\u5c42\u6388\u6743 | 5 | 4 | 0 | 1 | 0 |');

// Find M13 and M14 rows dynamically
const lines = content.split('\n');
for (let i = 0; i < lines.length; i++) {
    // M13 row
    if (lines[i].includes('M13 \u8bbf\u95ee\u901a\u9053') && lines[i].includes('|')) {
        const m = lines[i].match(/\| (\d+) \| (\d+) \| (\d+) \| (\d+) \| (\d+) \|/);
        if (m) {
            const oldLine = lines[i];
            const newLine = lines[i].replace(
                '| ' + m[1] + ' | ' + m[2] + ' | ' + m[3] + ' | ' + m[4] + ' | ' + m[5] + ' |',
                '| ' + m[1] + ' | ' + (parseInt(m[2])+1) + ' | ' + (parseInt(m[3])-1) + ' | ' + m[4] + ' | ' + m[5] + ' |'
            );
            lines[i] = newLine;
            changes++;
            console.log('[OK] M13 stats row: ' + m[2] + '\u2192' + (parseInt(m[2])+1) + ', ' + m[3] + '\u2192' + (parseInt(m[3])-1));
        }
    }
    // M14 row
    if (lines[i].includes('M14 \u5546\u4e1a\u5316\u8ba1\u8d39') && lines[i].includes('|')) {
        const m = lines[i].match(/\| (\d+) \| (\d+) \| (\d+) \| (\d+) \| (\d+) \|/);
        if (m) {
            const oldLine = lines[i];
            const newLine = lines[i].replace(
                '| ' + m[1] + ' | ' + m[2] + ' | ' + m[3] + ' | ' + m[4] + ' | ' + m[5] + ' |',
                '| ' + m[1] + ' | ' + (parseInt(m[2])+1) + ' | ' + (parseInt(m[3])-1) + ' | ' + m[4] + ' | ' + m[5] + ' |'
            );
            lines[i] = newLine;
            changes++;
            console.log('[OK] M14 stats row: ' + m[2] + '\u2192' + (parseInt(m[2])+1) + ', ' + m[3] + '\u2192' + (parseInt(m[3])-1));
        }
    }
}
content = lines.join('\n');

// === 10.1 change overview ===
const overviewMatch = content.match(/\| \u2705(\d+)\/\ud83d\udfe1(\d+)\/\ud83d\udd04(\d+)/);
if (overviewMatch) {
    const oldStr = overviewMatch[0];
    const newStr = '| \u2705' + (parseInt(overviewMatch[1])+3) + '/\ud83d\udfe1' + (parseInt(overviewMatch[2])-2) + '/\ud83d\udd04' + (parseInt(overviewMatch[3])-1);
    replace('10.1 overview', oldStr, newStr);
}

// === v1.15 \ud83d\udd04 status distribution note ===
// "\ud83d\udd04 7 \u9879" \u2192 "\ud83d\udd04 6 \u9879"
replace('\ud83d\udd04 count in note',
    '\u2014\u2014 \ud83d\udd04 **7 \u9879**',
    '\u2014\u2014 \ud83d\udd04 **6 \u9879**');

// T2.3 removal from \ud83d\udd04 list
replace('T2.3 in \ud83d\udd04 list',
    'T1.5\u3001T2.3\u3001T4.5',
    'T1.5\u3001T4.5');

// === Section "Section \u4e09" stats table: also update the old stats row ===
// The old total row at L1029 area
replace('Old total stats',
    '| **\u5408\u8ba1** | **160** | **77** | **45** | **13** | **25** |',
    '| **\u5408\u8ba1** | **160** | **85** | **44** | **6** | **25** |');

fs.writeFileSync(docPath, content, 'utf8');
console.log('\n=== Total changes: ' + changes + ' ===');
