const mysql = require('mysql2/promise');
(async () => {
  const c = await mysql.createConnection({
    host: 'localhost', port: 3306, user: 'root', password: 'root', database: 'ry-flowable-plus',
    supportBigNumbers: true, bigNumberStrings: true
  });

  // 1. 找到 lijie 的 boss role_id
  const [roles] = await c.query(
    "SELECT CAST(r.role_id AS CHAR) as role_id_str, r.role_key, r.role_name, r.tenant_id " +
    "FROM sys_user u " +
    "JOIN sys_user_role ur ON ur.user_id = u.user_id " +
    "JOIN sys_role r ON r.role_id = ur.role_id " +
    "WHERE u.user_name = 'lijie' AND r.role_key = 'boss'"
  );
  console.log('=== lijie boss role ===');
  console.log(JSON.stringify(roles, null, 2));

  if (roles.length > 0) {
    const roleId = roles[0].role_id_str;
    console.log('role_id = ' + roleId);

    // 2. 检查该角色的 sys_role_menu 中是否有 7156/7157/7158
    const [rm] = await c.query(
      'SELECT CAST(rm.role_id AS CHAR) as role_id, rm.menu_id, m.perms ' +
      'FROM sys_role_menu rm ' +
      'LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id ' +
      'WHERE rm.role_id = ' + roleId + ' AND rm.menu_id IN (7156,7157,7158)'
    );
    console.log('\n=== sys_role_menu for role ' + roleId + ' (7156~7158) ===');
    console.log(JSON.stringify(rm, null, 2));

    // 3. 如果没有，补录
    if (rm.length === 0) {
      console.log('\n>>> No entries found! Inserting sys_role_menu for role ' + roleId);
      await c.query(
        'INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES ' +
        '(' + roleId + ', 7156), ' +
        '(' + roleId + ', 7157), ' +
        '(' + roleId + ', 7158)'
      );
      console.log('INSERT done');

      // 4. 验证
      const [rm2] = await c.query(
        'SELECT CAST(rm.role_id AS CHAR) as role_id, rm.menu_id, m.perms ' +
        'FROM sys_role_menu rm ' +
        'JOIN sys_menu m ON m.menu_id = rm.menu_id ' +
        'WHERE rm.role_id = ' + roleId + ' AND rm.menu_id IN (7156,7157,7158)'
      );
      console.log('\n=== verify after insert ===');
      console.log(JSON.stringify(rm2, null, 2));
    }

    // 5. 检查祖先目录是否在 sys_role_menu 中
    const [ancestors] = await c.query(
      'SELECT CAST(rm.role_id AS CHAR) as role_id, rm.menu_id, m.menu_name ' +
      'FROM sys_role_menu rm ' +
      'JOIN sys_menu m ON m.menu_id = rm.menu_id ' +
      'WHERE rm.role_id = ' + roleId + ' AND rm.menu_id IN (7150, 7100, 7000)'
    );
    console.log('\n=== ancestor menus in sys_role_menu ===');
    console.log(JSON.stringify(ancestors, null, 2));
    
    // 补祖先目录
    const missingAncestors = [7150, 7100, 7000].filter(id => !ancestors.some(a => a.menu_id == id));
    if (missingAncestors.length > 0) {
      console.log('\n>>> Missing ancestors: ' + missingAncestors.join(', '));
      const vals = missingAncestors.map(id => '(' + roleId + ', ' + id + ')').join(', ');
      await c.query('INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES ' + vals);
      console.log('Ancestor insert done');
    }
  }

  await c.end();
  console.log('\nDone! Please logout and login again.');
})();
