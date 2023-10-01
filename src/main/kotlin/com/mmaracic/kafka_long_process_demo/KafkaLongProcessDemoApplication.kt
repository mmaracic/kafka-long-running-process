package com.mmaracic.kafka_long_process_demo

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.binding.BindingsLifecycleController
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.Executors
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.logging.Logger


fun main(args: Array<String>) {
    runApplication<KafkaLongProcessDemoApplication>(*args)
}

@SpringBootApplication
class KafkaLongProcessDemoApplication {

    private val logger = Logger.getLogger(KafkaLongProcessDemoApplication::class.java.name)

    //consumer - single
    @Bean
    fun messageConsumer(): Consumer<Message> {
        return Consumer {
            logger.info("Received message ${it.dateTime}")
        }
    }

    //consumer - batch
    @Bean
    fun messagesConsumer(): Consumer<List<Message>> {
        return Consumer {
            logger.info("Received batch message  count ${it.size}")
            it.forEach {
                logger.info("Received batched message ${it.dateTime}")
            }
        }
    }

    //Generic consumers are better for debugging errors while setting up, contain message headers with offsets and payload type
    //generic consumer - single
    @Bean
    fun genericMessageConsumer(): Consumer<GenericMessage<Message>> {
        return Consumer {
            logger.info("Received message ${it.payload.dateTime} on offset ${it.headers[KafkaHeaders.OFFSET]}")
            acknowledgeMessage(it, true)
        }
    }

    //generic consumer - batch
    //if you don't acknowledge messages it will get resent on group restart
    //but until restart next messages will continue coming although all of them will be marked as not acknowledged, which can result in more than once processing
    @Bean
    fun genericMessagesConsumer(): Consumer<GenericMessage<List<Message>>> {
        return Consumer {
            logger.info("Received batch message  count ${it.payload.size}, offsets ${it.headers[KafkaHeaders.OFFSET]}")
            it.payload.forEachIndexed {index, it1 ->
                logger.info("Received batched message ${it1.dateTime}")
                //partial batch acknowledge in case of ackMode manual_immediate
                //acknowledgeMessages(it, index, true)
            }
            acknowledgeMessage(it, true)
        }
    }

    //generic consumer - batch
    //Long process without pausing that should be rebalanced, will process same messages over and over because there is only one consumer
    @Bean
    fun genericMessagesConsumerLongRebalanced(): Consumer<GenericMessage<List<Message>>> {
        return Consumer {
            logger.info("Received batch message  count ${it.payload.size}, offsets ${it.headers[KafkaHeaders.OFFSET]}")
            it.payload.forEach {it1 ->
                logger.info("Received batched message ${it1.dateTime}")
                doWork()
            }
            acknowledgeMessage(it, true)
        }
    }

    //generic consumer - batch
    //Long process with pausing that should NOT be rebalanced, will continuously process new messages
    @Bean
    fun genericMessagesConsumerLongCompleted(consumerManipulator: ConsumerManipulator): Consumer<GenericMessage<List<Message>>> {
        return Consumer {
            logger.info("Received batch message  count ${it.payload.size}, offsets ${it.headers[KafkaHeaders.OFFSET]}")
            consumerManipulator.pauseConsumer("genericMessagesConsumerLongCompleted-in-0")
            doBatchWork(it) {
                consumerManipulator.resumeConsumer("genericMessagesConsumerLongCompleted-in-0")
                acknowledgeMessage(it, true)
            }
            logger.info("Offloaded batch work")
        }
    }

    //Producer1 - automated
    @Bean
    fun messageProducerAutomated(): Supplier<Message> {
        return Supplier { Message(OffsetDateTime.now()) }
    }

    //acknowledge a message or whole batch in ack-mode: MANUAL
    fun <T> acknowledgeMessage(message: GenericMessage<T>, ack: Boolean) {
        val acknowledgment = message.headers[KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment::class.java]
        if (acknowledgment != null) {
            if (ack) {
                acknowledgment.acknowledge()
                logger.info("Acknowledged message offset ${message.headers[KafkaHeaders.OFFSET]}")
            } else {
                //Method not implemented in generic interface
                //acknowledgment.nack(Duration.ofMillis(100))
                logger.info("Rejected message offset ${message.headers[KafkaHeaders.OFFSET]}")
            }
        } else {
            logger.warning("Acknowledgment NOT provided on message offset ${message.headers[KafkaHeaders.OFFSET]}")
        }
    }

    //Acknowledge a single batch message in batch mode: AckMode.MANUAL_IMMEDIATE
    fun <T> acknowledgeMessages(message: GenericMessage<T>, index: Int, ack: Boolean) {
        val acknowledgment = message.headers[KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment::class.java]
        if (acknowledgment != null) {
            logger.info("Acknowledgment provided on message offset ${message.headers[KafkaHeaders.OFFSET]} batch index $index")
            if (ack) {
                acknowledgment.acknowledge()
                logger.info("Acknowledged message offset ${message.headers[KafkaHeaders.OFFSET]}")
            } else {
                acknowledgment.nack(Duration.ofMillis(100))
                logger.info("Rejected message offset ${message.headers[KafkaHeaders.OFFSET]}")
            }
        } else {
            logger.warning("Acknowledgment NOT provided on message offset ${message.headers[KafkaHeaders.OFFSET]}")
        }
    }

    fun doBatchWork(messages: GenericMessage<List<Message>>, callback: () -> Unit) {
        val executorService = Executors.newFixedThreadPool(1)
        executorService.submit {
            messages.payload.forEach { it1 ->
                logger.info("Received batched message ${it1.dateTime}")
                doWork()
            }
            callback()
        }
        executorService.shutdown()
    }

    fun doWork() {
        //Pool interval is 1000ms so non-paused consumers should be rebalanced while doing this work
        logger.info("Starting the work on message")
        Thread.sleep(2000)
        logger.info("Completing the work on message")
    }
}

//Producer2 - manual
@Service
class ProducerManual(private val streamBridge: StreamBridge) {

    private val logger = Logger.getLogger(ProducerManual::class.java.name)

    fun publish(message: Message) {
        logger.info("Producing message: $message")
        streamBridge.send("messageProducerAutomated-out-0", message)
    }
}

//Required to handle default message type deserialisation which is json. Uses jackson under the hood, used in yml config
class MessageDeserializer : JsonDeserializer<Message?>()

class Message(@JsonProperty("dateTime") val dateTime: OffsetDateTime)

@Component
class ConsumerManipulator(val bindingsController: BindingsLifecycleController) {

    private val logger = Logger.getLogger(ConsumerManipulator::class.java.name)

    //Name is the name of the binding in yml file
    fun pauseConsumer(name: String): Boolean {
        val binding = bindingsController.queryState(name).first()
        return if (binding.isRunning) {
            logger.info("Pausing consumer $name")
            bindingsController.changeState(name, BindingsLifecycleController.State.PAUSED);
            if (binding.isPaused) {
                logger.info("Consumer $name is paused")
                true
            } else {
                logger.warning("Consumer $name is not paused")
                false
            }
        } else {
            logger.warning("Consumer $name is not running; can not pause")
            false
        }
    }

    fun resumeConsumer(name: String): Boolean {
        val binding = bindingsController.queryState(name).first()
        return if (binding.isPaused) {
            logger.info("Resuming consumer $name")
            bindingsController.changeState(name, BindingsLifecycleController.State.RESUMED);
            if (binding.isRunning) {
                logger.info("Consumer $name is running")
                true
            } else {
                logger.warning("Consumer $name is not running")
                false
            }
        } else {
            logger.warning("Consumer $name is running; can not resume")
            false
        }
    }
}

data class StatusInfo(
    val name: String,
    val action: String,
    val result: Boolean,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)

@RestController
@RequestMapping("consumer")
class ConsumerController(val consumerManipulator: ConsumerManipulator) {

    @GetMapping("/pause/{name}", produces = ["application/json"])
    fun pauseConsumer(@PathVariable("name") consumerName: String): StatusInfo {
        return StatusInfo(consumerName, "Paused", consumerManipulator.pauseConsumer(consumerName))
    }

    @GetMapping("/resume/{name}", produces = ["application/json"])
    fun resumeConsumer(@PathVariable("name") consumerName: String): StatusInfo {
        return StatusInfo(consumerName, "Resumed", consumerManipulator.resumeConsumer(consumerName))
    }
}
