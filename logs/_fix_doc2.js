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

// Fix L1013: M2 detailed stats - T2.3 \ud83d\udd04\u2192\u2705
// Current: 5 | 3 | 1 | 1 | 0 => Should be: 5 | 4 | 1 | 0 | 0
replace('M2 detailed stats',
    '| M2 \u89d2\u8272\u4e0e\u5206\u7248\u672c\u5206\u5c42\u6388\u6743 | 5 | 3 | 1 | 1 | 0 |',
    '| M2 \u89d2\u8272\u4e0e\u5206\u7248\u672c\u5206\u5c42\u6388\u6743 | 5 | 4 | 1 | 0 | 0 |');

// Fix L1025: M13 detailed stats - T13.3 \ud83d\udfe1\u2192\u2705
// Current: 6 | 1 | 4 | 0 | 1 => Should be: 6 | 1 | 3 | 0 | 1
// Wait, the dynamic match already changed 0\u21921 for \u2705 and 4\u21923 for \ud83d\udfe1
// But the output shows 6 | 1 | 4 | 0 | 1 - meaning it was NOT changed
// So the original was 6 | 0 | 4 | 0 | 1, and T13.3\u2192\u2705 means \u2705+1, \ud83d\udfe1-1
// Should be: 6 | 1 | 3 | 0 | 1
replace('M13 detailed stats',
    '| M13 \u8bbf\u95ee\u901a\u9053 | 6 | 1 | 4 | 0 | 1 |',
    '| M13 \u8bbf\u95ee\u901a\u9053 | 6 | 1 | 3 | 0 | 1 |');

// Fix L1026: M14 detailed stats - T14.2 \ud83d\udfe1\u2192\u2705
// Current: 5 | 1 | 3 | 0 | 1 => Should be: 5 | 1 | 2 | 0 | 1
// Wait, it shows 5 | 1 | 3 | 0 | 1. The dynamic match changed 0\u21921 for \u2705 but not 3\u21922
// Actually the original was 5 | 0 | 3 | 0 | 1. T14.2\u2192\u2705: \u2705+1(0\u21921), \ud83d\udfe1-1(3\u21922)
// So should be: 5 | 1 | 2 | 0 | 1
replace('M14 detailed stats',
    '| M14 \u5546\u4e1a\u5316\u8ba1\u8d39\u4e0e\u5408\u89c4 | 5 | 1 | 3 | 0 | 1 |',
    '| M14 \u5546\u4e1a\u5316\u8ba1\u8d39\u4e0e\u5408\u89c4 | 5 | 1 | 2 | 0 | 1 |');

// Also update the note text at L1013 area - T2.3 should be removed from the "not done" note
// "T2.3 `SysDataScopeServiceImpl` \u672a\u5f15\u7528\u4e94\u679a\u4e3e" => now it's done
replace('M2 note T2.3',
    'T2.2 \u4e1a\u6001\u8fc7\u6ee4\u5c42\u672a\u53e0\u52a0\u8fdb\u83dc\u5355\u6811\uff1bT2.3 `SysDataScopeServiceImpl` \u672a\u5f15\u7528\u4e94\u679a\u4e3e',
    'T2.2 \u4e1a\u6001\u8fc7\u6ee4\u5c42\u672a\u53e0\u52a0\u8fdb\u83dc\u5355\u6811\uff1bT2.3 \u5df2\u5b8c\u6210\u9879\u76ee\u7ef4\u5ea6\u6570\u636e\u8303\u56f4\u8fc7\u6ee4');

// Update M13 note - stubs replaced with real providers
replace('M13 note stubs',
    '`WecomIntegrationStub` + \u4e09\u4e2a `Message*Stub` \u5168\u4e3a\u6869',
    '\u4e09\u4e2a MessageProvider \u5df2\u5347\u7ea7\u4e3a\u771f\u5b9e HTTP \u5b9e\u73b0');

fs.writeFileSync(docPath, content, 'utf8');
console.log('\n=== Total fixes: ' + changes + ' ===');
