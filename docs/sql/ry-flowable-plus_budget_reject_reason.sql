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
-- Table structure for table `budget_reject_reason`
--

DROP TABLE IF EXISTS `budget_reject_reason`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_reject_reason` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reason_code` varchar(50) NOT NULL COMMENT '理由编码（唯一）',
  `reason_content` varchar(500) NOT NULL COMMENT '理由内容',
  `reject_level` varchar(10) NOT NULL COMMENT '驳回层级（HQ/Branch）',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reason_code` (`reason_code`),
  KEY `idx_reject_level` (`reject_level`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='驳回理由库表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_reject_reason`
--

LOCK TABLES `budget_reject_reason` WRITE;
/*!40000 ALTER TABLE `budget_reject_reason` DISABLE KEYS */;
INSERT INTO `budget_reject_reason` VALUES (1,'HQ_001','全辖超标：整体预算超出公司年度预算配额','HQ','预算超标',1,1,'admin','2026-05-30 11:26:01','',NULL,NULL),(2,'HQ_002','汇总逻辑错：分公司汇总数据与部门明细不一致','HQ','逻辑错误',1,2,'admin','2026-05-30 11:26:01','',NULL,NULL),(3,'HQ_003','科目归类不符：预算科目使用错误','HQ','科目错误',1,3,'admin','2026-05-30 11:26:01','',NULL,NULL),(4,'HQ_004','数据偏差超±20%：与上月实际数据偏差过大','HQ','数据偏差',1,4,'admin','2026-05-30 11:26:01','',NULL,NULL),(5,'HQ_005','关键指标缺失：缺少必要的支撑材料或说明','HQ','数据缺失',1,5,'admin','2026-05-30 11:26:01','',NULL,NULL),(6,'BR_001','部门数据异常：某部门数据明显偏离正常范围','Branch','数据异常',1,1,'admin','2026-05-30 11:26:01','',NULL,NULL),(7,'BR_002','科目错：预算科目选择错误','Branch','科目错误',1,2,'admin','2026-05-30 11:26:01','',NULL,NULL),(8,'BR_003','计算错：公式计算错误或手工录入错误','Branch','计算错误',1,3,'admin','2026-05-30 11:26:01','',NULL,NULL),(9,'BR_004','超配额：超出分公司分配给部门的预算配额','Branch','预算超标',1,4,'admin','2026-05-30 11:26:01','',NULL,NULL),(10,'BR_005','缺支撑材料：缺少必要的附件或说明文档','Branch','数据缺失',1,5,'admin','2026-05-30 11:26:01','',NULL,NULL);
/*!40000 ALTER TABLE `budget_reject_reason` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:12
