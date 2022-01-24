package models

import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}

import slick.lifted.MappedTo

case class NotificationId(value: Long) extends MappedTo[Long]
object NotificationId {
  implicit def queryStringBindable(implicit longBinder: QueryStringBindable[Long]) =
    new QueryStringBindable[NotificationId] {
      override def bind(key: String,
        params: Map[String, Seq[String]]): Option[Either[String, NotificationId]] = {
        for {
          id <- longBinder.bind(key, params)
        } yield id.map(NotificationId(_))
      }
      override def unbind(key: String, id: NotificationId): String =
        longBinder.unbind(key, id.value)
    }

  implicit def pathBinder(implicit longBinder: PathBindable[Long]) =
    new PathBindable[NotificationId] {
      override def bind(key: String, value: String): Either[String, NotificationId] = {
        for {
          id <- longBinder.bind(key, value)
        } yield NotificationId(id.toShort)
      }
      override def unbind(key: String, id: NotificationId): String =
        longBinder.unbind(key, id.value)
    }
}
trait NotificationIdJsonOps {
  implicit val notificationIdFormat = Json.valueFormat[NotificationId]
}

case class Notification(id: NotificationId, value: String)
case class ChangeNotification(notification: String, password: String)
trait NotificationJsonOps extends NotificationIdJsonOps {
  implicit val notificationFormat = Json.format[Notification]
  implicit val changeNotificationFormat = Json.format[ChangeNotification]
}
