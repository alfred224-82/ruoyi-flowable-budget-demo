// Read 初始化项目清单_上海.xlsx and dump all sheets to JSON
const XLSX = require('xlsx');
const fs = require('fs');
const path = require('path');

const file = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '最新整理好的需求', '初始化项目清单_上海.xlsx');
const wb = XLSX.readFile(file);

const out = { sheets: {} };
for (const name of wb.SheetNames) {
  const ws = wb.Sheets[name];
  const rows = XLSX.utils.sheet_to_json(ws, { header: 1, defval: null, raw: false });
  out.sheets[name] = rows;
}

fs.writeFileSync(path.join(__dirname, '_industries_dump.json'), JSON.stringify(out, null, 2), 'utf8');
console.log('sheets:', wb.SheetNames.join(' | '));
for (const name of wb.SheetNames) {
  console.log(name + ': rows=' + out.sheets[name].length);
}
