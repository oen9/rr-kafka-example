package example

import example.data.Data._
import example.modules.kafkaConsumer
import example.modules.kafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.consumer._
import zio.kafka.producer._
import zio.kafka.serde._
import zio.logging._

object Hello extends App {
  type AppEnv = ZEnv with Producer[Any, String, HelloData] with Consumer with Logging

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app().provideCustomLayer {
      val appCfg  = Config.load
      val logging = slf4j.Slf4jLogger.make((_, msg) => msg)
      val produ   = appCfg >>> kafkaProducer.live
      val consu   = (Blocking.any ++ Clock.any ++ appCfg) >>> kafkaConsumer.live

      produ ++
        consu ++
        logging
    }.exitCode

  def app(): ZIO[AppEnv, Throwable, Unit] =
    for {
      _ <- log.info("micro-handler started")

      _ <- Consumer
        .subscribeAnd(Subscription.topics(REQUEST_TOPIC))
        .plainStream(Serde.string, helloDataSerde)
        .tap { cr =>
          val value    = cr.record.value
          val key      = cr.record.key
          val newMsg   = s"msg: '${value.msg}' processed"
          val response = value.copy(msg = newMsg)
          val record   = new ProducerRecord(RESPONSE_TOPIC, value.partition, key, response)
          for {
            _ <- log.info(s"Received message '${cr.record}'")
            _ <- log.info(s"Sending message '$record'")
            _ <- Producer.produceAsync[Any, String, HelloData](record)
          } yield ()
        }
        .map(_.offset)
        .aggregateAsync(Consumer.offsetBatches)
        .mapM(_.commit)
        .runDrain
    } yield ()
}
