package controllers

import com.github.tototoshi.csv.CSVWriter

import database.{PasswordRepository, ProductRepository}

import java.io.File
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

import javax.inject._

import models._

import play.api._
import play.api.libs.concurrent.Futures._
import play.api.libs.json._
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProductController @Inject()(
  val scc: SecuredControllerComponents,
  val passwordRepository: PasswordRepository,
  val productRepository: ProductRepository,
)(implicit ec: ExecutionContext, conf: Configuration)
  extends SecuredAbstractController(scc)
  with CategoryJsonOps
  with PasswordJsonOps
  with ProductJsonOps {

  implicit val dateFormatter = DateTimeFormatter
    .ofPattern("dd-MM-yyyy hh:mm:ss")

  def allCategories() = Action.async { implicit request =>
    futures.timeout(60 seconds)(productRepository.categories.all())
      .map {
        categories => Ok(Json.toJson(categories))
      }.recover {
        case e: TimeoutException => {
          logger.error("allCategories", e)
          InternalServerError(Json.toJson(List.empty[Category]))
        }
      }
  }

  def allCollection() = Action { implicit request =>
    Ok(Json.toJson(Collection.values))
  }

  def updateCategory() = Action(parse.json).async { implicit request =>
    request.body.validate[CategoryOption].fold(
      errors => {
        logger.warn("Validate: " + JsError.toJson(errors).toString)
        Future(BadRequest(Json.obj("message" -> JsError.toJson(errors))))
      },
      option => {
        val numChanged = for {
          password <- futures.timeout(60 seconds)(passwordRepository.passwords.last())
          updated <-  futures.timeout(60 seconds)(
            if (option.password == password.value) productRepository.categories.updateByOption(option)
            else (Future(-1))
          )
        } yield updated

        numChanged.map {
          _ match {
            case x if x >= 0 => Ok("Updated")
            case _ => Unauthorized("Not Changed")
          }
        }.recover {
          case e: TimeoutException => {
            logger.error("updateCategories", e)
            InternalServerError("0")
          }
        }
      }
    )
  }

  def productsForEvent() = Action(parse.json).async { implicit request =>
    request.body.validate[ProductQuery].fold(
      errors => {
        logger.warn("Validate: " + JsError.toJson(errors).toString)
        Future(BadRequest(Json.obj("message" -> JsError.toJson(errors))))
      },
      query => {
        val products = for {
          password <- futures.timeout(60 seconds)(passwordRepository.passwords.last(query.master))
          updated <-  futures.timeout(60 seconds)(
            if (query.password == password.value) productRepository.products.getAllForEvent(query.eventId)
            else (Future(List.empty[Product]))
          )
        } yield updated

        products.map {
          _ match {
            case list: List[Product] => Unauthorized(Json.toJson(List.empty[Product]))
            case seq => Ok(Json.toJson(seq))
          }
        }.recover {
          case e: TimeoutException => {
            logger.error("updateCategories", e)
            InternalServerError(Json.toJson(List.empty[Product]))
          }
        }
      }
    )
  }

  def allEventsWithInfomrationInCSV() = Action(parse.json).async { implicit request =>
    request.body.validate[Password].fold(
      errors => {
        logger.warn("Validate: " + JsError.toJson(errors).toString)
        Future(BadRequest(Json.obj("message" -> JsError.toJson(errors))))
      },
      passwordContainer => {
        val eventsWithInformation = for {
          password <- futures.timeout(60 seconds)(passwordRepository.passwords.last(false))
          events <-  futures.timeout(60 seconds)(
            if (passwordContainer.value == password.value) productRepository.products.getAllWithEventAndLocationAndCategory()
            else (Future(List.empty[(Product, Event, Location, Category)]))
          )
        } yield events

        eventsWithInformation.map {
          _ match {
            case list: List[(Product, Event, Location, Category)] => Unauthorized("Can't download csv")
            case seq => {
              val tempFile = SingletonTemporaryFileCreator.create("events.csv")
              val writer = CSVWriter.open(tempFile)
              writer.writeRow(
                Seq(
                  "producto-cantidad",
                  "producto-calidad",
                  "producto-categoria",
                  "producto-colección",
                  "fecha-inicio",
                  "fecha-termino",
                  "cliente-correo",
                  "cliente-nombre",
                  "cliente-telefono",
                  "cita-confirmada",
                  "tienda-nombre",
                  "tienda-direccion",
                )
              )
              writer.writeAll(seq.map({ case (product, event, location, category) =>
                product.toSeq() ++ category.toSeq() ++ event.toSeq() ++ location.toSeq() }))
              writer.close()
              Ok.sendFile(tempFile)}
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
}
