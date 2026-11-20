const mysql = require('mysql2/promise');
(async () => {
  const c = await mysql.createConnection({
    host: 'localhost', port: 3306, user: 'root', password: 'root', database: 'ry-flowable-plus'
  });
  const [cols] = await c.query('SHOW COLUMNS FROM sys_menu');
  console.log(JSON.stringify(cols.map(c => c.Field)));
  await c.end();
})();
