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
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
INSERT INTO `sys_dict_data` VALUES (1,1,'男','0','sys_user_sex','','','Y','0','admin','2026-05-30 08:07:36','',NULL,'性别男'),(2,2,'女','1','sys_user_sex','','','N','0','admin','2026-05-30 08:07:36','',NULL,'性别女'),(3,3,'未知','2','sys_user_sex','','','N','0','admin','2026-05-30 08:07:36','',NULL,'性别未知'),(4,1,'显示','0','sys_show_hide','','primary','Y','0','admin','2026-05-30 08:07:36','',NULL,'显示菜单'),(5,2,'隐藏','1','sys_show_hide','','danger','N','0','admin','2026-05-30 08:07:36','',NULL,'隐藏菜单'),(6,1,'正常','0','sys_normal_disable','','primary','Y','0','admin','2026-05-30 08:07:36','',NULL,'正常状态'),(7,2,'停用','1','sys_normal_disable','','danger','N','0','admin','2026-05-30 08:07:36','',NULL,'停用状态'),(12,1,'是','Y','sys_yes_no','','primary','Y','0','admin','2026-05-30 08:07:36','',NULL,'系统默认是'),(13,2,'否','N','sys_yes_no','','danger','N','0','admin','2026-05-30 08:07:36','',NULL,'系统默认否'),(14,1,'通知','1','sys_notice_type','','warning','Y','0','admin','2026-05-30 08:07:36','',NULL,'通知'),(15,2,'公告','2','sys_notice_type','','success','N','0','admin','2026-05-30 08:07:36','',NULL,'公告'),(16,1,'正常','0','sys_notice_status','','primary','Y','0','admin','2026-05-30 08:07:36','',NULL,'正常状态'),(17,2,'关闭','1','sys_notice_status','','danger','N','0','admin','2026-05-30 08:07:36','',NULL,'关闭状态'),(18,1,'新增','1','sys_oper_type','','info','N','0','admin','2026-05-30 08:07:36','',NULL,'新增操作'),(19,2,'修改','2','sys_oper_type','','info','N','0','admin','2026-05-30 08:07:36','',NULL,'修改操作'),(20,3,'删除','3','sys_oper_type','','danger','N','0','admin','2026-05-30 08:07:36','',NULL,'删除操作'),(21,4,'授权','4','sys_oper_type','','primary','N','0','admin','2026-05-30 08:07:36','',NULL,'授权操作'),(22,5,'导出','5','sys_oper_type','','warning','N','0','admin','2026-05-30 08:07:36','',NULL,'导出操作'),(23,6,'导入','6','sys_oper_type','','warning','N','0','admin','2026-05-30 08:07:36','',NULL,'导入操作'),(24,7,'强退','7','sys_oper_type','','danger','N','0','admin','2026-05-30 08:07:36','',NULL,'强退操作'),(25,8,'生成代码','8','sys_oper_type','','warning','N','0','admin','2026-05-30 08:07:36','',NULL,'生成操作'),(26,9,'清空数据','9','sys_oper_type','','danger','N','0','admin','2026-05-30 08:07:36','',NULL,'清空操作'),(27,1,'成功','0','sys_common_status','','primary','N','0','admin','2026-05-30 08:07:36','',NULL,'正常状态'),(28,2,'失败','1','sys_common_status','','danger','N','0','admin','2026-05-30 08:07:36','',NULL,'停用状态'),(29,99,'其他','0','sys_oper_type','','info','N','0','admin','2026-05-30 08:07:36','',NULL,'其他操作'),(30,1,'进行中','running','wf_process_status','','primary','N','0','admin','2026-05-30 08:07:36','',NULL,'进行中状态'),(31,2,'已终止','terminated','wf_process_status','','danger','N','0','admin','2026-05-30 08:07:36','',NULL,'已终止状态'),(32,3,'已完成','completed','wf_process_status','','success','N','0','admin','2026-05-30 08:07:36','',NULL,'已完成状态'),(33,4,'已取消','canceled','wf_process_status','','warning','N','0','admin','2026-05-30 08:07:36','',NULL,'已取消状态');
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
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
