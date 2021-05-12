# kafka-poc

## Create and deploy a GitHub issue scraper as a custom Kafka Connect Source
- Get a command line container
```
docker run --rm -it -v ${PWD}:/tutorial --net=host landoop/fast-data-dev:latest bash
```
- Create topic:
```
kafka-topics --bootstrap-server 127.0.0.1:9092 --create --topic github-issues --partitions 3 --replication-factor 1 
```
- Build the sub-module `kafka-connect-github-source-connector`
```
mvn clean package
```
- Copy the fat jar (with dependencies) to your `connectors` docker volume location
```
cp ./kafka-connect-github-source-connector/target/kafka-connect-github-source-connector-1.0-jar-with-dependencies.jar ./docker/volumes/full-suite-single/connectors/.
```
- restart the kafka-connect container to force it to reload its source of connectors (This can be done by API instead)
- Go to the Kafka Connect UI to check if the connector was added
  - [Kafka Connect UI](http://localhost:8003 "By Landoop") `http://localhost:8003`
  - Press the NEW button, `GitHubSourceConnector` should be listed under `Sources`
  - This GUI could be used to deploy the connector. The REST API can also be used. Apply the property-file: `./kafka-connect-configs/kafka-connect-github-source.properties`
```
curl -X PUT \
  /api/kafka-connect-1/connectors/GitHubSourceConnectorDemo/config \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
  "connector.class": "no.eksempel.kafka.connector.sources.github.GitHubSourceConnector",
  "github.owner": "kubernetes",
  "tasks.max": "1",
  "topic": "github-issues",
  "github.repo": "kubernetes",
  "since.timestamp": "2021-04-01T00:00:00Z"
}'
```

## Create and deploy an Elasticsearch Kafka Connect Sink for the GitHub issues
- Go to the Kafka Connect UI to check if the connector was added
  - [Kafka Connect UI](http://localhost:8003 "By Landoop") `http://localhost:8003`
  - Press the NEW button, `Elastic Search` should be listed under `Sinks`
  - This GUI could be used to deploy the connector. The REST API can also be used. Apply the property-file: `./kafka-connect-configs/kafka-connect-elasticsearch-github-sink.properties`
```
curl -X PUT \
  /api/kafka-connect-1/connectors/ElasticsearchSinkConnector/config \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
  "connector.class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
  "type.name": "_doc",
  "topics": "github-issues",
  "tasks.max": "1",
  "key.ignore": "true",
  "key.converter.schemas.enable": "false",
  "value.converter.schema.registry.url": "http://kafka-schema-registry:8081",
  "value.converter.schemas.enable": "false",
  "connection.url": "http://es1:9200",
  "value.converter": "io.confluent.connect.avro.AvroConverter",
  "key.converter": "io.confluent.connect.avro.AvroConverter",
  "key.converter.schema.registry.url": "http://kafka-schema-registry:8081"
}'
```
- Check Elasticsearch for issues:
  - [Elasticsearch indices list](http://localhost:9200/_cat/indices?v "By Elasticsearch") `http://localhost:9200/_cat/indices?v`
  - Look for the line for the `github-issues` [index]. the field [docs.count] should be a number
  - Can also be viewed in Kibana





## Create and deploy a twitter scraper as a Kafka Connect Source
- Get a command line container
```
docker run --rm -it -v ${PWD}:/tutorial --net=host landoop/fast-data-dev:latest bash
```
- Create topics:
```
kafka-topics --bootstrap-server 127.0.0.1:9092 --create --topic twitter-status-connect --partitions 3 --replication-factor 1 
kafka-topics --bootstrap-server 127.0.0.1:9092 --create --topic twitter-deletes-connect --partitions 3 --replication-factor 1 
```
- We are going to use a twitter kafka source connector created and maintained by an employee of confluent. (It is added to kafka-connect in the docker-compose)
  - [kafka-connect-twitter on Connect Hub](https://www.confluent.io/hub/jcustenborder/kafka-connect-twitter "By Confluentinc") `https://www.confluent.io/hub/jcustenborder/kafka-connect-twitter`
  - [kafka-connect-twitter Source Code](https://github.com/jcustenborder/kafka-connect-twitter "By GitHub") `https://github.com/jcustenborder/kafka-connect-twitter`
  - [kafka-connect-twitter binaries v0.3.33](https://d1i4a15mxbxib1.cloudfront.net/api/plugins/jcustenborder/kafka-connect-twitter/versions/0.3.33/jcustenborder-kafka-connect-twitter-0.3.33.zip "By Cloudfront") `https://d1i4a15mxbxib1.cloudfront.net/api/plugins/jcustenborder/kafka-connect-twitter/versions/0.3.33/jcustenborder-kafka-connect-twitter-0.3.33.zip`
- Go to Kafka Connect UI to check if the connector is added
    - [Kafka Connect UI](http://localhost:8003 "By Landoop") `http://localhost:8003`
    - Press the NEW button, `TwitterSourceConnector` should be listed under `Sources`
    - This GUI could be used to deploy the connector. The REST API can also be used. Apply the property-file: `./kafka-connect-configs/kafka-connect-twitter-sink.properties`
    - To run this, a Twitter developer account must be created and credentials from there must be added
```
curl -X POST \
  /api/kafka-connect-1/connectors \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
  "name": "source-twitter-distributed",
  "config": {
    "tasks.max": "1",
    "connector.class": "com.github.jcustenborder.kafka.connect.twitter.TwitterSourceConnector",
    "twitter.oauth.consumerKey": "<Your Twitter Application Consumer Key>",
    "twitter.oauth.consumerSecret": "<Your Twitter Application Consumer Secret>",
    "twitter.oauth.accessToken": "<Your Twitter Application Token>",
    "twitter.oauth.accessTokenSecret": "<Your Twitter Application secret>",
    "process.deletes": "false",
    "filter.keywords": "programming,java,kafka,scala",
    "kafka.delete.topic": "demo-twitter-deletes",
    "kafka.status.topic": "demo-twitter-status"
  }
}'
```

## Create and deploy an Elasticsearch Kafka Connect Sink for the Tweets
- Go to the Kafka Connect UI to check if the connector was added
  - [Kafka Connect UI](http://localhost:8003 "By Landoop") `http://localhost:8003`
  - Press the NEW button, `Elastic Search` should be listed under `Sinks`
  - This GUI could be used to deploy the connector. The REST API can also be used. Apply the property file `./kafka-connect-configs/kafka-connect-elasticsearch-sink.properties`
  - Check Elasticsearch for tweets:
    - [Elasticsearch indices list](http://localhost:9200/_cat/indices?v "By Elasticsearch") `http://localhost:9200/_cat/indices?v`
    - Look for the line for the `twitter-status-connect` [index]. the field [docs.count] should be a number
    - Can also be viewed in Kibana


## Create and deploy a Kafka Streams Application for counting words in the tweets
- Get a command line container
```
docker run --rm -it -v ${PWD}:/tutorial --net=host landoop/fast-data-dev:latest bash
```
- Create topics:
```
kafka-topics --bootstrap-server 127.0.0.1:9092 --create --topic github-issues-word-count-output --partitions 3 --replication-factor 1 
```






### Topics in this poc
```
// CREATE TOPICS
kafka-topics --bootstrap-server 127.0.0.1:9092 --create --topic github-issues --partitions 3 --replication-factor 1 
kafka-topics --bootstrap-server 127.0.0.1:9092 --create --topic twitter-status-connect --partitions 3 --replication-factor 1 
kafka-topics --bootstrap-server 127.0.0.1:9092 --create --topic github-issues-word-count-output --partitions 3 --replication-factor 1 

// LIST TOPICS
kafka-topics --bootstrap-server 127.0.0.1:9092 --list

// DELETE TOPICS
DO NOT DO ON WINDOWS - KAFKA WILL CRASH!!!

// CREATE CONSUMER
kafka-console-consumer --bootstrap-server 127.0.0.1:9092 --topic github-issues-word-count-output --from-beginning --formatter kafka.tools.DefaultMessageFormatter --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```


### HTTP links
  - [Kafka REST Service](http://localhost:8082 "By Confluentinc") `http://localhost:8082`
  - [Kafka Connect REST Service](http://localhost:8083 "By Confluentinc") `http://localhost:8083`
  - [KSQLDB REST Service](http://localhost:8088 "By Confluentinc") `http://localhost:8088`
  - [Elasticsearch REST Service](http://localhost:9200 "By elastic") `http://localhost:9200`
  - [Kafka Topics UI](http://localhost:8000 "By Landoop") `http://localhost:8000`
  - [Schema Registry UI](http://localhost:8001 "By Landoop") `http://localhost:8001`
  - [Kafka Connect UI](http://localhost:8003 "By Landoop") `http://localhost:8003`
  - [Zookeeper Navigator UI](http://localhost:8004 "By elkozmon") `http://localhost:8004`
  - [Kibana Elasticsearch UI](http://localhost:5601 "By elastic") `http://localhost:5601`

### Docker volumes:
  - **connectors** `Mapping of folder to put custom kafka connecto jars for deployment` 
  - **elasticsearch-data** `All data for elasticsearch`
  - **kafka1** `All data for kafka node 1`
  - **zoo1** `All data for zookeeper node 1`
