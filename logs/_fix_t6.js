const fs = require('fs');
const R = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求/专业版/';
const p = R + '资金管控系统开发任务文档（专业版）.md';
let t = fs.readFileSync(p, 'utf8');

// 1. 修正 note 头计数：✅ 8→7, 🟡 0→1
t = t.replace(
  'v1.15 对账（✅ 8 / 🔄 0 / 🟡 0 / 🆕 0）：全量交付，零缺口',
  'v1.15 对账（✅ 7 / 🔄 0 / 🟡 1 / 🆕 0）：T7.2 MP 赛道缺口（归口 T8.5），其余 7 项 SQL/BE/PC 三赛道零缺口'
);
console.log('修正 note 头计数');

// 2. 修正 v1.16 代码验证行（去掉"零缺口"暗示）
t = t.replace(
  '8/8 任务代码 + SQL 均已落盘，状态无误。',
  '8/8 任务代码 + SQL 均已落盘（T7.2 MP 缺口见 v1.21 补注）。'
);
console.log('修正 v1.16 验证说明');

// 3. T7.6 验收要点补 MP 依赖声明
t = t.replace(
  '| ✅ T7.6 | 物理隔离预留：租户配置 `isolation_mode=SHARED/DEDICATED`，动态数据源骨架（不实现完整切换） | `FEAT_ISOLATED_SCHEMA` | — | 配置项存在、代码预留 | P0 |',
  '| ✅ T7.6 | 物理隔离预留：租户配置 `isolation_mode=SHARED/DEDICATED`，动态数据源骨架（不实现完整切换） | `FEAT_ISOLATED_SCHEMA` | — | 配置项存在、代码预留；**MP 赛道依赖 T8.11（独立小程序）** | P0 |'
);
console.log('T7.6 补 MP 依赖');

// 4. v1.21 补注追加 T7.6↔T8.11 依赖声明
t = t.replace(
  'T7.2 由 ✅ 降为 🟡（MP 赛道人脸采集未闭合，缺口归口 T8.5），其余 7 项维持 ✅。',
  'T7.2 由 ✅ 降为 🟡（MP 赛道人脸采集未闭合，缺口归口 T8.5）；T7.6 ✅ 但 MP 赛道依赖 T8.11（独立小程序，🆕）。其余 6 项 SQL/BE/PC 三赛道零缺口。'
);
console.log('v1.21 补注追加 T7.6 依赖');

fs.writeFileSync(p, t, 'utf8');
console.log('DONE');
