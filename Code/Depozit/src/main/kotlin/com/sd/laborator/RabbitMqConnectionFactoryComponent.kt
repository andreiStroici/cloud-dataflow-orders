package com.sd.laborator.components

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class RabbitMqConnectionFactoryComponent
{
    @Value("\${spring.rabbitmq.host}")
    private lateinit var host: String
    @Value("\${spring.rabbitmq.port}")
    private val port: Int = 0
    @Value("\${spring.rabbitmq.username}")
    private lateinit var username: String
    @Value("\${spring.rabbitmq.password}")
    private lateinit var password: String
    @Value("\${db.rabbitmq.exchange}")
    private lateinit var exchange: String
    @Value("\${db.rabbitmq.routingkey}")
    private lateinit var routingKey: String
    @Value("\${producer.rabbitmq.exchange}")
    private lateinit var exchange1: String
    @Value("\${producer.rabbitmq.routingkey}")
    private lateinit var routingKey1: String

    fun getExchange(): String = this.exchange

    fun getRoutingKey(): String = this.routingKey

    fun getExchange1(): String = this.exchange1

    fun getRoutingKey1(): String = this.routingKey1

    @Bean
    private fun connectionFactory(): ConnectionFactory {
        val connectionFactory = CachingConnectionFactory()
        connectionFactory.host = host
        connectionFactory.username = username
        connectionFactory.setPassword(password)
        connectionFactory.port = port
        return connectionFactory
    }

    @Bean
    fun rabbitTemplate(): RabbitTemplate = RabbitTemplate(this.connectionFactory())

}