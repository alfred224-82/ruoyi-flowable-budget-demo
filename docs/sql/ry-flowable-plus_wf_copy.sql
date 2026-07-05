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
-- Table structure for table `wf_copy`
--

DROP TABLE IF EXISTS `wf_copy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_copy` (
  `copy_id` bigint NOT NULL AUTO_INCREMENT COMMENT '抄送主键',
  `title` varchar(255) DEFAULT '' COMMENT '抄送标题',
  `process_id` varchar(64) DEFAULT '' COMMENT '流程主键',
  `process_name` varchar(255) DEFAULT '' COMMENT '流程名称',
  `category_id` varchar(255) DEFAULT '' COMMENT '流程分类主键',
  `deployment_id` varchar(64) DEFAULT '' COMMENT '部署主键',
  `instance_id` varchar(64) DEFAULT '' COMMENT '流程实例主键',
  `task_id` varchar(64) DEFAULT '' COMMENT '任务主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户主键',
  `originator_id` bigint DEFAULT NULL COMMENT '发起人主键',
  `originator_name` varchar(64) DEFAULT '' COMMENT '发起人名称',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`copy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程抄送表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_copy`
--

LOCK TABLES `wf_copy` WRITE;
/*!40000 ALTER TABLE `wf_copy` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_copy` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:23
