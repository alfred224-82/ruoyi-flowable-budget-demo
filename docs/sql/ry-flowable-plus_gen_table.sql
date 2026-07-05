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
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL COMMENT '编号',
  `table_name` varchar(200) DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `package_name` varchar(100) DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table`
--

LOCK TABLES `gen_table` WRITE;
/*!40000 ALTER TABLE `gen_table` DISABLE KEYS */;
INSERT INTO `gen_table` VALUES (2060569118145949698,'budget_detail','预算明细表',NULL,NULL,'BudgetDetail','crud','com.ruoyi.system','system','detail','预算明细','ruoyi','0','/',NULL,'admin','2026-05-30 11:26:00','admin','2026-05-30 11:26:00',NULL),(2060569118359859201,'budget_email_log','预算邮件发送日志表',NULL,NULL,'BudgetEmailLog','crud','com.ruoyi.system','system','emailLog','预算邮件发送日志','ruoyi','0','/',NULL,'admin','2026-05-30 11:26:01','admin','2026-05-30 11:26:01',NULL),(2060569118494076932,'budget_operation_log','预算操作日志表',NULL,NULL,'BudgetOperationLog','crud','com.ruoyi.system','system','operationLog','预算操作日志','ruoyi','0','/',NULL,'admin','2026-05-30 11:26:01','admin','2026-05-30 11:26:01',NULL),(2060569118561185804,'budget_reject_history','驳回历史记录表',NULL,NULL,'BudgetRejectHistory','crud','com.ruoyi.system','system','rejectHistory','驳回历史记录','ruoyi','0','/',NULL,'admin','2026-05-30 11:26:00','admin','2026-05-30 11:26:00',NULL),(2060569118636683276,'budget_reject_reason','驳回理由库表',NULL,NULL,'BudgetRejectReason','crud','com.ruoyi.system','system','rejectReason','驳回理由库','ruoyi','0','/',NULL,'admin','2026-05-30 11:26:01','admin','2026-05-30 11:26:01',NULL),(2060569118703792142,'budget_risk_rule','预算风控规则表',NULL,NULL,'BudgetRiskRule','crud','com.ruoyi.system','system','riskRule','预算风控规则','ruoyi','0','/',NULL,'admin','2026-05-30 11:26:01','admin','2026-05-30 11:26:01',NULL),(2060569118770901007,'budget_sheet','预算单头表',NULL,NULL,'BudgetSheet','crud','com.ruoyi.system','system','sheet','预算单头','ruoyi','0','/',NULL,'admin','2026-05-30 11:26:00','admin','2026-05-30 11:26:00',NULL),(2060569119173554178,'budget_subject','预算科目表',NULL,NULL,'BudgetSubject','crud','com.ruoyi.system','system','subject','预算科目','ruoyi','0','/',NULL,'admin','2026-05-30 11:26:00','admin','2026-05-30 11:26:00',NULL);
/*!40000 ALTER TABLE `gen_table` ENABLE KEYS */;
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
