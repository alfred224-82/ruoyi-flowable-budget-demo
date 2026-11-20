import request from '@/utils/request'

// platform_admin 人员管理（T15.33，租户管理员三件套之三）
// ★与 /micro/userRole 共用底层 Service，仅 URL 路径不同（platform_admin 菜单独立入口）

// 本租户人员列表（含角色分配信息）
export function listPlatformUser(query) {
  return request({
    url: '/micro/platform/userManage/list',
    method: 'get',
    params: query
  })
}

// 新增人员 + 分配角色
export function addPlatformUser(data) {
  return request({
    url: '/micro/platform/userManage/add',
    method: 'post',
    data: data
  })
}

// 编辑人员角色
export function editPlatformUser(data) {
  return request({
    url: '/micro/platform/userManage/edit',
    method: 'put',
    data: data
  })
}

// 移除人员（取消角色分配）
export function delPlatformUser(id) {
  return request({
    url: '/micro/platform/userManage/remove/' + id,
    method: 'delete'
  })
}
