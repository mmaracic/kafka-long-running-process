package com.mmaracic.kafka_long_process_demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.logging.Logger


fun main(args: Array<String>) {
    runApplication<KafkaLongProcessDemoApplication>(*args)
}

@SpringBootApplication
class KafkaLongProcessDemoApplication {

    private val logger = Logger.getLogger(KafkaLongProcessDemoApplication::class.java.name)

    //consumer
    @Bean
    fun messageConsumer(): Consumer<Message> {
        return Consumer { listen(message = it) }
    }

    fun listen(message: Message) {
        logger.info("Received message ${message.dateTime} ${message.title}")
    }

}

//Producer1 - automated
@Service
class ProducerAutomated {
    @Bean
    fun studentsChannel(): Supplier<Message> {
        return Supplier { Message("Message sample", OffsetDateTime.now()) }
    }
}

//Producer2 - manual
@Service
class ProducerManual(private val streamBridge: StreamBridge) {
    fun publish(message: Message) {
        streamBridge.send("studentsChannel-out-0", message)
    }
}

class Message(val title: String, val dateTime: OffsetDateTime)
