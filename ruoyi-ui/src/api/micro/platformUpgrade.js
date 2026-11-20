import request from '@/utils/request'

// platform_admin 版本升级（T15.33）

// 当前租户版本信息
export function getUpgradeInfo() {
  return request({
    url: '/micro/platform/upgrade/info',
    method: 'get'
  })
}

// 导入 License 密钥升级
export function importLicense(licenseKey) {
  return request({
    url: '/micro/platform/upgrade/license',
    method: 'post',
    data: licenseKey
  })
}

// T14.2 续费下单（V1 零支付：仅生成订单，线下联系管理员）
export function createRenewOrder(planCode) {
  return request({
    url: '/billing/order/create',
    method: 'post',
    data: { planCode }
  })
}
