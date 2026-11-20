import request from '@/utils/request'

// 版本档位管理（T15.29）+ 新建租户三级级联（产品线 → 版本档位 → 行业模板）
// ★后端 SysEditionController 仅提供只读聚合接口，无 list/add/edit/bindMenu CRUD；
//   档位配方的写操作统一走 @/api/micro/roleTemplate（bindTemplateMenu，perms system:roleTemplate:bindMenu）
// ★version_type 取值：micro_lite / micro_pro / basic / advanced / pro
// ★级联接口 perms 复用 system:tenant:list（新建租户表单内调用）

// 平台能力总览（edition / bizCode / accessChannel / features / copy 聚合，用于档位对照表）
export function getEditionFeatures() {
  return request({
    url: '/system/edition/features',
    method: 'get'
  })
}

// 版本档位下拉（tenantType=micro|enterprise，级联第二级）
export function listVersionOptions(tenantType) {
  return request({
    url: '/system/edition/version-options',
    method: 'get',
    params: { tenantType: tenantType }
  })
}

// 行业模板下拉（级联第三级，★非 micro 返回空数组）
export function listIndustryOptions(tenantType) {
  return request({
    url: '/system/edition/industry-options',
    method: 'get',
    params: { tenantType: tenantType }
  })
}

// 档位默认角色预览（roleCode / menuCount / blockCount，用于「该档位解锁了什么」展示）
export function getDefaultRolePreview(versionType) {
  return request({
    url: '/system/edition/default-role-preview',
    method: 'get',
    params: { versionType: versionType }
  })
}
