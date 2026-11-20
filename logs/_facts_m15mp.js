/**
 * Task 4 取证 · M15 小程序赛道逐条验收（决定 T15.8/12/14/15/16/18/19/20/28/30/32 的 ✅/🟡）
 * 关键张力：母文档 L428 核心约束⑥「小程序页面 = 单一页面壳 + 区块配置(page_block_config)，
 *          不存在 boss/home、salesman/home 等多套页面」
 *          实测落盘 = micro/bossHome + micro/staffHome + micro/customerHome 三套独立页面 -> 架构口径偏离
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
const appFiles = walk(APP, (p, n) => /\.(vue|js|json)$/.test(n) && !p.includes('node_modules') && !p.includes('unpackage'));
const read = p => fs.readFileSync(p, 'utf8');
const rel = p => p.replace(/\\/g, '/').replace(APP + '/', '');
// 全仓命中：返回 [文件, 行号, 行文本]
const grep = (re, files) => {
  const hits = [];
  (files || appFiles).forEach(p => {
    read(p).split(/\r?\n/).forEach((l, i) => { if (re.test(l)) hits.push([rel(p), i + 1, l.trim()]); });
  });
  return hits;
};
const show = (label, re, limit) => {
  const h = grep(re);
  say('--- ' + label + '  命中 ' + h.length + ' 处');
  h.slice(0, limit || 12).forEach(x => say('      ' + x[0] + ':' + x[1] + '| ' + x[2].slice(0, 118)));
  if (h.length > (limit || 12)) say('      ... 余 ' + (h.length - (limit || 12)) + ' 处');
};

say('===== 1) T15.15 ② client_type=mini 菜单过滤（小程序侧是否消费）=====');
show('client_type / clientType', /client_?[Tt]ype/);
show('menu 树 / perms 拉取', /(getRouters|getMenus|listMenu|perms)/, 10);

say('');
say('===== 2) T15.15 ③ 租户号带入（扫码进店 scene / 分享链接 ?tenantId=）=====');
show('scene / q 参数解析', /\bscene\b|onScanCode|decodeURIComponent/);
show('tenantId 入参', /tenantId/, 14);

say('');
say('===== 3) T15.16 单一页面壳 + 区块配置（page_block_config / page/config）=====');
show('/page/config 接口调用', /page\/config|pageConfig|blockKey|block_key|page_block/);
show('区块组件库（component is 动态渲染）', /<component\s+:is|blockComponent/);
const cfg = read(path.join(APP, 'config/index.js'));
say('  config/index.js 含 pageConfig 键 = ' + /pageConfig/.test(cfg));

say('');
say('===== 4) T15.20 定价管理快改（改价格 + 写 price_change_log）=====');
show('micro:service / 定价快改', /microService|serviceList|priceChange|changePrice|改价/);
const shopCfg = read(path.join(APP, 'pages/micro/shop/config.vue'));
say('  micro/shop/config.vue 行数 = ' + shopCfg.split(/\r?\n/).length);
say('  其中 api 调用 = ' + [...new Set((shopCfg.match(/config\.api\.([A-Za-z0-9_$]+)/g) || []).map(s => s.split('.')[2]))].join(', '));
say('  含「价格」编辑 = ' + /price|价格|定价/.test(shopCfg));

say('');
say('===== 5) T15.8 wx.login + getPhoneNumber 一键登录 =====');
show('wx.login / uni.login', /uni\.login|wx\.login|xcxLogin/);
show('getPhoneNumber', /getPhoneNumber|phoneNumber|open-type/);

say('');
say('===== 6) T15.32 小程序侧黑名单（按钮置灰 + 请联系商家）=====');
show('is_blacklisted / blacklisted', /[Bb]lacklist/);
show('请联系商家 / 置灰 disabled', /请联系商家|disabled|置灰/, 10);

say('');
say('===== 7) T15.30 第③层渲染 = 天花板 ∩ 排版 =====');
show('filterByCeiling / ceiling', /[Cc]eiling/);

say('');
say('===== 8) T15.18/T15.19 关键 perms 对应页面是否齐备 =====');
const need = {
  'T15.18 我的排班': /microStaffSchedule/,
  'T15.18 我的收入': /microStaffIncome/,
  'T15.18 打卡提交': /microStaffCheckinSubmit/,
  'T15.19 浏览店铺': /microCustomerShop/,
  'T15.19 提交预约': /microCustomerSubmitBook/,
  'T15.19 我的预约': /microCustomerMyBook/,
  'T15.19 取消预约': /microCustomerCancelBook/,
  'T15.19 收款方式提示': /microCustomerPayInfo/,
  'T15.20 经营周报': /microReportWeekly/,
  'T15.20 月报': /microReportMonthly/,
  'T15.20 资金水位/现金流': /microReportCashFlow/,
  'T15.20 消息提醒': /microNotifyList/,
  'T15.28 活动浏览': /microActivityBrowse/,
  'T15.12 顾客预约(同上)': /microCustomerSubmitBook/,
  'T15.14 小程序报表(同上)': /microReportTrend/,
};
Object.entries(need).forEach(([k, re]) => {
  const h = grep(re);
  say('  ' + (h.length ? '✓' : '✗') + ' ' + k.padEnd(26) + ' 命中 ' + h.length + ' 处'
    + (h.length ? ' -> ' + [...new Set(h.map(x => x[0]))].join(', ') : '  ★缺口'));
});

say('');
say('===== 9) 在线支付零调用（T15.23 / T15.19 硬约束）=====');
show('支付 SDK / requestPayment', /requestPayment|wxpay|微信支付|transfer|payment/i, 8);

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_facts_m15mp.txt', out.join('\n'), 'utf8');
