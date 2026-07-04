import request from '@/utils/request'

// 获取未读消息数
export function getUnreadCount() {
  return request({
    url: '/system/message/unreadCount',
    method: 'get'
  })
}

// 查询消息列表
export function listMessage(query) {
  return request({
    url: '/system/message/list',
    method: 'get',
    params: query
  })
}

// 标记单条已读
export function markAsRead(id) {
  return request({
    url: '/system/message/read/' + id,
    method: 'put'
  })
}

// 标记全部已读
export function markAllAsRead() {
  return request({
    url: '/system/message/readAll',
    method: 'put'
  })
}
