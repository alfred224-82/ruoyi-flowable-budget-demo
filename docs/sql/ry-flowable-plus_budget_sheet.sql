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
-- Table structure for table `budget_sheet`
--

DROP TABLE IF EXISTS `budget_sheet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_sheet` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_no` varchar(50) NOT NULL COMMENT '预算单号（唯一，格式：BG-YYYYMM-XXX）',
  `org_id` bigint NOT NULL COMMENT '组织ID（关联sys_dept表的dept_id）',
  `org_name` varchar(100) DEFAULT NULL COMMENT '组织名称（冗余字段，便于查询）',
  `budget_month` varchar(7) NOT NULL COMMENT '预算月份（格式：YYYY-MM）',
  `status` varchar(20) NOT NULL DEFAULT 'Draft' COMMENT '状态码（Draft/Pending_Review/Branch_Pending/Pending_Revision/Approved）',
  `reject_level` varchar(10) DEFAULT 'None' COMMENT '驳回来源（HQ/Branch/None）',
  `reject_reason` text COMMENT '驳回理由（最新一次的驳回理由）',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间（根据驳回层级自动计算）',
  `current_handler` varchar(50) DEFAULT NULL COMMENT '当前处理人（用户工号）',
  `total_budget` decimal(18,2) DEFAULT '0.00' COMMENT '预算总额（自动汇总明细表）',
  `total_actual` decimal(18,2) DEFAULT '0.00' COMMENT '实际总额（脱敏后数据）',
  `variance_rate` decimal(5,2) DEFAULT '0.00' COMMENT '差异率（自动计算：(预算-实际)/实际*100）',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者（用户账号）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者（用户账号）',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sheet_no` (`sheet_no`),
  KEY `idx_org_month` (`org_id`,`budget_month`),
  KEY `idx_status` (`status`),
  KEY `idx_budget_month` (`budget_month`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算单头表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_sheet`
--

LOCK TABLES `budget_sheet` WRITE;
/*!40000 ALTER TABLE `budget_sheet` DISABLE KEYS */;
/*!40000 ALTER TABLE `budget_sheet` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:20
