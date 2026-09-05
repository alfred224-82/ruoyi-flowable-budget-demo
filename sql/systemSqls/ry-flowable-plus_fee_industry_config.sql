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
-- Table structure for table `fee_industry_config`
--

DROP TABLE IF EXISTS `fee_industry_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_industry_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `industry_code` varchar(50) NOT NULL COMMENT '行业编码（INTERNET/REALESTATE/MANUFACTURING/FINANCE）',
  `industry_name` varchar(100) NOT NULL COMMENT '行业名称',
  `switches` text COMMENT '行业开关组JSON',
  `depreciation_defaults` text COMMENT '折旧默认参数JSON',
  `budget_thresholds` text COMMENT '预算管控阈值JSON',
  `compliance_rules` text COMMENT '行业合规拦截规则JSON',
  `ai_prompt_pack` text COMMENT 'AI引导话术包JSON',
  `profit_tip_pack` text COMMENT '盈利提示文案包JSON',
  `accrual_params` text COMMENT '计提规则参数JSON',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_industry_code` (`industry_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行业配置主数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_industry_config`
--

LOCK TABLES `fee_industry_config` WRITE;
/*!40000 ALTER TABLE `fee_industry_config` DISABLE KEYS */;
INSERT INTO `fee_industry_config` VALUES (1,'INTERNET','互联网行业','{\"enableMultiCurrency\":true,\"enableAccrual\":true,\"enableLoan\":true}',NULL,'{\"expenseWarning\":80,\"capitalWarning\":90,\"cashflowWarning\":85}',NULL,NULL,NULL,NULL,'ENABLED','','2026-07-23 17:17:43','','2026-07-23 17:17:43'),(2,'REALESTATE','房地产行业','{\"enableMultiCurrency\":false,\"enableAccrual\":true,\"enableLoan\":true}',NULL,'{\"expenseWarning\":75,\"capitalWarning\":85,\"cashflowWarning\":80}',NULL,NULL,NULL,NULL,'ENABLED','','2026-07-23 17:17:43','','2026-07-23 17:17:43'),(3,'MANUFACTURING','制造业','{\"enableMultiCurrency\":true,\"enableAccrual\":true,\"enableLoan\":true}',NULL,'{\"expenseWarning\":80,\"capitalWarning\":85,\"cashflowWarning\":90}',NULL,NULL,NULL,NULL,'ENABLED','','2026-07-23 17:17:43','','2026-07-23 17:17:43'),(4,'FINANCE','金融行业','{\"enableMultiCurrency\":true,\"enableAccrual\":true,\"enableLoan\":true,\"enableDesensitize\":true}',NULL,'{\"expenseWarning\":70,\"capitalWarning\":80,\"cashflowWarning\":75}',NULL,NULL,NULL,NULL,'ENABLED','','2026-07-23 17:17:43','','2026-07-23 17:17:43');
/*!40000 ALTER TABLE `fee_industry_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-24 22:48:06
