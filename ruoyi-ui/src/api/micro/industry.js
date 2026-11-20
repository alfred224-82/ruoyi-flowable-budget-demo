import request from '@/utils/request'

// 行业模板管理（T15.10，admin PC，小微版专属）
// ★新增行业只需配行业模板，小程序零改动；系统级共享模板不按 tenant_id 隔离

// 行业模板列表（分页）
export function listIndustry(query) {
  return request({
    url: '/micro/industry/list',
    method: 'get',
    params: query
  })
}

// 全部启用行业（新建小微版租户时的行业选择下拉）
export function listAllIndustry() {
  return request({
    url: '/micro/industry/all',
    method: 'get'
  })
}

// 行业模板详情
export function getIndustry(id) {
  return request({
    url: '/micro/industry/' + id,
    method: 'get'
  })
}

// ① 新建行业模板
export function addIndustry(data) {
  return request({
    url: '/micro/industry',
    method: 'post',
    data: data
  })
}

// ①③ 编辑行业模板（含 preset_config 预置数据）
export function updateIndustry(data) {
  return request({
    url: '/micro/industry',
    method: 'put',
    data: data
  })
}

// 删除行业模板（未种子化独立按钮，归 system:industry:edit）
export function delIndustry(ids) {
  return request({
    url: '/micro/industry/' + ids,
    method: 'delete'
  })
}

// ② 查询指定行业的角色映射列表（业务角色名→标准角色 code）
export function listRoleMap(industryCode) {
  return request({
    url: '/micro/industry/roleMap/' + industryCode,
    method: 'get'
  })
}

// ② 保存指定行业的角色映射（整表替换）
export function saveRoleMap(industryCode, roleMaps) {
  return request({
    url: '/micro/industry/roleMap/' + industryCode,
    method: 'put',
    data: roleMaps
  })
}

// ④ 按行业模板初始化目标租户角色映射（新建小微版租户时调用，幂等）
export function initTenantByIndustry(tenantId, industryCode) {
  return request({
    url: '/micro/industry/initTenant',
    method: 'post',
    params: { tenantId: tenantId, industryCode: industryCode }
  })
}
