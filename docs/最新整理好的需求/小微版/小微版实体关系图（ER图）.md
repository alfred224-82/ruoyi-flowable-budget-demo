# 资金管控系统 · 小微版（MICRO）实体关系图（ER 图）

> 版本：v1.0　日期：2026-10-10
> 依据：`小微版/sql/00_执行顺序.sql`（v1.76）、`系统管理/sql/00_执行顺序.sql`（v1.32）、
> 《资金管控系统开发任务文档（小微版）》《资金管控系统开发任务文档（系统管理）》及各 `M*` 建表脚本。
> 范围：**小微版部署单元所涉及的全部数据表**（含平台底座共享表 + 小微版专属增量表），每张表为一个实体。

---

## 一、阅读约定

1. **实体命名**：直接使用数据库表名（Mermaid `erDiagram`）。
2. **属性取舍**：仅保留 **主键（PK）/ 外键（FK）/ 核心业务字段**；
   通用审计列 `create_by / create_time / update_by / update_time / del_flag / remark / create_dept` 一律省略，不再逐一画出。
3. **关联类型**：
   - `||--o{` 一对多（1:N）
   - `||--||` 一对一（1:1）
   - `}o--o{` 多对多（逻辑关联）
4. **外键说明**：本项目为 **逻辑外键**（无数据库级 `FOREIGN KEY` 约束），关联通过 `tenant_id / user_id / *_id / order_no` 等列在应用层维护；多租户由 MyBatis-Plus 插件按 `tenant_id` 自动隔离。
5. **表分层**：
   - 【系统级】无 `tenant_id`，全租户共享（须入 `MybatisPlusConfig.IGNORE_TENANT_TABLES`）。
   - 【租户级】含 `tenant_id`，租户间数据隔离。
   - 【混合级】`tenant_id=0` 或 `NULL` 表示平台级记录，非 0 表示租户自定义。
6. **两个核心锚点实体**：
   - `sys_tenant`（租户/门店，`id` 为主键）——几乎所有租户级表通过 `tenant_id` 归属其下。
   - `sys_user`（若依原生用户，`user_id` 为主键，**手机号为全局身份锚点**）——所有 `*_user_id` 逻辑指向它。

---

## 二、全景总览（域间关系）

> 下图省略字段，仅展示 **8 大业务域** 围绕 `sys_tenant` / `sys_user` 两大锚点的归属关系，细节见后续分域图。

```mermaid
erDiagram
    sys_tenant ||--o{ IDENT_ROLE   : "身份·角色·行业·页面"
    sys_tenant ||--o{ SERVICE      : "服务·定价·资源"
    sys_tenant ||--o{ BOOKING      : "预约·结算·现金流·固定支出"
    sys_tenant ||--o{ PROMOTION    : "推广分销·激励·优惠券"
    sys_tenant ||--o{ RATING       : "评价·点赞"
    sys_tenant ||--o{ COMPLIANCE   : "语音·敏感词·图片合规"
    sys_tenant ||--o{ BILLING      : "商业化计费·License·审计"
    sys_tenant ||--o{ PLATFORM     : "平台底座·多租户"

    sys_user   ||--o{ IDENT_ROLE   : "用户身份"
    sys_user   ||--o{ BOOKING      : "顾客/员工"
    sys_user   ||--o{ PROMOTION    : "推广人/被拉新人"
    sys_user   ||--o{ RATING       : "评价人/被评价人"

    PLATFORM {
        bigint id PK "占位-见域1"
    }
    IDENT_ROLE {
        bigint id PK "占位-见域2"
    }
    SERVICE {
        bigint id PK "占位-见域3"
    }
    BOOKING {
        bigint id PK "占位-见域4"
    }
    PROMOTION {
        bigint id PK "占位-见域5"
    }
    RATING {
        bigint id PK "占位-见域6"
    }
    COMPLIANCE {
        bigint id PK "占位-见域7"
    }
    BILLING {
        bigint id PK "占位-见域8"
    }
```

---

## 三、域 1 · 平台底座与多租户（M1_05 / M1_06 / M8）

```mermaid
erDiagram
    sys_tenant ||--o| tenant_biz_config  : "单店覆盖 1:1"
    sys_tenant ||--o{ tenant_theme       : "主题(可活动级)"
    sys_tenant ||--o{ user_tenant_bind   : "openid归属"
    biz_bundle ||--o{ feature_flag       : "业态声明开关(biz_code)"
    biz_bundle ||--o{ sys_tenant         : "业态包(biz_code)"
    sys_tenant ||--o{ portal_access_log  : "引导态访问审计(resolved_tenant_id)"

    sys_tenant {
        bigint  id PK "租户/门店ID"
        varchar company_name "门店名称"
        varchar biz_code FK "业态包→biz_bundle"
        varchar tenant_type "micro=小微版"
        varchar version_type "micro_lite/micro_pro"
        varchar industry_code FK "行业→industry_template"
        varchar access_channel "wechat=微信通道"
        varchar app_theme_key "小程序全局主题"
        varchar edition "版本编码"
        date    expire_date "到期日"
    }
    biz_bundle {
        varchar biz_code PK "fund_control/boardgame/fresh_food"
        json    menus "业态菜单声明"
        json    feature_flags "业态开放开关数组"
        json    copy "文案映射"
        varchar engine_strategy "成团引擎策略Bean"
    }
    feature_flag {
        bigint  id PK
        varchar code UK "FEAT_*开关编码"
        varchar flag_group "A~F分组"
        varchar biz_code FK "所属业态(NULL=通用)"
        tinyint basic_enabled "基础版"
        tinyint advanced_enabled "高级版"
        tinyint pro_enabled "专业版"
        tinyint micro_enabled "小微版"
    }
    tenant_biz_config {
        bigint id PK
        bigint tenant_id FK,UK "→sys_tenant"
        json   module_overrides "单店覆盖配置"
    }
    tenant_theme {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  activity_id FK "→promotion_activity(NULL=租户默认)"
        varchar preset_key "主题预设"
        varchar logo_rendered_url "Logo成品"
        varchar rendered_poster_url "海报成品"
    }
    user_tenant_bind {
        bigint  id PK
        varchar openid "微信openid"
        bigint  tenant_id FK "→sys_tenant"
        bigint  user_id FK "→sys_user(可空)"
        tinyint is_default "默认门店"
    }
    portal_access_log {
        bigint  id PK
        varchar client_ip "来源IP"
        varchar openid "微信openid"
        bigint  resolved_tenant_id FK "命中租户→sys_tenant"
        tinyint success "是否命中"
    }
```

**说明**：`biz_bundle / feature_flag` 为【系统级】共享表；`tenant_biz_config / tenant_theme / user_tenant_bind` 为【租户级】；`portal_access_log` 为【系统级】仅追加审计表。三维能力模型：`有效功能 = feature_flag(edition) ∩ biz_bundle.feature_flags ∩ License`。

---

## 四、域 2 · 身份 · 角色 · 行业模板 · 页面（M15_01 / M15_02 / M15_03）

```mermaid
erDiagram
    sys_user ||--o{ user_tenant_role     : "一人多租户多角色"
    sys_tenant ||--o{ user_tenant_role   : "租户下角色分配"
    sys_role ||--o{ user_tenant_role     : "role_code"
    sys_tenant ||--o{ tenant_role_mapping: "角色挑选/改名/启停"
    sys_role ||--o{ tenant_role_mapping  : "role_code"
    industry_template ||--o{ industry_role_map : "行业→业务角色"
    industry_template ||--o{ sys_tenant  : "行业模板(industry_code)"
    role_template ||--o{ sys_role        : "版本×角色配方驱动菜单"
    sys_tenant ||--o{ page_block_config  : "模块化页面区块"
    sys_tenant ||--o{ share_scene        : "扫码分享场景"

    sys_user {
        bigint user_id PK "手机号为全局身份锚点"
        varchar user_name "登录名"
        varchar phonenumber "手机号(唯一身份)"
    }
    sys_role {
        bigint role_id PK
        varchar role_key "boss/salesman/customer/platform_admin"
        bigint  tenant_id FK "NULL=共享标准角色"
        varchar tenant_type_scope "micro/enterprise/both"
    }
    user_tenant_role {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  user_id FK "→sys_user"
        varchar role_code FK "→sys_role.role_key"
        tinyint is_primary "主角色"
    }
    tenant_role_mapping {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar role_code FK "→sys_role.role_key"
        varchar display_name "租户内显示名(助教/顾客)"
    }
    role_template {
        bigint  id PK
        varchar version_type UK "micro/micro_lite/micro_pro/basic..."
        varchar role_code UK "标准角色编码"
        json    menu_keys "菜单权限集合"
        json    block_keys "小程序区块集合"
    }
    industry_template {
        bigint  id PK
        varchar industry_code UK "board_game/gym/spa..."
        varchar industry_name "行业名称"
        json    preset_config "预置分类/结算/单位/项目/房间"
        tinyint voice_enabled "语音总开关"
    }
    industry_role_map {
        bigint  id PK
        varchar industry_code FK "→industry_template"
        varchar biz_role_name "业务角色显示名"
        varchar role_code FK "映射标准角色"
    }
    page_block_config {
        bigint  id PK
        bigint  tenant_id FK "0=系统默认模板"
        varchar role_code "标准角色"
        varchar page_key "home/mine/booking"
        varchar block_key "区块组件key"
        varchar module_type "micro/enterprise"
    }
    share_scene {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar scene_id UK "小程序scene参数"
        varchar biz_type "shop/activity/booking"
        bigint  biz_id "业务ID(活动/服务)"
    }
```

**说明**：`role_template / industry_template / industry_role_map` 为【系统级】共享表（无 `tenant_id`）；`user_tenant_role / tenant_role_mapping / page_block_config / share_scene` 为【租户级】。手机号即身份：一个手机号 = 一个 `sys_user`，`user_tenant_role` 支持「一人多租户、一租户多角色」。

---

## 五、域 3 · 服务 · 定价 · 资源（M15_04 / M21_02 / M21_03）

```mermaid
erDiagram
    sys_tenant ||--o{ service_category   : "服务分类"
    service_category ||--o{ service_item : "分类下项目/房间"
    service_item ||--o{ price_change_log : "改价留痕"
    sys_user ||--o| staff_pricing        : "助教小时单价 1:1"
    service_item ||--o{ resource_status_log : "房间状态流转"
    sys_user ||--o{ resource_status_log  : "助教状态流转"
    booking_order ||--o{ resource_status_log : "预约触发状态变更"

    service_category {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar name "分类名(房间/助教/蔬菜)"
        varchar industry_type FK "→industry_template.industry_code"
        varchar category_type "service/space(房间)"
    }
    service_item {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  category_id FK "→service_category"
        varchar name "项目/房间名"
        decimal price "单价"
        varchar pricing_type "duration/weight/times/person/portion"
        int     capacity "可容纳人数(房间用)"
        varchar resource_status "idle/reserved/in_use"
    }
    price_change_log {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  item_id FK "→service_item"
        decimal old_price "调价前"
        decimal new_price "调价后"
        bigint  operator_id "操作人"
    }
    staff_pricing {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  user_id FK,UK "助教→sys_user"
        decimal hourly_rate "小时单价"
        tinyint enabled "启用"
    }
    resource_status_log {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar resource_type "staff/room"
        bigint  resource_id "助教=sys_user/房间=service_item"
        varchar from_status "变更前"
        varchar to_status "变更后"
        bigint  booking_id FK "→booking_order(可空)"
    }
```

**说明**：全部为【租户级】表。房间 = `category_type=space` 分类下的 `service_item`（零新表）；助教价格用 `staff_pricing`，房间价格复用 `service_item.price`。资源三态：`idle → reserved → in_use → idle`。

---

## 六、域 4 · 预约 · 结算 · 现金流 · 固定支出（M15_05 / M15_13 / M15_21 / M18_02 / M18_03）

```mermaid
erDiagram
    sys_tenant ||--o{ booking_order       : "预约订单"
    sys_user ||--o{ booking_order         : "顾客下单"
    sys_user ||--o{ booking_order         : "员工服务"
    service_item ||--o{ booking_order     : "预约项目"
    booking_order ||--o{ cash_flow        : "收入记账"
    sys_user ||--o{ staff_settlement      : "被结算员工"
    staff_settlement ||--o{ cash_flow     : "支出记账"
    sys_tenant ||--o{ micro_daily_revenue : "每日营收快照 1:N"
    sys_tenant ||--o{ monthly_fixed_cost  : "每月固定支出"
    period_rule ||--o{ monthly_fixed_cost : "周期分摊"
    fixed_cost_tag ||--o{ period_rule     : "标签→周期规则"
    sys_tenant ||--o| booking_policy      : "预约策略 1:1"
    sys_user ||--o| customer_credit       : "顾客信誉 1:1"
    sys_user ||--o{ no_show_record        : "爽约明细"
    booking_order ||--o{ no_show_record   : "爽约订单(order_no)"

    booking_order {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar order_no "订单编号"
        bigint  customer_user_id FK "顾客→sys_user"
        varchar customer_phone "顾客手机号"
        bigint  staff_user_id FK "员工→sys_user"
        bigint  service_item_id FK "→service_item"
        decimal amount "订单金额"
        varchar pay_status "PENDING_PAY/RECEIVED(无在线支付)"
        varchar order_status "PENDING_SERVICE/COMPLETED/CANCELLED/NO_SHOW"
        datetime expire_time "过期判定(booking_time+15min)"
    }
    staff_settlement {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar settle_no "结算单号"
        bigint  staff_user_id FK "→sys_user"
        date    period_start "周期起"
        date    period_end "周期止"
        decimal total_amount "应发金额"
        varchar pay_status "PENDING/PAID_OFFLINE(线下发放)"
    }
    cash_flow {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar flow_type "income/expense"
        decimal amount "金额"
        bigint  related_order_id FK "→booking_order/staff_settlement"
        varchar related_type "booking/settlement/manual"
        date    occur_date "发生日期"
    }
    micro_daily_revenue {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant"
        date    revenue_date UK "营收日期"
        decimal gross_income "当日收入"
        decimal total_expense "当日支出"
        decimal net_profit "净利润"
    }
    monthly_fixed_cost {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar name UK "支出项(房租/水电/工资)"
        varchar category "rent/utility/staff_salary..."
        decimal amount "金额"
        varchar effective_month UK "生效月份YYYY-MM"
        bigint  period_rule_id FK "→period_rule"
    }
    fixed_cost_tag {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar tag_name UK "标签名"
        varchar color "展示颜色"
    }
    period_rule {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar rule_name UK "规则名"
        decimal period_value "周期数值"
        varchar period_unit "DAY/WEEK/MONTH/QUARTER/YEAR"
        bigint  tag_id FK "→fixed_cost_tag"
    }
    booking_policy {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant(每租户一条)"
        tinyint customer_self_booking "允许自助预约"
        int     advance_booking_days "提前天数"
        int     no_show_threshold "爽约阈值"
    }
    customer_credit {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant"
        bigint  user_id FK,UK "顾客→sys_user"
        int     no_show_count "累计爽约次数"
        tinyint is_blacklisted "满3次自动拉黑"
        int     version "乐观锁"
    }
    no_show_record {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  user_id FK "顾客→sys_user"
        varchar order_no FK "→booking_order.order_no"
        varchar marked_by "system/boss"
    }
```

**说明**：全部为【租户级】表。V1 零支付合规——`booking_order.pay_status` 仅 `PENDING_PAY/RECEIVED`，`staff_settlement.pay_status` 仅 `PENDING/PAID_OFFLINE`，全系统无在线支付/分账打款，`cash_flow` 为纯记账口径。防爽约仅 `micro_pro` 生效。盈亏公式：`本月盈利 = 本月收入 − (流水支出 + Σ本月固定支出)`。

---

## 七、域 5 · 推广分销 · 激励 · 优惠券（M15_11 / M16 / M18_04）

```mermaid
erDiagram
    sys_tenant ||--o{ promotion_activity   : "活动/预告/海报"
    sys_user ||--o{ promotion_code         : "一人一码(门店/个人)"
    sys_user ||--o| promotion_attribution  : "被归因顾客(当前推广人)"
    sys_tenant ||--o{ promotion_reward_config : "拉新奖励配置"
    coupon ||--o{ promotion_reward_config  : "顾客奖励关联券"
    sys_user ||--o{ promotion_reward_record: "推广人受奖"
    booking_order ||--o{ promotion_reward_record : "首消订单触发"
    coupon ||--o{ coupon_record            : "券模板→发放"
    sys_user ||--o{ coupon_record          : "持券顾客"
    promotion_reward_record ||--o| coupon_record : "顾客券发放"
    promotion_activity ||--o{ first_order_record : "按活动独立判断"
    sys_user ||--o{ first_order_record     : "顾客首单(手机号)"
    booking_order ||--o{ first_order_record: "首次消费订单"

    promotion_activity {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar title "活动标题"
        varchar activity_type "activity/notice/poster"
        varchar theme_key "背景主题"
        bigint  selected_project_id "关联项目"
        varchar status "DRAFT/PUBLISHED/ENDED"
    }
    promotion_code {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  user_id FK "推广人→sys_user"
        varchar role_code "boss/salesman/customer"
        varchar code_type UK "store/personal"
        varchar scene_param "小程序scene(≤32)"
        char    status "0启用/1禁用"
    }
    promotion_attribution {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant"
        bigint  customer_user_id FK,UK "被归因顾客→sys_user"
        bigint  promoter_user_id FK "当前推广人(NULL=自然流量)"
        datetime last_scan_time "交接点(锁定不回溯)"
    }
    promotion_reward_config {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant"
        varchar target_role UK "staff(现金)/customer(券)"
        varchar reward_type "cash/coupon"
        decimal reward_value "奖励内容(唯一可变)"
        bigint  coupon_id FK "→coupon"
        varchar trigger_rule "固定=NEW_CUSTOMER"
    }
    promotion_reward_record {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant"
        bigint  promoter_user_id FK "受奖推广人→sys_user"
        bigint  customer_user_id FK,UK "被拉新顾客(仅一次)"
        bigint  first_order_id FK "→booking_order"
        bigint  coupon_record_id FK "→coupon_record"
        varchar issue_status "PENDING/PAID_OFFLINE/ISSUED"
    }
    coupon {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar name "券名称"
        varchar type "discount/gift/cash"
        decimal value "券值"
        int     valid_days "有效天数"
    }
    coupon_record {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  user_id FK "持券顾客→sys_user"
        bigint  coupon_id FK "→coupon"
        varchar source "promotion(拉新)/activity(活动)"
        varchar status "UNUSED/USED/EXPIRED"
    }
    first_order_record {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant"
        bigint  activity_id FK,UK "→promotion_activity"
        varchar customer_phone UK "手机号唯一识别"
        bigint  customer_user_id FK "→sys_user"
        bigint  first_order_id FK "→booking_order"
    }
```

**说明**：全部为【租户级】表。仅小微版启用（功能开关 `FEAT_PROMOTION_DISTRIBUTION`）。核心规则：一人一码永久有效；归因「谁最后扫码业绩归谁」，交接前锁定不回溯；拉新奖励同一被拉新顾客仅触发一次（`uk_tenant_customer`）；首单以「手机号 + 活动」独立判断，与推广归属解耦。

---

## 八、域 6 · 评价 · 点赞（M17_01）

```mermaid
erDiagram
    sys_user ||--o{ user_rating : "被评价人(服务人员)"
    sys_user ||--o{ user_rating : "评价人(顾客)"
    booking_order ||--o{ user_rating : "关联订单"
    sys_user ||--o{ user_like   : "被点赞人"
    sys_user ||--o{ user_like   : "点赞人"

    user_rating {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  target_user_id FK "被评价人→sys_user(禁自评)"
        bigint  author_user_id FK "评价人→sys_user"
        tinyint rating "1~5星"
        varchar content "评价内容"
        varchar scene "booking_complete/service_done/visit_done"
        bigint  order_id FK "→booking_order(可空)"
        varchar status "NORMAL/SHIELDED"
        varchar reply_content "老板回复(仅一次)"
    }
    user_like {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  target_user_id FK "被点赞人→sys_user"
        bigint  author_user_id FK "点赞人→sys_user"
        varchar scene "service/staff(toggle切换)"
    }
```

**说明**：【租户级】表，仅 `micro_pro` 启用（功能开关 `FEAT_RATING`）。防重唯一键：`user_rating(tenant_id,target_user_id,author_user_id,scene,order_id)`、`user_like(tenant_id,target_user_id,author_user_id,scene)`；`SHIELDED` 评价不计入综合评分。

---

## 九、域 7 · 合规（语音 / 敏感词 / 图片）（M18_01 / M18_10）

```mermaid
erDiagram
    sys_tenant ||--o{ voice_compliance_rule   : "语音合规规则"
    sys_tenant ||--o| voice_recording_policy  : "录音留存策略 1:1"
    voice_compliance_rule ||--o{ voice_compliance_hit : "规则命中"
    promotion_activity ||--o{ voice_compliance_hit : "活动语音命中"
    sys_tenant ||--o{ sensitive_word          : "租户自定义敏感词"
    sys_user ||--o{ sensitive_word_hit_log    : "触发用户"
    sys_tenant ||--o{ image_compliance_config : "图片合规覆盖"
    sys_tenant ||--o{ sensitive_word_library  : "租户自定义(旧表)"

    voice_compliance_rule {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant"
        varchar activity_type UK "活动类型"
        tinyint enabled "语音合规开关"
        varchar sensitive_word_override "INHERIT/OVERRIDE/MERGE"
        int     max_voice_duration "最大语音时长(秒)"
    }
    voice_recording_policy {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant(每租户一条)"
        int     max_retention_days "留存天数(≤全局上限)"
        varchar storage_path "存储路径"
    }
    voice_compliance_hit {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar hit_type "SENSITIVE_WORD/NO_TRANSCRIPTION/POLICY_VIOLATION"
        bigint  rule_id FK "→voice_compliance_rule"
        bigint  activity_id FK "→promotion_activity"
        varchar result "PASS/FAIL"
        varchar status "PENDING/ACKNOWLEDGED/DISMISSED"
    }
    sensitive_word {
        bigint  id PK
        bigint  tenant_id FK "0=全局底库/非0=租户自定义"
        varchar word UK "敏感词"
        varchar category UK "POLITICAL/PORN/VIOLENCE..."
        varchar match_mode "exact/fuzzy/regex"
        varchar source "global/tenant"
    }
    sensitive_word_hit_log {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  user_id FK "触发用户→sys_user"
        varchar input_channel "text/voice_ocr/image_ocr"
        varchar hit_word "命中词"
        varchar result "PASS/FAIL"
        varchar biz_type "rating/activity/poster/voice"
    }
    image_compliance_config {
        bigint  id PK
        bigint  tenant_id FK,UK "0=全局默认/非0=租户覆盖"
        varchar violation_type UK "porn/violence/politics"
        decimal confidence_threshold "置信度阈值"
        tinyint ocr_enabled "OCR路"
        tinyint visual_api_enabled "视觉审核路"
    }
    sensitive_word_library {
        bigint  id PK
        bigint  tenant_id FK "NULL=平台级/非NULL=租户"
        varchar word UK "敏感词(旧表)"
        varchar level "WARN/BLOCK"
    }
```

**说明**：`sensitive_word / sensitive_word_hit_log / image_compliance_config` 为【混合级】（`tenant_id=0` 全局底库，走条件查询而非白名单）；`voice_*` 三表为【租户级】；`sensitive_word_library` 为 M18_01 旧表，M18_11 已幂等迁移至 `sensitive_word`（保留兼容）。敏感词引擎三路共用（文本 / 语音 ASR / 图片 OCR），由 `FEAT_SENSITIVE_WORD / FEAT_IMAGE_COMPLIANCE` 开关控制。

---

## 十、域 8 · 商业化计费 · License · 审计（M14 / M3_05 / M18_06 / M18_07）

```mermaid
erDiagram
    sys_billing_plan ||--o{ sys_billing_order : "套餐→订单"
    sys_tenant ||--o{ sys_billing_order       : "租户计费订单"
    sys_billing_order ||--o| sys_license      : "签发License"
    sys_tenant ||--o| sys_virtual_account     : "虚拟账户 1:1"
    sys_tenant ||--o| sys_profit_sharing_rule : "分账规则 1:1"
    sys_billing_order ||--o{ sys_profit_sharing_record : "订单分账"
    sys_tenant ||--o{ sys_withdrawal_request  : "提现申请"
    sys_tenant ||--o{ sys_settlement_bill     : "结算对账单"
    sys_tenant ||--o{ sys_subscribe_quota     : "订阅消息配额"
    sys_agreement_version ||--o{ sys_user_agreement_log : "协议留痕"
    sys_user ||--o{ sys_user_agreement_log    : "同意用户"
    sys_user ||--o{ sys_account_action_log    : "账户操作留痕"
    sys_license_template ||--o{ sys_license   : "模板→签发"
    sys_tenant ||--o{ sys_license             : "租户License"

    sys_billing_plan {
        varchar plan_code PK "trial/first_month/monthly/yearly"
        decimal price "价格"
        int     days "有效天数"
        varchar license_type "TRIAL/FORMAL"
        varchar version_type "分档(小微版)"
    }
    sys_billing_order {
        bigint  id PK
        varchar order_no UK "订单号"
        bigint  tenant_id FK "→sys_tenant"
        varchar plan_code FK "→sys_billing_plan"
        decimal amount "金额"
        varchar license_id FK "→sys_license"
        varchar pay_status "PENDING/PAID/REFUNDED/CANCELLED"
    }
    sys_virtual_account {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant(每租户一个)"
        decimal balance "可用余额"
        decimal frozen_balance "冻结余额"
        int     version "乐观锁"
    }
    sys_profit_sharing_rule {
        bigint  id PK
        bigint  tenant_id FK,UK "→sys_tenant"
        decimal platform_rate "平台服务费率"
        decimal tenant_rate "租户应得率"
    }
    sys_profit_sharing_record {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        varchar order_no FK "→sys_billing_order"
        decimal platform_amount "平台服务费"
        decimal tenant_amount "租户应得"
    }
    sys_withdrawal_request {
        bigint  id PK
        varchar withdraw_no UK "提现单号"
        bigint  tenant_id FK "→sys_tenant"
        decimal amount "提现金额"
        varchar status "PENDING/APPROVED/REJECTED/PAID"
    }
    sys_settlement_bill {
        bigint  id PK
        varchar bill_no UK "对账单号"
        bigint  tenant_id FK "→sys_tenant"
        varchar period "账期yyyy-MM"
        decimal tenant_amount "租户应得合计"
    }
    sys_subscribe_quota {
        bigint id PK
        bigint tenant_id FK "→sys_tenant"
        int    quota "一次性订阅消息配额"
    }
    sys_agreement_version {
        bigint  id PK
        varchar agreement_type "协议类型"
        varchar version "版本号"
    }
    sys_user_agreement_log {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  user_id FK "→sys_user"
        bigint  agreement_id FK "→sys_agreement_version"
    }
    sys_account_action_log {
        bigint  id PK
        bigint  tenant_id FK "→sys_tenant"
        bigint  user_id FK "→sys_user"
        varchar action_type "导出/注销"
    }
    sys_license {
        varchar license_id PK "License编号"
        bigint  tenant_id FK "NULL=平台级/非NULL=租户级"
        varchar edition "授权版本"
        date    expire_date "到期日"
    }
    sys_license_template {
        bigint  id PK
        varchar edition UK "BASIC/ADVANCED/PROFESSIONAL"
        varchar license_type UK "TRIAL/FORMAL"
        varchar template_content "模板内容"
    }
    sys_alert_rule {
        bigint  id PK
        varchar rule_code UK "LICENSE_EXPIRE/COMPLIANCE_HIT..."
        varchar alert_level "告警级别"
        tinyint enabled "启用"
    }
```

**说明**：`sys_billing_plan / sys_agreement_version / sys_license_template / sys_alert_rule` 为【系统级】共享表；其余为【租户级】或平台级（`sys_license.tenant_id` 为 NULL 时是平台级）。**严禁自动扣款**：套餐到期只停用不扣款；`platform_amount + tenant_amount = 订单金额`。小微版分档套餐（`micro_lite_free` / `micro_pro` 四档）通过 `sys_billing_plan.version_type` 区分。

---

## 十一、实体清单（共 48 张表）

| # | 实体表 | 中文名 | 分层 | 所属域 | 来源脚本 |
|---|--------|--------|------|--------|----------|
| 1 | `sys_tenant` | 租户/门店 | 租户级(锚点) | 平台底座 | 若依原生 + M1_06/M8_01/M15_10/M15_20 |
| 2 | `sys_user` | 用户(手机号身份) | 系统级(锚点) | 身份角色 | 若依原生 |
| 3 | `sys_role` | 角色 | 混合(tenant_id可空) | 身份角色 | 若依原生 + M2_01/M2_04/M15_09 |
| 4 | `biz_bundle` | 业态包 | 系统级 | 平台底座 | M1_05 |
| 5 | `feature_flag` | 功能开关 | 系统级 | 平台底座 | M1_05 + M10_05/M16_03/M17_01/M18_10 |
| 6 | `tenant_biz_config` | 单店覆盖 | 租户级 | 平台底座 | M1_05 |
| 7 | `tenant_theme` | 租户主题 | 租户级 | 平台底座 | M1_05 + M15_19b |
| 8 | `user_tenant_bind` | openid多租户绑定 | 租户级 | 平台底座 | M1_05 |
| 9 | `portal_access_log` | 门户访问审计 | 系统级 | 平台底座 | M8_02 |
| 10 | `user_tenant_role` | 用户×租户×角色 | 租户级 | 身份角色 | M15_01 |
| 11 | `tenant_role_mapping` | 租户角色映射 | 租户级 | 身份角色 | M15_01 |
| 12 | `role_template` | 角色模板 | 系统级 | 身份角色 | M15_01 |
| 13 | `industry_template` | 行业模板 | 系统级 | 身份角色 | M15_02 + M15_22/M15_25 |
| 14 | `industry_role_map` | 行业角色映射 | 系统级 | 身份角色 | M15_02 |
| 15 | `page_block_config` | 页面区块配置 | 租户级 | 身份角色 | M15_03 |
| 16 | `share_scene` | 分享场景 | 租户级 | 身份角色 | M15_03 |
| 17 | `service_category` | 服务分类 | 租户级 | 服务定价 | M15_04 + M15_23 |
| 18 | `service_item` | 服务项目/房间 | 租户级 | 服务定价 | M15_04 + M15_24/M21_02 |
| 19 | `price_change_log` | 改价记录 | 租户级 | 服务定价 | M15_04 |
| 20 | `staff_pricing` | 助教小时定价 | 租户级 | 服务定价 | M21_03 |
| 21 | `resource_status_log` | 资源状态日志 | 租户级 | 服务定价 | M21_02 |
| 22 | `booking_order` | 预约订单 | 租户级 | 预约结算 | M15_05 + M15_13 |
| 23 | `staff_settlement` | 人员结算 | 租户级 | 预约结算 | M15_05 |
| 24 | `cash_flow` | 收支流水 | 租户级 | 预约结算 | M15_05 |
| 25 | `micro_daily_revenue` | 每日营收快照 | 租户级 | 预约结算 | M15_05 |
| 26 | `monthly_fixed_cost` | 每月固定支出 | 租户级 | 预约结算 | M15_21 + M20_06 |
| 27 | `fixed_cost_tag` | 固定支出标签 | 租户级 | 预约结算 | M18_02 |
| 28 | `period_rule` | 周期规则 | 租户级 | 预约结算 | M18_02 |
| 29 | `booking_policy` | 预约策略 | 租户级 | 预约结算 | M18_03 |
| 30 | `customer_credit` | 顾客信誉/黑名单 | 租户级 | 预约结算 | M15_13 |
| 31 | `no_show_record` | 爽约明细 | 租户级 | 预约结算 | M15_13 |
| 32 | `promotion_activity` | 活动/预告/海报 | 租户级 | 推广分销 | M15_11 + M15_19/M19_07 |
| 33 | `promotion_code` | 推广码 | 租户级 | 推广分销 | M16_01 |
| 34 | `promotion_attribution` | 推广归因 | 租户级 | 推广分销 | M16_01 |
| 35 | `promotion_reward_config` | 拉新奖励配置 | 租户级 | 推广分销 | M16_02 + M18_04 |
| 36 | `promotion_reward_record` | 拉新奖励发放记录 | 租户级 | 推广分销 | M16_02 |
| 37 | `coupon` | 优惠券模板 | 租户级 | 推广分销 | M16_02 |
| 38 | `coupon_record` | 顾客券记录 | 租户级 | 推广分销 | M16_02 |
| 39 | `first_order_record` | 首单识别记录 | 租户级 | 推广分销 | M16_03 |
| 40 | `user_rating` | 评价 | 租户级 | 评价点赞 | M17_01 |
| 41 | `user_like` | 点赞 | 租户级 | 评价点赞 | M17_01 |
| 42 | `voice_compliance_rule` | 语音合规规则 | 租户级 | 合规 | M18_01 + M20_07 |
| 43 | `voice_recording_policy` | 录音留存策略 | 租户级 | 合规 | M18_01 |
| 44 | `voice_compliance_hit` | 语音合规命中 | 租户级 | 合规 | M18_01 + M20_08 |
| 45 | `sensitive_word_library` | 敏感词库(旧) | 混合级 | 合规 | M18_01 |
| 46 | `sensitive_word` | 敏感词(新) | 混合级 | 合规 | M18_10 + M18_11 |
| 47 | `sensitive_word_hit_log` | 敏感词命中日志 | 混合级 | 合规 | M18_10 |
| 48 | `image_compliance_config` | 图片合规配置 | 混合级 | 合规 | M18_10 |
| 49 | `sys_billing_plan` | 计费套餐 | 系统级 | 商业化计费 | M14_01 + M15_10 |
| 50 | `sys_billing_order` | 计费订单 | 租户级 | 商业化计费 | M14_01 |
| 51 | `sys_virtual_account` | 虚拟账户 | 租户级 | 商业化计费 | M14_01 |
| 52 | `sys_profit_sharing_rule` | 分账规则 | 租户级 | 商业化计费 | M14_01 |
| 53 | `sys_profit_sharing_record` | 分账记录 | 租户级 | 商业化计费 | M14_01 |
| 54 | `sys_withdrawal_request` | 提现申请 | 租户级 | 商业化计费 | M14_01 |
| 55 | `sys_settlement_bill` | 结算对账单 | 租户级 | 商业化计费 | M14_01 |
| 56 | `sys_subscribe_quota` | 订阅消息配额 | 租户级 | 合规商业化 | M14_02 |
| 57 | `sys_agreement_version` | 协议版本 | 系统级 | 合规商业化 | M14_02 |
| 58 | `sys_user_agreement_log` | 用户协议留痕 | 租户级 | 合规商业化 | M14_02 |
| 59 | `sys_account_action_log` | 账户操作留痕 | 租户级 | 合规商业化 | M14_02 |
| 60 | `sys_license` | License | 混合(tenant_id可空) | 商业化计费 | 若依原生 + M3_05 |
| 61 | `sys_license_template` | License模板 | 系统级 | 商业化计费 | M18_06 |
| 62 | `sys_alert_rule` | 告警规则 | 系统级 | 运维审计 | M18_07 |

> 注：清单共列出 62 项（含 `sys_user / sys_role / sys_license` 等若依原生并被小微版扩展/引用的锚点表）；其中小微版专属新建增量表约 40 张，其余为平台底座/商业化共享表。菜单表（`sys_menu / sys_role_menu`）、部门表（`sys_dept`）、定时任务表（`xxl_job_*`）属框架基础设施，非业务实体，未纳入 ER 图。

---

## 十二、跨域关键关联链路（业务闭环）

1. **建店链路**：`industry_template`（选行业）→ `sys_tenant`（建租户，回填 `industry_code / version_type`）→ `role_template`（按 `version_type × role_code` 克隆 `sys_role` + `sys_role_menu`）→ `tenant_role_mapping`（改名/启停）→ `page_block_config`（克隆小程序页面）。
2. **经营闭环**：`service_category → service_item`（上架项目/房间）→ `booking_order`（顾客预约，到店付）→ 标记已收款写 `cash_flow(income)` → 完成服务 → `staff_settlement`（周期结算）→ 标记线下发放写 `cash_flow(expense)` → `micro_daily_revenue`（每日快照）→ 首页盈亏 = 收入 −（流水支出 + `monthly_fixed_cost`）。
3. **推广激励闭环**：`promotion_code`（生成推广码）→ 顾客扫码 upsert `promotion_attribution`（归因）→ 首消写 `first_order_record`（按活动判断）→ 查 `promotion_reward_config` → 员工现金写 `promotion_reward_record(PENDING→PAID_OFFLINE)`；顾客券写 `coupon_record(source=promotion)`。
4. **防爽约闭环**：`booking_order(expire_time 已过仍 PENDING_SERVICE)` → `noShowCheckJob` 标记 `NO_SHOW` → `no_show_record` + `customer_credit.no_show_count+1` → 满 3 次 `is_blacklisted=1` → 老板 `micro:customer:unblacklist` 解除。
5. **合规闭环**：活动语音/图片上传 → `voice_compliance_rule / image_compliance_config` 规则 → 敏感词引擎（`sensitive_word`）三路检测 → 命中写 `voice_compliance_hit / sensitive_word_hit_log` → Admin `sys_alert_rule` 告警。

---

*本 ER 图为逻辑实体关系示意，字段以各 `M*` 建表脚本为准；如需渲染请用支持 Mermaid 的 Markdown 预览器。*
