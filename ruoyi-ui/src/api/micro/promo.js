import request from '@/utils/request'

// 推广发布（T15.24，老板 PC）：活动管理 CRUD + 发布/结束 + 生成分享海报
// ★海报带租户号二维码，扫码进入即锁定租户（写入 share_scene）
// ★lite 档（micro_pro 未开通）发布的活动即「活动浏览页」只读数据源（T15.28）

// 活动列表（分页）
export function listPromo(query) {
  return request({
    url: '/micro/promo/list',
    method: 'get',
    params: query
  })
}

// 活动详情
export function getPromo(id) {
  return request({
    url: '/micro/promo/' + id,
    method: 'get'
  })
}

// 发布活动（★保存时后端同步执行语音合规 ASR，最坏约 60s，单独放宽超时至 120s，避免全局 10s 误报超时）
export function addPromo(data) {
  return request({
    url: '/micro/promo',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

// 编辑活动（★同 addPromo：语音变更时后端同步 ASR 校验）
export function updatePromo(data) {
  return request({
    url: '/micro/promo',
    method: 'put',
    data: data,
    timeout: 120000
  })
}

// 变更活动状态（草稿/已发布/已结束）
export function changePromoStatus(id, status) {
  return request({
    url: '/micro/promo/status/' + id + '/' + status,
    method: 'put'
  })
}

// 生成分享海报（返回活动的 posterUrl 与标题，二维码场景由 /micro/share/poster 落库）
export function getPromoPoster(id) {
  return request({
    url: '/micro/promo/poster/' + id,
    method: 'get'
  })
}

// 删除活动（逻辑删除，可批量）
export function delPromo(ids) {
  return request({
    url: '/micro/promo/' + ids,
    method: 'delete'
  })
}

// 当前租户是否启用语音（活动发布页据此显示语音上传）
export function checkVoiceEnabled() {
  return request({
    url: '/micro/activity/setting/voiceEnabled',
    method: 'get'
  })
}

// LLM 描述生成图片（★付费版专属）
export function generateLlmImage(description) {
  return request({
    url: '/micro/activity/setting/llmImage',
    method: 'post',
    data: { description }
  })
}

// ★M22 锁定/解锁活动（旧端点，保留兼容存量调用）
export function lockPromo(id, locked) {
  return request({
    url: '/micro/promo/lock/' + id + '/' + locked,
    method: 'put'
  })
}

// ★M28 T28.1 锁定活动（新端点，perms micro:activity:lock；后端经操作日志切面记录 LOCK）
export function lockActivity(id) {
  return request({
    url: '/micro/activity/' + id + '/lock',
    method: 'put'
  })
}

// ★M28 T28.1 解锁活动（新端点，perms micro:activity:unlock；解锁二次确认由前端弹窗完成）
export function unlockActivity(id) {
  return request({
    url: '/micro/activity/' + id + '/unlock',
    method: 'put'
  })
}

// ★H5推广码：生成 H5 URL 的二维码（base64 图片），顾客扫码直接进入登录页（带 ref_t/ref_u 参数）
//   与 app 端 boss 推广码共用同一后端方法 /micro/promo/code/myQrcode
export function getMyQrcode() {
  return request({
    url: '/micro/promo/code/myQrcode',
    method: 'get'
  })
}
