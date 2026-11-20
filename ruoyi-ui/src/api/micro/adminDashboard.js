import request from '@/utils/request'

// ===== T18.5 Admin 平台驾驶舱首页 =====

export function getAdminDashboard() {
  return request({ url: '/system/dashboard', method: 'get' })
}
