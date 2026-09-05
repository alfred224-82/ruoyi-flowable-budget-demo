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
-- Table structure for table `fee_accrual_record`
--

DROP TABLE IF EXISTS `fee_accrual_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_accrual_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `accrual_period` varchar(7) NOT NULL COMMENT '计提期间（YYYY-MM）',
  `source_type` varchar(30) NOT NULL COMMENT '来源类型（ASSET_DEPRECIATION/CONTRACT_AMORTIZE/LOAN_INTEREST）',
  `source_id` bigint NOT NULL COMMENT '来源ID（资产卡片/合同/借款ID）',
  `source_code` varchar(50) NOT NULL COMMENT '来源编号',
  `dept_id` bigint NOT NULL COMMENT '归属部门',
  `budget_subject_code` varchar(100) NOT NULL COMMENT '预算科目',
  `track_type` varchar(20) DEFAULT 'EXPENSE' COMMENT '轨道类型（计提全部归费用轨）',
  `amount` decimal(18,2) NOT NULL COMMENT '计提金额',
  `voucher_status` varchar(20) DEFAULT 'DRAFT' COMMENT '凭证状态（DRAFT/CONFIRMED/VOIDED）',
  `confirmed_by` bigint DEFAULT NULL COMMENT '财务复核人',
  `confirmed_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_period` (`accrual_period`),
  KEY `idx_source` (`source_type`,`source_id`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计提记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_accrual_record`
--

LOCK TABLES `fee_accrual_record` WRITE;
/*!40000 ALTER TABLE `fee_accrual_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `fee_accrual_record` ENABLE KEYS */;
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
