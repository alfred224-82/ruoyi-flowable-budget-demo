const fs = require('fs');
const p = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/专业版/资金管控系统开发任务文档（专业版）.md';
let t = fs.readFileSync(p, 'utf8');

const old = '**v1.15 对账（✅ 8 / 🔄 0 / 🟡 0 / 🆕 0）**：全量交付，零缺口。T7.7 多账套 + T7.8 开放平台 API 全栈交付。';
const rep = '**v1.15 对账（✅ 7 / 🔄 0 / 🟡 1 / 🆕 0）**：T7.2 MP 赛道缺口（归口 T8.5），其余 7 项 SQL/BE/PC 三赛道零缺口。T7.7 多账套 + T7.8 开放平台 API 全栈交付。';

if (t.includes(old)) {
  t = t.replace(old, rep);
  fs.writeFileSync(p, t, 'utf8');
  console.log('L27 note 头修正完成');
} else {
  console.log('未找到原文，跳过');
}
