package com.sd.laborator
import com.sd.laborator.components.RabbitMqConnectionFactoryComponent
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import kotlin.collections.set

@EnableBinding(Sink::class)
@SpringBootApplication
open class LivrareMicroservice
{
    private var response = ""
    private val pendingResponses = ConcurrentHashMap<String, CompletableFuture<String>>()

    @Autowired
    private lateinit var connectionFactory: RabbitMqConnectionFactoryComponent
    private lateinit var amqpTemplate: AmqpTemplate

    @Autowired
    fun initTemplate()
    {
        this.amqpTemplate = connectionFactory.rabbitTemplate()
    }

    fun sendMessage(msg: String):String
    {
        val correlationId = UUID.randomUUID().toString()
        val future = CompletableFuture<String>()
        pendingResponses[correlationId] = future
        val message = MessageBuilder.withBody(msg.toByteArray())
            .setHeader("correlationId", correlationId)
            .setHeader("replyTo", "db.queue")
            .build()
        println("Mesaj trimis $correlationId: $msg")
        this.amqpTemplate.convertAndSend(connectionFactory.getExchange(),
            connectionFactory.getRoutingKey(),
            message)
        return try {
            future.get(1, java.util.concurrent.TimeUnit.SECONDS)
        }
        catch (e: TimeoutException)
        {
            println("Astept raspuns")
            "Eroare: Timeout"
        }
        catch (e: Exception)
        {

            "Eroare: ${e.message}"
        } finally {
            pendingResponses.remove(correlationId)
        }
    }

    @RabbitListener(queues = ["\${db.rabbitmq.queue}"])
    fun receiveMessage(message: Message) {
        val correlationId = message.messageProperties.headers["correlationId"] as? String
        val body = String(message.body)
        if (correlationId != null && pendingResponses.containsKey(correlationId))
        {
            println("Raspuns pentru $correlationId: $body")
            this.response = body
            pendingResponses[correlationId]?.complete(body)
            pendingResponses.remove(correlationId)
        }
        else
        {
            println("Mesaj neasteptat sau fara correlationId: $body")
        }
    }

    @StreamListener(Sink.INPUT)
    ///TODO - parametrul ar trebui sa fie doar numarul de inregistrare al comenzii si atat
    fun expediereComanda(comanda: String)
    {
        var res = sendMessage("checkStatus~$comanda")
        while(res.startsWith("Eroare"))
        {
            res = sendMessage("checkStatus~$comanda")
        }
        this.response = res
        if(response == "NULL")
        {
            println("S-a expediat urmatoarea comanda: $comanda")
        }
        else
        {
            println("Comanda $comanda a fost respinsa")
        }
    }
}

fun main(args: Array<String>)
{
    runApplication<LivrareMicroservice>(*args)
}