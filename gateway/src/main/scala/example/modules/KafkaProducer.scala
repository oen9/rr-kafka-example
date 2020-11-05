package example.modules

import example.Config.AppConfig
import example.data.Data
import example.data.Data.HelloData
import zio._
import zio.kafka.producer._
import zio.kafka.serde._

object kafkaProducer {
  val live = ZLayer.fromServiceManaged[AppConfig, Any, Throwable, Producer.Service[Any, String, HelloData]] { cfg =>
    val producerSettings = ProducerSettings(List(cfg.kafka.broker))
    Producer.make(producerSettings, Serde.string, Data.helloDataSerde)
  }
}
