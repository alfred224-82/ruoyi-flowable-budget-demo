-- =====================================================================
-- M2_02 角色×版本菜单授权模板脚本（T2.2，防串第④层）
-- 版本：v2.2  日期：2026-09-06
-- v2.2 变更（对齐 v1.12《基础版操作手册》v2.0 验收口径）：
--      1. BOSS_BASIC 按「2 角色、boss 兼任财务/经理」重排：补收入确认(3202)/收入核验(3210)/
--         收款单管理(3207)/报销管理(3004 精简)六大菜单，付款单补 3267 新增，
--         boss 在 PC 端跑通项目资金流 + 轻量记账全闭环；
--      2. BOSS_BASIC 移除预算管理目录 2000 / 预算审核 2002 / 老板看板 3215（已下移高级版；
--         目录 2000 由 M2_10 v1.1 纳入下移集合统一回收，避免侧边栏空壳目录）；
--      3. BOSS_ADVANCED 显式承接 2000/2002/2017/2018/3215/3288（定总额 + 老板看板，
--         高级版首页恢复看板）
-- v2.1 变更：重建改为幂等非破坏——CREATE IF NOT EXISTS + 仅清空「角色×版本」模板键，
--      保留 BASIC/ADVANCED/PROFESSIONAL 版本全量键（M5_xx/M6_xx/M11_xx/M13_xx 登记），
--      与 M2_05 v1.1 互不摧毁，重复执行/乱序执行不再清空对方模板族
-- 依据：《各角色菜单权限文档》（v2.4）第三节（累计制：高级=基础全部+高级增量；
--       专业=高级全部+专业增量）；《资金管控系统开发任务文档》T2.2
-- v2.0 变更（重要）：
--   1. 模板粒度由"版本全量"改为"角色×版本"（BOSS_/FINANCE_/MANAGER_/EMPLOYEE_/
--      PLATFORM_ADMIN_ × BASIC/ADVANCED/PROFESSIONAL），开通租户时按角色分别授权，
--      解决此前"全量模板复制给所有角色"导致老板菜单过多的问题
--   2. 老板（BOSS）基础版按职责裁剪为：定总额、建项目、审批项目/付款/变更、看板；
--      报销单审批仅保留按钮权限（3041/3045），不挂报销单管理菜单（审批走小程序）
--   3. 配合租户级克隆角色：开通租户时按模板克隆租户专属角色（绑定 tenant_id），
--      模板菜单复制到克隆角色，版本变更仅影响本租户（见 SysTenantServiceImpl）
-- 硬约束：
--   1. 低版本模板不含任何高版本菜单（防串第④层）
--   2. 开通/升级租户时按模板复制授权到克隆角色的 sys_role_menu，禁止手工为低版本
--      租户添加高版本菜单
--   3. 菜单与版本解耦：即使误分配，API 层 @RequireFeature 仍拦截（②③层底线）
-- =====================================================================

-- v2.1：幂等非破坏重建（仅清空角色×版本键，保留版本全量键）
CREATE TABLE IF NOT EXISTS `sys_edition_menu_template` (
  `edition` VARCHAR(30) NOT NULL COMMENT '模板Key：角色前缀_版本（如 BOSS_BASIC / PLATFORM_ADMIN_ADVANCED）',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`edition`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色×版本菜单授权模板(开通/升级租户的唯一授权依据)';

DELETE FROM `sys_edition_menu_template`
 WHERE `edition` LIKE 'BOSS\_%'
    OR `edition` LIKE 'FINANCE\_%'
    OR `edition` LIKE 'MANAGER\_%'
    OR `edition` LIKE 'EMPLOYEE\_%'
    OR `edition` LIKE 'PLATFORM\_ADMIN\_%';

-- =====================================================================
-- 1. 老板（BOSS）
-- 基础版职责（v1.12）：建项目、审批项目/付款/变更/收款、收入确认与核验、
--                  报销单 PC 端单级极简确认审批（兼任财务/经理）；无预算域、无老板看板
-- =====================================================================

-- 基础版（v1.12：2 角色，boss 兼任财务/经理，PC 端全量资金流 + 轻量记账）
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
-- 目录（预算管理 2000 已下移高级版，基础版不挂）
('BOSS_BASIC', 3000),
-- 建项目 + 审批项目：项目管理（查询/新增/审批）+ 隐藏页（详情/新建）
('BOSS_BASIC', 3201), ('BOSS_BASIC', 3251), ('BOSS_BASIC', 3252), ('BOSS_BASIC', 3254),
('BOSS_BASIC', 3231), ('BOSS_BASIC', 3232),
-- 收入确认（查询/新增）+ 收入核验（通过/驳回）：boss 兼任经理/财务核验
('BOSS_BASIC', 3202), ('BOSS_BASIC', 3256), ('BOSS_BASIC', 3257),
('BOSS_BASIC', 3210), ('BOSS_BASIC', 3278), ('BOSS_BASIC', 3279),
-- 审批变更：合同变更（查询/审批）
('BOSS_BASIC', 3203), ('BOSS_BASIC', 3258), ('BOSS_BASIC', 3260),
-- 登记+审批付款：付款单管理（查询/新增/审批/驳回）
('BOSS_BASIC', 3206), ('BOSS_BASIC', 3266), ('BOSS_BASIC', 3267), ('BOSS_BASIC', 3268), ('BOSS_BASIC', 3269),
-- 登记+审核收款：收款单管理（查询/新增/审核）
('BOSS_BASIC', 3207), ('BOSS_BASIC', 3270), ('BOSS_BASIC', 3271), ('BOSS_BASIC', 3272),
-- 报销管理（精简）：查询/新增/提交/修改/删除/审批 + 隐藏页详情/表单
('BOSS_BASIC', 3004), ('BOSS_BASIC', 3041), ('BOSS_BASIC', 3042), ('BOSS_BASIC', 3043),
('BOSS_BASIC', 3044), ('BOSS_BASIC', 3045), ('BOSS_BASIC', 3046),
('BOSS_BASIC', 3124), ('BOSS_BASIC', 3125),
-- 消息中心（全角色公共）
('BOSS_BASIC', 3114);

-- 高级版 = 基础版 + 承接下移（预算管理目录/预算审核/老板看板）+ 项目利润报表（只读）+ 打卡记录（只读）
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'BOSS_ADVANCED', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'BOSS_BASIC';
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
-- 承接下移：预算管理目录 + 定总额（预算审核审批/驳回）+ 老板看板（高级版首页）
('BOSS_ADVANCED', 2000), ('BOSS_ADVANCED', 2002), ('BOSS_ADVANCED', 2017), ('BOSS_ADVANCED', 2018),
('BOSS_ADVANCED', 3215), ('BOSS_ADVANCED', 3288),
('BOSS_ADVANCED', 3218), ('BOSS_ADVANCED', 3293),
('BOSS_ADVANCED', 3220), ('BOSS_ADVANCED', 3242);

-- 专业版 = 高级版 + 看板团队人力分布卡片 + 组织预算编制（只读）
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'BOSS_PROFESSIONAL', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'BOSS_ADVANCED';
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('BOSS_PROFESSIONAL', 3294),
('BOSS_PROFESSIONAL', 3225), ('BOSS_PROFESSIONAL', 3250);

-- =====================================================================
-- 2. 财务（FINANCE）：单据录入 + 规则配置主力
-- =====================================================================

-- 基础版
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
-- 目录
('FINANCE_BASIC', 2000), ('FINANCE_BASIC', 3000),
-- 预算编制（查询/新增/修改/删除/导出/提交 + 编制向导）
('FINANCE_BASIC', 2001), ('FINANCE_BASIC', 2011), ('FINANCE_BASIC', 2012), ('FINANCE_BASIC', 2013),
('FINANCE_BASIC', 2014), ('FINANCE_BASIC', 2015), ('FINANCE_BASIC', 2016), ('FINANCE_BASIC', 2019),
-- 预算审核（查询 + 审核详情）
('FINANCE_BASIC', 2002), ('FINANCE_BASIC', 2020),
-- 预算科目
('FINANCE_BASIC', 2003), ('FINANCE_BASIC', 2031), ('FINANCE_BASIC', 2032), ('FINANCE_BASIC', 2033),
('FINANCE_BASIC', 2034), ('FINANCE_BASIC', 2035),
-- 报销单管理（查询/审批/修改 + 详情）
('FINANCE_BASIC', 3004), ('FINANCE_BASIC', 3041), ('FINANCE_BASIC', 3045), ('FINANCE_BASIC', 3046),
('FINANCE_BASIC', 3124),
-- 收入核验 / 收入确认
('FINANCE_BASIC', 3210), ('FINANCE_BASIC', 3278), ('FINANCE_BASIC', 3279),
('FINANCE_BASIC', 3202), ('FINANCE_BASIC', 3256),
-- 凭证导出 / 收款单 / 付款单
('FINANCE_BASIC', 3209), ('FINANCE_BASIC', 3276), ('FINANCE_BASIC', 3277),
('FINANCE_BASIC', 3207), ('FINANCE_BASIC', 3270), ('FINANCE_BASIC', 3271), ('FINANCE_BASIC', 3272),
('FINANCE_BASIC', 3206), ('FINANCE_BASIC', 3266), ('FINANCE_BASIC', 3267),
-- 项目管理（查询/新增/编辑 + 隐藏页）
('FINANCE_BASIC', 3201), ('FINANCE_BASIC', 3251), ('FINANCE_BASIC', 3252), ('FINANCE_BASIC', 3253),
('FINANCE_BASIC', 3231), ('FINANCE_BASIC', 3232), ('FINANCE_BASIC', 3233),
-- 合同变更（查询/收款确认）/ 烂账（查询/跟进）
('FINANCE_BASIC', 3203), ('FINANCE_BASIC', 3258), ('FINANCE_BASIC', 3261),
('FINANCE_BASIC', 3208), ('FINANCE_BASIC', 3273), ('FINANCE_BASIC', 3275),
-- 分成管理 / 资产折旧
('FINANCE_BASIC', 3204), ('FINANCE_BASIC', 3262), ('FINANCE_BASIC', 3263),
('FINANCE_BASIC', 3205), ('FINANCE_BASIC', 3264), ('FINANCE_BASIC', 3265),
-- 隐形成本配置：薪资社保 / 折旧规则 / 贷款利率
('FINANCE_BASIC', 3211), ('FINANCE_BASIC', 3280), ('FINANCE_BASIC', 3281),
('FINANCE_BASIC', 3212), ('FINANCE_BASIC', 3282), ('FINANCE_BASIC', 3283),
('FINANCE_BASIC', 3213), ('FINANCE_BASIC', 3284), ('FINANCE_BASIC', 3285),
-- 项目关联规则（只读）
('FINANCE_BASIC', 3216), ('FINANCE_BASIC', 3289),
-- 既有费用菜单：用途标签 / 映射规则 / 费用分摊 / 预算调整申请
('FINANCE_BASIC', 3002), ('FINANCE_BASIC', 3021), ('FINANCE_BASIC', 3022), ('FINANCE_BASIC', 3023),
('FINANCE_BASIC', 3024),
('FINANCE_BASIC', 3003), ('FINANCE_BASIC', 3031), ('FINANCE_BASIC', 3032), ('FINANCE_BASIC', 3033),
('FINANCE_BASIC', 3034), ('FINANCE_BASIC', 3035),
('FINANCE_BASIC', 3008), ('FINANCE_BASIC', 3071), ('FINANCE_BASIC', 3072),
('FINANCE_BASIC', 3010), ('FINANCE_BASIC', 3091), ('FINANCE_BASIC', 3092),
-- 报表 / 日志 / 预留预算 / 责任中心
('FINANCE_BASIC', 3006), ('FINANCE_BASIC', 3061),
('FINANCE_BASIC', 3007),
('FINANCE_BASIC', 3015), ('FINANCE_BASIC', 3101), ('FINANCE_BASIC', 3102),
('FINANCE_BASIC', 3009), ('FINANCE_BASIC', 3081), ('FINANCE_BASIC', 3082), ('FINANCE_BASIC', 3083),
('FINANCE_BASIC', 3084), ('FINANCE_BASIC', 3085),
('FINANCE_BASIC', 3001), ('FINANCE_BASIC', 3011), ('FINANCE_BASIC', 3012), ('FINANCE_BASIC', 3013),
('FINANCE_BASIC', 3014),
-- 消息中心
('FINANCE_BASIC', 3114);

-- 高级版 = 基础版 + 部门预算编制/打卡/信用/映射规则增强/自定义模板/项目利润报表
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'FINANCE_ADVANCED', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'FINANCE_BASIC';
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('FINANCE_ADVANCED', 3217), ('FINANCE_ADVANCED', 3291), ('FINANCE_ADVANCED', 3292),
('FINANCE_ADVANCED', 3219), ('FINANCE_ADVANCED', 3241),
('FINANCE_ADVANCED', 3220), ('FINANCE_ADVANCED', 3242),
('FINANCE_ADVANCED', 3221), ('FINANCE_ADVANCED', 3243), ('FINANCE_ADVANCED', 3244),
('FINANCE_ADVANCED', 3222), ('FINANCE_ADVANCED', 3245), ('FINANCE_ADVANCED', 3246),
('FINANCE_ADVANCED', 3296), ('FINANCE_ADVANCED', 3297),
('FINANCE_ADVANCED', 3223), ('FINANCE_ADVANCED', 3247), ('FINANCE_ADVANCED', 3248),
('FINANCE_ADVANCED', 3218), ('FINANCE_ADVANCED', 3293);

-- 专业版 = 高级版 + 组织管理/组织预算编制/人脸核身/二维导出
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'FINANCE_PROFESSIONAL', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'FINANCE_ADVANCED';
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('FINANCE_PROFESSIONAL', 3224), ('FINANCE_PROFESSIONAL', 3249),
('FINANCE_PROFESSIONAL', 3225), ('FINANCE_PROFESSIONAL', 3250), ('FINANCE_PROFESSIONAL', 3298),
('FINANCE_PROFESSIONAL', 3226), ('FINANCE_PROFESSIONAL', 3299),
('FINANCE_PROFESSIONAL', 3295);

-- =====================================================================
-- 3. 项目经理（MANAGER）：确认收入、盯利润、发起变更（数据范围限本人项目）
-- =====================================================================

-- 基础版
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
-- 目录
('MANAGER_BASIC', 3000),
-- 项目管理（查询/新增 + 详情/成员）
('MANAGER_BASIC', 3201), ('MANAGER_BASIC', 3251), ('MANAGER_BASIC', 3252),
('MANAGER_BASIC', 3231), ('MANAGER_BASIC', 3233),
-- 收入确认（查询/新增）
('MANAGER_BASIC', 3202), ('MANAGER_BASIC', 3256), ('MANAGER_BASIC', 3257),
-- 合同变更（查询/发起）
('MANAGER_BASIC', 3203), ('MANAGER_BASIC', 3258), ('MANAGER_BASIC', 3259),
-- 分成/资产（只读）/ 烂账（查询/上报）
('MANAGER_BASIC', 3204), ('MANAGER_BASIC', 3262),
('MANAGER_BASIC', 3205), ('MANAGER_BASIC', 3264),
('MANAGER_BASIC', 3208), ('MANAGER_BASIC', 3273), ('MANAGER_BASIC', 3275),
-- 报销单（兼任报销人，项目必填）
('MANAGER_BASIC', 3004), ('MANAGER_BASIC', 3041), ('MANAGER_BASIC', 3042), ('MANAGER_BASIC', 3043),
('MANAGER_BASIC', 3046), ('MANAGER_BASIC', 3044),
('MANAGER_BASIC', 3124), ('MANAGER_BASIC', 3125),
-- 消息中心
('MANAGER_BASIC', 3114);

-- 高级版 = 基础版 + 项目利润报表（限本人项目）
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'MANAGER_ADVANCED', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'MANAGER_BASIC';
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('MANAGER_ADVANCED', 3218), ('MANAGER_ADVANCED', 3293);

-- 专业版 = 高级版（增量在小程序端，无新增菜单）
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'MANAGER_PROFESSIONAL', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'MANAGER_ADVANCED';

-- =====================================================================
-- 4. 报销人（EMPLOYEE）：填报销单、选项目（可选）、补票
-- =====================================================================

-- 基础版
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
-- 目录
('EMPLOYEE_BASIC', 3000),
-- 报销单管理（查询/新增/提交/修改/删除 + 详情/表单）
('EMPLOYEE_BASIC', 3004), ('EMPLOYEE_BASIC', 3041), ('EMPLOYEE_BASIC', 3042), ('EMPLOYEE_BASIC', 3043),
('EMPLOYEE_BASIC', 3046), ('EMPLOYEE_BASIC', 3044),
('EMPLOYEE_BASIC', 3124), ('EMPLOYEE_BASIC', 3125),
-- 消息中心
('EMPLOYEE_BASIC', 3114);

-- 高级版 / 专业版 = 基础版（增量在小程序端，无新增菜单）
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'EMPLOYEE_ADVANCED', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'EMPLOYEE_BASIC';
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'EMPLOYEE_PROFESSIONAL', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'EMPLOYEE_ADVANCED';

-- =====================================================================
-- 5. 租户组织管理员（PLATFORM_ADMIN，按版本区分，不随业务模板）
-- v2.3：租户管理（6000 系列）由 admin 超管独占，platform_admin 仅持有用户/部门管理
--       高级版起增加角色管理（101）
-- =====================================================================

-- 基础版：用户管理 + 部门管理 + 消息中心
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('PLATFORM_ADMIN_BASIC', 1),      -- 系统管理目录(用户/部门挂载于此)
-- 用户管理（租户管理员创建业务用户）
('PLATFORM_ADMIN_BASIC', 100),
('PLATFORM_ADMIN_BASIC', 1001), ('PLATFORM_ADMIN_BASIC', 1002), ('PLATFORM_ADMIN_BASIC', 1003),
('PLATFORM_ADMIN_BASIC', 1004), ('PLATFORM_ADMIN_BASIC', 1005), ('PLATFORM_ADMIN_BASIC', 1006),
-- 部门管理（创建用户时需选择部门）
('PLATFORM_ADMIN_BASIC', 103),
('PLATFORM_ADMIN_BASIC', 1030), ('PLATFORM_ADMIN_BASIC', 1031), ('PLATFORM_ADMIN_BASIC', 1032),
('PLATFORM_ADMIN_BASIC', 1033),
-- 消息中心
('PLATFORM_ADMIN_BASIC', 3114);

-- 高级版 = 基础版全部 + 角色管理
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'PLATFORM_ADMIN_ADVANCED', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'PLATFORM_ADMIN_BASIC';
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('PLATFORM_ADMIN_ADVANCED', 101),
('PLATFORM_ADMIN_ADVANCED', 1010), ('PLATFORM_ADMIN_ADVANCED', 1011), ('PLATFORM_ADMIN_ADVANCED', 1012),
('PLATFORM_ADMIN_ADVANCED', 1013), ('PLATFORM_ADMIN_ADVANCED', 1014);

-- 专业版 = 高级版全部（无新增组织管理菜单）
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`)
SELECT 'PLATFORM_ADMIN_PROFESSIONAL', `menu_id` FROM `sys_edition_menu_template` WHERE `edition` = 'PLATFORM_ADMIN_ADVANCED';

-- =====================================================================
-- 防串校验查询（验收用：低版本模板不得包含任何高版本菜单）
-- =====================================================================
-- SELECT COUNT(*) FROM sys_edition_menu_template
--  WHERE edition IN ('BOSS_BASIC', 'FINANCE_BASIC', 'MANAGER_BASIC', 'EMPLOYEE_BASIC', 'PLATFORM_ADMIN_BASIC')
--    AND menu_id IN (3217,3218,3219,3220,3221,3222,3223,
--        3241,3242,3243,3244,3245,3246,3247,3248,3291,3292,3293,3296,3297,
--        3224,3225,3226,3249,3250,3294,3295,3298,3299,101,1010,1011,1012,1013,1014);
-- 预期结果：0（注意：老板基础版 3288 看板为看板查询，不在此列）
