const fs = require('fs');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X';
const out = [];
const say = s => out.push(s);
const read = p => { try { return fs.readFileSync(p, 'utf8'); } catch (e) { return ''; } };
const L = p => read(p).split(/\r?\n/);

// 1) FeeAccrualJob 三 handler
const aj = ROOT + '/ruoyi-job/src/main/java/com/ruoyi/job/service/FeeAccrualJob.java';
say('===== FeeAccrualJob (' + L(aj).length + ' 行) =====');
L(aj).forEach((l, i) => {
  if (/@XxlJob|@TenantJob|Job\b.*\(|isFeatureEnabled|tenantFeatureGuard|FEAT_/.test(l)) say('  L' + (i + 1) + '| ' + l.trim().slice(0, 140));
});

// 2) T5.17 三级预警阈值 85/95/100 全仓定位
say('');
say('===== T5.17 三级预警阈值（85/95/100）全仓定位 =====');
const dirs = ['ruoyi-system/src/main/java', 'ruoyi-admin/src/main/java', 'ruoyi-ui/src'];
const hits = [];
function walk(d) {
  if (!fs.existsSync(d)) return;
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = d + '/' + e.name;
    if (e.isDirectory()) { if (!['target', 'node_modules', 'dist'].includes(e.name)) walk(p); }
    else if (/\.(java|vue|js)$/.test(e.name)) {
      const t = read(p);
      if (/0\.85|0\.95|85%|95%|WARNING_THRESHOLD|warnThreshold|预警阈值/.test(t)) hits.push(p);
    }
  }
}
dirs.forEach(d => walk(ROOT + '/' + d));
say('  命中文件 = ' + hits.length);
hits.slice(0, 20).forEach(p => {
  say('  --- ' + p.replace(ROOT + '/', ''));
  L(p).forEach((l, i) => {
    if (/0\.85|0\.95|85%|95%|WARNING_THRESHOLD|warnThreshold|预警阈值/.test(l)) say('      L' + (i + 1) + '| ' + l.trim().slice(0, 130));
  });
});

// 3) ExpenseBillServiceImpl 时间线 2 处 + TrackControlController
say('');
say('===== T5.17 追溯时间线 =====');
const ebs = ROOT + '/ruoyi-system/src/main/java/com/ruoyi/system/service/impl/ExpenseBillServiceImpl.java';
L(ebs).forEach((l, i) => { if (/timeline|Timeline|追溯/.test(l)) say('  ExpenseBillServiceImpl L' + (i + 1) + '| ' + l.trim().slice(0, 130)); });
const tc = ROOT + '/ruoyi-admin/src/main/java/com/ruoyi/web/controller/fee/TrackControlController.java';
say('  TrackControlController 行数 = ' + L(tc).length);
L(tc).forEach((l, i) => { if (/@RequireFeature|@SaCheckPermission|@(Get|Post|Put|Delete)Mapping|public /.test(l)) say('      L' + (i + 1) + '| ' + l.trim().slice(0, 130)); });

// 4) SubscribeQuotaServiceImpl 定性（T14.4）
say('');
say('===== T14.4 SubscribeQuotaServiceImpl =====');
const sq = ROOT + '/ruoyi-system/src/main/java/com/ruoyi/system/service/impl/SubscribeQuotaServiceImpl.java';
say('  行数 = ' + L(sq).length);
L(sq).forEach((l, i) => { if (/public |remaining|剩余|consume|deduct|check/.test(l)) say('      L' + (i + 1) + '| ' + l.trim().slice(0, 130)); });
const wtp = ROOT + '/ruoyi-system/src/main/java/com/ruoyi/system/service/message/impl/WechatTemplateMessageProvider.java';
say('  WechatTemplateMessageProvider 是否调 SubscribeQuota = ' + /SubscribeQuota|subscribeQuota/.test(read(wtp)));

fs.writeFileSync(ROOT + '/logs/_facts_t4c.txt', out.join('\n'), 'utf8');
console.log('done lines=' + out.length);
