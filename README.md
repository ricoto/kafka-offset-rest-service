# Kafka Offset Lag Checker as Rest Service

## Getting Started
Check out kafka-offset-rest-service to the /site directory.

```sh
$ cd /site
$ git clone https://github.com/ricoto/kafka-offset-rest-service.git
$ cd /site/kafka-offset-rest-service 
$ mvn clean install
```

## Configuration
By default, application configured to run against docker version of Confluent which you can run using:
```sh
$ docker run --rm -it -p 3181:3181 -p 3040:3040 -p 7081:7081 -p 7082:7082 -p 7083:7083 -p 7092:7092 -e ZK_PORT=3181 -e WEB_PORT=3040 -e REGISTRY_PORT=8081 -e REST_PORT=7082 -e CONNECT_PORT=7083 -e BROKER_PORT=7092 -e ADV_HOST=127.0.0.1 landoop/fast-data-dev
```  
If you want to run it against existing Confluent/Kafka, you need to change configuration in application.properties located in:
```sh
/site/kafka-offset-rest-service/src/main/resources/application.properties
```
and then do repackaging using:
```sh
$ mvn clean install
```    
## How to run 

1. Run as jar
```sh
    $ cd /site/kafka-offset-rest-service/target
    $ java -jar rest-service-1.0.jar
```
1. Run KafkaOffsetApplication as java application from IDE (if you using this way to run, you don't need to repackage after changes in application.properties)

## How to use 

After you start application, you will be able to make REST API calls to localhost:8090 (port configured in application.properties)

1. to retrieve offset information for particular group and topic 

    ```
    http://localhost:8090/offset/topic/TopicName/group/GroupId
    or
    http://localhost:8090/offset/group/GroupId/topic/TopicName
    ```

1. to retrieve offset information for group across all topics 

    ```
    http://localhost:8090/offset/group/GroupId
    ```
1. to retrieve offset information for particular topic across all groups

    ```
    http://localhost:8090/offset/topic/TopicName
    ```
1. to retrieve offset information for all topic and groups 

    ```
    http://localhost:8090/offset
    ```
 

## Response example


```
{
  "groupId" : "customGroupId",
  "maxOffsetLag" : 4,
  "topicInfo" : [ {
    "name" : "event-topic",
    "offsetLag" : 4
  } ],
  "topicOffsets" : [ {
    "topic" : "event-topic",
    "topicOffsetLag" : 4,
    "partitionOffsets" : [ {
      "partition" : 9,
      "currentOffset" : 5,
      "newestOffset" : 7,
      "offsetLag" : 2
    }, {
      "partition" : 10,
      "currentOffset" : 12,
      "newestOffset" : 13,
      "offsetLag" : 1
    }, {
      "partition" : 2,
      "currentOffset" : 11,
      "newestOffset" : 12,
      "offsetLag" : 1
    }, {
      "partition" : 11,
      "currentOffset" : 6,
      "newestOffset" : 6,
      "offsetLag" : 0
    }, {
      "partition" : 8,
      "currentOffset" : 10,
      "newestOffset" : 10,
      "offsetLag" : 0
    }, {
      "partition" : 4,
      "currentOffset" : 7,
      "newestOffset" : 7,
      "offsetLag" : 0
    }, {
      "partition" : 6,
      "currentOffset" : 9,
      "newestOffset" : 9,
      "offsetLag" : 0
    }, {
      "partition" : 0,
      "currentOffset" : 11,
      "newestOffset" : 11,
      "offsetLag" : 0
    }, {
      "partition" : 7,
      "currentOffset" : 9,
      "newestOffset" : 9,
      "offsetLag" : 0
    }, {
      "partition" : 3,
      "currentOffset" : 9,
      "newestOffset" : 9,
      "offsetLag" : 0
    }, {
      "partition" : 5,
      "currentOffset" : 12,
      "newestOffset" : 12,
      "offsetLag" : 0
    }, {
      "partition" : 1,
      "currentOffset" : 7,
      "newestOffset" : 7,
      "offsetLag" : 0
    } ]
  } ]
}

```
