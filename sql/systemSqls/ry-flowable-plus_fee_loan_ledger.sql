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
-- Table structure for table `fee_loan_ledger`
--

DROP TABLE IF EXISTS `fee_loan_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_loan_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `loan_code` varchar(50) NOT NULL COMMENT '借款编号',
  `loan_type` varchar(20) NOT NULL COMMENT '借款类型（SHORT_TERM/LONG_TERM）',
  `principal` decimal(18,2) NOT NULL COMMENT '本金',
  `annual_rate` decimal(6,4) NOT NULL COMMENT '年化利率',
  `monthly_interest` decimal(18,2) NOT NULL COMMENT '月利息（principal×annual_rate÷12）',
  `start_date` date NOT NULL COMMENT '借款起始日',
  `end_date` date NOT NULL COMMENT '借款到期日',
  `interest_cycle` varchar(20) DEFAULT 'MONTHLY' COMMENT '计提周期（MONTHLY/QUARTERLY）',
  `dept_id` bigint DEFAULT NULL COMMENT '归属部门',
  `budget_subject_code` varchar(100) DEFAULT '3003001' COMMENT '关联科目（默认利息支出）',
  `accumulated_interest` decimal(18,2) DEFAULT '0.00' COMMENT '累计已计提利息',
  `source` varchar(20) DEFAULT 'MANUAL' COMMENT '录入来源（AI/MANUAL）',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/SETTLED/OVERDUE）',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_loan_code` (`loan_code`),
  KEY `idx_status` (`status`),
  KEY `idx_end_date` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='借款台账表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_loan_ledger`
--

LOCK TABLES `fee_loan_ledger` WRITE;
/*!40000 ALTER TABLE `fee_loan_ledger` DISABLE KEYS */;
/*!40000 ALTER TABLE `fee_loan_ledger` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-24 22:48:17
