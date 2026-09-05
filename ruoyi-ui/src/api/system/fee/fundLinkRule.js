import request from '@/utils/request'

// 规则列表
export function listLinkRule() {
  return request({
    url: '/fee/linkRule/list',
    method: 'get'
  })
}

// 新增/修改规则（财务维护）
export function saveLinkRule(data) {
  return request({
    url: '/fee/linkRule',
    method: 'post',
    data: data
  })
}

// 删除规则
export function delLinkRule(id) {
  return request({
    url: '/fee/linkRule/' + id,
    method: 'delete'
  })
}

// 匹配当前用户的关联规则（报销单表单调用，无匹配返回空）
export function matchLinkRule(categoryId) {
  return request({
    url: '/fee/linkRule/match',
    method: 'get',
    params: { categoryId }
  })
}
