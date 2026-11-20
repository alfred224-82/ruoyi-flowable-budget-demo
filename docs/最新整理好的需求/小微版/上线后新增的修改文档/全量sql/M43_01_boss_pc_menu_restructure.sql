-- =====================================================================
-- M43_01  Boss PC 菜单树重构（v4.2）
-- 日期：2026-11-04
-- =====================================================================
-- 概述：按用户设计的新菜单树重构 boss PC 端（client_type='pc'）菜单结构。
--       7100 小微运营 下只保留一层 M 目录 + C 叶子，去除多余中间层。
--       ★不含图标（icon 全部置空字符串）
-- 变更摘要：
--   1. 新建 4 个 M 目录：资源管理(7275)/员工管理(7276)/售后客诉(7277)/资金台账(7278)
--      ★避让 7260~7271（M25_01 投诉监管/平台监管台已占用）
--   2. 改名：7187→H5推广码 / 7189→业务员推广记录 / 7395→经营分析
--   3. 提升为 7100 直属：7151活动管理 / 7180营销推广
--   4. 重挂父级：18 个菜单移动
--   5. 删除：16 个废弃菜单（空目录/被合并/被替代/旧报表中心子页面）
--   6. 新建：7070 员工联系方式公开页（M34_01 未执行，本脚本补建）
--   7. role_template 完整重写（Step 8，先于 sys_role_menu 重建）
--   8. sys_role_menu 全量重建（Step 9，依赖 Step 8 的 role_template）
-- ★幂等：先删后插 + UPDATE 天然幂等，可重复执行
-- ★前置：M15_06 + M15_18 + M16_01 + M20_01 + M20_09 + M21_02 + M21_03
--       + M22_01 + M24_01 + M25_01 + M36_01 + M38M39_01
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;

-- =====================================================================
-- Step 1: 清理 role_menu（为后续重建做准备）
-- =====================================================================
DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (
    -- 将被删除的菜单
    7110, 7130, 7140, 7150, 7182, 7183, 7186, 7190, 7195, 7395
    -- 注：7190 旧为“数据与报表”，Step 2.4 删除后 Step 4 会以“顾客奖励记录”身份重建
);

-- =====================================================================
-- Step 2: 删除废弃菜单
-- =====================================================================
--   2.1 空目录 / 被合并的中间层
DELETE FROM `sys_menu` WHERE `menu_id` IN (
    7110,   -- 预约/订单管理（7111 已提升，空目录）
    7130,   -- 收支记录（M 目录，内容拆到资金台账）
    7140,   -- 报表中心（被经营分析 7395 替代）
    7150,   -- 推广发布（7151 提升后空目录）
    7195    -- 固定支出管理（M20_09 已废弃，7196 提升）
);

-- 2.1b 旧报表中心(7140)子页面——7140 已删除，子页面变孤儿，一并清理
--      （经营日报/月报/活动明细/导出CSV 已由新 7395~7399 替代）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7141, 7142, 7143, 7144);
DELETE FROM `sys_menu`      WHERE `menu_id` IN (7141, 7142, 7143, 7144);

-- 2.2 被合并/替代的功能页
DELETE FROM `sys_menu` WHERE `menu_id` IN (
    7182,   -- 消费归因（用户树中无此项）
    7183,   -- 业绩统计（用户树中无此项）
    7186    -- 首单活动配置（用户树中无此项）
);

-- 2.3 旧"经营报表"目录（7395→7399，被"经营分析"替代）
--     ★先删子菜单的旧关联，Step 5 会重建 7395
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7395, 7396, 7397, 7398, 7399);
DELETE FROM `sys_menu`      WHERE `menu_id` IN (7395, 7396, 7397, 7398, 7399);

-- 2.4 旧"数据与报表"目录 7190（内容已全部分散到其他目录）
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7190;
DELETE FROM `sys_menu`      WHERE `menu_id` = 7190;

-- =====================================================================
-- Step 3: 新建 M 目录（4 个）
-- =====================================================================
-- ★先删后插，保证幂等
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7275, 7276, 7277, 7278);
DELETE FROM `sys_menu`      WHERE `menu_id` IN (7275, 7276, 7277, 7278);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
(7275, '资源管理', 7100,  4, 'resource',      NULL, 1, 0, 'M', '0', '0', '', 'pc', '', 'admin', NOW(), 'Boss PC 资源管理目录（房间状态+资源价格）'),
(7276, '员工管理', 7100,  7, 'staff',         NULL, 1, 0, 'M', '0', '0', '', 'pc', '', 'admin', NOW(), 'Boss PC 员工管理目录（状态+排班+打卡+联系方式）'),
(7277, '售后客诉', 7100, 12, 'afterSales',    NULL, 1, 0, 'M', '0', '0', '', 'pc', '', 'admin', NOW(), 'Boss PC 售后客诉目录（投诉+异常+顾客+评价）'),
(7278, '资金台账', 7100, 10, 'FundLedger',    NULL, 1, 0, 'M', '0', '0', '', 'pc', '', 'admin', NOW(), 'Boss PC 资金台账目录（水位+收支+固定支出）');

-- =====================================================================
-- Step 3b: 补建 7070 员工联系方式公开页（M34_01 未执行，本脚本补建）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7070;
DELETE FROM `sys_menu`      WHERE `menu_id` = 7070;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
(7070, '员工联系方式', 7276, 4, 'staffContact', 'micro/staff/contactPublic/index', 1, 0, 'C', '0', '0', 'micro:staff:manage', 'pc', '', 'admin', NOW(), 'M34_01 补建：员工联系方式公开配置页（原计划由 M34_01 创建，未执行，本脚本代建）');

-- =====================================================================
-- Step 4: 改名
-- =====================================================================
UPDATE `sys_menu` SET `menu_name` = 'H5推广码',       `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7187;
UPDATE `sys_menu` SET `menu_name` = '业务员推广记录',  `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7189;
-- 7190 已在 Step 2.4 被删除（旧“数据与报表”），此处以“顾客奖励记录”身份重建
INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
(7190, '顾客奖励记录', 7180, 6, 'customerRecords', 'micro/distribution/customer/records', NULL, 1, 0, 'C', '0', '0', 'micro:distribution:customer:records', 'pc', '', 'admin', NOW(), '渠道=顾客：拉新优惠券/分享有礼/裂变奖励逐笔记录');

-- =====================================================================
-- Step 5: 新建"经营分析"目录 + 日报/月报（替代旧 7395~7399）
-- =====================================================================
INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
(7395, '经营分析', 7100, 14, 'operating',      NULL,                            1, 0, 'M', '0', '0', '',                           'pc', '', 'admin', NOW(), 'Boss PC 经营分析目录（日报+月报）'),
(7396, '经营日报', 7395,  1, 'daily',          'micro/operating/daily',          1, 0, 'C', '0', '0', 'micro:report:daily,micro:report:weekly', 'pc', '', 'admin', NOW(), '当日结算活动汇总 + 明细（含 7 天趋势 micro:report:weekly）'),
(7397, '经营月报', 7395,  2, 'monthly',        'micro/operating/monthly',        1, 0, 'C', '0', '0', 'micro:report:operating',      'pc', '', 'admin', NOW(), '月度经营报表（含固定成本分摊+税费净利+保本线）'),
(7398, '活动明细', 7395,  3, 'activityDetail', 'micro/operating/activityDetail', 1, 0, 'C', '0', '0', 'micro:report:activityDetail', 'pc', '', 'admin', NOW(), '活动明细表（一行一场+合计行）'),
(7399, '导出CSV',  7395,  4, '',                NULL,                            1, 0, 'F', '0', '0', 'micro:report:export',         'pc', '', 'admin', NOW(), '报表导出权限');

-- =====================================================================
-- Step 6: 移动菜单（UPDATE parent_id + order_num + icon 清空）
-- =====================================================================
-- 6.1 提升为 7100 直属
UPDATE `sys_menu` SET `parent_id` = 7100, `order_num` =  2, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7151;  -- 活动管理
UPDATE `sys_menu` SET `parent_id` = 7100, `order_num` =  5, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7180;  -- 营销推广

-- 6.2 营销推广(7180) 子菜单
UPDATE `sys_menu` SET `parent_id` = 7180, `order_num` =  5, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7189;  -- 业务员推广记录
UPDATE `sys_menu` SET `parent_id` = 7180, `order_num` =  6, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7190;  -- 顾客奖励记录

-- 6.3 资源管理(7275) 子菜单
UPDATE `sys_menu` SET `parent_id` = 7275, `order_num` =  1, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7204;  -- 房间状态管理
UPDATE `sys_menu` SET `parent_id` = 7275, `order_num` =  2, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7207;  -- 资源价格设置

-- 6.4 员工管理(7276) 子菜单
UPDATE `sys_menu` SET `parent_id` = 7276, `order_num` =  1, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7203;  -- 员工状态管理
UPDATE `sys_menu` SET `parent_id` = 7276, `order_num` =  2, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7230;  -- 员工排班
UPDATE `sys_menu` SET `parent_id` = 7276, `order_num` =  3, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7250;  -- 打卡记录
UPDATE `sys_menu` SET `parent_id` = 7276, `order_num` =  4, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7070;  -- 员工联系方式（M34_01 补建）

-- 6.5 人员结算(7120) 子菜单
UPDATE `sys_menu` SET `parent_id` = 7120, `order_num` =  2, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7126;  -- 结算历史

-- 6.6 资金台账(7278) 子菜单
UPDATE `sys_menu` SET `parent_id` = 7278, `order_num` =  1, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7145;  -- 资金水位
UPDATE `sys_menu` SET `parent_id` = 7278, `order_num` =  2, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7131;  -- 收入流水（原挂 7130）
UPDATE `sys_menu` SET `parent_id` = 7278, `order_num` =  3, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7133;  -- 支出流水（原挂 7130）
UPDATE `sys_menu` SET `parent_id` = 7278, `order_num` =  4, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7135;  -- 支出预警设置（原挂 7130）
UPDATE `sys_menu` SET `parent_id` = 7278, `order_num` =  5, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7196;  -- 固定支出列表

-- 6.7 售后客诉(7277) 子菜单
UPDATE `sys_menu` SET `parent_id` = 7277, `order_num` =  1, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7261;  -- 投诉列表
UPDATE `sys_menu` SET `parent_id` = 7277, `order_num` =  2, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7698;  -- 异常处理中心
UPDATE `sys_menu` SET `parent_id` = 7277, `order_num` =  3, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7170;  -- 顾客管理
UPDATE `sys_menu` SET `parent_id` = 7277, `order_num` =  4, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7160;  -- 评价管理

-- 6.8 F 按钮跟随父级移动
UPDATE `sys_menu` SET `parent_id` = 7187, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7188;  -- 启用/禁用推广码 → H5推广码
UPDATE `sys_menu` SET `parent_id` = 7184, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7185;  -- 配置奖励内容 → 拉新奖励配置

-- 6.9 清空所有 7100 系列 M 目录及推广数据(7181)的 icon（统一无图标）
UPDATE `sys_menu` SET `icon` = '', `update_by` = 'admin', `update_time` = NOW()
WHERE `parent_id` = 7100 AND `menu_type` = 'M' AND `menu_id` <> 7100;
UPDATE `sys_menu` SET `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7181;  -- 推广数据看板

-- 6.10 调整 7100 直属 C 菜单的 order_num（按用户树的顺序）
UPDATE `sys_menu` SET `order_num` =  1, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7101;  -- 经营驾驶舱
UPDATE `sys_menu` SET `order_num` =  3, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7111;  -- 预约列表
UPDATE `sys_menu` SET `order_num` =  8, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7360;  -- 活动结算单
UPDATE `sys_menu` SET `order_num` =  9, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7120;  -- 人员结算
UPDATE `sys_menu` SET `order_num` = 11, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7262;  -- 投诉处理(F, 挂7261下, 此处不动)

-- 6.11 7100 直属 M 目录排序
UPDATE `sys_menu` SET `order_num` =  4, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7275;  -- 资源管理
UPDATE `sys_menu` SET `order_num` =  6, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7276;  -- 员工管理
UPDATE `sys_menu` SET `order_num` = 10, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7278;  -- 资金台账
UPDATE `sys_menu` SET `order_num` = 12, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7277;  -- 售后客诉
UPDATE `sys_menu` SET `order_num` = 14, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7395;  -- 经营分析

-- =====================================================================
-- Step 7: 清理 7100 下不应直属的旧菜单（隐藏或归位）
-- =====================================================================
-- 7.1 7205/7206（F按钮，原挂7100）→ 改挂 7204（房间状态管理）
UPDATE `sys_menu` SET `parent_id` = 7204, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7205;
UPDATE `sys_menu` SET `parent_id` = 7204, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7206;

-- 7.2 7262（处理投诉 F按钮）→ 改挂 7261（投诉列表）
UPDATE `sys_menu` SET `parent_id` = 7261, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7262;

-- 7.3 7155（生成分享海报 F按钮）→ 改挂 7151（活动管理）
UPDATE `sys_menu` SET `parent_id` = 7151, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7155;

-- 7.4 7159（我的推广码）→ 用户树中无此项，但保留在 7151 活动管理下（不删除，避免丢失功能）
UPDATE `sys_menu` SET `parent_id` = 7151, `order_num` = 90, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7159;

-- 7.5 7618（语音合规记录）→ 改挂 7277 售后客诉
UPDATE `sys_menu` SET `parent_id` = 7277, `order_num` =  5, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7618;

-- 7.6 7690（合规命中记录）→ 改挂 7277 售后客诉
UPDATE `sys_menu` SET `parent_id` = 7277, `order_num` =  6, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7690;

-- 7.7 7650（员工业绩榜）→ 改挂 7395 经营分析
UPDATE `sys_menu` SET `parent_id` = 7395, `order_num` = 10, `icon` = '', `update_by` = 'admin', `update_time` = NOW() WHERE `menu_id` = 7650;

-- =====================================================================
-- Step 8: role_template 完整重写（必须先于 Step 9，sys_role_menu 依赖此数据）
-- =====================================================================
-- 8.1 micro_pro boss
DELETE FROM `role_template`
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss';

INSERT INTO `role_template` (`version_type`, `role_code`, `menu_keys`, `block_keys`, `status`, `create_by`, `remark`)
VALUES
('micro_pro', 'boss',
 JSON_ARRAY(
    -- 经营驾驶舱
    'micro:dashboard:view', 'micro:dashboard:today',
    -- 活动管理
    'micro:promo:list', 'micro:promo:add', 'micro:promo:edit', 'micro:promo:remove', 'micro:promo:poster',
    'micro:activity:setting:list', 'micro:promo:lock', 'micro:activity:browse',
    'micro:service:list', 'micro:service:add', 'micro:service:edit', 'micro:service:editConfirm', 'micro:service:toggle',
    'micro:shop:hours', 'micro:shop:payInfo', 'micro:notify:list', 'micro:notify:config',
    -- 预约列表
    'micro:booking:list', 'micro:booking:add', 'micro:booking:edit', 'micro:booking:cancel', 'micro:booking:complete', 'micro:order:detail',
    -- 营销推广
    'micro:myPromo:view',
    'micro:distribution:data', 'micro:distribution:attribution',
    'micro:distribution:stat', 'micro:distribution:rewardConfig',
    'micro:distribution:firstOrderConfig', 'micro:distribution:code',
    'micro:distribution:editReward', 'micro:distribution:toggleCode',
    'micro:distribution:salesman', 'micro:distribution:salesman:records',
    'micro:distribution:customer', 'micro:distribution:customer:records',
    -- 资源管理
    'micro:resourceStatus:room:view', 'micro:resourceStatus:release', 'micro:resourceStatus:log:list',
    'micro:resourcePrice:view', 'micro:resourcePrice:edit',
    'micro:room:layout', 'micro:room:uploadBg', 'micro:room:copyLayout', 'micro:room:saveTemplate',
    -- 员工管理
    'micro:resourceStatus:staff:view',
    'micro:staffSchedule:view', 'micro:staffSchedule:create', 'micro:staffSchedule:publish', 'micro:staffSchedule:edit',
    'micro:checkIn:view', 'micro:checkIn:supplement', 'micro:checkIn:export',
    'micro:staff:manage',
    'micro:staff:schedule', 'micro:staff:scheduleDetail',
    'micro:staff:income', 'micro:staff:incomeWeek', 'micro:staff:incomeMonth', 'micro:staff:incomeDetail',
    'micro:staffCommission:list', 'micro:staffCommission:add', 'micro:staffCommission:edit', 'micro:staffCommission:remove',
    -- 活动结算单
    'micro:settlement:list', 'micro:settlement:save', 'micro:settlement:submit', 'micro:settlement:lock',
    'micro:settlement:adjust', 'micro:settlement:export',
    -- 人员结算
    'micro:settle:list', 'micro:settle:create', 'micro:settle:generate', 'micro:settle:markPaid', 'micro:settle:detail', 'micro:settle:history',
    -- 资金台账
    'micro:report:cashFlow',
    'micro:income:list', 'micro:income:add', 'micro:expense:list', 'micro:expense:add', 'micro:budgetAlert:edit',
    'micro:pro:cost:list', 'micro:pro:cost:add', 'micro:pro:cost:edit', 'micro:pro:cost:remove',
    -- 售后客诉
    'micro:complaint:view', 'micro:complaint:handle',
    'micro:exception:list',
    'micro:customer:list', 'micro:customer:creditDetail', 'micro:customer:unblacklist',
    'micro:rating:list',
    'micro:compliance:records', 'micro:boss:complianceHit:list',
    'micro:customer:profile:view', 'micro:customer:profile:edit',
    'micro:customer:upgrade', 'micro:customer:profile:upgrade',
    -- 经营分析
    'micro:report:daily', 'micro:report:operating', 'micro:report:activityDetail',
    'micro:report:export', 'micro:report:reconcile',
    'micro:report:weekly', 'micro:report:monthly', 'micro:report:profit',
    'micro:staffRanking:list',
    -- OSS
    'system:oss:upload', 'system:oss:query', 'system:oss:remove'
 ),
 JSON_ARRAY(),
 '0', 'admin',
 'micro_pro boss：M43 菜单树重构后完整权限');

-- 8.2 micro_lite boss（lite 版精简权限，不含付费档功能）
DELETE FROM `role_template`
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss';

INSERT INTO `role_template` (`version_type`, `role_code`, `menu_keys`, `block_keys`, `status`, `create_by`, `remark`)
VALUES
('micro_lite', 'boss',
 JSON_ARRAY(
    -- 经营驾驶舱
    'micro:dashboard:view',
    -- 活动管理
    'micro:promo:list', 'micro:activity:setting:list',
    -- 预约列表
    'micro:booking:list',
    -- 营销推广（lite 不含分销）
    'micro:myPromo:view',
    -- 资源管理（lite 不含资源价格/房间布局）
    'micro:resourceStatus:room:view',
    -- 员工管理（lite 不含排班/打卡）
    'micro:resourceStatus:staff:view',
    -- 售后客诉
    'micro:complaint:view', 'micro:complaint:handle',
    'micro:customer:list',
    'micro:rating:list',
    -- OSS
    'system:oss:upload', 'system:oss:query',
    -- lite 基础
    'micro:lite:activity:publish', 'micro:lite:activity:view', 'micro:lite:boss:home',
    'micro:customer:activity:browse'
 ),
 JSON_ARRAY(),
 '0', 'admin',
 'micro_lite boss：M43 菜单树重构后基础权限');

-- =====================================================================
-- Step 9: sys_role_menu 全量重建（依赖 Step 8 已写入的 role_template）
-- =====================================================================
-- 9.1 清理 boss 在 7000~7699 区间的旧关联
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro'
  AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND rm.`menu_id` BETWEEN 7000 AND 7699;

-- 9.2 按 role_template menu_keys 匹配 sys_menu.perms 重建
--     ★用 FIND_IN_SET 兼容逗号分隔多权限（如 micro:report:daily,micro:report:weekly）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type` AND rt.`role_code` = r.`role_key` AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON FIND_IN_SET(j.`perm`, m.`perms`) > 0
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro'
  AND m.`menu_type` IN ('M', 'C', 'F')
  AND m.`perms` IS NOT NULL AND m.`perms` <> '';

-- 9.3 显式补入 M 类型目录（perms 为空，无法通过 perms 匹配）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, dirs.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN (
    SELECT 7100 AS `menu_id` UNION ALL  -- 小微运营（根）
    SELECT 7180 UNION ALL               -- 营销推广
    SELECT 7275 UNION ALL               -- 资源管理
    SELECT 7276 UNION ALL               -- 员工管理
    SELECT 7120 UNION ALL               -- 人员结算
    SELECT 7277 UNION ALL               -- 售后客诉
    SELECT 7278 UNION ALL               -- 资金台账
    SELECT 7395 UNION ALL               -- 经营分析
    SELECT 7151                         -- 活动管理（C 但作为提升后的直属）
) dirs
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro';

-- 9.4 超管 role_id=1 补录（全量）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` BETWEEN 7100 AND 7699;

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 1. 7100 直属子菜单（预期 14 项：10个M目录 + 4个直属C/F）
-- SELECT menu_id, menu_name, parent_id, menu_type, order_num
--   FROM sys_menu WHERE parent_id = 7100 AND client_type = 'pc'
--   ORDER BY order_num;
--
-- 2. 营销推广(7180)子菜单（预期 6 项）
-- SELECT menu_id, menu_name, parent_id, menu_type, order_num
--   FROM sys_menu WHERE parent_id = 7180 ORDER BY order_num;
-- 预期：7187 H5推广码 / 7181 推广数据看板 / 7184 拉新奖励配置
--       7189 业务员推广记录 / 7190 顾客奖励记录 + F按钮
--
-- 3. 资源管理(7275)子菜单（预期 2 项 + F按钮）
-- SELECT menu_id, menu_name, parent_id FROM sys_menu WHERE parent_id = 7275;
-- 预期：7204 房间状态管理 / 7207 资源价格设置
--
-- 4. 员工管理(7276)子菜单（预期 4 项 + F按钮）
-- SELECT menu_id, menu_name, parent_id FROM sys_menu WHERE parent_id = 7276;
-- 预期：7203 员工状态管理 / 7230 员工排班 / 7250 打卡记录 / 7070 员工联系方式
--
-- 5. 售后客诉(7277)子菜单（预期 6 项）
-- SELECT menu_id, menu_name, parent_id FROM sys_menu WHERE parent_id = 7277;
-- 预期：7261 投诉列表 / 7698 异常处理中心 / 7170 顾客管理 / 7160 评价管理 / 7618 / 7690
--
-- 6. 资金台账(7278)子菜单（预期 5 项）
-- SELECT menu_id, menu_name, parent_id FROM sys_menu WHERE parent_id = 7278;
-- 预期：7145 资金水位 / 7131 收入流水 / 7133 支出流水 / 7135 支出预警设置 / 7196 固定支出列表
--
-- 7. 经营分析(7395)子菜单
-- SELECT menu_id, menu_name, parent_id FROM sys_menu WHERE parent_id = 7395;
-- 预期：7396 经营日报 / 7397 经营月报 / 7398 活动明细 / 7399 导出CSV / 7650 员工业绩榜
--
-- 8. role_template 权限键数
-- SELECT version_type, JSON_LENGTH(menu_keys) AS cnt
--   FROM role_template WHERE role_code = 'boss' AND version_type IN ('micro_pro','micro_lite');
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

SET SQL_SAFE_UPDATES = 1;
