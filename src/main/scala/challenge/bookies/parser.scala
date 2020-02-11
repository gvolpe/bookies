package challenge.bookies

import cats.implicits._
import data._
import fs2._

object Parser {

  /*
   * It tries to parse a line of text into a BookOrder.
   *
   * If there is a formatting error, that line will be skipped.
   *   - This is one assumption I've made, but in real life it should be a business decision.
   *   - We could fail the entire book order in case of formatting error, for example.
   */
  def orders[F[_]]: Pipe[F, String, Option[BookOrder]] = { stream =>
    def catNumbers(inst: BookInstruction, side: BookSide, xs: List[String]): Option[BookOrder] =
      xs match {
        case (i :: p :: q :: Nil) =>
          (i.toIntOption, p.toIntOption, q.toIntOption).mapN {
            case (ii, pp, qq) =>
              BookOrder(inst, side, PriceLevelIndex(ii), TickPrice(pp), Quantity(qq))
          }
        case (i :: Nil) =>
          i.toIntOption.map { ii =>
            BookOrder(inst, side, PriceLevelIndex(ii), TickPrice(0), Quantity(0))
          }
        case _ => none[BookOrder]
      }

    stream.map(_.split(' ').toList match {
      case ("N" :: "B" :: xs) => catNumbers(New, Bid, xs)
      case ("N" :: "A" :: xs) => catNumbers(New, Ask, xs)
      case ("U" :: "B" :: xs) => catNumbers(Update, Bid, xs)
      case ("U" :: "A" :: xs) => catNumbers(Update, Ask, xs)
      case ("D" :: "B" :: xs) => catNumbers(Delete, Bid, xs)
      case ("D" :: "A" :: xs) => catNumbers(Delete, Ask, xs)
      case _                  => none[BookOrder]
    })
  }

}
