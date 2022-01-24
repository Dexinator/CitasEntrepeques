package database

import javax.inject.{Inject, Singleton}

import models._

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Try}

import slick.jdbc.JdbcProfile
import slick.migration.api._

@Singleton
class PasswordRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)
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

  protected class Passwords(tag: Tag) extends Table[Password](tag, "passwords") {
    def id = column[PasswordId]("id", O.PrimaryKey, O.AutoInc)
    def value = column[String]("value")
    def master = column[Boolean]("master")

    override def * = (id, value, master) <> (Password.tupled, Password.unapply)
  }

  object passwords extends TableQuery(new Passwords(_)) {
    private def deinit() = TableMigration(this)
      .dropColumns(_.id, _.value)
      .drop
    private def init() = TableMigration(this)
      .create
      .addColumns(_.id, _.value)
    private def seed() = {
      SqlMigration(s"""
        INSERT INTO passwords (value) VALUES
        ('password1234');
      """)
    }
    private def migration() = init() & seed()
    def migrate = db.run { migration().apply() }
    def all(): Future[Seq[Password]] = db.run { this.result }
    def last(master: Boolean = true): Future[Password] = db.run {
      this.filter(_.master === master).sortBy(_.id.desc).result.head
    }
    def addOneIfAuthorized(changePassword: ChangePassword): Future[Int] = {
      val transaction = (
        for {
          lastPassword <- this.filter(_.master === true)
            .sortBy(_.id.desc)
            .result
            .head
          if lastPassword.value == changePassword.masterPassword
          passwordsAdded <- this += changePassword.toPassword
        } yield passwordsAdded
      ).transactionally

      db.run(transaction).recover({ case e: NoSuchElementException => 0 })
    }
  }

}
