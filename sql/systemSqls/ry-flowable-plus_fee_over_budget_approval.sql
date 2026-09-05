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
-- Table structure for table `fee_over_budget_approval`
--

DROP TABLE IF EXISTS `fee_over_budget_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_over_budget_approval` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_id` bigint NOT NULL COMMENT '关联报销单ID',
  `cost_center_group` varchar(50) NOT NULL COMMENT '预算责任中心编码',
  `budget_subject` varchar(100) NOT NULL COMMENT '超预算的预算科目编码',
  `original_budget` decimal(18,2) NOT NULL COMMENT '该科目原预算金额',
  `used_amount` decimal(18,2) NOT NULL COMMENT '该科目已使用金额',
  `request_amount` decimal(18,2) NOT NULL COMMENT '本次申请金额',
  `shortage_amount` decimal(18,2) NOT NULL COMMENT '缺口金额',
  `approval_action` varchar(20) DEFAULT NULL COMMENT '审批动作(APPROVE/REJECT/ADJUST)',
  `adjust_from_subject` varchar(100) DEFAULT NULL COMMENT '调出科目编码',
  `adjust_amount` decimal(18,2) DEFAULT NULL COMMENT '调剂金额',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approval_opinion` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/PROCESSED)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='超预算审批表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_over_budget_approval`
--

LOCK TABLES `fee_over_budget_approval` WRITE;
/*!40000 ALTER TABLE `fee_over_budget_approval` DISABLE KEYS */;
/*!40000 ALTER TABLE `fee_over_budget_approval` ENABLE KEYS */;
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
