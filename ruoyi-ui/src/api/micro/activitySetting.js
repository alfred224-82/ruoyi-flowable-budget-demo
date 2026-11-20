import request from '@/utils/request'

// 活动设置（M19，老板 PC）：项目/房间/员工选择 + 自动标题 + 背景图
// ★背景图仅对当前活动生效，不跨活动复用

// 项目选择器数据源（排除 space 分类，单选）
export function listActivityProjects() {
  return request({
    url: '/micro/activity/setting/projects',
    method: 'get'
  })
}

// 房间选择器数据源（仅 space 分类，单/多选）
export function listActivityRooms() {
  return request({
    url: '/micro/activity/setting/rooms',
    method: 'get'
  })
}

// 员工选择器数据源（salesman 角色用户，单/多选）
export function listActivityStaff() {
  return request({
    url: '/micro/activity/setting/staff',
    method: 'get'
  })
}

// 查询活动设置（回显已保存的选择 + 解析名称）
export function getActivitySetting(activityId) {
  return request({
    url: '/micro/activity/setting/' + activityId,
    method: 'get'
  })
}

// 保存活动设置
export function saveActivitySetting(data) {
  return request({
    url: '/micro/activity/setting',
    method: 'put',
    data: data
  })
}

// 预览自动生成的标题（不保存）
export function previewAutoTitle(params) {
  return request({
    url: '/micro/activity/setting/previewTitle',
    method: 'get',
    params: params
  })
}

// 通用微信小程序码（不绑定活动，扫码直接进入小程序）
export function getGeneralQrcode() {
  return request({
    url: '/micro/activity/setting/generalQrcode',
    method: 'get'
  })
}
