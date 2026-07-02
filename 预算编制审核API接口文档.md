# 预算编制与审核 API 接口文档

## 基础信息

- **Base URL**: `http://localhost:8080/system/preparation`
- **认证方式**: Bearer Token (Sa-Token)
- **Content-Type**: `application/json`

---

## 1. 预算编制主表接口

### 1.1 查询预算编制列表

**接口地址**: `GET /system/preparation/list`

**权限标识**: `system:preparation:list`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认1 | 1 |
| pageSize | Integer | 否 | 每页条数，默认10 | 10 |
| budgetYear | Integer | 否 | 预算年度 | 2026 |
| budgetMonth | Integer | 否 | 预算月份(1-12) | 6 |
| status | String | 否 | 状态(Draft/Pending_Review/Approved/Rejected) | Draft |
| orgId | Long | 否 | 组织ID（部门权限控制） | 100 |
| sheetNo | String | 否 | 预算单号（模糊查询） | BG-202606 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "查询成功",
  "rows": [
    {
      "id": 1,
      "sheetNo": "BG-202606-001",
      "orgId": 100,
      "orgName": "财务部",
      "budgetYear": 2026,
      "budgetMonth": 6,
      "budgetPeriod": "2026-06",
      "status": "Draft",
      "rejectLevel": "None",
      "rejectReason": null,
      "currentHandler": "admin",
      "totalBudget": 100000.00,
      "totalActual": 0.00,
      "varianceRate": 0.00,
      "processInstanceId": null,
      "remark": null,
      "createBy": "admin",
      "createTime": "2026-06-19 10:00:00",
      "updateBy": null,
      "updateTime": null
    }
  ],
  "total": 1
}
```

---

### 1.2 获取预算编制详情

**接口地址**: `GET /system/preparation/{id}`

**权限标识**: `system:preparation:query`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 预算编制ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "sheetNo": "BG-202606-001",
    "orgId": 100,
    "orgName": "财务部",
    "budgetYear": 2026,
    "budgetMonth": 6,
    "budgetPeriod": "2026-06",
    "status": "Draft",
    "totalBudget": 100000.00,
    ...
  }
}
```

---

### 1.3 新增预算编制

**接口地址**: `POST /system/preparation`

**权限标识**: `system:preparation:add`

**防重复提交**: ✅ 是

**请求体**:
```json
{
  "orgId": 100,
  "orgName": "财务部",
  "budgetYear": 2026,
  "budgetMonth": 6,
  "remark": "6月份预算编制"
}
```

**字段说明**:
| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orgId | Long | 是 | 组织ID |
| orgName | String | 否 | 组织名称 |
| budgetYear | Integer | 是 | 预算年度 |
| budgetMonth | Integer | 是 | 预算月份(1-12) |
| remark | String | 否 | 备注 |

**注意**: 
- `sheetNo` 会自动生成（格式：BG-YYYYMM-XXX）
- `budgetPeriod` 会自动生成（格式：YYYY-MM）
- `status` 默认为 "Draft"
- `totalBudget`、`totalActual`、`varianceRate` 默认为 0

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 1.4 修改预算编制

**接口地址**: `PUT /system/preparation`

**权限标识**: `system:preparation:edit`

**防重复提交**: ✅ 是

**请求体**:
```json
{
  "id": 1,
  "orgId": 100,
  "orgName": "财务部",
  "budgetYear": 2026,
  "budgetMonth": 6,
  "remark": "修改后的备注"
}
```

**限制**:
- 只有 `Draft` 和 `Rejected` 状态的预算编制可以修改
- 其他状态会抛出异常

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 1.5 删除预算编制

**接口地址**: `DELETE /system/preparation/{ids}`

**权限标识**: `system:preparation:remove`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | Long[] | 是 | ID数组，支持批量删除 |

**限制**:
- 只有 `Draft` 和 `Rejected` 状态的预算编制可以删除

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 1.6 提交审核（单个）

**接口地址**: `POST /system/preparation/submit/{id}`

**权限标识**: `system:preparation:submit`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 预算编制ID |

**业务逻辑**:
- 校验状态必须是 `Draft` 或 `Rejected`
- 更新状态为 `Pending_Review`
- 清空驳回信息

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

**错误响应**:
```json
{
  "code": 500,
  "msg": "预算编制状态不是草稿或驳回，不能提交审核"
}
```

---

### 1.7 批量提交审核

**接口地址**: `POST /system/preparation/batchSubmit`

**权限标识**: `system:preparation:submit`

**请求体**:
```json
[1, 2, 3, 4, 5]
```

**说明**: 传入ID数组，对每个ID执行提交审核操作

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 1.8 审批通过（单个）

**接口地址**: `POST /system/preparation/approve/{id}`

**权限标识**: `system:preparation:approve`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 预算编制ID |

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| remark | String | 否 | 审批意见 |

**完整URL示例**: `POST /system/preparation/approve/1?remark=同意`

**业务逻辑**:
- 校验状态必须是 `Pending_Review`
- 更新状态为 `Approved`
- 保存审批意见到 remark 字段

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 1.9 批量审批通过

**接口地址**: `POST /system/preparation/batchApprove`

**权限标识**: `system:preparation:approve`

**请求体**:
```json
{
  "ids": [1, 2, 3],
  "remark": "同意"
}
```

**字段说明**:
| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | Long[] | 是 | 预算编制ID数组 |
| remark | String | 否 | 审批意见 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 1.10 审批驳回

**接口地址**: `POST /system/preparation/reject/{id}`

**权限标识**: `system:preparation:reject`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 预算编制ID |

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reason | String | **是** | 驳回理由（必填） |

**完整URL示例**: `POST /system/preparation/reject/1?reason=预算金额过高，请重新调整`

**业务逻辑**:
- 校验状态必须是 `Pending_Review`
- **校验驳回理由不能为空**
- 更新状态为 `Rejected`
- 设置 `rejectLevel` 为 "HQ"
- 保存驳回理由到 `rejectReason`
- 设置当前处理人为操作人

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

**错误响应**（未填写驳回理由）:
```json
{
  "code": 500,
  "msg": "驳回时必须填写驳回意见"
}
```

---

### 1.11 获取校验结果

**接口地址**: `GET /system/preparation/validation/{sheetId}`

**权限标识**: `system:preparation:query`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sheetId | Long | 是 | 预算单ID |

**响应示例**（待实现）:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "valid": true,
    "messages": []
  }
}
```

---

## 2. 预算编制明细接口

### 2.1 查询预算编制明细列表

**接口地址**: `GET /system/preparation/detail/list/{sheetId}`

**权限标识**: `system:preparation:query`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sheetId | Long | 是 | 预算单ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "sheetId": 1,
      "subjectCode": "6001",
      "subjectName": "主营业务收入",
      "subjectType": "INCOME",
      "budgetAmount": 50000.00,
      "actualAmount": 0.00,
      "varianceAmount": 50000.00,
      "varianceRate": 100.00,
      "deptId": 100,
      "deptName": "销售部",
      "sortOrder": 1,
      "createTime": "2026-06-19 10:00:00",
      "updateTime": null
    },
    {
      "id": 2,
      "sheetId": 1,
      "subjectCode": "6601",
      "subjectName": "销售费用",
      "subjectType": "EXPENSE",
      "budgetAmount": 10000.00,
      "actualAmount": 0.00,
      "varianceAmount": 10000.00,
      "varianceRate": 100.00,
      "deptId": 100,
      "deptName": "销售部",
      "sortOrder": 2,
      "createTime": "2026-06-19 10:00:00",
      "updateTime": null
    }
  ]
}
```

**排序规则**: 按 `sortOrder` 升序排列

---

### 2.2 保存预算编制明细（单条）

**接口地址**: `POST /system/preparation/detail`

**权限标识**: `system:preparation:edit`

**防重复提交**: ✅ 是

**请求体**（新增）:
```json
{
  "sheetId": 1,
  "subjectCode": "6001",
  "subjectName": "主营业务收入",
  "subjectType": "INCOME",
  "budgetAmount": 50000.00,
  "actualAmount": 0.00,
  "deptId": 100,
  "deptName": "销售部",
  "sortOrder": 1
}
```

**请求体**（修改）:
```json
{
  "id": 1,
  "sheetId": 1,
  "subjectCode": "6001",
  "subjectName": "主营业务收入",
  "subjectType": "INCOME",
  "budgetAmount": 55000.00,
  "actualAmount": 0.00,
  "deptId": 100,
  "deptName": "销售部",
  "sortOrder": 1
}
```

**字段说明**:
| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 修改时必填 | 明细ID |
| sheetId | Long | 是 | 预算单ID |
| subjectCode | String | 是 | 科目编码 |
| subjectName | String | 否 | 科目名称 |
| subjectType | String | 否 | 科目类型(INCOME/EXPENSE/ASSET/LIABILITY/EQUITY/COST) |
| budgetAmount | BigDecimal | 否 | 预算金额（>=0） |
| actualAmount | BigDecimal | 否 | 实际金额（>=0） |
| deptId | Long | 否 | 部门ID |
| deptName | String | 否 | 部门名称 |
| sortOrder | Integer | 否 | 排序号 |

**自动计算**:
- `varianceAmount` = `budgetAmount` - `actualAmount`
- `varianceRate` = (`varianceAmount` / `budgetAmount`) × 100%
- 当 `budgetAmount` = 0 时，`varianceRate` = 0

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 2.3 批量保存预算编制明细

**接口地址**: `POST /system/preparation/detail/batch`

**权限标识**: `system:preparation:edit`

**防重复提交**: ✅ 是

**请求体**:
```json
[
  {
    "sheetId": 1,
    "subjectCode": "6001",
    "subjectName": "主营业务收入",
    "subjectType": "INCOME",
    "budgetAmount": 50000.00,
    "actualAmount": 0.00,
    "deptId": 100,
    "deptName": "销售部",
    "sortOrder": 1
  },
  {
    "sheetId": 1,
    "subjectCode": "6601",
    "subjectName": "销售费用",
    "subjectType": "EXPENSE",
    "budgetAmount": 10000.00,
    "actualAmount": 0.00,
    "deptId": 100,
    "deptName": "销售部",
    "sortOrder": 2
  },
  {
    "sheetId": 1,
    "subjectCode": "6602",
    "subjectName": "管理费用",
    "subjectType": "EXPENSE",
    "budgetAmount": 8000.00,
    "actualAmount": 0.00,
    "deptId": 101,
    "deptName": "行政部",
    "sortOrder": 3
  }
]
```

**业务逻辑**:
1. 校验所有明细的 `sheetId` 必须一致且不为空
2. 删除该预算单的所有旧明细数据
3. 批量插入新的明细数据
4. 每条明细自动计算差异金额和差异率
5. 整个操作在事务中执行，保证数据一致性

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

## 3. 状态流转图

```
Draft (草稿)
   ↓ 提交审核
Pending_Review (待审核)
   ↓ 审批通过         ↓ 审批驳回
Approved (已通过)    Rejected (已驳回)
                      ↓ 修改后再次提交
                   Pending_Review (待审核)
```

**状态说明**:
- `Draft`: 草稿状态，可编辑、删除、提交
- `Pending_Review`: 待审核状态，可审批通过或驳回
- `Approved`: 已通过状态，最终状态，不可再操作
- `Rejected`: 已驳回状态，可重新编辑后再提交

---

## 4. 常见错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 401 | 未授权，需要登录 |
| 403 | 无权限访问 |
| 500 | 服务器内部错误（业务异常信息在 msg 中） |

**常见业务错误消息**:
- "预算编制不存在"
- "预算编制状态不是草稿或驳回，不能提交审核"
- "预算编制状态不是待审核，不能审批"
- "预算编制状态不是待审核，不能驳回"
- "驳回时必须填写驳回意见"
- "只有草稿和驳回状态的预算编制可以修改"
- "预算单号[XXX]不是草稿或驳回状态，不能删除"
- "请选择要提交的预算编制"
- "请选择要审批的预算编制"
- "预算单ID不能为空"

---

## 5. 权限标识汇总

| 权限标识 | 说明 | 对应接口 |
|---------|------|---------|
| system:preparation:list | 查询列表 | GET /list |
| system:preparation:query | 查询详情 | GET /{id}, GET /detail/list/{sheetId}, GET /validation/{sheetId} |
| system:preparation:add | 新增 | POST / |
| system:preparation:edit | 修改 | PUT /, POST /detail, POST /detail/batch |
| system:preparation:remove | 删除 | DELETE /{ids} |
| system:preparation:submit | 提交审核 | POST /submit/{id}, POST /batchSubmit |
| system:preparation:approve | 审批通过 | POST /approve/{id}, POST /batchApprove |
| system:preparation:reject | 审批驳回 | POST /reject/{id} |
| system:preparation:export | 导出 | POST /export |

---

## 6. 数据字典

### 6.1 预算编制状态 (status)

| 值 | 说明 |
|----|------|
| Draft | 草稿 |
| Pending_Review | 待审核 |
| Approved | 已通过 |
| Rejected | 已驳回 |

### 6.2 驳回来源 (rejectLevel)

| 值 | 说明 |
|----|------|
| None | 无 |
| HQ | 总部驳回 |
| Branch | 分公司驳回 |

### 6.3 科目类型 (subjectType)

| 值 | 说明 |
|----|------|
| INCOME | 收入类 |
| COST | 成本类 |
| EXPENSE | 费用类 |
| ASSET | 资产类 |
| LIABILITY | 负债类 |
| EQUITY | 权益类 |

---

## 7. 使用示例（JavaScript）

```javascript
import request from '@/utils/request'

// 1. 查询预算编制列表
export function listPreparation(query) {
  return request({
    url: '/system/preparation/list',
    method: 'get',
    params: query
  })
}

// 2. 提交审核
export function submitPreparation(id) {
  return request({
    url: '/system/preparation/submit/' + id,
    method: 'post'
  })
}

// 3. 批量审批
export function batchApprove(data) {
  return request({
    url: '/system/preparation/batchApprove',
    method: 'post',
    data: data
  })
}

// 4. 审批驳回
export function rejectPreparation(id, reason) {
  return request({
    url: '/system/preparation/reject/' + id,
    method: 'post',
    params: { reason }
  })
}

// 5. 批量保存明细
export function batchSavePreparationDetail(data) {
  return request({
    url: '/system/preparation/detail/batch',
    method: 'post',
    data: data
  })
}
```

**调用示例**:
```javascript
// 查询列表
listPreparation({
  pageNum: 1,
  pageSize: 10,
  budgetYear: 2026,
  budgetMonth: 6,
  status: 'Draft'
}).then(response => {
  console.log(response.rows)
})

// 提交审核
submitPreparation(1).then(() => {
  this.$modal.msgSuccess('提交成功')
})

// 批量审批
batchApprove({
  ids: [1, 2, 3],
  remark: '同意'
}).then(() => {
  this.$modal.msgSuccess('批量审批成功')
})

// 审批驳回
rejectPreparation(1, '预算金额过高').then(() => {
  this.$modal.msgSuccess('驳回成功')
})

// 批量保存明细
batchSavePreparationDetail([
  {
    sheetId: 1,
    subjectCode: '6001',
    subjectName: '主营业务收入',
    subjectType: 'INCOME',
    budgetAmount: 50000.00,
    actualAmount: 0.00,
    sortOrder: 1
  }
]).then(() => {
  this.$modal.msgSuccess('保存成功')
})
```

---

## 8. Postman 测试集合

可以导入以下 JSON 到 Postman：

```json
{
  "info": {
    "name": "预算编制与审核API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. 查询预算编制列表",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/system/preparation/list?pageNum=1&pageSize=10&budgetYear=2026&budgetMonth=6",
          "host": ["{{baseUrl}}"],
          "path": ["system", "preparation", "list"],
          "query": [
            {"key": "pageNum", "value": "1"},
            {"key": "pageSize", "value": "10"},
            {"key": "budgetYear", "value": "2026"},
            {"key": "budgetMonth", "value": "6"}
          ]
        }
      }
    },
    {
      "name": "2. 提交审核",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/system/preparation/submit/1",
          "host": ["{{baseUrl}}"],
          "path": ["system", "preparation", "submit", "1"]
        }
      }
    },
    {
      "name": "3. 批量审批",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"ids\": [1, 2, 3],\n  \"remark\": \"同意\"\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/system/preparation/batchApprove",
          "host": ["{{baseUrl}}"],
          "path": ["system", "preparation", "batchApprove"]
        }
      }
    },
    {
      "name": "4. 审批驳回",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/system/preparation/reject/1?reason=预算金额过高",
          "host": ["{{baseUrl}}"],
          "path": ["system", "preparation", "reject", "1"],
          "query": [
            {"key": "reason", "value": "预算金额过高"}
          ]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    },
    {
      "key": "token",
      "value": "your_token_here"
    }
  ]
}
```

---

## 总结

本文档涵盖了预算编制与审核功能的所有 API 接口，包括：
- ✅ 11个预算编制主表接口
- ✅ 3个预算编制明细接口
- ✅ 完整的请求/响应示例
- ✅ 详细的字段说明
- ✅ 业务规则和限制
- ✅ 错误处理
- ✅ 权限控制
- ✅ 使用示例

前端开发人员可以根据此文档进行接口对接，后端开发人员可以根据此文档进行接口测试和维护。
