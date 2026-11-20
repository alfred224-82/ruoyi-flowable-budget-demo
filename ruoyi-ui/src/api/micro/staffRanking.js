import request from '@/utils/request'

// ===== T18.13 老板员工业绩榜 =====

export function getStaffRanking(params) {
  return request({ url: '/micro/staffRanking', method: 'get', params })
}
