package net.manub.embeddedkafka

import java.net.InetSocketAddress
import java.util.Properties

import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import kafka.serializer.StringEncoder
import kafka.server.{KafkaConfig, KafkaServer}
import org.apache.zookeeper.server.{ServerCnxnFactory, ZooKeeperServer}
import org.scalatest.Suite

import scala.reflect.io.Directory

trait EmbeddedKafka {

  this: Suite =>

  def withRunningKafka(body: => Unit)(implicit config: EmbeddedKafkaConfig) = {

    val factory = startZooKeeper(config.zooKeeperPort)
    val broker = startKafka(config)

    try {
      body
    } finally {
      broker.shutdown()
      factory.shutdown()
    }
  }

  def publishToKafka(topic: String, message: String)(implicit config: EmbeddedKafkaConfig) = {

    val producerProps = new Properties()
    producerProps.put("metadata.broker.list", s"127.0.0.1:${config.kafkaPort}")
    producerProps.put("serializer.class", classOf[StringEncoder].getName)

    val producer = new Producer[String, String](new ProducerConfig(producerProps))
    producer.send(new KeyedMessage[String, String](topic, message))
    println("***** FINISHED PUBLISHING")
    producer.close()
  }

  private def startZooKeeper(zooKeeperPort: Int): ServerCnxnFactory = {
    val zkLogsDir = Directory.makeTemp("zookeeper-logs")

    val tickTime = 2000

    val zkServer = new ZooKeeperServer(zkLogsDir.toFile.jfile, zkLogsDir.toFile.jfile, tickTime)

    val factory = ServerCnxnFactory.createFactory


    factory.configure(new InetSocketAddress("127.0.0.1", zooKeeperPort), 1024)
    factory.startup(zkServer)
    factory
  }

  private def startKafka(config: EmbeddedKafkaConfig): KafkaServer = {
    val kafkaLogDir = Directory.makeTemp("kafka")

    val zkAddress = s"127.0.0.1:${config.zooKeeperPort}"

    val properties: Properties = new Properties
    properties.setProperty("zookeeper.connect", zkAddress)
    properties.setProperty("broker.id", "0")
    properties.setProperty("host.name", "127.0.0.1")
    properties.setProperty("advertised.host.name", "127.0.0.1")
    properties.setProperty("auto.create.topics.enable", "true")
    properties.setProperty("port", Integer.toString(config.kafkaPort))
    properties.setProperty("log.dir", kafkaLogDir.toAbsolute.path)
    properties.setProperty("log.flush.interval.messages", String.valueOf(1))

    val broker = new KafkaServer(new KafkaConfig(properties))
    broker.startup()
    broker


    //    val kafka2LogDir = Directory.makeTemp("kafka2")
    //    properties.setProperty("broker.id", "1")
    //    properties.setProperty("port", "9092")
    //    properties.setProperty("log.dir", kafka2LogDir.toAbsolute.path)
    //
    //    val broker2 = new KafkaServer(new KafkaConfig(properties))
    //    broker2.startup()

    broker
  }
}
