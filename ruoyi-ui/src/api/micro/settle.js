import request from '@/utils/request'

// 结算单列表
export function listSettle(query) {
  return request({
    url: '/micro/settle/list',
    method: 'get',
    params: query
  })
}

// 结算历史（含「已发/待发」状态）
export function historySettle(query) {
  return request({
    url: '/micro/settle/history',
    method: 'get',
    params: query
  })
}

// 结算单详情
export function getSettle(id) {
  return request({
    url: '/micro/settle/' + id,
    method: 'get'
  })
}

// 预览：选择员工+周期→自动汇总单量+算应发金额（不落库）
export function previewSettle(staffUserId, periodStart, periodEnd) {
  return request({
    url: '/micro/settle/preview',
    method: 'get',
    params: { staffUserId, periodStart, periodEnd }
  })
}

// 生成「待发放」结算单
export function generateSettle(data) {
  return request({
    url: '/micro/settle',
    method: 'post',
    data: data
  })
}

// 标记已线下发放（线下转账后点此，改状态+记 cash_flow 支出流水，不调支付接口）
export function markPaid(id) {
  return request({
    url: '/micro/settle/markPaid/' + id,
    method: 'put'
  })
}

// 删除结算单（仅允许删除「待发放」状态）
export function delSettle(ids) {
  return request({
    url: '/micro/settle/' + ids,
    method: 'delete'
  })
}

// 查询结算单关联的订单明细（含顾客、活动、时长、起止时间）
export function listSettleOrders(id) {
  return request({
    url: '/micro/settle/' + id + '/orders',
    method: 'get'
  })
}
