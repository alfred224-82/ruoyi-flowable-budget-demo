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
-- Table structure for table `budget_email_log`
--

DROP TABLE IF EXISTS `budget_email_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_email_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` bigint NOT NULL COMMENT '预算单ID（关联budget_sheet表）',
  `sheet_no` varchar(50) DEFAULT NULL COMMENT '预算单号（冗余字段）',
  `email_type` varchar(20) NOT NULL COMMENT '邮件类型（REJECT/SEND_BACK/TIMEOUT_REMIND）',
  `recipient_user` varchar(50) NOT NULL COMMENT '收件人（用户账号）',
  `recipient_email` varchar(100) NOT NULL COMMENT '收件人邮箱',
  `email_subject` varchar(200) NOT NULL COMMENT '邮件主题',
  `send_status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态（PENDING/SUCCESS/FAILED）',
  `error_message` text COMMENT '错误信息（发送失败时记录）',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`),
  KEY `idx_recipient_user` (`recipient_user`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算邮件发送日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_email_log`
--

LOCK TABLES `budget_email_log` WRITE;
/*!40000 ALTER TABLE `budget_email_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `budget_email_log` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:24
