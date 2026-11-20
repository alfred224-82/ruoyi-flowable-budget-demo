import request from '@/utils/request'

// 资源状态管理（T21.5/T21.6，Boss PC 员工/房间状态管理 + 手动释放 + 日志查看）
// ★类级 @RequireFeature("FEAT_SELF_BOOKING")，与自助预约共用同一功能开关
// ★员工显示名按租户角色映射（utils/roleDisplay），无映射回退通用名「员工」

// 房间资源状态列表（space 分类 service_item，含 resource_status）
export function listRoomStatus() {
  return request({
    url: '/micro/resourceStatus/room/list',
    method: 'get'
  })
}

// 员工资源状态列表（salesman 角色用户 + 最新 resource_status_log 推导状态）
export function listStaffStatus() {
  return request({
    url: '/micro/resourceStatus/staff/list',
    method: 'get'
  })
}

// 手动释放资源（reserved → idle + 写 resource_status_log）
// resourceType: staff=员工 / room=房间
export function manualRelease(resourceType, resourceId, reason) {
  return request({
    url: '/micro/resourceStatus/release',
    method: 'put',
    params: { resourceType, resourceId, reason }
  })
}

// 手动设置资源状态（写 resource_status_log；房间同步更新 service_item.resource_status）
// resourceType: staff=员工 / room=房间
// targetStatus: 员工=idle(空闲)/working(工作)/resting(休息)；房间=idle(空闲)/in_use(使用)
// ★仅 idle 状态的资源可被顾客预约
export function setResourceStatus(resourceType, resourceId, targetStatus, reason) {
  return request({
    url: '/micro/resourceStatus/set',
    method: 'put',
    params: { resourceType, resourceId, targetStatus, reason }
  })
}

// 资源状态变更日志（最近 50 条）
export function listResourceStatusLogs(query) {
  return request({
    url: '/micro/resourceStatus/log/list',
    method: 'get',
    params: query
  })
}
