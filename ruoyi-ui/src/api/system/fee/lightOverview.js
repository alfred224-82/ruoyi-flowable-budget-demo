import request from '@/utils/request'

// 轻量收支概览（基础版首页标配）：本月收入/支出/结余
export function getLightOverview() {
  return request({
    url: '/fee/overview/light',
    method: 'get'
  })
}
