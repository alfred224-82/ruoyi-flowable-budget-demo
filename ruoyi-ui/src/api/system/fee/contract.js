import request from '@/utils/request'

// 合同台账列表
export function listContract(query) {
  return request({ url: '/fee/contract/list', method: 'get', params: query })
}

// 合同详情
export function getContract(id) {
  return request({ url: '/fee/contract/' + id, method: 'get' })
}

// 新增合同
export function addContract(data) {
  return request({ url: '/fee/contract', method: 'post', data })
}

// 修改合同
export function updateContract(data) {
  return request({ url: '/fee/contract', method: 'put', data })
}

// 终止合同
export function terminateContract(id) {
  return request({ url: '/fee/contract/terminate/' + id, method: 'post' })
}
