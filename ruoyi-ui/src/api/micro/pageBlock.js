import request from '@/utils/request'

// 页面装修（T15.16/T15.17）：page_block_config 区块勾选 + 拖拽排序 + 参数 + 恢复默认
// ★老板只做「个性化排版」，全程不涉及菜单/perms/区块权限配置
// ★三层控制模型（T15.30）：role_template 定权限天花板 ∩ page_block_config 定排版 = 小程序最终渲染

// 装修面板：查某角色某页面的原始区块配置（不过滤权限，按 sortOrder）
// tenantId 仅超管可指定（默认 0=系统默认模板），非超管后端强制本租户
export function listPageBlock(roleCode, pageKey, tenantId) {
  return request({
    url: '/micro/page/list',
    method: 'get',
    params: { roleCode: roleCode, pageKey: pageKey, tenantId: tenantId }
  })
}

// 区块详情
export function getPageBlock(id) {
  return request({
    url: '/micro/page/' + id,
    method: 'get'
  })
}

// 批量保存装修（勾选/拖拽排序/设参数 → 整页全量覆盖，先删后插）
export function decoratePage(data) {
  return request({
    url: '/micro/page/decorate',
    method: 'post',
    data: data
  })
}

// 恢复默认（从 tenant_id=0 系统模板克隆到目标租户）
export function resetDefault(roleCode, pageKey, tenantId) {
  return request({
    url: '/micro/page/resetDefault',
    method: 'post',
    params: { roleCode: roleCode, pageKey: pageKey, tenantId: tenantId }
  })
}

// 新增单区块（超管默认写入系统模板 tenant_id=0；老板写入本租户）
export function addPageBlock(data) {
  return request({
    url: '/micro/page',
    method: 'post',
    data: data
  })
}

// 编辑单区块（排版：排序/显隐/参数）
export function updatePageBlock(data) {
  return request({
    url: '/micro/page',
    method: 'put',
    data: data
  })
}

// 删除区块（page_block_config 无 del_flag，物理删除，可批量）
export function delPageBlock(ids) {
  return request({
    url: '/micro/page/' + ids,
    method: 'delete'
  })
}

// 小程序渲染态预览：/page/config 返回 = page_block_config ∩ 当前登录角色 perms（已剔除无权限区块）
export function getPageConfig(pageKey) {
  return request({
    url: '/page/config',
    method: 'get',
    params: { pageKey: pageKey }
  })
}
