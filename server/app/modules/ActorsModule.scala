package modules

import actors._

import com.google.inject.AbstractModule

import play.api.libs.concurrent.AkkaGuiceSupport

class ActorsModule extends AbstractModule with AkkaGuiceSupport {
  override def configure = {
    bindActor[CancelAppointmentActor]("cancel-appointment-actor")
    bindActor[AppoinmentReminderActor]("appointment-reminder-actor")
  }
}
