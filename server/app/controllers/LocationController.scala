package controllers

import database.LocationRepository

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
class LocationController @Inject()(
  val scc: SecuredControllerComponents,
  val repository: LocationRepository,
)(implicit ec: ExecutionContext, conf: Configuration)
  extends SecuredAbstractController(scc)
  with LocationJsonOps
  with LocationWithBusinessHoursJsonOps {

  def allLocations() = Action.async { implicit request =>
    futures.timeout(60 seconds)(repository.locations.all())
      .map {
        locations => Ok(Json.toJson(locations))
      }.recover {
        case e: TimeoutException => {
          logger.error("allCategories", e)
          InternalServerError(Json.toJson(List.empty[Location]))
        }
      }
  }

  def allBusinessHours() = Action.async { implicit request =>
    futures.timeout(60 seconds)(repository.businessHours.all())
      .map {
        businessHours => Ok(Json.toJson(businessHours))
      }.recover {
        case e: TimeoutException => {
          logger.error("allBusinessHours", e)
          InternalServerError(Json.toJson(List.empty[BusinessHour]))
        }
      }
  }

  def allBusinessHoursWithLocation() = Action.async { implicit request =>
    futures.timeout(60 seconds)(repository.businessHours.allWithLocation())
      .map {
        businessHoursWithLocation => {
          Ok(
            Json.toJson(
              businessHoursWithLocation
                .groupBy(_._1.locationId)
                .map(group => {
                  val location: Location = group._2.head._2
                  LocationWithBusinessHours(
                    location.id,
                    location.name,
                    location.address,
                    group._2.map(_._1),
                  )
                })
            )
          )
        }
      }.recover {
        case e: TimeoutException => {
          logger.error("allBusinessHoursWithLocation", e)
          InternalServerError(Json.toJson(List.empty[BusinessHour]))
        }
      }
  }
}
