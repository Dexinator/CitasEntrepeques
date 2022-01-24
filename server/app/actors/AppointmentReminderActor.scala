package actors

import akka.actor.Actor

import database.ProductRepository

import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.Locale

import javax.inject.Inject

import models.EventId

import org.apache.commons.mail.EmailAttachment

import play.api.Logging
import play.api.libs.mailer.{AttachmentData, Email, MailerClient}

import scala.concurrent.ExecutionContext
import scala.util.Try

object AppoinmentReminderActor {
  case class SendReminder(id: EventId, appEmails: Seq[String], appUrl: String)
}

class AppoinmentReminderActor @Inject()(
  val mailerClient: MailerClient,
  val repository: ProductRepository,
) extends Actor with Logging {
  import AppoinmentReminderActor._

  implicit val executionContext: ExecutionContext =
    context.system.dispatchers.lookup("event-reminder-dispatcher")

  val dateFormatter = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.LONG)
    .withLocale(new Locale("es", "mx"))
  val timeFormatter = DateTimeFormatter
    .ofLocalizedTime(FormatStyle.SHORT)
    .withLocale(new Locale("es", "mx"))

  def receive = {
    case SendReminder(id: EventId, appEmails: Seq[String], appUrl: String) =>
      sender() ! repository.products.getAllForEventWithEventAndLocationAndCategory(id)
        .map {
          productsWithEventAndLocationAndCategory => {
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
              "Recordatorio de Cita - Entrepeques.mx",
              "CitasEntrepeques <citas@entrepeques.mx>",
              s"${event.contactName} <${event.contactEmail}>" +: appEmails,
              attachments = images,
              bodyHtml = Some(
                views.html.mail.remindAppointment(
                  event, location, productsWithCategory, dateFormatter, timeFormatter, appUrl
                ).body
              )
            )

            Try(mailerClient.send(email)).toOption match {
              case Some(id) => ()
              case None => logger.error("Unable to send email")
            }
          }
        }
  }
}
