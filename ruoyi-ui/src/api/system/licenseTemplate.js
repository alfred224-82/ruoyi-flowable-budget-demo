import request from '@/utils/request'

// ===== T18.14 License模板管理 =====

export function listLicenseTemplate(params) {
  return request({ url: '/system/licenseTemplate/list', method: 'get', params })
}

export function getLicenseTemplate(id) {
  return request({ url: '/system/licenseTemplate/' + id, method: 'get' })
}

export function addLicenseTemplate(data) {
  return request({ url: '/system/licenseTemplate', method: 'post', data })
}

export function updateLicenseTemplate(data) {
  return request({ url: '/system/licenseTemplate', method: 'put', data })
}

export function deleteLicenseTemplate(ids) {
  return request({ url: '/system/licenseTemplate/' + ids, method: 'delete' })
}

export function changeLicenseTemplateStatus(id, status) {
  return request({ url: '/system/licenseTemplate/changeStatus', method: 'put', params: { id, status } })
}
