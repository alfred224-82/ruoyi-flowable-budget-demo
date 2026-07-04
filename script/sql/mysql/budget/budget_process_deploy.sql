-- ----------------------------
-- 预算审批流程部署SQL
-- 说明：创建三级审批角色（部门领导、分公司领导、总公司领导）
-- ----------------------------

-- ========== 1. 创建"部门领导"角色（如果不存在） ==========
INSERT INTO sys_role(role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 3, '部门领导', 'dept_leader', 3, 5, 1, 1, '0', '0', 'admin', sysdate(), '', NULL, '部门领导角色，用于预算审批流程（第1级）'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'dept_leader');

-- ========== 2. 创建"分公司领导"角色（如果不存在） ==========
INSERT INTO sys_role(role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 4, '分公司领导', 'branch_leader', 4, 5, 1, 1, '0', '0', 'admin', sysdate(), '', NULL, '分公司领导角色，用于预算审批流程（第2级）'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'branch_leader');

-- ========== 3. 创建"总公司领导"角色（如果不存在） ==========
INSERT INTO sys_role(role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 5, '总公司领导', 'hq_leader', 5, 5, 1, 1, '0', '0', 'admin', sysdate(), '', NULL, '总公司领导角色，用于预算审批流程（第3级）'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'hq_leader');

-- ========== 4. 为角色分配预算管理菜单权限 ==========
-- 部门领导菜单权限
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2000 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2000);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2001);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2002);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2011);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2012);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2013);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2014);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2015);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2016);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2017);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2018);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2019);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 3, 2020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=3 AND menu_id=2020);

-- 分公司领导菜单权限
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2000 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2000);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2001);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2002);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2011);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2012);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2013);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2014);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2015);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2016);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2017);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2018);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2019);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 4, 2020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=4 AND menu_id=2020);

-- 总公司领导菜单权限
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2000 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2000);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2001);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2002);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2011);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2012);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2013);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2014);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2015);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2016);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2017);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2018);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2019);
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 5, 2020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id=5 AND menu_id=2020);

-- ========== 5. 说明：流程定义部署 ==========
-- 预算审批流程定义（budgetProcess）通过应用启动时自动部署
-- 部署组件：com.ruoyi.system.config.BudgetProcessDeployer
-- 流程Key：budgetProcess
-- 流程节点：开始 → 部门领导审批 → 分公司领导审批 → 总公司领导审批 → 通过/驳回
-- 角色映射：
--   - 部门领导：role_id=3, role_key='dept_leader', Flowable候选组='ROLE3'
--   - 分公司领导：role_id=4, role_key='branch_leader', Flowable候选组='ROLE4'
--   - 总公司领导：role_id=5, role_key='hq_leader', Flowable候选组='ROLE5'
