package controllers

import database.PasswordRepository

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
class PasswordController @Inject()(
  val scc: SecuredControllerComponents,
  val repository: PasswordRepository,
)(implicit ec: ExecutionContext, conf: Configuration)
  extends SecuredAbstractController(scc) with PasswordJsonOps {

  def authorize() = Action(parse.json).async { implicit request =>
    request.body.validate[Password].fold(
      errors => {
        logger.warn("Validate: " + JsError.toJson(errors).toString)
        Future(BadRequest(Json.obj("message" -> JsError.toJson(errors))))
      },
      password => {
        futures.timeout(60 seconds)(repository.passwords.last(password.master))
          .map {
            _ match {
              case lastPassword if password.value == lastPassword.value => Ok("Authorized")
              case _ => Unauthorized("Unauthorized")
            }
          }.recover {
            case e: TimeoutException => {
              logger.error("authorize", e)
              InternalServerError("0")
            }
          }
      }
    )
  }

  def changePassword() = Action(parse.json).async { implicit request =>
    request.body.validate[ChangePassword].fold(
      errors => {
        logger.warn("Validate: " + JsError.toJson(errors).toString)
        Future(BadRequest(Json.obj("message" -> JsError.toJson(errors))))
      },
      changePassword => {
        futures.timeout(60 seconds)(repository.passwords.addOneIfAuthorized(changePassword))
          .map {
            _ match {
              case x if x > 0 => Ok("Changed")
              case _ => Unauthorized("Not Changed")
            }
          }.recover {
            case e: TimeoutException => {
              logger.error("changePassword", e)
              InternalServerError("0")
            }
          }
      }
    )
  }

}
