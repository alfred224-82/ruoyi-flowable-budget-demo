/**
 * Task 4 取证 · 收尾三问
 *  Q1 内容安全真身：ContentSecurityBlacklistChecker 是本地黑名单还是真调微信 imgSecCheck/msgSecCheck？
 *  Q2 图片管线：IImagePipeline 是否只有 ImagePipelineStub 一个实现（= 无真实实现）？
 *  Q3 企微：WecomIntegrationStub 是否唯一实现（T13.2 🟡 是否成立）？
 *  Q4 顺带核实 T12.4 验收四要素在真身里的落地情况
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
const dump = (name, limit) => {
  const h = byName(new RegExp('^' + name + '\\.java$'))[0];
  if (!h) { say('--- ' + name + ' 不存在'); return; }
  const t = read(h);
  say('--- ' + name + '  (' + rel(h) + ')  行数 ' + t.split(/\r?\n/).length);
  t.split(/\r?\n/).slice(0, limit || 70).forEach((l, i) => say('  ' + (i + 1) + '| ' + l));
};

say('===== Q1) 内容安全实现清单与定性 =====');
const secImpl = allJava.filter(p => /IContentSecurity\b/.test(read(p)));
say('  引用 IContentSecurity 的类 = ' + secImpl.length);
secImpl.forEach(p => say('      ' + rel(p)));
const impls = allJava.filter(p => /implements\s+.*IContentSecurity/.test(read(p)));
say('  implements IContentSecurity 的实现类 = ' + impls.length + (impls.length ? ': ' + impls.map(p => path.basename(p, '.java')).join(', ') : ''));
dump('ContentSecurityBlacklistChecker', 80);
say('');
say('  --- ContentSecurityServiceImpl 关键行 ---');
const css = byName(/^ContentSecurityServiceImpl\.java$/)[0];
if (css) read(css).split(/\r?\n/).forEach((l, i) => {
  if (/imgSecCheck|msgSecCheck|HttpUtil|api\.weixin|cgi-bin|access_token|IContentSecurity|checker|Checker|@Value|return/.test(l))
    say('      L' + (i + 1) + '| ' + l.trim().slice(0, 130));
});

say('');
say('===== Q2) 图片管线实现清单 =====');
const ipIface = allJava.filter(p => /interface\s+I?ImagePipeline/.test(read(p)));
say('  图片管线接口 = ' + ipIface.length + (ipIface.length ? ': ' + ipIface.map(p => rel(p)).join(', ') : ''));
const ipImpls = allJava.filter(p => /implements\s+.*ImagePipeline/.test(read(p)));
say('  实现类 = ' + ipImpls.length + (ipImpls.length ? ': ' + ipImpls.map(p => path.basename(p, '.java')).join(', ') : ' ★仅桩'));
say('  --- ImagePipelineStub 全文 ---');
dump('ImagePipelineStub', 70);

say('');
say('===== Q3) 企微实现清单（T13.2）=====');
const wxIface = allJava.filter(p => /interface\s+\w*Wecom\w*/.test(read(p)));
say('  企微接口 = ' + wxIface.length + (wxIface.length ? ': ' + wxIface.map(p => rel(p)).join(', ') : ''));
const wxImpls = allJava.filter(p => /implements\s+.*Wecom/.test(read(p)));
say('  实现类 = ' + wxImpls.length + (wxImpls.length ? ': ' + wxImpls.map(p => path.basename(p, '.java')).join(', ') : ''));
say('  --- WecomIntegrationStub 全文 ---');
dump('WecomIntegrationStub', 60);

fs.writeFileSync(ROOT + '/logs/_facts_stub3.txt', out.join('\n'), 'utf8');
