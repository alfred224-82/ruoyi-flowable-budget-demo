const fs = require('fs');
const path = require('path');

const docPath = path.join(__dirname, '..', 'docs', 'newrequest', '\u8d44\u91d1\u7ba1\u63a7\u7cfb\u7edf\u5f00\u53d1\u4efb\u52a1\u6587\u6863.md');
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

// T13.3: \ud83d\udfe1 \u2192 \u2705
replace('T13.3 status',
    '| \ud83d\udfe1 T13.3 |',
    '| \u2705 T13.3 |');

// T14.2: \ud83d\udfe1 \u2192 \u2705
replace('T14.2 status',
    '| \ud83d\udfe1 T14.2 |',
    '| \u2705 T14.2 |');

// T2.3: \ud83d\udd04 \u2192 \u2705
replace('T2.3 status',
    '| \ud83d\udd04 T2.3 |',
    '| \u2705 T2.3 |');

// Update statistics - need to find current numbers first
// Read current stats
const totalMatch = content.match(/\| \*\*(\d+)\*\* \| \*\*(\d+)\*\* \| \*\*(\d+)\*\* \| \*\*(\d+)\*\* \| \*\*(\d+)\*\* \|/);
if (totalMatch) {
    const total = parseInt(totalMatch[1]);
    const done = parseInt(totalMatch[2]);
    const partial = parseInt(totalMatch[3]);
    const rework = parseInt(totalMatch[4]);
    const todo = parseInt(totalMatch[5]);
    
    console.log('\n--- Current stats ---');
    console.log('Total: ' + total + ', Done: ' + done + ', Partial: ' + partial + ', Rework: ' + rework + ', Todo: ' + todo);
    
    // 3 items flip: T13.3(\ud83d\udfe1\u2192\u2705), T14.2(\ud83d\udfe1\u2192\u2705), T2.3(\ud83d\udd04\u2192\u2705)
    // Net: \u2705+3, \ud83d\udfe1-2, \ud83d\udd04-1
    const newDone = done + 3;
    const newPartial = partial - 2;
    const newRework = rework - 1;
    
    console.log('New: Done: ' + newDone + ', Partial: ' + newPartial + ', Rework: ' + newRework + ', Todo: ' + todo);
    console.log('Sum check: ' + (newDone + newPartial + newRework + todo) + ' (should be ' + total + ')');
    
    // Update total row
    const oldTotal = '| **' + total + '** | **' + done + '** | **' + partial + '** | **' + rework + '** | **' + todo + '** |';
    const newTotal = '| **' + total + '** | **' + newDone + '** | **' + newPartial + '** | **' + newRework + '** | **' + todo + '** |';
    replace('Total row', oldTotal, newTotal);
}

// Update M2 row (T2.3 \ud83d\udd04\u2192\u2705)
replace('M2 row',
    '| 6 | 3 | 1 | 1 | 1 |',
    '| 6 | 4 | 1 | 0 | 1 |');

// Update M13 row (T13.3 \ud83d\udfe1\u2192\u2705)
// Need to find M13 row pattern - search for it
const m13Match = content.match(/\| (\d+) \| (\d+) \| (\d+) \| (\d+) \| (\d+) \|.*M13/s);
if (m13Match) {
    const oldM13 = m13Match[0].replace(/\n.*/, '');
    const m13total = parseInt(m13Match[1]);
    const m13done = parseInt(m13Match[2]);
    const m13partial = parseInt(m13Match[3]);
    const m13rework = parseInt(m13Match[4]);
    const m13todo = parseInt(m13Match[5]);
    console.log('\nM13 current: ' + m13total + '/' + m13done + '/' + m13partial + '/' + m13rework + '/' + m13todo);
    
    // T13.3: \ud83d\udfe1\u2192\u2705
    const newM13 = '| ' + m13total + ' | ' + (m13done+1) + ' | ' + (m13partial-1) + ' | ' + m13rework + ' | ' + m13todo + ' |';
    replace('M13 row', oldM13, newM13);
}

// Update M14 row (T14.2 \ud83d\udfe1\u2192\u2705)
const m14Match = content.match(/\| (\d+) \| (\d+) \| (\d+) \| (\d+) \| (\d+) \|.*M14/s);
if (m14Match) {
    const oldM14 = m14Match[0].replace(/\n.*/, '');
    const m14total = parseInt(m14Match[1]);
    const m14done = parseInt(m14Match[2]);
    const m14partial = parseInt(m14Match[3]);
    const m14rework = parseInt(m14Match[4]);
    const m14todo = parseInt(m14Match[5]);
    console.log('\nM14 current: ' + m14total + '/' + m14done + '/' + m14partial + '/' + m14rework + '/' + m14todo);
    
    // T14.2: \ud83d\udfe1\u2192\u2705
    const newM14 = '| ' + m14total + ' | ' + (m14done+1) + ' | ' + (m14partial-1) + ' | ' + m14rework + ' | ' + m14todo + ' |';
    replace('M14 row', oldM14, newM14);
}

// Update 10.1 change overview if exists
const overviewMatch = content.match(/\| \u2705(\d+)\/\ud83d\udfe1(\d+)\/\ud83d\udd04(\d+)/);
if (overviewMatch) {
    const ovDone = parseInt(overviewMatch[1]);
    const ovPartial = parseInt(overviewMatch[2]);
    const ovRework = parseInt(overviewMatch[3]);
    const oldOverview = overviewMatch[0];
    const newOverview = '| \u2705' + (ovDone+3) + '/\ud83d\udfe1' + (ovPartial-2) + '/\ud83d\udd04' + (ovRework-1);
    replace('10.1 overview', oldOverview, newOverview);
}

fs.writeFileSync(docPath, content, 'utf8');
console.log('\n=== Total changes: ' + changes + ' ===');
