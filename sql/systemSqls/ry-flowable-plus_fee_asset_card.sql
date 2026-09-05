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
-- Table structure for table `fee_asset_card`
--

DROP TABLE IF EXISTS `fee_asset_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_asset_card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_code` varchar(50) NOT NULL COMMENT '资产编号',
  `asset_name` varchar(200) NOT NULL COMMENT '资产名称',
  `asset_type` varchar(30) NOT NULL COMMENT '资产类型（FIXED/INTANGIBLE）',
  `dept_id` bigint NOT NULL COMMENT '归属部门ID',
  `budget_subject_code` varchar(100) DEFAULT NULL COMMENT '关联预算科目',
  `original_value` decimal(18,2) NOT NULL COMMENT '原值',
  `residual_rate` decimal(5,2) DEFAULT '5.00' COMMENT '残值率(%)',
  `useful_life_months` int NOT NULL COMMENT '折旧/摊销月数',
  `start_date` date NOT NULL COMMENT '启用日期（次月起计提）',
  `monthly_amount` decimal(18,2) NOT NULL COMMENT '月折旧/摊销额',
  `accumulated_amount` decimal(18,2) DEFAULT '0.00' COMMENT '累计已计提金额',
  `source_bill_id` bigint DEFAULT NULL COMMENT '来源报销单ID',
  `industry_code` varchar(50) DEFAULT NULL COMMENT '行业编码',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/DISPOSED/FULLY_DEPRECIATED）',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_code` (`asset_code`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产卡片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_asset_card`
--

LOCK TABLES `fee_asset_card` WRITE;
/*!40000 ALTER TABLE `fee_asset_card` DISABLE KEYS */;
/*!40000 ALTER TABLE `fee_asset_card` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-24 22:48:21
