package challenge

import bookies._
import bookies.data._
import cats.effect._
import fs2._
import org.scalatest.compatible.Assertion
import org.scalatest.funsuite.AsyncFunSuite
import scala.concurrent.{ ExecutionContext, Future }

class BookiesSpec extends AsyncFunSuite {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  test("sample data 1") {
    val input = Stream(
      "N B 1 5 30",
      "N B 2 4 40",
      "N A 1 6 10",
      "N A 2 7 10",
      "U A 2 7 20",
      "U B 1 5 40"
    ).covary[IO]

    val output =
      List(
        "50.0,40,60.0,10",
        "40.0,40,70.0,20"
      )

    bookiesTest(input, Depth(2), TickSize(10.0), output)
  }

  test("sample data 2") {
    val input = Stream(
      "N B 1 5 30",
      "N B 2 4 40",
      "N A 1 6 10",
      "N A 1 7 20",
      "N A 1 8 25",
      "U B 1 5 40"
    ).covary[IO]

    val output =
      List(
        "50.0,40,80.0,25",
        "40.0,40,70.0,20",
        "0.0,0,60.0,10"
      )

    bookiesTest(input, Depth(3), TickSize(10.0), output)
  }

  test("sample data 3") {
    val input = Stream(
      "N B 1 5 30",
      "N B 2 4 40",
      "N A 1 6 10",
      "N A 1 7 20",
      "N A 1 8 25",
      "D A 1",
      "D A 1",
      "U B 1 5 40"
    ).covary[IO]

    val output =
      List(
        "50.0,40,60.0,10",
        "40.0,40,0.0,0",
        "0.0,0,0.0,0"
      )

    bookiesTest(input, Depth(3), TickSize(10.0), output)
  }

  def bookiesTest(
      src: Stream[IO, String],
      depth: Depth,
      tickSize: TickSize,
      expected: List[String]
  ): Future[Assertion] =
    Processor
      .bookies[IO](src, depth, tickSize)
      .map { acc =>
        assert(acc == expected)
      }
      .unsafeToFuture()

}
