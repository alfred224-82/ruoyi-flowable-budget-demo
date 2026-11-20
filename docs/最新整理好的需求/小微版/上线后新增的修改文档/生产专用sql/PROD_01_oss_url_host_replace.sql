-- =====================================================================
-- 生产环境专属数据修复 PROD_01：OSS 存量 URL 主机替换为公网地址
-- 日期：2026-10-14（修订：2026-10-17）
-- 适用：仅生产库（腾讯云 118.89.133.114）手工执行，禁止在开发/测试库执行
-- 背景：
--   生产 MinIO 启动 endpoint 为 127.0.0.1:9000（内网 10.0.0.10:9000），sys_oss_config
--   endpoint 同为此值，导致历史上传文件的 URL 以本机/内网主机入库；公网客户端
--   （手机 H5、外部 PC）永远访问不到该地址 → 图片/语音/座位背景图全部加载失败。
--   修复内容（修订后本脚本一次完成以下两步，无需再手工操作管理端页面）：
--     ① 第 1) 节更新 sys_oss_config.domain 为 118.89.133.114/oss
--        （Nginx /oss/ 反代本机 9000，外网无需开 9000；endpoint 保持 127.0.0.1:9000
--          供后端本机通信），此后新上传文件 URL 自动带公网主机；
--        ★该配置被后端缓存（Redis + 内存 OssClient），执行本脚本后必须重启 ruoyi-admin 一次才生效；
--     ② 第 2)~4) 节替换存量数据中的旧主机（替换后即生效，无需重启）。
-- ★登记例外说明（用户全局规则「SQL 必须落地并登记 00_执行顺序.sql」的显式例外）：
--   本脚本为【环境专属】数据修复：开发/测试环境 MinIO 同为 127.0.0.1:9000，若登记进
--   00_执行顺序.sql 被开发环境执行，本地图片 URL 会被误改写指向生产公网 IP 而立即损坏。
--   故单独归档于「生产专用sql」目录，不登记、不随全量脚本分发。
-- 幂等性：WHERE LIKE 守卫 + REPLACE 天然幂等，domain 定值赋值亦幂等，可重复执行；不匹配时 0 行更新。
-- =====================================================================

SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;

-- ----------------------------
-- 0) 预检：查看存量旧主机 URL 分布（先执行此段确认命中行数，再执行下面的 UPDATE）
-- ----------------------------
SELECT 'sys_oss' AS tbl, COUNT(*) AS cnt FROM sys_oss
 WHERE url LIKE 'http://127.0.0.1:9000/%' OR url LIKE 'http://localhost:9000/%' OR url LIKE 'http://10.0.0.10:9000/%'
UNION ALL
SELECT 'room_seat_layout', COUNT(*) FROM room_seat_layout
 WHERE bg_image_url LIKE 'http://127.0.0.1:9000/%' OR bg_image_url LIKE 'http://localhost:9000/%' OR bg_image_url LIKE 'http://10.0.0.10:9000/%'
UNION ALL
SELECT 'room_seat_layout_template', COUNT(*) FROM room_seat_layout_template
 WHERE bg_image_url LIKE 'http://127.0.0.1:9000/%' OR bg_image_url LIKE 'http://localhost:9000/%' OR bg_image_url LIKE 'http://10.0.0.10:9000/%'
UNION ALL
SELECT 'sys_user.avatar', COUNT(*) FROM sys_user
 WHERE avatar LIKE 'http://127.0.0.1:9000/%' OR avatar LIKE 'http://localhost:9000/%' OR avatar LIKE 'http://10.0.0.10:9000/%';

-- ----------------------------
-- 1) sys_oss_config：自定义域名（决定新上传 URL 的公网主机；原修复步骤①，2026-10-17 并入本脚本）
--    ★执行后必须重启 ruoyi-admin 一次刷新后端 OSS 配置缓存，否则新上传仍拼 127.0.0.1:9000
-- ----------------------------
UPDATE sys_oss_config SET domain = '118.89.133.114/oss' WHERE config_key = 'minio';

-- ----------------------------
-- 2) sys_oss：文件主表 URL（活动头图/海报/语音/二维码等 ossId 引用均经此表解析）
-- ----------------------------
UPDATE sys_oss SET url = REPLACE(url, 'http://127.0.0.1:9000/', 'http://118.89.133.114/oss/') WHERE url LIKE 'http://127.0.0.1:9000/%';
UPDATE sys_oss SET url = REPLACE(url, 'http://localhost:9000/',   'http://118.89.133.114/oss/') WHERE url LIKE 'http://localhost:9000/%';
UPDATE sys_oss SET url = REPLACE(url, 'http://10.0.0.10:9000/',   'http://118.89.133.114/oss/') WHERE url LIKE 'http://10.0.0.10:9000/%';

-- ----------------------------
-- 3) room_seat_layout / 模板：座位布局背景图直存全 URL（不走 ossId 解析，需单独替换）
-- ----------------------------
UPDATE room_seat_layout SET bg_image_url = REPLACE(bg_image_url, 'http://127.0.0.1:9000/', 'http://118.89.133.114/oss/') WHERE bg_image_url LIKE 'http://127.0.0.1:9000/%';
UPDATE room_seat_layout SET bg_image_url = REPLACE(bg_image_url, 'http://localhost:9000/',   'http://118.89.133.114/oss/') WHERE bg_image_url LIKE 'http://localhost:9000/%';
UPDATE room_seat_layout SET bg_image_url = REPLACE(bg_image_url, 'http://10.0.0.10:9000/',   'http://118.89.133.114/oss/') WHERE bg_image_url LIKE 'http://10.0.0.10:9000/%';

UPDATE room_seat_layout_template SET bg_image_url = REPLACE(bg_image_url, 'http://127.0.0.1:9000/', 'http://118.89.133.114/oss/') WHERE bg_image_url LIKE 'http://127.0.0.1:9000/%';
UPDATE room_seat_layout_template SET bg_image_url = REPLACE(bg_image_url, 'http://localhost:9000/',   'http://118.89.133.114/oss/') WHERE bg_image_url LIKE 'http://localhost:9000/%';
UPDATE room_seat_layout_template SET bg_image_url = REPLACE(bg_image_url, 'http://10.0.0.10:9000/',   'http://118.89.133.114/oss/') WHERE bg_image_url LIKE 'http://10.0.0.10:9000/%';

-- ----------------------------
-- 4) sys_user.avatar：头像直存全 URL（座位占座头像/个人中心展示）
-- ----------------------------
UPDATE sys_user SET avatar = REPLACE(avatar, 'http://127.0.0.1:9000/', 'http://118.89.133.114/oss/') WHERE avatar LIKE 'http://127.0.0.1:9000/%';
UPDATE sys_user SET avatar = REPLACE(avatar, 'http://localhost:9000/',   'http://118.89.133.114/oss/') WHERE avatar LIKE 'http://localhost:9000/%';
UPDATE sys_user SET avatar = REPLACE(avatar, 'http://10.0.0.10:9000/',   'http://118.89.133.114/oss/') WHERE avatar LIKE 'http://10.0.0.10:9000/%';

SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 5) 复检：应全部返回 0
-- ----------------------------
SELECT 'sys_oss' AS tbl, COUNT(*) AS remain FROM sys_oss
 WHERE url LIKE 'http://127.0.0.1:9000/%' OR url LIKE 'http://localhost:9000/%' OR url LIKE 'http://10.0.0.10:9000/%'
UNION ALL
SELECT 'room_seat_layout', COUNT(*) FROM room_seat_layout
 WHERE bg_image_url LIKE 'http://127.0.0.1:9000/%' OR bg_image_url LIKE 'http://localhost:9000/%' OR bg_image_url LIKE 'http://10.0.0.10:9000/%'
UNION ALL
SELECT 'room_seat_layout_template', COUNT(*) FROM room_seat_layout_template
 WHERE bg_image_url LIKE 'http://127.0.0.1:9000/%' OR bg_image_url LIKE 'http://localhost:9000/%' OR bg_image_url LIKE 'http://10.0.0.10:9000/%'
UNION ALL
SELECT 'sys_user.avatar', COUNT(*) FROM sys_user
 WHERE avatar LIKE 'http://127.0.0.1:9000/%' OR avatar LIKE 'http://localhost:9000/%' OR avatar LIKE 'http://10.0.0.10:9000/%';

-- ----------------------------
-- 6) 配置复检：domain 应为 118.89.133.114/oss（若为空说明第 1) 节未命中，请核对 config_key）
-- ----------------------------
SELECT config_key, endpoint, domain, status FROM sys_oss_config WHERE config_key = 'minio';

-- 说明：第 2)~4) 节存量 URL 替换为请求时实时读库（无缓存），执行后即生效，无需重启后端；
--       第 1) 节 domain 变更被后端缓存（Redis + 内存 OssClient），执行本脚本后必须重启 ruoyi-admin 一次，
--       否则新上传仍按旧空 domain 拼接 127.0.0.1:9000；
--       若个别图片仍不显示，复制其 URL 看主机是否仍为旧值，是则定位所在表按同法 REPLACE。
