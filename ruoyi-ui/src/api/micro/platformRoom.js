import request from '@/utils/request'

// platform_admin 房间/空间管理（T15.38，V3.7 原子资源管理·房间空间）
// ★房间/空间 = 空间分类（service_category.category_type='space'）下的 service_item（元/小时按时长计价）
// ★数据源：首次进入本租户项目为空时，后端按 sys_tenant.industry 从 admin 行业模板 preset_config.rooms 自愈初始化

// 房间/空间列表（仅空间分类项）
export function listPlatformRoom(query) {
  return request({
    url: '/micro/platform/room/list',
    method: 'get',
    params: query
  })
}

// 空间分类下拉（category_type=space）
export function listPlatformRoomCategories() {
  return request({
    url: '/micro/platform/room/categories',
    method: 'get'
  })
}

// 房间详情
export function getPlatformRoom(id) {
  return request({
    url: '/micro/platform/room/' + id,
    method: 'get'
  })
}

// 新建房间/空间（未指定分类时后端自动挂到本租户空间分类）
export function addPlatformRoom(data) {
  return request({
    url: '/micro/platform/room',
    method: 'post',
    data: data
  })
}

// 编辑房间/空间（价格变更自动写 price_change_log）
export function updatePlatformRoom(data) {
  return request({
    url: '/micro/platform/room',
    method: 'put',
    data: data
  })
}

// 启用/停用（status：0启用 1停用）
export function togglePlatformRoom(id, status) {
  return request({
    url: '/micro/platform/room/toggle/' + id + '/' + status,
    method: 'put'
  })
}

// 删除房间/空间
export function delPlatformRoom(ids) {
  return request({
    url: '/micro/platform/room/' + ids,
    method: 'delete'
  })
}
