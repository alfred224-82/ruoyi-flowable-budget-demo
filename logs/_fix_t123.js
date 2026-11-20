/**
 * Task 1~3：SQL 部署包修复
 *  T1: M15_12 + M10_01 补进专业版/高级版/基础版包；M10_01 补进小微版包；系统管理新建 sql 包
 *  T2: 母目录回填 M5_12 / M5_13 / M5_14 / M15_14；M5_02 用已修复副本回写母目录
 *  T3: M8_01_portal_store_search_and_audit.sql 拆号为 M8_02_*（母目录 + 4 包 + 索引）
 * 强顺序约束：M15_12（DELETE 1300~1359）必须在 M10_01（INSERT 1320~1339）之前
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const MASTER = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/newrequest/sql';
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const PKG = g => path.join(ROOT, g, 'sql');
const md5 = p => crypto.createHash('md5').update(fs.readFileSync(p)).digest('hex');
const log = [];
const say = s => { log.push(s); console.log(s); };

function copy(src, dstDir, newName) {
  if (!fs.existsSync(dstDir)) fs.mkdirSync(dstDir, { recursive: true });
  const dst = path.join(dstDir, newName || path.basename(src));
  fs.copyFileSync(src, dst);
  return dst;
}

// ---------------------------------------------------------------- T3 first (rename)
say('===== T3: M8_01 一号双脚本拆号 -> M8_02 =====');
const OLD8 = 'M8_01_portal_store_search_and_audit.sql';
const NEW8 = 'M8_02_portal_store_search_and_audit.sql';
const dirs8 = [MASTER, PKG('专业版'), PKG('高级版'), PKG('基础版'), PKG('小微版')];
dirs8.forEach(d => {
  const o = path.join(d, OLD8), n = path.join(d, NEW8);
  if (fs.existsSync(o)) { fs.renameSync(o, n); say('  renamed: ' + d.replace(ROOT, '需求') + '/' + NEW8); }
  else say('  skip(absent): ' + d.replace(ROOT, '需求') + '/' + OLD8);
});

// ---------------------------------------------------------------- T2 master backfill
say('');
say('===== T2: 母目录回填缺失脚本 + M5_02 stale 回写 =====');
const backfill = [
  ['M5_12_fee_expense_bill_sync_columns.sql', '高级版'],
  ['M5_13_basic_category_subject_mapping.sql', '基础版'],
  ['M5_14_budget_subject_tenant_init.sql', '高级版'],
  ['M15_14_frontend_menu_gap_patch.sql', '小微版'],
];
backfill.forEach(([f, from]) => {
  const src = path.join(PKG(from), f);
  if (!fs.existsSync(src)) { say('  !! source missing: ' + from + '/' + f); return; }
  const dst = path.join(MASTER, f);
  if (fs.existsSync(dst)) { say('  already in master: ' + f); return; }
  fs.copyFileSync(src, dst);
  say('  backfilled -> master: ' + f + '  (from ' + from + ', md5=' + md5(dst).slice(0, 8) + ')');
});

// M5_02: master 是修复前 stale（缺 subject_type 限定去重），以高级版/专业版副本回写
const M502 = 'M5_02_create_tables_category.sql';
const adv502 = path.join(PKG('高级版'), M502), pro502 = path.join(PKG('专业版'), M502), mst502 = path.join(MASTER, M502);
if (md5(adv502) === md5(pro502)) {
  const before = md5(mst502);
  fs.copyFileSync(adv502, mst502);
  say('  M5_02 master rewritten (stale ' + before.slice(0, 8) + ' -> ' + md5(mst502).slice(0, 8) + ', 采用含 subject_type 限定去重修复的副本)');
} else {
  say('  !! M5_02 高级版/专业版副本本身不一致，跳过回写');
}

// ---------------------------------------------------------------- T1 platform menu scripts
say('');
say('===== T1: M15_12 + M10_01 补进各包 =====');
const M1512 = 'M15_12_fund_platform_menu_edition.sql';
const M1001 = 'M10_01_biz_bundle_feature_flag_menu.sql';
const srcM1512 = path.join(MASTER, M1512);   // 母目录本无，先由小微版回填
if (!fs.existsSync(srcM1512)) { fs.copyFileSync(path.join(PKG('小微版'), M1512), srcM1512); say('  M15_12 backfilled -> master'); }

['专业版', '高级版', '基础版'].forEach(g => {
  const d = PKG(g);
  [M1512, M1001].forEach(f => {
    const src = path.join(MASTER, f), dst = path.join(d, f);
    if (fs.existsSync(dst)) { say('  ' + g + '/' + f + ' already present'); return; }
    fs.copyFileSync(src, dst);
    say('  copied -> ' + g + '/' + f + ' (md5=' + md5(dst).slice(0, 8) + ')');
  });
});
// 小微版已有 M15_12，仅补 M10_01
const wx1001 = path.join(PKG('小微版'), M1001);
if (!fs.existsSync(wx1001)) { fs.copyFileSync(path.join(MASTER, M1001), wx1001); say('  copied -> 小微版/' + M1001 + ' (md5=' + md5(wx1001).slice(0, 8) + ')'); }

// ---------------------------------------------------------------- T1 系统管理 sql package
say('');
say('===== T1: 系统管理 新建 sql 自包含包 =====');
const SYS = PKG('系统管理');
const SYS_SCRIPTS = [
  'M1_01_existing_tables_add_tenant_id.sql', 'M1_02_create_tables_basic.sql',
  'M1_03_create_tables_advanced_pro.sql', 'M1_04_sys_menu.sql',
  'M1_05_platform_foundation_tables.sql', 'M1_06_sys_tenant_biz_channel.sql',
  'M1_07_project_container_tables.sql',
  'M2_01_roles.sql', 'M2_02_role_menu_templates.sql', 'M2_03_sys_role_menu_basic.sql',
  'M2_04_tenant_isolation.sql', 'M2_05_platform_admin_upgrade.sql', 'M2_06_user_tenant_fix.sql',
  'M2_07_tenant_root_dept_backfill.sql', 'M2_08_tenant_role_clone_migration.sql',
  'M2_09_tenant_max_roles.sql', 'M2_10_downshift_menu_templates.sql', 'M2_11_basic_boss_menu_realign.sql',
  'M3_01_xxl_job_tenant_expire_check.sql', 'M3_02_xxl_job_all_tasks.sql',
  'M3_05_license_tenant_association.sql', 'M3_06_license_management_menu.sql',
  'M4_01_edition_enterprise_to_professional.sql',
  M1512, M1001,
];
let okc = 0;
SYS_SCRIPTS.forEach(f => {
  const src = path.join(MASTER, f);
  if (!fs.existsSync(src)) { say('  !! master lacks ' + f); return; }
  fs.copyFileSync(src, path.join(SYS, f)); okc++;
});
say('  系统管理/sql 已建，复制脚本 ' + okc + '/' + SYS_SCRIPTS.length);

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t123_result.txt', log.join('\n'), 'utf8');
