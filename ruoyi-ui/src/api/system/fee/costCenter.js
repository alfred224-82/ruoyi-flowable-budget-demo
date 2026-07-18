import request from '@/utils/request'

// 查询责任中心列表
export function listCostCenter(query) {
  return request({
    url: '/fee/costCenter/list',
    method: 'get',
    params: query
  })
}

// 查询责任中心详细
export function getCostCenter(id) {
  return request({
    url: '/fee/costCenter/' + id,
    method: 'get'
  })
}

// 新增责任中心
export function addCostCenter(data) {
  return request({
    url: '/fee/costCenter',
    method: 'post',
    data: data
  })
}

// 修改责任中心
export function updateCostCenter(data) {
  return request({
    url: '/fee/costCenter',
    method: 'put',
    data: data
  })
}

// 删除责任中心
export function delCostCenter(id) {
  return request({
    url: '/fee/costCenter/' + id,
    method: 'delete'
  })
}

// 保存部门关联
export function saveDeptMapping(centerCode, deptIds) {
  return request({
    url: '/fee/costCenter/deptMapping',
    method: 'post',
    data: { centerCode, deptIds }
  })
}

// 查询部门列表
export function queryDeptList(centerCode) {
  return request({
    url: '/fee/costCenter/deptList/' + centerCode,
    method: 'get'
  })
}
