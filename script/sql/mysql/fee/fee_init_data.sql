-- =============================================
-- 预算与核算映射优化（二期）初始化数据
-- 版本：v1.0
-- 日期：2026-07-13
-- =============================================

-- ----------------------------
-- 初始化用途标签预设数据
-- ----------------------------
INSERT INTO fee_purpose_tag (tag_code, tag_name, tag_icon, default_accounting_class, require_project, sort_order) VALUES
('DAILY_OPS', '日常运营', '🏢', 'EXPENSE', 0, 1),
('RD_INVEST', '研发投入', '🔬', 'EXPENSE', 0, 2),
('PROJECT_DELIVERY', '项目交付', '📋', 'COST', 1, 3),
('MANUFACTURE', '生产制造', '🏭', 'COST', 0, 4),
('MARKET_ACT', '市场活动', '📢', 'EXPENSE', 0, 5);

-- ----------------------------
-- 初始化映射规则数据
-- 规则说明：物资小类 + 用途标签 → 预算科目 + 会计大类 + 费用确认方式
-- cost_method: DIRECT(直接费用化) / DEPRECIATION(折旧) / ACCRUAL(计提摊销) / FEE_RATE(费率利息)
-- priority 数值越大优先级越高
-- ----------------------------
INSERT INTO fee_mapping_rule
  (rule_code, item_category, purpose_tag, budget_subject, accounting_class, is_fixed_asset, cost_method, priority, status, create_by, create_time)
VALUES
-- ========== 办公用品 ==========
('MR-OFFICE-DAILY-001',    'OFFICE_SUPPLIES',    'DAILY_OPS',         '3002001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-OFFICE-RD-001',       'OFFICE_SUPPLIES',    'RD_INVEST',         '3002001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-OFFICE-PROJECT-001',  'OFFICE_SUPPLIES',    'PROJECT_DELIVERY',  '2001001001', 'COST',  0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-OFFICE-MKT-001',      'OFFICE_SUPPLIES',    'MARKET_ACT',        '3001001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 差旅费用 ==========
('MR-TRAVEL-DAILY-001',    'TRAVEL_EXPENSES',    'DAILY_OPS',         '3002002', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-TRAVEL-PROJECT-001',  'TRAVEL_EXPENSES',    'PROJECT_DELIVERY',  '2001001001', 'COST',  0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-TRAVEL-MKT-001',      'TRAVEL_EXPENSES',    'MARKET_ACT',        '3001001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-TRAVEL-MFG-001',      'TRAVEL_EXPENSES',    'MANUFACTURE',       '2001001003', 'COST',  0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 通讯费用 ==========
('MR-COMM-DAILY-001',      'COMMUNICATION',      'DAILY_OPS',         '3002001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-COMM-RD-001',         'COMMUNICATION',      'RD_INVEST',         '3002001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 培训费用 ==========
('MR-TRAIN-DAILY-001',     'TRAINING',           'DAILY_OPS',         '3002001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-TRAIN-RD-001',        'TRAINING',           'RD_INVEST',         '3002001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 市场推广 ==========
('MR-MKT-MKT-001',         'MARKETING',          'MARKET_ACT',        '3001001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-MKT-DAILY-001',       'MARKETING',          'DAILY_OPS',         '3001004', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-MKT-PROJECT-001',     'MARKETING',          'PROJECT_DELIVERY',  '3001001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 设备采购（固定资产 → 折旧） ==========
('MR-EQUIP-DAILY-001',     'EQUIPMENT',          'DAILY_OPS',         '4002002', 'ASSET',   1, 'DEPRECIATION', 10, 'ENABLED', 'admin', NOW()),
('MR-EQUIP-RD-001',        'EQUIPMENT',          'RD_INVEST',         '4002002', 'ASSET',   1, 'DEPRECIATION', 10, 'ENABLED', 'admin', NOW()),
('MR-EQUIP-MFG-001',       'EQUIPMENT',          'MANUFACTURE',       '4002002', 'ASSET',   1, 'DEPRECIATION', 10, 'ENABLED', 'admin', NOW()),
('MR-EQUIP-PROJECT-001',   'EQUIPMENT',          'PROJECT_DELIVERY',  '4002002', 'ASSET',   1, 'DEPRECIATION', 10, 'ENABLED', 'admin', NOW()),

-- ========== 软件授权（无形资产 → 计提摊销） ==========
('MR-SW-DAILY-001',        'SOFTWARE_LICENSE',   'DAILY_OPS',         '3002001', 'EXPENSE', 1, 'ACCRUAL', 10, 'ENABLED', 'admin', NOW()),
('MR-SW-RD-001',           'SOFTWARE_LICENSE',   'RD_INVEST',         '3002001', 'EXPENSE', 1, 'ACCRUAL', 10, 'ENABLED', 'admin', NOW()),
('MR-SW-PROJECT-001',      'SOFTWARE_LICENSE',   'PROJECT_DELIVERY',  '2001001001', 'COST',  1, 'ACCRUAL', 10, 'ENABLED', 'admin', NOW()),

-- ========== 咨询服务 ==========
('MR-CONS-DAILY-001',      'CONSULTING',         'DAILY_OPS',         '3002005', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-CONS-RD-001',         'CONSULTING',         'RD_INVEST',         '3002005', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-CONS-PROJECT-001',    'CONSULTING',         'PROJECT_DELIVERY',  '2001001003', 'COST',  0, 'ACCRUAL', 10, 'ENABLED', 'admin', NOW()),

-- ========== 维修保养 ==========
('MR-MAINT-DAILY-001',     'MAINTENANCE',        'DAILY_OPS',         '3002004', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-MAINT-MFG-001',       'MAINTENANCE',        'MANUFACTURE',       '2001001003', 'COST',  0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 通讯费用补全（项目交付/生产制造场景） ==========
('MR-COMM-PROJECT-001',    'COMMUNICATION',      'PROJECT_DELIVERY',  '2001001003', 'COST',  0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-COMM-MFG-001',        'COMMUNICATION',      'MANUFACTURE',       '2001001003', 'COST',  0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-COMM-MKT-001',        'COMMUNICATION',      'MARKET_ACT',        '3001001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 培训费用补全（项目交付/市场活动场景） ==========
('MR-TRAIN-PROJECT-001',   'TRAINING',           'PROJECT_DELIVERY',  '2001001003', 'COST',  0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-TRAIN-MFG-001',       'TRAINING',           'MANUFACTURE',       '2001001003', 'COST',  0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-TRAIN-MKT-001',       'TRAINING',           'MARKET_ACT',        '3001001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 办公用品补全（生产制造场景） ==========
('MR-OFFICE-MFG-001',      'OFFICE_SUPPLIES',    'MANUFACTURE',       '2001001003', 'COST',  0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 差旅费用补全（研发投入场景） ==========
('MR-TRAVEL-RD-001',       'TRAVEL_EXPENSES',    'RD_INVEST',         '3002002', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 咨询服务补全（日常运营/市场活动场景） ==========
('MR-CONS-MKT-001',        'CONSULTING',         'MARKET_ACT',        '3001001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-CONS-MFG-001',        'CONSULTING',         'MANUFACTURE',       '2001001003', 'COST',  0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 维修保养补全（研发/市场场景） ==========
('MR-MAINT-RD-001',        'MAINTENANCE',        'RD_INVEST',         '3002004', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),
('MR-MAINT-MKT-001',       'MAINTENANCE',        'MARKET_ACT',        '3002004', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 市场推广补全（生产制造场景） ==========
('MR-MKT-MFG-001',         'MARKETING',          'MANUFACTURE',       '3001001', 'EXPENSE', 0, 'DIRECT', 10, 'ENABLED', 'admin', NOW()),

-- ========== 其他费用（兆底，优先级较低） ==========
('MR-OTHER-DAILY-001',     'OTHER',              'DAILY_OPS',         '3002001', 'EXPENSE', 0, 'DIRECT', 5,  'ENABLED', 'admin', NOW()),
('MR-OTHER-RD-001',        'OTHER',              'RD_INVEST',         '3002001', 'EXPENSE', 0, 'DIRECT', 5,  'ENABLED', 'admin', NOW()),
('MR-OTHER-PROJECT-001',   'OTHER',              'PROJECT_DELIVERY',  '2001001003', 'COST',  0, 'DIRECT', 5,  'ENABLED', 'admin', NOW()),
('MR-OTHER-MKT-001',       'OTHER',              'MARKET_ACT',        '3001001', 'EXPENSE', 0, 'DIRECT', 5,  'ENABLED', 'admin', NOW()),
('MR-OTHER-MFG-001',       'OTHER',              'MANUFACTURE',       '2001001003', 'COST',  0, 'DIRECT', 5,  'ENABLED', 'admin', NOW());
