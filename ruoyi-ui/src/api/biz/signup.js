import request from '@/utils/request'

// 活动报名列表（人数/名额/候补/签到）
export function listSignup(query) {
  return request({
    url: '/biz/signup/list',
    method: 'get',
    params: query
  })
}

// 报名登记（满员进候补，返回成团判定结果）
export function addSignup(data) {
  return request({
    url: '/biz/signup',
    method: 'post',
    data: data
  })
}

// 取消报名
export function cancelSignup(id) {
  return request({
    url: '/biz/signup/' + id,
    method: 'delete'
  })
}

// 签到（NOT_SIGNED→SIGNED/ABSENT）
export function checkinSignup(id, signInStatus) {
  return request({
    url: '/biz/signup/checkin/' + id,
    method: 'put',
    params: { signInStatus }
  })
}

// 容器当前成团状态（策略实时判定）
export function teamStatus(projectId) {
  return request({
    url: '/biz/signup/teamStatus/' + projectId,
    method: 'get'
  })
}
