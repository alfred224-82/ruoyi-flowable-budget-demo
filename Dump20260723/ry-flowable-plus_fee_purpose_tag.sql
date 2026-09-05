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
-- Table structure for table `fee_purpose_tag`
--

DROP TABLE IF EXISTS `fee_purpose_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_purpose_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tag_code` varchar(50) NOT NULL COMMENT '标签编码',
  `tag_name` varchar(200) NOT NULL COMMENT '标签名称',
  `tag_icon` varchar(100) DEFAULT NULL COMMENT '标签图标',
  `default_accounting_class` varchar(50) DEFAULT 'EXPENSE' COMMENT '默认会计大类',
  `require_project` tinyint DEFAULT '0' COMMENT '是否要求项目编号(0否 1是)',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` varchar(20) DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_code` (`tag_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用途标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_purpose_tag`
--

LOCK TABLES `fee_purpose_tag` WRITE;
/*!40000 ALTER TABLE `fee_purpose_tag` DISABLE KEYS */;
INSERT INTO `fee_purpose_tag` VALUES (1,'DAILY_OPS','日常运营','?','EXPENSE',0,1,'ENABLED','','2026-07-13 22:17:00','','2026-07-13 22:17:00'),(2,'RD_INVEST','研发投入','?','EXPENSE',0,2,'ENABLED','','2026-07-13 22:17:00','','2026-07-13 22:17:00'),(3,'PROJECT_DELIVERY','项目交付','?','COST',1,3,'ENABLED','','2026-07-13 22:17:00','','2026-07-13 22:17:00'),(4,'MANUFACTURE','生产制造','?','COST',0,4,'ENABLED','','2026-07-13 22:17:00','','2026-07-13 22:17:00'),(5,'MARKET_ACT','市场活动','?','EXPENSE',0,5,'ENABLED','','2026-07-13 22:17:00','','2026-07-13 22:17:00');
/*!40000 ALTER TABLE `fee_purpose_tag` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-23 14:43:04
