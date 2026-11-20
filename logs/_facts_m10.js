/**
 * Task 4 取证 · M10 真实残留缺口（T10.2 / T10.3 / T10.4 / T10.7）
 * 母文档 L336 现文：T10.2「无管理页面（ruoyi-ui 无 views/system/bizBundle）」
 *                  T10.3「无开关管理页面」
 * 实测：views/system/{bizBundle,featureFlag,edition}/index.vue + api/system/{bizBundle,featureFlag,edition}.js 均存在
 *      -> 该理由 stale，必须换成真实残留缺口，否则等于用新猜测覆盖旧猜测
 * 取证维度：
 *   T10.2 缓存变更失效：BizBundleServiceImpl 是否有 @Cacheable/@CacheEvict 或本地缓存 + 刷新入口
 *   T10.3 表与常量不一致告警：FeatureFlagServiceImpl 是否比对 EditionFeatureConfig 并告警；EditionFeatureConfig 是否仍为主用
 *   T10.4 tenant_biz_config：是否有 JSON Schema 校验 + 覆盖配置页面
 *   T10.7 normalizeEdition 单测 + 前端下拉/LicenseGenerateTool 是否接受两套编码
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
const allJava = walk(ROOT, (p, n) => /\.java$/.test(n) && !p.includes('\\target\\'));
// 内容缓存：下文有 5 趟全量过滤，不缓存会重复读 1352 个文件
const CACHE = new Map();
const read = p => { if (!CACHE.has(p)) CACHE.set(p, fs.existsSync(p) ? fs.readFileSync(p, 'utf8') : ''); return CACHE.get(p); };
const find = re => allJava.filter(p => re.test(path.basename(p)));
const rel = p => p.replace(/\\/g, '/').split('RuoYi-Flowable-Plus-0.8.X/')[1];
const probe = (label, file, keys) => {
  say('--- ' + label + '  (' + rel(file) + ')');
  const t = read(file);
  if (!t) { say('      !! 文件不存在'); return; }
  say('      行数 = ' + t.split(/\r?\n/).length);
  keys.forEach(([k, re]) => {
    const n = (t.match(re) || []).length;
    say('      ' + (n ? '✓' : '✗') + ' ' + k.padEnd(34) + ' = ' + n);
  });
};

say('===== T10.2 biz_bundle 引擎与缓存 =====');
const bbs = find(/^BizBundleServiceImpl\.java$/)[0];
if (bbs) probe('BizBundleServiceImpl', bbs, [
  ['@Cacheable', /@Cacheable/g], ['@CacheEvict', /@CacheEvict/g],
  ['ConcurrentHashMap 本地缓存', /ConcurrentHashMap/g], ['Caffeine', /Caffeine/g],
  ['refresh/clear/evict 方法', /(refresh|clear|evict|invalidate)\w*\s*\(/g],
  ['getBundle', /getBundle\s*\(/g], ['getFeatureFlags', /getFeatureFlags\s*\(/g],
  ['schema_blocks 读取', /schemaBlocks|schema_blocks/g], ['engine_strategy 读取', /engineStrategy|engine_strategy/g],
  ['copy 文案读取', /\bcopy\b|getCopy/g],
]); else say('  !! BizBundleServiceImpl 未找到');
const bbc = find(/^BizBundleController\.java$/)[0];
if (bbc) probe('BizBundleController', bbc, [
  ['@RequireFeature/@SaCheckPermission', /@SaCheckPermission|@RequireFeature/g],
  ['system:bizBundle perms', /system:bizBundle/g],
  ['刷新缓存端点', /(refresh|clearCache|evict)/g],
  ['端点数(@*Mapping)', /@(Get|Post|Put|Delete)Mapping/g],
]); else say('  !! BizBundleController 未找到');

say('');
say('===== T10.3 feature_flag 表驱动 + 常量兜底告警 =====');
const ffs = find(/^FeatureFlagServiceImpl\.java$/)[0];
if (ffs) probe('FeatureFlagServiceImpl', ffs, [
  ['引用 EditionFeatureConfig', /EditionFeatureConfig/g],
  ['不一致告警 log.warn', /log\.(warn|error)/g],
  ['@Cacheable/@CacheEvict', /@Cache(able|Evict)/g],
  ['按 edition 读默认开启集', /getFlagsByEdition|listByEdition|defaultOn/g],
]); else say('  !! FeatureFlagServiceImpl 未找到');
const efc = find(/^EditionFeatureConfig\.java$/)[0];
if (efc) {
  const t = read(efc);
  say('--- EditionFeatureConfig  (' + rel(efc) + ')  行数 ' + t.split(/\r?\n/).length);
  say('      normalizeEdition = ' + (t.match(/normalizeEdition/g) || []).length);
  say('      @Deprecated = ' + (t.match(/@Deprecated/g) || []).length);
  say('      引用 featureFlagService（表驱动）= ' + (t.match(/[Ff]eatureFlagService/g) || []).length);
  say('      ★被多少类引用（主用常量集？）= ');
  const users = allJava.filter(p => /EditionFeatureConfig/.test(read(p)) && !p.endsWith('EditionFeatureConfig.java'));
  say('        ' + users.length + ' 个类: ' + users.map(p => path.basename(p, '.java')).join(', '));
} else say('  !! EditionFeatureConfig 未找到');
const ffc = find(/^FeatureFlagController\.java$/)[0];
if (ffc) probe('FeatureFlagController', ffc, [
  ['system:featureFlag perms', /system:featureFlag/g],
  ['端点数', /@(Get|Post|Put|Delete)Mapping/g],
]); else say('  !! FeatureFlagController 未找到');

say('');
say('===== T10.4 tenant_biz_config 覆盖 + Schema 校验 =====');
const tbc = find(/TenantBizConfig/);
say('  文件名含 TenantBizConfig 的 .java = ' + tbc.length);
tbc.forEach(p => say('      ' + rel(p)));
const schemaHits = allJava.filter(p => /moduleOverrides|module_overrides/.test(read(p)));
say('  引用 module_overrides/moduleOverrides 的类 = ' + schemaHits.length + (schemaHits.length ? ': ' + schemaHits.map(p => path.basename(p, '.java')).join(', ') : ''));
const jsonSchema = allJava.filter(p => /JsonSchema|json-schema|SchemaValidator|everit|networknt/.test(read(p)));
say('  JSON Schema 校验实现命中 = ' + jsonSchema.length + (jsonSchema.length ? ': ' + jsonSchema.map(p => path.basename(p, '.java')).join(', ') : ' （无）'));
const uiViews = walk(ROOT + '/ruoyi-ui/src/views', (p, n) => /\.vue$/.test(n));
['system/bizBundle', 'system/featureFlag', 'system/edition', 'system/tenantBizConfig', 'system/bizConfig'].forEach(k => {
  const hit = uiViews.filter(p => p.replace(/\\/g, '/').includes(k));
  say('  views/' + k + ' = ' + hit.length + (hit.length ? ' -> ' + hit.map(p => path.basename(p)).join(',') : ''));
});

say('');
say('===== T10.7 normalizeEdition 覆盖面 =====');
const normUsers = allJava.filter(p => /normalizeEdition/.test(read(p)));
say('  调用 normalizeEdition 的类 = ' + normUsers.length);
normUsers.forEach(p => say('      ' + rel(p)));
const tests = walk(ROOT, (p, n) => /\.java$/.test(n) && /src\\test\\/.test(p));
say('  src/test 下 .java 总数 = ' + tests.length);
const normTest = tests.filter(p => /normalizeEdition|EditionFeatureConfig/.test(read(p)));
say('  覆盖 normalizeEdition 的测试类 = ' + normTest.length + (normTest.length ? ': ' + normTest.map(p => path.basename(p)).join(', ') : ' （无 -> 单测缺口成立）'));
const lgt = find(/LicenseGenerateTool/);
say('  LicenseGenerateTool = ' + lgt.length + (lgt.length ? ' -> ' + lgt.map(p => rel(p)).join(', ') : ''));
if (lgt.length) {
  const t = read(lgt[0]);
  say('      含 normalizeEdition = ' + /normalizeEdition/.test(t) + '；含 ENTERPRISE = ' + /ENTERPRISE/.test(t) + '；含 PROFESSIONAL = ' + /PROFESSIONAL/.test(t));
}
const uiEdition = ROOT + '/ruoyi-ui/src/views/system/edition/index.vue';
if (fs.existsSync(uiEdition)) {
  const t = read(uiEdition);
  say('  views/system/edition/index.vue 行数 = ' + t.split(/\r?\n/).length);
  ['ENTERPRISE', 'PROFESSIONAL', 'professional', 'normalize'].forEach(k =>
    say('      含 ' + k + ' = ' + (t.match(new RegExp(k, 'g')) || []).length));
}

fs.writeFileSync(ROOT + '/logs/_facts_m10.txt', out.join('\n'), 'utf8');
