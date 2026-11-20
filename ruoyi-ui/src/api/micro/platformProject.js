import request from '@/utils/request'

// platform_admin 项目管理（T15.38，V3.7 原子资源管理·项目）
// ★与 /micro/service（老板·服务与定价）共用底层 ServiceItemService，仅 URL 路径不同（platform_admin 菜单独立入口）
// ★数据源：首次进入本租户项目为空时，后端按 sys_tenant.industry 从 admin 行业模板 preset_config.projects 自愈初始化

// 项目列表（排除房间/空间项）
export function listPlatformProject(query) {
  return request({
    url: '/micro/platform/project/list',
    method: 'get',
    params: query
  })
}

// 服务分类下拉（category_type=service）
export function listPlatformProjectCategories() {
  return request({
    url: '/micro/platform/project/categories',
    method: 'get'
  })
}

// 项目详情
export function getPlatformProject(id) {
  return request({
    url: '/micro/platform/project/' + id,
    method: 'get'
  })
}

// 新建项目
export function addPlatformProject(data) {
  return request({
    url: '/micro/platform/project',
    method: 'post',
    data: data
  })
}

// 编辑项目（价格变更自动写 price_change_log）
export function updatePlatformProject(data) {
  return request({
    url: '/micro/platform/project',
    method: 'put',
    data: data
  })
}

// 上架/下架（status：0上架 1下架）
export function togglePlatformProject(id, status) {
  return request({
    url: '/micro/platform/project/toggle/' + id + '/' + status,
    method: 'put'
  })
}

// 删除项目
export function delPlatformProject(ids) {
  return request({
    url: '/micro/platform/project/' + ids,
    method: 'delete'
  })
}
