import request from '@/utils/request'

// 活动列表（后端强制 type=activity）
export function listActivity(query) {
  return request({
    url: '/biz/activity/list',
    method: 'get',
    params: query
  })
}

// 活动详情
export function getActivity(id) {
  return request({
    url: '/biz/activity/' + id,
    method: 'get'
  })
}

// 新建活动
export function addActivity(data) {
  return request({
    url: '/biz/activity',
    method: 'post',
    data: data
  })
}

// 编辑活动
export function updateActivity(data) {
  return request({
    url: '/biz/activity',
    method: 'put',
    data: data
  })
}

// 发布活动（DRAFT→PUBLISHED，发布后报名可用）
export function publishActivity(id) {
  return request({
    url: '/biz/activity/publish/' + id,
    method: 'put'
  })
}

// 结束活动（→FINISHED，结束后不可报名）
export function finishActivity(id) {
  return request({
    url: '/biz/activity/finish/' + id,
    method: 'put'
  })
}

// 删除活动
export function delActivity(ids) {
  return request({
    url: '/biz/activity/' + ids,
    method: 'delete'
  })
}
