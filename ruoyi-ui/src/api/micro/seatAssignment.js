import request from '@/utils/request'

// 活动座位编排 API（M26 / M28 / T26.10，Boss PC 端）
// ★菜单权限：micro:seat:assign（7289，Boss 编排）
// ★坐标：座位点以背景图百分比 0~1 存储，PC/手机/平板三端渲染一致

// ============================================================
// 座位图 / 占座名单（查看）
// ============================================================

/**
 * 查询活动座位图（背景图 + 座位三态 FREE/OCCUPIED/PRESET + 统计）
 * @param {number|string} activityId
 */
export function getSeatMap(activityId) {
  return request({
    url: '/micro/seat/map/' + activityId,
    method: 'get'
  })
}

/**
 * 查询活动座位占用列表（含用户昵称/头像/角色回填）
 * @param {number|string} activityId
 */
export function listSeatAssignments(activityId) {
  return request({
    url: '/micro/seat/assignments/' + activityId,
    method: 'get'
  })
}

// ============================================================
// T26.10 背景画布加人（发布/编辑活动弹窗）
// ============================================================

/**
 * 画布加人候选人（按角色分组：员工=导游/司机 + 顾客=游客，含头像）
 * @returns {Promise<SeatCandidateVo[]>} [{ roleCode, roleLabel, persons:[{userId,name,avatar}] }]
 */
export function listSeatCandidates() {
  return request({
    url: '/micro/seat/candidates',
    method: 'get'
  })
}

/**
 * 背景画布加人批量保存（全量替换本活动 PRESET/BOSS_ASSIGN，顾客自选保留）
 * @param {{activityId, items:[{seatId, seatNo, userId, remark}]}} data
 */
export function saveCanvasPersons(data) {
  return request({
    url: '/micro/seat/saveCanvasPersons',
    method: 'post',
    data: data
  })
}
