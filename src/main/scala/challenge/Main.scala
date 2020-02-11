package challenge

import bookies._
import bookies.data._
import cats.effect._
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._
import fs2._
import java.nio.file.{ Path, Paths }

object Main
    extends CommandIOApp(
      name = "bookies",
      header = "Order book on a financial exchange",
      version = "0.0.1"
    ) {

  def putStrLn(str: String): IO[Unit] = IO(println(str))

  val filePathOpts: Opts[Path] =
    Opts.argument[String](metavar = "filepath").map(Paths.get(_))

  val tickSizeOpts: Opts[TickSize] =
    Opts.argument[BigDecimal](metavar = "tickSize").map(_.as[TickSize])

  val depthOpts: Opts[Depth] =
    Opts.argument[Int](metavar = "depth").map(Depth.apply)

  def main: Opts[IO[ExitCode]] =
    (filePathOpts, tickSizeOpts, depthOpts).mapN {
      case (path, tickSize, depth) =>
        Stream
          .resource(Blocker[IO])
          .evalMap { blocker =>
            val src =
              io.file
                .readAll[IO](path, blocker, 4096)
                .through(text.utf8Decode)
                .through(text.lines)

            Processor
              .bookies[IO](src, depth, tickSize)
              .flatMap(_.traverse_(putStrLn))
          }
          .compile
          .drain
          .as(ExitCode.Success)
    }

}
