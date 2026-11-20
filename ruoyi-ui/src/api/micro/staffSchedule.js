import request from '@/utils/request'

// 员工排班管理（T22.3，Boss PC 排班日历 + 批量编排）
// ★类级 @RequireFeature("FEAT_STAFF_SCHEDULE")，micro_pro 收费版启用，micro_lite 免费版不启用
// ★班次分早班 morning / 晚班 evening，起止时间由 Boss 自定（禁止硬编码）
// ★状态 draft→published；发布后仍可修正，修正写 schedule_change_log 留痕

// ===== 员工下拉选项 =====

// 查询本租户 salesman 角色员工作为排班下拉选项（含昵称/头像）
export function listStaffOptions() {
  return request({
    url: '/micro/staffSchedule/staffOptions',
    method: 'get'
  })
}

// ===== 排班查询 =====

// 查询排班列表（Boss PC 排班日历）
// params: { periodType, periodKey, status, staffId, startDate, endDate }
export function listSchedules(params) {
  return request({
    url: '/micro/staffSchedule/list',
    method: 'get',
    params: params
  })
}

// ===== 批量创建 =====

// 批量创建草稿排班（Boss 按周/月编排）
// data: { periodType, periodKey, items: [{ staffId, scheduleDate, shiftType, startTime, endTime, remark }] }
export function batchCreate(data) {
  return request({
    url: '/micro/staffSchedule/batchCreate',
    method: 'post',
    data: data
  })
}

// ===== 发布 =====

// 发布排班（将指定周期 draft 批量置为 published）
// params: { periodType, periodKey }
export function publishSchedule(params) {
  return request({
    url: '/micro/staffSchedule/publish',
    method: 'post',
    params: params
  })
}

// ===== 修正 =====

// 修正排班（published 记录编辑起止时间/班次，写留痕日志）
// data: { id, staffId, scheduleDate, shiftType, startTime, endTime, changeReason, remark }
export function modifySchedule(data) {
  return request({
    url: '/micro/staffSchedule/modify',
    method: 'put',
    data: data
  })
}

// 设定班次时间（Boss 自定义早班/晚班起止时间，批量更新指定周期内所有对应班次记录）
// params: { periodType, periodKey, shiftType, startTime, endTime }
export function setShiftTime(params) {
  return request({
    url: '/micro/staffSchedule/setShiftTime',
    method: 'put',
    params: params
  })
}

// 删除草稿排班（仅 draft 可删；published 不可删）
export function deleteDraft(id) {
  return request({
    url: '/micro/staffSchedule/' + id,
    method: 'delete'
  })
}

// ===== 修正日志 =====

// 查询修正日志（Boss PC 排班页展示）
// params: { scheduleId, periodType, periodKey }
export function listChangeLogs(params) {
  return request({
    url: '/micro/staffSchedule/changeLogs',
    method: 'get',
    params: params
  })
}
