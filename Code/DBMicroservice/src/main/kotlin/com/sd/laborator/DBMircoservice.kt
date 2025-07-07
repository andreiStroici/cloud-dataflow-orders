package com.sd.laborator

import com.sd.laborator.components.RabbitMqConnectionFactoryComponent
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageBuilder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@SpringBootApplication
open class DBMircoservice
{
    @Autowired
    private lateinit var connectionFactory: RabbitMqConnectionFactoryComponent
    private lateinit var amqpTemplate: AmqpTemplate

    @Autowired
    fun initTemplate()
    {
        this.amqpTemplate = connectionFactory.rabbitTemplate()
    }

    fun sendMessage(msg: String, correlationId:String)
    {
        val message = MessageBuilder.withBody(msg.toByteArray())
            .setHeader("correlationId", correlationId)
            .build()
        this.amqpTemplate.convertAndSend(connectionFactory.getExchange(),
            connectionFactory.getRoutingKey(),
            message)
    }

    fun insertIntoCommand(id:Long, productName:String, cuantity:Int)
    {
        try
        {
            println("!!!!!!!Pregatesc inserarea")
            val url="jdbc:sqlite:/home/student/Anu_lII/L9/Problema2/DBMicroservice/data.db?busy_timeout=5000"
            Class.forName("org.sqlite.JDBC")
            println("Realizarre conexiune")
            val conn: Connection = DriverManager.getConnection(url)
            println("aici")
            var sql = "SELECT product_id FROM product WHERE product_name = ?"
            var pstmt: PreparedStatement = conn.prepareStatement(sql)

            pstmt.setString(1, productName)
            val resultSet: ResultSet = pstmt.executeQuery()
            while(resultSet.next())
            {
                val productID = resultSet.getInt("product_id")
                sql = "INSERT INTO command (id, product_id, cuantity) values(?, ?, ?)"
                pstmt = conn.prepareStatement(sql)
                pstmt.setLong(1, id)
                pstmt.setInt(2, productID)
                pstmt.setInt(3, cuantity)
                pstmt.executeUpdate()
            }
            conn.close()
        }
        catch (e:ClassNotFoundException)
        {
            println("Eroare ClassNotFoundException")
            throw RuntimeException(e)
        }
        catch (e: Exception)
        {
            println("Eroare necunoscuta")
            e.printStackTrace()
        }
    }

    fun insertIntoUserData(id:Long, name:String, address:String)
    {
        try
        {
            println("!!!!!!!Pregatesc inserarea")
            val url="jdbc:sqlite:/home/student/Anu_lII/L9/Problema2/DBMicroservice/data.db?busy_timeout=5000"
            Class.forName("org.sqlite.JDBC")
            println("Realizare conexiune")
            val conn: Connection = DriverManager.getConnection(url)
            println("aici")
            val sql = "INSERT INTO user_data (id_comanda, client_name, address) values(?, ?, ?)"
            var pstmt: PreparedStatement = conn.prepareStatement(sql)
            pstmt = conn.prepareStatement(sql)
            pstmt.setLong(1, id)
            pstmt.setString(2, name)
            pstmt.setString(3, address)
            pstmt.executeUpdate()
            conn.close()
        }
        catch (e:ClassNotFoundException)
        {
            println("Eroare ClassNotFoundException")
            throw RuntimeException(e)
        }
        catch (e: Exception)
        {
            println("Eroare necunoscuta")
            e.printStackTrace()
        }
    }

    fun insertIntoFacturare(id:Long)
    {
        try
        {
            println("!!!!!!!Pregatesc inserarea")
            val url="jdbc:sqlite:/home/student/Anu_lII/L9/Problema2/DBMicroservice/data.db?busy_timeout=5000"
            Class.forName("org.sqlite.JDBC")
            println("Realizarre conexiune")
            val conn: Connection = DriverManager.getConnection(url)
            println("aici")
            val sql = "INSERT INTO facturare (id_comanda) values(?)"
            var pstmt: PreparedStatement = conn.prepareStatement(sql)
            pstmt = conn.prepareStatement(sql)
            pstmt.setLong(1, id)
            pstmt.executeUpdate()
            conn.close()
        }
        catch (e:ClassNotFoundException)
        {
            println("Eroare ClassNotFoundException")
            throw RuntimeException(e)
        }
        catch (e: Exception)
        {
            println("Eroare necunoscuta")
            e.printStackTrace()
        }
    }

    fun getFromCommand(id:Long, correlationID:String)
    {
        try
        {
            val url="jdbc:sqlite:/home/student/Anu_lII/L9/Problema2/DBMicroservice/data.db?busy_timeout=5000"
            Class.forName("org.sqlite.JDBC")
            val conn: Connection = DriverManager.getConnection(url)
            println("aici")
            var sql = "SELECT product_id, cuantity FROM command where id=?"
            var pstmt: PreparedStatement = conn.prepareStatement(sql)
            pstmt.setLong(1, id)
            var resultSet: ResultSet = pstmt.executeQuery()
            if (resultSet.next())
            {
                val productId = resultSet.getInt("product_id")
                val cuantity = resultSet.getInt("cuantity")
                sql = "SELECT product_name FROM product WHERE product_id = ?"
                pstmt = conn.prepareStatement(sql)
                pstmt.setInt(1, productId)
                resultSet = pstmt.executeQuery()
                var name = ""
                while (resultSet.next())
                {
                    name = resultSet.getString("product_name")
                }

                println("Product ID: $productId, Cuantity: $cuantity: $name")
                sendMessage("$name|$cuantity", correlationID)
            }
            conn.close()
        }
        catch (e:ClassNotFoundException)
        {
            println("Eroare ClassNotFoundException")
            throw RuntimeException(e)
        }
        catch (e: Exception)
        {
            println("Eroare necunoscuta")
            e.printStackTrace()
        }
    }


    fun getFromUserData(id:Long, correlationID:String)
    {
        try
        {
            println("!!!!!!!Pregatesc inserarea")
            val url="jdbc:sqlite:/home/student/Anu_lII/L9/Problema2/DBMicroservice/data.db?busy_timeout=5000"
            Class.forName("org.sqlite.JDBC")
            println("Realizarre conexiune")
            val conn: Connection = DriverManager.getConnection(url)
            println("aici")
            val sql = "SELECT client_name, address FROM user_data WHERE id_comanda = ?"
            var pstmt: PreparedStatement = conn.prepareStatement(sql)
            pstmt = conn.prepareStatement(sql)
            pstmt.setLong(1, id)
            val resultSet: ResultSet = pstmt.executeQuery()
            while (resultSet.next())
            {
                val client_name = resultSet.getString("client_name")
                val address = resultSet.getString("address")
                sendMessage("$client_name|$address", correlationID)
            }
            conn.close()
        }
        catch (e:ClassNotFoundException)
        {
            println("Eroare ClassNotFoundException")
            throw RuntimeException(e)
        }
        catch (e: Exception)
        {
            println("Eroare necunoscuta")
            e.printStackTrace()
        }
    }

    fun checkDepozit(id:Int, cuantity: Int, correlationID:String)
    {
        try
        {
            val url="jdbc:sqlite:/home/student/Anu_lII/L9/Problema2/DBMicroservice/data.db?busy_timeout=5000"
            Class.forName("org.sqlite.JDBC")
            val conn: Connection = DriverManager.getConnection(url)
            println("aici")
            val sql = "SELECT cunatity FROM depozit WHERE product_id = ?"
            var pstmt: PreparedStatement = conn.prepareStatement(sql)
            pstmt = conn.prepareStatement(sql)
            pstmt.setInt(1, id)
            val resultSet: ResultSet = pstmt.executeQuery()
            while (resultSet.next())
            {
                val c = resultSet.getInt("cunatity")
                if(c < cuantity)
                {
                    sendMessage("true", correlationID)
                }
                else
                {
                    sendMessage("false", correlationID)
                }
            }
            conn.close()
        }
        catch (e:ClassNotFoundException)
        {
            println("Eroare ClassNotFoundException")
            throw RuntimeException(e)
        }
        catch (e: Exception)
        {
            println("Eroare necunoscuta")
            e.printStackTrace()
        }
    }

    fun updateDepozitStock(id: String, cuantity: Int, nr_commands:Int)
    {
        try
        {
            val url="jdbc:sqlite:/home/student/Anu_lII/L9/Problema2/DBMicroservice/data.db?busy_timeout=5000"
            Class.forName("org.sqlite.JDBC")
            val conn: Connection = DriverManager.getConnection(url)
            var sql = "SELECT product_id FROM product WHERE product_name = ?"
            var pstmt: PreparedStatement = conn.prepareStatement(sql)
            pstmt.setString(1, id)
            var resultSet = pstmt.executeQuery()
            var product_id = 0
            while (resultSet.next())
            {
                product_id = resultSet.getInt("product_id")
            }
            sql  = "UPDATE depozit SET cuantity = ?, nr_commands = ?WHERE product_id = ?"
            pstmt = conn.prepareStatement(sql)
            pstmt.setInt(1, cuantity)
            pstmt.setInt(2, nr_commands)
            pstmt.setInt(3, product_id)
            pstmt.executeUpdate()

            conn.close()
        }
        catch (e:ClassNotFoundException)
        {
            println("Eroare ClassNotFoundException")
            throw RuntimeException(e)
        }
        catch (e: Exception)
        {
            println("Eroare necunoscuta")
            e.printStackTrace()
        }
    }

    fun updateCommandStatus(id: Long, status: String)
    {
        try
        {
            val url="jdbc:sqlite:/home/student/Anu_lII/L9/Problema2/DBMicroservice/data.db?busy_timeout=5000"
            Class.forName("org.sqlite.JDBC")
            val conn: Connection = DriverManager.getConnection(url)
            var sql = "UPDATE command SET respins = ? WHERE id = ?"
            var pstmt: PreparedStatement = conn.prepareStatement(sql)
            pstmt.setString(1, status)
            pstmt.setLong(2, id)
            pstmt.executeUpdate()
            conn.close()
        }
        catch (e:ClassNotFoundException)
        {
            println("Eroare ClassNotFoundException")
            throw RuntimeException(e)
        }
        catch (e: Exception)
        {
            println("Eroare necunoscuta")
            e.printStackTrace()
        }
    }

    fun depsozitStock(): MutableMap<String, Pair<Int, Int>>
    {
        val h = mutableMapOf<String, Pair<Int, Int>>()
        try
        {
            val url="jdbc:sqlite:/home/student/Anu_lII/L9/Problema2/DBMicroservice/data.db?busy_timeout=5000"
            Class.forName("org.sqlite.JDBC")
            val conn: Connection = DriverManager.getConnection(url)
            var sql = "SELECT * FROM depozit"
            var pstmt: PreparedStatement = conn.prepareStatement(sql)
            val resultSet: ResultSet = pstmt.executeQuery()
            while (resultSet.next())
            {
                val c = resultSet.getInt("cuantity")
                val cmd = resultSet.getInt("nr_commands")
                val id = resultSet.getInt("product_id")
                println("$c|$cmd|$id")
                sql = "SELECT product_name FROM product WHERE product_id = ?"
                pstmt = conn.prepareStatement(sql)
                pstmt.setInt(1, id)
                val resultSet1: ResultSet = pstmt.executeQuery()
                var n = ""
                while(resultSet1.next())
                {
                    n = resultSet1.getString("product_name")
                }
                h[n] = Pair(c, cmd)
            }
            conn.close()
            return h
        }
        catch (e:ClassNotFoundException)
        {
            println("Eroare ClassNotFoundException")
            throw RuntimeException(e)
        }
        catch (e: Exception)
        {
            println("Eroare necunoscuta")
            e.printStackTrace()
        }
        return mutableMapOf()
    }

    fun checkStatus(id:Long):String
    {
        var s = "NULL"
        try
        {
            val url="jdbc:sqlite:/home/student/Anu_lII/L9/Problema2/DBMicroservice/data.db?busy_timeout=5000"
            Class.forName("org.sqlite.JDBC")
            val conn: Connection = DriverManager.getConnection(url)
            var sql = "SELECT respins FROM command WHERE id = ?"
            var pstmt: PreparedStatement = conn.prepareStatement(sql)
            pstmt.setLong(1, id)
            val resultSet: ResultSet = pstmt.executeQuery()
            while (resultSet.next())
            {
                val r = resultSet.getString("respins")
                if(r != null)
                {
                    s = r
                }
            }
            conn.close()
        }
        catch (e:ClassNotFoundException)
        {
            println("Eroare ClassNotFoundException")
            throw RuntimeException(e)
        }
        catch (e: Exception)
        {
            println("Eroare necunoscuta")
            e.printStackTrace()
        }
        return s
    }


    @RabbitListener(queues = ["\${db.rabbitmq.queue}"])
    fun receiveMessage(mess: Message)
    {
        var correlationID = ""
        val msg = String(mess.body)
        if(!(msg.contains("insertCommand") or msg.contains("insertUserData")))
        {
            correlationID = mess.messageProperties.headers["correlationId"] as String
        }
        val message = msg.split('~')
        println("Mesajul primit este: $msg")
        val lock = ReentrantLock()
        lock.withLock {
            when (message[0]) {
                "insertCommand" -> {
                    val c = message[1].split('|')
                    insertIntoCommand(c[0].toLong(), c[1], c[2].toInt())
                }

                "insertUserData" -> {
                    val c = message[1].split('|')
                    insertIntoUserData(c[0].toLong(), c[1], c[2])
                }

                "depozitStock" -> {
                    val h = depsozitStock()
                    println(Json.encodeToString(h))
                    if (h.isEmpty()) {
                        sendMessage("DEPOZIT GOL!", correlationID)
                    } else {
                        sendMessage(Json.encodeToString(h), correlationID)
                    }
                }

                "insertFactruare" -> {
                    insertIntoFacturare(message[1].toLong())
                }

                "getCommand" -> {
                    getFromCommand(message[1].toLong(), correlationID)
                }

                "updateDepozit" -> {
                    val c = message[1].split("|")
                    updateDepozitStock(c[0], c[1].toInt(), c[2].toInt())
                }

                "respingereComanda" -> {
                    val c = message[1].split("|")
                    this.updateCommandStatus(c[0].toLong(), c[1])
                }

                "checkStatus" -> {
                    val s = checkStatus(message[1].toLong())
                    sendMessage(s, correlationID)
                }
            }
        }
    }
}

fun main(args: Array<String>)
{
    runApplication<DBMircoservice>()
}
