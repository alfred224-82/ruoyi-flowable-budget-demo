import request from '@/utils/request'

// 消息提醒与通知方式设置（T15.14 / T15.24）
// ★通知方式设置落地在 tenant_biz_config.module_overrides.notifyConfig（字符串，约定为 JSON 结构）
// ★消息列表复用系统消息中心 sys_message（ISysMessageService），按当前登录人过滤

// 消息提醒列表（可按消息类型 / 已读状态过滤）
export function listNotify(query) {
  return request({
    url: '/micro/notify/list',
    method: 'get',
    params: query
  })
}

// 未读消息数
export function getUnreadCount() {
  return request({
    url: '/micro/notify/unreadCount',
    method: 'get'
  })
}

// 标记单条消息已读
export function readNotify(id) {
  return request({
    url: '/micro/notify/read/' + id,
    method: 'put'
  })
}

// 标记全部消息已读
export function readAllNotify() {
  return request({
    url: '/micro/notify/readAll',
    method: 'put'
  })
}

// 读取通知方式设置（返回 notifyConfig 字符串）
export function getNotifyConfig() {
  return request({
    url: '/micro/notify/config',
    method: 'get'
  })
}

// 保存通知方式设置（仅覆盖 notifyConfig 键，其余店铺配置保留）
export function saveNotifyConfig(notifyConfig) {
  return request({
    url: '/micro/notify/config',
    method: 'put',
    data: { notifyConfig: notifyConfig }
  })
}
