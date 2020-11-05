package example.endpoints

import example.modules.requesthandler
import example.modules.requesthandler.RequestHandler
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import zio._
import zio.interop.catz._

object RestEndpoints {
  def routes[R <: RequestHandler]: HttpRoutes[RIO[R, *]] = {
    val dsl = Http4sDsl[RIO[R, *]]
    import dsl._

    HttpRoutes.of[RIO[R, *]] {
      case request @ GET -> Root / "rr" / name =>
        for {
          resp     <- requesthandler.handle(name)
          response <- Ok(resp.await)
        } yield response
    }
  }
}
