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
-- Table structure for table `fee_workflow_config`
--

DROP TABLE IF EXISTS `fee_workflow_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_workflow_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_key` varchar(50) NOT NULL COMMENT '工作流标识（budget_preparation/expense_approval/budget_adjustment）',
  `workflow_name` varchar(100) NOT NULL COMMENT '工作流名称',
  `description` varchar(500) DEFAULT NULL COMMENT '流程说明',
  `node_count` int NOT NULL DEFAULT '0' COMMENT '审批节点数量',
  `deploy_id` varchar(64) DEFAULT NULL COMMENT '当前已部署的Flowable部署ID',
  `proc_def_id` varchar(64) DEFAULT NULL COMMENT '当前流程定义ID',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态（DRAFT未部署/DEPLOYED已部署/DISABLED已停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_key` (`workflow_key`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流配置主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_workflow_config`
--

LOCK TABLES `fee_workflow_config` WRITE;
/*!40000 ALTER TABLE `fee_workflow_config` DISABLE KEYS */;
INSERT INTO `fee_workflow_config` VALUES (1,'budget_preparation','预算编制工作流','预算编制提交后的多级审批流程',3,NULL,NULL,'DRAFT','','2026-07-24 22:30:51','','2026-07-24 22:30:51',NULL),(2,'expense_approval','报销审核工作流','报销单提交后的审批流程',2,NULL,NULL,'DRAFT','','2026-07-24 22:30:51','','2026-07-24 22:30:51',NULL),(3,'budget_adjustment','预算调整工作流','预算调整/超预算申请的审批流程',3,NULL,NULL,'DRAFT','','2026-07-24 22:30:51','','2026-07-24 22:30:51',NULL);
/*!40000 ALTER TABLE `fee_workflow_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-24 22:48:21
