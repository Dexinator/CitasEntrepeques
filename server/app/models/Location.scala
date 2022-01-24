package models

import enumeratum.{PlayJsonEnum, Enum, EnumEntry}
import enumeratum.values._

import java.time.{Duration, LocalTime}

import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}

import slick.lifted.MappedTo

case class LocationId(value: Short) extends MappedTo[Short]
object LocationId {
  implicit def queryStringBindable(implicit shortBinder: QueryStringBindable[Short]) =
    new QueryStringBindable[LocationId] {
      override def bind(key: String,
        params: Map[String, Seq[String]]): Option[Either[String, LocationId]] = {
        for {
          id <- shortBinder.bind(key, params)
        } yield id.map(LocationId(_))
      }
      override def unbind(key: String, id: LocationId): String =
        shortBinder.unbind(key, id.value)
    }

  implicit def pathBinder(implicit intBinder: PathBindable[Int]) =
    new PathBindable[LocationId] {
      override def bind(key: String, value: String): Either[String, LocationId] = {
        for {
          id <- intBinder.bind(key, value)
        } yield LocationId(id.toShort)
      }
      override def unbind(key: String, id: LocationId): String =
        intBinder.unbind(key, id.value)
    }
}
trait LocationIdJsonOps {
  implicit val locationIdFormat = Json.valueFormat[LocationId]
}

case class Location(id: LocationId, name: String, address: String, phone: String) {
  def toSeq(): Seq[String] = Seq(name, address)
}
trait LocationJsonOps extends LocationIdJsonOps {
  implicit val locationFormat = Json.format[Location]
}

case class BusinessHourId(value: Short) extends MappedTo[Short]
trait BusinessHourIdJsonOps {
  implicit val businessHourIdFormat = Json.valueFormat[BusinessHourId]
}

sealed abstract class Day(val value: Short) extends ShortEnumEntry
object Day extends ShortEnum[Day] with ShortPlayJsonValueEnum[Day] {
  case object Sunday extends Day(0)
  case object Monday extends Day(1)
  case object Tuesday extends Day(2)
  case object Wednesday extends Day(3)
  case object Thursday extends Day(4)
  case object Friday extends Day(5)
  case object Saturday extends Day(6)

  val values = findValues
}

case class BusinessHour(id: BusinessHourId, start: LocalTime,
  duration: Duration, day: Day, locationId: LocationId)
trait BusinessHourJsonOps extends BusinessHourIdJsonOps with LocationIdJsonOps {
  implicit val businessHourFormat = Json.format[BusinessHour]
}

case class LocationWithBusinessHours(id: LocationId, name: String,
  address: String, businessHours: Seq[BusinessHour])
trait LocationWithBusinessHoursJsonOps extends BusinessHourJsonOps {
  implicit val locationWithBusinessHoursFormat = Json.format[LocationWithBusinessHours]
}
