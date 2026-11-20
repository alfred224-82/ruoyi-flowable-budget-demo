/**
 * Task 4 收尾取证：M5（T5.9 隐形成本 / T5.17 报销管控 / T5.18 规则引擎与发票验真）
 *                  M14（T14.2 续费入口 / T14.4 订阅消息授权次数）
 * 目的：母文档 note 与表格互斥，必须以代码证据定谁对
 */
const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X';
const out = [];
const say = s => out.push(s);

function walk(dir, filter, acc) {
  acc = acc || [];
  if (!fs.existsSync(dir)) return acc;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) {
      if (['target', 'node_modules', '.git', 'dist'].includes(e.name)) continue;
      walk(p, filter, acc);
    } else if (filter(p)) acc.push(p);
  }
  return acc;
}
const javaFiles = walk(path.join(ROOT, 'ruoyi-admin/src'), p => p.endsWith('.java'))
  .concat(walk(path.join(ROOT, 'ruoyi-system/src'), p => p.endsWith('.java')))
  .concat(walk(path.join(ROOT, 'ruoyi-job/src'), p => p.endsWith('.java')))
  .concat(walk(path.join(ROOT, 'ruoyi-common/src'), p => p.endsWith('.java')))
  .concat(walk(path.join(ROOT, 'ruoyi-framework/src'), p => p.endsWith('.java')));
const vueFiles = walk(path.join(ROOT, 'ruoyi-ui/src'), p => p.endsWith('.vue') || p.endsWith('.js'));
const read = p => { try { return fs.readFileSync(p, 'utf8'); } catch (e) { return ''; } };
const lines = p => read(p).split(/\r?\n/);
const cnt = (p, re) => (read(p).match(re) || []).length;
const rel = p => p.replace(/\\/g, '/').replace(ROOT + '/', '');

// ---------- Q1 T5.9 隐形成本：四个计提 Job + FEAT_HIDDEN_COST ----------
say('===== Q1) T5.9 隐形成本 =====');
const JOBS = ['salaryAccrualJob', 'assetDepreciationJob', 'loanInterestJob', 'projectSharing'];
JOBS.forEach(k => {
  const hit = javaFiles.filter(p => read(p).includes(k));
  say('  ' + k + ' -> ' + hit.length + ' 个类');
  hit.slice(0, 6).forEach(p => say('      ' + rel(p)));
});
const hid = javaFiles.filter(p => read(p).includes('FEAT_HIDDEN_COST'));
say('  FEAT_HIDDEN_COST 标注类 = ' + hid.length);
hid.forEach(p => {
  const ls = lines(p);
  ls.forEach((l, i) => { if (l.includes('FEAT_HIDDEN_COST')) say('      ' + rel(p) + ' L' + (i + 1) + '| ' + l.trim()); });
});
// 计提相关 Service 是否真实计算
['SalaryAccrual', 'AssetDepreciation', 'LoanInterest', 'ProjectSharing'].forEach(k => {
  const svc = javaFiles.filter(p => path.basename(p).includes(k));
  say('  ' + k + ' 类 = ' + svc.length + ': ' + svc.map(p => path.basename(p) + '(' + lines(p).length + '行)').join(', '));
});

// ---------- Q2 T5.18 发票验真 + 规则反哺 ----------
say('');
say('===== Q2) T5.18 规则引擎匹配链 + 发票验真 =====');
const ivc = javaFiles.filter(p => /InvoiceVerify|InvoiceCheck|InvoiceOcr/i.test(path.basename(p)));
say('  发票验真相关类 = ' + ivc.length);
ivc.forEach(p => say('      ' + rel(p) + ' (' + lines(p).length + ' 行)'));
const ivf = javaFiles.filter(p => read(p).includes('FEAT_INVOICE_VERIFY'));
say('  FEAT_INVOICE_VERIFY 标注类 = ' + ivf.length);
ivf.forEach(p => {
  lines(p).forEach((l, i) => { if (l.includes('FEAT_INVOICE_VERIFY')) say('      ' + rel(p) + ' L' + (i + 1) + '| ' + l.trim()); });
});
say('  --- 规则反哺（财务确认/纠偏写回规则库）---');
const ruleFiles = javaFiles.filter(p => /MappingRule|RuleEngine|SubjectMatch/i.test(path.basename(p)));
ruleFiles.forEach(p => {
  const t = read(p);
  const feedback = ['反哺', 'feedback', 'confirmRule', 'learnFrom', 'updateHitCount', 'hit_count', 'hitCount'].filter(k => t.includes(k));
  say('      ' + rel(p) + ' (' + lines(p).length + ' 行) 反哺信号=' + (feedback.length ? feedback.join('/') : '无'));
});
say('  --- 匹配链路五级（发票号精确/关键字/模糊/金额区间/人工兜底）---');
const matchSvc = javaFiles.filter(p => /SubjectMatch|MappingRuleService|ExpenseMatch/i.test(path.basename(p)));
matchSvc.forEach(p => {
  const t = read(p);
  ['EXACT', 'KEYWORD', 'FUZZY', 'AMOUNT', 'MANUAL'].forEach(k => {
    if (t.includes(k)) say('      ' + path.basename(p) + ' 含 ' + k);
  });
});

// ---------- Q3 T5.17 报销管控 ----------
say('');
say('===== Q3) T5.17 报销管控三级预警 + 追溯时间线 =====');
const eb = javaFiles.filter(p => /ExpenseBill/i.test(path.basename(p)));
eb.forEach(p => {
  const t = read(p);
  say('      ' + rel(p) + ' (' + lines(p).length + ' 行)'
    + ' FEAT_EXPENSE_CONTROL=' + cnt(p, /FEAT_EXPENSE_CONTROL/g)
    + ' 85/95/100阈值=' + cnt(p, /0\.85|0\.95|85|95/g)
    + ' 时间线=' + cnt(p, /timeline|Timeline|追溯/g));
});
const warn = javaFiles.filter(p => read(p).includes('FEAT_EXPENSE_CONTROL'));
say('  FEAT_EXPENSE_CONTROL 标注类 = ' + warn.length + ': ' + warn.map(p => path.basename(p)).join(', '));

// ---------- Q4 T14.2 续费入口 ----------
say('');
say('===== Q4) T14.2 续费入口（PC + 小程序）=====');
const renew = vueFiles.filter(p => /renew|续费/i.test(read(p)) || /renew/i.test(path.basename(p)));
say('  ruoyi-ui 含「续费/renew」的文件 = ' + renew.length);
renew.slice(0, 12).forEach(p => say('      ' + rel(p)));
const billingVue = vueFiles.filter(p => /billing/i.test(p));
say('  ruoyi-ui views 下 billing 相关 = ' + billingVue.length);
billingVue.slice(0, 12).forEach(p => say('      ' + rel(p)));
const appRenew = walk(path.join(ROOT, 'ruoyi-app'), p => p.endsWith('.vue') || p.endsWith('.js'))
  .filter(p => /renew|续费/i.test(read(p)));
say('  ruoyi-app 含「续费/renew」的文件 = ' + appRenew.length);
appRenew.forEach(p => say('      ' + rel(p)));
const bo = javaFiles.filter(p => /BillingOrder|BillingPlan/i.test(path.basename(p)));
say('  计费后端类 = ' + bo.length + ': ' + bo.map(p => path.basename(p) + '(' + lines(p).length + '行)').join(', '));
bo.forEach(p => {
  const t = read(p);
  if (/renew|续费/i.test(t)) {
    lines(p).forEach((l, i) => { if (/renew|续费/i.test(l)) say('      ' + path.basename(p) + ' L' + (i + 1) + '| ' + l.trim().slice(0, 120)); });
  }
});

// ---------- Q5 T14.4 一次性订阅消息授权次数 ----------
say('');
say('===== Q5) T14.4 订阅消息剩余授权次数校验 =====');
const sub = javaFiles.filter(p => /subscribe|Subscribe|订阅/i.test(read(p)) && /message|Message|notify|Notify/i.test(path.basename(p)));
say('  候选类 = ' + sub.length);
sub.slice(0, 10).forEach(p => say('      ' + rel(p) + ' (' + lines(p).length + ' 行)'));
const quota = javaFiles.filter(p => /remaining|剩余次数|authorizeCount|subscribeCount|quota/i.test(read(p)) && /Message|Notify|Subscribe/i.test(path.basename(p)));
say('  含剩余授权次数信号的消息类 = ' + quota.length);
quota.slice(0, 10).forEach(p => say('      ' + rel(p)));

fs.writeFileSync(path.join(ROOT, 'logs/_facts_t4b.txt'), out.join('\n'), 'utf8');
console.log('done, lines=' + out.length);
