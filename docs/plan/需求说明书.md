# 企业全面预算管理系统 — 需求说明书

**版本**：v1.5  
**日期**：2026-07-04  
**项目**：基于 RuoYi-Flowable-Plus 框架的企业级预算管理解决方案

| 角色分类 | 对应员工编号 |
|---------|----------|
| 部门预算编制人员 | H00889 |
| 部门经理 | H00998 |
| 分公司本部 | H00223 |
| 总公司本部 | H00001 |
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

| 阶段 | 时间范围 | 核心动作 | 责任主体 | 邮件通知 |
|------|----------|----------|----------|----------|
| 编制期 | 本月26日 - 次月3日 | 部门编制→提交审核 | 部门编制人员 | 编制期开始提醒（26日） |
| 三级审批期 | 编制提交后即时 | 部门经理→分公司本部→总公司本部逐级审批 | 各级审批人 | 待审批通知、截止前提醒 |
| 驳回修改期 | 驳回后即时 | 编制人员接收驳回并修改重提 | 部门编制人员 | 驳回通知（含驳回理由、操作链接） |
| 执行期 | 总公司本部通过后 | 预算锁定，正式执行 | 系统自动 | 审批通过通知（预算已锁定） |

### 2.2 权限模型

#### 角色权限矩阵

| 角色 | 编制权限 | 审核权限 | 对账单查看 | 打回权限 | 数据可见范围 |
|------|---------|---------|-----------|---------|-------------|
| 总公司本部 | 全辖编制 | 全辖审批 | 可见 | 可打回至分公司本部 | 全辖数据 |
| 分公司本部 | 分公司汇总编制 | 分公司审批 | 可见 | 可打回至部门/自行重编 | 分公司及下属部门 |
| 部门编制人员 | 本部门编制 | 无 | 不可见 | 仅接收打回任务 | 本部门（实际金额脱敏） |
| 部门经理 | 无 | 本部门审批 | 不可见 | 无 | 本部门（实际金额脱敏） |


### 2.3 状态流转模型

#### 双字段设计

预算编制采用 `status`（生命周期状态）+ `approval_stage`（审批阶段）双字段设计，将业务状态与审批进度解耦：

#### 生命周期状态（status）

| 状态码 | 名称 | 颜色 | 说明 |
|--------|------|------|------|
| Draft | 草稿 | 灰色 | 可编辑、可删除，编辑后仍为 Draft |
| Completed | 完成编制 | 蓝色 | 编制已完成，可提交审核；不可编辑、不可删除 |
| Pending_Review | 待审核 | 橙色 | 三级审批流程中，具体阶段由 approval_stage 字段决定 |
| Pending_Revision | 待修订 | 橙色 | 分公司打回至部门，需修改后重新提交 |
| Approved | 审核通过 | 绿色 | 三级审批全部通过，预算锁定，不可修改 |
| Rejected | 已驳回 | 红色 | 任一级别驳回，回退到编制人员；可编辑（编辑后重置为 Draft） |

#### 审批阶段（approval_stage）

仅在 status = Pending_Review 时有意义：

| 阶段码 | 名称 | 说明 |
|--------|------|------|
| None | 无 | 非审核状态时默认为 None |
| Dept | 部门领导 | 第1级审批，部门经理审批 |
| Branch | 分公司领导 | 第2级审批，分公司本部审批 |
| HQ | 总公司领导 | 第3级审批，总公司本部审批 |

#### 状态流转规则

```
Draft → Completed（向导「完成编制」，保存明细并校验数据完整性）
Completed → Pending_Review + approval_stage=Dept（提交审核，启动Flowable流程）
Pending_Review + Dept → Pending_Review + Branch（部门经理通过，流转到分公司）
Pending_Review + Branch → Pending_Review + HQ（分公司通过，流转到总公司）
Pending_Review + HQ → Approved + approval_stage=None（总公司通过，预算锁定）
任意审批级 → Rejected + approval_stage=None（任一级别驳回，回退到编制人员）
Pending_Review + Branch → Pending_Revision + approval_stage=None（分公司打回至部门）
Rejected → Draft（编制人员编辑驳回记录后，状态自动重置为 Draft）
Draft/Rejected → Completed（再次通过向导「完成编制」）
```

#### 操作权限矩阵

| 操作 | 允许的状态 | 说明 |
|------|----------|------|
| 编辑 | Draft、Rejected | 编辑后状态自动重置为 Draft |
| 删除 | 仅 Draft | 其他状态均不可删除 |
| 完成编制 | Draft、Rejected | 通过向导第3步确认，状态变为 Completed |
| 提交审核 | 仅 Completed | 单个/批量提交，启动审批流程 |
| 审批通过/驳回 | Pending_Review | 仅当前审批阶段对应角色的用户可操作 |

#### 设计优势

- **status 稳定**：不管审批层级如何变化，生命周期状态只有 6 个，不会膨胀
- **approval_stage 灵活**：增减审批层级只影响此字段，不影响状态机
- **查询简洁**：查所有待审核单只需 `WHERE status = 'Pending_Review'`，无需 OR 多个状态值
- **前端简化**：状态标签只映射 6 个值，审批阶段单独展示
- **流程可控**：新增 Completed 状态作为编制与审核的缓冲，确保只有完整编制才能进入审批流程

#### 三级审批流程说明

1. **第1级：部门经理审批**
    - 编制人员提交审核后，流程流转到部门经理
    - 部门经理可以选择通过或驳回
    - 驳回必须填写原因（5-500字符）
    - 通过可以批量操作
    - 通过后流程流转到分公司本部

2. **第2级：分公司本部审批**
    - 部门经理通过后，流程流转到分公司本部
    - 分公司本部可以选择通过或驳回
    - 驳回必须填写原因
    - 通过后流程流转到总公司本部

3. **第3级：总公司本部审批**
    - 分公司本部通过后，流程流转到总公司本部
    - 总公司本部可以选择通过或驳回
    - 通过后预算锁定，不可再编制
    - 驳回回退到编制人员

4. **驳回处理**
    - 任何级别驳回都会回退到编制人员（状态变为 Rejected）
    - 编制人员可以修改后重新提交审核（编辑后状态重置为 Draft，需重新完成编制）
    - 驳回历史记录会保存到驳回历史表中
    - 驳回时记录当前审批阶段到 reject_level 字段，便于追溯
    - 编制列表页和向导页均展示最近一条驳回原因

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

**预算总额实时展示**：
- 预算总额显示在页面右上角（卡片头部区域），全程可见
- 金额使用千位符格式化显示（如 5,290.00）
- 第2步录入金额时实时计算并更新总额显示
- 第3步汇总区同步展示预算总额
- 安全处理 NaN 值，确保异常情况下显示 `0.00`

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
- 编辑草稿/已驳回状态的编制（直接进入科目预算步骤，编辑后状态重置为 Draft）
- 删除草稿状态的编制（仅 Draft 可删除，其他状态不可删除）
- 完成编制（通过向导第3步确认，状态变为 Completed）
- 提交审核（仅 Completed 状态可提交，单个/批量）
- 自动计算差异金额和差异率
- 预算总额自动汇总（第2步点击下一步时保存明细，后端自动汇总更新到编制主表）
- 明细数据保存策略：明细表无数据则新增，有数据则先删后插（全量更新）
- 前端金额输入确保数值类型，防止字符串拼接导致后端 BigDecimal 反序列化失败
- 部门+月份重复校验（防止同一部门同一月份重复创建）
- 已驳回记录展示驳回原因：列表页状态旁显示红色提示图标（tooltip），向导页顶部显示红色横幅

#### 3.1.4 上月实绩驱动下月预算

**功能说明**：
新建预算编制时，系统自动获取本部门上月已审批通过的预算数据，作为本月预算金额的初始值。

**实现逻辑**：
- 新建编制进入第2步时，自动调用接口查询上月已审批数据
- 查询条件：同一部门 + 上月 + 状态为 Approved
- 若找到上月数据，自动填充到对应科目的预算金额字段
- 填充后提示用户「已加载上月预算数据，请根据实际情况调整」
- 用户可在上月数据基础上修改调整

**边界处理**：
- 1月份的上月为上年12月份（自动处理跨年）
- 若上月无已审批数据，则科目金额默认为0，用户手动填写

### 3.2 预算审核模块

#### 3.2.1 审核列表

- 默认显示待审核状态记录
- 按角色自动过滤部门范围
- 支持年度、月份、部门筛选
- 支持批量审批

#### 3.2.2 审核详情页

- 基础信息展示（el-descriptions）
- **当前审批阶段显示**（显示当前是第几级审批：部门经理/分公司本部/总公司本部）
- 校验结果展示（风控警告）
- 科目预算明细（按科目类型分Tab）
- 审核操作（通过/驳回）- 采用角色-阶段匹配校验，仅当前审批阶段对应角色的用户可以看到操作按钮；非当前角色或已终结状态（已驳回/已通过）仅查看
- 已驳回/已通过的记录不显示审批操作按钮
- 驳回理由必填（5-500字符）
- 审批意见会作为 Flowable 任务评论保存

#### 3.2.3 审批与驳回机制

**审批权限校验**：
- 审批接口不使用 `@SaCheckPermission` 菜单权限码，改由 Service 层进行角色-阶段匹配校验
- 校验逻辑：当前用户角色 key 必须与当前审批阶段对应角色匹配（Dept→deptManager、Branch→branch、HQ→headquarters）
- admin 角色可审批所有阶段
- 非当前阶段角色的用户只能查看详情，无法执行审批操作
- 已驳回/已通过等已终结状态的记录，前端不显示审批按钮

**驳回规则**：
- 任何级别（部门经理/分公司本部/总公司本部）驳回都直接打回给编制人员
- 编制记录状态回退为 Rejected，编制人员可修改后重新提交审核
- 编辑驳回记录后状态自动重置为 Draft，需重新完成编制后才能提交
- 驳回时必须填写驳回原因（5-500字符）
- 驳回历史记录自动保存到驳回历史表
- 驳回时自动记录当前审批阶段（reject_level）
- 驳回原因在前端展示：列表页状态列旁红色 ? 图标（tooltip），向导页顶部红色横幅

**审批通过截止日期**：
- 每级审批通过后，系统记录当前审批时间
- 总公司本部最终通过后，预算锁定不可再编制
- 审批流程需在编制期内完成（本月26日 - 次月3日）

### 3.3 智能风控

#### 规则引擎说明

校验规则存储在 `budget_validation_rule` 表中，支持动态配置。每条规则包含：规则编码、规则类型、严重级别、适用科目类型/编码、阈值、规则表达式、错误提示等。

#### 规则类型说明

| 规则类型 | 说明 |
|---------|------|
| REQUIRED | 必填校验 |
| MIN_AMOUNT | 最低金额校验 |
| MAX_AMOUNT | 最高金额预警 |
| RATIO_LIMIT | 比例限制校验 |
| SUM_CHECK | 合计校验 |
| FORMULA | 公式/建议性规则 |

#### 严重级别

| 级别 | 颜色 | 行为 |
|------|------|------|
| ERROR | 红色 | 阻断，必须修正后才能提交 |
| WARNING | 黄色 | 警告，提示但不阻断 |
| INFO | 蓝色 | 建议性提示 |

#### 初始化规则清单（11条）

| 编码 | 名称 | 类型 | 级别 | 适用科目 | 阈值 | 表达式 |
|------|------|------|------|---------|------|--------|
| REQ_ALL | 所有叶子科目必须填写预算金额 | REQUIRED | ERROR | 全部 | - | - |
| MIN_INCOME | 收入类科目预算金额不得为负 | MIN_AMOUNT | ERROR | INCOME | 0 | amount >= 0 |
| MIN_COST | 成本类科目预算金额不得为负 | MIN_AMOUNT | ERROR | COST | 0 | amount >= 0 |
| MIN_EXPENSE | 费用类科目预算金额不得为负 | MIN_AMOUNT | ERROR | EXPENSE | 0 | amount >= 0 |
| MAX_SINGLE_EXPENSE | 单项费用预算超过100万 | MAX_AMOUNT | WARNING | EXPENSE | 1,000,000 | amount <= 1000000 |
| MAX_SINGLE_COST | 单项成本预算超过200万 | MAX_AMOUNT | WARNING | COST | 2,000,000 | amount <= 2000000 |
| RATIO_ADMIN_FEE | 管理费用占费用类总额比例不超过40% | RATIO_LIMIT | WARNING | EXPENSE (3002) | 40% | subject_sum / type_total <= 0.4 |
| RATIO_SALES_FEE | 销售费用占收入类总额比例不超过15% | RATIO_LIMIT | INFO | EXPENSE (3001) | 15% | sales_fee / income_total <= 0.15 |
| SUM_INCOME_NOT_ZERO | 收入类预算总额不能为零 | SUM_CHECK | ERROR | INCOME | - | type_total > 0 |
| INFO_ZERO_BASED | 建议采用零基预算法逐项审核 | FORMULA | INFO | 全部 | - | - |
| INFO_ROUND_CHECK | 预算金额建议取整到百位 | FORMULA | INFO | 全部 | - | amount % 100 == 0 |

### 3.4 邮件通知

#### 触发场景与邮件模板

| 触发场景 | 收件人 | 邮件内容 |
|---------|--------|----------|
| 编制期开始 | 部门编制人员 | 编制期已开始，请完成本月预算编制（截止：次月3日） |
| 提交审核 | 部门经理 | 您有待审批的预算编制，请及时处理 |
| 审批流转 | 下一级审批人 | 您有新的预算审批任务（部门经理已通过，请审批） |
| 任一级驳回 | 编制人员 | 您的预算编制被驳回，驳回理由：XXX，请修改后重新提交 |
| 审批全部通过 | 编制人员 | 您的预算编制已审批通过，预算已锁定 |
| 截止前提醒 | 当前处理人 | 您有待处理的预算任务，距截止时间不足4小时 |
| 超时告警 | 当前处理人的上级 | XXX的预算任务已超时，请及时跟进 |

#### 发送策略

- 即时发送：驳回/打回后立即发送
- 定时提醒：截止前4小时、1小时各一次
- 超时告警：超时后发送告警给上级
- 重试机制：失败重试3次，间隔5分钟

### 3.5 系统消息通知

#### 功能概述

系统内消息中心，登录后在页面顶部导航栏显示未读消息数量角标，点击展开消息列表。

#### 消息类型

| 类型 | 说明 | 触发场景 |
|------|------|----------|
| 审批通知 | 待办审批任务 | 提交审核、审批流转 |
| 驳回通知 | 预算被驳回 | 任一级审批人驳回 |
| 系统通知 | 系统公告 | 管理员发布 |

#### 前端展示

- 导航栏右上角铃铛图标 + 未读数角标（红色）
- 点击铃铛展开消息下拉面板（最近10条）
- 未读消息加粗显示，已读消息正常样式
- 点击消息可跳转至对应业务页面（如审批详情页）
- 底部「查看全部」跳转消息中心页面

#### 后端接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| /system/message/unreadCount | GET | 获取当前用户未读消息数 |
| /system/message/list | GET | 分页查询消息列表 |
| /system/message/read/{id} | PUT | 标记单条消息为已读 |
| /system/message/readAll | PUT | 标记全部消息为已读 |

#### 数据表设计

**消息表 sys_message**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 接收用户ID |
| title | VARCHAR(200) | 消息标题 |
| content | TEXT | 消息内容 |
| message_type | VARCHAR(20) | 消息类型（APPROVAL/REJECT/SYSTEM） |
| biz_type | VARCHAR(50) | 业务类型（如 budget_preparation） |
| biz_id | BIGINT | 业务ID（用于跳转） |
| is_read | TINYINT | 是否已读（0未读 1已读） |
| create_time | DATETIME | 创建时间 |

#### 与邮件通知的关系

- 系统消息：站内实时通知，用户登录后立即可见
- 邮件通知：站外通知，用户未登录时发送
- 两者触发场景一致，互为补充

### 3.6 报表系统

> 待后续开发

### 3.7 AI 智能助手

#### 功能概述

系统内置 AI 智能助手，基于大语言模型（LLM）为用户提供对话式交互能力，辅助预算数据分析、报表摘要生成、预算管理问题解答及编制建议。AI 功能为可选模块，通过配置开关 `ai.enabled=true` 控制启用状态。

#### 核心功能

| 功能 | 说明 |
|------|------|
| 预算数据分析 | 自动读取当前查看的预算单数据（表头+明细），按科目类型分组汇总，进行差异分析 |
| 报表摘要生成 | 根据预算明细生成按科目类型的汇总摘要，金额使用千位符格式化 |
| 预算管理问答 | 解答审批流程、状态含义、风控规则等预算管理相关问题 |
| 编制建议 | 基于风控规则和财务最佳实践，提供预算编制优化建议 |

#### 对话模式

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| 同步对话 | 一次性返回完整回复 | 简单问答、快速查询 |
| 流式对话（SSE） | 通过 Server-Sent Events 逐字推送，前端实时渲染 | 长文本生成、报表摘要 |

#### 会话记忆机制

- 支持多轮对话上下文保持，默认记忆窗口为最近 **20 轮**对话（可配置）
- 每个会话分配唯一 `sessionId`，前端可主动清除会话记忆
- 超出记忆窗口的历史消息自动淘汰（FIFO）

#### 预算数据上下文注入

当用户在查看某张预算单时发起 AI 对话（携带 `sheetId`），系统自动将以下数据注入 AI 上下文：

| 注入数据 | 内容 |
|---------|------|
| 表头信息 | 预算单号、组织名称、预算期间、状态、预算总额、实际总额、差异率 |
| 明细数据 | 按科目类型（收入/成本/费用/资产/负债/权益）分组，列出每个科目的编码、名称、预算金额、实际金额及小计 |

> 数据注入遵循现有数据脱敏规则，不同角色看到的上下文数据范围与前端一致。

#### 系统提示词（System Prompt）

```
你是一个企业全面预算管理系统的AI助手。你可以帮助用户：
1. 分析预算数据（收入、成本、费用、资产、负债、权益各类科目的预算金额）
2. 生成预算报表摘要（按科目类型汇总、差异分析等）
3. 解答预算管理相关问题（审批流程、状态含义等）
4. 提供预算编制建议（基于风控规则和财务最佳实践）

请用中文回答，回答要专业、简洁、易懂。涉及金额时使用千位符格式化。
```

#### 配置参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `ai.enabled` | 是否启用 AI 功能 | `false` |
| `ai.api-key` | OpenAI API Key（兼容第三方 API） | 无（必填） |
| `ai.base-url` | API Base URL | `https://api.openai.com/v1` |
| `ai.model-name` | 模型名称 | `gpt-3.5-turbo` |
| `ai.temperature` | 温度参数（0-2，越高越随机） | `0.7` |
| `ai.max-tokens` | 最大生成 token 数 | `2048` |
| `ai.memory-window-size` | 聊天记忆窗口大小（轮数） | `20` |
| `ai.system-prompt` | 系统提示词 | 见上方 |

#### 技术实现

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| AI 框架 | OkHttp + DashScope Responses API | 绕过 LangChain4j，直接对接 DashScope 以支持 qwen3.7-max 等新模型 |
| 模型接口 | DashScope Responses API | 阿里云百炼平台，使用 `previous_response_id` 实现服务端多轮对话 |
| 流式推送 | SSE（Server-Sent Events） | 基于 Spring SseEmitter，逐字推送 |
| 会话存储 | 内存（ConcurrentHashMap） | 当前版本存储于应用内存，重启后丢失 |
| 条件装配 | `@ConditionalOnProperty` | AI 模块按需启用，未配置时不加载 Bean |

#### 接口设计

| 接口路径 | 方法 | 说明 | 权限 |
|---------|------|------|------|
| `/ai/chat` | POST | 同步对话（一次性返回） | 已登录用户 |
| `/ai/chat/stream` | POST | 流式对话（SSE 逐字推送） | 已登录用户 |
| `/ai/chat/session/{sessionId}` | DELETE | 清除会话记忆 | 已登录用户 |
| `/ai/chat/status` | GET | 检查 AI 服务是否可用 | 已登录用户 |

**请求示例**：
```json
POST /ai/chat
{
  "sessionId": "可选，为空则创建新会话",
  "message": "帮我分析一下这张预算单的费用类科目",
  "sheetId": 123,
  "stream": false
}
```

**响应示例**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "sessionId": "a1b2c3d4e5f6",
    "reply": "根据您正在查看的预算单，费用类科目汇总如下：\n\n- 办公费：预算 15,000.00 元\n- 差旅费：预算 28,500.00 元\n\n费用类合计：53,500.00 元",
    "finished": true
  }
}
```

#### 前端交互规范

- **入口位置**：预算编制向导页和预算审核详情页右下角 AI 助手悬浮按钮
- **对话面板**：点击弹出，支持最小化/关闭
- **消息展示**：用户消息右对齐（蓝色气泡），AI 回复左对齐（灰色气泡），流式模式逐字显示
- **关联预算单**：在特定预算单页面发起对话时自动携带 `sheetId`
- **加载状态**：AI 思考中显示「正在思考...」动画

#### 异常处理

| 异常场景 | 处理方式 |
|---------|----------|
| AI 服务不可用 | 返回友好提示，不阻断用户其他操作 |
| 预算单不存在或无权限 | 上下文注入返回空，AI 按通用知识回答 |
| SSE 连接超时（2分钟） | 自动断开，前端提示用户重试 |
| 记忆窗口溢出 | 自动淘汰最早的历史消息 |

---

## 四、数据模型

> 表结构详见 SQL 脚本（`script/sql/mysql/budget/`）

---

## 五、接口规范

### 5.1 预算编制接口

| 接口路径 | 方法 | 说明 | 权限 |
|---------|------|------|------|
| /system/preparation/list | GET | 分页查询列表 | system:preparation:list |
| /system/preparation/{id} | GET | 查询详情 | system:preparation:query |
| /system/preparation | POST | 新增（返回新建记录ID） | system:preparation:add |
| /system/preparation | PUT | 修改 | system:preparation:edit |
| /system/preparation/{ids} | DELETE | 删除 | system:preparation:remove |
| /system/preparation/submit/{id} | POST | 提交审核（仅 Completed 状态） | system:preparation:submit |
| /system/preparation/complete/{id} | POST | 完成编制（Draft/Rejected → Completed） | system:preparation:submit |
| /system/preparation/batchSubmit | POST | 批量提交（仅 Completed 状态） | system:preparation:submit |

### 5.2 预算审核接口

| 接口路径 | 方法 | 说明 | 权限 |
|---------|------|------|------|
| /system/preparation/approve/{id} | POST | 审批通过 | Service 层角色-阶段匹配校验 |
| /system/preparation/batchApprove | POST | 批量审批 | Service 层角色-阶段匹配校验 |
| /system/preparation/reject/{id} | POST | 驳回 | Service 层角色-阶段匹配校验 |
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

### 4.4 2026-07-03 更新记录

| 修改项 | 说明 |
|--------|------|
| 预算总额展示优化 | 移至页面右上角卡片头部，千位符格式化，去掉“返回查询页面”按钮 |
| 新增接口返回ID | `POST /system/preparation` 返回类型从 `R<Void>` 改为 `R<Long>`，返回新建记录主键ID |
| 明细保存逻辑 | 第2步下一步时统一调用批量保存接口，后端先删后插并自动汇总更新编制总额 |
| budget_period 生成列 | 使用 `@TableField(insertStrategy=NEVER, updateStrategy=NEVER)` 避免 MyBatis-Plus 写入生成列导致 SQL 错误 3105 |
| 金额类型安全 | 前端 `calculateTotalBudget` 使用 `parseFloat` 确保数值类型，模板渲染加 `isNaN` 兜底 |

### 4.5 2026-07-03 三级审批流程实现

| 修改项 | 说明 |
|--------|------|
| BPMN流程定义 | 更新为三级审批流程：部门经理 → 分公司本部 → 总公司本部，每级通过/驳回网关 |
| Flowable集成 | `approve`/`reject` 方法集成 Flowable TaskService，完成任务时设置流程变量 `result=pass/reject` |
| 角色配置 | 新增分公司本部（role_id=4, role_key='branch_leader'）、总公司本部（role_id=5, role_key='hq_leader'）角色 |
| 候选组映射 | 部门经理=ROLE3、分公司本部=ROLE4、总公司本部=ROLE5，通过流程变量传入BPMN |
| 流程部署器 | BudgetProcessDeployer 改为始终部署最新版本（移除跳过已存在的逻辑） |

### 4.6 2026-07-03 状态模型重构（双字段设计）

| 修改项 | 说明 |
|--------|------|
| 状态模型重构 | 将单字段 `status` 拆分为 `status`（生命周期状态）+ `approval_stage`（审批阶段）双字段设计 |
| status 字段 | 简化为 6 个值：Draft、Completed、Pending_Review、Pending_Revision、Approved、Rejected |
| approval_stage 字段 | 新增字段，取值：None、Dept、Branch、HQ，仅在 Pending_Review 时有意义 |
| 驳回状态变更 | 驳回后状态从原来的 Draft 改为 Rejected，更准确反映业务语义 |
| 前端适配 | 编制列表新增审批阶段列，状态筛选简化为 4 项，审批阶段单独筛选 |
| 数据库迁移 | 需执行迁移 SQL：新增 approval_stage 列、迁移旧状态值、添加索引 |

### 4.7 2026-07-03 审批权限控制与完成编制状态

| 修改项 | 说明 |
|--------|------|
| 审批权限改造 | 审批接口移除 `@SaCheckPermission` 注解，改由 Service 层 `checkCurrentUserCanApprove()` 按角色-阶段匹配校验 |
| 新增 Completed 状态 | 新增「完成编制」状态（Completed），只有 Completed 状态可以提交审核 |
| 删除权限收紧 | 仅 Draft 状态可以删除，Rejected 及其他状态不可删除 |
| 编辑重置状态 | 编辑已驳回记录后，状态自动重置为 Draft，需重新完成编制 |
| 驳回原因展示 | 列表页已驳回行状态旁显示红色 ? 图标（tooltip 展示驳回原因），向导页顶部显示红色驳回原因横幅 |
| 审核详情页优化 | 已驳回/已通过的记录不再显示审批操作按钮，非当前阶段角色用户仅查看 |
| 新增完成编制接口 | `POST /system/preparation/complete/{id}`，校验明细完整性后将状态设置为 Completed |
| 状态流转调整 | 编制流程变更为：Draft → Completed → Pending_Review，驳回后：Rejected → Draft → Completed |

### 4.8 2026-07-04 AI智能助手、系统消息与邮件通知模块实现

| 修改项 | 说明 |
|--------|------|
| ruoyi-ai 模块新增 | 新增 AI 智能助手独立模块，包含 AiChatController、AiChatService、AiChatServiceImpl、AiProperties 配置类、DTO 等 |
| DashScope Responses API 对接 | AiChatServiceImpl 使用 OkHttp 直接调用 DashScope Responses API（绕过 LangChain4j），支持 qwen3.7-max 等新模型 |
| 多轮对话上下文 | 使用 `previous_response_id` 实现服务端多轮对话管理，客户端 ConcurrentHashMap 存储会话记忆 |
| 预算数据上下文注入 | 对话时携带 sheetId 自动注入预算单表头+明细数据到 AI 上下文；无 sheetId 时按预算关键词匹配全局数据 |
| AI 前端页面 | 新增 `views/system/ai/chat.vue` 对话页面、`api/system/ai.js` 接口、路由注册 |
| AI 菜单 SQL | 新增 `script/sql/mysql/ai_menu.sql` 菜单初始化脚本 |
| 系统消息通知模块 | 新增 SysMessage 实体/VO/Mapper/Service/Controller，支持 APPROVAL/REJECT/SYSTEM 三种消息类型 |
| 消息铃铛组件 | 新增 `components/MessageBell/index.vue`，导航栏右上角铃铛图标+未读数角标，点击展开消息面板 |
| 消息中心页面 | 新增 `views/system/message/index.vue` 消息列表页面、`api/system/message.js` 接口 |
| 邮件通知服务 | 新增 BudgetNotificationService/Impl，支持审批通知、驳回通知、编制期提醒、超时告警等邮件发送 |
| 邮件模板 | 新增 6 个 Thymeleaf 邮件模板：approval-notify、approved-notify、period-start-notify、reject-notify、timeout-alert、timeout-remind |
| 预算定时任务 | 新增 BudgetNotificationJob（ruoyi-job 模块），用于截止前提醒和超时告警 |
| BPMN 流程部署器 | 新增 BudgetProcessDeployer，应用启动时自动部署三级审批 BPMN 流程定义 |
| BPMN 流程 SQL | 新增 `budget_process_deploy.sql` 流程部署相关 SQL |
| 首页改造 | `views/index.vue` 大幅改造（424 行变更），适配预算管理主题 |
| 导航栏优化 | `Navbar.vue` 集成消息铃铛组件，右侧新增消息入口 |
| 路由新增 | `router/index.js` 新增 AI 对话、消息中心等路由 |
| 配置新增 | `application.yml` 新增 AI 配置段（ai.enabled/apiKey/baseUrl/modelName 等）和邮件配置 |
| Java 8 兼容性修复 | AiChatServiceImpl 中 `Map.of()` 替换为 `new HashMap<>()` + `put()`，兼容 Java 8 |
| OkHttp 4.x 兼容 | `RequestBody.create(MediaType, String)` 参数顺序修正为 OkHttp 4.x API |

---

**文档来源**：综合自 需求文档.md、预算编制审核API接口文档.md、预算编制审核后端实现说明.md、预算编制前端开发说明.md、预算审核前端开发说明.md、预算编制向导式表单设计方案.md、预算编制按科目类型分Tab改造说明.md、预算编制审核功能测试指南.md
**文档作者**：alfred224-82
**文档版本**：1.1
**文档生成日期**：2026-07-03
**文档更新日期**：2026-07-04
**文档状态**：完成
**文档说明**：本文档为预算编制系统需求说明书，详细描述了系统功能、数据模型、接口规范、非功能需求、术语表等内容。
