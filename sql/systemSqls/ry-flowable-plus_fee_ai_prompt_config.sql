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
-- Table structure for table `fee_ai_prompt_config`
--

DROP TABLE IF EXISTS `fee_ai_prompt_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_ai_prompt_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scene_code` varchar(50) NOT NULL COMMENT '场景编码（EXPENSE_BASIC/ASSET/LOAN/FOREIGN）',
  `scene_name` varchar(100) NOT NULL COMMENT '场景名称',
  `prompt_template` text NOT NULL COMMENT 'AI引导话术模板',
  `question_sequence` text COMMENT '提问顺序JSON',
  `industry_code` varchar(50) DEFAULT '*' COMMENT '行业编码',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scene` (`scene_code`,`industry_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI引导话术配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fee_ai_prompt_config`
--

LOCK TABLES `fee_ai_prompt_config` WRITE;
/*!40000 ALTER TABLE `fee_ai_prompt_config` DISABLE KEYS */;
INSERT INTO `fee_ai_prompt_config` VALUES (1,'EXPENSE_BASIC','基础报销引导','您好！我来帮您填写报销单。请问您要报销什么类型的费用？我会引导您逐步完成填写。','[{\"step\":1,\"question\":\"请选择费用类型（物资小类）\",\"field\":\"itemCategory\"},{\"step\":2,\"question\":\"请选择用途标签\",\"field\":\"purposeTag\"},{\"step\":3,\"question\":\"请输入报销金额\",\"field\":\"amount\"},{\"step\":4,\"question\":\"请选择费用发生日期\",\"field\":\"expenseDate\"},{\"step\":5,\"question\":\"请补充备注说明（可选）\",\"field\":\"remark\"}]','*','ENABLED','','2026-07-23 19:02:10','','2026-07-23 19:02:10'),(2,'ASSET','固定资产报销引导','您好！您正在填写固定资产类报销。我会额外询问使用年限等信息，以便系统自动计算折旧。','[{\"step\":1,\"question\":\"请选择资产类别（物资小类）\",\"field\":\"itemCategory\"},{\"step\":2,\"question\":\"请选择用途标签\",\"field\":\"purposeTag\"},{\"step\":3,\"question\":\"请输入资产金额\",\"field\":\"amount\"},{\"step\":4,\"question\":\"请输入预计使用年限（年）\",\"field\":\"usefulLife\"},{\"step\":5,\"question\":\"请选择购入日期\",\"field\":\"expenseDate\"},{\"step\":6,\"question\":\"请补充备注说明（可选）\",\"field\":\"remark\"}]','*','ENABLED','','2026-07-23 19:02:10','','2026-07-23 19:02:10'),(3,'LOAN','借款报销引导','您好！您正在填写借款类报销单。请提供借款相关信息，系统将自动关联借款台账。','[{\"step\":1,\"question\":\"请选择借款类型\",\"field\":\"itemCategory\"},{\"step\":2,\"question\":\"请选择用途标签\",\"field\":\"purposeTag\"},{\"step\":3,\"question\":\"请输入借款金额\",\"field\":\"amount\"},{\"step\":4,\"question\":\"请选择借款日期\",\"field\":\"expenseDate\"},{\"step\":5,\"question\":\"请输入关联借款编号（可选）\",\"field\":\"projectCode\"},{\"step\":6,\"question\":\"请补充备注说明（可选）\",\"field\":\"remark\"}]','*','ENABLED','','2026-07-23 19:02:10','','2026-07-23 19:02:10'),(4,'FOREIGN','外币报销引导','您好！您正在填写外币报销单。请选择币种，系统将自动按当月汇率折算为人民币。','[{\"step\":1,\"question\":\"请选择币种\",\"field\":\"currencyCode\"},{\"step\":2,\"question\":\"请输入原币金额\",\"field\":\"originalAmount\"},{\"step\":3,\"question\":\"请选择费用类型（物资小类）\",\"field\":\"itemCategory\"},{\"step\":4,\"question\":\"请选择用途标签\",\"field\":\"purposeTag\"},{\"step\":5,\"question\":\"请选择费用发生日期\",\"field\":\"expenseDate\"},{\"step\":6,\"question\":\"请补充备注说明（可选）\",\"field\":\"remark\"}]','*','ENABLED','','2026-07-23 19:02:10','','2026-07-23 19:02:10');
/*!40000 ALTER TABLE `fee_ai_prompt_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-24 22:48:20
