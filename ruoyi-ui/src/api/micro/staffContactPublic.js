import request from '@/utils/request'

// 员工联系方式公开开关（M34 T34.5，老板 PC 端配置页）
// ★BE 已在 T31.5 交付表/实体/Service；本文件对接 /micro/staff/contactPublic 系列接口

// 列出本租户全部业务员及其开关状态（默认全关）
export function listContactPublic() {
  return request({
    url: '/micro/staff/contactPublic/list',
    method: 'get'
  })
}

// 查询单个员工联系方式公开设置
export function getContactPublic(staffId) {
  return request({
    url: '/micro/staff/contactPublic/' + staffId,
    method: 'get'
  })
}

// 更新员工联系方式公开开关（不存在则新建）
// data: { staffId, showPhone, showWechat, wechatQrcodeUrl }
export function updateContactPublic(data) {
  return request({
    url: '/micro/staff/contactPublic',
    method: 'put',
    data: data
  })
}
