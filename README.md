With the popularity of smartphones, people are more accustomed to watching news through mobile phones. Due to the accelerated pace of life, many people can only use fragmented time to obtain information. Therefore, the demand for mobile information clients is also increasing. It is against this background that the Dark Horse Toutiao project was developed. The leadnews project is implemented using the current hot microservice + big data technology architecture. This project mainly focuses on obtaining the latest and hottest news information, and accurately pushes consulting news through big data analysis of user preferences.

The technology stacks used in the project mainly include:
Vue+Echarts: We media system uses Vue to develop the key, integrates Echarts chart framework, completes related fan portraits, data analysis and other functions

Vue+Echarts+WebSocket: The management system is also developed using Vue, integrates Echarts, completes website statistics, content statistics and other functions, integrates WebSocket, and realizes the automatic update of real-time data of system kanban

Spring-Cloud-Gateway : The gateway service set up before microservices, implementing API request routing in service registration, and controlling flow rate control and fuse processing are commonly used architectural means, and these functions are naturally supported by Gateway

PMD&P3C : Static code scanning tool, scan the project code in the project, check abnormal points, optimization points, code specifications, etc., provide standardization for the development team, and improve the quality of the project code

Junit : In the idea of continuous integration, unit testing tends to automate the process, and the project realizes this process through the integration of Junit+Maven

Use the Spring Boot rapid development framework to build project projects; and combine with Spring Cloud family bucket technology to realize back-end personal center, self-media, management center and other micro-services.

Use WebMagic crawler technology to improve the automatic collection of system content

Use Kafka to complete internal system message notification; and client system message notification; and real-time data calculation

Use MyCat database middleware to calculate and separate system data into tables to improve system data layer performance

Use Redis caching technology to realize hot data calculation, NoSession and other functions to improve system performance indicators

Use Zoookeeper technology to complete the coordination and management of big data nodes and improve the high availability of the system storage layer

Use Mysql to store user data to ensure high performance of upper-level data query

Use Mongo to store user hot data to ensure high expansion and high performance indicators of user hot data

Use FastDFS as static resource storage, and implement functions such as hot static resource caching and elimination on it

Use Habse technology to store cold data in the system to ensure the reliability of system data

Use ES search technology to index cold data and article data to ensure the performance of cold data and article query

Use tools such as Sqoop and Kettle to realize offline warehousing of big data; or backup data to Hadoop

Use Spark+Hive for offline data analysis to realize various statistical reports in the system

Real-time data analysis and application using Spark Streaming + Hive+Kafka; such as article recommendation

Use Neo4j knowledge graph technology to analyze data relationships, produce knowledge results, and apply them to upper-level businesses to help users, self-media, and operational effects/capacities improve. such as fan rating
