// 只读诊断：自助预约订单 remark 中助教ID 与 sys_user / staff_pricing 的对应关系
const mysql = require('mysql2/promise');
(async () => {
  const c = await mysql.createConnection({
    host: 'localhost', port: 3306, user: 'root', password: 'root', database: 'ry-flowable-plus'
  });
  const [orders] = await c.query(
    `SELECT id, order_no, tenant_id, customer_user_id, staff_user_id, service_name, amount, order_status, remark
     FROM booking_order WHERE remark LIKE '%costDetails%' ORDER BY id DESC LIMIT 5`);
  for (const o of orders) {
    console.log('=== order id=' + o.id, o.order_no, 'tenant=' + o.tenant_id, 'staffUserId=' + o.staff_user_id, 'status=' + o.order_status);
    let rm = {};
    try { rm = JSON.parse(o.remark); } catch (e) { console.log('remark parse fail'); continue; }
    console.log('  bookingType=' + rm.bookingType, 'mainResourceId=' + rm.mainResourceId,
      'optionalStaffIds=' + JSON.stringify(rm.optionalStaffIds), 'optionalRoomIds=' + JSON.stringify(rm.optionalRoomIds));
    console.log('  costDetails=' + JSON.stringify(rm.costDetails));
    const staffIds = (rm.optionalStaffIds || []).concat(rm.bookingType === 'staff' && rm.mainResourceId ? [rm.mainResourceId] : []);
    if (staffIds.length) {
      const [users] = await c.query(
        `SELECT user_id, user_name, nick_name, del_flag, status, tenant_id FROM sys_user WHERE user_id IN (?)`, [staffIds]);
      console.log('  sys_user rows=' + JSON.stringify(users));
      const [prices] = await c.query(
        `SELECT id, tenant_id, user_id, hourly_rate, enabled, del_flag FROM staff_pricing WHERE user_id IN (?)`, [staffIds]);
      console.log('  staff_pricing rows=' + JSON.stringify(prices));
    }
  }
  // 对照：当前全部 salesman 助教及其价格
  const [staff] = await c.query(
    `SELECT u.user_id, u.nick_name, u.del_flag, u.status, u.tenant_id AS user_tenant,
            p.hourly_rate, p.enabled AS price_enabled, p.del_flag AS price_del, p.tenant_id AS price_tenant
     FROM sys_user_role ur
     JOIN sys_role r ON r.role_id = ur.role_id AND r.role_key = 'salesman'
     JOIN sys_user u ON u.user_id = ur.user_id
     LEFT JOIN staff_pricing p ON p.user_id = u.user_id AND p.del_flag = '0'
     ORDER BY u.user_id`);
  console.log('=== all salesman staff ===');
  staff.forEach(s => console.log(' ', JSON.stringify(s)));
  await c.end();
})();
