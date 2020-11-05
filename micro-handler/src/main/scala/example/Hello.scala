package example

import example.data.Data._
import example.modules.kafkaConsumer
import example.modules.kafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console
import zio.kafka.consumer._
import zio.kafka.producer._
import zio.kafka.serde._

object Hello extends App {
  type AppEnv = ZEnv with Producer[Any, String, HelloData] with Consumer

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app().provideCustomLayer {
      val appCfg = Config.load
      val produ  = appCfg >>> kafkaProducer.live
      val consu  = (Blocking.any ++ Clock.any ++ appCfg) >>> kafkaConsumer.live

      produ ++
        consu
    }.exitCode

  def app(): ZIO[AppEnv, Throwable, Unit] =
    for {
      _ <- console.putStr("Hello from micro-handler!")

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
            _ <- console.putStrLn(s"Received message $key: $value")
            _ <- Producer.produceAsync[Any, String, HelloData](record)
          } yield ()
        }
        .map(_.offset)
        .aggregateAsync(Consumer.offsetBatches)
        .mapM(_.commit)
        .runDrain
    } yield ()
}
