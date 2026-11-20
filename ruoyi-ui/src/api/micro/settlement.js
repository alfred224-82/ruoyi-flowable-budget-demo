import request from '@/utils/request'

// ★M36 活动结算单 API（PC 端 Boss）

// 按活动ID查询结算单（不存在则后端自动初始化草稿）
export function getSettlementByActivity(activityId) {
  return request({
    url: '/micro/settlement/activity/' + activityId,
    method: 'get'
  })
}

// 按结算单ID查询
export function getSettlement(id) {
  return request({
    url: '/micro/settlement/' + id,
    method: 'get'
  })
}

// 保存草稿（仅 DRAFT 状态可编辑）
export function saveDraft(data) {
  return request({
    url: '/micro/settlement/saveDraft',
    method: 'post',
    data: data
  })
}

// 提交结算（DRAFT→SUBMITTED）
export function submitSettlement(id) {
  return request({
    url: '/micro/settlement/submit/' + id,
    method: 'post'
  })
}

// 锁定结算（SUBMITTED→LOCKED）
export function lockSettlement(id) {
  return request({
    url: '/micro/settlement/lock/' + id,
    method: 'post'
  })
}

// 追加调整单（锁定后发现错误 → 新建调整单）
export function createAdjustment(originId, data) {
  return request({
    url: '/micro/settlement/adjustment/' + originId,
    method: 'post',
    data: data || {}
  })
}

// 导出结算单 CSV（返回 blob 下载）
export function exportSettlement(id) {
  return request({
    url: '/micro/settlement/export/' + id,
    method: 'get',
    responseType: 'blob'
  })
}

// 收工时追加消费提醒判断
export function needAddonReminder(settlementId) {
  return request({
    url: '/micro/settlement/needAddonReminder/' + settlementId,
    method: 'get'
  })
}
