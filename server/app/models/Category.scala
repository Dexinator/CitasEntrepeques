package models

import enumeratum.{PlayJsonEnum, Enum, EnumEntry}
import enumeratum.values._

import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}

import slick.lifted.MappedTo

case class CategoryId(value: Short) extends MappedTo[Short]
object CategoryId {
  implicit def queryStringBindable(implicit shortBinder: QueryStringBindable[Short]) =
    new QueryStringBindable[CategoryId] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, CategoryId]] = {
        for {
          id <- shortBinder.bind(key, params)
        } yield id.map(CategoryId(_))
      }
      override def unbind(key: String, id: CategoryId): String =
        shortBinder.unbind(key, id.value)
    }

  implicit def pathBinder(implicit intBinder: PathBindable[Int]) =
    new PathBindable[CategoryId] {
      override def bind(key: String, value: String): Either[String, CategoryId] = {
        for {
          id <- intBinder.bind(key, value)
        } yield CategoryId(id.toShort)
      }
      override def unbind(key: String, id: CategoryId): String =
        intBinder.unbind(key, id.value)
    }
}
trait CategoryIdJsonOps {
  implicit val categoryIdFormat = Json.valueFormat[CategoryId]
}

sealed abstract class Collection(val value: Short) extends ShortEnumEntry
object Collection extends ShortEnum[Collection] with ShortPlayJsonValueEnum[Collection] {
  case object ForAWalk extends Collection(1)
  case object ElectronicsAndAccesories extends Collection(2)
  case object Entertainment extends Collection(3)
  case object BathAndHome extends Collection(4)
  case object Clothing extends Collection(5)
  case object Other extends Collection(6)

  val values = findValues
}

case class Category(id: CategoryId, name: String, collection: Collection,
  icon: String, requiresImage: Boolean = false, active: Boolean = true) {
  def toSeq(): Seq[String] = Seq(name, collection.toString)
}

case class CategoryOption(id: CategoryId, requiresImage: Boolean,
  active: Boolean, password: String)

trait CategoryJsonOps extends CategoryIdJsonOps {
  implicit val categoryFormat = Json.format[Category]
  implicit val categoryOptionFormat = Json.format[CategoryOption]
}
