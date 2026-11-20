import request from '@/utils/request'

// 平台默认房间模板管理 API（M26 T26.6，platform_admin PC 端）
// ★菜单：7285（默认房间模板 C，parent=7510 原子资源管理）+ 7286（新增/编辑/删除 F）
// ★功能开关：FEAT_ROOM_SEAT_LAYOUT
// ★角色：@SaCheckRole("platform_admin") 类级限制
// ★数据：tenant_id=1 + is_industry_default=1（Service 层强制，跨租户共享）

// 平台默认模板列表
export function listPlatformRoomTemplate(query) {
  return request({
    url: '/system/platformRoomTemplate/list',
    method: 'get',
    params: query
  })
}

// 平台默认模板详情
export function getPlatformRoomTemplate(id) {
  return request({
    url: '/system/platformRoomTemplate/' + id,
    method: 'get'
  })
}

// 新增平台默认模板
export function addPlatformRoomTemplate(data) {
  return request({
    url: '/system/platformRoomTemplate',
    method: 'post',
    data: data
  })
}

// 编辑平台默认模板
export function updatePlatformRoomTemplate(data) {
  return request({
    url: '/system/platformRoomTemplate',
    method: 'put',
    data: data
  })
}

// 删除平台默认模板
export function delPlatformRoomTemplate(id) {
  return request({
    url: '/system/platformRoomTemplate/' + id,
    method: 'delete'
  })
}
