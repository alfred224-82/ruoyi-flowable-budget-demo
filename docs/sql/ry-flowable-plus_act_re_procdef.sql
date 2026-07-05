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
-- Table structure for table `act_re_procdef`
--

DROP TABLE IF EXISTS `act_re_procdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_procdef` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `VERSION_` int NOT NULL,
  `DEPLOYMENT_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  `ENGINE_VERSION_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_VERSION_` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_procdef`
--

LOCK TABLES `act_re_procdef` WRITE;
/*!40000 ALTER TABLE `act_re_procdef` DISABLE KEYS */;
INSERT INTO `act_re_procdef` VALUES ('Process_1783061114424:10:5a0b11ae-76bc-11f1-93d1-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',10,'59fdf24c-76bc-11f1-93d1-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:11:3048c476-7745-11f1-8f18-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',11,'303144d4-7745-11f1-8f18-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:12:94bf8a4e-7747-11f1-91f3-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',12,'94abb42c-7747-11f1-91f3-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:13:be290ab1-7747-11f1-9f1f-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',13,'be17a58f-7747-11f1-9f1f-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:14:5eca1d9c-774c-11f1-9184-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',14,'5eb6206a-774c-11f1-9184-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:15:eac33fb1-774c-11f1-bc8a-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',15,'eab6475f-774c-11f1-bc8a-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:16:c9478d62-774e-11f1-bd3c-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',16,'c93a9510-774e-11f1-bd3c-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:17:4df44911-7750-11f1-bf29-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',17,'4de41c6f-7750-11f1-bf29-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:18:5e2be500-7750-11f1-945a-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',18,'5e1f13be-7750-11f1-945a-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:19:bdfd780c-7750-11f1-bf28-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',19,'bdf0a6ca-7750-11f1-bf28-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:1:c470b8d5-76ac-11f1-94b4-28a06b026e93',2,'planflow','业务流程_预算编制审核','Process_1783061114424',1,'c3c64802-76ac-11f1-94b4-28a06b026e93','业务流程_预算编制审核.bpmn','业务流程_预算编制审核.Process_1783061114424.png',NULL,0,1,1,'',NULL,NULL,NULL,0),('Process_1783061114424:20:9893b555-7772-11f1-8780-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',20,'98820213-7772-11f1-8780-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:21:b2f9d2ee-7772-11f1-9818-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',21,'b2ed01ac-7772-11f1-9818-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:22:a09d652f-7773-11f1-8278-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',22,'a08d388d-7773-11f1-8278-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:23:1d55d870-7775-11f1-9c13-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',23,'1d47808e-7775-11f1-9c13-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:24:c8488243-7775-11f1-b825-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',24,'c83a2a61-7775-11f1-b825-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:25:3bdf80e4-7778-11f1-be43-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',25,'3bd08cc2-7778-11f1-be43-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:26:de4a2d27-7778-11f1-a56e-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',26,'de3d0dc5-7778-11f1-a56e-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:27:ab51f151-777c-11f1-81c7-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',27,'ab3ce2af-777c-11f1-81c7-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:28:aec26e1e-7815-11f1-80a9-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',28,'aeb093cc-7815-11f1-80a9-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:2:d4cf3e49-76ac-11f1-94b4-28a06b026e93',2,'planflow','业务流程_预算编制审核','Process_1783061114424',2,'d4998846-76ac-11f1-94b4-28a06b026e93','业务流程_预算编制审核.bpmn','业务流程_预算编制审核.Process_1783061114424.png',NULL,0,1,1,'',NULL,NULL,NULL,0),('Process_1783061114424:3:96e525bd-76af-11f1-94b4-28a06b026e93',2,'planflow','业务流程_预算编制审核','Process_1783061114424',3,'96b3404a-76af-11f1-94b4-28a06b026e93','业务流程_预算编制审核.bpmn','业务流程_预算编制审核.Process_1783061114424.png',NULL,0,1,1,'',NULL,NULL,NULL,0),('Process_1783061114424:4:7bb53295-76b0-11f1-afcd-28a06b026e93',2,'planflow','业务流程_预算编制审核','Process_1783061114424',4,'7b268722-76b0-11f1-afcd-28a06b026e93','业务流程_预算编制审核.bpmn','业务流程_预算编制审核.Process_1783061114424.png',NULL,0,1,1,'',NULL,NULL,NULL,0),('Process_1783061114424:5:a61b28f7-76b2-11f1-8b6c-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',5,'a60e7ec5-76b2-11f1-8b6c-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:6:d30467c1-76b7-11f1-bb42-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',6,'d2f7e49f-76b7-11f1-bb42-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:7:667c1107-76b9-11f1-976c-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',7,'666f66d5-76b9-11f1-976c-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:8:144bc346-76ba-11f1-86a9-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',8,'143ea3e4-76ba-11f1-86a9-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('Process_1783061114424:9:f74e6dd6-76bb-11f1-b57d-28a06b026e93',1,'http://www.ruoyi.com/budget','预算编制三级审批流程','Process_1783061114424',9,'f7414e74-76bb-11f1-b57d-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,1,'',NULL,NULL,NULL,0),('budgetProcess:1:b45b0dc6-76aa-11f1-b917-28a06b026e93',2,'http://www.ruoyi.com/budget','预算编制三级审批流程','budgetProcess',1,'b44d7934-76aa-11f1-b917-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,2,'',NULL,NULL,NULL,0),('budgetProcess:2:ac129faf-76ac-11f1-94b4-28a06b026e93',2,'http://www.ruoyi.com/budget','预算编制三级审批流程','budgetProcess',2,'ac05f57d-76ac-11f1-94b4-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,2,'',NULL,NULL,NULL,0),('budgetProcess:3:4595e929-76b0-11f1-958d-28a06b026e93',2,'http://www.ruoyi.com/budget','预算编制三级审批流程','budgetProcess',3,'457f7af7-76b0-11f1-958d-28a06b026e93','bpmn/budgetProcess.bpmn20.xml',NULL,'预算编制提交后依次流转到部门领导、分公司领导、总公司领导审批',0,0,2,'',NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `act_re_procdef` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:13
