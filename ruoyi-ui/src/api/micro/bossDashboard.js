import request from '@/utils/request'

// ===== T18.7 老板经营驾驶舱首页 =====

export function getBossDashboard() {
  return request({ url: '/micro/bossDashboard', method: 'get' })
}
