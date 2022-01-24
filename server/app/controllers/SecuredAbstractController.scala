package controllers

import java.time.Clock

import javax.inject.Inject

import play.api.Environment
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.libs.concurrent.Futures
import play.api.libs.json._
import play.api.Logging
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext

case class SecuredControllerComponents @Inject()(
  actionBuilder: DefaultActionBuilder,
  environment: Environment,
  executionContext: ExecutionContext,
  fileMimeTypes: FileMimeTypes,
  futures: Futures,
  langs: Langs,
  messagesApi: MessagesApi,
  parsers: PlayBodyParsers,
) extends ControllerComponents

abstract class SecuredAbstractController @Inject()(scc: SecuredControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(scc) with Logging {

  implicit val clock: Clock = Clock.systemUTC

  def environment: Environment = scc.environment
  def futures: Futures = scc.futures
}
