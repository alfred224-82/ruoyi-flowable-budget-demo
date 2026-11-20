import request from '@/utils/request'

// 查询指定业态包 + 语言的文案映射（前端 $bizT 一次性加载）
export function getCopyMap(bizCode, lang) {
  return request({
    url: '/system/bizCopy/map',
    method: 'get',
    params: { bizCode, lang }
  })
}

// 查询指定业态包的全部语言文案
export function getCopyMapAllLangs(bizCode) {
  return request({
    url: '/system/bizCopy/map-all',
    method: 'get',
    params: { bizCode }
  })
}

// 查询支持的语言列表（去重）
export function listLangs() {
  return request({
    url: '/system/bizCopy/langs',
    method: 'get'
  })
}

// 查询文案列表（管理页表格）
export function listCopies(query) {
  return request({
    url: '/system/bizCopy/list',
    method: 'get',
    params: query
  })
}

// 新增或更新文案
export function saveCopy(data) {
  return request({
    url: '/system/bizCopy',
    method: 'post',
    data: data
  })
}

// 批量新增/更新文案
export function batchSaveCopies(list) {
  return request({
    url: '/system/bizCopy/batch',
    method: 'post',
    data: list
  })
}

// 按 ID 删除文案
export function delCopy(id) {
  return request({
    url: '/system/bizCopy/' + id,
    method: 'delete'
  })
}

// 按 (bizCode, lang) 批量删除
export function delCopyBatch(bizCode, lang) {
  return request({
    url: '/system/bizCopy/batch',
    method: 'delete',
    params: { bizCode, lang }
  })
}

// 手动失效缓存
export function evictCopyCache(bizCode, lang) {
  return request({
    url: '/system/bizCopy/cache',
    method: 'delete',
    params: { bizCode, lang }
  })
}
