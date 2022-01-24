package controllers

import actors.AppoinmentReminderActor.SendReminder
import actors.CancelAppointmentActor.CancelAppointment

import akka.actor._

import database.ProductRepository

import java.time.{Duration => EventDuration, ZonedDateTime, ZoneId}
import java.time.format._
import java.util.{Base64, Locale}

import javax.inject._

import models._

import org.apache.commons.mail.EmailAttachment

import play.api._
import play.api.libs.concurrent.Futures._
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.mvc._

import scala.collection.concurrent.TrieMap
import scala.concurrent._
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class MailController @Inject()(
  @Named("cancel-appointment-actor") cancelActor: ActorRef,
  @Named("appointment-reminder-actor") reminderActor: ActorRef,
  val actorSystem: ActorSystem,
  val mailerClient: MailerClient,
  val repository: ProductRepository,
  val scc: SecuredControllerComponents,
)(implicit ec: ExecutionContext, conf: Configuration)
  extends SecuredAbstractController(scc)
  with AppointmentJsonOps
  with Logging {

  private val cancellables = TrieMap.empty[EventId, Cancellable]

  val appEmails = conf.underlying.getStringList("appEmails").asScala.toSeq
  val appUrl = conf.underlying.getString("appUrl")

  val dateFormatter = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.LONG)
    .withLocale(new Locale("es", "mx"))
  val timeFormatter = DateTimeFormatter
    .ofLocalizedTime(FormatStyle.SHORT)
    .withLocale(new Locale("es", "mx"))

  def saveAppointment() = Action(parse.json).async { implicit request =>
    request.body.validate[Appointment].fold(
      errors => {
        logger.warn("Validate: " + JsError.toJson(errors).toString)
        Future(BadRequest(Json.obj("message" -> JsError.toJson(errors))))
      },
      appointment => {
        def resolve(): Future[play.api.mvc.Result] = {
          futures.timeout(60 seconds)(repository.products.addAppointment(appointment))
            .map {
              eventId => {
                val mimePattern = "^data:([a-zA-Z0-9]+/[a-zA-Z0-9]+).*,.*".r
                val images = appointment.products.zipWithIndex.filter(_._1.image.isDefined).map({
                  case (product, index) => {
                    AttachmentData(
                      name = s"image$index",
                      data = Base64.getDecoder().decode(
                        product.image.get.substring(product.image.get.indexOf(",") + 1)
                      ),
                      mimetype = product.image.get match {
                        case mimePattern(mime) => mime
                        case _ => "image/png"
                      },
                      disposition = Some(EmailAttachment.INLINE),
                      contentId = Some(index.toString),
                    )
                  }
                })

                /*
                val email = Email(
                  "Se Requiere Confirmar Cita - Entrepeques.mx",
                  "CitasEntrepeques <citas@entrepeques.mx>",
                  Seq(s"${appointment.contactName} <${appointment.contactEmail}>"),
                  attachments = images,
                  bodyHtml = Some(
                    views.html.mail.verifyAppointment(
                      eventId, appointment, dateFormatter, timeFormatter, appUrl
                    ).body
                  )
                )

                cancellables.update(
                  eventId,
                  actorSystem.scheduler.scheduleOnce(
                    12.minutes,
                    cancelActor,
                    CancelAppointment(eventId)
                  ),
                )

                Try(mailerClient.send(email)).toOption match {
                  case Some(id) => Ok(id)
                  case None => InternalServerError("Unable to send email")
                }
                */
                Ok(eventId.value.toString)
              }
            }.recover {
              case e: TimeoutException => {
                logger.error("saveAppointment", e)
                InternalServerError("0")
              }
            }
        }

        val now = ZonedDateTime.now()
        val appointmentStart = appointment.start
        val appointmentEnd = appointment.start.plus(appointment.duration)

        futures.timeout(60 seconds)(
          repository.events.allInRange(
            appointment.start.minusMinutes(60),
            appointment.start.plusMinutes(120),
          ),
        )
          .map {
            _ match {
              case _ if appointmentStart.isBefore(now) => BadRequest("Event is in the past")
              case events if events.length == 0 => Await.result(resolve(), 60 seconds)
              case events => {
                (
                  events
                    .filter(_.locationId == appointment.locationId)
                    .sortBy(_.start)
                    .foldLeft(false) {
                      (conflict, event) => conflict match {
                        case true => true
                        case false => {
                          val eventStart = event.start
                          val eventEnd = event.start.plus(event.duration)

                          (eventStart.compareTo(appointmentStart) <= 0 && eventEnd.compareTo(appointmentStart) > 0) ||
                          (eventStart.compareTo(appointmentStart) >= 0 && eventStart.compareTo(appointmentEnd) < 0) ||
                          (eventStart.compareTo(appointmentStart) >= 0  && eventEnd.compareTo(appointmentEnd) <= 0)
                        }
                      }
                    }
                ) match {
                  case true => Status(409)("Event conflicts with another")
                  case false => Await.result(resolve(), 60 seconds)
                }
              }
            }
          }.recover {
            case e: TimeoutException => {
              logger.error("eventsInRange", e)
              InternalServerError("0")
            }
          }
      }
    )
  }

  def verifyAppointment(id: EventId) = Action.async { implicit request =>
    val futureProductsWithEventAndLocation = for {
      _ <- futures.timeout(60 seconds)(repository.events.confirmEvent(id))
      productsWithEventAndLocationAndCategory <- futures.timeout(60 seconds)(repository.products.getAllForEventWithEventAndLocationAndCategory(id))
    } yield productsWithEventAndLocationAndCategory

    futureProductsWithEventAndLocation.map {
      _ match {
        case productsWithEventAndLocationAndCategory if productsWithEventAndLocationAndCategory.length > 0=> {
          val event = productsWithEventAndLocationAndCategory.head._2
          val location = productsWithEventAndLocationAndCategory.head._3
          val productsWithCategory = productsWithEventAndLocationAndCategory.map({
            case (product, event, location, category) => (product, category)
          })
          val images = productsWithCategory
            .filter(productWithCategory =>
              productWithCategory._1.imageData.isDefined && productWithCategory._1.imageMime.isDefined)
            .map(productWithCategory => {
              AttachmentData(
                name = s"image${productWithCategory._1.id.value}",
                data = productWithCategory._1.imageData.get,
                mimetype = productWithCategory._1.imageMime.get,
                disposition = Some(EmailAttachment.INLINE),
                contentId = Some(productWithCategory._1.id.value.toString),
              )
            })
          val email = Email(
            "Cita Confirmada - Entrepeques.mx",
            "CitasEntrepeques <citas@entrepeques.mx>",
            s"${event.contactName} <${event.contactEmail}>" +: appEmails,
            attachments = images,
            bodyHtml = Some(
              views.html.mail.appointmentCreated(
                event, location, productsWithCategory, dateFormatter, timeFormatter, appUrl
              ).body
            )
          )

          //cancellables.remove(id).map(_.cancel())
          val duration = EventDuration.between(ZonedDateTime.now(), event.start).minusHours(24)
          if (!duration.isNegative()) {
            cancellables.update(
              id,
              actorSystem.scheduler.scheduleOnce(
                Duration.fromNanos(duration.toNanos),
                reminderActor,
                SendReminder(id, appEmails, appUrl),
              ),
            )
          }

          Try(mailerClient.send(email)) match {
            case Success(id) => Ok(
              views.html.appointmentVerified(
                event,
                location,
                dateFormatter,
                timeFormatter,
              )
            )
            case Failure(error) => {
              futures.timeout(60 seconds)(repository.products.cancelEvent(id))
              cancellables.remove(id);
              logger.error(error.toString)
              InternalServerError("Unable to send email")
            }
          }
        }
        case _ => NotFound("The specified id does not exist")
      }
    }.recover {
      case e: TimeoutException => {
        logger.error("verifyAppointment", e)
        InternalServerError("Not verified")
      }
    }
  }

  def cancelAppointment(id: EventId) = Action.async { implicit request =>
    val futureProductsWithEventAndLocation = for {
      productsWithEventAndLocationAndCategory <- futures.timeout(60 seconds)(repository.products.getAllForEventWithEventAndLocationAndCategory(id))
      _ <- futures.timeout(60 seconds)(repository.products.cancelEvent(id))
    } yield productsWithEventAndLocationAndCategory

    futureProductsWithEventAndLocation.map {
      _ match {
        case productsWithEventAndLocationAndCategory if productsWithEventAndLocationAndCategory.length > 0 => {
          val event = productsWithEventAndLocationAndCategory.head._2
          val location = productsWithEventAndLocationAndCategory.head._3

          event.confirmed match {
            case true => {
              val productsWithCategory = productsWithEventAndLocationAndCategory.map({
                case (product, event, location, category) => (product, category)
              })
              val images = productsWithCategory
                .filter(productWithCategory =>
                  productWithCategory._1.imageData.isDefined && productWithCategory._1.imageMime.isDefined)
                .map(productWithCategory => {
                  AttachmentData(
                    name = s"image${productWithCategory._1.id.value}",
                    data = productWithCategory._1.imageData.get,
                    mimetype = productWithCategory._1.imageMime.get,
                    disposition = Some(EmailAttachment.INLINE),
                    contentId = Some(productWithCategory._1.id.value.toString),
                  )
                })
              val email = Email(
                "Cita Cancelada - Entrepeques.mx",
                "CitasEntrepeques <citas@entrepeques.mx>",
                s"${event.contactName} <${event.contactEmail}>" +: appEmails,
                attachments = images,
                bodyHtml = Some(
                  views.html.mail.appointmentCanceled(
                    event, location, productsWithCategory, dateFormatter, timeFormatter, appUrl
                  ).body
                )
              )

              cancellables.remove(id).map(_.cancel())

              Try(mailerClient.send(email)).toOption match {
                case Some(id) => Ok(
                    views.html.appointmentCanceled(
                    event,
                    location,
                    dateFormatter,
                    timeFormatter,
                  )
                )
                case None => InternalServerError("Unable to send email")
              }
            }
            case false => Ok(
                views.html.appointmentCanceled(
                event,
                location,
                dateFormatter,
                timeFormatter,
              )
            )
          }
        }
        case _ => NotFound("The specified id does not exit")
      }
    }.recover {
      case e: TimeoutException => {
        logger.error("cancelAppointment", e)
        InternalServerError("Not canceled")
      }
    }
  }

}
