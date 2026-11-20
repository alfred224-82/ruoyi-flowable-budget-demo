-- =====================================================================
-- M10_04 Schema 页面引擎区块定义脚本（T10.6，v1.29 新增）
-- 版本：v1.0  日期：2026-09-16
-- 依据：《资金管控系统开发任务文档（系统管理）》v1.29 T10.6
--       《资金管控系统设计文档》v1.3 §1.8.1（三层架构·业态包层·Schema 页面引擎）
-- 规范：
--   1. 幂等：直接 UPDATE biz_bundle.schema_blocks（JSON 列），可重复执行
--   2. schema_blocks 由【字符串区块码数组】升级为【区块定义对象数组】，
--      供后端 TenantBizConfigServiceImpl#getRenderSchema 解析、前端 SchemaRenderer 声明式渲染
--   3. 区块定义契约（block）：
--        code    区块唯一编码（与 module_overrides 的 copy.<code> 对齐）
--        module  关联模块编码（与 tenant_biz_config.module_overrides.modules.<module> 对齐）
--        type    渲染类型：form（表单）/ list（列表）/ board（自定义看板，交前端插槽）
--        title   区块标题（可被 module_overrides.modules.<module>.copy.title 覆盖）
--        fields  form 类型的字段声明数组（key/label/type/required/visible/options/span/placeholder）
--        columns list 类型的列声明数组（key/label/width）
--   4. 单店覆盖叠加规则（运行时）：
--        modules.<module>.enabled=false          → 剔除整个区块
--        modules.<module>.copy.title             → 覆盖区块标题
--        modules.<module>.fields.<key>.visible   → false 剔除字段
--        modules.<module>.fields.<key>.required  → 覆盖必填
--        modules.<module>.fields.<key>.label     → 覆盖字段标签
-- =====================================================================

-- ----------------------------
-- 1. 资金管控业态包（fund_control）：项目表单 + 报销表单 + 收入列表
-- ----------------------------
UPDATE `biz_bundle` SET `schema_blocks` = JSON_ARRAY(
  JSON_OBJECT(
    'code', 'fund_project_form', 'module', 'project', 'type', 'form', 'title', '项目信息',
    'fields', JSON_ARRAY(
      JSON_OBJECT('key','projectName','label','项目名称','type','input','required',true,'span',12),
      JSON_OBJECT('key','amount','label','预算金额','type','number','required',true,'span',12),
      JSON_OBJECT('key','owner','label','负责人','type','input','span',12),
      JSON_OBJECT('key','startDate','label','开始日期','type','date','span',12),
      JSON_OBJECT('key','remark','label','备注','type','textarea','span',24)
    )
  ),
  JSON_OBJECT(
    'code', 'fund_expense_form', 'module', 'expense', 'type', 'form', 'title', '报销单',
    'fields', JSON_ARRAY(
      JSON_OBJECT('key','expenseType','label','费用类型','type','select','required',true,'span',12,
        'options', JSON_ARRAY(
          JSON_OBJECT('label','差旅费','value','travel'),
          JSON_OBJECT('label','招待费','value','entertain'),
          JSON_OBJECT('label','办公费','value','office')
        )),
      JSON_OBJECT('key','amount','label','报销金额','type','number','required',true,'span',12),
      JSON_OBJECT('key','invoiceNo','label','发票号','type','input','span',12),
      JSON_OBJECT('key','occurDate','label','发生日期','type','date','span',12)
    )
  ),
  JSON_OBJECT(
    'code', 'fund_income_list', 'module', 'income', 'type', 'list', 'title', '收入确认列表',
    'columns', JSON_ARRAY(
      JSON_OBJECT('key','projectName','label','项目','width',180),
      JSON_OBJECT('key','amount','label','金额','width',120),
      JSON_OBJECT('key','confirmDate','label','确认日期','width',140),
      JSON_OBJECT('key','status','label','状态','width',100)
    )
  )
) WHERE `biz_code` = 'fund_control';

-- ----------------------------
-- 2. 桌游轰趴业态包（boardgame）：活动表单 + 报名表单 + 满员看板（自定义）
-- ----------------------------
UPDATE `biz_bundle` SET `schema_blocks` = JSON_ARRAY(
  JSON_OBJECT(
    'code', 'activity_form', 'module', 'project', 'type', 'form', 'title', '桌局/活动',
    'fields', JSON_ARRAY(
      JSON_OBJECT('key','activityName','label','活动名称','type','input','required',true,'span',12),
      JSON_OBJECT('key','maxMember','label','成团人数','type','number','required',true,'span',12),
      JSON_OBJECT('key','startTime','label','开始时间','type','date','span',12),
      JSON_OBJECT('key','price','label','人均价格','type','number','span',12)
    )
  ),
  JSON_OBJECT(
    'code', 'signup_form', 'module', 'signup', 'type', 'form', 'title', '报名登记',
    'fields', JSON_ARRAY(
      JSON_OBJECT('key','customerName','label','报名人','type','input','required',true,'span',12),
      JSON_OBJECT('key','phone','label','手机号','type','input','required',true,'span',12),
      JSON_OBJECT('key','memberCount','label','随行人数','type','number','span',12)
    )
  ),
  JSON_OBJECT(
    'code', 'fullhouse_board', 'module', 'team', 'type', 'board', 'title', '拼桌成团看板'
  )
) WHERE `biz_code` = 'boardgame';

-- ----------------------------
-- 3. 生鲜团购业态包（fresh_food）：团购表单 + 下单表单 + 商品看板（自定义）
-- ----------------------------
UPDATE `biz_bundle` SET `schema_blocks` = JSON_ARRAY(
  JSON_OBJECT(
    'code', 'groupbuy_form', 'module', 'project', 'type', 'form', 'title', '团购/批次',
    'fields', JSON_ARRAY(
      JSON_OBJECT('key','batchName','label','团购批次','type','input','required',true,'span',12),
      JSON_OBJECT('key','targetQty','label','成团数量','type','number','required',true,'span',12),
      JSON_OBJECT('key','ladderPrice','label','阶梯价','type','number','span',12),
      JSON_OBJECT('key','freshUntil','label','保鲜期至','type','date','span',12)
    )
  ),
  JSON_OBJECT(
    'code', 'signup_form', 'module', 'signup', 'type', 'form', 'title', '下单登记',
    'fields', JSON_ARRAY(
      JSON_OBJECT('key','customerName','label','下单人','type','input','required',true,'span',12),
      JSON_OBJECT('key','qty','label','购买数量','type','number','required',true,'span',12),
      JSON_OBJECT('key','address','label','配送地址','type','textarea','span',24)
    )
  ),
  JSON_OBJECT(
    'code', 'fresh_product_block', 'module', 'team', 'type', 'board', 'title', '团购商品看板'
  )
) WHERE `biz_code` = 'fresh_food';

-- =====================================================================
-- 4. 验收查询
-- =====================================================================
-- 4.1 三业态包 schema_blocks 已升级为对象数组
-- SELECT biz_code, JSON_LENGTH(schema_blocks) AS block_cnt,
--        JSON_TYPE(JSON_EXTRACT(schema_blocks, '$[0]')) AS first_type
-- FROM biz_bundle WHERE biz_code IN ('fund_control','boardgame','fresh_food');
-- 预期：block_cnt>=3，first_type='OBJECT'
--
-- 4.2 首区块含 module/type 键（后端 getRenderSchema 依赖）
-- SELECT biz_code,
--        JSON_EXTRACT(schema_blocks, '$[0].module') AS m0,
--        JSON_EXTRACT(schema_blocks, '$[0].type')   AS t0
-- FROM biz_bundle WHERE biz_code='fund_control';
-- 预期：m0='"project"'，t0='"form"'
