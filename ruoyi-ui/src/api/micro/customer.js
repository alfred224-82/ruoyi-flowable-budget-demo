import request from '@/utils/request'

// 顾客管理（T15.32，老板 PC 防爽约/黑名单，★仅 micro_pro 档生效）
// ★黑名单唯一解除出口 = 老板手动移出：is_blacklisted=0，no_show_count 保留不清零（历史可追溯）
// ★满 3 次爽约自动拉黑由后端 XXL-JOB / 标记爽约时触发，前端不提供「拉黑」写入口
// ★注意：本模块与顾客小程序（T15.12）共用 /micro/customer 前缀，子路径不重叠

// 顾客信誉/黑名单列表（userId 精确过滤，isBlacklisted：0正常/1已拉黑）
export function listCustomer(query) {
  return request({
    url: '/micro/customer/list',
    method: 'get',
    params: query
  })
}

// 移出黑名单（perms micro:customer:unblacklist，唯一解除出口）
export function unblacklistCustomer(userId) {
  return request({
    url: '/micro/customer/unblacklist/' + userId,
    method: 'put'
  })
}

// 顾客爽约明细（perms micro:customer:creditDetail，返回 no_show_record 完整历史）
export function getCreditDetail(userId) {
  return request({
    url: '/micro/customer/creditDetail/' + userId,
    method: 'get'
  })
}
