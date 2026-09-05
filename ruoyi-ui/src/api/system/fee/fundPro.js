import request from '@/utils/request'

// License 授权信息（前端能力下发依据：edition + features）
export function getLicenseInfo() {
  return request({
    url: '/system/license/info',
    method: 'get'
  })
}

// 组织树（含成员数统计）
export function getOrgTree() {
  return request({
    url: '/fee/org/tree',
    method: 'get'
  })
}

// 新增组织
export function addOrg(data) {
  return request({
    url: '/fee/org',
    method: 'post',
    data: data
  })
}

// 修改组织
export function updateOrg(data) {
  return request({
    url: '/fee/org',
    method: 'put',
    data: data
  })
}

// 删除组织
export function delOrg(id) {
  return request({
    url: '/fee/org/' + id,
    method: 'delete'
  })
}

// 组织成员列表
export function listOrgUsers(orgId) {
  return request({
    url: '/fee/org/users/' + orgId,
    method: 'get'
  })
}

// 全量保存组织成员（用户挂组织）
export function saveOrgUsers(data) {
  return request({
    url: '/fee/org/users',
    method: 'put',
    data: data
  })
}

// 组织维度预算编制列表
export function listOrgPreparations(orgId, period) {
  return request({
    url: '/fee/orgBudget/preparations',
    method: 'get',
    params: { orgId, period }
  })
}

// 集团合并视角（各组织汇总 + 集团合计行）
export function listConsolidated(period) {
  return request({
    url: '/fee/orgBudget/consolidated',
    method: 'get',
    params: { period }
  })
}

// 锁定组织预算快照
export function lockOrgSnapshot(orgId, period) {
  return request({
    url: '/fee/orgBudget/snapshot',
    method: 'post',
    params: { orgId, period }
  })
}

// 组织快照版本列表
export function listOrgSnapshotVersions(orgId) {
  return request({
    url: '/fee/orgBudget/versions',
    method: 'get',
    params: { orgId }
  })
}

// 快照详情
export function getOrgSnapshot(id) {
  return request({
    url: '/fee/orgBudget/version/' + id,
    method: 'get'
  })
}

// 人脸核身配置查询
export function getFaceConfig() {
  return request({
    url: '/fee/face/config',
    method: 'get'
  })
}

// 人脸核身配置保存
export function saveFaceConfig(data) {
  return request({
    url: '/fee/face/config',
    method: 'put',
    data: data
  })
}

// 人脸核身触发判定
export function faceTrigger(amount, category) {
  return request({
    url: '/fee/face/trigger',
    method: 'get',
    params: { amount, category }
  })
}

// 提交人脸核身结果
export function submitFaceVerify(data) {
  return request({
    url: '/fee/face/submit',
    method: 'post',
    data: data
  })
}

// 核身记录列表（异常池=仅存疑）
export function listFaceRecords(suspiciousOnly) {
  return request({
    url: '/fee/face/records',
    method: 'get',
    params: { suspiciousOnly }
  })
}

// 规则版本列表
export function listRuleVersions() {
  return request({
    url: '/fee/ruleVersion/list',
    method: 'get'
  })
}

// 发布规则版本（灰度百分比）
export function publishRuleVersion(grayscalePct) {
  return request({
    url: '/fee/ruleVersion/publish',
    method: 'post',
    data: { grayscalePct }
  })
}

// 灰度命中率比对
export function compareRuleVersion(id) {
  return request({
    url: '/fee/ruleVersion/compare/' + id,
    method: 'get'
  })
}

// 全量发布
export function fullReleaseRuleVersion(id) {
  return request({
    url: '/fee/ruleVersion/fullRelease/' + id,
    method: 'put'
  })
}

// 回滚版本
export function rollbackRuleVersion(id) {
  return request({
    url: '/fee/ruleVersion/rollback/' + id,
    method: 'put'
  })
}

// AI 语义推荐科目
export function recommendSubject(text) {
  return request({
    url: '/fee/ruleVersion/recommend',
    method: 'get',
    params: { text }
  })
}

// 成员跨项目负载检查（不传=当前登录用户）
export function memberLoadCheck(userId) {
  return request({
    url: '/fee/project/memberLoad',
    method: 'get',
    params: { userId }
  })
}

// 看板团队人力分布卡片
export function dashboardMemberLoad() {
  return request({
    url: '/fee/dashboard/memberLoad',
    method: 'get'
  })
}

// 二维导出（凭证 + 项目×科目矩阵，Excel 二进制流）
export function runExport2d(month, templateId, projectId) {
  return request({
    url: '/fee/export/run2d',
    method: 'get',
    params: { month, templateId, projectId },
    responseType: 'blob'
  })
}
