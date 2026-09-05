import request from '@/utils/request'

// 查询财务科目列表（含映射A结果）
export function listFinanceSubject(query) {
  return request({
    url: '/fee/financeSubject/list',
    method: 'get',
    params: query
  })
}

// 关键字自动匹配（对所有未映射科目建立映射A）
export function autoMatchFinanceSubject() {
  return request({
    url: '/fee/financeSubject/match',
    method: 'post'
  })
}

// 人工补完/纠偏映射A
export function mapFinanceSubject(id, mappedSubjectCode) {
  return request({
    url: '/fee/financeSubject/map/' + id,
    method: 'post',
    params: { mappedSubjectCode }
  })
}

// 忽略科目（不参与映射）
export function ignoreFinanceSubject(id) {
  return request({
    url: '/fee/financeSubject/ignore/' + id,
    method: 'put'
  })
}

// 删除财务科目
export function delFinanceSubject(ids) {
  return request({
    url: '/fee/financeSubject/' + ids,
    method: 'delete'
  })
}

// 下载导入模板
export function importTemplate() {
  return request({
    url: '/fee/financeSubject/importTemplate',
    method: 'post'
  })
}
