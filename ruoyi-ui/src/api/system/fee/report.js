import request from '@/utils/request'

// 盈利分析明细视图
export function profitAnalysisDetail(query) {
  return request({
    url: '/fee/report/profitAnalysis/detail',
    method: 'get',
    params: query
  })
}

// 盈利分析汇总视图
export function profitAnalysisSummary(query) {
  return request({
    url: '/fee/report/profitAnalysis/summary',
    method: 'get',
    params: query
  })
}

// 异常监控列表
export function anomalyMonitorList(query) {
  return request({
    url: '/fee/report/anomalyMonitor',
    method: 'get',
    params: query
  })
}
