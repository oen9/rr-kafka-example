package example.modules

import example.Config.AppConfig
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.kafka.consumer._

object kafkaConsumer {
  val live = ZLayer.fromServiceManaged[AppConfig, Clock with Blocking, Throwable, Consumer.Service] { cfg =>
    val consumerSettings = ConsumerSettings(List(cfg.kafka.broker))
      .withGroupId(cfg.kafka.groupId)
      .withCloseTimeout(5.seconds)
    Consumer.make(consumerSettings)
  }
}
