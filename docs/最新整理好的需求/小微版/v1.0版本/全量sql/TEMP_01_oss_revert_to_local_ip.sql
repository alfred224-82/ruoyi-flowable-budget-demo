-- =====================================================================
-- TEMP  本地开发：OSS 回退到 127.0.0.1:9000（清空 domain，走 endpoint 拼接）
-- 用途：本地 MinIO 未配置公网域名时，将 sys_oss_config 的 domain 清空，
--       使 OssClient.getUrl() 回退到 http://127.0.0.1:9000/ruoyi/... 路径
-- 幂等性：仅当 domain 非空时才更新，可重复执行
-- 恢复生产：重新执行 PROD_02 / PROD_03 即可
-- =====================================================================

SET NAMES utf8mb4;

-- 1) 清空 minio / image 的 domain（endpoint 保持 127.0.0.1:9000 不动）
UPDATE sys_oss_config
   SET domain = ''
 WHERE config_key IN ('minio', 'image')
   AND domain IS NOT NULL AND domain <> '';

-- 2) 验收：确认 domain 已清空，endpoint 仍为 127.0.0.1:9000
SELECT config_key, endpoint, domain, is_https, status
  FROM sys_oss_config
 ORDER BY oss_config_id;
-- 预期：minio / image 行 domain 为空字符串，endpoint = 127.0.0.1:9000
