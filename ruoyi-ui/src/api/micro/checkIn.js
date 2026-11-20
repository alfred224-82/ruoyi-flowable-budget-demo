import request from '@/utils/request'

// 打卡记录管理（T24.5，Boss PC 打卡记录管理 + 补录 + 导出 + 配置）
// ★类级 @RequireFeature("FEAT_CHECK_IN")，micro_pro 收费版启用，micro_lite 免费版不启用
// ★三级开关链路：industry_template.check_in_enabled → sys_tenant.check_in_enabled → check_in_config.enabled

// ===== 打卡记录查询 =====

// 查询活动打卡列表（Boss PC）
// params: { staffUserId }
export function listActivityRecords(activityId, params) {
  return request({
    url: '/micro/checkIn/activity/' + activityId,
    method: 'get',
    params: params
  })
}

// ===== 补录 =====

// Boss 补录打卡（代员工补录，员工忘打时）
// data: { staffUserId, activityId, checkInTime, locationName, supplementReason }
export function supplementCheckIn(data) {
  return request({
    url: '/micro/checkIn/supplement',
    method: 'post',
    data: data
  })
}

// ===== 导出 =====

// 导出打卡记录 Excel
// params: { activityId, staffUserId, startDate, endDate }
export function exportRecords(params) {
  return request({
    url: '/micro/checkIn/export',
    method: 'post',
    params: params,
    responseType: 'blob'
  })
}

// ===== 打卡配置 =====

// 获取打卡配置
export function getCheckInConfig() {
  return request({
    url: '/micro/checkIn/config',
    method: 'get'
  })
}

// 更新打卡配置（Boss 设置拍照要求/迟到阈值）
// data: { enabled, photoRequired, lateThresholdMinutes }
export function updateCheckInConfig(data) {
  return request({
    url: '/micro/checkIn/config',
    method: 'put',
    data: data
  })
}

// 查询三级开关状态（前端判断是否显示打卡入口）
export function checkInEnabled() {
  return request({
    url: '/micro/checkIn/enabled',
    method: 'get'
  })
}
