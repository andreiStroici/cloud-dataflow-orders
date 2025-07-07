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
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.integration.annotation.Transformer
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeoutException
import kotlin.collections.set
import kotlin.math.abs
import kotlin.random.Random

@EnableBinding(Processor::class)
@SpringBootApplication
open class FacturareMicroservice
{
    private val pendingResponses = ConcurrentHashMap<String, CompletableFuture<String>>()

    @Autowired
    private lateinit var connectionFactory: RabbitMqConnectionFactoryComponent
    private lateinit var amqpTemplate: AmqpTemplate

    private var response:String = "DEPOZIT GOL!!"
    @Autowired
    fun initTemplate()
    {
        this.amqpTemplate = connectionFactory.rabbitTemplate()
    }

    fun sendMessageWithoutResponse(msg: String)
    {
        val correlationId = UUID.randomUUID().toString()
        val future = CompletableFuture<String>()
        pendingResponses[correlationId] = future
        val message = MessageBuilder.withBody(msg.toByteArray())
            .setHeader("correlationId", correlationId)
            .build()
        println("Mesaj trimis $correlationId: $msg")
        this.amqpTemplate.convertAndSend(connectionFactory.getExchange(),
            connectionFactory.getRoutingKey(),
            message)
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

    @Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
    ///TODO - parametrul ar trebui sa fie doar numarul de inregistrare al comenzii si atat
    fun emitereFactura(comanda: String?): String
    {
        //val (identitateClient, produsComandat, cantitate, adresaLivrare) = comanda!!.split("|")
        println("Emit factura pentru comanda $comanda...")
        var res = sendMessage("checkStatus~$comanda")
        while(res.startsWith("Eroare"))
        {
            res = sendMessage("checkStatus~$comanda")
        }
        this.response = res
        println("!!!!!!!!!!!!!!!!!${this.response}")
        if(response == "NULL")
        {
            val nrFactura = abs(Random.nextInt())
            println("S-a emis factura cu nr $nrFactura.")
            sendMessageWithoutResponse("insertFactruare~$comanda")
        }
        else
        {
            println("Comanda $comanda este respinsa si nu pot face o factura")
        }

        ///TODO - inregistrare factura in baza de date
        return "$comanda"
    }
}

fun main(args: Array<String>)
{
    runApplication<FacturareMicroservice>(*args)
}