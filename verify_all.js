const fs = require('fs');
const path = require('path');
const base = path.join('D:\\workspace\\RuoYi-Flowable-Plus-0.8.X\\docs', '\u6700\u65B0\u6574\u7406\u597D\u7684\u9700\u6C42');

let pass = 0, fail = 0;
function check(label, condition) {
  if (condition) { pass++; console.log('  OK: ' + label); }
  else { fail++; console.log('  FAIL: ' + label); }
}

// ===== 1. 小微版操作手册 =====
console.log('\n=== \u5C0F\u5FAE\u7248\u64CD\u4F5C\u624B\u518C ===');
{
  const f = path.join(base, '\u5C0F\u5FAE\u7248', '04_\u5C0F\u5FAE\u7248\u64CD\u4F5C\u624B\u518C.md');
  const c = fs.readFileSync(f, 'utf8');
  check('M17 eval section exists', c.indexOf('\u901A\u7528\u8BC4\u4EF7\u4E0E\u70B9\u8D5E') >= 0);
  check('FEAT_RATING mentioned', c.indexOf('FEAT_RATING') >= 0);
  check('user_rating table', c.indexOf('user_rating') >= 0);
  check('user_like table', c.indexOf('user_like') >= 0);
  check('FAQ 8.7 eval', c.indexOf('8.7 \u8BC4\u4EF7\u76F8\u5173') >= 0);
  check('FAQ 8.8 \u5206\u6863\u5347\u7EA7', c.indexOf('8.8 \u5206\u6863\u5347\u7EA7') >= 0);
  check('M17_01 in SQL index', c.indexOf('M17_01') >= 0);
  check('footer v1.4', c.indexOf('v1.4') >= 0);
  check('footer date 2026-09-16', c.indexOf('2026-09-16') >= 0);
}

// ===== 2. 基础版开发文档 =====
console.log('\n=== \u57FA\u7840\u7248\u5F00\u53D1\u6587\u6863 ===');
{
  const f = path.join(base, '\u57FA\u7840\u7248', '\u8D44\u91D1\u7BA1\u63A7\u7CFB\u7EDF\u5F00\u53D1\u4EFB\u52A1\u6587\u6863\uFF08\u57FA\u7840\u7248\uFF09.md');
  const c = fs.readFileSync(f, 'utf8');
  check('version v1.24', c.indexOf('v1.24') >= 0);
  check('date 2026-09-16', c.indexOf('2026-09-16') >= 0);
  check('M16/M17 not deployed', c.indexOf('M16/M17 \u4E0D\u90E8\u7F72') >= 0);
  check('FEAT_RATING mentioned', c.indexOf('FEAT_RATING') >= 0);
  check('anti-cross 35', c.indexOf('35 \u6761') >= 0);
  check('no more 34条 anti-cross', c.indexOf('\u9632\u4E32\u6D4B\u8BD5\u57FA\u7EBF\uFF0834 \u6761') < 0);
}

// ===== 3. 基础版操作手册 =====
console.log('\n=== \u57FA\u7840\u7248\u64CD\u4F5C\u624B\u518C ===');
{
  const f = path.join(base, '\u57FA\u7840\u7248', '01_\u57FA\u7840\u7248\u64CD\u4F5C\u624B\u518C.md');
  const c = fs.readFileSync(f, 'utf8');
  check('version v2.1', c.indexOf('v2.1') >= 0);
  check('date 2026-09-16', c.indexOf('2026-09-16') >= 0);
  check('M17 exclusion row', c.indexOf('\u901A\u7528\u8BC4\u4EF7\u4E0E\u70B9\u8D5E') >= 0);
  check('FEAT_RATING', c.indexOf('FEAT_RATING') >= 0);
  check('script count 33', c.indexOf('33 \u4E2A\u811A\u672C') >= 0 || c.indexOf('33 \u811A\u672C') >= 0);
  check('footer v2.1', c.indexOf('\u6587\u6863\u7248\u672C**\uFF1Av2.1') >= 0);
}

// ===== 4. 高级版开发文档 =====
console.log('\n=== \u9AD8\u7EA7\u7248\u5F00\u53D1\u6587\u6863 ===');
{
  const f = path.join(base, '\u9AD8\u7EA7\u7248', '\u8D44\u91D1\u7BA1\u63A7\u7CFB\u7EDF\u5F00\u53D1\u4EFB\u52A1\u6587\u6863\uFF08\u9AD8\u7EA7\u7248\uFF09.md');
  const c = fs.readFileSync(f, 'utf8');
  check('version v1.24', c.indexOf('v1.24') >= 0);
  check('date 2026-09-16', c.indexOf('2026-09-16') >= 0);
  check('M17 not deployed', c.indexOf('M17 \u4E0D\u90E8\u7F72') >= 0);
  check('FEAT_RATING', c.indexOf('FEAT_RATING') >= 0);
}

// ===== 5. 高级版操作手册 =====
console.log('\n=== \u9AD8\u7EA7\u7248\u64CD\u4F5C\u624B\u518C ===');
{
  const f = path.join(base, '\u9AD8\u7EA7\u7248', '02_\u9AD8\u7EA7\u7248\u64CD\u4F5C\u624B\u518C.md');
  const c = fs.readFileSync(f, 'utf8');
  check('header v1.24 ref', c.indexOf('v1.24') >= 0);
  check('date 2026-09-16', c.indexOf('2026-09-16') >= 0);
  check('M17 exclusion row', c.indexOf('M17 \u901A\u7528\u8BC4\u4EF7') >= 0);
  check('FEAT_RATING disabled', c.indexOf('FEAT_RATING') >= 0);
  check('footer v2.1', c.indexOf('\u6587\u6863\u7248\u672C**\uFF1Av2.1') >= 0);
}

// ===== 6. 专业版开发文档 =====
console.log('\n=== \u4E13\u4E1A\u7248\u5F00\u53D1\u6587\u6863 ===');
{
  const f = path.join(base, '\u4E13\u4E1A\u7248', '\u8D44\u91D1\u7BA1\u63A7\u7CFB\u7EDF\u5F00\u53D1\u4EFB\u52A1\u6587\u6863\uFF08\u4E13\u4E1A\u7248\uFF09.md');
  const c = fs.readFileSync(f, 'utf8');
  check('version v1.24', c.indexOf('v1.24') >= 0);
  check('date 2026-09-16', c.indexOf('2026-09-16') >= 0);
  check('M17 not deployed', c.indexOf('M17 \u4E0D\u90E8\u7F72') >= 0);
  check('FEAT_RATING', c.indexOf('FEAT_RATING') >= 0);
  check('35 switches', c.indexOf('35 \u9879\u5F00\u5173') >= 0);
}

// ===== 7. 专业版操作手册 =====
console.log('\n=== \u4E13\u4E1A\u7248\u64CD\u4F5C\u624B\u518C ===');
{
  const f = path.join(base, '\u4E13\u4E1A\u7248', '03_\u4E13\u4E1A\u7248\u64CD\u4F5C\u624B\u518C.md');
  const c = fs.readFileSync(f, 'utf8');
  check('header v1.24 ref', c.indexOf('v1.24') >= 0);
  check('date 2026-09-16', c.indexOf('2026-09-16') >= 0);
  check('M17 exclusion in feature table', c.indexOf('M17 \u901A\u7528\u8BC4\u4EF7') >= 0);
  check('M17 in comparison table', c.indexOf('\u4EC5\u5C0F\u5FAE\u7248\u4ED8\u8D39\u7248') >= 0);
  check('FEAT_RATING disabled', c.indexOf('FEAT_RATING') >= 0);
  check('footer v2.1', c.indexOf('\u6587\u6863\u7248\u672C**\uFF1Av2.1') >= 0);
}

console.log('\n=============================');
console.log('PASS: ' + pass + ' / FAIL: ' + fail);
if (fail === 0) console.log('ALL CHECKS PASSED!');
