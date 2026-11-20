import request from '@/utils/request'

// 店铺管理（T15.24）：店铺信息 / 营业时间 / 收款方式说明
// 配置落地在 tenant_biz_config.module_overrides（单店覆盖），不影响同业态其他租户
// ★V1 零支付：payInfo 仅为「到店付 / 老板收款码」提示文案，不绑定任何支付账户

// 读取店铺配置（businessHours / payInfo / notifyConfig）
export function getShopConfig() {
  return request({
    url: '/micro/shop/config',
    method: 'get'
  })
}

// 保存店铺配置（三项一次性覆盖写入）
export function saveShopConfig(data) {
  return request({
    url: '/micro/shop/config',
    method: 'put',
    data: data
  })
}

// 单独保存营业时间（perms micro:shop:hours）
export function saveShopHours(businessHours) {
  return request({
    url: '/micro/shop/hours',
    method: 'put',
    data: { businessHours: businessHours }
  })
}

// 单独保存收款方式说明（perms micro:shop:payInfo，★仅提示文案）
export function saveShopPayInfo(payInfo) {
  return request({
    url: '/micro/shop/payInfo',
    method: 'put',
    data: { payInfo: payInfo }
  })
}
