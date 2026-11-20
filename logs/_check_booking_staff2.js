// 只读诊断2：以字符串形式取出精确雪花ID，确认精度丢失
const mysql = require('mysql2/promise');
(async () => {
  const c = await mysql.createConnection({
    host: 'localhost', port: 3306, user: 'root', password: 'root', database: 'ry-flowable-plus'
  });
  const [r] = await c.query("SELECT CAST(user_id AS CHAR) uid, nick_name FROM sys_user WHERE nick_name IN ('芒果','橘子')");
  console.log('sys_user exact ids:', JSON.stringify(r));
  const [o] = await c.query("SELECT CAST(id AS CHAR) oid, CAST(staff_user_id AS CHAR) sid, order_status, JSON_EXTRACT(remark,'$.optionalStaffIds') staffIds FROM booking_order ORDER BY id DESC LIMIT 3");
  console.log('booking_order:', JSON.stringify(o));
  await c.end();
})();
