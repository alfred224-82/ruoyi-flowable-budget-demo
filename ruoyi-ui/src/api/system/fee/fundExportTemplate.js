import request from '@/utils/request'

// 自定义凭证模板列表（内置+本租户自定义，T6.6）
export function listTemplateConfig(query) {
  return request({
    url: '/fee/exportTemplate/list',
    method: 'get',
    params: query
  })
}

// 模板详情
export function getTemplateConfig(id) {
  return request({
    url: '/fee/exportTemplate/' + id,
    method: 'get'
  })
}

// 新增自定义模板
export function addTemplateConfig(data) {
  return request({
    url: '/fee/exportTemplate',
    method: 'post',
    data: data
  })
}

// 修改自定义模板（内置模板禁止修改）
export function updateTemplateConfig(data) {
  return request({
    url: '/fee/exportTemplate',
    method: 'put',
    data: data
  })
}

// 删除自定义模板（内置模板禁止删除）
export function delTemplateConfig(id) {
  return request({
    url: '/fee/exportTemplate/' + id,
    method: 'delete'
  })
}
