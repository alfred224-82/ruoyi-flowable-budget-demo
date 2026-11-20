# Schema 页面引擎规范（T10.6）

> 版本：v1.0　日期：2026-09-16
> 依据：《资金管控系统开发任务文档（系统管理）》v1.29 T10.6、《资金管控系统设计文档》v1.3 §1.8.1（三层架构·业态包层·Schema 页面引擎）

## 1. 目标与定位

Schema 页面引擎是**多业态平台底座**的声明式渲染能力：一套页面结构（表单/列表/看板）由数据（`biz_bundle.schema_blocks`）驱动，**新增业态或调整单店差异只需改数据、不动核心代码**，PC（`ruoyi-ui`）与小程序（`ruoyi-app`）共用同一份 Schema 契约。

三层协作：

| 层 | 载体 | 职责 |
| --- | --- | --- |
| 业态包层 | `biz_bundle.schema_blocks` | 声明该业态的默认区块结构（基底） |
| 单店覆盖层 | `tenant_biz_config.module_overrides` | 按租户叠加启停/文案/字段可见性（禁止 fork 业态包） |
| 渲染层 | 后端 `getRenderSchema` + 前端 `SchemaRenderer` | 合并解析 → 声明式渲染 |

## 2. 数据契约

### 2.1 区块定义（block）

`schema_blocks` 为**区块定义对象数组**，单个 block 结构：

| 键 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `code` | String | 是 | 区块唯一编码，与 `module_overrides.global.copy.<code>` 对齐 |
| `module` | String | 是 | 关联模块编码，与 `module_overrides.modules.<module>` 对齐 |
| `type` | String | 是 | 渲染类型：`form`（表单）/ `list`（列表）/ `board`（自定义看板，交前端插槽） |
| `title` | String | 否 | 区块标题，可被覆盖 |
| `fields` | Array | form 必填 | 字段声明数组 |
| `columns` | Array | list 必填 | 列声明数组 |

### 2.2 字段声明（field / column）

| 键 | 类型 | 说明 |
| --- | --- | --- |
| `key` | String | 字段键（表单数据属性名 / 列 prop） |
| `label` | String | 字段标签，可被覆盖 |
| `type` | String | 控件类型：`input`/`number`/`date`/`select`/`switch`/`textarea`/`user`（未知类型兜底为 input） |
| `required` | Boolean | 是否必填，可被覆盖 |
| `visible` | Boolean | 是否可见（默认 true），被覆盖为 false 时字段剔除 |
| `options` | Array | select 选项 `[{label,value}]` |
| `span` | Number | 栅格宽度（1~24，默认 12） |
| `placeholder` | String | 占位提示 |
| `width` | Number | list 列宽 |

## 3. 单店覆盖叠加规则

`tenant_biz_config.module_overrides` 经 T10.4 Schema 校验后，运行时按以下优先级叠加（后端 `TenantBizConfigServiceImpl#getRenderSchema`）：

1. `modules.<module>.enabled = false` → **剔除整个区块**（`module` 对齐 `block.module`）
2. `modules.<module>.copy.title` → 覆盖区块标题；`global.copy.<code>` → 区块标题兜底文案
3. `modules.<module>.fields.<key>.visible = false` → 剔除该字段
4. `modules.<module>.fields.<key>.required` → 覆盖必填
5. `modules.<module>.fields.<key>.label`（优先）/ `copy.<key>`（次之）→ 覆盖字段标签
6. `global.theme` → 下发主题预设（前端 `SchemaRenderer` 挂 `theme-<preset>` class）

> 覆盖结构白名单由 `TenantBizConfigSchemaValidator` 保证（顶层仅 `modules`/`global`，见 T10.4）。

## 4. 后端接口

| 端点 | 方法 | 权限 | 说明 |
| --- | --- | --- | --- |
| `/system/tenantBizConfig/render` | GET | 任意登录用户 | 返回当前登录租户的运行时渲染 Schema |
| `/system/tenantBizConfig/schema` | GET | `system:tenantBizConfig:query` | 返回 `module_overrides` 的 JSON Schema 描述（T10.4） |

`/render` 返回体：

```json
{
  "bizCode": "fund_control",
  "theme": "",
  "blocks": [
    {
      "code": "fund_project_form",
      "module": "project",
      "type": "form",
      "title": "项目信息",
      "fields": [
        { "key": "projectName", "label": "项目名称", "type": "input", "required": true, "span": 12 }
      ]
    }
  ]
}
```

后端自动解析当前租户上下文：`tenantId = LoginHelper.isAdmin() ? null : LoginHelper.getTenantId()`，`bizCode = licenseService.getCurrentBizCode()`（T10.5 同源）。

## 5. 前端组件 SchemaRenderer

全局注册组件 `SchemaRenderer`（`@/components/SchemaRenderer/index.vue`），三种用法：

```vue
<!-- 用法一：自动加载当前租户 Schema -->
<schema-renderer auto-load @submit="onSubmit" />

<!-- 用法二：父组件传入 blocks + 双向绑定表单数据 -->
<schema-renderer :blocks="myBlocks" v-model="form" />

<!-- 用法三：自定义 board 区块（具名插槽，slot 名 = block.code） -->
<schema-renderer auto-load>
  <template #fullhouse_board="{ block }">
    <my-board :block="block" />
  </template>
</schema-renderer>
```

### Props

| Prop | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `blocks` | Array | null | 直接传入区块（优先级高于 `autoLoad`） |
| `auto-load` | Boolean | false | 自动从 `/render` 加载当前租户 Schema |
| `theme` | String | '' | 主题预设（未传时取 Schema 下发的 theme） |
| `show-actions` | Boolean | true | 是否展示提交/重置按钮 |
| `value` | Object | {} | 表单数据（`v-model`） |

### Events

| 事件 | 载荷 | 说明 |
| --- | --- | --- |
| `loaded` | 完整 Schema | `autoLoad` 加载完成 |
| `submit` | 表单数据副本 | 点击提交 |
| `reset` | - | 点击重置 |
| `input` | 表单数据 | `v-model` 同步 |

## 6. 种子数据

执行 `docs/最新整理好的需求/系统管理/sql/M10_04_schema_engine.sql`，将三业态包 `schema_blocks` 升级为区块定义对象数组：

- `fund_control`：`fund_project_form`(form) / `fund_expense_form`(form) / `fund_income_list`(list)
- `boardgame`：`activity_form`(form) / `signup_form`(form) / `fullhouse_board`(board)
- `fresh_food`：`groupbuy_form`(form) / `signup_form`(form) / `fresh_product_block`(board)

## 7. 验收清单

- [x] 后端 `getRenderSchema` 合并解析（enabled/copy/fields/theme）
- [x] `/render` 端点任意登录用户可访问，自动解析租户上下文
- [x] `SchemaRenderer` 组件支持 form/list/board 三类区块 + 插槽自定义
- [x] 组件全局注册，`auto-load` / `:blocks` / `v-model` 三用法可用
- [x] 种子 SQL 将 schema_blocks 升级为对象定义（幂等 UPDATE）
- [x] 与 T10.4 Schema 校验、T10.5 文案 i18n 契约对齐（module/copy/code 一致）
