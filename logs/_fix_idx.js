/**
 * Task 1/3 续：更新 4 个既有 00_执行顺序.sql + 新建 系统管理/sql/00_执行顺序.sql
 *  - source M8_01_portal_store_search_and_audit.sql -> M8_02_*
 *  - 追加「平台底座管理菜单」阶段：M15_12 -> M10_01（顺序强约束）
 */
const fs = require('fs');
const path = require('path');
const ROOT = 'D:/workspace/RuoYi-Flowable-Plus-0.8.X/docs/最新整理好的需求';
const PKG = g => path.join(ROOT, g, 'sql');
const IDX = g => path.join(PKG(g), '00_执行顺序.sql');
const log = [];
const say = s => { log.push(s); console.log(s); };

const EOL = '\r\n';
const OLD8 = 'source M8_01_portal_store_search_and_audit.sql;';
const NEW8 = 'source M8_02_portal_store_search_and_audit.sql;';

// 平台底座管理菜单阶段（专业版/高级版/基础版：M15_12 + M10_01；小微版：仅 M10_01，M15_12 已在阶段六）
function platformStage(kind, baseNo) {
  const L = [];
  L.push('-- ---------------------------------------------------------------------');
  if (kind === 'full') {
    L.push('-- 阶段' + baseNo + '：平台底座管理菜单（T10.2 / T10.3 / T15.29，v1.19 补入自包含包）');
  } else {
    L.push('-- 阶段' + baseNo + '：平台底座管理菜单补齐（T10.2 / T10.3，v1.19 补入）');
  }
  L.push('-- ---------------------------------------------------------------------');
  L.push('-- ★顺序强约束（不可颠倒）：M15_12 以');
  L.push('--     DELETE FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359');
  L.push('--   开局，而 M10_01 的「业态包管理」(1320~1322) 与「功能开关管理」(1330~1332) 正落在该区段内。');
  L.push('--   必须【先 M15_12 建 1300 一级目录 + 1310~1314 版本档位管理，再 M10_01】，');
  L.push('--   颠倒则 M10_01 建出的菜单会被 M15_12 的 DELETE 清除，T10.2/T10.3 的 PC 入口消失。');
  if (kind === 'full') {
    L.push('-- [' + baseNo + '-1] 资金平台管理菜单收纳 + 版本档位管理（menu_id 1300 一级目录 / 1310~1314，');
    L.push('--       T15.29）。re-parent 的 UPDATE 全部按 perms/menu_name 匹配，目标不存在即 no-op，');
    L.push('--       本包无 M15_06（小微版菜单）与 M15_10（version_type 分档）亦安全。');
    L.push('source M15_12_fund_platform_menu_edition.sql;');
    L.push('-- [' + baseNo + '-2] 业态包管理（system:bizBundle:*）+ 功能开关管理（system:featureFlag:*），');
    L.push('--       menu_id 1320~1339，六层防串第①层「版本分配入口」的 PC 管理页（T10.2 / T10.3），');
    L.push('--       全部授予超管 role_id=1；前置依赖 M1_05（biz_bundle / feature_flag 建表）已在阶段一执行。');
    L.push('source M10_01_biz_bundle_feature_flag_menu.sql;');
  } else {
    L.push('-- [' + baseNo + '-1] 业态包管理（system:bizBundle:*）+ 功能开关管理（system:featureFlag:*），');
    L.push('--       menu_id 1320~1339，六层防串第①层「版本分配入口」的 PC 管理页（T10.2 / T10.3），');
    L.push('--       全部授予超管 role_id=1。前置：M15_12 已在阶段六建出 1300 一级目录，M1_05 已建 biz_bundle / feature_flag。');
    L.push('source M10_01_biz_bundle_feature_flag_menu.sql;');
  }
  return L;
}

['专业版', '高级版', '基础版', '小微版'].forEach(g => {
  const f = IDX(g);
  let t = fs.readFileSync(f, 'utf8');
  const eol = t.includes('\r\n') ? '\r\n' : '\n';
  let lines = t.split(/\r?\n/);

  // 1) M8_01 -> M8_02
  let renamed = 0;
  lines = lines.map(l => { if (l.trim() === OLD8) { renamed++; return l.replace(OLD8, NEW8); } return l; });

  // 2) 在最后一条 source 行之后插入平台底座管理菜单阶段
  let lastSrc = -1;
  lines.forEach((l, i) => { if (/^\s*source\s+/i.test(l)) lastSrc = i; });
  const isWx = (g === '小微版');
  // 阶段号：沿用各包既有最大阶段号 +1（小微版为 M15 阶段六之后，单独编号）
  const stageNo = isWx ? '七（补）' : '十五';
  const block = platformStage(isWx ? 'mini' : 'full', stageNo);
  const already = lines.some(l => /source\s+M10_01_biz_bundle_feature_flag_menu\.sql/i.test(l));
  if (!already && lastSrc >= 0) {
    lines.splice(lastSrc + 1, 0, '', ...block);
  }

  fs.writeFileSync(f, lines.join(eol), 'utf8');
  say(g + ': M8_01->M8_02 改名 ' + renamed + ' 处；平台底座菜单阶段' + (already ? '（已存在，跳过）' : '已插入于最后一条 source 之后') + '；总行数 ' + lines.length);
});

// ---------------------------------------------------------------- 系统管理 索引
const SYS_LINES = [
  '-- =====================================================================',
  '-- 系统管理（平台底座 / 租户与授权 / License / 开关体系）SQL 执行顺序索引 · v1.19',
  '-- 日期：2026-09-10',
  '-- 依据：《资金管控系统开发任务文档》v1.16（一体化母文档）',
  '--       《资金管控系统开发任务文档（系统管理）》v1.19',
  '-- 口径：本目录为《系统管理》分册承载的 M1 / M2 / M3 / M3.5 / M4 / M9 / M10 共 46 项任务',
  '--       的【自包含可独立部署】脚本集，是平台侧（admin / platform_admin）最小部署单元。',
  '-- 说明：',
  '--   1. 全部脚本幂等，可重复执行；严格按序号执行，不可颠倒；',
  '--   2. 建表一次建齐，版本靠菜单模板（M2_02 / M2_10）+ 功能开关（M1_05 feature_flag）区分，',
  '--      故本包含 M1_03（高级/专业版增量表）——表结构全版本共用，不代表基础版开放这些能力；',
  '--   3. M9（升级流程与防串验收）为验证类里程碑，交付物是演练报告与测试报告，无 SQL 脚本；',
  '--   4. 业态功能（M5 预算域 / M6 高级增量 / M7 专业版增量 / M11~M14 业态与商业化）',
  '--      与小微版（M15 / M16）脚本不在本包，分别见《高级版》《专业版》《小微版》目录；',
  '--   5. ★版本谱系（v1.16）：小微版（MICRO，micro_lite / micro_pro）为独立产品线，',
  '--      企业版谱系为 BASIC / ADVANCED / PROFESSIONAL（存量 ENTERPRISE 由 normalizeEdition 归一化）。',
  '-- =====================================================================',
  '',
  '-- ---------------------------------------------------------------------',
  '-- 阶段一：数据库底座与平台底座建表（M1，T1.1~T1.9）',
  '-- ---------------------------------------------------------------------',
  '-- [01] 存量表补 tenant_id + 索引（幂等存储过程，重复执行不报 1060/1061）',
  'source M1_01_existing_tables_add_tenant_id.sql;',
  '-- [02] 基础版业务表（fee_project / fee_payment / fee_receipt / fee_income_confirmation /',
  '--      fee_project_change / fee_bad_debt / fee_project_link_rule / fee_invoice 等）',
  'source M1_02_create_tables_basic.sql;',
  '-- [03] 高级/专业版增量表主体（fee_employee_credit / fee_credit_rule / fee_clock_record /',
  '--      fee_org / fee_org_budget_version / fee_face_verify_record / fee_rule_version 等）',
  '--      ★建表一次建齐，能力开放由 feature_flag + 菜单模板控制，与本包版本无关',
  'source M1_03_create_tables_advanced_pro.sql;',
  '-- [04] sys_menu 全量菜单（含 client_type 由 M15_06 补列，本包不含小微版菜单）',
  'source M1_04_sys_menu.sql;',
  '-- [05] ★平台底座三表：biz_bundle（3 业态包）/ feature_flag（33 项开关，组 A~D）/',
  '--      sys_edition_menu_template 等；T1.7 / T10.2 / T10.3 的数据基础',
  'source M1_05_platform_foundation_tables.sql;',
  '-- [06] sys_tenant 扩展三字段（biz_code / access_channel / wecom_*），T1.8 / T10.1',
  'source M1_06_sys_tenant_biz_channel.sql;',
  '-- [07] project 统一容器四表（project / project_member / project_task / project_signup），T1.9',
  'source M1_07_project_container_tables.sql;',
  '',
  '-- ---------------------------------------------------------------------',
  '-- 阶段二：角色与授权模板（M2，T2.1~T2.5）',
  '-- ---------------------------------------------------------------------',
  '-- [08] 标准角色集（boss / project_manager / expense_staff / finance / salesman / customer）',
  'source M2_01_roles.sql;',
  '-- [09] ★角色×版本授权模板 sys_edition_menu_template（*_BASIC / *_ADVANCED / *_PROFESSIONAL）',
  '--      六层防串第④层「菜单授权」的数据源，必须早于 M2_05',
  'source M2_02_role_menu_templates.sql;',
  '-- [10] 基础版 sys_role_menu 授权',
  'source M2_03_sys_role_menu_basic.sql;',
  '-- [11] sys_dept / sys_role 补 tenant_id + 索引（幂等存储过程），T2.4 数据隔离',
  'source M2_04_tenant_isolation.sql;',
  '-- [12] platform_admin 平台管理员升级',
  '--      ★已知陷阱：本脚本会重建模板表，必须在 M2_02 之后执行，否则摧毁角色×版本键',
  '--        导致业务角色菜单为空',
  'source M2_05_platform_admin_upgrade.sql;',
  '-- [13] 存量用户-租户归属修复',
  'source M2_06_user_tenant_fix.sql;',
  '-- [14] 租户根部门回填',
  'source M2_07_tenant_root_dept_backfill.sql;',
  '-- [15] 租户角色克隆迁移（共享预设角色 -> 租户私有角色）',
  'source M2_08_tenant_role_clone_migration.sql;',
  '-- [16] sys_tenant.max_roles 角色数上限（基础版 2 / 高级版 4-6 / 专业版 NULL 不限）',
  'source M2_09_tenant_max_roles.sql;',
  '-- [17] ★下移菜单模板迁移（11 类资金管控重能力由 *_BASIC 迁至 *_ADVANCED / *_PROFESSIONAL）',
  'source M2_10_downshift_menu_templates.sql;',
  '',
  '-- ---------------------------------------------------------------------',
  '-- 阶段三：定时任务与 License 体系（M3 / M3.5，T3.1~T3.15）',
  '-- ---------------------------------------------------------------------',
  '-- [18] XXL-JOB 租户到期检查任务登记（tenantExpireCheckJob）',
  'source M3_01_xxl_job_tenant_expire_check.sql;',
  '-- [19] XXL-JOB 全量任务登记',
  'source M3_02_xxl_job_all_tasks.sql;',
  '-- [20] sys_license 与租户关联（tenant_id NULL=平台级 / 非 NULL=租户级），T3.10 / T3.11',
  'source M3_05_license_tenant_association.sql;',
  '-- [21] License 管理菜单',
  'source M3_06_license_management_menu.sql;',
  '',
  '-- ---------------------------------------------------------------------',
  '-- 阶段四：版本编码归一化（M4，T4.1~T4.5）',
  '-- ---------------------------------------------------------------------',
  '-- [22] ENTERPRISE -> PROFESSIONAL 历史编码兼容标注（不写迁移、不改存量生效值，',
  '--      归一化统一由 EditionFeatureConfig#normalizeEdition 承担，T10.7）',
  'source M4_01_edition_enterprise_to_professional.sql;',
  '',
  '-- ---------------------------------------------------------------------',
  '-- 阶段五：平台底座管理菜单（M10，T10.2 / T10.3 + T15.29 菜单收纳）',
  '-- ---------------------------------------------------------------------',
  '-- ★顺序强约束（不可颠倒）：M15_12 以',
  '--     DELETE FROM sys_menu WHERE menu_id BETWEEN 1300 AND 1359',
  '--   开局，而 M10_01 的「业态包管理」(1320~1322) 与「功能开关管理」(1330~1332) 正落在该区段内。',
  '--   必须【先 M15_12 建 1300 一级目录 + 1310~1314 版本档位管理，再 M10_01】，',
  '--   颠倒则 M10_01 建出的菜单会被 M15_12 的 DELETE 清除。',
  '-- [23] 「资金平台管理菜单」一级目录（menu_id 1300）+ 版本档位管理（1310~1314）',
  '--      + 平台自有管理菜单 re-parent（按 perms 匹配，目标不存在即 no-op）。',
  '--      任务归属 M15（T15.29，小微版方案 v10.3 引入），但产出的是【平台级 admin 菜单】，',
  '--      跨全部版本共用，故一并纳入本系统管理包。',
  'source M15_12_fund_platform_menu_edition.sql;',
  '-- [24] 业态包管理（system:bizBundle:*，1320~1322）+ 功能开关管理（system:featureFlag:*，1330~1332）',
  '--      六层防串第①层「版本分配入口」的 PC 管理页（T10.2 / T10.3），全部授予超管 role_id=1。',
  '--      前置：M1_05（biz_bundle / feature_flag 建表）已在阶段一执行。',
  'source M10_01_biz_bundle_feature_flag_menu.sql;',
  '',
  '-- ---------------------------------------------------------------------',
  '-- 阶段六：存量租户菜单对齐（M2 收尾，必须在全部菜单模板脚本之后）',
  '-- ---------------------------------------------------------------------',
  '-- [25] 存量基础版租户克隆角色菜单对齐修复（v1.12 / 手册 v2.0 验收）：',
  '--      回收空壳「预算管理」目录 2000 等下移菜单 + 按 *_BASIC 模板补齐 boss/employee 授权。',
  '--      前置：M2_02 / M2_03 / M2_10 均已执行。',
  'source M2_11_basic_boss_menu_realign.sql;',
  '',
  '-- =====================================================================',
  '-- 验收检查（系统管理）',
  '-- =====================================================================',
  '-- 1) 平台底座三表就绪',
  '-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()',
  "--   AND TABLE_NAME IN ('biz_bundle','feature_flag','tenant_biz_config');  -- 预期 >= 3",
  '',
  '-- 2) 33 项功能开关落表且分组正确（组 A 11 / 组 B 9 / 组 C 6 / 组 D 7）',
  "-- SELECT flag_group, COUNT(*) FROM feature_flag GROUP BY flag_group;  -- 预期 A=11 B=9 C=6 D=7",
  '',
  '-- 3) 3 个内置业态包就绪',
  "-- SELECT biz_code, name, status FROM biz_bundle;  -- 预期 fund_control / boardgame / fresh_food",
  '',
  '-- 4) ★资金平台管理菜单目录 + 版本档位管理 + 业态包/开关管理菜单齐备（v1.19 补入）',
  '-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu',
  '--  WHERE menu_id BETWEEN 1300 AND 1359 ORDER BY menu_id;',
  '-- 预期：1300(目录) + 1310~1314(版本档位管理) + 1320~1322(业态包管理) + 1330~1332(功能开关管理)',
  '',
  '-- 5) ★六层防串第①层入口可达：超管已获授权',
  '-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 1300 AND 1359;',
  '-- 预期 >= 12',
  '',
  '-- 6) 角色×版本授权模板未被 M2_05 摧毁',
  "-- SELECT edition, COUNT(*) FROM sys_edition_menu_template GROUP BY edition;  -- 预期各版本 > 0",
  '',
  '-- 7) 版本编码归一化：不存在残留 ENTERPRISE 生效编码',
  '-- SELECT edition, COUNT(*) FROM sys_tenant GROUP BY edition;',
  '',
  '-- 8) 角色数上限分层：基础版 2 / 高级版 4-6 / 专业版 NULL（不限）',
  '-- SELECT edition, max_roles, COUNT(*) FROM sys_tenant GROUP BY edition, max_roles;',
  '',
  '-- =====================================================================',
  '-- 本包不含的里程碑（归属其他分册，避免重复部署）',
  '-- ---------------------------------------------------------------------',
  '-- · M5 预算域 / M6 高级版增量        -> 《高级版/sql/》（基础版部分见《基础版/sql/》）',
  '-- · M7 专业版增量（多组织/多账套/开放 API） -> 《专业版/sql/》',
  '-- · M8 小程序（M8_01 / M8_02）        -> 各版本包均有（引导态门店与租户档案为全版本共用）',
  '-- · M11~M14 项目容器/业态功能/访问通道/商业化 -> 《高级版/sql/》《专业版/sql/》',
  '-- · M15 / M16 小微版专属              -> 《小微版/sql/》',
  '--   （例外：M15_12「资金平台管理菜单」为平台级 admin 菜单，跨版本共用，已纳入本包阶段五）',
  '-- · M9 升级流程与防串验收             -> 无 SQL，交付物为演练报告 / 测试报告',
  '-- =====================================================================',
  '',
].join(EOL);

fs.writeFileSync(IDX('系统管理'), SYS_LINES, 'utf8');
say('系统管理/sql/00_执行顺序.sql 已新建，行数 ' + SYS_LINES.split(EOL).length);

fs.writeFileSync('D:/workspace/RuoYi-Flowable-Plus-0.8.X/logs/_t13idx_result.txt', log.join('\n'), 'utf8');
