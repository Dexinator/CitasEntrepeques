package actors

import akka.actor._

import database.ProductRepository

import javax.inject._

import models.EventId

import play.api.Logging

object CancelAppointmentActor {
  case class CancelAppointment(id: EventId)
}

class CancelAppointmentActor @Inject()(val repository: ProductRepository)
extends Actor with Logging {
  import CancelAppointmentActor._

  def receive = {
    case CancelAppointment(id: EventId) =>
      sender() ! repository.products.cancelEvent(id)
  }
}
