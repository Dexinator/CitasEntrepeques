package controllers

import database.NotificationRepository

import models._

import javax.inject._

import play.api._
import play.api.libs.concurrent.Futures._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class NotificationController @Inject()(
  val scc: SecuredControllerComponents,
  val repository: NotificationRepository,
)(implicit ec: ExecutionContext, conf: Configuration)
  extends SecuredAbstractController(scc) with NotificationJsonOps {

  def getNotification() = Action.async { implicit request =>
    futures.timeout(60 seconds)(repository.notifications.last())
      .map {
        _ match {
          case Some(notification) => Ok(notification.value)
          case None => Ok("")
        }
      }.recover {
        case e: TimeoutException => {
          logger.error("getNotification", e)
          InternalServerError("")
        }
      }
  }

  def changeNotification() = Action(parse.json).async { implicit request =>
    request.body.validate[ChangeNotification].fold(
      errors => {
        logger.warn("Validate: " + JsError.toJson(errors).toString)
        Future(BadRequest(Json.obj("message" -> JsError.toJson(errors))))
      },
      changeNotification => {
        futures.timeout(60 seconds)(repository.notifications.addOneIfAuthorized(changeNotification))
          .map {
            _ match {
              case x if x > 0 => Ok("Changed")
              case _ => Unauthorized("Not Changed")
            }
          }.recover {
            case e: TimeoutException => {
              logger.error("changeNotification", e)
              InternalServerError("0")
            }
          }
      }
    )
  }

}
