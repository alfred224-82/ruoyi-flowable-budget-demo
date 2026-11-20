-- =====================================================================
-- PROD_03  域名启用：IP 118.89.133.114 → xiaozhangben.com.cn 全量迁移
-- 版本：v1.0  日期：2026-10-21
-- 背景：域名 https://www.xiaozhangben.com.cn / https://xiaozhangben.com.cn 已审批通过并可用，
--       将生产数据库中所有残留的 IP 地址替换为域名。
-- 本脚本内容：
--   1. sys_oss_config 的 minio / image 两行 domain 置 'https://xiaozhangben.com.cn/oss'（★带协议头）
--   2. 幂等存储过程扫全库字符串列，把存量 http(s)://118.89.133.114/oss 与
--      http://xiaozhangben.com.cn/oss 前缀统一替换为 https://xiaozhangben.com.cn/oss
--      ★长度守卫：用同样的 REPLACE 链直接算替换后真实长度，超列容量的行
--        （如 sys_oper_log.oper_param varchar(2000) 嵌多个 URL）自动跳过，
--        避免 Error 1406 Data too long；审计表保留原文无害
--      ★v_maxlen 用 BIGINT：longtext 的 CHARACTER_MAXIMUM_LENGTH=4294967295
--        超 INT 上限，用 INT 会在 FETCH 时报 Error 1264 Out of range
--   3. 存量推广码 URL 中的 IP 替换（promotion_code.qr_code_url 等）
-- ★注意：is_https 保持 'N' 不动！它同时控制后端 SDK 连 MinIO(127.0.0.1:9000 明文 HTTP) 的协议，
--        置 Y 会导致上传全挂；公网 https 由 domain 自带协议头实现（需后端 OssClient.withProtocol 支持）
-- 执行后必做：
--   A. 重启 ruoyi-admin（或在管理端「文件存储配置」页切换一次启用，刷新 Redis 缓存）
--   B. 确认 Nginx server_name 已包含 xiaozhangben.com.cn www.xiaozhangben.com.cn
--   C. 确认 SSL 证书已部署（listen 443 ssl）
--   D. 验证：curl -sI https://xiaozhangben.com.cn/oss/ruoyi/<任一对象路径>  预期 HTTP 200
--   E. 小程序服务器域名白名单（微信公众平台 → 开发管理 → 服务器域名）添加：
--      request合法域名：https://xiaozhangben.com.cn
--      uploadFile合法域名：https://xiaozhangben.com.cn
--      downloadFile合法域名：https://xiaozhangben.com.cn
-- 幂等性：UPDATE 带值等值守卫 + WHERE LIKE 守卫；存储过程用完即 DROP；可重复执行
-- 前置依赖：PROD_02（已执行过 IP 域名启用的基础上再做迁移）
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1) sys_oss_config domain 迁移到域名（★带 https 协议头，收敛式幂等）
-- ---------------------------------------------------------------------
UPDATE sys_oss_config
   SET domain = 'https://xiaozhangben.com.cn/oss'
 WHERE config_key IN ('minio', 'image')
   AND (domain IS NULL OR domain <> 'https://xiaozhangben.com.cn/oss');

-- ---------------------------------------------------------------------
-- 2) 存量 URL 批量替换 → https://xiaozhangben.com.cn/oss
--    （覆盖 http(s)://118.89.133.114/oss 与 http://xiaozhangben.com.cn/oss 三类残留）
--    幂等存储过程扫当前库全部基表字符串列
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS prod_03_ip_to_domain;
DELIMITER $$
CREATE PROCEDURE prod_03_ip_to_domain()
BEGIN
    DECLARE v_table  VARCHAR(128);
    DECLARE v_col    VARCHAR(128);
    DECLARE v_maxlen BIGINT;   -- ★必须 BIGINT：longtext 容量 4294967295 超 INT 上限（Error 1264）
    DECLARE v_done   INT DEFAULT 0;
    DECLARE cur CURSOR FOR
        SELECT c.TABLE_NAME, c.COLUMN_NAME, IFNULL(c.CHARACTER_MAXIMUM_LENGTH, 4294967295)
          FROM information_schema.COLUMNS c
          JOIN information_schema.TABLES t
            ON t.TABLE_SCHEMA = c.TABLE_SCHEMA
           AND t.TABLE_NAME = c.TABLE_NAME
         WHERE c.TABLE_SCHEMA = DATABASE()
           AND t.TABLE_TYPE = 'BASE TABLE'
           AND c.DATA_TYPE IN ('char', 'varchar', 'tinytext', 'text', 'mediumtext', 'longtext')
           AND c.EXTRA NOT LIKE '%GENERATED%'
           AND c.TABLE_NAME <> 'sys_oss_config';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;
    OPEN cur;
    rep_loop: LOOP
        FETCH cur INTO v_table, v_col, v_maxlen;
        IF v_done = 1 THEN LEAVE rep_loop; END IF;
        -- ★长度守卫：用与 SET 完全相同的 REPLACE 链直接算出替换后真实长度，
        --   超列容量的行跳过（如 sys_oper_log.oper_param 嵌多个 URL 的历史审计行），
        --   避免 Error 1406 Data too long 中断整个过程
        SET @ddl = CONCAT(
            'UPDATE `', v_table, '` SET `', v_col, '` = ',
            'REPLACE(REPLACE(REPLACE(`', v_col, '`, ',
            '''http://118.89.133.114/oss'', ''https://xiaozhangben.com.cn/oss''), ',
            '''https://118.89.133.114/oss'', ''https://xiaozhangben.com.cn/oss''), ',
            '''http://xiaozhangben.com.cn/oss'', ''https://xiaozhangben.com.cn/oss'') ',
            'WHERE (`', v_col, '` LIKE ''%118.89.133.114/oss%'' ',
            'OR `', v_col, '` LIKE ''http://xiaozhangben.com.cn/oss%'') ',
            'AND CHAR_LENGTH(REPLACE(REPLACE(REPLACE(`', v_col, '`, ',
            '''http://118.89.133.114/oss'', ''https://xiaozhangben.com.cn/oss''), ',
            '''https://118.89.133.114/oss'', ''https://xiaozhangben.com.cn/oss''), ',
            '''http://xiaozhangben.com.cn/oss'', ''https://xiaozhangben.com.cn/oss'')) ',
            '<= ', v_maxlen);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;
CALL prod_03_ip_to_domain();
DROP PROCEDURE IF EXISTS prod_03_ip_to_domain;

-- ---------------------------------------------------------------------
-- 3) 验收检查
-- ---------------------------------------------------------------------
-- 3.1 domain 已迁移到域名（带 https 协议头）
SELECT config_key, endpoint, domain, is_https, status FROM sys_oss_config ORDER BY oss_config_id;
-- 预期：minio / image 两行 domain = https://xiaozhangben.com.cn/oss，is_https 仍为 N，endpoint 仍为 127.0.0.1:9000

-- 3.2 全库无 IP / 裸 http 域名残留（sys_oss 抽检）
SELECT COUNT(*) AS remaining_bad_urls FROM sys_oss
 WHERE url LIKE '%118.89.133.114%' OR url LIKE 'http://xiaozhangben.com.cn/oss%';
-- 预期：0

-- 3.3 最新 URL 应为 https 域名前缀
SELECT url, create_time FROM sys_oss ORDER BY create_time DESC LIMIT 5;
-- 预期：https://xiaozhangben.com.cn/oss/ruoyi/...

-- 3.4 被长度守卫跳过的行（审计表原文保留，无害，可不管）
SELECT COUNT(*) AS skipped_oper_param FROM sys_oper_log
 WHERE oper_param LIKE '%118.89.133.114/oss%';
-- 预期：少量（历史操作请求参数原文，不在任何页面渲染为图片/链接）

-- =====================================================================
-- ★执行后请重启后端，并确认以下配置已同步更新：
--   · application-prod.yml: promo.h5-base-url = https://xiaozhangben.com.cn/h5/#/pages/login/index
--   · nginx.conf: server_name xiaozhangben.com.cn www.xiaozhangben.com.cn + listen 443 ssl
--   · 微信公众平台服务器域名白名单已添加 https://xiaozhangben.com.cn
-- =====================================================================
