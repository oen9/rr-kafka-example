package example

import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config.typesafe.TypesafeConfig

object Config {
  case class HttpConfig(host: String, port: Int)
  case class KafkaConfig(broker: String, groupId: String)
  case class AppConfig(http: HttpConfig, kafka: KafkaConfig)

  val appConfig = descriptor[AppConfig]

  def load: zio.Layer[ReadError[String], ZConfig[AppConfig]] = TypesafeConfig.fromDefaultLoader(appConfig)
}
