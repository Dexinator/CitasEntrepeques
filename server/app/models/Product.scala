package models

import enumeratum.values._

import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}

import slick.lifted.MappedTo

case class ProductId(value: Long) extends MappedTo[Long]
object ProductId {
  implicit def queryStringBindable(implicit longBinder: QueryStringBindable[Long]) =
    new QueryStringBindable[ProductId] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ProductId]] = {
        for {
          id <- longBinder.bind(key, params)
        } yield id.map(ProductId(_))
      }
      override def unbind(key: String, id: ProductId): String =
        longBinder.unbind(key, id.value)
    }

  implicit def pathBinder(implicit longBinder: PathBindable[Long]) =
    new PathBindable[ProductId] {
      override def bind(key: String, value: String): Either[String, ProductId] = {
        for {
          id <- longBinder.bind(key, value)
        } yield ProductId(id)
      }
      override def unbind(key: String, id: ProductId): String =
        longBinder.unbind(key, id.value)
    }
}
trait ProductIdJsonOps {
  implicit val productIdFormat = Json.valueFormat[ProductId]
}

sealed abstract class Quality(val value: Short) extends ShortEnumEntry
object Quality extends ShortEnum[Quality] with ShortPlayJsonValueEnum[Quality] {
  case object Bad extends Quality(1)
  case object Good extends Quality(2)
  case object Excellent extends Quality(3)

  val values = findValues
}

case class Product(id: ProductId, quality: Quality, quantity: Short, categoryId: CategoryId,
  eventId: EventId, imageMime: Option[String] = None, imageData: Option[Array[Byte]] = None) {
  def toSeq(): Seq[String] = Seq(quantity.toString, quality.toString)
}
case class ProductQuery(eventId: EventId, password: String, master: Boolean = true)

trait ProductJsonOps extends ProductIdJsonOps
  with CategoryIdJsonOps with EventIdJsonOps {
  implicit val productFormat = Json.format[Product]
  implicit val productQueryFormat = Json.format[ProductQuery]
}
