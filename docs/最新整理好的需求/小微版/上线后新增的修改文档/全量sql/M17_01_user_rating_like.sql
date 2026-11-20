-- =====================================================================
-- M17_01 通用评价与点赞建表 + 评价管理菜单 + FEAT_RATING 功能开关脚本
-- （T17.1 / T17.2 / T17.3 / T17.4 / T17.5，v1.24 新增）
-- 版本：v1.0  日期：2026-09-16
-- 依据：《资金管控系统开发任务文档（小微版）》v1.24 M17 通用评价与点赞体系
--       T17.1~T17.5（评价与点赞表/评价提交接口/综合评分聚合/老板后台评价管理/版本开关）
-- 前置依赖：M1_05（feature_flag 表 + 33 项开关）、M16_03（第 34 项 FEAT_PROMOTION_DISTRIBUTION；本脚本追加第 35 项）、
--           M15_06（micro 菜单 7000~7499 + client_type）
-- ★版本适用硬约束：评价体系仅小微版付费版（micro_pro）启用，免费版（micro_lite）与基础版/高级版/专业版均不生效。
--   后端统一封装为功能开关 FEAT_RATING（以版本为维度整体开/关），禁止在业务代码各处散落版本判断（规范第 19 条）。
-- 规范：
--   1. user_rating / user_like 均为【租户级表】(含 tenant_id)，多租户插件自动隔离；跨租户互不影响
--   2. ★禁止自评：target_user_id ≠ author_user_id（UNIQUE 约束 + Java 层校验双重保障）
--   3. ★一条评价仅能回复一次（reply_content 非空即已回复，Java 层拦截重复回复）
--   4. ★图片须走 imgSecCheck 安全校验（微信内容安全接口），评价图片 URL 存入 images 字段（JSON 数组）
--   5. 评价管理菜单（老板 PC）：micro:rating:*，menu_id 7160~7164（避让 7170~7172 M15_13 顾客管理）
--   6. feature_flag 仅 INSERT 第 35 项 FEAT_RATING，先删后插幂等（不影响前 34 项）
--   7. 先删后建 / 先删后插幂等
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 评价表 user_rating（顾客对服务人员的评价，1~5 星 + 内容 + 图片 + 场景）
-- ----------------------------
DROP TABLE IF EXISTS `user_rating`;
CREATE TABLE `user_rating` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `target_user_id` BIGINT NOT NULL COMMENT '被评价人用户ID（服务人员；★target_user_id ≠ author_user_id，禁止自评）',
  `author_user_id` BIGINT NOT NULL COMMENT '评价人用户ID（顾客）',
  `rating` TINYINT NOT NULL COMMENT '评分（1~5 星；1=很差 2=较差 3=一般 4=满意 5=非常满意）',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '评价内容（必填，至少 1 字）',
  `images` VARCHAR(2000) DEFAULT NULL COMMENT '评价图片URL列表（JSON数组，最多 9 张；★图片须过 imgSecCheck）',
  `scene` VARCHAR(32) NOT NULL COMMENT '评价场景：booking_complete(预约完成)/service_done(服务完成)/visit_done(到店完成)',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID（booking_order.id，可空=非订单场景）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL(正常)/SHIELDED(已屏蔽，老板操作)',
  `reply_content` VARCHAR(500) DEFAULT NULL COMMENT '老板回复内容（★一条评价仅能回复一次，非空即已回复）',
  `reply_time` DATETIME DEFAULT NULL COMMENT '老板回复时间',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_target_author_scene_order` (`tenant_id`, `target_user_id`, `author_user_id`, `scene`, `order_id`),
  KEY `idx_tenant_target` (`tenant_id`, `target_user_id`),
  KEY `idx_tenant_author` (`tenant_id`, `author_user_id`),
  KEY `idx_tenant_scene` (`tenant_id`, `scene`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表(顾客对服务人员1~5星评价;禁止自评;按tenant_id隔离;仅micro_pro)';

-- ----------------------------
-- 2. 点赞表 user_like（对服务人员的点赞/踩；toggle 切换）
-- ----------------------------
DROP TABLE IF EXISTS `user_like`;
CREATE TABLE `user_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `target_user_id` BIGINT NOT NULL COMMENT '被点赞人用户ID（服务人员）',
  `author_user_id` BIGINT NOT NULL COMMENT '点赞人用户ID（顾客；★target_user_id ≠ author_user_id）',
  `scene` VARCHAR(32) NOT NULL COMMENT '点赞场景：service(服务点赞)/staff(员工点赞)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_target_author_scene` (`tenant_id`, `target_user_id`, `author_user_id`, `scene`),
  KEY `idx_tenant_target` (`tenant_id`, `target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表(顾客对服务人员点赞;toggle切换;按tenant_id隔离;仅micro_pro)';

-- ----------------------------
-- 3. 评价管理菜单（老板 PC，挂「小微运营」7100 下；menu_id 7160~7164，先删后插幂等）
--    ★menu_id 避让：7170~7172(M15_13 顾客管理) / 7180~7188(M16_01 推广分销) / 7189~7190(M15_18 奖励记录)
--      / 7195~7199(M15_21 固定支出)
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7160 AND 7169;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7160 AND 7169;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7160, '评价管理', 7100, 10, 'rating', 'micro/rating/index', NULL, 1, 0, 'C', '0', '0', 'micro:rating:list', 'pc', 'star', 'admin', NOW(), '★仅micro_pro(FEAT_RATING)：查看/筛选评价列表 + 回复评价 + 屏蔽不当评价 + 服务人员综合评分排行'),
(7161, '回复评价', 7160, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:rating:reply', 'pc', '#', 'admin', NOW(), '一条评价仅能回复一次（reply_content 非空即已回复）'),
(7162, '屏蔽评价', 7160, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:rating:shield', 'pc', '#', 'admin', NOW(), '将不当评价状态 NORMAL→SHIELDED（屏蔽后不计入综合评分）'),
(7163, '评价详情', 7160, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:rating:detail', 'pc', '#', 'admin', NOW(), '查看评价详情（含图片）'),
(7164, '综合评分排行', 7160, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:rating:ranking', 'pc', '#', 'admin', NOW(), '按 target_user_id 跨场景聚合平均评分/评价总数，服务人员综合评分排行');

-- 授予超管 role_id=1（业务角色授权由 role_template 驱动）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 7160 AND 7169;

-- ----------------------------
-- 4. 功能开关 FEAT_RATING（feature_flag 第 35 项，组 E=小微版专属）
--    ★basic/advanced/pro 三列均 = 0（三版本不生效）；仅小微版付费版（micro_pro）启用——
--      小微版启用路径由 Java 层按 tenant_type=micro / version_type=micro_pro 判定，
--      feature_flag 本行作为「评价总开关」供 @RequireFeature("FEAT_RATING") 统一校验；
--      ★不给 feature_flag 增列（该表为企业版/小微版共享）。
--    ★先删后插幂等：仅删本 code，不动前 34 项。
-- ----------------------------
DELETE FROM `feature_flag` WHERE `code` = 'FEAT_RATING';
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('FEAT_RATING', '通用评价与点赞(评价/点赞/综合评分/评价管理)', 'E', NULL, 0, 0, 0, '0', 'admin', NOW(),
 'T17.5；★仅小微版付费版(micro_pro)启用；micro_lite/basic/advanced/pro 均不生效；全部评价接口走本开关');

-- ----------------------------
-- 5. 后端接口约定（Java 交付，登记供部署核对，不入本 SQL）
--    ★全部接口标注 @RequireFeature("FEAT_RATING") + @SaCheckPermission；非 micro_pro 租户返回「当前版本不支持」
--    T17.2 评价提交：POST /micro/rating/submit
--          校验：rating 1~5 + content 非空 + scene 合法（booking_complete/service_done/visit_done）
--          + 图片走 imgSecCheck（微信内容安全接口）+ target_user_id ≠ author_user_id（禁止自评）
--          + UNIQUE(tenant_id,target_user_id,author_user_id,scene,order_id) 防重
--    T17.2 综合评分查询：GET /micro/rating/summary/{userId}
--          返回：平均评分(avg_rating) + 评价总数(total_count) + 各星级分布
--    T17.2 点赞 toggle：POST /micro/rating/like/toggle
--          已点赞→取消（DELETE）；未点赞→新增（INSERT）；UNIQUE(tenant_id,target_user_id,author_user_id,scene)
--    T17.3 综合评分聚合：按 target_user_id 跨场景聚合平均评分/评价总数；支持按 scene 分组聚合
--    T17.4 老板后台：GET /micro/rating/list（分页 + 筛选）/ PUT /micro/rating/reply（回复）
--          / PUT /micro/rating/shield（屏蔽 NORMAL→SHIELDED）/ GET /micro/rating/ranking（排行）
-- ----------------------------

-- ----------------------------
-- 6. 验收查询（执行后人工核对）
-- ----------------------------
-- 6.1 两张评价表均含 tenant_id + 唯一约束
-- SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND COLUMN_NAME='tenant_id' AND TABLE_NAME IN ('user_rating','user_like');  -- 预期 2 行
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='user_rating' AND INDEX_NAME='uk_tenant_target_author_scene_order';
--   -- 预期 uk_tenant_target_author_scene_order
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='user_like' AND INDEX_NAME='uk_tenant_target_author_scene';
--   -- 预期 uk_tenant_target_author_scene
--
-- 6.2 ★禁止自评设计：无 self-rating 行（target_user_id = author_user_id 不可能插入，UNIQUE 含两列）
-- SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='user_rating' AND COLUMN_NAME IN ('target_user_id','author_user_id');  -- 预期 2
--
-- 6.3 ★FEAT_RATING 落表且三版本均 disabled
-- SELECT code, flag_group, basic_enabled, advanced_enabled, pro_enabled, status FROM feature_flag
--  WHERE code='FEAT_RATING';  -- 预期 1 行：E / 0 / 0 / 0 / 0(启用)
--
-- 6.4 ★开关总数 35 项（M1_05 的 33 项 + M16_03 的 1 项 + 本脚本 1 项），组 E=2
-- SELECT COUNT(*) FROM feature_flag;  -- 预期 35
-- SELECT flag_group, COUNT(*) FROM feature_flag GROUP BY flag_group;
--   -- 预期 A=11/B=9/C=6/D=7/E=2
--
-- 6.5 评价管理菜单落库（老板 PC 7160~7164，挂 7100 小微运营下）
-- SELECT menu_id, menu_name, parent_id, perms, client_type FROM sys_menu
--  WHERE menu_id BETWEEN 7160 AND 7169 ORDER BY menu_id;
--  -- 预期 5 行：7160(micro:rating:list,pc) + 7161(reply) + 7162(shield) + 7163(detail) + 7164(ranking)
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id=1 AND menu_id BETWEEN 7160 AND 7169;  -- 预期 5
--
-- 6.6 ★menu_id 无冲突（避让 7170~7172 顾客管理 / 7180~7188 推广分销 / 7195~7199 固定支出）
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id IN (7170,7171,7172,7180,7195);
--   -- 预期 5（M15/M16 既有，未被本脚本覆盖）
--
-- 6.7 ★评价闭环验证（运行时，T17.2~T17.5 部署后）：
--   ① 顾客完成预约消费后 → POST /micro/rating/submit（rating=5,content="服务很好",scene=booking_complete）
--   ② 同一顾客同一服务同一订单再次评价 → 命中 UNIQUE 约束，返回「已评价」
--   ③ 顾客尝试给自己评价 → target_user_id=author_user_id → Java 校验拦截「不能评价自己」
--   ④ 老板 PC「评价管理」查看评价列表 → 回复评价（仅一次）→ 屏蔽不当评价（NORMAL→SHIELDED）
--   ⑤ GET /micro/rating/summary/{userId} → 返回该服务人员综合评分（跨场景聚合，SHIELDED 不计入）
--   ⑥ POST /micro/rating/like/toggle → 首次点赞→新增；再次调用→取消
--   ⑦ ★非 micro_pro 租户调任一评价接口 → FEAT_RATING 拦截，返回「当前版本不支持」
--   ⑧ ★micro_lite 租户调评价接口 → 同样拦截（lite 档不含评价能力）
--   ⑨ ★跨租户隔离：A 店评价不出现在 B 店（tenant_id 过滤生效）
