import request from '@/utils/request'

// 资产列表
export function listAsset(query) {
  return request({
    url: '/fee/assetDepr/list',
    method: 'get',
    params: query
  })
}

// 新增/修改资产（月折旧额自动计算）
export function saveAsset(data) {
  return request({
    url: '/fee/assetDepr',
    method: 'post',
    data: data
  })
}

// 资产报废（停止折旧）
export function scrapAsset(id) {
  return request({
    url: '/fee/assetDepr/scrap/' + id,
    method: 'put'
  })
}

// 折旧规则列表
export function listDepreciationRule() {
  return request({
    url: '/fee/assetDepr/rule/list',
    method: 'get'
  })
}

// 保存折旧规则（资产分类唯一）
export function saveDepreciationRule(data) {
  return request({
    url: '/fee/assetDepr/rule',
    method: 'post',
    data: data
  })
}

// 折旧记录列表
export function listDepreciation(query) {
  return request({
    url: '/fee/assetDepr/depreciation/list',
    method: 'get',
    params: query
  })
}

// 手动触发月度折旧计提（幂等）
export function accrueDepreciation(accrualMonth) {
  return request({
    url: '/fee/assetDepr/accrue',
    method: 'put',
    params: { accrualMonth }
  })
}
