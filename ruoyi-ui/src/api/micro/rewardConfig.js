import request from '@/utils/request'

// ===== T18.12 老板拉新奖励配置 =====

export function getRewardConfig() {
  return request({ url: '/micro/rewardConfig', method: 'get' })
}

export function saveRewardConfig(data) {
  return request({ url: '/micro/rewardConfig', method: 'put', data })
}
