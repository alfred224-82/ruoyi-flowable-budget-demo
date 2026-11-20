import request from '@/utils/request'

// 价格调整记录（T15.4 / T15.11 ④ 改价历史查询）
// ★只读模块：改价记录由「服务项目编辑 / 改价确认」时后端自动写入，无新增/修改/删除入口
// ★PriceChangeLogBo 的 beginTime/endTime 为顶层 java.util.Date 字段，项目未配置 String→Date
//   查询参数转换器（spring.mvc.format.date-time 只作用于 java.time.*），故列表页不提供日期范围筛选，
//   改价时间过滤请通过 itemId 缩小范围后在表格内查看（change_time 已倒序）

// 改价记录列表（分页，change_time 倒序）
export function listPriceLog(query) {
  return request({
    url: '/micro/priceLog/list',
    method: 'get',
    params: query
  })
}

// 指定服务项目的改价历史（服务项目详情页内嵌「价格轨迹」用）
export function listPriceLogByItem(itemId) {
  return request({
    url: '/micro/priceLog/item/' + itemId,
    method: 'get'
  })
}
