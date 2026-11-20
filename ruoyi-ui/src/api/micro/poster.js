import request from '@/utils/request'

// M31 T31.1/T31.2 活动海报生成（Boss PC 端）

// 上传海报底图（POST /micro/promo/{id}/uploadPosterBg）
export function uploadPosterBg(activityId, data) {
  return request({
    url: '/micro/promo/' + activityId + '/uploadPosterBg',
    method: 'post',
    data: data
  })
}

// 生成活动海报（POST /micro/promo/{id}/generatePoster?bgImageUrl=...）
export function generatePoster(activityId, bgImageUrl) {
  return request({
    url: '/micro/promo/' + activityId + '/generatePoster',
    method: 'post',
    params: { bgImageUrl: bgImageUrl }
  })
}

// 查询海报生成任务状态（GET /micro/promo/poster/task/{taskId}）
export function getPosterTaskStatus(taskId) {
  return request({
    url: '/micro/promo/poster/task/' + taskId,
    method: 'get'
  })
}
