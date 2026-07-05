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
-- Table structure for table `budget_operation_log`
--

DROP TABLE IF EXISTS `budget_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` bigint NOT NULL COMMENT '预算单ID（关联budget_sheet表）',
  `sheet_no` varchar(50) DEFAULT NULL COMMENT '预算单号（冗余字段）',
  `operation_type` varchar(20) NOT NULL COMMENT '操作类型（CREATE/SUBMIT/APPROVE/REJECT/SEND_BACK/UPDATE/DELETE）',
  `operator_user` varchar(50) NOT NULL COMMENT '操作人（用户账号）',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名（冗余字段）',
  `operator_dept_id` bigint DEFAULT NULL COMMENT '操作人部门ID（关联sys_dept表）',
  `operator_ip` varchar(128) DEFAULT NULL COMMENT '操作IP地址',
  `old_status` varchar(20) DEFAULT NULL COMMENT '原状态',
  `new_status` varchar(20) DEFAULT NULL COMMENT '新状态',
  `operation_desc` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`),
  KEY `idx_operator_user` (`operator_user`),
  KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_operation_log`
--

LOCK TABLES `budget_operation_log` WRITE;
/*!40000 ALTER TABLE `budget_operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `budget_operation_log` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:19
