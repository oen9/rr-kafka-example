package example.modules

import example.data.Data._
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.consumer._
import zio.kafka.consumer.Consumer
import zio.kafka.producer.Producer
import zio.kafka.serde._
import zio.logging._

object requesthandler {
  type RequestHandler = Has[RequestHandler.Service]

  case class ReqCorr(id: String, response: Promise[Nothing, String])

  object RequestHandler {
    trait Service {
      def handle(param: String): Task[Promise[Nothing, String]]
    }

    def startResponseConsumer(reqCorrs: Ref[Seq[ReqCorr]]) = {
      def findReqPred(corr: ReqCorr, cr: CommittableRecord[String, HelloData]) = corr.id == cr.record.value().key
      def handleResponse(reqCorrs: Ref[Seq[ReqCorr]], cr: CommittableRecord[String, HelloData]) =
        for {
          _     <- log.info(s"Received message '${cr.record}'")
          corrs <- reqCorrs.get
          _ <- corrs
            .find(findReqPred(_, cr))
            .fold(ZIO.unit)(_.response.succeed(cr.record.value().msg) *> ZIO.unit)
          _ <- reqCorrs.update(_.filter(findReqPred(_, cr)))
        } yield ()

      Consumer
        .subscribeAnd(Subscription.topics(RESPONSE_TOPIC))
        .plainStream(Serde.string, helloDataSerde)
        .tap(handleResponse(reqCorrs, _))
        .map(_.offset)
        .aggregateAsync(Consumer.offsetBatches)
        .mapM(_.commit)
        .runDrain
        .fork
    }

    def live =
      ZLayer.fromServicesM[
        Producer.Service[Any, String, HelloData],
        Blocking.Service,
        Consumer.Service,
        Logger[String],
        Clock with Blocking with Consumer with Logging,
        Throwable,
        RequestHandler.Service
      ] { (producer, blocking, consumer, logger) =>
        for {
          reqCorrs <- Ref.make(Seq[ReqCorr]())
          _        <- startResponseConsumer(reqCorrs)
        } yield new RequestHandlerLive(producer, blocking, consumer, reqCorrs, logger)
      }
  }

  def handle(param: String): ZIO[RequestHandler, Throwable, Promise[Nothing, String]] =
    ZIO.accessM[RequestHandler](_.get.handle(param))
}
