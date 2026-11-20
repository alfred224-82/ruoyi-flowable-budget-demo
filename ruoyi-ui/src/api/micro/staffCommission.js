import request from '@/utils/request'

// 员工提成比例配置（REQ-03，仅 PC）

// 提成配置列表（分页，含实时回填当月基数/提成/人力成本）
export function listStaffCommission(query) {
  return request({
    url: '/micro/staffCommission/list',
    method: 'get',
    params: query
  })
}

// 提成配置详情
export function getStaffCommission(id) {
  return request({
    url: '/micro/staffCommission/' + id,
    method: 'get'
  })
}

// 新增提成配置
export function addStaffCommission(data) {
  return request({
    url: '/micro/staffCommission',
    method: 'post',
    data: data
  })
}

// 修改提成配置
export function updateStaffCommission(data) {
  return request({
    url: '/micro/staffCommission',
    method: 'put',
    data: data
  })
}

// 删除提成配置
export function delStaffCommission(ids) {
  return request({
    url: '/micro/staffCommission/' + ids,
    method: 'delete'
  })
}

// 员工下拉选项（本租户 salesman/助教）
export function listStaffOptions() {
  return request({
    url: '/micro/staffCommission/staffOptions',
    method: 'get'
  })
}

// 试算某员工某月提成（弹窗预览用；month 格式 yyyy-MM，缺省当月）
export function calcCommission(staffUserId, month) {
  return request({
    url: '/micro/staffCommission/calc',
    method: 'get',
    params: { staffUserId, month }
  })
}
