import request from '@/utils/request'

// 部门预算编制单列表（仅部门级单据，T6.1）
export function listDeptBudget(query) {
  return request({
    url: '/fee/deptBudget/list',
    method: 'get',
    params: query
  })
}

// 部门预算编制单详情
export function getDeptBudget(id) {
  return request({
    url: '/fee/deptBudget/' + id,
    method: 'get'
  })
}

// 部门科目额度汇总（编制额/已用/剩余）
export function getDeptQuota(query) {
  return request({
    url: '/fee/deptBudget/quota',
    method: 'get',
    params: query
  })
}

// 创建部门预算编制单
export function addDeptBudget(data) {
  return request({
    url: '/fee/deptBudget',
    method: 'post',
    data: data
  })
}

// 弹性额度配置列表（T6.1）
export function listElasticQuota() {
  return request({
    url: '/fee/elasticQuota/list',
    method: 'get'
  })
}

// 新增弹性额度配置
export function addElasticQuota(data) {
  return request({
    url: '/fee/elasticQuota',
    method: 'post',
    data: data
  })
}

// 修改弹性额度配置
export function updateElasticQuota(data) {
  return request({
    url: '/fee/elasticQuota',
    method: 'put',
    data: data
  })
}

// 删除弹性额度配置
export function delElasticQuota(id) {
  return request({
    url: '/fee/elasticQuota/' + id,
    method: 'delete'
  })
}
