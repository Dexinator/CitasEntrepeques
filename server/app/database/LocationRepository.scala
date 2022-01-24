package database

import java.time.{Duration, LocalTime}
import java.time.temporal.ChronoUnit

import javax.inject.{Inject, Singleton}

import models._

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{Future, ExecutionContext}

import slick.jdbc.JdbcProfile
import slick.migration.api._

@Singleton
class LocationRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)
  (implicit ec: ExecutionContext) {

  // We want the JdbcProfile for this provider

  protected val dbConfig = dbConfigProvider.get[JdbcProfile]

  implicit val dialect = new PostgresDialect

  /*
    These imports are important, the first one brings db into scope,
    which will let you do the actual db operations.
    The second one brings the Slick DSL into scope,
    which lets you define the table and other queries.
  */
  import dbConfig._
  import profile.api._

  protected class Locations(tag: Tag) extends Table[Location](tag, "locations") {
    def id = column[LocationId]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def address = column[String]("address")
    def phone = column[String]("phone")

    override def * = (id, name, address, phone) <> (Location.tupled, Location.unapply)
  }

  object locations extends TableQuery(new Locations(_)) {
    private def deinit() = TableMigration(this)
      .dropColumns(_.id, _.name, _.address, _.phone)
      .drop
    private def init() = TableMigration(this)
      .create
      .addColumns(_.id, _.name, _.address, _.phone)
    private def seed() = {
      SqlMigration(s"""
        INSERT INTO locations (name, address, phone) VALUES
        (
          'Polanco',
          'Homero #1616 Colonia Polanco, Alcaldía Miguel Hidalgo, CDMX CP 11510',
          '5575811780'
        ),
        (
          'Anzures',
          'Gutenberg #194 Colonia Anzures, Alcaldía Miguel Hidalgo, CDMX CP 11590',
          '5565883245'
        );
      """)
    }
    private def migration() = init() & seed()
    def migrate = db.run { migration().apply() }
    def all(): Future[Seq[Location]] = db.run { this.result }
  }

  implicit val dayColumnType =
    MappedColumnType.base[Day, Short](_.value, Day.withValue(_))
  implicit val durationColumnType =
    MappedColumnType.base[Duration, String](_.toString, Duration.parse(_))

  protected class BusinessHours(tag: Tag) extends Table[BusinessHour](tag, "business_hours") {
    def id = column[BusinessHourId]("id", O.PrimaryKey, O.AutoInc)
    def start = column[LocalTime]("start")
    def duration = column[Duration]("duration")
    def day = column[Day]("day")
    def locationId = column[LocationId]("location_id")

    def locationIdForeignKey = foreignKey(
      "business_hours_location_id",
      locationId,
      locations,
    )(
      _.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade,
    )

    override def * = (id, start, duration, day,
      locationId) <> (BusinessHour.tupled, BusinessHour.unapply)
  }

  object businessHours extends TableQuery(new BusinessHours(_)) {
    private def deinit() = TableMigration(this)
      .dropForeignKeys(_.locationIdForeignKey)
      .drop
    private def init() = TableMigration(this)
      .create
      .addColumns(_.id, _.start, _.duration, _.day, _.locationId)
      .addForeignKeys(_.locationIdForeignKey)
    private def seed() = {
      SqlMigration(s"""
        INSERT INTO business_hours (start, duration, day, location_id) VALUES
        ('${LocalTime.of(10, 40).toString}', '${Duration.ofHours(3).plusMinutes(20).toString}', ${Day.Monday.value}, 1),
        ('${LocalTime.of(16, 0).toString}', '${Duration.ofHours(2).toString}', ${Day.Monday.value}, 1),
        ('${LocalTime.of(10, 40).toString}', '${Duration.ofHours(3).plusMinutes(20).toString}', ${Day.Wednesday.value}, 1),
        ('${LocalTime.of(16, 0).toString}', '${Duration.ofHours(2).toString}', ${Day.Wednesday.value}, 1),
        ('${LocalTime.of(12, 0).toString}', '${Duration.ofHours(2).toString}', ${Day.Friday.value}, 1),
        ('${LocalTime.of(10, 40).toString}', '${Duration.ofHours(3).plusMinutes(20).toString}', ${Day.Tuesday.value}, 2),
        ('${LocalTime.of(16, 0).toString}', '${Duration.ofHours(2).toString}', ${Day.Tuesday.value}, 2),
        ('${LocalTime.of(10, 40).toString}', '${Duration.ofHours(3).plusMinutes(20).toString}', ${Day.Thursday.value}, 2),
        ('${LocalTime.of(16, 0).toString}', '${Duration.ofHours(2).toString}', ${Day.Thursday.value}, 2),
        ('${LocalTime.of(12, 0).toString}', '${Duration.ofHours(2).toString}', ${Day.Friday.value}, 2);
      """)
    }
    private def migration() = init() & seed()
    def migrate = db.run { migration().apply() }
    def all(): Future[Seq[BusinessHour]] = db.run { this.result }
    def allWithLocation(): Future[Seq[(BusinessHour, Location)]] = db.run {
      this.join(locations).on(_.locationId === _.id).result
    }
  }

}
