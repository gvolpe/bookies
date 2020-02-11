package challenge.bookies

import cats.Functor
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import data._
import fs2.Stream

object Processor {

  type BidState = Map[PriceLevelIndex, (BidPrice, BidQuantity)]
  type AskState = Map[PriceLevelIndex, (AskPrice, AskQuantity)]

  /*
   * It processes the latest book orders given
   *
   * - a @src of data
   * - a @depth indicating the number of price levels
   * - a @tickSize representing the price movements
   *
   * And it returns each line of the result as a List[String]
   */
  def bookies[F[_]: Concurrent](
      src: Stream[F, String],
      depth: Depth,
      tickSize: TickSize
  ): F[List[String]] =
    Stream
      .eval(mkState(depth))
      .flatMap { ref =>
        src
          .through(Parser.orders)
          .unNone
          .evalMap(o => processOrder(ref, o, tickSize))
          .drain
          .append(
            Stream.eval(
              getRecentOrders(ref, depth)
            )
          )
      }
      .compile
      .toList
      .map(_.flatten)

  /*
   * It creates the initial state for Bids and Asks, given a @depth
   */
  private def mkState[F[_]: Sync](depth: Depth): F[Ref[F, (BidState, AskState)]] = {
    val bids = (1 to depth.value).toList.map { index =>
      PriceLevelIndex(index) -> (BigDecimal(0).as[BidPrice] -> BidQuantity(0))
    }.toMap

    val asks = (1 to depth.value).toList.map { index =>
      PriceLevelIndex(index) -> (BigDecimal(0.0).as[AskPrice] -> AskQuantity(0))
    }.toMap

    Ref.of[F, (BidState, AskState)](bids -> asks)
  }

  /*
   * It updates the price level indexes of the state in case of @New or @Delete instruction
   */
  private def updateLevels[A, B](
      order: BookOrder,
      state: Map[PriceLevelIndex, (A, B)],
      inst: BookInstruction
  ): Map[PriceLevelIndex, (A, B)] =
    state.map {
      case record @ (i, (bp, bq)) if (i.value >= order.index.value) =>
        inst match {
          case New =>
            PriceLevelIndex(i.value + 1) -> (bp -> bq)
          case Delete =>
            PriceLevelIndex(i.value - 1) -> (bp -> bq)
          case Update => record
        }
      case same => same
    }

  /*
   * It updates the state of the orders given a new @order
   */
  private def processOrder[F[_]: Functor](
      state: Ref[F, (BidState, AskState)],
      order: BookOrder,
      tick: TickSize
  ): F[Unit] =
    state.update {
      case (bids, asks) =>
        (order.instruction, order.side) match {
          case (New, Bid) =>
            val incr     = updateLevels(order, bids, New)
            val price    = (tick.value * order.price.value).as[BidPrice]
            val quantity = BidQuantity(order.quantity.value)
            incr.updated(order.index, price -> quantity) -> asks
          case (New, Ask) =>
            val incr     = updateLevels(order, asks, New)
            val price    = (tick.value * order.price.value).as[AskPrice]
            val quantity = AskQuantity(order.quantity.value)
            bids -> incr.updated(order.index, price -> quantity)
          case (Update, Bid) =>
            val price    = (tick.value * order.price.value).as[BidPrice]
            val quantity = BidQuantity(order.quantity.value)
            bids.updated(order.index, price -> quantity) -> asks
          case (Update, Ask) =>
            val price    = (tick.value * order.price.value).as[AskPrice]
            val quantity = AskQuantity(order.quantity.value)
            bids -> asks.updated(order.index, price -> quantity)
          case (Delete, Bid) =>
            val decr = updateLevels(order, bids, Delete)
            decr -> asks
          case (Delete, Ask) =>
            val decr = updateLevels(order, asks, Delete)
            bids -> decr
          case _ =>
            bids -> asks
        }
    }.void

  /*
   * It groups the current state of bids and asks in a single formatted list,
   * ready to be displayed.
   */
  private def getRecentOrders[F[_]: Functor](
      state: Ref[F, (BidState, AskState)],
      depth: Depth
  ): F[List[String]] =
    state.get.map {
      case (bids, asks) =>
        bids
          .flatMap {
            case (i, (bp, bq)) =>
              asks.get(i).map {
                case (ap, aq) =>
                  i -> Bookie(bp, bq, ap, aq)
              }
          }
          .toList
          .sortBy(_._1.value) // sorted by index
          .take(depth.value)
          .map {
            case (_, b) =>
              s"${b.bidPrice.value},${b.bidQuantity.value},${b.askPrice.value},${b.askQuantity.value}"
          }
    }

}
