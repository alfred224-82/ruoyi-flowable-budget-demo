import request from '@/utils/request'

// 打卡记录分页查询（T6.2）
export function listClockRecord(query) {
  return request({
    url: '/fee/clock/list',
    method: 'get',
    params: query
  })
}

// 打卡记录详情
export function getClockRecord(id) {
  return request({
    url: '/fee/clock/' + id,
    method: 'get'
  })
}

// 报销单关联打卡记录（追溯时间线）
export function listClockByBill(billId) {
  return request({
    url: '/fee/clock/bill/' + billId,
    method: 'get'
  })
}

// 打卡触发规则配置列表
export function listClockConfig() {
  return request({
    url: '/fee/clock/config/list',
    method: 'get'
  })
}

// 新增打卡触发规则
export function addClockConfig(data) {
  return request({
    url: '/fee/clock/config',
    method: 'post',
    data: data
  })
}

// 修改打卡触发规则
export function updateClockConfig(data) {
  return request({
    url: '/fee/clock/config',
    method: 'put',
    data: data
  })
}

// 删除打卡触发规则
export function delClockConfig(id) {
  return request({
    url: '/fee/clock/config/' + id,
    method: 'delete'
  })
}
