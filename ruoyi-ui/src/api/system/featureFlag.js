import request from '@/utils/request'

// 功能开关管理（T10.3 表驱动开关）
// 后端 FeatureFlagController: /system/featureFlag

// 开关列表（33+ 项，按分组/编码排序）
export function listFeatureFlag() {
  return request({
    url: '/system/featureFlag/list',
    method: 'get'
  })
}

// 查询指定版本的默认开启开关集
export function getEditionFeatures(edition) {
  return request({
    url: '/system/featureFlag/edition/' + edition,
    method: 'get'
  })
}

// 启停开关（仅改 status，不改版本归属）
export function toggleFeatureFlagStatus(id, status) {
  return request({
    url: '/system/featureFlag/status/' + id,
    method: 'put',
    params: { status: status }
  })
}

// 手动失效开关缓存
export function evictFeatureFlagCache() {
  return request({
    url: '/system/featureFlag/cache',
    method: 'delete'
  })
}
