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
-- Table structure for table `budget_risk_rule`
--

DROP TABLE IF EXISTS `budget_risk_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_risk_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码（唯一）',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_category` varchar(20) NOT NULL COMMENT '规则类别（BUDGET_OVER/LOGIC_ERROR/SUBJECT_ERROR/MISSING_DATA/HISTORICAL_COMPARE）',
  `threshold_value` decimal(10,2) DEFAULT NULL COMMENT '阈值',
  `risk_level` varchar(10) NOT NULL DEFAULT 'MEDIUM' COMMENT '风险等级（HIGH/MEDIUM/LOW）',
  `rule_description` varchar(500) DEFAULT NULL COMMENT '规则描述',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算风控规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_risk_rule`
--

LOCK TABLES `budget_risk_rule` WRITE;
/*!40000 ALTER TABLE `budget_risk_rule` DISABLE KEYS */;
INSERT INTO `budget_risk_rule` VALUES (1,'RULE_001','预算超标检查','BUDGET_OVER',20.00,'HIGH','部门预算与上月实际偏差超过±20%',1,1,'admin','2026-05-30 11:26:01','',NULL,NULL),(2,'RULE_002','汇总逻辑检查','LOGIC_ERROR',NULL,'HIGH','明细合计与汇总金额不一致',1,2,'admin','2026-05-30 11:26:01','',NULL,NULL),(3,'RULE_003','科目归类检查','SUBJECT_ERROR',NULL,'MEDIUM','科目编码不存在于科目表',1,3,'admin','2026-05-30 11:26:01','',NULL,NULL),(4,'RULE_004','关键指标完整性检查','MISSING_DATA',NULL,'MEDIUM','必填字段存在空值',1,4,'admin','2026-05-30 11:26:01','',NULL,NULL),(5,'RULE_005','历史数据对比','HISTORICAL_COMPARE',30.00,'LOW','本期与上期同期波动超过±30%',1,5,'admin','2026-05-30 11:26:01','',NULL,NULL);
/*!40000 ALTER TABLE `budget_risk_rule` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:21
