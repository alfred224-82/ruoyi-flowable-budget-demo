const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/';

// === 1. 专业版 ===
{
  const p = R + '专业版/资金管控系统开发任务文档（专业版）.md';
  let t = fs.readFileSync(p, 'utf8');
  // 头部版本
  t = t.replace(
    /\*\*版本\*\*：v1\.16（专业版拆分\)/,
    '**版本**：v1.21（专业版 · 对齐母文档 v1.21）'
  );
  t = t.replace(
    /\*\*日期\*\*：2026-09-10/,
    '**日期**：2026-09-11'
  );
  t = t.replace(
    /《资金管控系统开发任务文档》v1\.16（一体化母文档）/,
    '《资金管控系统开发任务文档》v1.21（一体化母文档）'
  );
  // 删除"后续无需全系统扫描"
  t = t.replace(/后续无需全系统扫描。/, '');
  // T7.2 ✅→🟡
  t = t.replace(
    /\| ✅ T7\.2 \| 人脸比对/,
    '| 🟡 T7.2 | 人脸比对'
  );
  // 在 T7.2 验收要点后追加 MP 缺口说明
  t = t.replace(
    /不一致仅标记不拦截 \| P0 \|/,
    '不一致仅标记不拦截；**MP 赛道「小程序人脸采集」未闭合**（缺口归口 T8.5） | P0 |'
  );
  // 文末版本
  t = t.replace(
    /\*\*文档版本\*\*：v1\.16（专业版拆分\)/,
    '**文档版本**：v1.21（专业版 · 对齐母文档 v1.21）'
  );
  // 文末指针
  t = t.replace(
    /见一体化母文档《资金管控系统开发任务文档》v1\.16/,
    '见一体化母文档《资金管控系统开发任务文档》v1.21'
  );
  // 追加变更记录
  if (!t.includes('最近变更')) {
    t = t.replace(
      /(\*\*文档版本\*\*：v1\.21.*?\n)/,
      '$1**最近变更**：2026-09-11（对齐母文档 v1.21：T7.2 ✅→🟡 MP 人脸采集未闭合、删除「后续无需全系统扫描」stale 结论、版本号统一为 v1.21）\n'
    );
  }
  fs.writeFileSync(p, t, 'utf8');
  console.log('专业版 fixed');
}

// === 2. 小微版 ===
{
  const p = R + '小微版/资金管控系统开发任务文档（小微版）.md';
  let t = fs.readFileSync(p, 'utf8');
  // 头部版本
  t = t.replace(
    /\*\*版本\*\*：v1\.16（小微版拆分\)/,
    '**版本**：v1.21（小微版 · 对齐母文档 v1.21）'
  );
  t = t.replace(
    /\*\*日期\*\*：2026-09-10/,
    '**日期**：2026-09-11'
  );
  t = t.replace(
    /《资金管控系统开发任务文档》v1\.16（一体化母文档）/,
    '《资金管控系统开发任务文档》v1.21（一体化母文档）'
  );
  // "零 micro 页面" → 18 页面
  t = t.replace(
    /小程序端（ruoyi-app）零 micro 页面/,
    '小程序端（ruoyi-app）已交付 18 个 pages/micro/*.vue 页面'
  );
  // 脚本数 32→34
  t = t.replace(/自包含可独立部署，32 个脚本/, '自包含可独立部署，34 个脚本');
  // 文末版本
  t = t.replace(
    /\*\*文档版本\*\*：v1\.16（小微版拆分\)/,
    '**文档版本**：v1.21（小微版 · 对齐母文档 v1.21）'
  );
  // 文末指针
  t = t.replace(
    /见一体化母文档《资金管控系统开发任务文档》v1\.16/,
    '见一体化母文档《资金管控系统开发任务文档》v1.21'
  );
  // 追加变更记录
  if (!t.includes('最近变更')) {
    t = t.replace(
      /(\*\*文档版本\*\*：v1\.21.*?\n)/,
      '$1**最近变更**：2026-09-11（对齐母文档 v1.21：小程序页面 0→18、脚本数 32→34、版本号统一为 v1.21）\n'
    );
  }
  fs.writeFileSync(p, t, 'utf8');
  console.log('小微版 fixed');
}

// === 3. 母文档 ===
{
  const p = R + '资金管控系统开发任务文档.md';
  let t = fs.readFileSync(p, 'utf8');
  const eol = t.includes('\r\n') ? '\r\n' : '\n';
  const lines = t.split(/\r?\n/);

  // L601: v1.15 历史状态分布说明 - 桩实现类名加注
  // 原文: `ImagePipelineStub` / `ContentSecurityStub` / `WecomIntegrationStub` / 三个 `Message*Stub`
  const i601 = 600; // 0-based
  if (lines[i601] && lines[i601].includes('ContentSecurityStub') && !lines[i601].includes('已更正')) {
    lines[i601] = lines[i601]
      .replace(
        /`ContentSecurityStub`/,
        '`ContentSecurityStub`（v1.19 更正：全仓不存在）'
      )
      .replace(
        /三个 `Message\*Stub`/,
        '三个 `Message*Stub`（v1.19 更正：全仓不存在）'
      );
  }

  // L1199: 13.3 矩阵 M10 行
  const i1199 = 1198;
  if (lines[i1199] && lines[i1199].includes('ContentSecurityStub') && !lines[i1199].includes('已更正')) {
    lines[i1199] = lines[i1199].replace(
      /`ContentSecurityStub` 默认全 PASS/,
      '`ContentSecurityStub`（v1.19 更正：类名不存在）默认全 PASS'
    );
  }

  // L1201: 13.3 矩阵 M12 行
  const i1201 = 1200;
  if (lines[i1201] && lines[i1201].includes('ContentSecurityStub') && !lines[i1201].includes('已更正')) {
    lines[i1201] = lines[i1201].replace(
      /`ContentSecurityStub` 默认全 PASS/,
      '`ContentSecurityBlacklistChecker` 文本侧真实 REJECT、图片侧放行（v1.15 `ContentSecurityStub` 类名系虚构，已更正）'
    );
  }

  // L1219: 13.4.1 桩实现行
  const i1219 = 1218;
  if (lines[i1219] && lines[i1219].includes('ContentSecurityStub')) {
    lines[i1219] = lines[i1219]
      .replace(
        /`ContentSecurityStub`/,
        '`ContentSecurityStub`（v1.19 更正：类名不存在）'
      )
      .replace(
        /`WechatTemplateMessageStub` \/ `WechatKfMessageStub` \/ `WecomAppMessageStub`/,
        '`WechatTemplateMessageStub` / `WechatKfMessageStub` / `WecomAppMessageStub`（v1.19 更正：类名不存在）'
      );
  }

  t = lines.join(eol);
  fs.writeFileSync(p, t, 'utf8');
  console.log('母文档 fixed');
}

console.log('ALL DONE');
