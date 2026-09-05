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
-- Table structure for table `fee_subsidy_standard`
--

DROP TABLE IF EXISTS `fee_subsidy_standard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_subsidy_standard` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subsidy_type` varchar(30) NOT NULL COMMENT '补助类型（TRAVEL/MEAL/TRANSPORT）',
  `subsidy_name` varchar(100) NOT NULL COMMENT '补助名称',
  `unit_amount` decimal(10,2) NOT NULL COMMENT '单位金额（元/天）',
  `unit` varchar(20) DEFAULT 'DAY' COMMENT '计量单位（DAY/TRIP/MONTH）',
  `budget_subject_code` varchar(100) DEFAULT NULL COMMENT '归集科目（职工福利费）',
  `max_days` int DEFAULT NULL COMMENT '最大天数限制',
  `industry_code` varchar(50) DEFAULT '*',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='补助标准配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_subsidy_standard`
--

LOCK TABLES `fee_subsidy_standard` WRITE;
/*!40000 ALTER TABLE `fee_subsidy_standard` DISABLE KEYS */;
INSERT INTO `fee_subsidy_standard` (`subsidy_type`, `subsidy_name`, `unit_amount`, `unit`, `budget_subject_code`, `max_days`, `industry_code`, `status`, `create_by`, `create_time`) VALUES
('TRAVEL',    '出差补助',   100.00, 'DAY', '3002001', 30, '*', 'ENABLED', 'admin', NOW()),
('MEAL',      '餐补',       50.00,  'DAY', '3002001', 30, '*', 'ENABLED', 'admin', NOW()),
('TRANSPORT', '交通补助',   30.00,  'DAY', '3002001', 30, '*', 'ENABLED', 'admin', NOW());
/*!40000 ALTER TABLE `fee_subsidy_standard` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-24 22:48:05
