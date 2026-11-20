const XLSX = require('xlsx');
const path = require('path');
const filePath = path.join(__dirname, '..', 'docs', '最新整理好的需求', '初始化项目清单_上海.xlsx');
console.log('Reading:', filePath);
const wb = XLSX.readFile(filePath);
console.log('Sheets:', wb.SheetNames);
wb.SheetNames.forEach(name => {
  const ws = wb.Sheets[name];
  const data = XLSX.utils.sheet_to_csv(ws, { FS: '\t', RS: '\n' });
  console.log(`\n=== Sheet: ${name} ===`);
  console.log(data);
});
