import request from '@/utils/request'

// M31 T31.6/T31.8 Boss 游客信息管理

// 临时游客列表（分页）
export function listVisitorProfile(query) {
  return request({
    url: '/micro/customer/profile/list',
    method: 'get',
    params: query
  })
}

// Boss 补全游客信息
export function updateVisitorProfile(userId, data) {
  return request({
    url: '/micro/customer/profile/' + userId,
    method: 'put',
    data: data
  })
}

// Boss 批量转正临时游客
export function batchUpgradeVisitors(userIds) {
  return request({
    url: '/micro/customer/profile/batchUpgrade',
    method: 'post',
    data: userIds
  })
}
