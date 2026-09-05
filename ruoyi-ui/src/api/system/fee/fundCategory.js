import request from '@/utils/request'

// 费用大类列表
export function listCategory() {
  return request({
    url: '/fee/category/list',
    method: 'get'
  })
}

// 启用的费用大类（编制/报销下拉用）
export function listActiveCategory() {
  return request({
    url: '/fee/category/activeList',
    method: 'get'
  })
}

// 费用大类详情
export function getCategory(id) {
  return request({
    url: '/fee/category/' + id,
    method: 'get'
  })
}

// 新增费用大类
export function addCategory(data) {
  return request({
    url: '/fee/category',
    method: 'post',
    data: data
  })
}

// 修改费用大类
export function updateCategory(data) {
  return request({
    url: '/fee/category',
    method: 'put',
    data: data
  })
}

// 删除费用大类（内置不可删）
export function delCategory(id) {
  return request({
    url: '/fee/category/' + id,
    method: 'delete'
  })
}

// 查询大类默认拆分比例
export function listCategoryRatio(categoryId) {
  return request({
    url: '/fee/category/ratio/' + categoryId,
    method: 'get'
  })
}

// 保存大类默认拆分比例（合计=100%）
export function saveCategoryRatio(categoryId, data) {
  return request({
    url: '/fee/category/ratio/' + categoryId,
    method: 'post',
    data: data
  })
}

// 全部启用预算科目（拆分比例选择用，映射B由科目表自带）
export function listAllSubject() {
  return request({
    url: '/system/subject/all',
    method: 'get'
  })
}
