import request from '@/utils/request'

// 分享场景（T15.3 建表 / T15.19 扫码锁定租户 / T15.24 生成分享海报）
// ★sceneId 全局唯一（uk_scene）：扫码/分享进入时按 sceneId 跨租户反查 tenantId，实现「进入即锁定租户」
// ★save/poster 接口带 @Validated(AddGroup.class)，sceneId 为必填（后端不会自动补），前端须显式生成

// 按 sceneId 反查场景（小程序扫码入口，跨租户忽略 tenant 过滤）
export function resolveScene(sceneId) {
  return request({
    url: '/micro/share/resolve',
    method: 'get',
    params: { sceneId: sceneId }
  })
}

// 分享场景列表（分页，perms 复用 micro:promo:list）
export function listShare(query) {
  return request({
    url: '/micro/share/list',
    method: 'get',
    params: query
  })
}

// 场景详情
export function getShare(id) {
  return request({
    url: '/micro/share/' + id,
    method: 'get'
  })
}

// 新增场景（按 sceneId 幂等 upsert）
export function addShare(data) {
  return request({
    url: '/micro/share',
    method: 'post',
    data: data
  })
}

// 编辑场景（含 id 即更新）
export function updateShare(data) {
  return request({
    url: '/micro/share',
    method: 'put',
    data: data
  })
}

// 生成分享海报：海报带租户号二维码（sceneId 编码 tenantId、qrCodeUrl 为 OSS 海报图），写入 share_scene
export function genSharePoster(data) {
  return request({
    url: '/micro/share/poster',
    method: 'post',
    data: data
  })
}

// 删除场景（share_scene 无 del_flag，物理删除，可批量）
export function delShare(ids) {
  return request({
    url: '/micro/share/' + ids,
    method: 'delete'
  })
}
