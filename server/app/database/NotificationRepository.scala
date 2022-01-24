package database

import javax.inject.{Inject, Singleton}

import models._

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Try}

import slick.jdbc.JdbcProfile
import slick.migration.api._

@Singleton
class NotificationRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)
  (implicit ec: ExecutionContext) extends PasswordRepository(dbConfigProvider) {

  /*
    These imports are important, the first one brings db into scope,
    which will let you do the actual db operations.
    The second one brings the Slick DSL into scope,
    which lets you define the table and other queries.
  */
  import dbConfig._
  import profile.api._

  protected class Notifications(tag: Tag) extends Table[Notification](tag, "notifications") {
    def id = column[NotificationId]("id", O.PrimaryKey, O.AutoInc)
    def value = column[String]("value")

    override def * = (id, value) <> (Notification.tupled, Notification.unapply)
  }

  object notifications extends TableQuery(new Notifications(_)) {
    private def deinit() = TableMigration(this)
      .dropColumns(_.id, _.value)
      .drop
    private def init() = TableMigration(this)
      .create
      .addColumns(_.id, _.value)
    private def seed() = {
      SqlMigration(s"""""")
    }
    private def migration() = init() & seed()
    def migrate = db.run { migration().apply() }
    def all(): Future[Seq[Notification]] = db.run { this.result }
    def last(): Future[Option[Notification]] = db.run { this.sortBy(_.id.desc).result.headOption }
    def addOneIfAuthorized(changeNotification: ChangeNotification): Future[Int] = {
      val transaction = (
        for {
          lastPassword <- passwords.filter(_.master === true).sortBy(_.id.desc).result.head
          if lastPassword.value == changeNotification.password
          notificationsAdded <- this += Notification(NotificationId(0), changeNotification.notification)
        } yield notificationsAdded
      ).transactionally

      db.run(transaction).recover({ case e: NoSuchElementException => 0 })
    }
  }

}
