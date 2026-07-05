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
-- Table structure for table `budget_reject_history`
--

DROP TABLE IF EXISTS `budget_reject_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_reject_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` bigint NOT NULL COMMENT '预算单ID（外键，关联budget_sheet.id）',
  `sheet_no` varchar(50) DEFAULT NULL COMMENT '预算单号（冗余字段）',
  `reject_from_level` varchar(10) NOT NULL COMMENT '驳回方层级（HQ/Branch）',
  `reject_from_user` varchar(50) NOT NULL COMMENT '驳回人工号（用户账号）',
  `reject_from_name` varchar(50) DEFAULT NULL COMMENT '驳回人姓名（冗余字段）',
  `reject_to_level` varchar(10) NOT NULL COMMENT '被驳回方层级（Branch/Dept）',
  `reject_to_user` varchar(50) DEFAULT NULL COMMENT '被驳回人工号（用户账号，可为空）',
  `reject_to_dept_id` bigint DEFAULT NULL COMMENT '被驳回部门ID（打回部门时必填，关联sys_dept表）',
  `reject_to_dept_name` varchar(100) DEFAULT NULL COMMENT '被驳回部门名称（冗余字段）',
  `reject_reason` text NOT NULL COMMENT '驳回理由',
  `deadline_time` datetime NOT NULL COMMENT '截止时间',
  `reject_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '驳回时间',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_duration_hours` decimal(8,2) DEFAULT NULL COMMENT '处理耗时（小时，自动计算）',
  `is_timeout` tinyint(1) DEFAULT '0' COMMENT '是否超时（0-否，1-是）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`),
  KEY `idx_reject_time` (`reject_time`),
  KEY `idx_reject_from_user` (`reject_from_user`)
) ENGINE=InnoDB AUTO_INCREMENT=2073230866552336387 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='驳回历史记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_reject_history`
--

LOCK TABLES `budget_reject_history` WRITE;
/*!40000 ALTER TABLE `budget_reject_history` DISABLE KEYS */;
INSERT INTO `budget_reject_history` VALUES (2072963736225370114,2072949143587434498,'BG-202601-001','HQ','H00001','总公司本吧','Dept',NULL,NULL,NULL,'测试驳回流程，总公司驳回，需要编制人员编制。','2026-08-07 12:00:00','2026-07-03 16:40:43',NULL,NULL,0,'H00001','2026-07-03 16:40:43','H00001','2026-07-03 16:40:43'),(2073230866552336386,2072949143587434498,'BG-202601-001','HQ','H00001','总公司本吧','Dept',NULL,NULL,NULL,'驳回后，流程定义不正确。需要重新驳回测试。','2026-08-07 12:00:00','2026-07-04 10:22:12',NULL,NULL,0,'H00001','2026-07-04 10:22:12','H00001','2026-07-04 10:22:12');
/*!40000 ALTER TABLE `budget_reject_history` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:18
