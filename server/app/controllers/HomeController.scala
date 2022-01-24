package controllers

import database.EventRepository

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
class HomeController @Inject()(
  val scc: SecuredControllerComponents,
  val repository: EventRepository,
)(implicit ec: ExecutionContext, conf: Configuration)
  extends SecuredAbstractController(scc) {

}
