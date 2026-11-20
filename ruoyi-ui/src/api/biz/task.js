import request from '@/utils/request'

// 企业任务列表（type=general 容器）
export function listTask(query) {
  return request({
    url: '/biz/task/list',
    method: 'get',
    params: query
  })
}

// 任务详情
export function getTask(id) {
  return request({
    url: '/biz/task/' + id,
    method: 'get'
  })
}

// 新建任务
export function addTask(data) {
  return request({
    url: '/biz/task',
    method: 'post',
    data: data
  })
}

// 编辑任务
export function updateTask(data) {
  return request({
    url: '/biz/task',
    method: 'put',
    data: data
  })
}

// 指派 + 进度更新
export function assignTask(id, assigneeUserId, progress) {
  return request({
    url: '/biz/task/assign/' + id,
    method: 'put',
    params: { assigneeUserId, progress }
  })
}

// 删除任务
export function delTask(ids) {
  return request({
    url: '/biz/task/' + ids,
    method: 'delete'
  })
}
