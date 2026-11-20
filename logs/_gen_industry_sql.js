// Generate M15_22_industry_template_seed_v35.sql from Excel dump
// 23 industries x ~150 projects (V3.5 + T15.33)
const fs = require('fs');
const path = require('path');

const dump = JSON.parse(fs.readFileSync(path.join(__dirname, '_industries_dump.json'), 'utf8'));
const projRows = dump.sheets['初始化项目'];
const projHeader = projRows[0];
const projData = projRows.slice(1).filter(r => r && r[1]);

// Group projects by 二级行业
const byIndustry = {};
for (const row of projData) {
  const [, subInd, name, type, price, duration, merchant, city, source, feature] = row;
  if (!subInd) continue;
  if (!byIndustry[subInd]) byIndustry[subInd] = [];
  byIndustry[subInd].push({ name, type, price, duration, merchant, city, feature });
}

// Industry metadata: 二级行业 -> { code, category, roles[boss,salesman,customer], settle_rule, units[], categories[] }
const META = [
  // 休闲娱乐 (6)
  { cn: '桌游',           code: 'board_game',    cat: '休闲娱乐', roles: ['桌游老板', '助教', '顾客'],           settle: '按单量周结',     units: ['小时', '局', '人'],       categories: ['房间', '助教', '附加'] },
  { cn: '剧本杀',         code: 'script_kill',   cat: '休闲娱乐', roles: ['店主', 'DM', '玩家'],                 settle: '按场次日结',     units: ['场', '人', '小时'],       categories: ['盒装本', '城市限定本', '沉浸换装', '团建包场'] },
  { cn: '棋牌室',         code: 'chess_room',    cat: '休闲娱乐', roles: ['店主', '服务员', '顾客'],             settle: '按台位小时结',   units: ['小时', '台', '人'],       categories: ['包间', '台位', '茶水', '简餐'] },
  { cn: 'KTV',            code: 'ktv',           cat: '休闲娱乐', roles: ['店主', '服务员', '顾客'],             settle: '按包厢小时结',   units: ['小时', '包厢', '人'],     categories: ['小包厢', '中包厢', '大包厢', '酒水套餐'] },
  { cn: '电竞馆',         code: 'esports',       cat: '休闲娱乐', roles: ['店主', '网管', '玩家'],               settle: '按机位小时结',   units: ['小时', '机位', '次'],     categories: ['普通区', '电竞区', '包厢', '饮品'] },
  { cn: '轰趴馆',         code: 'party_room',    cat: '休闲娱乐', roles: ['店主', '管家', '顾客'],               settle: '按场次日结',     units: ['场', '天', '人'],         categories: ['主题房', '套餐', '餐饮', '娱乐设备'] },
  // 生活服务 (7)
  { cn: '足浴',           code: 'foot_bath',     cat: '生活服务', roles: ['店主', '洗脚技师', '顾客'],           settle: '按单提成日结',   units: ['分钟', '次', '人'],       categories: ['足浴', '修脚', 'SPA', '套餐'] },
  { cn: '推拿',           code: 'massage',       cat: '生活服务', roles: ['店主', '按摩技师', '顾客'],           settle: '按单提成日结',   units: ['分钟', '次', '人'],       categories: ['中式推拿', '泰式按摩', '肩颈', '全身'] },
  { cn: 'SPA',            code: 'spa',           cat: '生活服务', roles: ['店主', 'SPA技师', '顾客'],            settle: '按单提成日结',   units: ['分钟', '次', '人'],       categories: ['面部SPA', '身体SPA', '精油', '套餐'] },
  { cn: '美容美发',       code: 'beauty_salon',  cat: '生活服务', roles: ['店主', '发型师', '顾客'],             settle: '按单提成日结',   units: ['次', '小时', '人'],       categories: ['剪发', '烫染', '护理', '美容'] },
  { cn: '美甲美睫',       code: 'nail_lash',     cat: '生活服务', roles: ['店主', '美甲师', '顾客'],             settle: '按单提成日结',   units: ['次', '小时', '人'],       categories: ['美甲', '美睫', '手部护理', '足部护理'] },
  { cn: '洗衣洗鞋',       code: 'laundry',       cat: '生活服务', roles: ['店主', '洗护师', '顾客'],             settle: '按件计周结',     units: ['件', '双', '次'],         categories: ['干洗', '水洗', '洗鞋', '皮具护理'] },
  { cn: '宠物美容（小型）', code: 'pet_grooming', cat: '生活服务', roles: ['店主', '宠物美容师', '顾客'],        settle: '按单提成日结',   units: ['次', '小时', '只'],       categories: ['洗澡', '美容', '造型', '护理'] },
  // 运动健身 (7)
  { cn: '健身房',         code: 'gym',           cat: '运动健身', roles: ['老板', '教练', '会员'],               settle: '按课时月结',     units: ['节', '月', '次', '年'],   categories: ['课程', '私教', '会员卡', '团体课'] },
  { cn: '瑜伽馆',         code: 'yoga',          cat: '运动健身', roles: ['馆主', '瑜伽老师', '学员'],           settle: '按课时月结',     units: ['节', '月', '次'],         categories: ['哈他瑜伽', '流瑜伽', '空中瑜伽', '私教'] },
  { cn: '普拉提',         code: 'pilates',       cat: '运动健身', roles: ['馆主', '普拉提教练', '学员'],         settle: '按课时月结',     units: ['节', '月', '次'],         categories: ['垫上普拉提', '器械普拉提', '私教', '团体课'] },
  { cn: '羽毛球馆',       code: 'badminton',     cat: '运动健身', roles: ['馆主', '教练', '球友'],               settle: '按场地小时结',   units: ['小时', '场地', '节'],     categories: ['场地租用', '私教', '培训班', '会员卡'] },
  { cn: '网球馆',         code: 'tennis',        cat: '运动健身', roles: ['馆主', '教练', '球友'],               settle: '按场地小时结',   units: ['小时', '场地', '节'],     categories: ['场地租用', '私教', '培训班', '会员卡'] },
  { cn: '游泳馆',         code: 'swimming',      cat: '运动健身', roles: ['馆主', '教练', '学员'],               settle: '按次或月结',     units: ['次', '月', '节'],         categories: ['散客票', '私教', '培训班', '年卡'] },
  { cn: '舞蹈室',         code: 'dance',         cat: '运动健身', roles: ['馆主', '舞蹈老师', '学员'],           settle: '按课时月结',     units: ['节', '月', '次'],         categories: ['街舞', '中国舞', '拉丁', '私教'] },
  // 专业工作室 (3)
  { cn: '摄影写真',       code: 'photo_studio',  cat: '专业工作室', roles: ['工作室主', '摄影师', '顾客'],       settle: '按单预收+尾款',  units: ['套', '小时', '张'],       categories: ['个人写真', '情侣照', '全家福', '商务形象'] },
  { cn: '理发工作室',     code: 'barber_studio', cat: '专业工作室', roles: ['工作室主', '理发师', '顾客'],       settle: '按单提成日结',   units: ['次', '小时', '人'],       categories: ['男士理发', '女士剪发', '烫染', '护理'] },
  { cn: '家政保洁团队',   code: 'housekeeping',  cat: '专业工作室', roles: ['团队长', '保洁员', '雇主'],         settle: '按单结算周结',   units: ['次', '小时', '月'],       categories: ['日常保洁', '深度保洁', '家电清洗', '做饭阿姨'] },
];

// Verify all META items have projects in Excel
const missing = META.filter(m => !byIndustry[m.cn]);
if (missing.length) {
  console.error('MISSING projects for:', missing.map(m => m.cn).join(', '));
  console.error('Excel industries:', Object.keys(byIndustry).join(', '));
}

// SQL escape helpers
function sqlStr(v) {
  if (v === null || v === undefined) return 'NULL';
  return "'" + String(v).replace(/\\/g, '\\\\').replace(/'/g, "\\'") + "'";
}
function jsonEscape(obj) {
  // MySQL JSON_OBJECT/JSON_ARRAY style, but easier: embed as JSON string literal
  const s = JSON.stringify(obj);
  return "'" + s.replace(/\\/g, '\\\\').replace(/'/g, "\\'") + "'";
}

// Build SQL
const lines = [];
lines.push('-- =====================================================================');
lines.push('-- M15_22 行业模板种子数据填充脚本（T15.33，V3.5 对齐，v1.34 新增）');
lines.push('-- 版本：v1.0  日期：2026-09-17');
lines.push('-- 依据：《SaaS 平台需求草稿_最终定稿版 V3.5》第十六章「行业模板与初始化机制」');
lines.push('--       《资金管控系统开发任务文档》v1.24 T15.33~T15.34');
lines.push('--       `docs/最新整理好的需求/初始化项目清单_上海.xlsx`（23 个二级行业 × 132 个真实服务项目）');
lines.push('-- 前置依赖：M15_02（industry_template + industry_role_map 建表 + 4 个既有行业）');
lines.push('-- 规范：');
lines.push('--   1. 【幂等】INSERT IGNORE + uk_industry_code / uk_industry_bizrole 保护，可重复执行');
lines.push('--   2. 【存量兼容】既有 board_game / gym / spa 三个行业仅 UPDATE preset_config（保留 industry_code 不变，');
lines.push('--      避免破坏 sys_tenant.industry 外键引用）；既有 market 因 V3.5 §10 排除零售/生鲜，SET status=1 停用不删除');
lines.push('--   3. 【23 行业口径】休闲娱乐 6 + 生活服务 7 + 运动健身 7 + 专业工作室 3 = 23（V3.5 §16 + 任务文档 v1.24 T15.33）');
lines.push('--   4. preset_config JSON 结构：{ category, service_categories[], settle_rule, units[], projects[{name,reference_price,duration,sample_merchant,feature}] }');
lines.push('--   5. projects 每行业取 Excel 前 5~7 条（大众点评上海公开团购真实数据）');
lines.push('--   6. industry_role_map 每行业 3 条：boss/salesman/customer 标准角色 + 行业化业务角色显示名');
lines.push('-- =====================================================================');
lines.push('');
lines.push('SET NAMES utf8mb4;');
lines.push('');
lines.push('-- ---------------------------------------------------------------------');
lines.push('-- 1. 存量兼容：market（买菜/生鲜）停用（V3.5 §10 明确排除零售/生鲜）');
lines.push('--    保留 industry_code 与既有 sys_tenant.industry 引用，仅隐藏下拉框');
lines.push('-- ---------------------------------------------------------------------');
lines.push("UPDATE `industry_template` SET `status` = '1', `update_by` = 'admin', `update_time` = NOW(),");
lines.push("  `remark` = '【V3.5 停用】零售/生鲜不在小微版适用范围（V3.5 §10），保留数据兼容既有租户'");
lines.push("WHERE `industry_code` = 'market';");
lines.push('');
lines.push('-- ---------------------------------------------------------------------');
lines.push('-- 2. 23 个二级行业模板（INSERT IGNORE 幂等 + 后续 UPDATE 补 preset_config）');
lines.push('-- ---------------------------------------------------------------------');

let sortOrder = 0;
for (const m of META) {
  sortOrder += 1;
  const projects = (byIndustry[m.cn] || []).slice(0, 7).map(p => ({
    name: p.name,
    reference_price: p.price ? Number(String(p.price).replace(/[^\d.]/g, '')) || p.price : null,
    duration: p.duration || null,
    sample_merchant: p.merchant || null,
    feature: p.feature || null,
  }));
  const preset = {
    category: m.cat,
    service_categories: m.categories,
    settle_rule: m.settle,
    units: m.units,
    projects: projects,
  };
  const industryName = m.cn;
  const iconMap = { '休闲娱乐': 'game', '生活服务': 'star', '运动健身': 'peoples', '专业工作室': 'tool' };
  const icon = iconMap[m.cat] || 'component';
  const remark = `${m.cat}·${m.cn}（V3.5 T15.33 种子数据，含 ${projects.length} 个上海真实团购项目参考）`;

  lines.push(`-- [${sortOrder}] ${m.cat} / ${m.cn} → ${m.code}（${projects.length} 项目）`);
  lines.push(`INSERT IGNORE INTO \`industry_template\` (\`industry_code\`, \`industry_name\`, \`icon\`, \`preset_config\`, \`sort_order\`, \`status\`, \`create_by\`, \`remark\`) VALUES`);
  lines.push(`  (${sqlStr(m.code)}, ${sqlStr(industryName)}, ${sqlStr(icon)}, ${jsonEscape(preset)}, ${sortOrder}, '0', 'admin', ${sqlStr(remark)});`);
  // Also UPDATE preset_config in case row already exists (idempotent refresh)
  lines.push(`UPDATE \`industry_template\` SET \`industry_name\` = ${sqlStr(industryName)}, \`icon\` = ${sqlStr(icon)}, \`preset_config\` = ${jsonEscape(preset)}, \`sort_order\` = ${sortOrder}, \`status\` = '0', \`update_by\` = 'admin', \`update_time\` = NOW(), \`remark\` = ${sqlStr(remark)} WHERE \`industry_code\` = ${sqlStr(m.code)};`);
  lines.push('');
}

lines.push('-- ---------------------------------------------------------------------');
lines.push('-- 3. 23 × 3 = 69 条行业角色映射（INSERT IGNORE 幂等）');
lines.push('--    业务角色名（如 洗脚技师/教练/摄影师）→ 标准角色 code（boss/salesman/customer）');
lines.push('-- ---------------------------------------------------------------------');
lines.push("INSERT IGNORE INTO `industry_role_map` (`industry_code`, `biz_role_name`, `role_code`, `default_flag`, `sort_order`) VALUES");
const roleRows = [];
for (const m of META) {
  roleRows.push(`  (${sqlStr(m.code)}, ${sqlStr(m.roles[0])}, 'boss',     1, 1)`);
  roleRows.push(`  (${sqlStr(m.code)}, ${sqlStr(m.roles[1])}, 'salesman', 1, 2)`);
  roleRows.push(`  (${sqlStr(m.code)}, ${sqlStr(m.roles[2])}, 'customer', 1, 3)`);
}
lines.push(roleRows.join(',\n') + ';');
lines.push('');
lines.push('-- ---------------------------------------------------------------------');
lines.push('-- 4. 验收查询（执行后人工核对）');
lines.push('-- ---------------------------------------------------------------------');
lines.push('-- 4.1 行业模板总数（预期 24 = 23 启用 + 1 停用 market；启用状态仅 23）');
lines.push("-- SELECT COUNT(*) FROM industry_template WHERE del_flag = '0';                    -- 预期 24");
lines.push("-- SELECT COUNT(*) FROM industry_template WHERE del_flag = '0' AND status = '0';   -- 预期 23");
lines.push('');
lines.push('-- 4.2 四大类分布（预期：休闲娱乐 6 / 生活服务 7 / 运动健身 7 / 专业工作室 3）');
lines.push("-- SELECT JSON_UNQUOTE(JSON_EXTRACT(preset_config, '$.category')) AS cat, COUNT(*) FROM industry_template");
lines.push("--  WHERE status = '0' GROUP BY cat ORDER BY cat;");
lines.push('');
lines.push('-- 4.3 每行业项目预设 5~7 条（V3.5 T15.33 覆盖要求）');
lines.push("-- SELECT industry_code, industry_name, JSON_LENGTH(preset_config, '$.projects') AS project_count");
lines.push("--   FROM industry_template WHERE status = '0' ORDER BY sort_order;");
lines.push('-- 预期：每行 project_count BETWEEN 5 AND 7');
lines.push('');
lines.push('-- 4.4 行业角色映射完整（每行业 3 条：boss/salesman/customer）');
lines.push("-- SELECT industry_code, COUNT(*) FROM industry_role_map WHERE industry_code <> 'market' GROUP BY industry_code;");
lines.push('-- 预期：23 行，每行 count = 3（合计 69）');
lines.push('');
lines.push('-- 4.5 market 已停用但数据保留（存量租户 industry_code=market 不受影响）');
lines.push("-- SELECT industry_code, industry_name, status, remark FROM industry_template WHERE industry_code = 'market';");
lines.push("-- 预期：status = '1'（停用），remark 含 'V3.5 停用'");
lines.push('');
lines.push('-- 4.6 前端下拉框验证（租户新增页面 → 行业模板下拉应显示 23 项）');
lines.push("-- SELECT industry_code, industry_name FROM industry_template WHERE status = '0' AND del_flag = '0' ORDER BY sort_order;");
lines.push('');

const outPath = path.join('D:', 'workspace', 'RuoYi-Flowable-Plus-0.8.X', 'docs', '最新整理好的需求', '小微版', 'sql', 'M15_22_industry_template_seed_v35.sql');
fs.writeFileSync(outPath, lines.join('\n'), 'utf8');

// Stats
const totalProjects = META.reduce((acc, m) => acc + Math.min((byIndustry[m.cn] || []).length, 7), 0);
console.log('Generated:', outPath);
console.log('Industries:', META.length);
console.log('Total preset projects (capped at 7/industry):', totalProjects);
console.log('Role map rows:', META.length * 3);
console.log('SQL lines:', lines.length);
