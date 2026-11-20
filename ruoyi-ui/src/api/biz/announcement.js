import request from '@/utils/request'

// 直播公告列表（含服务端派生展示态）
export function listAnnouncement(query) {
  return request({
    url: '/biz/announcement/list',
    method: 'get',
    params: query
  })
}

// 首页直播看板（仅可见公告，LIVING 优先；无则 hasAnnouncement=false）
export function getAnnouncementBoard(projectId) {
  return request({
    url: '/biz/announcement/board',
    method: 'get',
    params: { projectId }
  })
}

// 公告详情
export function getAnnouncement(id) {
  return request({
    url: '/biz/announcement/' + id,
    method: 'get'
  })
}

// 新建公告
export function addAnnouncement(data) {
  return request({
    url: '/biz/announcement',
    method: 'post',
    data: data
  })
}

// 编辑公告
export function updateAnnouncement(data) {
  return request({
    url: '/biz/announcement',
    method: 'put',
    data: data
  })
}

// 开播（PENDING→LIVING）
export function startLiving(id) {
  return request({
    url: '/biz/announcement/startLiving/' + id,
    method: 'put'
  })
}

// 下播（LIVING→OFFLINE）
export function offlineAnnouncement(id) {
  return request({
    url: '/biz/announcement/offline/' + id,
    method: 'put'
  })
}

// 删除公告
export function delAnnouncement(ids) {
  return request({
    url: '/biz/announcement/' + ids,
    method: 'delete'
  })
}
