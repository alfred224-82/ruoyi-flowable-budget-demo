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
-- Table structure for table `budget_validation_rule`
--

DROP TABLE IF EXISTS `budget_validation_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_validation_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码（唯一）',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_type` varchar(30) NOT NULL COMMENT '规则类型（REQUIRED-必填, MIN_AMOUNT-最低金额, MAX_AMOUNT-最高金额, RATIO_LIMIT-比例限制, SUM_CHECK-合计校验, FORMULA-公式校验）',
  `severity_level` varchar(20) NOT NULL DEFAULT 'ERROR' COMMENT '严重级别（ERROR-错误/红色阻断, WARNING-警告/黄色, INFO-建议/蓝色）',
  `subject_type` varchar(20) DEFAULT NULL COMMENT '适用科目类型（NULL表示全部类型）',
  `subject_code` varchar(20) DEFAULT NULL COMMENT '适用科目编码（NULL表示该类型全部）',
  `threshold_value` decimal(18,2) DEFAULT NULL COMMENT '阈值',
  `threshold_value2` decimal(18,2) DEFAULT NULL COMMENT '阈值2（用于范围校验）',
  `rule_expression` varchar(500) DEFAULT NULL COMMENT '规则表达式/说明',
  `error_message` varchar(500) NOT NULL COMMENT '校验不通过时的提示信息',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `description` varchar(500) DEFAULT NULL COMMENT '规则说明',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_subject_type` (`subject_type`),
  KEY `idx_severity` (`severity_level`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算科目规则校验表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_validation_rule`
--

LOCK TABLES `budget_validation_rule` WRITE;
/*!40000 ALTER TABLE `budget_validation_rule` DISABLE KEYS */;
INSERT INTO `budget_validation_rule` VALUES (1,'REQ_ALL','所有叶子科目必须填写预算金额','REQUIRED','INFO',NULL,NULL,NULL,NULL,NULL,'存在未填写预算金额的叶子科目，请补充完整',1,1,'检查所有叶子科目是否已填写预算金额','admin','2026-07-02 15:16:23','',NULL,NULL),(2,'MIN_INCOME','收入类科目预算金额不得为负','MIN_AMOUNT','ERROR','INCOME',NULL,0.00,NULL,'amount >= 0','收入类科目预算金额不能为负数',1,2,'收入类科目金额必须 >= 0','admin','2026-07-02 15:16:23','',NULL,NULL),(3,'MIN_COST','成本类科目预算金额不得为负','MIN_AMOUNT','ERROR','COST',NULL,0.00,NULL,'amount >= 0','成本类科目预算金额不能为负数',1,3,'成本类科目金额必须 >= 0','admin','2026-07-02 15:16:23','',NULL,NULL),(4,'MIN_EXPENSE','费用类科目预算金额不得为负','MIN_AMOUNT','ERROR','EXPENSE',NULL,0.00,NULL,'amount >= 0','费用类科目预算金额不能为负数',1,4,'费用类科目金额必须 >= 0','admin','2026-07-02 15:16:23','',NULL,NULL),(5,'MAX_SINGLE_EXPENSE','单项费用预算超过100万','MAX_AMOUNT','WARNING','EXPENSE',NULL,1000000.00,NULL,'amount <= 1000000','单项费用类预算金额超过100万元，请确认是否合理',1,5,'单项费用超过100万时警告','admin','2026-07-02 15:16:23','',NULL,NULL),(6,'MAX_SINGLE_COST','单项成本预算超过200万','MAX_AMOUNT','WARNING','COST',NULL,2000000.00,NULL,'amount <= 2000000','单项成本类预算金额超过200万元，请确认是否合理',1,6,'单项成本超过200万时警告','admin','2026-07-02 15:16:23','',NULL,NULL),(7,'RATIO_ADMIN_FEE','管理费用占费用类总额比例建议不超过40%','RATIO_LIMIT','WARNING','EXPENSE','3002',NULL,NULL,'subject_sum / type_total <= 0.4','管理费用占费用类总额比例超过40%，建议优化费用结构',1,7,'管理费用占比校验','admin','2026-07-02 15:16:23','',NULL,NULL),(8,'RATIO_SALES_FEE','销售费用占收入类总额比例建议不超过15%','RATIO_LIMIT','INFO','EXPENSE','3001',NULL,NULL,'sales_fee / income_total <= 0.15','销售费用占收入比例超过15%，建议关注费效比',1,8,'销售费用占收入比校验','admin','2026-07-02 15:16:23','',NULL,NULL),(9,'SUM_INCOME_NOT_ZERO','收入类预算总额不能为零','SUM_CHECK','ERROR','INCOME',NULL,NULL,NULL,'type_total > 0','收入类科目预算总额为零，请至少填写一项收入预算',1,9,'收入类合计不能为零','admin','2026-07-02 15:16:23','',NULL,NULL),(10,'INFO_ZERO_BASED','建议采用零基预算法逐项审核','FORMULA','INFO',NULL,NULL,NULL,NULL,NULL,'建议对每个科目采用零基预算法重新审核，避免惯性预算',1,10,'零基预算建议','admin','2026-07-02 15:16:23','',NULL,NULL),(11,'INFO_ROUND_CHECK','预算金额建议取整到百位','FORMULA','INFO',NULL,NULL,NULL,NULL,'amount % 100 == 0','预算金额未取整到百位，建议调整为整百数以提升预算编制质量',1,11,'金额取整建议','admin','2026-07-02 15:16:23','',NULL,NULL);
/*!40000 ALTER TABLE `budget_validation_rule` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:15
