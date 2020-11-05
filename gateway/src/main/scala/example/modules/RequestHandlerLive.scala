package example.modules

import example.data.Data._
import example.modules.requesthandler.ReqCorr
import example.modules.requesthandler.RequestHandler
import java.util.UUID.randomUUID
import zio._
import zio.blocking.Blocking
import zio.kafka.consumer.Consumer
import zio.kafka.producer.Producer
import zio.Promise

class RequestHandlerLive(
  producer: Producer.Service[Any, String, HelloData],
  blocking: Blocking.Service,
  consumer: Consumer.Service,
  reqCorrs: Ref[Seq[ReqCorr]]
) extends RequestHandler.Service {

  val consLayers     = ZLayer.succeed(consumer) ++ ZLayer.succeed(blocking)
  val prodLayers     = ZLayer.succeed(producer) ++ ZLayer.succeed(blocking)
  def generateUUID() = ZIO.effect(randomUUID().toString())

  override def handle(param: String): Task[Promise[Nothing, String]] =
    for {
      partitions <- (Consumer.assignment).provideLayer(consLayers)
      newUUID    <- generateUUID()
      response   <- Promise.make[Nothing, String]
      _          <- reqCorrs.update(_ :+ ReqCorr(newUUID, response))

      targetPartition = partitions.map(_.partition()).headOption.getOrElse(-1)
      request         = HelloData(targetPartition, newUUID, param)
      _ <- Producer
        .produceAsync[Any, String, HelloData](REQUEST_TOPIC, null, request)
        .provideLayer(prodLayers)
    } yield response
}
