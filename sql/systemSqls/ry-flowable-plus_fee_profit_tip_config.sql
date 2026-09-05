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
-- Table structure for table `fee_profit_tip_config`
--

DROP TABLE IF EXISTS `fee_profit_tip_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_profit_tip_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_type` varchar(20) NOT NULL COMMENT '科目类型（INCOME/EXPENSE/ASSET/LIABILITY）',
  `tip_type` varchar(20) NOT NULL COMMENT '提示类型（INCREASE/DECREASE/NEUTRAL）',
  `tip_text` varchar(500) NOT NULL COMMENT '提示文案',
  `tip_color` varchar(20) DEFAULT '#333' COMMENT '文字颜色',
  `industry_code` varchar(50) DEFAULT '*' COMMENT '行业编码',
  `large_amount_threshold` decimal(18,2) DEFAULT NULL COMMENT '大额支出阈值（超则弹窗）',
  `force_popup` tinyint DEFAULT '0' COMMENT '是否强制弹窗（0否1是）',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_subject_type` (`subject_type`,`industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='盈利提示配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_profit_tip_config`
--

LOCK TABLES `fee_profit_tip_config` WRITE;
/*!40000 ALTER TABLE `fee_profit_tip_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `fee_profit_tip_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-24 22:48:09
