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
-- Table structure for table `fee_reserved_allocation_log`
--

DROP TABLE IF EXISTS `fee_reserved_allocation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_reserved_allocation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reserved_id` bigint NOT NULL COMMENT '关联预留预算ID',
  `application_id` bigint NOT NULL COMMENT '关联预算调整申请ID',
  `app_no` varchar(50) DEFAULT NULL COMMENT '关联申请单号',
  `target_subject` varchar(50) NOT NULL COMMENT '目标预算科目编码',
  `target_subject_name` varchar(100) DEFAULT NULL COMMENT '目标预算科目名称',
  `allocate_amount` decimal(15,2) NOT NULL COMMENT '分摊金额',
  `before_available` decimal(15,2) NOT NULL COMMENT '分摊前科目可用余额',
  `after_available` decimal(15,2) NOT NULL COMMENT '分摊后科目可用余额',
  `before_reserved_available` decimal(15,2) NOT NULL COMMENT '分摊前预留剩余额度',
  `after_reserved_available` decimal(15,2) NOT NULL COMMENT '分摊后预留剩余额度',
  `operator_id` bigint NOT NULL COMMENT '操作人ID（审批人）',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `operate_time` datetime NOT NULL COMMENT '操作时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reserved_id` (`reserved_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2078472358581649411 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预留预算分摊记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_reserved_allocation_log`
--

LOCK TABLES `fee_reserved_allocation_log` WRITE;
/*!40000 ALTER TABLE `fee_reserved_allocation_log` DISABLE KEYS */;
INSERT INTO `fee_reserved_allocation_log` VALUES (2078176907202555906,2078176906850234369,2078153013905252353,'BA-20260718-308','4002001','房屋建筑物',500.00,500.00,1000.00,100000.00,99500.00,2060581847141371905,'部门经理','2026-07-18 01:56:00','2026-07-18 01:56:00'),(2078445657260707841,2078176906850234369,2078445344491458562,'BA-20260718-534','3002001','办公费',500.00,500.00,1000.00,99500.00,99000.00,2060581847141371905,'部门经理','2026-07-18 19:43:55','2026-07-18 19:43:55'),(2078472358581649410,2078176906850234369,2078472080939696130,'BA-20260718-179','3001001','广告费',200.00,400.00,600.00,99000.00,98800.00,2060581847141371905,'部门经理','2026-07-18 21:30:01','2026-07-18 21:30:01');
/*!40000 ALTER TABLE `fee_reserved_allocation_log` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-23 14:43:08
