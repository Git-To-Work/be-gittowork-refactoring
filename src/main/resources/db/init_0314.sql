-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: gittowork
-- ------------------------------------------------------
-- Server version	8.0.40

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
-- Table structure for table `cover_letter`
--

DROP TABLE IF EXISTS `cover_letter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cover_letter` (
  `file_id` int NOT NULL,
  `user_id` int NOT NULL,
  `origin_name` varchar(255) NOT NULL,
  `file_url` varchar(255) NOT NULL,
  `create_dttm` timestamp NOT NULL,
  `title` varchar(255) NOT NULL,
  PRIMARY KEY (`file_id`),
  KEY `FK_user_TO_cover_letter_1` (`user_id`),
  CONSTRAINT `FK_user_TO_cover_letter_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cover_letter`
--

LOCK TABLES `cover_letter` WRITE;
/*!40000 ALTER TABLE `cover_letter` DISABLE KEYS */;
/*!40000 ALTER TABLE `cover_letter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cover_letter_analysis`
--

DROP TABLE IF EXISTS `cover_letter_analysis`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cover_letter_analysis` (
  `cover_letter_analysis_id` varchar(255) NOT NULL,
  `file_id` int NOT NULL,
  `user_id` int NOT NULL,
  `analysis_result` text,
  `global_capability` int DEFAULT NULL,
  `challenge_spirit` int DEFAULT NULL,
  `sincerity` int DEFAULT NULL,
  `communication_skill` int DEFAULT NULL,
  `achievement_orientation` int DEFAULT NULL,
  `responsibility` int DEFAULT NULL,
  `honesty` int DEFAULT NULL,
  `creativity` int DEFAULT NULL,
  `create_dttm` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`cover_letter_analysis_id`),
  KEY `FK_cover_letter_TO_cover_letter_analysis_1` (`file_id`),
  KEY `FK_user_TO_cover_letter_analysis_1` (`user_id`),
  CONSTRAINT `FK_cover_letter_TO_cover_letter_analysis_1` FOREIGN KEY (`file_id`) REFERENCES `cover_letter` (`file_id`),
  CONSTRAINT `FK_user_TO_cover_letter_analysis_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cover_letter_analysis`
--

LOCK TABLES `cover_letter_analysis` WRITE;
/*!40000 ALTER TABLE `cover_letter_analysis` DISABLE KEYS */;
/*!40000 ALTER TABLE `cover_letter_analysis` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fields`
--

DROP TABLE IF EXISTS `fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fields` (
  `field_id` int NOT NULL,
  `field_name` varchar(100) NOT NULL,
  `field_img_url` varchar(255) NOT NULL,
  PRIMARY KEY (`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fields`
--

LOCK TABLES `fields` WRITE;
/*!40000 ALTER TABLE `fields` DISABLE KEYS */;
/*!40000 ALTER TABLE `fields` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` int NOT NULL,
  `github_id` int NOT NULL,
  `github_name` varchar(100) NOT NULL,
  `name` varchar(30) DEFAULT NULL,
  `github_email` varchar(100) DEFAULT NULL,
  `phone` varchar(13) DEFAULT NULL,
  `birth_dt` date DEFAULT NULL,
  `experience` int DEFAULT '0',
  `location` varchar(100) DEFAULT NULL,
  `create_dttm` timestamp NOT NULL,
  `update_dttm` timestamp NULL DEFAULT NULL,
  `privacy_consent_dttm` timestamp NULL DEFAULT NULL,
  `github_access_token` varchar(255) DEFAULT NULL,
  `interest_fields` varchar(255) DEFAULT NULL,
  `delete_dttm` timestamp NULL DEFAULT NULL,
  `notification_agree_dttm` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_alert_logs`
--

DROP TABLE IF EXISTS `user_alert_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_alert_logs` (
  `alert_id` int NOT NULL,
  `user_id` int NOT NULL,
  `alert_type` varchar(50) NOT NULL,
  `message` varchar(255) NOT NULL,
  `create_dttm` timestamp NOT NULL,
  PRIMARY KEY (`alert_id`),
  KEY `FK_user_TO_user_alert_logs_1` (`user_id`),
  CONSTRAINT `FK_user_TO_user_alert_logs_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_alert_logs`
--

LOCK TABLES `user_alert_logs` WRITE;
/*!40000 ALTER TABLE `user_alert_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_alert_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_blacklist`
--

DROP TABLE IF EXISTS `user_blacklist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_blacklist` (
  `user_id` int NOT NULL,
  `company_id` int NOT NULL,
  `delete_dttm` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FK_user_TO_user_blacklist_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_blacklist`
--

LOCK TABLES `user_blacklist` WRITE;
/*!40000 ALTER TABLE `user_blacklist` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_blacklist` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_favorites`
--

DROP TABLE IF EXISTS `user_favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_favorites` (
  `user_id` int NOT NULL,
  `company_id` int NOT NULL,
  `delete_dttm` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FK_user_TO_user_favorites_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_favorites`
--

LOCK TABLES `user_favorites` WRITE;
/*!40000 ALTER TABLE `user_favorites` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_favorites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_git_info`
--

DROP TABLE IF EXISTS `user_git_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_git_info` (
  `user_id` int NOT NULL,
  `avartar_url` varchar(255) NOT NULL,
  `public_repositories` int NOT NULL,
  `followers` int NOT NULL,
  `followings` int NOT NULL,
  `create_dttm` timestamp NOT NULL,
  `update_dttm` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FK_user_TO_user_git_info_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_git_info`
--

LOCK TABLES `user_git_info` WRITE;
/*!40000 ALTER TABLE `user_git_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_git_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_likes`
--

DROP TABLE IF EXISTS `user_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_likes` (
  `user_id` int NOT NULL,
  `company_id` int NOT NULL,
  `delete_dttm` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FK_user_TO_user_likes_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_likes`
--

LOCK TABLES `user_likes` WRITE;
/*!40000 ALTER TABLE `user_likes` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_likes` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-03-14 11:43:20
