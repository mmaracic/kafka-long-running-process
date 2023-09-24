# README

Project to Demo Kafka usage for long running processes.

Long running process
https://medium.com/codex/dealing-with-long-running-jobs-using-apache-kafka-192f053e1691

Using Avro schemas (not used)
https://www.baeldung.com/spring-cloud-stream-kafka-avro-confluent

Spring Cloud Stream
https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#_configuration_options

Spring cloud kafka binder properties
https://cloud.spring.io/spring-cloud-stream-binder-kafka/spring-cloud-stream-binder-kafka.html#pause-resume

Writing consumers and producers in newer versions of spring cloud stream
https://stackoverflow.com/questions/65978055/enablebinding-output-input-deprecated-since-version-of-3-1-of-spring-cloud-str

Custom java DNS aliases (not used)
https://stackoverflow.com/questions/11647629/how-to-configure-hostname-resolution-to-use-a-custom-dns-server-in-java

Hosts file Win:
c:\Windows\System32\Drivers\etc\hosts

## Commands
```
docker-compose -f .\docker-compose.yml -p kafka-long-process up -d
```
