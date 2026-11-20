-- =====================================================================
-- M16_01 推广码与归因建表 + 推广管理菜单脚本（T16.1 / T16.5 / T16.6 / T16.10 / T16.11，v1.16 新增）
-- 版本：v1.0  日期：2026-09-10
-- 依据：《推广分销与激励方案》v1.0 第三章（推广码规则）/ 第五章（推广归因规则）/ 第九章（前端页面需求）
--       《资金管控系统开发任务文档》v1.16 T16.1 / T16.5 / T16.6 / T16.10 / T16.11
-- 前置依赖：M15_06（micro 菜单 7000~7499 + client_type）、M15_13（menu_id 7170~7179 已占用，本脚本避让）、
--           M16_03（FEAT_PROMOTION_DISTRIBUTION 开关；推广菜单按开关渲染，非小微版不可见）
-- ★版本适用硬约束（方案第二节）：本套推广分销规则仅「小微版」启用（基础版/高级版/专业版不部署、不开放入口）；
--   四个校验点（生成推广码 / 扫码归因 / 拉新奖励发放 / 老板后台配置）统一走 FEAT_PROMOTION_DISTRIBUTION，
--   禁止在业务代码各处散落 if(version==小微版) 判断（规范第 18 条）。
-- 规范：
--   1. promotion_code / promotion_attribution 均为【租户级表】(含 tenant_id)，多租户插件自动隔离，不入 IGNORE_TENANT_TABLES
--   2. ★推广码永久有效、一人一码：UNIQUE(tenant_id,user_id,code_type)，无过期/续期字段；离职/禁用仅关 status，扫码不再归因
--   3. ★归因只认最后一次扫码：promotion_attribution 以 UNIQUE(tenant_id,customer_user_id) 维护「当前推广人」，
--      last_scan_time 每次扫码更新即「交接点」，交接之前业绩已锁定不回溯；从未扫码 customer 推广人为 NULL(自然流量)
--   4. scene_param ≤ 32 字符（微信小程序码 scene 限制）：门店码 t=租户ID；个人码 t=租户ID&r=角色&u=用户ID（如 t=001&r=c&u=23）
--   5. 菜单：老板 PC「推广分销管理」7180~7188（micro:distribution:*，挂 7100 小微运营下）
--      + 员工小程序「我的推广码」7340~7342（micro:staff:promotion*）+ 顾客小程序「我的推广码」7440~7442（micro:customer:promotion*）
--   6. 全部新菜单授予超管 role_id=1 便于 admin 管理；业务角色授权由 role_template 驱动（见 M15_07 / M15_10）
--   7. 先删后建 / 先删后插幂等
-- =====================================================================

-- ----------------------------
-- 1. 推广码表 promotion_code（门店码 + 个人推广码；一人一码、永久有效）
-- ----------------------------
DROP TABLE IF EXISTS `promotion_code`;
CREATE TABLE `promotion_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '推广人用户ID(老板/业务员/顾客均有码)',
  `role_code` VARCHAR(30) NOT NULL COMMENT '推广人标准角色：boss/salesman/customer',
  `code_type` VARCHAR(20) NOT NULL DEFAULT 'personal' COMMENT '码类型：store(门店码 t=租户ID)/personal(个人推广码 t=租户ID&r=角色&u=用户ID)',
  `scene_param` VARCHAR(32) NOT NULL COMMENT '小程序码 scene 参数(≤32字符)：门店码 t=001；个人码 t=001&r=c&u=23',
  `qr_code_url` VARCHAR(500) DEFAULT NULL COMMENT '推广码图片URL(可保存到相册,用于朋友圈/私聊/打印)',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0启用 1禁用(离职/禁用后扫码不再归因,禁用前消费照常算)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(★码永久有效,无过期字段)',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user_type` (`tenant_id`, `user_id`, `code_type`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_scene` (`scene_param`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广码表(门店码/个人推广码;一人一码;永久有效;按tenant_id隔离)';

-- ----------------------------
-- 2. 推广归因关系表 promotion_attribution（谁最后扫码业绩归谁；交接点锁定不回溯）
-- ----------------------------
DROP TABLE IF EXISTS `promotion_attribution`;
CREATE TABLE `promotion_attribution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `customer_user_id` BIGINT NOT NULL COMMENT '被归因顾客用户ID',
  `customer_phone` VARCHAR(20) DEFAULT NULL COMMENT '顾客手机号(身份锚点,冗余便于统计与首单联动)',
  `promoter_user_id` BIGINT DEFAULT NULL COMMENT '当前推广人用户ID(NULL=自然流量,从未扫码)',
  `promoter_role_code` VARCHAR(30) DEFAULT NULL COMMENT '当前推广人角色：boss/salesman/customer',
  `last_scan_time` DATETIME DEFAULT NULL COMMENT '最后一次扫码时间(★交接点;之前业绩已锁定不回溯)',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '归因状态：0有效 1失效(推广人被禁用后新扫码不更新)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_customer` (`tenant_id`, `customer_user_id`),
  KEY `idx_tenant_promoter` (`tenant_id`, `promoter_user_id`),
  KEY `idx_tenant_phone` (`tenant_id`, `customer_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广归因关系表(谁最后扫码业绩归谁;交接点锁定不回溯;按tenant_id隔离)';

-- ----------------------------
-- 3. 推广分销菜单（先删后插幂等）
--    3.1 老板 PC「推广分销管理」7180~7188（挂 7100 小微运营下；micro:distribution:*）
--    3.2 员工小程序「我的推广码」7340~7342（挂 7300 业务员小程序下；client_type=mini）
--    3.3 顾客小程序「我的推广码」7440~7442（挂 7400 顾客小程序下；client_type=mini）
--    ★menu_id 避让：7170~7179(M15_13 顾客管理) / 7330(M15_11 活动浏览) / 7430(M15_11 活动浏览) 均已占用
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7180 AND 7199;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7180 AND 7199;
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7340 AND 7349;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7340 AND 7349;
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7440 AND 7449;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7440 AND 7449;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- ===== 3.1 老板 PC 推广分销管理（7180~7188）=====
(7180, '推广分销管理', 7100, 9, 'distribution', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'guide', 'admin', NOW(), '★仅小微版(FEAT_PROMOTION_DISTRIBUTION)：推广数据/消费归因/业绩统计/奖励配置/首单配置/推广码启停'),
(7181, '推广数据', 7180, 1, 'data', 'micro/distribution/data', NULL, 1, 0, 'C', '0', '0', 'micro:distribution:data', 'pc', '#', 'admin', NOW(), '查看所有推广人的推广数据(带来人数/已消费/拉新成功数)'),
(7182, '消费归因', 7180, 2, 'attribution', 'micro/distribution/attribution', NULL, 1, 0, 'C', '0', '0', 'micro:distribution:attribution', 'pc', '#', 'admin', NOW(), '查看消费记录及归因(谁最后扫码业绩归谁)'),
(7183, '业绩统计', 7180, 3, 'stat', 'micro/distribution/stat', NULL, 1, 0, 'C', '0', '0', 'micro:distribution:stat', 'pc', '#', 'admin', NOW(), '按天/周/月统计推广业绩'),
(7184, '拉新奖励配置', 7180, 4, 'rewardConfig', 'micro/distribution/rewardConfig', NULL, 1, 0, 'C', '0', '0', 'micro:distribution:rewardConfig', 'pc', '#', 'admin', NOW(), '★唯一可变项：员工拉新现金/顾客拉新优惠券(规则固定不可改,写 promotion_reward_config)'),
(7185, '配置奖励内容', 7184, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:distribution:editReward', 'pc', '#', 'admin', NOW(), '改奖励内容(折扣力度/送什么/每拉新奖多少),规则本身固定'),
(7186, '首单活动配置', 7180, 5, 'firstOrderConfig', 'micro/distribution/firstOrder', NULL, 1, 0, 'C', '0', '0', 'micro:distribution:firstOrderConfig', 'pc', '#', 'admin', NOW(), '配置首单活动力度(首次到店消费优惠,按手机号+活动独立判断)'),
(7187, '推广码管理', 7180, 6, 'code', 'micro/distribution/code', NULL, 1, 0, 'C', '0', '0', 'micro:distribution:code', 'pc', '#', 'admin', NOW(), '查看/启停全员推广码(门店码+个人码)'),
(7188, '启用/禁用推广码', 7187, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:distribution:toggleCode', 'pc', '#', 'admin', NOW(), '★禁用后新扫码不再归因;禁用前消费照常算(改 promotion_code.status)'),
-- ===== 3.2 员工小程序「我的推广码」（7340~7342）=====
(7340, '我的推广码', 7300, 4, 'myPromo', 'miniStaff/myPromo', NULL, 1, 0, 'C', '0', '0', 'micro:staff:promotion', 'mini', '#', 'admin', NOW(), '员工小程序：展示推广码(可长按保存)+推广数据+现金奖励(标记已线下发放)'),
(7341, '生成/查看推广码', 7340, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staff:promotionCode', 'mini', '#', 'admin', NOW(), '首次点击生成,之后一直用同一个码'),
(7342, '推广数据与奖励', 7340, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staff:promotionData', 'mini', '#', 'admin', NOW(), '本月推广/已消费(拉新成功)/累计奖励(现金)'),
-- ===== 3.3 顾客小程序「我的推广码」（7440~7442）=====
(7440, '我的推广码', 7400, 4, 'myPromo', 'miniCustomer/myPromo', NULL, 1, 0, 'C', '0', '0', 'micro:customer:promotion', 'mini', '#', 'admin', NOW(), '顾客小程序：展示推广码(可长按保存)+拉新数据+优惠券到账(站内券)'),
(7441, '生成/查看推广码', 7440, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:customer:promotionCode', 'mini', '#', 'admin', NOW(), '首次点击生成,之后一直用同一个码'),
(7442, '拉新数据与优惠券', 7440, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:customer:promotionData', 'mini', '#', 'admin', NOW(), '拉来人数/已消费/优惠券到账(站内券,非现金)');

-- 授予超管 role_id=1（业务角色授权由 role_template 驱动）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu`
 WHERE menu_id BETWEEN 7180 AND 7199
    OR menu_id BETWEEN 7340 AND 7349
    OR menu_id BETWEEN 7440 AND 7449;

-- ----------------------------
-- 4. 后端接口约定（Java 交付，登记供部署核对，不入本 SQL）
--    ★全部接口标注 @RequireFeature("FEAT_PROMOTION_DISTRIBUTION") + @SaCheckPermission；非小微版返回「当前版本不支持此功能」
--    T16.5 生成推广码：POST /micro/distribution/code/generate（首次生成、之后复用；门店码 scene=t=租户ID；
--          个人码 scene=t=租户ID&r=角色&u=用户ID，scene≤32 字符；返回 qr_code_url 可保存相册）
--    T16.6 扫码归因：POST /micro/distribution/attribution/scan（解析 tenant_id+role+user_id → upsert promotion_attribution；
--          谁最后扫码业绩归谁；推广人 status=禁用则不更新归因；从未扫码消费→promoter_user_id=NULL 自然流量）
--    T16.10 老板后台：GET /micro/distribution/data|attribution|stat；PUT /micro/distribution/rewardConfig；
--          PUT /micro/distribution/firstOrderConfig；PUT /micro/distribution/code/toggle
--    T16.11 小程序：GET /micro/distribution/my（我的推广码 + 推广数据 + 奖励/优惠券到账）
-- ----------------------------

-- ----------------------------
-- 5. 验收查询（执行后人工核对）
-- ----------------------------
-- 5.1 两张推广表均含 tenant_id + 唯一约束
-- SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id'
--   AND TABLE_NAME IN ('promotion_code','promotion_attribution');  -- 预期 2 行
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_code' AND INDEX_NAME='uk_tenant_user_type';        -- 预期 uk_tenant_user_type(一人一码)
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_attribution' AND INDEX_NAME='uk_tenant_customer';  -- 预期 uk_tenant_customer(当前推广人唯一)
--
-- 5.2 ★推广码永久有效：promotion_code 无任何过期/续期字段
-- SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_code' AND COLUMN_NAME IN ('expire_time','valid_until','renew_time');  -- 预期 0
--
-- 5.3 scene_param ≤ 32 字符（微信限制）
-- SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_code' AND COLUMN_NAME='scene_param';  -- 预期 32
--
-- 5.4 推广分销菜单落库（老板 PC 7180~7188 + 员工小程序 7340~7342 + 顾客小程序 7440~7442）
-- SELECT menu_id, menu_name, parent_id, perms, client_type FROM sys_menu
--  WHERE menu_id BETWEEN 7180 AND 7199 OR menu_id BETWEEN 7340 AND 7349 OR menu_id BETWEEN 7440 AND 7449 ORDER BY menu_id;
-- 预期：9(老板PC) + 3(员工小程序,client_type=mini) + 3(顾客小程序,client_type=mini) = 15 行
--
-- 5.5 ★menu_id 无冲突（避让 7170~7179 顾客管理 / 7330 / 7430 活动浏览）
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id IN (7170,7171,7172,7330,7430);  -- 预期 5（M15 既有,未被本脚本覆盖）
--
-- 5.6 新菜单已授权 admin(role_id=1)
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id=1
--   AND (menu_id BETWEEN 7180 AND 7199 OR menu_id BETWEEN 7340 AND 7349 OR menu_id BETWEEN 7440 AND 7449);  -- 预期 15
--
-- 5.7 ★归因只认最后扫码（运行时）：顾客扫 A 码消费→扫 B 码消费，promotion_attribution 仅一行且 promoter 随最后扫码更新，
--     交接前业绩记录不回溯；★跨租户隔离：A 店归因不出现在 B 店（tenant_id 过滤生效）
