/**
 * Task 4 取证 · 桩类真身 + 通知漏斗 + 到期提醒
 * 上一步结论：ContentSecurityStub / Wechat*MessageStub / WecomAppMessageStub 四类【不存在】
 *            -> 母文档 M12/M13 note 点名的是虚构类名，必须换成真身再定性
 * 真身候选：WechatTemplateMessageProvider / WechatKfMessageProvider / WecomAppMessageProvider
 */
const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X';
const out = [];
const say = s => { out.push(s); console.log(s); };
function walk(dir, pred, acc) {
  acc = acc || [];
  if (!fs.existsSync(dir)) return acc;
  fs.readdirSync(dir, { withFileTypes: true }).forEach(e => {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, pred, acc); else if (pred(p, e.name)) acc.push(p);
  });
  return acc;
}
const CACHE = new Map();
const read = p => { if (!CACHE.has(p)) CACHE.set(p, fs.existsSync(p) ? fs.readFileSync(p, 'utf8') : ''); return CACHE.get(p); };
const allJava = walk(ROOT, (p, n) => /\.java$/.test(n) && !p.includes('\\target\\'));
const rel = p => p.replace(/\\/g, '/').split('RuoYi-Flowable-Plus-0.8.X/')[1];
const byName = re => allJava.filter(p => re.test(path.basename(p)));

say('===== A) message 包全貌 =====');
byName(/Message|Provider/).filter(p => p.includes('\\service\\message\\') || /MessageProvider|MessageDispatcher/.test(path.basename(p)))
  .forEach(p => say('  ' + rel(p) + '  (' + read(p).split(/\r?\n/).length + ' 行)'));

say('');
say('===== B) 三个 Provider 是真发送还是桩 =====');
['WechatTemplateMessageProvider', 'WechatKfMessageProvider', 'WecomAppMessageProvider'].forEach(n => {
  const h = byName(new RegExp('^' + n + '\\.java$'))[0];
  if (!h) { say('--- ' + n + ' 不存在'); return; }
  const t = read(h);
  say('--- ' + n + '  (' + rel(h) + ')  行数 ' + t.split(/\r?\n/).length);
  [['真实 HTTP 调用', /HttpUtil|HttpRequest|restTemplate|OkHttp|WebClient|api\.weixin|cgi-bin|qyapi/g],
   ['TODO/未实现/模拟', /TODO|未实现|模拟|stub|Stub|占位|not implemented/g],
   ['仅记日志/直接 return true', /log\.(info|warn)|return true|return false/g],
   ['配置项读取 @Value', /@Value/g]].forEach(([k, re]) =>
    say('      ' + ((t.match(re) || []).length ? '✓' : '✗') + ' ' + k.padEnd(28) + ' = ' + (t.match(re) || []).length));
  t.split(/\r?\n/).forEach((l, i) => {
    if (/TODO|未实现|模拟|stub|Stub|占位/.test(l)) say('      L' + (i + 1) + '| ' + l.trim().slice(0, 120));
  });
});

say('');
say('===== C) 内容安全真身（imgSecCheck / msgSecCheck）=====');
const cs = allJava.filter(p => /imgSecCheck|msgSecCheck|ContentSecurity|contentSecurity/.test(read(p)));
say('  命中文本的类 = ' + cs.length);
cs.forEach(p => say('      ' + rel(p)));
const csName = allJava.filter(p => /ContentSec|SecCheck|Security.*Check/i.test(path.basename(p)));
say('  文件名含 ContentSec/SecCheck 的类 = ' + csName.length + (csName.length ? ': ' + csName.map(p => rel(p)).join(', ') : ' （无 -> 无独立内容安全类）'));

say('');
say('===== D) 通知漏斗：谁调 MessageServiceImpl / ISysMessageService =====');
const msi = byName(/^MessageServiceImpl\.java$/)[0];
if (msi) {
  const t = read(msi);
  say('--- MessageServiceImpl  (' + rel(msi) + ')  行数 ' + t.split(/\r?\n/).length);
  [['调 Dispatcher', /MessageProviderDispatcher|dispatcher/g], ['access_channel 分发', /accessChannel|access_channel/g],
   ['MessageLog 留痕', /MessageLog/g], ['降级不阻塞', /try|catch/g]].forEach(([k, re]) =>
    say('      ' + ((t.match(re) || []).length ? '✓' : '✗') + ' ' + k.padEnd(24) + ' = ' + (t.match(re) || []).length));
  const iface = (t.match(/implements\s+([A-Za-z0-9_,\s]+)/) || [])[1];
  say('      implements = ' + (iface || '?').trim());
}
const iMsg = byName(/^I(Message|SysMessage)Service\.java$/);
say('  消息服务接口 = ' + iMsg.map(p => path.basename(p, '.java')).join(', '));
const msgConsumers = iMsg.length
  ? allJava.filter(p => iMsg.some(i => read(p).includes(path.basename(i, '.java'))) && !p.endsWith('MessageServiceImpl.java'))
  : [];
say('  引用消息服务接口的类 = ' + msgConsumers.length);
msgConsumers.forEach(p => say('      ' + rel(p)));

say('');
say('===== E) T14.2 到期提醒：BillingRemindJob =====');
const brj = byName(/^BillingRemindJob\.java$/)[0];
if (brj) {
  const t = read(brj);
  say('--- BillingRemindJob  (' + rel(brj) + ')  行数 ' + t.split(/\r?\n/).length);
  [['7/3/1 天档位', /\b7\b|\b3\b|\b1\b/g], ['REMIND_DAYS 常量', /REMIND|DAYS|remindDays/g],
   ['走 Provider/MessageService', /MessageProvider|IMessageProvider|MessageService|messageService|dispatcher/g],
   ['SysNotice 站内信', /SysNotice|noticeService/g], ['@TenantJob', /@TenantJob/g],
   ['@XxlJob', /@XxlJob/g]].forEach(([k, re]) =>
    say('      ' + ((t.match(re) || []).length ? '✓' : '✗') + ' ' + k.padEnd(28) + ' = ' + (t.match(re) || []).length));
  t.split(/\r?\n/).forEach((l, i) => {
    if (/REMIND|remind|messageService|MessageService|Provider|notice|Notice/.test(l)) say('      L' + (i + 1) + '| ' + l.trim().slice(0, 120));
  });
} else say('  !! BillingRemindJob 不存在');

say('');
say('===== F) 宽限期只读拦截（T14.2 ③）=====');
const twb = byName(/^TenantWriteBlockInterceptor\.java$/)[0];
if (twb) {
  const t = read(twb);
  say('--- TenantWriteBlockInterceptor  (' + rel(twb) + ')  行数 ' + t.split(/\r?\n/).length);
  [['GRACE 宽限常量', /GRACE|grace|宽限/g], ['写方法拦截 POST/PUT/DELETE', /POST|PUT|DELETE|HttpMethod/g],
   ['注册到 WebMvcConfig', /addInterceptor|WebMvc/g]].forEach(([k, re]) =>
    say('      ' + ((t.match(re) || []).length ? '✓' : '✗') + ' ' + k.padEnd(28) + ' = ' + (t.match(re) || []).length));
} else say('  !! TenantWriteBlockInterceptor 不存在');
const reg = allJava.filter(p => /TenantWriteBlockInterceptor/.test(read(p)) && !p.endsWith('TenantWriteBlockInterceptor.java'));
say('  注册/引用 TenantWriteBlockInterceptor 的类 = ' + reg.length + (reg.length ? ': ' + reg.map(p => path.basename(p, '.java')).join(', ') : '  ★未注册 = 拦截器不生效'));

fs.writeFileSync(ROOT + '/logs/_facts_stub2.txt', out.join('\n'), 'utf8');
