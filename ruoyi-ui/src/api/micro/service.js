import request from '@/utils/request'

// 服务项目列表
export function listService(query) {
  return request({
    url: '/micro/service/list',
    method: 'get',
    params: query
  })
}

// 上架服务项目（categoryId 可空=全部，预约/店铺选择用）
export function onSaleService(categoryId) {
  return request({
    url: '/micro/service/onSale',
    method: 'get',
    params: { categoryId }
  })
}

// 服务项目详情
export function getService(id) {
  return request({
    url: '/micro/service/' + id,
    method: 'get'
  })
}

// 新建服务项目
export function addService(data) {
  return request({
    url: '/micro/service',
    method: 'post',
    data: data
  })
}

// 编辑服务项目（价格变更自动写 price_change_log）
export function updateService(data) {
  return request({
    url: '/micro/service',
    method: 'put',
    data: data
  })
}

// 改价确认（更新单价并写 price_change_log）
export function changePrice(id, newPrice, remark) {
  return request({
    url: '/micro/service/changePrice',
    method: 'put',
    params: { id, newPrice, remark }
  })
}

// 上架/下架（status：0上架 1下架）
export function toggleService(id, status) {
  return request({
    url: '/micro/service/toggle/' + id + '/' + status,
    method: 'put'
  })
}

// 删除服务项目
export function delService(ids) {
  return request({
    url: '/micro/service/' + ids,
    method: 'delete'
  })
}
