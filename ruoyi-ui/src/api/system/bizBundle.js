import request from '@/utils/request'

// 业态包管理（T10.2 多业态平台底座）
// 后端 BizBundleController: /system/bizBundle

// 业态包列表
export function listBizBundle() {
  return request({
    url: '/system/bizBundle/list',
    method: 'get'
  })
}

// 业态包详情（含解析后的开关集与文案映射）
export function getBizBundle(bizCode) {
  return request({
    url: '/system/bizBundle/' + bizCode,
    method: 'get'
  })
}

// 新增/修改业态包
export function saveBizBundle(data) {
  return request({
    url: '/system/bizBundle',
    method: 'post',
    data: data
  })
}

// 手动失效业态包缓存
export function evictBizBundleCache(bizCode) {
  return request({
    url: '/system/bizBundle/cache',
    method: 'delete',
    params: { bizCode: bizCode }
  })
}
