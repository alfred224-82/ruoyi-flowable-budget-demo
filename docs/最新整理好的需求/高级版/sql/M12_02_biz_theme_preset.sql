-- =====================================================================
-- M12_02 主题预设表补建脚本（T12.2 Design Token，v1.20 补建）
-- 版本：v1.0  日期：2026-09-12
-- 依据：ThemePreset 实体（@TableName("biz_theme_preset")）
--       M12_01 建立了 tenant_theme（租户级主题），但遗漏了系统级共享表 biz_theme_preset
-- 前置依赖：无（独立建表）
-- 规范：
--   1. 先删后建，脚本可重复执行（幂等）
--   2. biz_theme_preset 为【系统级共享表】（无 tenant_id），
--      须纳入 MybatisPlusConfig.IGNORE_TENANT_TABLES（已在）
--   3. 四套 Design Token：dark_boardgame / warm_party / fresh_japan / neon_cyber
-- =====================================================================

DROP TABLE IF EXISTS `biz_theme_preset`;
CREATE TABLE `biz_theme_preset` (
  `preset_key` VARCHAR(50) NOT NULL COMMENT '主题预设 key（主键）：dark_boardgame/warm_party/fresh_japan/neon_cyber',
  `name` VARCHAR(100) DEFAULT NULL COMMENT '主题名称',
  `tokens` JSON COMMENT 'Design Token（主色/辅色/字体/圆角/阴影/渐变，CSS 变量键值 JSON）',
  `preview_url` VARCHAR(500) DEFAULT NULL COMMENT '主题预览图URL',
  `contrast_ratio` VARCHAR(20) DEFAULT NULL COMMENT '对比度达标标记（AA/AAA）',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`preset_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主题预设表(系统级共享,IGNORE_TENANT_TABLES;Design Token)';

-- =====================================================================
-- 内置四套 Design Token 初始数据（先删后插幂等）
-- =====================================================================
DELETE FROM `biz_theme_preset` WHERE `preset_key` IN ('dark_boardgame', 'warm_party', 'fresh_japan', 'neon_cyber');
INSERT INTO `biz_theme_preset` (`preset_key`, `name`, `tokens`, `contrast_ratio`, `sort`, `status`, `create_by`, `remark`) VALUES
('dark_boardgame', '暗夜桌游',
 JSON_OBJECT('--primary', '#6C5CE7', '--secondary', '#00CEC9', '--bg', '#1A1A2E', '--text', '#EAEAEA', '--radius', '8px', '--shadow', '0 4px 12px rgba(0,0,0,0.3)'),
 'AA', 1, '0', 'admin', '桌游/剧本杀暗色主题'),
('warm_party', '暖色派对',
 JSON_OBJECT('--primary', '#E17055', '--secondary', '#FDCB6E', '--bg', '#FFF5F0', '--text', '#2D3436', '--radius', '12px', '--shadow', '0 2px 8px rgba(0,0,0,0.1)'),
 'AA', 2, '0', 'admin', '派对/轰趴暖色主题'),
('fresh_japan', '清新日式',
 JSON_OBJECT('--primary', '#00B894', '--secondary', '#55EFC4', '--bg', '#F8FFF8', '--text', '#2D3436', '--radius', '6px', '--shadow', '0 2px 6px rgba(0,0,0,0.08)'),
 'AAA', 3, '0', 'admin', '生鲜/日式清新主题'),
('neon_cyber', '霓虹赛博',
 JSON_OBJECT('--primary', '#A29BFE', '--secondary', '#FD79A8', '--bg', '#0D0D1A', '--text', '#DFE6E9', '--radius', '4px', '--shadow', '0 0 20px rgba(162,155,254,0.3)'),
 'AA', 4, '0', 'admin', '赛博朋克/科技感主题');

-- ----------------------------
-- 验收查询（执行后人工核对）
-- ----------------------------
-- SELECT preset_key, name, contrast_ratio FROM biz_theme_preset ORDER BY sort;  -- 预期 4 行
