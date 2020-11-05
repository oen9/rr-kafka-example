package example.data

import zio.kafka.serde._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

object Data {
  val REQUEST_TOPIC  = "request"
  val RESPONSE_TOPIC = "response"
  case class HelloData(partition: Int = -1, key: String = "", msg: String = "")

  def decodeHelloData(data: String): HelloData = decode[HelloData](data).getOrElse(HelloData())
  val helloDataSerde: Serde[Any, HelloData] = Serde.string.inmap(decodeHelloData)(_.asJson.noSpaces)
}
