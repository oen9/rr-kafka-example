package example.endpoints

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import zio._
import zio.interop.catz._

object StaticEndpoints {

  def routes[R](): HttpRoutes[RIO[R, *]] = {
    val dsl = Http4sDsl[RIO[R, *]]
    import dsl._

    HttpRoutes.of[RIO[R, *]] {
      case request @ GET -> Root =>
        Ok("Hello, world!")
    }
  }
}
