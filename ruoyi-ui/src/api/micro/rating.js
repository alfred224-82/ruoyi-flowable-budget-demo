import request from '@/utils/request'

// 评价与点赞管理（T17，老板 PC 端 + 小程序端）

// ===== 评价管理 =====

// 评价列表（老板 PC，分页 + 筛选）
export function listRating(query) {
  return request({
    url: '/micro/rating/list',
    method: 'get',
    params: query
  })
}

// 评价详情（含图片）
export function getRating(id) {
  return request({
    url: '/micro/rating/' + id,
    method: 'get'
  })
}

// 回复评价（一条评价仅能回复一次）
export function replyRating(id, replyContent) {
  return request({
    url: '/micro/rating/reply',
    method: 'put',
    params: { id, replyContent }
  })
}

// 屏蔽不当评价（NORMAL→SHIELDED）
export function shieldRating(id) {
  return request({
    url: '/micro/rating/shield/' + id,
    method: 'put'
  })
}

// ===== 综合评分 =====

// 综合评分查询（按 userId 聚合：平均评分 + 评价总数 + 星级分布 + 点赞数）
export function getRatingSummary(userId) {
  return request({
    url: '/micro/rating/summary/' + userId,
    method: 'get'
  })
}

// 综合评分排行（按 targetUserId 聚合排行）
export function getRatingRanking(query) {
  return request({
    url: '/micro/rating/ranking',
    method: 'get',
    params: query
  })
}
