/**
 * Task 4 取证 · 桩实现四件套 + 通知是否统一走 Provider
 * 争议：母文档 M12 note 说 T12.4 🟡（ImagePipelineStub 为桩）但表格标 ✅
 *       M13 note 说 T13.3 🟡（三个 Message*Stub 全为桩）但表格标 ✅
 *       M14 note 说 T14.2 🟡（7/3/1 提醒未走 Provider）但表格标 ✅
 * 第十三章判定口径明列「桩实现按不计为赛道完成」-> 必须以桩的真实实现定性
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

say('===== A) 桩类是否存在 =====');
['ImagePipelineStub', 'ContentSecurityStub', 'WecomIntegrationStub',
 'WechatTemplateMessageStub', 'WechatKfMessageStub', 'WecomAppMessageStub'].forEach(n => {
  const h = byName(new RegExp('^' + n + '\\.java$'));
  say('  ' + (h.length ? '✓' : '✗') + ' ' + n.padEnd(28) + (h.length ? ' -> ' + rel(h[0]) : ' 不存在'));
});

say('');
say('===== B) 桩实现定性（是否真返回假数据 / 全放行）=====');
const inspect = (name, keys) => {
  const h = byName(new RegExp('^' + name + '\\.java$'))[0];
  if (!h) { say('--- ' + name + ' : 不存在'); return; }
  const t = read(h);
  say('--- ' + name + '  (' + rel(h) + ')  行数 ' + t.split(/\r?\n/).length);
  keys.forEach(([k, re]) => say('      ' + ((t.match(re) || []).length ? '✓' : '✗') + ' ' + k.padEnd(38) + ' = ' + (t.match(re) || []).length));
  // 打印 return 语句，直接看是否造假数据
  const rets = t.split(/\r?\n/).map((l, i) => [i + 1, l]).filter(([, l]) => /^\s*return\s/.test(l));
  say('      return 语句 ' + rets.length + ' 条:');
  rets.slice(0, 8).forEach(([n, l]) => say('        L' + n + '| ' + l.trim().slice(0, 110)));
};
inspect('ImagePipelineStub', [['模拟/假 URL 拼接', /x-render-spec|rendered=stub|\+\s*"\?/g], ['真实裁剪 resize/scale', /resize|scale|Thumbnails|BufferedImage|ImageIO/g], ['尺寸容差校验', /0\.05|tolerance|容差/g], ['200KB 体积校验', /200|fileSize|length\s*>/g]]);
inspect('ContentSecurityStub', [['默认放行 PASS', /PASS|pass|true/g], ['真实 imgSecCheck/msgSecCheck 调用', /imgSecCheck|msgSecCheck|HttpUtil|restTemplate|wechat|weixin/g]]);
inspect('WecomIntegrationStub', [['真实企微 API 调用', /qyapi|weixin\.qq|HttpUtil|access_token/g], ['桩返回', /stub|模拟|TODO|not implemented/g]]);
inspect('WechatTemplateMessageStub', [['真实发送调用', /HttpUtil|restTemplate|api\.weixin|cgi-bin/g], ['桩返回/仅记日志', /log\.|stub|模拟|return true/g]]);

say('');
say('===== C) T13.3 通知是否统一走 IMessageProvider =====');
const prov = byName(/^IMessageProvider\.java$/);
say('  IMessageProvider = ' + (prov.length ? rel(prov[0]) : '不存在'));
const disp = byName(/MessageProviderDispatcher/);
say('  MessageProviderDispatcher = ' + (disp.length ? rel(disp[0]) : '不存在'));
// 谁引用了 IMessageProvider / Dispatcher（= 业务侧是否真的改走 Provider）
const consumers = allJava.filter(p => /IMessageProvider|MessageProviderDispatcher/.test(read(p)))
  .filter(p => !/MessageProviderDispatcher\.java$|IMessageProvider\.java$|Stub\.java$/.test(p));
say('  引用 Provider/Dispatcher 的类（排除接口与桩自身）= ' + consumers.length);
consumers.forEach(p => say('      ' + rel(p)));
say('  ★判定要点：T5.12（三层通知）/ T3.4（到期提醒）/ T5.17（三级预警）是否在其中');
['Notify', 'Notice', 'Message', 'Expire', 'Warn', 'Alert', 'Budget', 'Expense'].forEach(k => {
  const hit = consumers.filter(p => path.basename(p).includes(k));
  say('      含 "' + k + '" 的消费方 = ' + hit.length + (hit.length ? ': ' + hit.map(p => path.basename(p, '.java')).join(', ') : ''));
});

say('');
say('===== D) T14.2 到期提醒 7/3/1 天 + 3 天只读宽限期 =====');
const job = allJava.filter(p => /TenantExpire|ExpireCheck|tenantExpire/.test(path.basename(p)));
say('  到期相关类 = ' + job.length);
job.forEach(p => {
  const t = read(p);
  say('      ' + rel(p) + '  行数 ' + t.split(/\r?\n/).length);
  [['7/3/1 天提醒', /\b7\b.*\b3\b.*\b1\b|DAYS_7|day7|reminderDays/g],
   ['宽限期 grace', /grace|宽限|readOnly|只读/g],
   ['走 Provider 发送', /IMessageProvider|MessageProviderDispatcher|messageProvider/g],
   ['直接发站内信/短信', /SysNotice|smsService|sendMessage|MessageService/g]].forEach(([k, re]) =>
    say('        ' + ((t.match(re) || []).length ? '✓' : '✗') + ' ' + k.padEnd(24) + ' = ' + (t.match(re) || []).length));
});
const renewal = allJava.filter(p => /renew|Renew|续费/.test(read(p)));
say('  含「续费/renew」的类 = ' + renewal.length + (renewal.length ? ': ' + renewal.map(p => path.basename(p, '.java')).slice(0, 10).join(', ') : ''));

fs.writeFileSync(ROOT + '/logs/_facts_stub.txt', out.join('\n'), 'utf8');
