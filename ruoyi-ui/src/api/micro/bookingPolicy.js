import request from '@/utils/request'

// ===== T18.10 预约策略配置 =====

export function getBookingPolicy() {
  return request({ url: '/micro/bookingPolicy', method: 'get' })
}

export function saveBookingPolicy(data) {
  return request({ url: '/micro/bookingPolicy', method: 'put', data })
}
