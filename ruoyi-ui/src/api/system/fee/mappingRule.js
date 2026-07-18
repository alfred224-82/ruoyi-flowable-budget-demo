import request from '@/utils/request'

// 查询映射规则列表
export function listMappingRule(query) {
  return request({
    url: '/fee/mappingRule/list',
    method: 'get',
    params: query
  })
}

// 查询映射规则详细
export function getMappingRule(id) {
  return request({
    url: '/fee/mappingRule/' + id,
    method: 'get'
  })
}

// 新增映射规则
export function addMappingRule(data) {
  return request({
    url: '/fee/mappingRule',
    method: 'post',
    data: data
  })
}

// 修改映射规则
export function updateMappingRule(data) {
  return request({
    url: '/fee/mappingRule',
    method: 'put',
    data: data
  })
}

// 删除映射规则
export function delMappingRule(id) {
  return request({
    url: '/fee/mappingRule/' + id,
    method: 'delete'
  })
}

// 预览预算归属
export function previewMapping(data) {
  return request({
    url: '/fee/mappingRule/preview',
    method: 'post',
    data: data
  })
}
