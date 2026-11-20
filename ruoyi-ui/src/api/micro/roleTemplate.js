import request from '@/utils/request'

// 角色模板管理（T15.7 / T15.26 / T15.29）：version_type × role_code 的菜单 + 区块配方
// ★三层控制模型第①层：角色模板定「权限天花板」，页面装修只能在天花板内调排版（T15.30）
// ★version_type 取值：micro_lite / micro_pro / basic / advanced / pro
// ★lite→pro 升级 = 换配方（菜单扩围），角色 code / 用户 / 业务数据零迁移
// ★role_template 为系统级共享表（不按 tenant_id 隔离），仅 admin 维护

// 角色模板列表（分页）
export function listRoleTemplate(query) {
  return request({
    url: '/micro/roleTemplate/list',
    method: 'get',
    params: query
  })
}

// 按版本档位查全部角色配方（版本档位管理页的「每档菜单范围」数据源）
export function listTemplateByVersion(versionType) {
  return request({
    url: '/micro/roleTemplate/version/' + versionType,
    method: 'get'
  })
}

// 查单个配方（version_type + role_code 唯一定位）
export function getRecipe(versionType, roleCode) {
  return request({
    url: '/micro/roleTemplate/recipe',
    method: 'get',
    params: { versionType: versionType, roleCode: roleCode }
  })
}

// 角色模板详情
export function getRoleTemplate(id) {
  return request({
    url: '/micro/roleTemplate/' + id,
    method: 'get'
  })
}

// 新建角色模板配方
export function addRoleTemplate(data) {
  return request({
    url: '/micro/roleTemplate',
    method: 'post',
    data: data
  })
}

// 编辑角色模板配方
export function updateRoleTemplate(data) {
  return request({
    url: '/micro/roleTemplate',
    method: 'put',
    data: data
  })
}

// 绑定菜单范围（menu_keys / block_keys JSON 数组，★该档位解锁的菜单集合，升级即扩围）
export function bindTemplateMenu(data) {
  return request({
    url: '/micro/roleTemplate/bindMenu',
    method: 'put',
    data: data
  })
}

// 删除角色模板配方
export function delRoleTemplate(ids) {
  return request({
    url: '/micro/roleTemplate/' + ids,
    method: 'delete'
  })
}
