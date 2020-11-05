package example

import cats.implicits._
import Config.AppConfig
import example.data.Data.HelloData
import example.endpoints.RestEndpoints
import example.endpoints.StaticEndpoints
import example.modules.kafkaConsumer
import example.modules.kafkaProducer
import example.modules.requesthandler.RequestHandler
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.middleware.CORSConfig
import scala.concurrent.ExecutionContext
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.config._
import zio.console._
import zio.duration._
import zio.interop.catz._
import zio.kafka.consumer._
import zio.kafka.producer._

object Hello extends App {
  type AppEnv = ZEnv
    with ZConfig[AppConfig]
    with Producer[Any, String, HelloData]
    with Consumer
    with RequestHandler
  type AppTask[A] = ZIO[AppEnv, Throwable, A]

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app().provideCustomLayer {
      val appCfg     = Config.load
      val kafkaProd  = appCfg >>> kafkaProducer.live
      val kafkaCons  = (Blocking.any ++ Clock.any ++ appCfg) >>> kafkaConsumer.live
      val reqHandler = (Blocking.any ++ Clock.any ++ kafkaCons ++ kafkaProd) >>> RequestHandler.live

      appCfg ++
        kafkaProd ++
        kafkaCons ++
        reqHandler
    }.exitCode

  def app(): ZIO[AppEnv, Throwable, Unit] =
    for {
      _   <- putStrLn("Hello, world!")
      cfg <- getConfig[AppConfig]

      originConfig = CORSConfig(anyOrigin = true, allowCredentials = false, maxAge = 1.day.toSeconds)
      httpApp = (
        RestEndpoints.routes[AppEnv] <+> StaticEndpoints.routes[AppEnv]()
      ).orNotFound

      server <- ZIO.runtime[AppEnv].flatMap { implicit rts =>
        BlazeServerBuilder[AppTask](ExecutionContext.global)
          .bindHttp(cfg.http.port, cfg.http.host)
          .withHttpApp(CORS(httpApp, originConfig))
          .serve
          .compile
          .drain
      }
    } yield server
}
