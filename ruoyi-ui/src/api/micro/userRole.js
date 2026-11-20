import request from '@/utils/request'

// 本租户用户角色分配列表（回填用户账号/昵称/手机号 + 角色显示名）
// 预约/结算表单的「员工下拉」数据源：传 roleCode='salesman' 过滤业务员
export function listUserRole(query) {
  return request({
    url: '/micro/userRole/list',
    method: 'get',
    params: query
  })
}

// ★员工下拉数据源（预约/结算页面用）：perms 用 micro:booking:list / micro:settle:list OR 放行
// 避免将 micro:userRole:list（含完整角色分配管理菜单）授予 boss 导致侧边栏污染
export function listStaff(query) {
  return request({
    url: '/micro/userRole/staff',
    method: 'get',
    params: query
  })
}

// 查询指定用户在本租户下的全部角色
export function listRoleByUser(userId) {
  return request({
    url: '/micro/userRole/user/' + userId,
    method: 'get'
  })
}

// ④ 分配用户角色（单角色 roleCode 或批量 roleCodes，后端幂等 upsert；分配前校验角色已在本租户挑选且启用）
export function assignUserRole(data) {
  return request({
    url: '/micro/userRole/assign',
    method: 'post',
    data: data
  })
}

// 设置主角色（同一用户同一租户下主角色唯一，perms micro:userRole:assign）
export function setPrimaryRole(id) {
  return request({
    url: '/micro/userRole/primary/' + id,
    method: 'put'
  })
}

// 取消分配（按关联ID逻辑删除，perms micro:userRole:remove）
export function delUserRole(id) {
  return request({
    url: '/micro/userRole/' + id,
    method: 'delete'
  })
}
