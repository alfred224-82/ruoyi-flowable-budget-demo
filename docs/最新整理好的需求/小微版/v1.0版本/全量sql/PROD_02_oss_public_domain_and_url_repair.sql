-- =====================================================================
-- PROD_02  生产 OSS 公网域名启用 + 存量 127.0.0.1 URL 批量修复（v1.0）
-- 版本：v1.0  日期：2026-10-15
-- 依据：《localhost嫌疑验证测试表.xlsx》Sheet1 步骤④⑤ / Sheet2 C 列 / Sheet3 第 1~2 行
--        真机实测命中：sys_oss.url = http://127.0.0.1:9000/ruoyi/...
--        （sys_oss_config 中 minio 行 endpoint=127.0.0.1:9000 且 domain 为空）
--        真机把 127.0.0.1 解析为手机自身 → 图片/语音全挂；PC 开发机本地有 MinIO 故看似正常（指纹吻合）
-- 根因链：
--   1. OssClient.getUrl()（ruoyi-oss）minio 分支：domain 非空 → http://domain/bucket；
--      domain 为空 → http://endpoint/bucket（当前落库 URL 即走后者的回退路径）
--   2. endpoint 同时被 AWS SDK 用作服务端连接地址（OssClient 构造器
--      EndpointConfiguration），改成公网会导致服务端上传/读取 MinIO 失败，故 endpoint 不动
--   3. 公网前缀必须走 domain 字段（即管理端「文件存储配置」的自定义域名）：
--      xiaozhangben.com.cn/oss —— Nginx /oss 反代 MinIO，9000 端口不暴露公网
-- 本脚本内容：
--   1. sys_oss_config 的 minio / image 两行 domain 置 'xiaozhangben.com.cn/oss'
--      （endpoint 保持 127.0.0.1:9000 供服务端 SDK 直连本机 MinIO）
--   2. 幂等存储过程扫描当前库全部基表的全部字符串列，把存量
--      http(s)://127.0.0.1:9000 与 http(s)://localhost:9000 前缀批量替换为
--      https://xiaozhangben.com.cn/oss（覆盖 sys_oss.url 及 promotion_activity.cover_url
--      等业务表存 URL 串的列；排除 sys_oss_config 与生成列）
-- 执行后必做（否则不生效）：
--   A. 重启 ruoyi-admin，或在管理端「系统管理 → 文件存储配置」页对 minio 行做一次
--      启用/停用切换（sys_oss_config 有 Redis 缓存 CacheNames.SYS_OSS_CONFIG，直改库不刷新）
--   B. 确认 Nginx 存在 location /oss/ { proxy_pass http://127.0.0.1:9000/; }
--      （proxy_pass 尾部斜杠必须带，用于剥离 /oss 前缀）
--   C. 确认 MinIO 桶匿名读（sys_oss_config.access_policy=1 public）；
--      若 D 步 curl 返回 403 则为桶策略未开
--   D. 验证：curl -sI https://xiaozhangben.com.cn/oss/ruoyi/<sys_oss.url 中任一对象路径>
--      预期 HTTP 200
--   E. 真机回归：重开业务页图片应显示；确认修复后删除 ruoyi-app/index.html 中
--      BEGIN-DEBUG-LOCALHOST / END-DEBUG-LOCALHOST 调试块并重新 build:h5
-- 幂等性：UPDATE 带值等值守卫 + WHERE LIKE 守卫；存储过程用完即 DROP；可重复执行
-- 前置依赖：无（纯数据修复，无表结构 / 菜单 / 权限变更）
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1) 启用公网域名（domain 仅参与 URL 拼接；endpoint 保留供服务端 SDK 连接 MinIO）
-- ---------------------------------------------------------------------
UPDATE sys_oss_config
   SET domain = 'xiaozhangben.com.cn/oss'
 WHERE config_key IN ('minio', 'image')
   AND (domain IS NULL OR domain <> 'xiaozhangben.com.cn/oss');

-- ---------------------------------------------------------------------
-- 2) 存量 URL 批量替换（幂等存储过程：扫当前库全部基表字符串列）
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS prod_02_replace_oss_urls;
DELIMITER $$
CREATE PROCEDURE prod_02_replace_oss_urls()
BEGIN
    DECLARE v_table VARCHAR(128);
    DECLARE v_col   VARCHAR(128);
    DECLARE v_done  INT DEFAULT 0;
    DECLARE cur CURSOR FOR
        SELECT c.TABLE_NAME, c.COLUMN_NAME
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
        FETCH cur INTO v_table, v_col;
        IF v_done = 1 THEN LEAVE rep_loop; END IF;
        SET @ddl = CONCAT(
            'UPDATE `', v_table, '` SET `', v_col, '` = ',
            'REPLACE(REPLACE(REPLACE(REPLACE(`', v_col, '`, ',
            '''https://127.0.0.1:9000'', ''https://xiaozhangben.com.cn/oss''), ',
            '''http://127.0.0.1:9000'', ''https://xiaozhangben.com.cn/oss''), ',
            '''https://localhost:9000'', ''https://xiaozhangben.com.cn/oss''), ',
            '''http://localhost:9000'', ''https://xiaozhangben.com.cn/oss'') ',
            'WHERE `', v_col, '` LIKE ''%//127.0.0.1:9000%'' ',
            'OR `', v_col, '` LIKE ''%//localhost:9000%''');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;
CALL prod_02_replace_oss_urls();
DROP PROCEDURE IF EXISTS prod_02_replace_oss_urls;

-- ---------------------------------------------------------------------
-- 3) 验收检查
-- ---------------------------------------------------------------------
-- 3.1 domain 已启用且 endpoint 未动
SELECT config_key, endpoint, domain, status FROM sys_oss_config ORDER BY oss_config_id;
-- 预期：minio / image 两行 domain = xiaozhangben.com.cn/oss，endpoint 仍为 127.0.0.1:9000

-- 3.2 sys_oss 无 localhost / 127 残留 URL（预期 0）
SELECT COUNT(*) AS remaining_bad_urls FROM sys_oss
 WHERE url LIKE '%//127.0.0.1:9000%' OR url LIKE '%//localhost:9000%';

-- 3.3 最新 URL 应为公网前缀
SELECT url, create_time FROM sys_oss ORDER BY create_time DESC LIMIT 5;
-- 预期：https://xiaozhangben.com.cn/oss/ruoyi/...
