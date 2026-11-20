import request from '@/utils/request'

// 预约订单列表
export function listBooking(query) {
  return request({
    url: '/micro/booking/list',
    method: 'get',
    params: query
  })
}

// 预约订单详情
export function getBooking(id) {
  return request({
    url: '/micro/booking/' + id,
    method: 'get'
  })
}

// 新建预约（帮顾客约）
export function addBooking(data) {
  return request({
    url: '/micro/booking',
    method: 'post',
    data: data
  })
}

// 编辑预约
export function updateBooking(data) {
  return request({
    url: '/micro/booking',
    method: 'put',
    data: data
  })
}

// 取消预约
export function cancelBooking(id) {
  return request({
    url: '/micro/booking/cancel/' + id,
    method: 'put'
  })
}

// 完成服务
export function completeBooking(id) {
  return request({
    url: '/micro/booking/complete/' + id,
    method: 'put'
  })
}

// 标记已收款（线下收款后点此，自动写 cash_flow 收入流水，不调支付接口）
export function receiveBooking(id) {
  return request({
    url: '/micro/booking/receive/' + id,
    method: 'put'
  })
}
