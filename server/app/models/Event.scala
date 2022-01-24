package models

import enumeratum.values._

import java.time.format.DateTimeFormatter
import java.time.{Duration, ZonedDateTime}
import java.util.UUID

import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}

import slick.lifted.MappedTo

case class EventId(value: UUID = UUID.randomUUID) extends MappedTo[UUID]
object EventId {
  implicit def queryStringBindable(implicit uuidBinder: QueryStringBindable[UUID]) =
    new QueryStringBindable[EventId] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, EventId]] = {
        for {
          id <- uuidBinder.bind(key, params)
        } yield id.map(EventId(_))
      }
      override def unbind(key: String, id: EventId): String =
        uuidBinder.unbind(key, id.value)
    }

  implicit def pathBinder(implicit uuidBinder: PathBindable[UUID]) =
    new PathBindable[EventId] {
      override def bind(key: String, value: String): Either[String, EventId] = {
        for {
          id <- uuidBinder.bind(key, value)
        } yield EventId(id)
      }
      override def unbind(key: String, id: EventId): String =
        uuidBinder.unbind(key, id.value)
    }
}
trait EventIdJsonOps {
  implicit val eventIdFormat = Json.valueFormat[EventId]
}

case class Event(
  id: EventId = EventId(),
  start: ZonedDateTime,
  duration: Duration,
  locationId: LocationId,
  contactEmail: String,
  contactName: String,
  contactPhone: String,
  confirmed: Boolean,
) {
  def toSeq()(implicit dateFormatter: DateTimeFormatter): Seq[String] = Seq(
    start.format(dateFormatter),
    start.plus(duration).format(dateFormatter),
    contactEmail,
    contactName,
    contactPhone,
    confirmed.toString
  )
}
case class EventWithLocation(id: EventId, start: ZonedDateTime, duration: Duration,
  locationAddress: String, locationName: String,  contactEmail: String,
  contactName: String, contactPhone: String, confirmed: Boolean)
case class EventShort(start: ZonedDateTime, duration: Duration,
  locationId: LocationId, confirmed: Boolean)
case class EventShortWithLocation(start: ZonedDateTime, duration: Duration,
  locationAddress: String, locationName: String, confirmed: Boolean)

trait EventJsonOps extends EventIdJsonOps with LocationIdJsonOps {
  implicit val eventFormat = Json.format[Event]
  implicit val eventWithLocationFormat = Json.format[EventWithLocation]
  implicit val eventShortFormat = Json.format[EventShort]
  implicit val eventShortWithLocationFormat = Json.format[EventShortWithLocation]
}
