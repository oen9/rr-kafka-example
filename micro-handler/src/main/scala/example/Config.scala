package example

import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config.typesafe.TypesafeConfig

object Config {
  case class KafkaConfig(broker: String, groupId: String)
  case class AppConfig(kafka: KafkaConfig)

  val appConfig = descriptor[AppConfig]

  def load: zio.Layer[ReadError[String], ZConfig[AppConfig]] = TypesafeConfig.fromDefaultLoader(appConfig)
}
