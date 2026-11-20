import request from '@/utils/request'

// ★M38/M39 经营报表 API（PC 端 Boss）

// 日报（当日汇总 + 当日活动清单）
export function dailyReport(date) {
  return request({
    url: '/micro/operating/daily',
    method: 'get',
    params: date ? { date } : {}
  })
}

// 月度经营报表（月汇总 + 固定成本构成 + 税费/净利 + 保本线 + 对比上月）
export function monthlyReport(month) {
  return request({
    url: '/micro/operating/monthly',
    method: 'get',
    params: month ? { month } : {}
  })
}

// 活动明细表（一行一场 + 底部合计行 + 未结算提醒）
export function activityDetail(params) {
  return request({
    url: '/micro/operating/activityDetail',
    method: 'get',
    params: params || {}
  })
}

// 三张报表对账校验
export function reconcile(month) {
  return request({
    url: '/micro/operating/reconcile',
    method: 'get',
    params: month ? { month } : {}
  })
}

// 月度税费/月结状态
export function getClosing(month) {
  return request({
    url: '/micro/operating/closing',
    method: 'get',
    params: month ? { month } : {}
  })
}

// 保存月度税费
export function saveTax(data) {
  return request({
    url: '/micro/operating/tax',
    method: 'post',
    data: data
  })
}

// 月结锁定
export function lockMonth(month) {
  return request({
    url: '/micro/operating/lock/' + month,
    method: 'post'
  })
}

// 导出活动明细 CSV
export function exportActivityDetailCsv(params) {
  return request({
    url: '/micro/operating/export/activityDetail',
    method: 'get',
    params: params || {},
    responseType: 'blob'
  })
}

// 导出月度经营报表 CSV
export function exportMonthlyCsv(month) {
  return request({
    url: '/micro/operating/export/monthly',
    method: 'get',
    params: month ? { month } : {},
    responseType: 'blob'
  })
}
