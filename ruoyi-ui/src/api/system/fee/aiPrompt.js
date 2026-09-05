import request from '@/utils/request'

// 按场景+行业获取话术模板（前端AI引导调用）
export function getPrompt(sceneCode, industryCode) {
  return request({
    url: '/fee/aiPrompt/get',
    method: 'get',
    params: { sceneCode, industryCode }
  })
}

// 查询话术配置列表
export function listAiPromptConfig(query) {
  return request({
    url: '/fee/aiPrompt/list',
    method: 'get',
    params: query
  })
}

// 查询全部启用的话术配置
export function listEnabledAiPrompt() {
  return request({
    url: '/fee/aiPrompt/listEnabled',
    method: 'get'
  })
}

// 查询话术配置详细
export function getAiPromptConfig(id) {
  return request({
    url: '/fee/aiPrompt/' + id,
    method: 'get'
  })
}

// 新增话术配置
export function addAiPromptConfig(data) {
  return request({
    url: '/fee/aiPrompt',
    method: 'post',
    data: data
  })
}

// 修改话术配置
export function updateAiPromptConfig(data) {
  return request({
    url: '/fee/aiPrompt',
    method: 'put',
    data: data
  })
}

// 删除话术配置
export function delAiPromptConfig(ids) {
  return request({
    url: '/fee/aiPrompt/' + ids,
    method: 'delete'
  })
}
