import request from '@/utils/request'

// 查询预算编制列表
export function listPreparation(query) {
  return request({
    url: '/system/preparation/list',
    method: 'get',
    params: query
  })
}

// 查询预算编制详细
export function getPreparation(id) {
  return request({
    url: '/system/preparation/' + id,
    method: 'get'
  })
}

// 新增预算编制
export function addPreparation(data) {
  return request({
    url: '/system/preparation',
    method: 'post',
    data: data
  })
}

// 修改预算编制
export function updatePreparation(data) {
  return request({
    url: '/system/preparation',
    method: 'put',
    data: data
  })
}

// 删除预算编制
export function delPreparation(id) {
  return request({
    url: '/system/preparation/' + id,
    method: 'delete'
  })
}

// 完成编制
export function completePreparation(id) {
  return request({
    url: '/system/preparation/complete/' + id,
    method: 'post'
  })
}

// 提交审核
export function submitPreparation(id) {
  return request({
    url: '/system/preparation/submit/' + id,
    method: 'post'
  })
}

// 批量提交审核
export function batchSubmitPreparation(ids) {
  return request({
    url: '/system/preparation/batchSubmit',
    method: 'post',
    data: ids
  })
}

// 查询预算编制明细列表
export function listPreparationDetail(sheetId) {
  return request({
    url: '/system/preparation/detail/list/' + sheetId,
    method: 'get'
  })
}

// 保存预算编制明细
export function savePreparationDetail(data) {
  return request({
    url: '/system/preparation/detail',
    method: 'post',
    data: data
  })
}

// 批量保存预算编制明细
export function batchSavePreparationDetail(data) {
  return request({
    url: '/system/preparation/detail/batch',
    method: 'post',
    data: data
  })
}

// 查询所有科目（树形）
export function listAllSubjects() {
  return request({
    url: '/system/subject/all',
    method: 'get'
  })
}

// 查询一级科目列表
export function listFirstLevelSubject() {
  return request({
    url: '/system/subject/firstLevel',
    method: 'get'
  })
}

// 查询二级科目列表（根据父级ID）
export function listSecondLevelSubject(parentId) {
  return request({
    url: '/system/subject/secondLevel/' + parentId,
    method: 'get'
  })
}

// 批量审批
export function batchApprove(data) {
  return request({
    url: '/system/preparation/batchApprove',
    method: 'post',
    data: data
  })
}

// 审批通过
export function approvePreparation(id, remark) {
  return request({
    url: '/system/preparation/approve/' + id,
    method: 'post',
    params: { remark }
  })
}

// 审批驳回
export function rejectPreparation(id, reason) {
  return request({
    url: '/system/preparation/reject/' + id,
    method: 'post',
    params: { reason }
  })
}

// 获取校验结果
export function getValidationResults(sheetId) {
  return request({
    url: '/system/preparation/validation/' + sheetId,
    method: 'get'
  })
}

// 执行规则校验（实时）
export function executeValidation(details) {
  return request({
    url: '/system/preparation/validation/execute',
    method: 'post',
    data: details
  })
}

// 打回至部门
export function sendBackToDept(id, deptId, reason) {
  return request({
    url: '/system/preparation/sendBack/' + id,
    method: 'post',
    params: { deptId, reason }
  })
}

// 查询驳回历史
export function getRejectHistory(sheetId) {
  return request({
    url: '/system/preparation/reject/history/' + sheetId,
    method: 'get'
  })
}

// 获取上月已审批通过的预算明细（用于初始化本月预算金额）
export function getPreviousMonthDetails(orgId, budgetYear, budgetMonth) {
  return request({
    url: '/system/preparation/previousMonthDetails',
    method: 'get',
    params: { orgId, budgetYear, budgetMonth }
  })
}
