import request from '@/utils/request'

// ===== T18.14 License使用报表 =====

export function getLicenseReport() {
  return request({ url: '/system/licenseReport', method: 'get' })
}
