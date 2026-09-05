import request from '@/utils/request'

// 账套分页列表
export function listAccountSet(query) {
  return request({ url: '/fee/accountSet/list', method: 'get', params: query })
}

// 全部账套（下拉/切换用）
export function listAllAccountSet() {
  return request({ url: '/fee/accountSet/listAll', method: 'get' })
}

// 账套详情
export function getAccountSet(id) {
  return request({ url: '/fee/accountSet/' + id, method: 'get' })
}

// 新增账套
export function addAccountSet(data) {
  return request({ url: '/fee/accountSet', method: 'post', data })
}

// 修改账套
export function updateAccountSet(data) {
  return request({ url: '/fee/accountSet', method: 'put', data })
}

// 删除账套（有单据归属或为默认账套时后端禁止）
export function delAccountSet(ids) {
  return request({ url: '/fee/accountSet/' + ids, method: 'delete' })
}

// 设为默认账套
export function setDefaultAccountSet(id) {
  return request({ url: '/fee/accountSet/default/' + id, method: 'put' })
}

// 切换当前操作账套上下文
export function switchAccountSet(id) {
  return request({ url: '/fee/accountSet/switch/' + id, method: 'put' })
}

// 合并报表：各账套已审批单据汇总（集团视角）
export function consolidatedReport(period) {
  return request({ url: '/fee/accountSet/consolidatedReport', method: 'get', params: { period } })
}
