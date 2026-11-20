import request from '@/utils/request'

// 小微版套餐管理（T15.11/12）

// 小微版套餐列表（按版本分组）
export function listMicroPackage(versionType) {
  return request({
    url: '/micro/package/list',
    method: 'get',
    params: { versionType }
  })
}

// 小微版套餐概览
export function getMicroPackageOverview() {
  return request({
    url: '/micro/package/overview',
    method: 'get'
  })
}
