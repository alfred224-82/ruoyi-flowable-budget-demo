import request from '@/utils/request'

// 项目容器列表（project 统一容器）
export function listProject(query) {
  return request({
    url: '/biz/project/list',
    method: 'get',
    params: query
  })
}

// 业态分组工作台：各业态容器数量
export function groupCount() {
  return request({
    url: '/biz/project/groupCount',
    method: 'get'
  })
}

// 容器详情
export function getProject(id) {
  return request({
    url: '/biz/project/' + id,
    method: 'get'
  })
}

// 新建容器
export function addProject(data) {
  return request({
    url: '/biz/project',
    method: 'post',
    data: data
  })
}

// 编辑容器（含 extra_config）
export function updateProject(data) {
  return request({
    url: '/biz/project',
    method: 'put',
    data: data
  })
}

// 发布容器（DRAFT→PUBLISHED）
export function publishProject(id) {
  return request({
    url: '/biz/project/publish/' + id,
    method: 'put'
  })
}

// 删除容器
export function delProject(ids) {
  return request({
    url: '/biz/project/' + ids,
    method: 'delete'
  })
}

// 容器成员列表（五枚举）
export function listMembers(projectId) {
  return request({
    url: '/biz/project/members/' + projectId,
    method: 'get'
  })
}

// 批量保存成员
export function saveMembers(projectId, members) {
  return request({
    url: '/biz/project/members/' + projectId,
    method: 'put',
    data: members
  })
}

// 移除成员
export function removeMember(projectId, userId) {
  return request({
    url: '/biz/project/members/' + projectId + '/' + userId,
    method: 'delete'
  })
}

// 本租户启用状态用户下拉（成员/指派选择用）
export function listUserOptions() {
  return request({
    url: '/biz/project/userOptions',
    method: 'get'
  })
}
