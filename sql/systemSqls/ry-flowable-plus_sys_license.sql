-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: ry-flowable-plus
-- ------------------------------------------------------
-- Server version	9.3.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `sys_license`
--

DROP TABLE IF EXISTS `sys_license`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_license` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `license_id` varchar(50) NOT NULL COMMENT '授权编号（LIC-YYYY-NNNNNN）',
  `license_type` varchar(20) NOT NULL COMMENT '授权类型（TRIAL/FORMAL）',
  `customer` varchar(200) NOT NULL COMMENT '客户名称',
  `edition` varchar(20) NOT NULL COMMENT '版本等级（BASIC/ADVANCED/ENTERPRISE）',
  `trial_days` int DEFAULT '0' COMMENT '试用天数（仅TRIAL有效）',
  `industries` varchar(500) DEFAULT NULL COMMENT '授权行业列表JSON（如["INTERNET","FINANCE"]）',
  `max_tenants` int DEFAULT '1' COMMENT '最大租户数',
  `features` text NOT NULL COMMENT '功能开关列表JSON',
  `machine_fingerprint` varchar(128) NOT NULL COMMENT '机器指纹哈希',
  `issued_at` date NOT NULL COMMENT '签发日期',
  `expires_at` date DEFAULT NULL COMMENT '到期日期（NULL=永久）',
  `license_version` int DEFAULT '1' COMMENT 'License版本号（防降级回滚）',
  `signature` text NOT NULL COMMENT 'RSA-SHA256数字签名',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/EXPIRED/REVOKED/TAMPERED）',
  `activated_at` datetime DEFAULT NULL COMMENT '首次激活时间（试用起算）',
  `last_check_time` datetime DEFAULT NULL COMMENT '最后校验时间（防时钟回拨）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_license_id` (`license_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='License授权表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_license`
--

LOCK TABLES `sys_license` WRITE;
/*!40000 ALTER TABLE `sys_license` DISABLE KEYS */;
INSERT INTO `sys_license` VALUES (1,'LIC-2026-000001','FORMAL','默认开发客户','ENTERPRISE',0,'[\"INTERNET\",\"REALESTATE\",\"MANUFACTURING\",\"FINANCE\"]',10,'[\"FEE_MULTI_CURRENCY\",\"FEE_COMPLIANCE_CUSTOM\",\"FEE_INDUSTRY_SINGLE\",\"FEE_INDUSTRY_PACK_IO\",\"FEE_TRACK_CONTROL\",\"FEE_ACCRUAL_ENGINE\",\"FEE_LOAN_LEDGER\",\"FEE_PROFIT_CUSTOM_TIP\",\"FEE_SUBSIDY_CONFIG\",\"FEE_AI_CUSTOM_PROMPT\",\"FEE_DESENSITIZE_CUSTOM\",\"FEE_CROSS_DEPT_ALLOCATE\",\"FEE_FUND_POOL\"]','DEV-ENVIRONMENT','2026-01-01',NULL,1,'DEV-SIGNATURE-PLACEHOLDER','ACTIVE',NULL,NULL,'2026-07-23 17:17:43','2026-07-23 17:17:43');
/*!40000 ALTER TABLE `sys_license` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-24 22:48:08
