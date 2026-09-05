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
-- Table structure for table `fee_fund_pool_config`
--

DROP TABLE IF EXISTS `fee_fund_pool_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_fund_pool_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `level_type` varchar(20) NOT NULL COMMENT '层级类型（DEPT/BRANCH/HQ）',
  `level_name` varchar(50) NOT NULL COMMENT '层级名称（部门级/分公司级/总公司级）',
  `role_id` bigint DEFAULT NULL COMMENT '关联角色ID（sys_role.role_id）',
  `role_key` varchar(100) DEFAULT NULL COMMENT '角色标识（冗余）',
  `role_name` varchar(100) DEFAULT NULL COMMENT '角色名称（冗余）',
  `initial_amount` decimal(18,2) NOT NULL COMMENT '初始额度',
  `warning_threshold` decimal(5,2) NOT NULL DEFAULT '80.00' COMMENT '预警阈值百分比',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态（ENABLED/DISABLED）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_level_type` (`level_type`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金池初始额度配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_fund_pool_config`
--

LOCK TABLES `fee_fund_pool_config` WRITE;
/*!40000 ALTER TABLE `fee_fund_pool_config` DISABLE KEYS */;
INSERT INTO `fee_fund_pool_config` VALUES (1,'DEPT','部门级',NULL,'deptManager','部门经理',100000.00,80.00,'ENABLED','','2026-07-24 22:31:23','','2026-07-24 22:31:23',NULL),(2,'BRANCH','分公司级',NULL,'branch','分公司管理员',500000.00,80.00,'ENABLED','','2026-07-24 22:31:23','','2026-07-24 22:31:23',NULL),(3,'HQ','总公司级',NULL,'headquarters','总公司管理员',1000000.00,80.00,'ENABLED','','2026-07-24 22:31:23','','2026-07-24 22:31:23',NULL);
/*!40000 ALTER TABLE `fee_fund_pool_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-24 22:48:08
