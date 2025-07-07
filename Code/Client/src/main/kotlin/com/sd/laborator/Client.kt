package com.sd.laborator

import com.sd.laborator.components.RabbitMqConnectionFactoryComponent
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Source
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.Poller
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue


data class Command(
    val identitate_client: String,
    val produs_comandat: String,
    val cantitate: Int,
    val adresa_livrare: String
)

@EnableBinding(Source::class)
@SpringBootApplication
open class ClientMicroservice
{
    companion object
    {
//        val listaProduse: List<String> = arrayListOf(
//            "Masca protectie",
//            "Vaccin anti-COVID-19",
//            "Combinezon",
//            "Manusa chirurgicala"
//        )

        ///TODO - lista de produse sa fie preluata din baza de date / din fisier
        val resource = ClassPathResource("lista_produse.txt")
        val listaProduse = resource.inputStream.bufferedReader().readLines()

        // Aici tinem ultimul Command primit de la Flask
        val comenziQueue: BlockingQueue<Command> = LinkedBlockingQueue()
    }

    @Bean
    //@InboundChannelAdapter(value = Source.OUTPUT, poller = [Poller(fixedDelay = "10000", maxMessagesPerPoll = "1")])
    @InboundChannelAdapter(value = Source.OUTPUT, poller = [Poller(fixedDelay = "0", maxMessagesPerPoll = "1")])
    open fun comandaProdus(): () -> Message<String>
    {
        return {
//            val produsComandat = listaProduse[(0 until listaProduse.size).shuffled()[0]]
//            val cantitate: Int = Random.nextInt(1, 100)
//            val identitateClient = "Popescu Ion"
//            val adresaLivrare = "Codrii Vlasiei nr 14"

            ///TODO - datele clientului sa fie citite dinamic si inregistrate in baza de date

            val comanda = comenziQueue.take()
            println("!!!!!!!!$comanda")
            val mesaj = "${comanda.identitate_client}|${comanda.produs_comandat}|" +
                    "${comanda.cantitate}|${comanda.adresa_livrare}"
            MessageBuilder.withPayload(mesaj).build()
        }
    }

    @RabbitListener(queues = ["\${comenzi.rabbitmq.queue}"])
    fun receiveCommand(message: String)
    {
        println("!!!!!!!!!!!Am primit mesajul $message")
        val parts = message.split("|")
        val comanda = Command(
            identitate_client = parts[0],
            produs_comandat = parts[1],
            cantitate = parts[2].toIntOrNull() ?: 0,
            adresa_livrare = parts[3]
        )

        comenziQueue.offer(comanda)
    }


}

fun main(args: Array<String>)
{
    runApplication<ClientMicroservice>(*args)
}