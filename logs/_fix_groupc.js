const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/';

// === 1. 高级版：补 5 项组 C 开关（FEAT_WECOM 已在表中） ===
{
  const p = R + '高级版/资金管控系统开发任务文档（高级版）.md';
  let t = fs.readFileSync(p, 'utf8');
  
  const insertAfter = '| `FEAT_WECOM` | 企业微信通道 | ✅ | ❌ |';
  const groupCRows = [
    '| `FEAT_FLOW_MULTI_LEVEL` | 完整多级审批流（组 C） | ✅ | ❌ |',
    '| `FEAT_OWN_MINIAPP` | 独立小程序（客户自有 AppID，组 C） | ✅ | ❌ |',
    '| `FEAT_FLOW_CUSTOM` | 自定义流程设计器（组 C，仅专业版） | ❌ | ❌ |',
    '| `FEAT_MULTI_ACCOUNT_SET` | 多账套（组 C，仅专业版） | ❌ | ❌ |',
    '| `FEAT_OPEN_API` | 开放平台 API（组 C，仅专业版） | ❌ | ❌ |',
  ];
  
  if (!t.includes('| `FEAT_FLOW_MULTI_LEVEL`')) {
    t = t.replace(insertAfter, insertAfter + '\n' + groupCRows.join('\n'));
    console.log('高级版: 补 5 项组 C 开关');
  } else {
    console.log('高级版: 组 C 开关已存在');
  }
  
  // 统计开关数
  const sw = t.match(/^\| `FEAT_/gm);
  console.log('高级版: 开关表共 ' + (sw ? sw.length : '?') + ' 项');
  
  fs.writeFileSync(p, t, 'utf8');
}

// === 2. 专业版：修正组 C 说明 + 补 FEAT_FLOW_CUSTOM 到组 D 表 ===
{
  const p = R + '专业版/资金管控系统开发任务文档（专业版）.md';
  let t = fs.readFileSync(p, 'utf8');
  
  // 修正组 C 说明
  const oldNote = '组 C（6 项通道/多账套/开放 API 配套）开关同高级版，详见《高级版》文档。';
  const newNote = '组 C（6 项多业态底座新增开关）：`FEAT_FLOW_MULTI_LEVEL`/`FEAT_OWN_MINIAPP`/`FEAT_WECOM` 高级版也启用；`FEAT_FLOW_CUSTOM`/`FEAT_MULTI_ACCOUNT_SET`/`FEAT_OPEN_API` 仅专业版启用（已列入上方组 D 表）。';
  if (t.includes(oldNote)) {
    t = t.replace(oldNote, newNote);
    console.log('专业版: 修正组 C 说明');
  } else {
    console.log('专业版: 组 C 说明未找到原文，跳过');
  }
  
  // 补 FEAT_FLOW_CUSTOM 到组 D 表（FEAT_MULTI_ACCOUNT_SET 和 FEAT_OPEN_API 已在表中）
  // 在 FEAT_ISOLATED_SCHEMA 行后插入（保持逻辑分组：先隔离再多账套再开放API）
  const insertBefore = '| `FEAT_MULTI_ACCOUNT_SET` | 多账套 | ✅ | ❌ | ❌ |';
  const newSwitchRow = '| `FEAT_FLOW_CUSTOM` | 自定义流程设计器 | ✅ | ❌ | ❌ |';
  if (!t.includes('| `FEAT_FLOW_CUSTOM`')) {
    t = t.replace(insertBefore, newSwitchRow + '\n' + insertBefore);
    console.log('专业版: 补 FEAT_FLOW_CUSTOM 到组 D 表');
  } else {
    console.log('专业版: FEAT_FLOW_CUSTOM 已存在');
  }
  
  // 更新开关总数（33→34，因为加了 FEAT_FLOW_CUSTOM）
  // 原文: 专业版拥有**全部 33 项开关**
  t = t.replace(
    /专业版拥有\*\*全部 33 项开关\*\*/,
    '专业版拥有**全部 34 项开关**'
  );
  console.log('专业版: 更新开关总数');
  
  fs.writeFileSync(p, t, 'utf8');
}

console.log('ALL DONE');
