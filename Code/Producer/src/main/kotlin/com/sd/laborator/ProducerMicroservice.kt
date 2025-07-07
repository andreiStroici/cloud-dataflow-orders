package com.sd.laborator

import com.sd.laborator.components.RabbitMqConnectionFactoryComponent
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SpringBootApplication
open class ProducerMicroservice
{
    @Autowired
    private lateinit var connectionFactory: RabbitMqConnectionFactoryComponent
    private lateinit var amqpTemplate: AmqpTemplate

    @Autowired
    fun initTemplate()
    {
        this.amqpTemplate = connectionFactory.rabbitTemplate()
    }

    fun sendMessage(msg: String) {
        this.amqpTemplate.convertAndSend(connectionFactory.getExchange(),
            connectionFactory.getRoutingKey(),
            msg)
    }

    @RabbitListener(queues = ["\${producer.rabbitmq.queue}"])
    fun receiveMessage(msg:String)
    {
        println("!!!!!!$msg")
        val (name, cuantity) = msg.split("|")
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formatted = current.format(formatter)
        println("${formatted}:Se vor produce $cuantity de $name")
        Thread.sleep(cuantity.toLong())
        println("${formatted}:S-au produs $cuantity de $name")
        sendMessage("$name|$cuantity")
    }
}

fun main(args: Array<String>) {
    runApplication<ProducerMicroservice>()
}

