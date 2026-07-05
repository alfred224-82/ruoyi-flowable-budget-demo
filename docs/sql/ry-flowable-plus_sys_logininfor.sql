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
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL COMMENT '访问ID',
  `user_name` varchar(50) DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
  `status` char(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_logininfor`
--

LOCK TABLES `sys_logininfor` WRITE;
/*!40000 ALTER TABLE `sys_logininfor` DISABLE KEYS */;
INSERT INTO `sys_logininfor` VALUES (2060524259355803650,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-05-30 08:50:41'),(2060566849992175618,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-05-30 11:39:55'),(2060582320384692226,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-05-30 12:41:24'),(2060582383009845250,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-05-30 12:41:39'),(2060891867263438849,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-05-31 09:11:25'),(2060964922299912194,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-05-31 14:01:43'),(2067802283545616386,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-06-19 10:50:57'),(2072499035720814593,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-02 09:54:10'),(2072513142666735618,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-02 10:50:13'),(2072527735749292034,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-02 11:48:13'),(2072549585703301121,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-02 13:15:02'),(2072560259766579202,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-02 13:57:27'),(2072838347985465345,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 08:22:28'),(2072924771329937409,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 14:05:53'),(2072945367891755010,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 15:27:44'),(2072945514281353217,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 15:28:19'),(2072945552462102529,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 15:28:28'),(2072945567519653890,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 15:28:31'),(2072946042251952129,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 15:30:25'),(2072946094223572994,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','1','验证码错误','2026-07-03 15:30:37'),(2072946116738596865,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 15:30:42'),(2072947550808883202,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 15:36:24'),(2072947587546791938,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 15:36:33'),(2072948963572101121,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 15:42:01'),(2072949075711012866,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 15:42:28'),(2072950829253046273,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 15:49:26'),(2072950915676680194,'H00998','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 15:49:47'),(2072954663044034562,'H00998','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 16:04:40'),(2072954681444446209,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 16:04:44'),(2072961224638398466,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 16:30:44'),(2072961292875530242,'H00998','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 16:31:01'),(2072961500493578241,'H00998','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 16:31:50'),(2072961592399167490,'H00223','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 16:32:12'),(2072963424269815809,'H00223','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 16:39:29'),(2072963516653555713,'H00001','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 16:39:51'),(2072965035222290434,'H00001','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 16:45:53'),(2072965117661335554,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 16:46:13'),(2072965750414004226,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-03 16:48:43'),(2072966515379589122,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-03 16:51:46'),(2073217886515290113,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-04 09:30:37'),(2073228336577531906,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-04 10:12:09'),(2073228406765015042,'H00998','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-04 10:12:26'),(2073228486855249922,'H00998','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-04 10:12:45'),(2073228577695485954,'H00223','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-04 10:13:06'),(2073230624708767746,'H00223','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-04 10:21:14'),(2073230693591822337,'H00001','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-04 10:21:31'),(2073230902287806466,'H00001','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-04 10:22:21'),(2073230972538204162,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-04 10:22:37'),(2073233961109880834,'H00889','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-04 10:34:30'),(2073234020597694465,'H00998','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-04 10:34:44'),(2073234087614283777,'H00998','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','退出成功','2026-07-04 10:35:00'),(2073234139342635009,'H00001','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-04 10:35:12'),(2073295000585314306,'admin','127.0.0.1','内网IP','Chrome','Windows 10 or Windows Server 2016','0','登录成功','2026-07-04 14:37:03');
/*!40000 ALTER TABLE `sys_logininfor` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-05 10:33:18
