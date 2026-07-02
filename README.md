# 企业全面预算管理系统 — 需求说明书

**版本**：v1.0  
**日期**：2026-07-02  
**项目**：基于 RuoYi-Flowable-Plus 框架的企业级预算管理解决方案

---

## 一、项目概述

### 1.1 项目背景

本系统是基于 RuoYi-Flowable-Plus 框架开发的企业级全面预算管理平台，采用「上月实绩驱动下月预算」的核心业务模式，支持总公司、分公司、部门三级组织架构的协同工作。

### 1.2 核心价值

- **流程自动化**：通过 Flowable 工作流引擎实现预算审批流程自动化
- **多级管控**：总公司→分公司→部门的多层级预算管控体系
- **智能风控**：内置风控规则引擎，自动识别异常数据
- **数据可视化**：报表与图表展示能力
- **安全合规**：敏感数据脱敏、操作日志追溯、水印保护

---

## 二、业务模型

### 2.1 财务周期与时间节点

| 阶段 | 时间范围 | 核心动作 | 责任主体 |
|------|----------|----------|----------|
| 编制期 | 本月26日 - 次月3日 | 部门编制→分公司汇总→提交总公司 | 部门/分公司编制人员 |
| 总公司审核期 | 次月5日 - 次月6日 | 全辖预算审核 | 总公司审核员 |
| 分公司处理期 | 次月7日 00:00-12:00 | 处理驳回/自行修改 | 分公司编制人员 |
| 部门重编期 | 次月7日 12:00-18:00 | 接收打回后重新编制 | 部门编制人员 |
| 执行期 | 次月8日 00:00起 | 预算锁定，正式执行 | 系统自动 |

### 2.2 权限模型

#### 角色权限矩阵

| 角色 | 编制权限 | 审核权限 | 对账单查看 | 打回权限 | 数据可见范围 |
|------|---------|---------|-----------|---------|-------------|
| 总公司本部 | 全辖编制 | 全辖审批 | 可见 | 可打回至分公司本部 | 全辖数据 |
| 分公司本部 | 分公司汇总编制 | 分公司审批 | 可见 | 可打回至部门/自行重编 | 分公司及下属部门 |
| 部门编制人员 | 本部门编制 | 无 | 不可见 | 仅接收打回任务 | 本部门（实际金额脱敏） |
| 部门经理 | 无 | 本部门审批 | 不可见 | 无 | 本部门（实际金额脱敏） |

#### 数据脱敏规则

| 数据字段 | 总公司 | 分公司 | 部门 |
|---------|--------|--------|------|
| budget_amount | 完整显示 | 完整显示 | 完整显示 |
| actual_amount | 完整显示 | 部分脱敏（整数位`***`） | 完全脱敏（`***`） |
| variance_rate | 完整显示 | 完整显示 | 隐藏 |

### 2.3 状态流转模型

#### 状态定义

| 状态码 | 名称 | 颜色 | 说明 |
|--------|------|------|------|
| Draft | 草稿 | 绿色 | 可编辑，提交后变为待审核 |
| Pending_Review | 待审核 | 蓝色 | 等待总公司审核 |
| Branch_Pending | 分公司待处理 | 橙色 | 总公司驳回后，分公司可自行修改或打回部门 |
| Pending_Revision | 待重新编制 | 黄色 | 分公司打回部门后，部门修改后重新提交 |
| Approved | 审核通过 | 灰色 | 预算锁定，不可修改 |

#### 状态流转规则

```
Draft → Pending_Review（提交审核）
Pending_Review → Approved（总公司通过）
Pending_Review → Branch_Pending（总公司驳回）
Branch_Pending → Draft（分公司自行修改后重新提交）
Branch_Pending → Pending_Revision（分公司打回部门）
Pending_Revision → Draft（部门修改后重新提交）
```

---

## 三、功能需求

### 3.1 预算编制模块

#### 3.1.1 向导式编制流程

采用3步向导式表单：

| 步骤 | 内容 | 说明 |
|------|------|------|
| 第1步 | 基础信息 | 年度、月份、组织、编制人 |
| 第2步 | 科目预算明细 | 按6种科目类型分Tab录入预算金额 |
| 第3步 | 确认提交 | 汇总展示、明细预览、提交 |

**编辑模式特殊行为**：
- 修改已有记录时，直接进入第2步（科目预算明细），不可返回第1步
- 查看模式（审批中记录）直接进入第3步，只读展示

**重复校验规则**：
- 新建编制点击第1步「下一步」时，自动校验同一部门在同一月份是否已存在编制记录
- 若已存在，弹出警告提示并阻止进入下一步，不保存数据
- 编辑模式下校验时排除当前记录自身

#### 3.1.2 科目类型分Tab

按6种科目类型分Tab显示：

| 类型代码 | 中文名称 | 示例科目 |
|---------|---------|---------|
| INCOME | 收入类 | 营业收入、投资收益 |
| COST | 成本类 | 营业成本、直接材料 |
| EXPENSE | 费用类 | 销售费用、管理费用 |
| ASSET | 资产类 | 流动资产、固定资产 |
| LIABILITY | 负债类 | 流动负债、长期负债 |
| EQUITY | 权益类 | 实收资本、未分配利润 |

#### 3.1.3 编制功能清单

- 新建预算编制（自动生成单号 BG-YYYYMM-XXX）
- 编辑草稿/已驳回状态的编制（直接进入科目预算步骤）
- 删除草稿/已驳回状态的编制
- 提交审核（单个/批量）
- 自动计算差异金额和差异率
- 预算总额自动汇总（第2步完成后更新到编制主表）
- 部门+月份重复校验（防止同一部门同一月份重复创建）

### 3.2 预算审核模块

#### 3.2.1 审核列表

- 默认显示待审核状态记录
- 按角色自动过滤部门范围
- 支持年度、月份、部门筛选
- 支持批量审批

#### 3.2.2 审核详情页

- 基础信息展示（el-descriptions）
- 校验结果展示（风控警告）
- 科目预算明细（按科目类型分Tab）
- 审核操作（通过/驳回）
- 驳回理由必填（5-500字符）

#### 3.2.3 多级打回机制

**总公司驳回分公司**：
- 状态变为 Branch_Pending
- 截止时间：次月7日 12:00
- 分公司可选：自行修改 或 打回部门

**分公司打回部门**：
- 状态变为 Pending_Revision
- 截止时间：次月7日 18:00
- 部门修改后重新提交

### 3.3 智能风控

#### 风控规则

| 规则类别 | 校验项 | 阈值 | 风险等级 |
|---------|-------|------|---------|
| 预算超标 | 部门预算 vs 上月实际 | 偏差>±20% | 高危 |
| 汇总逻辑 | 明细合计 vs 汇总金额 | 不相等 | 高危 |
| 科目归类 | 科目编码有效性 | 不存在于科目表 | 中危 |
| 关键指标 | 必填字段完整性 | 存在空值 | 中危 |
| 历史对比 | 本期 vs 上期同期 | 波动>±30% | 低危 |

#### 校验规则清单（7条）

| 编码 | 名称 | 类别 | 阈值 | 范围 | 严格 |
|------|------|------|------|------|------|
| VAL_001 | 预算超标检查 | BUDGET_OVER | 20% | ALL | 否 |
| VAL_002 | 汇总逻辑检查 | LOGIC_ERROR | - | ALL | 是 |
| VAL_003 | 科目有效性检查 | SUBJECT_ERROR | - | ALL | 是 |
| VAL_004 | 必填字段完整性 | MISSING_DATA | - | ALL | 是 |
| VAL_005 | 历史数据对比 | HISTORICAL_COMPARE | 30% | EXPENSE | 否 |
| VAL_006 | 收入类公式校验 | FORMULA_CHECK | - | INCOME | 否 |
| VAL_007 | 费用合理性检查 | BUDGET_OVER | 50% | EXPENSE | 否 |

### 3.4 邮件通知

#### 邮件模板（3类）

| 模板 | 触发场景 | 关键内容 |
|------|---------|---------|
| 驳回通知 | 总公司驳回 | 驳回理由、截止时间(次月7日12:00)、操作链接 |
| 打回通知 | 分公司打回 | 打回理由、截止时间(次月7日18:00)、操作链接 |
| 超时提醒 | 即将超时 | 剩余时间、当前状态、截止时间 |

#### 发送策略

- 即时发送：驳回/打回后立即发送
- 定时提醒：截止前4小时、1小时各一次
- 超时告警：超时后发送告警给上级
- 重试机制：失败重试3次，间隔5分钟

### 3.5 报表系统

#### 核心报表

| 报表名称 | 数据粒度 | 可见角色 |
|---------|---------|---------|
| 预算汇总表 | 组织层级 | 总公司、分公司 |
| 预算明细表 | 科目层级 | 全部角色（脱敏） |
| 执行对比表 | 月度对比 | 总公司、分公司 |
| 驳回记录统计表 | 驳回维度 | 总公司、分公司 |
| 趋势分析表 | 历史趋势 | 总公司 |

#### 导出规范

- 格式：PDF、Excel(.xlsx)
- 水印：`机密 - [用户名] - [导出时间]`，45°倾斜，透明度30%
- 权限：仅授权角色可导出，操作记录日志

---

## 四、数据模型

### 4.1 核心表结构

#### budget_sheet（预算表头表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| sheet_no | VARCHAR(50) | 预算单号（唯一，BG-YYYYMM-XXX） |
| org_id | BIGINT | 组织ID |
| org_name | VARCHAR(100) | 组织名称 |
| budget_month | VARCHAR(7) | 预算月份（YYYY-MM） |
| status | VARCHAR(20) | 状态码 |
| reject_level | VARCHAR(10) | 驳回来源（HQ/Branch/None） |
| reject_reason | TEXT | 驳回理由 |
| deadline_time | DATETIME | 截止时间 |
| current_handler | VARCHAR(50) | 当前处理人 |
| total_budget | DECIMAL(18,2) | 预算总额 |
| total_actual | DECIMAL(18,2) | 实际总额 |
| variance_rate | DECIMAL(5,2) | 差异率 |

#### budget_detail（预算明细表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| sheet_id | BIGINT | 表头ID（外键） |
| subject_code | VARCHAR(20) | 科目编码 |
| subject_name | VARCHAR(100) | 科目名称 |
| budget_amount | DECIMAL(18,2) | 预算金额 |
| actual_amount | DECIMAL(18,2) | 实际金额 |
| variance_amount | DECIMAL(18,2) | 差异金额 |
| variance_rate | DECIMAL(5,2) | 差异率 |
| dept_id | BIGINT | 部门ID |
| dept_name | VARCHAR(100) | 部门名称 |

#### budget_reject_history（驳回历史记录表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| sheet_id | BIGINT | 预算单ID |
| sheet_no | VARCHAR(50) | 预算单号 |
| reject_from_level | VARCHAR(10) | 驳回方层级 |
| reject_from_user | VARCHAR(50) | 驳回人工号 |
| reject_to_level | VARCHAR(10) | 被驳回方层级 |
| reject_to_dept_id | BIGINT | 被驳回部门ID |
| reject_reason | TEXT | 驳回理由 |
| deadline_time | DATETIME | 截止时间 |
| reject_time | DATETIME | 驳回时间 |
| handle_time | DATETIME | 处理时间 |
| is_timeout | TINYINT(1) | 是否超时 |

#### budget_subject（预算科目表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| subject_code | VARCHAR(20) | 科目编码（唯一） |
| subject_name | VARCHAR(100) | 科目名称 |
| parent_code | VARCHAR(20) | 父级科目编码 |
| level | INT | 科目层级（1/2/3） |
| is_leaf | TINYINT(1) | 是否叶子节点 |
| sort_order | INT | 排序号 |
| is_active | TINYINT(1) | 是否启用 |

### 4.2 实际已落地的表名映射

| 需求文档表名 | 实际代码表名 | 说明 |
|-------------|-------------|------|
| budget_sheet | budget_preparation | 预算编制主表 |
| budget_detail | budget_preparation_detail | 预算编制明细表 |
| budget_reject_history | （未建表） | 驳回历史记录 |
| budget_subject | budget_subject | 预算科目表 |
| （无） | budget_validation_rule | 校验规则表 |

---

## 五、接口规范

### 5.1 预算编制接口

| 接口路径 | 方法 | 说明 | 权限 |
|---------|------|------|------|
| /system/preparation/list | GET | 分页查询列表 | system:preparation:list |
| /system/preparation/{id} | GET | 查询详情 | system:preparation:query |
| /system/preparation | POST | 新增 | system:preparation:add |
| /system/preparation | PUT | 修改 | system:preparation:edit |
| /system/preparation/{ids} | DELETE | 删除 | system:preparation:remove |
| /system/preparation/submit/{id} | POST | 提交审核 | system:preparation:submit |
| /system/preparation/batchSubmit | POST | 批量提交 | system:preparation:submit |

### 5.2 预算审核接口

| 接口路径 | 方法 | 说明 | 权限 |
|---------|------|------|------|
| /system/preparation/approve/{id} | POST | 审批通过 | system:preparation:approve |
| /system/preparation/batchApprove | POST | 批量审批 | system:preparation:approve |
| /system/preparation/reject/{id} | POST | 驳回 | system:preparation:reject |
| /system/preparation/validation/{sheetId} | GET | 获取校验结果 | system:preparation:query |

### 5.3 明细管理接口

| 接口路径 | 方法 | 说明 | 权限 |
|---------|------|------|------|
| /system/preparation/detail/list/{sheetId} | GET | 查询明细 | system:preparation:query |
| /system/preparation/detail | POST | 保存明细 | system:preparation:edit |
| /system/preparation/detail/batch | POST | 批量保存 | system:preparation:edit |

---

## 六、非功能需求

### 6.1 性能要求

| 指标 | 目标值 |
|------|--------|
| 数据库查询响应 | < 500ms |
| 接口响应时间 | < 2s |
| 并发用户支持 | 100+ |
| 数据导出能力 | 万级数据 |

### 6.2 安全要求

- 功能权限：基于 Sa-Token 的角色权限控制（菜单/按钮/接口三级）
- 数据权限：基于注解的数据范围控制（5种范围：全部/自定义/本部门/本部门及以下/仅本人）
- 数据脱敏：后端动态处理，前端不存储原始数据
- 导出水印：用户名+时间戳
- 操作日志：关键操作全部记录

### 6.3 兼容性要求

- 浏览器：Chrome / Firefox / Edge
- 屏幕：支持不同分辨率

---

## 七、术语表

| 术语 | 英文 | 说明 |
|------|------|------|
| HQ | Headquarters | 总公司本部 |
| Branch Office | Branch Office | 分公司本部 |
| Reject | Reject | 驳回（上级对下级） |
| Send Back | Send Back | 打回（同级或上级对下级） |
| Actuals | Actuals | 实绩数据（实际财务数据） |
| Budget Sheet | Budget Sheet | 预算单 |
| Variance Rate | Variance Rate | 差异率 |

---

**文档来源**：综合自 需求文档.md、预算编制审核API接口文档.md、预算编制审核后端实现说明.md、预算编制前端开发说明.md、预算审核前端开发说明.md、预算编制向导式表单设计方案.md、预算编制按科目类型分Tab改造说明.md、预算编制审核功能测试指南.md
**文档作者**：张三
