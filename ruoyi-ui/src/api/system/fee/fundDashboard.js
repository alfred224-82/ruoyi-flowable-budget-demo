import request from '@/utils/request'

// 看板总数据（总览/大类/风险/固定成本/未归类池）
export function getDashboard() {
  return request({
    url: '/fee/dashboard',
    method: 'get'
  })
}

// 项目卡片列表（含利润快照）
export function listProjectCards() {
  return request({
    url: '/fee/dashboard/projects',
    method: 'get'
  })
}

// 四层下钻：公司→大类→项目→明细
export function drillDown(level, category, projectId) {
  return request({
    url: '/fee/dashboard/drillDown',
    method: 'get',
    params: { level, category, projectId }
  })
}
