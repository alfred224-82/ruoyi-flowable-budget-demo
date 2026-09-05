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
-- Table structure for table `fee_budget_adjustment_app`
--

DROP TABLE IF EXISTS `fee_budget_adjustment_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_budget_adjustment_app` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_no` varchar(50) NOT NULL COMMENT '申请单号（BA-YYYYMMDD-XXX）',
  `bill_id` bigint DEFAULT NULL COMMENT '关联报销单ID',
  `bill_no` varchar(50) DEFAULT NULL COMMENT '关联报销单号',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `applicant_name` varchar(50) DEFAULT NULL COMMENT '申请人姓名',
  `dept_id` bigint NOT NULL COMMENT '申请人部门ID',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '申请人部门名称',
  `item_category` varchar(100) DEFAULT NULL COMMENT '物资小类编码',
  `purpose_tag` varchar(50) DEFAULT NULL COMMENT '用途标签编码',
  `target_subject` varchar(50) NOT NULL COMMENT '目标预算科目编码',
  `target_subject_name` varchar(100) DEFAULT NULL COMMENT '目标预算科目名称',
  `cost_center_code` varchar(50) DEFAULT NULL COMMENT '责任中心编码',
  `current_balance` decimal(15,2) NOT NULL COMMENT '申请时科目可用余额',
  `request_amount` decimal(15,2) NOT NULL COMMENT '申请调整金额',
  `budget_amount` decimal(15,2) DEFAULT NULL COMMENT '预算金额',
  `reason` text NOT NULL COMMENT '调整原因说明',
  `current_level` int NOT NULL DEFAULT '1' COMMENT '当前审批层级：1部门/2分公司/3总公司',
  `dept_reserved_id` bigint DEFAULT NULL COMMENT '部门预留预算ID',
  `branch_reserved_id` bigint DEFAULT NULL COMMENT '分公司预留预算ID',
  `hq_reserved_id` bigint DEFAULT NULL COMMENT '总公司预留预算ID',
  `allocate_amount` decimal(15,2) DEFAULT NULL COMMENT '实际分摊金额',
  `allocate_from_reserved_id` bigint DEFAULT NULL COMMENT '实际分摊来源的预留预算ID',
  `reject_reason` text COMMENT '驳回原因',
  `status` varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` varchar(64) DEFAULT '',
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_no` (`app_no`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_status` (`status`),
  KEY `idx_applicant` (`applicant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2078723758007951362 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算调整申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_budget_adjustment_app`
--

LOCK TABLES `fee_budget_adjustment_app` WRITE;
/*!40000 ALTER TABLE `fee_budget_adjustment_app` DISABLE KEYS */;
INSERT INTO `fee_budget_adjustment_app` VALUES (2078152455861493762,'BA-20260718-380',NULL,NULL,1,'若依管理员',103,NULL,NULL,NULL,'4001002','应收账款',NULL,1700.00,2000.00,1700.00,'增加300',1,NULL,NULL,NULL,NULL,NULL,NULL,'DEPT_APPROVING','2026-07-18 00:18:50',NULL,'admin','admin','2026-07-18 00:18:50'),(2078153013905252353,'BA-20260718-308',NULL,NULL,2060581619571019778,'部门编制人员',107,NULL,NULL,NULL,'4002001','房屋建筑物',NULL,500.00,1000.00,500.00,'增加500',1,2078176906850234369,NULL,NULL,500.00,2078176906850234369,NULL,'APPROVED','2026-07-18 00:21:03','2026-07-18 01:56:00','H00889','H00998','2026-07-18 01:56:00'),(2078445344491458562,'BA-20260718-534',NULL,NULL,2060581619571019778,'部门编制人员',107,'运维部门','OFFICE_SUPPLIES','DAILY_OPS','3002001','办公费',NULL,500.00,1000.00,500.00,'要增加600预算',1,2078176906850234369,NULL,NULL,500.00,2078176906850234369,NULL,'APPROVED','2026-07-18 19:42:40','2026-07-18 19:43:55','H00889','H00998','2026-07-18 19:43:55'),(2078472080939696130,'BA-20260718-179',NULL,NULL,2060581619571019778,'部门编制人员',107,'运维部门','TRAVEL_EXPENSES','MARKET_ACT','3001001','广告费',NULL,400.00,600.00,400.00,'增加广告费',1,2078176906850234369,NULL,NULL,200.00,2078176906850234369,NULL,'APPROVED','2026-07-18 21:28:55','2026-07-18 21:30:01','H00889','H00998','2026-07-18 21:30:01');
/*!40000 ALTER TABLE `fee_budget_adjustment_app` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-23 14:43:07
