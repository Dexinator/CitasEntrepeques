package models

import java.time.{Duration, ZonedDateTime}

import play.api.libs.json._

case class Appointment(
  start: ZonedDateTime,
  duration: Duration,
  locationId: LocationId,
  locationName: String,
  locationAddress: String,
  contactEmail: String,
  contactName: String,
  contactPhone: String,
  products: List[NewProduct],
) {
  def toEvent = Event(EventId(), start, duration, locationId,
    contactEmail, contactName, contactPhone, false)
}
case class NewProduct(quality: Quality, quantity: Short, categoryId: CategoryId,
  categoryName: String, collection: Collection, image: Option[String])

trait AppointmentJsonOps extends LocationIdJsonOps with CategoryIdJsonOps {
  implicit val newProductFormat = Json.format[NewProduct]
  implicit val appointmentFormat = Json.format[Appointment]
}
