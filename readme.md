# README

Project to Demo Kafka usage for long running processes.

Long running process
https://medium.com/codex/dealing-with-long-running-jobs-using-apache-kafka-192f053e1691

Using Avro schemas (not used)
https://www.baeldung.com/spring-cloud-stream-kafka-avro-confluent

Spring Cloud Stream
https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#_configuration_options

Spring cloud kafka binder properties with emphasis on pause and resume
https://cloud.spring.io/spring-cloud-stream-binder-kafka/spring-cloud-stream-binder-kafka.html#pause-resume

Using rest actuator calls
https://blog.devgenius.io/pause-resume-kafka-consumer-using-actuator-in-spring-cloud-stream-147250055e0d

Using BindingsLifecycleController
https://docs.spring.io/spring-cloud-stream/docs/3.1.3/reference/html/spring-cloud-stream.html#binding_visualization_control

Writing consumers and producers in newer versions of spring cloud stream
https://stackoverflow.com/questions/65978055/enablebinding-output-input-deprecated-since-version-of-3-1-of-spring-cloud-str

Custom java DNS aliases (not used)
https://stackoverflow.com/questions/11647629/how-to-configure-hostname-resolution-to-use-a-custom-dns-server-in-java

Hosts file Win:
c:\Windows\System32\Drivers\etc\hosts  
Add to hosts:
```
127.0.0.1	broker-1
```

## Commands
```
docker-compose -f .\docker-compose.yml -p kafka-long-process up -d
```
Pause and resume consumer
```
http://localhost:8280/consumer/pause/messagesConsumer-in-0
http://localhost:8280/consumer/resume/messagesConsumer-in-0
```
