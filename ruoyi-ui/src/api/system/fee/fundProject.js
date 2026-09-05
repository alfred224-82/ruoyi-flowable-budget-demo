import request from '@/utils/request'

// 查询项目列表（数据范围：项目经理仅本人参与项目）
export function listProject(query) {
  return request({
    url: '/fee/project/list',
    method: 'get',
    params: query
  })
}

// 查询项目详情
export function getProject(id) {
  return request({
    url: '/fee/project/' + id,
    method: 'get'
  })
}

// 立项
export function addProject(data) {
  return request({
    url: '/fee/project',
    method: 'post',
    data: data
  })
}

// 编辑项目（仅待审批状态）
export function updateProject(data) {
  return request({
    url: '/fee/project',
    method: 'put',
    data: data
  })
}

// 老板审批通过
export function approveProject(id) {
  return request({
    url: '/fee/project/approve/' + id,
    method: 'put'
  })
}

// 审批驳回
export function rejectProject(id, reason) {
  return request({
    url: '/fee/project/reject/' + id,
    method: 'put',
    params: { reason }
  })
}

// 关闭项目
export function closeProject(id) {
  return request({
    url: '/fee/project/close/' + id,
    method: 'put'
  })
}

// 进行中项目下拉
export function listActiveProject() {
  return request({
    url: '/fee/project/activeList',
    method: 'get'
  })
}

// 项目利润拆解（纸面/实际分离）
export function getProjectProfit(id) {
  return request({
    url: '/fee/project/profit/' + id,
    method: 'get'
  })
}

// 项目科目列表
export function listSubjects(id) {
  return request({
    url: '/fee/project/subjects/' + id,
    method: 'get'
  })
}

// 批量设置项目科目（按大类分配预算）
export function saveSubjects(id, subjects) {
  return request({
    url: '/fee/project/subjects/' + id,
    method: 'put',
    data: subjects
  })
}

// 项目成员列表
export function listMembers(id) {
  return request({
    url: '/fee/project/members/' + id,
    method: 'get'
  })
}

// 批量保存项目成员
export function saveMembers(id, members) {
  return request({
    url: '/fee/project/members/' + id,
    method: 'put',
    data: members
  })
}
