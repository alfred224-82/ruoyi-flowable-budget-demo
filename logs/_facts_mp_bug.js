/**
 * Task 4 取证 · 18 个 micro 页面的「未定义变量」缺陷扫描
 * 起因：bossHome/index.vue L98 解构 [todayRes, trendRes, bookingRes]，L105 却引用 weeklyRes
 *       -> ReferenceError 被 try/catch 吞掉，趋势与待处理提醒静默失效
 * 判定影响：T15.18/T15.19/T15.20 究竟 ✅ 还是 🟡
 *
 * 方法（保守静态分析，只报高置信度项）：
 *  1) 收集每个 <script> 块内的标识符「定义源」：
 *     · const/let/var 声明名（含数组/对象解构展开名）
 *     · 函数参数、data() 返回键、methods 键、computed 键、import 绑定名
 *     · this.xxx（视为已定义）
 *  2) 收集「使用点」：形如 `xxxRes` / `xxxData` / `xxxList` 的裸标识符引用
 *  3) 报出使用点无对应定义源的项
 *  另扫描：config.api.<key> 的 key 是否真实存在于 config/index.js（防拼写漂移）
 */
const fs = require('fs');
const path = require('path');
const APP = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/ruoyi-app';
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

// config/index.js 里 api.* 的合法键名（宽松：抓所有 `key: '/...'` 形式）
const cfg = fs.readFileSync(path.join(APP, 'config/index.js'), 'utf8');
const apiKeys = new Set((cfg.match(/([A-Za-z0-9_]+)\s*:\s*['"`]\//g) || [])
  .map(s => s.split(':')[0].trim()));
say('config/index.js 合法 api 键 = ' + apiKeys.size);
say('');

const KEYWORDS = new Set(['const','let','var','function','return','if','else','for','while','await','async',
  'new','typeof','instanceof','try','catch','finally','throw','switch','case','break','continue','default',
  'this','true','false','null','undefined','in','of','do','delete','void','yield','class','extends','super',
  'import','export','from','as','get','post','put','del','request','config','uni','console','Math','JSON',
  'Object','Array','String','Number','Boolean','Date','Promise','Number','parseInt','parseFloat','isNaN']);

let totalDefects = 0, totalApiMiss = 0;
walk(APP + '/pages/micro', (p, n) => /\.vue$/.test(n)).sort().forEach(p => {
  const raw = fs.readFileSync(p, 'utf8');
  const rel = p.replace(/\\/g, '/').split('/pages/')[1];
  const sm = raw.match(/<script>([\s\S]*?)<\/script>/);
  if (!sm) { say('--- ' + rel + ' : 无 <script>'); return; }
  const lines = raw.split(/\r?\n/);
  const scriptStart = raw.slice(0, sm.index).split(/\r?\n/).length;
  const script = sm[1];

  // 1) 定义源
  const defs = new Set();
  (script.match(/\b(?:const|let|var)\s+([A-Za-z0-9_$]+)/g) || [])
    .forEach(s => defs.add(s.replace(/^(const|let|var)\s+/, '')));
  // 解构：const [a, b, c] = / const { a, b } =
  (script.match(/\b(?:const|let|var)\s*\[([^\]]*)\]/g) || []).forEach(s => {
    s.replace(/^(const|let|var)\s*\[/, '').replace(/\]$/, '').split(',')
      .map(x => x.trim().split(/[:=]/)[0].trim()).filter(Boolean).forEach(x => defs.add(x));
  });
  (script.match(/\b(?:const|let|var)\s*\{([^}]*)\}/g) || []).forEach(s => {
    s.replace(/^(const|let|var)\s*\{/, '').replace(/\}$/, '').split(',')
      .map(x => x.trim().split(/[:=]/).pop().trim()).filter(Boolean).forEach(x => defs.add(x));
  });
  // import 绑定
  (script.match(/import\s+(?:\{([^}]*)\}|([A-Za-z0-9_$]+))\s+from/g) || []).forEach(s => {
    s.replace(/^import\s+/, '').replace(/\s+from$/, '')
      .replace(/[{}]/g, '').split(',').map(x => x.trim().split(/\s+as\s+/).pop().trim())
      .filter(Boolean).forEach(x => defs.add(x));
  });
  // 函数/方法参数 + 方法名 + data 键
  (script.match(/([A-Za-z0-9_$]+)\s*\(([^)]*)\)\s*\{/g) || []).forEach(m => {
    defs.add(m.split('(')[0].trim());
    m.split('(')[1].replace(/\)\s*\{$/, '').split(',').map(x => x.trim().split('=')[0].trim())
      .filter(x => x && !x.startsWith('...')).forEach(x => defs.add(x));
  });
  (script.match(/^\s{4,12}([A-Za-z0-9_$]+)\s*:/gm) || [])
    .forEach(s => defs.add(s.replace(':', '').trim()));
  // async 方法名
  (script.match(/async\s+([A-Za-z0-9_$]+)\s*\(/g) || [])
    .forEach(s => defs.add(s.replace(/^async\s+/, '').replace(/\s*\($/, '')));

  // 2) 使用点：裸标识符（排除属性访问 .xxx、对象键 xxx:、字符串内）
  const defects = [];
  script.split(/\r?\n/).forEach((l, i) => {
    const noStr = l.replace(/(['"`])(?:(?!\1)[^\\]|\\.)*\1/g, '""')
                   .replace(/\/\/.*$/, '').replace(/<!--.*?-->/g, '');
    (noStr.matchAll(/(?<![.\w$])([A-Za-z_$][A-Za-z0-9_$]*)(?!\s*:)(?![\w$])/g)).forEach(m => {
      const id = m[1];
      if (KEYWORDS.has(id) || defs.has(id)) return;
      if (!/(Res|Data|List|Info|Map|Arr|Result|Resp)$/.test(id)) return;  // 只查数据类命名，降低误报
      defects.push('      L' + (scriptStart + i) + ' 未定义引用: ' + id + '   | ' + l.trim());
    });
  });

  // 3) config.api.<key> 拼写核对
  const apiMiss = [];
  (raw.matchAll(/config\.api\.([A-Za-z0-9_$]+)/g)).forEach(m => {
    if (!apiKeys.has(m[1])) apiMiss.push('      config.api.' + m[1] + ' 在 config/index.js 中不存在');
  });
  const apiUse = [...new Set((raw.match(/config\.api\.([A-Za-z0-9_$]+)/g) || []).map(s => s.split('.')[2]))];

  if (defects.length || apiMiss.length) {
    say('--- ' + rel + '  (api 引用 ' + apiUse.length + ' 个)');
    defects.forEach(d => say(d));
    [...new Set(apiMiss)].forEach(d => say(d));
    totalDefects += defects.length; totalApiMiss += new Set(apiMiss).size;
  } else {
    say('OK  ' + rel.padEnd(34) + ' api 引用 ' + apiUse.length + ' 个: ' + apiUse.join(', '));
  }
});

say('');
say('===== 汇总 =====');
say('  未定义引用告警 = ' + totalDefects + ' 处；config.api 拼写缺失 = ' + totalApiMiss + ' 处');
say('  注：本扫描为保守静态分析，KEYWORDS 白名单 + 仅查 *Res/*Data/*List 类命名，');
say('      命中项需人工复核（读上下文确认非误报）后方可作为状态判定依据。');

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_facts_mp_bug.txt', out.join('\n'), 'utf8');
