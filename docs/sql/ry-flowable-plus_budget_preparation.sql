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
-- Table structure for table `budget_preparation`
--

DROP TABLE IF EXISTS `budget_preparation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_preparation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_no` varchar(50) NOT NULL COMMENT '预算单号（唯一，格式：BG-YYYYMM-XXX）',
  `org_id` bigint NOT NULL COMMENT '组织ID（关联sys_dept表的dept_id）',
  `org_name` varchar(100) DEFAULT NULL COMMENT '组织名称（冗余字段，便于查询）',
  `budget_year` int NOT NULL COMMENT '预算年度',
  `budget_month` int NOT NULL COMMENT '预算月份（1-12）',
  `budget_period` varchar(7) GENERATED ALWAYS AS (concat(`budget_year`,_utf8mb4'-',lpad(`budget_month`,2,_utf8mb4'0'))) STORED COMMENT '预算期间（格式：YYYY-MM，自动生成）',
  `status` varchar(20) NOT NULL DEFAULT 'Draft' COMMENT '生命周期状态（Draft-草稿，Pending_Review-待审核，Pending_Revision-待修订，Approved-已批准，Rejected-已驳回）',
  `reject_level` varchar(10) DEFAULT 'None' COMMENT '驳回来源（HQ-总部，Branch-分公司，None-无）',
  `reject_reason` text COMMENT '驳回理由',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间',
  `current_handler` varchar(50) DEFAULT NULL COMMENT '当前处理人（用户账号）',
  `total_budget` decimal(18,2) DEFAULT '0.00' COMMENT '预算总额（自动汇总明细表）',
  `total_actual` decimal(18,2) DEFAULT '0.00' COMMENT '实际总额',
  `variance_rate` decimal(5,2) DEFAULT '0.00' COMMENT '差异率（百分比）',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `approval_stage` varchar(10) DEFAULT '' COMMENT '审批阶段（None-无，Dept-部门领导，Branch-分公司领导，HQ-总公司领导）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sheet_no` (`sheet_no`),
  KEY `idx_org_period` (`org_id`,`budget_year`,`budget_month`),
  KEY `idx_status` (`status`),
  KEY `idx_budget_period` (`budget_period`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_approval_stage` (`approval_stage`)
) ENGINE=InnoDB AUTO_INCREMENT=2072949143587434499 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算编制主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_preparation`
--

LOCK TABLES `budget_preparation` WRITE;
/*!40000 ALTER TABLE `budget_preparation` DISABLE KEYS */;
INSERT INTO `budget_preparation` (`id`, `sheet_no`, `org_id`, `org_name`, `budget_year`, `budget_month`, `status`, `reject_level`, `reject_reason`, `deadline_time`, `current_handler`, `total_budget`, `total_actual`, `variance_rate`, `process_instance_id`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `approval_stage`) VALUES (2072949143587434498,'BG-202601-001',107,'运维部门',2026,1,'Approved','None','驳回后，流程定义不正确。需要重新驳回测试。',NULL,'H00001',4200.00,0.00,0.00,'d8106d1d-7750-11f1-bf28-28a06b026e93','部门编制人员','2026-07-03 15:42:44','H00001','2026-07-04 10:36:02','','None');
/*!40000 ALTER TABLE `budget_preparation` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:16
