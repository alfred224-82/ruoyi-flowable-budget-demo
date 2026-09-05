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
-- Table structure for table `fee_workflow_node_config`
--

DROP TABLE IF EXISTS `fee_workflow_node_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_workflow_node_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_id` bigint NOT NULL COMMENT '所属工作流ID（关联fee_workflow_config.id）',
  `node_seq` int NOT NULL COMMENT '节点顺序（从1开始）',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称（如：部门经理审批）',
  `role_id` bigint NOT NULL COMMENT '审批角色ID（关联sys_role.role_id）',
  `role_key` varchar(100) DEFAULT NULL COMMENT '角色标识（冗余，如：deptManager）',
  `role_name` varchar(100) DEFAULT NULL COMMENT '角色名称（冗余，如：部门经理）',
  `approval_type` varchar(20) NOT NULL DEFAULT 'ANY' COMMENT '审批方式（ANY单人审批/ALL会签）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_seq` (`workflow_id`,`node_seq`),
  KEY `idx_workflow_id` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流审批节点配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_workflow_node_config`
--

LOCK TABLES `fee_workflow_node_config` WRITE;
/*!40000 ALTER TABLE `fee_workflow_node_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `fee_workflow_node_config` ENABLE KEYS */;
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
