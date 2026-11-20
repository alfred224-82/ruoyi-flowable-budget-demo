import request from '@/utils/request'

// 员工分成规则（M35 T35.2，单场活动结算分账口径，仅 PC）
// ★与 M15 REQ-03「员工提成」（staffCommission.js，月度薪酬口径）独立

// 分成规则列表（分页）
export function listCommissionRule(query) {
  return request({
    url: '/micro/commissionRule/list',
    method: 'get',
    params: query
  })
}

// 分成规则详情
export function getCommissionRule(id) {
  return request({
    url: '/micro/commissionRule/' + id,
    method: 'get'
  })
}

// 新增分成规则
export function addCommissionRule(data) {
  return request({
    url: '/micro/commissionRule',
    method: 'post',
    data: data
  })
}

// 修改分成规则
export function updateCommissionRule(data) {
  return request({
    url: '/micro/commissionRule',
    method: 'put',
    data: data
  })
}

// 删除分成规则
export function delCommissionRule(ids) {
  return request({
    url: '/micro/commissionRule/' + ids,
    method: 'delete'
  })
}

// 员工下拉选项（本租户 salesman/助教）
export function listRuleStaffOptions() {
  return request({
    url: '/micro/commissionRule/staffOptions',
    method: 'get'
  })
}

// T35.4 定价试算（按已选员工 + 售价 + 预计人数 + 其他成本实时计算）
export function calcPricing(params) {
  return request({
    url: '/micro/commissionRule/calcPricing',
    method: 'get',
    params: params
  })
}

// T35.3 查询某活动的员工分成快照列表
export function listSnapshots(activityId) {
  return request({
    url: '/micro/commissionRule/snapshot/' + activityId,
    method: 'get'
  })
}

// T35.3 汇总某活动的员工成本
export function sumEmployeeCost(activityId) {
  return request({
    url: '/micro/commissionRule/snapshot/' + activityId + '/sumEmployeeCost',
    method: 'get'
  })
}

// T35.7 预估 vs 实际报表
export function queryEstimateVsActual(params) {
  return request({
    url: '/micro/commissionRule/estimateVsActual',
    method: 'get',
    params: params
  })
}
