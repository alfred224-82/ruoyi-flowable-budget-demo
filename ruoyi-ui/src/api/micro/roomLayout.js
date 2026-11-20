import request from '@/utils/request'

// 房间座位布局管理 API（M26 T26.3~T26.5，Boss PC 端）
// ★菜单：7280（房间布局配置 C）+ 7281（上传背景图 F）+ 7282（复制布局 F）+ 7283（存为模板 F）
// ★功能开关：FEAT_ROOM_SEAT_LAYOUT（micro_pro 专属，micro_lite 不启用）
// ★坐标：以背景图百分比 0~1 存储，PC/手机/平板三端渲染一致

// ============================================================
// T26.3 背景图上传（前置校验格式/大小/尺寸后落 OSS）
// ============================================================

/**
 * 上传房间背景图
 * @param {File} file 图片文件（JPG/PNG/WebP，≤2MB，≤1920×1080）
 * @returns {Promise<{url, fileName, ossId}>}
 */
export function uploadBgImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/micro/room/uploadBgImage',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: formData
  })
}

// ============================================================
// T26.4 布局查询 / 保存 / 删除（拖拽定位）
// ============================================================

/**
 * 查询房间布局（含座位点批量列表）
 * @param {number} roomId 房间ID（FK→service_item.id）
 * @returns {Promise<RoomSeatLayoutVo>} data=null 表示未配置布局
 */
export function getRoomLayout(roomId) {
  return request({
    url: '/micro/room/layout/' + roomId,
    method: 'get'
  })
}

/**
 * 保存布局（新增/编辑；一次事务写入 room_seat_layout + room_seat 全量替换）
 * ★保存成功后同步刷新 service_item.has_seat_layout=1 + seat_capacity=座位数（T26.2 联动）
 * @param {RoomSeatLayoutBo} data { roomId, bgImageUrl, bgImageSource, layoutWidth, layoutHeight, seats[] }
 */
export function saveRoomLayout(data) {
  return request({
    url: '/micro/room/layout/save',
    method: 'post',
    data: data
  })
}

/**
 * 删除房间布局（连带删除座位点，房间回退 has_seat_layout=0）
 * @param {number} layoutId
 */
export function deleteRoomLayout(layoutId) {
  return request({
    url: '/micro/room/layout/' + layoutId,
    method: 'delete'
  })
}

// ============================================================
// T26.5 复制布局 / 存为模板 / 应用模板 / 模板列表
// ============================================================

/**
 * 从已有房间复制布局（脱钩原则：目标房间拥有独立布局记录）
 * @param {{sourceRoomId, targetRoomId}} data
 */
export function copyLayoutFromRoom(data) {
  return request({
    url: '/micro/room/layout/copyFromRoom',
    method: 'post',
    data: data
  })
}

/**
 * 存为模板（Boss 私有模板，is_industry_default=0）
 * ★UNIQUE(tenant_id, name) 兜底：同租户下模板名唯一
 * @param {{layoutId, templateName}} data
 */
export function saveLayoutAsTemplate(data) {
  return request({
    url: '/micro/room/layout/saveAsTemplate',
    method: 'post',
    data: data
  })
}

/**
 * 查询 Boss 可见的模板列表（本租户私有 + 平台行业默认）
 * @param {string} [industryCode] 行业编码（可空；传入时按行业过滤平台默认模板）
 */
export function listVisibleTemplates(industryCode) {
  return request({
    url: '/micro/room/layout/templates',
    method: 'get',
    params: { industryCode }
  })
}

/**
 * 应用模板到指定房间
 * ★应用后 room_seat_layout.template_id 记录模板ID（留痕），后续修改与模板脱钩
 * @param {number} roomId 目标房间ID
 * @param {number} templateId 模板ID
 */
export function applyRoomTemplate(roomId, templateId) {
  return request({
    url: '/micro/room/layout/applyTemplate',
    method: 'post',
    params: { roomId, templateId }
  })
}
