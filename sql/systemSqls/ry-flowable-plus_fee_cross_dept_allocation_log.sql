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
-- Table structure for table `fee_cross_dept_allocation_log`
--

DROP TABLE IF EXISTS `fee_cross_dept_allocation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_cross_dept_allocation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `allocation_period` varchar(7) NOT NULL COMMENT '分摊期间（YYYY-MM）',
  `source_dept_id` bigint NOT NULL COMMENT '来源部门（虚拟待分摊部门）',
  `target_dept_id` bigint NOT NULL COMMENT '目标部门',
  `budget_subject_code` varchar(100) NOT NULL COMMENT '预算科目',
  `total_amount` decimal(18,2) NOT NULL COMMENT '待分摊总额',
  `alloc_ratio` decimal(5,4) NOT NULL COMMENT '分摊比例',
  `alloc_amount` decimal(18,2) NOT NULL COMMENT '分摊金额',
  `alloc_basis` varchar(30) DEFAULT 'HEADCOUNT' COMMENT '分摊依据（HEADCOUNT/WORKHOURS）',
  `voucher_no` varchar(50) DEFAULT NULL COMMENT '调整凭证号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_period` (`allocation_period`),
  KEY `idx_source_dept` (`source_dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨部门分摊记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_cross_dept_allocation_log`
--

LOCK TABLES `fee_cross_dept_allocation_log` WRITE;
/*!40000 ALTER TABLE `fee_cross_dept_allocation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `fee_cross_dept_allocation_log` ENABLE KEYS */;
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
