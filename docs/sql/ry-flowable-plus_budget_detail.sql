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
-- Table structure for table `budget_detail`
--

DROP TABLE IF EXISTS `budget_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` bigint NOT NULL COMMENT '表头ID（外键，关联budget_sheet.id）',
  `subject_code` varchar(20) NOT NULL COMMENT '科目编码（关联budget_subject表）',
  `subject_name` varchar(100) DEFAULT NULL COMMENT '科目名称（冗余字段）',
  `budget_amount` decimal(18,2) DEFAULT '0.00' COMMENT '预算金额',
  `actual_amount` decimal(18,2) DEFAULT '0.00' COMMENT '实际金额（脱敏存储，查询时根据权限决定是否显示）',
  `variance_amount` decimal(18,2) DEFAULT '0.00' COMMENT '差异金额（自动计算：预算-实际）',
  `variance_rate` decimal(5,2) DEFAULT '0.00' COMMENT '差异率（自动计算：(预算-实际)/实际*100）',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID（关联sys_dept表的dept_id）',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称（冗余字段）',
  `sort_order` int DEFAULT '0' COMMENT '排序号（用于前端展示顺序）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`),
  KEY `idx_subject` (`subject_code`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_detail`
--

LOCK TABLES `budget_detail` WRITE;
/*!40000 ALTER TABLE `budget_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `budget_detail` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:15
