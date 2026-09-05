import request from '@/utils/request'

// 薪资基数列表
export function listSalary(query) {
  return request({
    url: '/fee/salary/list',
    method: 'get',
    params: query
  })
}

// 新增/修改薪资基数（userId 唯一）
export function saveSalary(data) {
  return request({
    url: '/fee/salary',
    method: 'post',
    data: data
  })
}

// 离职（停止计提）
export function resignSalary(id) {
  return request({
    url: '/fee/salary/resign/' + id,
    method: 'put'
  })
}

// 计提记录列表
export function listSalaryRecord(query) {
  return request({
    url: '/fee/salary/record/list',
    method: 'get',
    params: query
  })
}

// 手动触发月度薪资计提（幂等）
export function accrueSalaries(accrualMonth) {
  return request({
    url: '/fee/salary/accrue',
    method: 'put',
    params: { accrualMonth }
  })
}
