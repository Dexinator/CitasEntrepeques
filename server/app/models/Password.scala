package models

import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}

import slick.lifted.MappedTo

case class PasswordId(value: Short) extends MappedTo[Short]
object PasswordId {
  implicit def queryStringBindable(implicit shortBinder: QueryStringBindable[Short]) =
    new QueryStringBindable[PasswordId] {
      override def bind(key: String,
        params: Map[String, Seq[String]]): Option[Either[String, PasswordId]] = {
        for {
          id <- shortBinder.bind(key, params)
        } yield id.map(PasswordId(_))
      }
      override def unbind(key: String, id: PasswordId): String =
        shortBinder.unbind(key, id.value)
    }

  implicit def pathBinder(implicit intBinder: PathBindable[Int]) =
    new PathBindable[PasswordId] {
      override def bind(key: String, value: String): Either[String, PasswordId] = {
        for {
          id <- intBinder.bind(key, value)
        } yield PasswordId(id.toShort)
      }
      override def unbind(key: String, id: PasswordId): String =
        intBinder.unbind(key, id.value)
    }
}
trait PasswordIdJsonOps {
  implicit val passwordIdFormat = Json.valueFormat[PasswordId]
}

case class Password(id: PasswordId, value: String, master: Boolean = true)
case class ChangePassword(newPassword: String, masterPassword: String, master: Boolean = false) {
  def toPassword = Password(PasswordId(0), newPassword, master)
}
trait PasswordJsonOps extends PasswordIdJsonOps {
  implicit val passwordFormat = Json.format[Password]
  implicit val changePasswordFormat = Json.format[ChangePassword]
}
