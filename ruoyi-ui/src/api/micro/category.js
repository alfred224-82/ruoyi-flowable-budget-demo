import request from '@/utils/request'

// 服务分类列表
export function listCategory(query) {
  return request({
    url: '/micro/category/list',
    method: 'get',
    params: query
  })
}

// 启用分类下拉（服务项目表单选择用）
export function enabledCategory() {
  return request({
    url: '/micro/category/enabled',
    method: 'get'
  })
}

// 服务分类详情
export function getCategory(id) {
  return request({
    url: '/micro/category/' + id,
    method: 'get'
  })
}

// 新建服务分类
export function addCategory(data) {
  return request({
    url: '/micro/category',
    method: 'post',
    data: data
  })
}

// 编辑服务分类
export function updateCategory(data) {
  return request({
    url: '/micro/category',
    method: 'put',
    data: data
  })
}

// 停用/启用（status：0正常 1停用）
export function toggleCategory(id, status) {
  return request({
    url: '/micro/category/toggle/' + id + '/' + status,
    method: 'put'
  })
}

// 删除服务分类
export function delCategory(ids) {
  return request({
    url: '/micro/category/' + ids,
    method: 'delete'
  })
}
