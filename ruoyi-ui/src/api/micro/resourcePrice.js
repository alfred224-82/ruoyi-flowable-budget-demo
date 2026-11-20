import request from '@/utils/request'

// 资源价格设置（T21.7，Boss PC 人员定价，支持 4 种计价模式）
// ★类级 @RequireFeature("FEAT_SELF_BOOKING")，与自助预约共用同一功能开关
// ★人员定价对齐 jjadmin 四档计价模式（计时/套餐/计量/一口价）
// ★房间定价由「房间/空间管理」(platform/room) 统一管理，本接口不再涉及
// ★人员显示名按租户角色映射（utils/roleDisplay），无映射回退通用名「员工」

// 查询本租户全部人员价格列表（回填昵称/头像 + 计价模式）
export function listStaffPricing() {
  return request({
    url: '/micro/resourcePrice/staff/list',
    method: 'get'
  })
}

// 设置/更新人员价格（upsert：同一人员仅一条记录，支持 4 种计价模式）
export function saveStaffPrice(data) {
  return request({
    url: '/micro/resourcePrice/staff/save',
    method: 'post',
    data: data
  })
}
