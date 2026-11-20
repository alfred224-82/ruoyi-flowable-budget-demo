import request from '@/utils/request'

// ===== T18.11 租户管理员系统设置 =====

export function getSystemSetting() {
  return request({ url: '/micro/systemSetting', method: 'get' })
}

export function saveCompanySetting(data) {
  return request({ url: '/micro/systemSetting/company', method: 'put', data })
}
