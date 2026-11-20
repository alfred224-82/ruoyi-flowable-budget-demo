import request from '@/utils/request'

// 推广分销管理（T15.34/M16，老板 PC 端 + 小程序端）

// ===== 推广码 =====

// 生成推广码（codeType: store/personal）
export function generateCode(codeType) {
  return request({
    url: '/micro/promo/code/generate',
    method: 'post',
    params: { codeType: codeType }
  })
}

// 推广码列表（老板 PC）
export function listPromoCode(query) {
  return request({
    url: '/micro/promo/code/list',
    method: 'get',
    params: query
  })
}

// 启用/禁用推广码
export function togglePromoCode(id, status) {
  return request({
    url: '/micro/promo/code/toggle/' + id + '/' + status,
    method: 'put'
  })
}

// 我的推广码（小程序端）
export function myPromotion() {
  return request({
    url: '/micro/promo/code/my',
    method: 'get'
  })
}

// ===== 扫码归因 =====

// 扫码归因（小程序端扫码调用）
export function scanAttribution(sceneParam, customerUserId, customerPhone) {
  return request({
    url: '/micro/promo/attribution/scan',
    method: 'post',
    params: { sceneParam, customerUserId, customerPhone }
  })
}

// 消费归因列表（老板 PC）
export function listAttribution(query) {
  return request({
    url: '/micro/promo/attribution/list',
    method: 'get',
    params: query
  })
}

// ===== 奖励 =====

// 奖励配置列表
export function listRewardConfig(query) {
  return request({
    url: '/micro/promo/reward/config/list',
    method: 'get',
    params: query
  })
}

// 更新奖励配置
export function updateRewardConfig(data) {
  return request({
    url: '/micro/promo/reward/config',
    method: 'put',
    data: data
  })
}

// 拉新奖励结算
export function settleReward(customerUserId, firstOrderId) {
  return request({
    url: '/micro/promo/reward/settle',
    method: 'post',
    params: { customerUserId, firstOrderId }
  })
}

// 奖励发放记录列表
export function listRewardRecords(query) {
  return request({
    url: '/micro/promo/reward/records',
    method: 'get',
    params: query
  })
}

// 标记现金奖励已线下发放
export function markPaidOffline(id) {
  return request({
    url: '/micro/promo/reward/markPaid/' + id,
    method: 'put'
  })
}

// ===== 推广统计 =====

// 推广数据统计
export function getPromotionStat(period) {
  return request({
    url: '/micro/distribution/stat/promotion',
    method: 'get',
    params: { period }
  })
}

// 推广业绩统计
export function getPerformanceStat(period) {
  return request({
    url: '/micro/distribution/stat/performance',
    method: 'get',
    params: { period }
  })
}

// ===== 盈亏三指标 =====

// 首页盈亏三指标聚合（T15.37）
export function getProfitSummary() {
  return request({
    url: '/micro/report/profitSummary',
    method: 'get'
  })
}
