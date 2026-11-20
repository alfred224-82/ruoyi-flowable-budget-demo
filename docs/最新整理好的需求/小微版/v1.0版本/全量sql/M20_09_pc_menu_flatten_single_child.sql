-- =====================================================================
-- M20_09 PC 菜单拍平：目录下仅一个可见子菜单时提升到上级（V3.7，v1.66，2026-09-30）
-- ---------------------------------------------------------------------
-- 需求：PC 菜单「保留一层」——若某目录（menu_type='M'）下只有一个可见菜单，
--       则把该菜单提升到目录所在层级，消除「目录 → 唯一子菜单」的无意义点击层。
--
-- 扫描结果（client_type='pc'，仅 1 个 visible='0' 子菜单的 M 目录）共 5 个，处理策略：
--   ① 6000 租户管理      (仅 6001 租户列表)                          → 6001 提到 1300，path=tenant；删除空目录 6000
--   ② 7020 业务角色管理  (仅 7021 角色列表)                          → 7021 提到 7010，path=tenantRole；删除空目录 7020
--   ③ 7050 基础数据配置  (可见仅 7058 店铺信息设置；含隐藏 7051/7055) → 7058 提到 7010（path=shop 不变）；7050 隐藏(visible='1')保留
--   ④ 7110 预约/订单管理 (可见仅 7111 预约列表；含隐藏 7116 订单详情) → 7111 提到 7100，path=bookingList；7110 隐藏(visible='1')保留
--   ⑤ 7195 固定支出管理  (仅 7196 固定支出列表 + F 按钮 7197~7199)   → 7196 提到 7190，path=cost；F 按钮改挂 7196；删除空目录 7195
--
-- ★隐藏目录（7050/7110）不删除：其下隐藏子页（7051 员工档案 / 7055 结算规则设置 / 7116 订单详情）
--   仍通过既有路由（/microConfig/basicData/*、/microOps/booking/detail）访问，删除会破坏前端跳转。
-- ★role_template 不受影响：被删/隐藏的 M 目录 perms 均为 NULL，menu_keys 按 perms 字符串授权，不引用这些 menu_id。
-- ★前端联动：7111 路由由 /microOps/booking/list 改为 /microOps/bookingList，
--   已同步更新 ruoyi-ui 中 4 处硬编码引用（router/index.js activeMenu、dashboard 两处快捷入口、order/detail 返回按钮）。
-- ★幂等：全部 UPDATE/DELETE 按 menu_id 定位，重复执行结果一致。
-- =====================================================================

-- ① 6000 租户管理 → 6001 租户列表 提升到 1300 资金平台管理菜单（继承 order_num=1）
UPDATE sys_menu SET parent_id = 1300, path = 'tenant', order_num = 1 WHERE menu_id = 6001;
DELETE FROM sys_role_menu WHERE menu_id = 6000;
DELETE FROM sys_menu WHERE menu_id = 6000;

-- ② 7020 业务角色管理 → 7021 角色列表 提升到 7010 店铺配置（继承 order_num=2）
UPDATE sys_menu SET parent_id = 7010, path = 'tenantRole', order_num = 2 WHERE menu_id = 7021;
DELETE FROM sys_role_menu WHERE menu_id = 7020;
DELETE FROM sys_menu WHERE menu_id = 7020;

-- ③ 7050 基础数据配置 → 7058 店铺信息设置 提升到 7010（path=shop 唯一，继承 order_num=4）
UPDATE sys_menu SET parent_id = 7010, order_num = 4 WHERE menu_id = 7058;
-- 7050 隐藏：保留其下隐藏子页 7051/7055 的既有路由 /microConfig/basicData/staff|settleRule
UPDATE sys_menu SET visible = '1' WHERE menu_id = 7050;

-- ④ 7110 预约/订单管理 → 7111 预约列表 提升到 7100 小微运营（path=bookingList，继承 order_num=2）
UPDATE sys_menu SET parent_id = 7100, path = 'bookingList', order_num = 2 WHERE menu_id = 7111;
-- 7110 隐藏：保留其下隐藏子页 7116 订单详情 的既有路由 /microOps/booking/detail
UPDATE sys_menu SET visible = '1' WHERE menu_id = 7110;

-- ⑤ 7195 固定支出管理 → 7196 固定支出列表 提升到 7190 数据与报表（path=cost，继承 order_num=5）
UPDATE sys_menu SET parent_id = 7190, path = 'cost', order_num = 5 WHERE menu_id = 7196;
-- F 按钮（新增/修改/删除）随页面改挂到 7196，保证权限跟随菜单
UPDATE sys_menu SET parent_id = 7196 WHERE menu_id IN (7197, 7198, 7199);
DELETE FROM sys_role_menu WHERE menu_id = 7195;
DELETE FROM sys_menu WHERE menu_id = 7195;

-- =====================================================================
-- 验收检查
-- =====================================================================
-- 1) 被提升的 5 个菜单已挂到新父级且路径正确
-- SELECT menu_id, menu_name, parent_id, path, order_num FROM sys_menu
--  WHERE menu_id IN (6001,7021,7058,7111,7196) ORDER BY menu_id;
-- 预期：6001→parent 1300 path=tenant；7021→parent 7010 path=tenantRole；
--       7058→parent 7010 path=shop；7111→parent 7100 path=bookingList；7196→parent 7190 path=cost
--
-- 2) 空目录已删除，含隐藏子页的目录已隐藏
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id IN (6000,7020,7195);              -- 预期：0（已删除）
-- SELECT menu_id, visible FROM sys_menu WHERE menu_id IN (7050,7110);            -- 预期：均 visible=1（隐藏）
--
-- 3) 固定支出 F 按钮已改挂 7196
-- SELECT menu_id, parent_id FROM sys_menu WHERE menu_id IN (7197,7198,7199);     -- 预期：parent_id 均为 7196
--
-- 4) 无「仅一个可见子菜单」的残留 M 目录（本次 5 个已全部处理）
-- SELECT p.menu_id FROM sys_menu p JOIN sys_menu c ON c.parent_id=p.menu_id
--   AND c.menu_type IN ('C','M') AND c.visible='0'
--  WHERE p.menu_type='M' AND p.visible='0' AND p.client_type='pc'
--  GROUP BY p.menu_id HAVING COUNT(c.menu_id)=1;                                 -- 预期：空集
