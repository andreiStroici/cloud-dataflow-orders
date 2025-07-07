package com.sd.laborator

import com.sd.laborator.components.RabbitMqConnectionFactoryComponent
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.integration.annotation.Transformer
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.springframework.amqp.core.Message
//import org.springframework.messaging.support.MessageBuilder
import org.springframework.amqp.core.MessageBuilder
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeoutException
import kotlin.random.Random

@EnableBinding(Processor::class)
@SpringBootApplication
open class DepozitMicroservice
{
//    companion object {
//        ///TODO - modelare stoc depozit (baza de date cu stocurile de produse)
//        val stocProduse: List<Pair<String, Int>> = mutableListOf(
//            "Masca protectie" to 100,
//            "Vaccin anti-COVID-19" to 20,
//            "Combinezon" to 30,
//            "Manusa chirurgicala" to 40
//        )
//    }
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

    fun sendToProducer(msg:String)
    {
        this.amqpTemplate.convertAndSend(connectionFactory.getExchange1(),
            connectionFactory.getRoutingKey1(), msg)
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

    @RabbitListener(queues = ["\${producer.rabbitmq.queue}"])
    fun receiveFromProducer(message: String)
    {
        val (name, c) = message.split("|")
        stocProduse[name] = Pair(c.toInt() + stocProduse[name]!!.first, 0)
        sendMessageWithoutResponse("updateDepozit~$name|${stocProduse[name]!!.first}|" +
                "${stocProduse[name]!!.second}")
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

    var stocProduse: HashMap<String, Pair<Int, Int>> = HashMap<String, Pair<Int, Int>>()
    private fun acceptareComanda(identificator: Long): String {
        println("Comanda cu identificatorul $identificator a fost acceptata!")

        val produsDeExpediat = stocProduse["Masca protectie"]
        val cantitate = Random.nextInt( 100)

        return pregatireColet("Masca protectie", cantitate)
    }

    private fun respingereComanda(identificator: Long): String {
        println("Comanda cu identificatorul $identificator a fost respinsa! Stoc insuficient.")
        return "RESPINSA"
    }

    private fun verificareStoc(produs: String, cantitate: Int): Boolean {
        ///TODO - verificare stoc produs
        return stocProduse[produs]!!.first >= cantitate
    }

    private fun pregatireColet(produs: String, cantitate: Int): String {
        ///TODO - retragere produs de pe stoc in cantitatea specificata
        println("Produsul $produs in cantitate de $cantitate buc. este pregatit de livrare.")
        return "$produs|$cantitate"
    }

    @Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
    fun procesareComanda(comanda: String?): String
    {
        try {
            var res = sendMessage("getCommand~$comanda")
            println("######################${res}")
            println("~~~~~~~~~~~~~~~~~~~~~~${this.response}")
            while(res.startsWith("Eroare"))
            {
                res = sendMessage("getCommand~$comanda")
            }
            this.response = res
            val (name, cuantity) = this.response.split("|")
            res = sendMessage("depozitStock~")
            println("%%%%%%%%%%%%%%%%%%%%%%%${res}")
            println("!!!!!!!!!!!!!!!!!!!!!!!${this.response}")
            while(res.startsWith("Eroare"))
            {
                res = sendMessage("depozitStock~")
            }
            this.response = res
            this.stocProduse = Json.decodeFromString(this.response)
            val identificatorComanda = comanda!!.toLong()
            println("Procesez comanda cu identificatorul $identificatorComanda...")

            //TODO - procesare comanda in depozit
            val rezultatProcesareComanda: String = if (verificareStoc(name, cuantity.toInt()))
            {
                stocProduse[name] = Pair(stocProduse[name]!!.first - cuantity.toInt(),
                    stocProduse[name]!!.second + 1)
                sendMessageWithoutResponse("updateDepozit~$name|${stocProduse[name]!!.first}|" +
                        "${stocProduse[name]!!.second}")
                acceptareComanda(identificatorComanda)
            }
            else
            {
                stocProduse[name] = Pair(stocProduse[name]!!.first, stocProduse[name]!!.second + 1)
                sendMessageWithoutResponse("updateDepozit~$name|${stocProduse[name]!!.first}|" +
                        "${stocProduse[name]!!.second}")
                sendMessageWithoutResponse("respingereComanda~$comanda|Stoc insuficient")
                respingereComanda(identificatorComanda)
            }
            if(stocProduse[name]!!.second>=3)
            {
                sendToProducer("$name|${stocProduse[name]!!.first * 2}")
            }
            ///TODO - in loc sa se trimita mesajul cu toate datele in continuare, trebuie trimis doar ID-ul comenzii
        }
        catch (e:Exception)
        {
            sendMessageWithoutResponse("respingereComanda~$comanda|Eroare procesare comanda")
            println(e.message)
            e.printStackTrace()
        }
        return "$comanda"

    }
}

fun main(args: Array<String>) {
    runApplication<DepozitMicroservice>(*args)
}