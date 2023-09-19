package com.mmaracic.KafkaLongProcessDemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KafkaLongProcessDemoApplication

fun main(args: Array<String>) {
	runApplication<KafkaLongProcessDemoApplication>(*args)
}
