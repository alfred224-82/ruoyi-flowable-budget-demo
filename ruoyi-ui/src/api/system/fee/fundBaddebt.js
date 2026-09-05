import request from '@/utils/request'

// 查询烂账列表
export function listBaddebt(query) {
  return request({
    url: '/fee/baddebt/list',
    method: 'get',
    params: query
  })
}

// 查询烂账详情
export function getBaddebt(id) {
  return request({
    url: '/fee/baddebt/' + id,
    method: 'get'
  })
}

// 风险标记（带计提金额时同步计提坏账）
export function markBaddebt(data) {
  return request({
    url: '/fee/baddebt',
    method: 'post',
    data: data
  })
}

// 追加坏账计提
export function provisionBaddebt(id, amount) {
  return request({
    url: '/fee/baddebt/provision/' + id,
    method: 'put',
    params: { amount }
  })
}

// 跟进（追加跟进记录）
export function followBaddebt(id, notes) {
  return request({
    url: '/fee/baddebt/follow/' + id,
    method: 'put',
    params: { notes }
  })
}

// 关闭烂账（项目状态置 LOST）
export function closeBaddebt(id) {
  return request({
    url: '/fee/baddebt/close/' + id,
    method: 'put'
  })
}
