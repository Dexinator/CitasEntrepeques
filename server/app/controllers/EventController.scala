package controllers

import database.EventRepository

import java.time.{LocalDateTime, ZonedDateTime, ZoneId}

import javax.inject._

import models._

import play.api._
import play.api.libs.concurrent.Futures._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class EventController @Inject()(
  val scc: SecuredControllerComponents,
  val repository: EventRepository,
)(implicit ec: ExecutionContext, conf: Configuration)
  extends SecuredAbstractController(scc)
  with EventJsonOps
  with Logging {

  def allEvents() = Action.async { implicit request =>
    futures.timeout(60 seconds)(repository.events.all())
      .map {
        events => Ok(Json.toJson(events))
      }.recover {
        case e: TimeoutException => {
          logger.error("allEvents", e)
          InternalServerError(Json.toJson(List.empty[Event]))
        }
      }
  }

  def allEventsShort() = Action.async { implicit request =>
    futures.timeout(60 seconds)(repository.events.all())
      .map {
        events => Ok(
          Json.toJson(
            events.map(event => EventShort(
              event.start,
              event.duration,
              event.locationId,
              event.confirmed,
            ))
          )
        )
      }.recover {
        case e: TimeoutException => {
          logger.error("allEventsShort", e)
          InternalServerError(Json.toJson(List.empty[Event]))
        }
      }
  }

  def allEventsShortInRangeWithLocation(startISO: String, endISO: String, timeZoneISO: Option[String]) =
    Action.async { implicit request =>
      val range = timeZoneISO match {
        case Some(timeZone) if !timeZone.equalsIgnoreCase("UTC") => for {
          start <- Try(LocalDateTime.parse(startISO)).toOption
          end <- Try(LocalDateTime.parse(endISO)).toOption
          timeZone <- Try(ZoneId.of(timeZone)).toOption
        } yield (ZonedDateTime.of(start, timeZone), ZonedDateTime.of(end, timeZone))
        case _ => for {
          start <- Try(ZonedDateTime.parse(startISO)).toOption
          end <- Try(ZonedDateTime.parse(endISO)).toOption
        } yield (start, end)
      }

      range match {
        case Some((start, end)) => futures.timeout(60 seconds)(repository.events.allInRangeWithLocation(start, end))
          .map {
            eventsWithLocation => Ok(
              Json.toJson(
                eventsWithLocation.map(eventWithLocation => EventShortWithLocation(
                  eventWithLocation._1.start,
                  eventWithLocation._1.duration,
                  eventWithLocation._2.address,
                  eventWithLocation._2.name,
                  eventWithLocation._1.confirmed,
                ))
              )
            )
          }.recover {
            case e: TimeoutException => {
              logger.error("eventsInRange", e)
              InternalServerError(Json.toJson(List.empty[Event]))
            }
          }
        case None => Future(BadRequest("Invalid parameters"))
      }
    }

  def allEventsInRange(startISO: String, endISO: String, timeZoneISO: Option[String]) =
    Action.async { implicit request =>
      val range = timeZoneISO match {
        case Some(timeZone) if !timeZone.equalsIgnoreCase("UTC") => for {
          start <- Try(LocalDateTime.parse(startISO)).toOption
          end <- Try(LocalDateTime.parse(endISO)).toOption
          timeZone <- Try(ZoneId.of(timeZone)).toOption
        } yield (ZonedDateTime.of(start, timeZone), ZonedDateTime.of(end, timeZone))
        case _ => for {
          start <- Try(ZonedDateTime.parse(startISO)).toOption
          end <- Try(ZonedDateTime.parse(endISO)).toOption
        } yield (start, end)
      }

      range match {
        case Some((start, end)) => futures.timeout(60 seconds)(repository.events.allInRange(start, end))
          .map {
            events => Ok(Json.toJson(events))
          }.recover {
            case e: TimeoutException => {
              logger.error("eventsInRange", e)
              InternalServerError(Json.toJson(List.empty[Event]))
            }
          }
        case None => Future(BadRequest("Invalid parameters"))
      }
    }

  def allEventsInRangeWithLocation(startISO: String, endISO: String, timeZoneISO: Option[String]) =
    Action.async { implicit request =>
      val range = timeZoneISO match {
        case Some(timeZone) if !timeZone.equalsIgnoreCase("UTC") => for {
          start <- Try(LocalDateTime.parse(startISO)).toOption
          end <- Try(LocalDateTime.parse(endISO)).toOption
          timeZone <- Try(ZoneId.of(timeZone)).toOption
        } yield (ZonedDateTime.of(start, timeZone), ZonedDateTime.of(end, timeZone))
        case _ => for {
          start <- Try(ZonedDateTime.parse(startISO)).toOption
          end <- Try(ZonedDateTime.parse(endISO)).toOption
        } yield (start, end)
      }

      range match {
        case Some((start, end)) => futures.timeout(60 seconds)(repository.events.allInRangeWithLocation(start, end))
          .map {
            eventsWithLocation => Ok(
                Json.toJson(
                eventsWithLocation.map(eventWithLocation => EventWithLocation(
                  eventWithLocation._1.id,
                  eventWithLocation._1.start,
                  eventWithLocation._1.duration,
                  eventWithLocation._2.address,
                  eventWithLocation._2.name,
                  eventWithLocation._1.contactEmail,
                  eventWithLocation._1.contactName,
                  eventWithLocation._1.contactPhone,
                  eventWithLocation._1.confirmed,
                ))
              )
            )
          }.recover {
            case e: TimeoutException => {
              logger.error("eventsInRange", e)
              InternalServerError(Json.toJson(List.empty[Event]))
            }
          }
        case None => Future(BadRequest("Invalid parameters"))
      }
    }
}
