package controllers

import database._

import models._

import java.time.Clock

import javax.inject._

import play.api._
import play.api.libs.concurrent.Futures._
import play.api.libs.json._
import play.api.Logging
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.concurrent.duration._

case class Secret(value: String) extends AnyVal

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class MigrationsController @Inject()(
  val scc: SecuredControllerComponents,
  val productRepository: ProductRepository,
  val notificationRepository: NotificationRepository,
)(implicit ec: ExecutionContext, conf: Configuration)
  extends SecuredAbstractController(scc)
  with Logging {

  val appSecret = conf.underlying.getString("play.http.secret.key")

  implicit val secretFormat = Json.format[Secret]

  def applyMigrations() = Action(parse.json).async { implicit request =>
    request.body.validate[Secret].fold(
      errors => {
        logger.warn("Validate: " + JsError.toJson(errors).toString)
        Future(BadRequest(Json.obj("message" -> JsError.toJson(errors))))
      },
      _ match {
        case secret if secret.value == appSecret => {
          val results = for {
            location <- futures.timeout(60 seconds)(productRepository.locations.migrate)
            businessHours <- futures.timeout(60 seconds)(productRepository.businessHours.migrate)
            events <- futures.timeout(60 seconds)(productRepository.events.migrate)
            categories <- futures.timeout(60 seconds)(productRepository.categories.migrate)
            products <- futures.timeout(60 seconds)(productRepository.products.migrate)
            passwords <- futures.timeout(60 seconds)(notificationRepository.passwords.migrate)
            notifications <- futures.timeout(60 seconds)(notificationRepository.notifications.migrate)
          } yield true

          results.map {
            _ => Ok("Success")
          }.recover {
            case e: TimeoutException => {
              logger.error("applyMigrations", e)
              InternalServerError("Timeout Exception")
            }
          }
        }
        case _ => Future(Unauthorized("Not authorized"))
      }
    )
  }

}
