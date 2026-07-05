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
-- Table structure for table `budget_subject`
--

DROP TABLE IF EXISTS `budget_subject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `subject_code` varchar(20) NOT NULL COMMENT '科目编码（唯一，格式：一级1001，二级1001001）',
  `subject_name` varchar(100) NOT NULL COMMENT '科目名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父级科目ID（顶级为0）',
  `parent_code` varchar(20) DEFAULT '' COMMENT '父级科目编码（冗余字段，便于查询）',
  `level` int NOT NULL DEFAULT '1' COMMENT '科目层级（1-一级科目，2-二级科目，3-三级科目）',
  `ancestors` varchar(500) DEFAULT '' COMMENT '祖级列表（如：0/1001/1001001）',
  `is_leaf` tinyint(1) DEFAULT '0' COMMENT '是否叶子节点（0-否有子科目，1-是无子科目）',
  `sort_order` int DEFAULT '0' COMMENT '排序号（同级科目内的显示顺序）',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
  `description` varchar(500) DEFAULT NULL COMMENT '科目说明',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `subject_type` varchar(20) DEFAULT NULL COMMENT '科目类型（INCOME-收入类，COST-成本类，EXPENSE-费用类，ASSET-资产类，LIABILITY-负债类，EQUITY-权益类）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_code` (`subject_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`),
  KEY `idx_ancestors` (`ancestors`(100)),
  KEY `idx_subject_type` (`subject_type`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算科目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_subject`
--

LOCK TABLES `budget_subject` WRITE;
/*!40000 ALTER TABLE `budget_subject` DISABLE KEYS */;
INSERT INTO `budget_subject` VALUES (1,'1001','营业收入',0,'',1,'0',0,1,1,'企业主营业务收入','admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(2,'1001001','主营业务收入',1,'1001',2,'0/1001',0,1,1,'主要业务产生的收入','admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(3,'1001001001','产品销售收入',2,'1001001',3,'0/1001/1001001',1,1,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(4,'1001001002','服务收入',2,'1001001',3,'0/1001/1001001',1,2,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(5,'1001002','其他业务收入',1,'1001',2,'0/1001',1,2,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(6,'1002','营业成本',0,'',1,'0',0,2,1,'企业主营业务成本','admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(7,'1002001','主营业务成本',6,'1002',2,'0/1002',0,1,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(8,'1002001001','直接材料成本',7,'1002001',3,'0/1002/1002001',1,1,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(9,'1002001002','直接人工成本',7,'1002001',3,'0/1002/1002001',1,2,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(10,'1003','销售费用',0,'',1,'0',0,3,1,'销售过程中发生的费用','admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(11,'1003001','广告费',10,'1003',2,'0/1003',1,1,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(12,'1003002','运输费',10,'1003',2,'0/1003',1,2,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(13,'1004','管理费用',0,'',1,'0',0,4,1,'企业管理活动发生的费用','admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(14,'1004001','办公费',13,'1004',2,'0/1004',1,1,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(15,'1004002','差旅费',13,'1004',2,'0/1004',1,2,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(16,'1004003','业务招待费',13,'1004',2,'0/1004',1,3,1,NULL,'admin','2026-05-30 11:26:00','',NULL,NULL,'INCOME'),(17,'2001','营业成本',0,'',1,'0',0,1,1,'企业主营业务成本','admin','2026-07-02 12:17:57','',NULL,NULL,'COST'),(18,'2001001','主营业务成本',17,'2001',2,'0/2001',0,1,1,'主要业务发生的成本','admin','2026-07-02 12:17:57','',NULL,NULL,'COST'),(19,'2001001001','直接材料成本',18,'2001001',3,'0/2001/2001001',1,1,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'COST'),(20,'2001001002','直接人工成本',18,'2001001',3,'0/2001/2001001',1,2,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'COST'),(21,'2001001003','制造费用',18,'2001001',3,'0/2001/2001001',1,3,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'COST'),(22,'2002','其他业务成本',0,'',1,'0',0,2,1,'其他业务活动发生的成本','admin','2026-07-02 12:17:57','',NULL,NULL,'COST'),(23,'3001','销售费用',0,'',1,'0',0,1,1,'销售过程中发生的费用','admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(24,'3001001','广告费',23,'3001',2,'0/3001',1,1,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(25,'3001002','运输费',23,'3001',2,'0/3001',1,2,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(26,'3001003','销售人员薪酬',23,'3001',2,'0/3001',1,3,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(27,'3001004','业务招待费',23,'3001',2,'0/3001',1,4,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(28,'3002','管理费用',0,'',1,'0',0,2,1,'企业管理活动发生的费用','admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(29,'3002001','办公费',28,'3002',2,'0/3002',1,1,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(30,'3002002','差旅费',28,'3002',2,'0/3002',1,2,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(31,'3002003','管理人员薪酬',28,'3002',2,'0/3002',1,3,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(32,'3002004','折旧费',28,'3002',2,'0/3002',1,4,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(33,'3002005','咨询费',28,'3002',2,'0/3002',1,5,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(34,'3003','财务费用',0,'',1,'0',0,3,1,'企业筹资活动发生的费用','admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(35,'3003001','利息支出',34,'3003',2,'0/3003',1,1,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(36,'3003002','手续费',34,'3003',2,'0/3003',1,2,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(37,'3003003','汇兑损失',34,'3003',2,'0/3003',1,3,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EXPENSE'),(38,'4001','流动资产',0,'',1,'0',0,1,1,'一年内可变现的资产','admin','2026-07-02 12:17:57','',NULL,NULL,'ASSET'),(39,'4001001','货币资金',38,'4001',2,'0/4001',1,1,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'ASSET'),(40,'4001002','应收账款',38,'4001',2,'0/4001',1,2,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'ASSET'),(41,'4001003','存货',38,'4001',2,'0/4001',1,3,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'ASSET'),(42,'4002','固定资产',0,'',1,'0',0,2,1,'长期使用的资产','admin','2026-07-02 12:17:57','',NULL,NULL,'ASSET'),(43,'4002001','房屋建筑物',42,'4002',2,'0/4002',1,1,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'ASSET'),(44,'4002002','机器设备',42,'4002',2,'0/4002',1,2,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'ASSET'),(45,'4002003','运输工具',42,'4002',2,'0/4002',1,3,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'ASSET'),(46,'5001','流动负债',0,'',1,'0',0,1,1,'一年内需偿还的债务','admin','2026-07-02 12:17:57','',NULL,NULL,'LIABILITY'),(47,'5001001','短期借款',46,'5001',2,'0/5001',1,1,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'LIABILITY'),(48,'5001002','应付账款',46,'5001',2,'0/5001',1,2,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'LIABILITY'),(49,'5001003','应付职工薪酬',46,'5001',2,'0/5001',1,3,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'LIABILITY'),(50,'5002','长期负债',0,'',1,'0',0,2,1,'一年以上需偿还的债务','admin','2026-07-02 12:17:57','',NULL,NULL,'LIABILITY'),(51,'5002001','长期借款',50,'5002',2,'0/5002',1,1,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'LIABILITY'),(52,'5002002','应付债券',50,'5002',2,'0/5002',1,2,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'LIABILITY'),(53,'6001','所有者权益',0,'',1,'0',0,1,1,'所有者对企业净资产的所有权','admin','2026-07-02 12:17:57','',NULL,NULL,'EQUITY'),(54,'6001001','实收资本',53,'6001',2,'0/6001',1,1,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EQUITY'),(55,'6001002','资本公积',53,'6001',2,'0/6001',1,2,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EQUITY'),(56,'6001003','盈余公积',53,'6001',2,'0/6001',1,3,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EQUITY'),(57,'6001004','未分配利润',53,'6001',2,'0/6001',1,4,1,NULL,'admin','2026-07-02 12:17:57','',NULL,NULL,'EQUITY');
/*!40000 ALTER TABLE `budget_subject` ENABLE KEYS */;
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
