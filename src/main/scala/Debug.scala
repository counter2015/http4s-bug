import cats.effect.*
import com.comcast.ip4s.{Host, Port}
import io.circe.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.{Slf4jLogger, loggerFactoryforSync}


object Debug extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    val client = EmberClientBuilder.default[IO].build

    def io(body: Json, local: Boolean) =
      val endpoint =
        if (!local) uri"https://httpbin.org/post" // random error: Body Has Been Consumed Completely Already
        else uri"http://127.0.0.1:8081/post" // switch to local api, the error will not occur

      val request = Request[IO](Method.POST, endpoint).withEntity(body)
      for {
        result <- client.use { httpClient =>
          httpClient.run(request).use { response =>
            for {
              body <- response.bodyText.compile.string
              _ <- logger.info(s"Response body: $body")
              data <- response.asJsonDecode[Json]
              _ <- logger.info(data.toString)
            } yield data
          }
        }
      } yield result

    object LocalQuery extends QueryParamDecoderMatcher[Boolean]("local")

    val service = HttpRoutes.of[IO] {
      case req@POST -> Root / "post" =>
        req.as[Json].flatMap(Ok(_))
      case req@POST -> Root / "debug" :? LocalQuery(local) =>
        req.as[Json].flatMap(body => Ok(io(body, local))).handleErrorWith(
          e =>
            e.printStackTrace()
            InternalServerError(e.getMessage)
        )
    }


    val serverIO = for {
      host <- IO.fromOption(Host.fromString("0.0.0.0"))(
        throw new IllegalArgumentException(s"Invalid IPv4 address")
      )

      port <- IO.fromOption(Port.fromInt(8081))(
        throw new IllegalArgumentException(s"Invalid port")
      )

      server <- EmberServerBuilder
        .default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpApp(service.orNotFound)
        .build
        .use(_ => IO.never)
    } yield server

    serverIO.as(ExitCode.Success)
  }
}
