import request from '@/utils/request'

// 查询用途标签列表（管理端，需权限）
export function listPurposeTag(query) {
  return request({
    url: '/fee/purposeTag/list',
    method: 'get',
    params: query
  })
}

// 获取全部启用标签（下拉用，无需权限）
export function allEnabledPurposeTag() {
  return request({
    url: '/fee/purposeTag/allEnabled',
    method: 'get'
  })
}

// 查询用途标签详细
export function getPurposeTag(id) {
  return request({
    url: '/fee/purposeTag/' + id,
    method: 'get'
  })
}

// 新增用途标签
export function addPurposeTag(data) {
  return request({
    url: '/fee/purposeTag',
    method: 'post',
    data: data
  })
}

// 修改用途标签
export function updatePurposeTag(data) {
  return request({
    url: '/fee/purposeTag',
    method: 'put',
    data: data
  })
}

// 删除用途标签
export function delPurposeTag(id) {
  return request({
    url: '/fee/purposeTag/' + id,
    method: 'delete'
  })
}
