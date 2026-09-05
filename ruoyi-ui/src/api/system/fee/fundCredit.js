import request from '@/utils/request'

// 员工信用分分页查询（T6.3）
export function listCredit(query) {
  return request({
    url: '/fee/credit/list',
    method: 'get',
    params: query
  })
}

// 查询指定员工信用（不存在自动初始化）
export function getCreditByUser(userId) {
  return request({
    url: '/fee/credit/user/' + userId,
    method: 'get'
  })
}

// 我的信用分
export function getMyCredit() {
  return request({
    url: '/fee/credit/my',
    method: 'get'
  })
}

// 财务手工调整信用分
export function adjustCredit(userId, delta, reason) {
  return request({
    url: '/fee/credit/adjust',
    method: 'post',
    params: { userId, delta, reason }
  })
}

// 信用规则配置列表
export function listCreditRule() {
  return request({
    url: '/fee/credit/rule/list',
    method: 'get'
  })
}

// 新增信用规则
export function addCreditRule(data) {
  return request({
    url: '/fee/credit/rule',
    method: 'post',
    data: data
  })
}

// 修改信用规则
export function updateCreditRule(data) {
  return request({
    url: '/fee/credit/rule',
    method: 'put',
    data: data
  })
}

// 删除信用规则
export function delCreditRule(id) {
  return request({
    url: '/fee/credit/rule/' + id,
    method: 'delete'
  })
}
