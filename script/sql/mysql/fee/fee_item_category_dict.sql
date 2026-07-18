-- ----------------------------
-- 物资小类字典数据初始化
-- ----------------------------

-- 插入字典类型
INSERT INTO sys_dict_type VALUES(12, '物资小类', 'fee_item_category', '0', 'admin', NOW(), '', NULL, '报销单物资小类列表');

-- 插入字典数据（常见的物资小类）
INSERT INTO sys_dict_data VALUES(34, 1,  '办公用品', 'OFFICE_SUPPLIES', 'fee_item_category', '', 'primary', 'N', '0', 'admin', NOW(), '', NULL, '办公文具、耗材等');
INSERT INTO sys_dict_data VALUES(35, 2,  '差旅费用', 'TRAVEL_EXPENSES', 'fee_item_category', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '交通、住宿、餐饮等');
INSERT INTO sys_dict_data VALUES(36, 3,  '通讯费用', 'COMMUNICATION', 'fee_item_category', '', 'info', 'N', '0', 'admin', NOW(), '', NULL, '电话费、网络费等');
INSERT INTO sys_dict_data VALUES(37, 4,  '培训费用', 'TRAINING', 'fee_item_category', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '培训课程、教材等');
INSERT INTO sys_dict_data VALUES(38, 5,  '市场推广', 'MARKETING', 'fee_item_category', '', 'danger', 'N', '0', 'admin', NOW(), '', NULL, '广告、宣传物料等');
INSERT INTO sys_dict_data VALUES(39, 6,  '设备采购', 'EQUIPMENT', 'fee_item_category', '', 'primary', 'N', '0', 'admin', NOW(), '', NULL, '电脑、打印机等设备');
INSERT INTO sys_dict_data VALUES(40, 7,  '软件授权', 'SOFTWARE_LICENSE', 'fee_item_category', '', 'info', 'N', '0', 'admin', NOW(), '', NULL, '软件购买、续费等');
INSERT INTO sys_dict_data VALUES(41, 8,  '咨询服务', 'CONSULTING', 'fee_item_category', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '专业咨询、顾问服务等');
INSERT INTO sys_dict_data VALUES(42, 9,  '维修保养', 'MAINTENANCE', 'fee_item_category', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '设备维修、保养等');
INSERT INTO sys_dict_data VALUES(43, 10, '其他费用', 'OTHER', 'fee_item_category', '', 'default', 'Y', '0', 'admin', NOW(), '', NULL, '其他未分类费用');
