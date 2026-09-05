import request from '@/utils/request'

// 获取当前授权信息（前端根据此接口动态渲染菜单/按钮）
export function getLicenseInfo() {
  return request({
    url: '/system/license/info',
    method: 'get'
  })
}

// 导出机器指纹
export function getFingerprint() {
  return request({
    url: '/system/license/fingerprint',
    method: 'get'
  })
}

// 替换License / 切换版本
export function replaceLicense(data) {
  return request({
    url: '/system/license/replace',
    method: 'post',
    data: data
  })
}

// 获取试用剩余时长
export function getTrialStatus() {
  return request({
    url: '/system/license/trial-status',
    method: 'get'
  })
}

// 获取当前版本等级
export function getEdition() {
  return request({
    url: '/system/license/edition',
    method: 'get'
  })
}
