import request from '@/utils/request'

// 今日概览（实时营收/净利，从 cash_flow 当日聚合）
export function todayReport() {
  return request({
    url: '/micro/report/today',
    method: 'get'
  })
}

// 经营周报（近 7 天聚合）
export function weeklyReport() {
  return request({
    url: '/micro/report/weekly',
    method: 'get'
  })
}

// 经营月报（当月聚合）
export function monthlyReport() {
  return request({
    url: '/micro/report/monthly',
    method: 'get'
  })
}

// 利润报表（指定日期范围：收入-支出）data: { beginDate, endDate }
export function profitReport(data) {
  return request({
    url: '/micro/report/profit',
    method: 'post',
    data: data
  })
}

// 资金水位（累计结余 = 全周期 Σincome-Σexpense，附当日数据）
export function cashPosition() {
  return request({
    url: '/micro/report/cashFlow',
    method: 'get'
  })
}

// 营收趋势（按日快照，取自 micro_daily_revenue）data: { beginDate, endDate }
export function trendReport(data) {
  return request({
    url: '/micro/report/trend',
    method: 'post',
    data: data
  })
}
