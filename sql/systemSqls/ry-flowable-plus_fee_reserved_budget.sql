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
-- Table structure for table `fee_reserved_budget`
--

DROP TABLE IF EXISTS `fee_reserved_budget`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_reserved_budget` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `budget_period` varchar(20) NOT NULL COMMENT '预算期间',
  `level_type` varchar(20) NOT NULL COMMENT '层级类型：DEPT/BRANCH/HQ',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID（HQ级别为空）',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `total_amount` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '预留预算总额度',
  `allocated_amount` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '已分摊金额',
  `available_amount` decimal(15,2) GENERATED ALWAYS AS ((`total_amount` - `allocated_amount`)) STORED COMMENT '剩余可用额度（生成列）',
  `warning_threshold` decimal(5,2) DEFAULT '80.00' COMMENT '预警阈值百分比',
  `creator_id` bigint DEFAULT NULL COMMENT '编制人ID',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/FROZEN/EXPIRED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(64) DEFAULT '',
  `update_by` varchar(64) DEFAULT '',
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_period_level` (`budget_period`,`level_type`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2078176906850234370 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预留预算表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_reserved_budget`
--

LOCK TABLES `fee_reserved_budget` WRITE;
/*!40000 ALTER TABLE `fee_reserved_budget` DISABLE KEYS */;
INSERT INTO `fee_reserved_budget` (`id`, `budget_period`, `level_type`, `dept_id`, `dept_name`, `total_amount`, `allocated_amount`, `warning_threshold`, `creator_id`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `remark`) VALUES (2078176906850234369,'2026-07','DEPT',107,NULL,100000.00,1200.00,80.00,2060581847141371905,'ACTIVE','2026-07-18 01:56:00','2026-07-18 21:30:01','H00998','H00998','审批时自动创建的默认资金池');
/*!40000 ALTER TABLE `fee_reserved_budget` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-24 22:48:13
