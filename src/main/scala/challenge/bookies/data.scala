package challenge.bookies

import io.estatico.newtype.Coercible
import io.estatico.newtype.macros._
import io.estatico.newtype.ops._
import scala.math.BigDecimal.RoundingMode

object data {

  implicit class bidDecimalCoercible(value: BigDecimal) {
    def as[A: Coercible[BigDecimal, *]]: A =
      value.setScale(1, RoundingMode.UP).coerce[A]
  }

  sealed trait BookInstruction
  case object New extends BookInstruction
  case object Update extends BookInstruction
  case object Delete extends BookInstruction

  sealed trait BookSide
  case object Bid extends BookSide
  case object Ask extends BookSide

  @newtype case class PriceLevelIndex(value: Int)
  @newtype case class TickPrice(value: Int)
  @newtype case class Quantity(value: Int)

  @newtype case class Depth(value: Int)
  @newtype case class TickSize(value: BigDecimal)

  case class BookOrder(
      instruction: BookInstruction,
      side: BookSide,
      index: PriceLevelIndex,
      price: TickPrice,
      quantity: Quantity
  )

  @newtype case class BidPrice(value: BigDecimal)
  @newtype case class BidQuantity(value: Int)
  @newtype case class AskPrice(value: BigDecimal)
  @newtype case class AskQuantity(value: Int)

  case class Bookie(
      bidPrice: BidPrice,
      bidQuantity: BidQuantity,
      askPrice: AskPrice,
      askQuantity: AskQuantity
  )

}
