package database

import java.time.{Duration, ZonedDateTime, ZoneId}
import java.time.temporal.ChronoUnit

import javax.inject.{Inject, Singleton}

import models._

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{Future, ExecutionContext}

import slick.jdbc.JdbcProfile
import slick.migration.api._

@Singleton
class EventRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)
  (implicit ec: ExecutionContext) extends LocationRepository(dbConfigProvider) {

  /*
    These imports are important, the first one brings db into scope,
    which will let you do the actual db operations.
    The second one brings the Slick DSL into scope,
    which lets you define the table and other queries.
  */
  import dbConfig._
  import profile.api._

  protected val zoneId = ZoneId.of("America/Mexico_City")

  protected class Events(tag: Tag) extends Table[Event](tag, "events") {
    def id = column[EventId]("id", O.PrimaryKey)
    def start = column[ZonedDateTime]("start")
    def duration = column[Duration]("duration")
    def locationId = column[LocationId]("location_id")
    def contactName = column[String]("contact_name")
    def contactEmail = column[String]("contact_email")
    def contactPhone = column[String]("contact_phone")
    def confirmed = column[Boolean]("confirmed")

    def contactEmailIndex = index("index_events_contact_email", contactEmail)
    def startIndex = index("index_events_start", start)

    def locationIdForeignKey = foreignKey(
      "events_location_id",
      locationId,
      locations,
    )(
      _.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade,
    )

    override def * = (id, start, duration, locationId, contactEmail,
      contactName, contactPhone, confirmed) <> (Event.tupled, Event.unapply)
  }

  object events extends TableQuery(new Events(_)) {
    private def deinit() = TableMigration(this)
      .dropForeignKeys(_.locationIdForeignKey)
      .dropIndexes(_.contactEmailIndex, _.startIndex)
      .drop
    private def init() = TableMigration(this).create
      .addColumns(
        _.id,
        _.start,
        _.duration,
        _.locationId,
        _.contactName,
        _.contactEmail,
        _.contactPhone,
        _.confirmed,
      )
      .addIndexes(_.contactEmailIndex, _.startIndex)
      .addForeignKeys(_.locationIdForeignKey)
    private def seed() = {
      SqlMigration()
    }
    private def migration() = init() & seed()
    def migrate = db.run { migration().apply() }

    def all(): Future[Seq[Event]] = db.run { this.result }

    def allInRange(start: ZonedDateTime, end: ZonedDateTime): Future[Seq[Event]] = db.run {
      events
        .filter(event => event.start >= start &&
          event.start <= end.minusMinutes(60))
        .result
    }

    def allInRangeWithLocation(start: ZonedDateTime, end: ZonedDateTime): Future[Seq[(Event, Location)]] = db.run {
      events
        .join(locations)
        .on(_.locationId === _.id)
        .filter(eventWithLocation => eventWithLocation._1.start >= start &&
          eventWithLocation._1.start <= end.minusMinutes(60))
        .result
    }

    def confirmEvent(id: EventId) = db.run {
      this.filter(_.id === id).map(_.confirmed).update(true)
    }
  }

}
