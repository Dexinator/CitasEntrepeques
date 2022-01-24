package database

import java.time.{ZonedDateTime, ZoneId}
import java.time.temporal.ChronoUnit
import java.util.Base64

import javax.inject.{Inject, Singleton}

import models._

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{Future, ExecutionContext}

import slick.jdbc.JdbcProfile
import slick.migration.api._

@Singleton
class ProductRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)
  (implicit ec: ExecutionContext) extends EventRepository(dbConfigProvider) {

  /*
    These imports are important, the first one brings db into scope,
    which will let you do the actual db operations.
    The second one brings the Slick DSL into scope,
    which lets you define the table and other queries.
  */
  import dbConfig._
  import profile.api._

  implicit val collectionColumnType =
    MappedColumnType.base[Collection, Short](_.value, Collection.withValue(_))

  protected class Categories(tag: Tag) extends Table[Category](tag, "categories") {
    def id = column[CategoryId]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def collection = column[Collection]("collection")
    def icon = column[String]("icon", O.Default("tags"))
    def requiresImage = column[Boolean]("requires_image", O.Default(false))
    def active = column[Boolean]("active", O.Default(true))

    def collectionIndex = index("index_categories_collection", collection)

    override def * = (id, name, collection, icon, requiresImage,
      active) <> (Category.tupled, Category.unapply)
  }

  object categories extends TableQuery(new Categories(_)) {
    private def deinit() = TableMigration(this)
      .dropIndexes(_.collectionIndex)
      .drop
    private def init() = TableMigration(this)
      .create
      .addColumns(_.id, _.name, _.collection, _.icon, _.requiresImage, _.active)
      .addIndexes(_.collectionIndex)
    private def seed() = {
      SqlMigration(s"""
        INSERT INTO categories (name, collection) VALUES
        ('Carriolas', ${Collection.ForAWalk.value}),
        ('Sillas para auto', ${Collection.ForAWalk.value}),
        ('Cangureras, fular y mochilas', ${Collection.ForAWalk.value}),
        ('Bicis y montables', ${Collection.ForAWalk.value}),
        ('Pañaleras', ${Collection.ForAWalk.value}),
        ('Otros de paseo', ${Collection.ForAWalk.value}),
        ('Cunas y colechos', ${Collection.ElectronicsAndAccesories.value}),
        ('Accesorios de recámara', ${Collection.ElectronicsAndAccesories.value}),
        ('Bañeras, bañitos y tinas', ${Collection.ElectronicsAndAccesories.value}),
        ('Donas y almohadas', ${Collection.ElectronicsAndAccesories.value}),
        ('Mecedoras y columpios', ${Collection.ElectronicsAndAccesories.value}),
        ('Sillas para comer', ${Collection.ElectronicsAndAccesories.value}),
        ('Otros de hogar y baño', ${Collection.ElectronicsAndAccesories.value}),
        ('Exctractores de leche', ${Collection.Entertainment.value}),
        ('Para los biberones', ${Collection.Entertainment.value}),
        ('Monitores', ${Collection.Entertainment.value}),
        ('Procesadores de alimentos', ${Collection.Entertainment.value}),
        ('Seguridad', ${Collection.Entertainment.value}),
        ('Otros accesorios', ${Collection.Entertainment.value}),
        ('Alimentación', ${Collection.Entertainment.value}),
        ('Andaderas y brincolines', ${Collection.BathAndHome.value}),
        ('Caminadoras y montables de bebé', ${Collection.BathAndHome.value}),
        ('Mesas y centros de actividades', ${Collection.BathAndHome.value}),
        ('Juegos de jardín', ${Collection.BathAndHome.value}),
        ('Gimnasios y tapetes', ${Collection.BathAndHome.value}),
        ('Libros y rompecabezas', ${Collection.BathAndHome.value}),
        ('Juguetes', ${Collection.BathAndHome.value}),
        ('Ropa para bebes o niñas', ${Collection.Clothing.value}),
        ('Ropa para bebes o niños', ${Collection.Clothing.value}),
        ('Ropa para mujeres o de maternidad', ${Collection.Clothing.value}),
        ('Disfraces', ${Collection.Clothing.value}),
        ('Calzado para niña', ${Collection.Clothing.value}),
        ('Calzado para niño', ${Collection.Clothing.value}),
        ('Otro categoria', ${Collection.Other.value});
        UPDATE categories SET requires_image = true WHERE id IN (1,2,7,11,24);
      """)
    }
    private def migration() = init() & seed()
    def migrate = db.run { migration().apply() }

    def findById(id: CategoryId): Future[Option[Category]] = db.run {
      this.filter(_.id === id).result.headOption
    }

    def all(): Future[Seq[Category]] = db.run { this.result }

    def updateByOption(option: CategoryOption): Future[Int] = db.run {
      this
        .filter(_.id === option.id)
        .map(category => (category.requiresImage, category.active))
        .update((option.requiresImage, option.active))
    }
  }

  implicit val qualityColumnType =
    MappedColumnType.base[Quality, Short](_.value, Quality.withValue(_))

  protected class Products(tag: Tag) extends Table[Product](tag, "products") {
    def id = column[ProductId]("id", O.PrimaryKey, O.AutoInc)
    def quality = column[Quality]("quality")
    def quantity = column[Short]("quantity")
    def categoryId = column[CategoryId]("category_id")
    def eventId = column[EventId]("event_id")
    def imageMime = column[Option[String]]("image_mime")
    def imageData = column[Option[Array[Byte]]]("image_data")

    def categoryIdForeignKey = foreignKey(
      "products_category_id",
      categoryId,
      categories,
    )(
      _.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade,
    )
    def eventIdForeignKey = foreignKey(
      "products_event_id",
      eventId,
      events,
    )(
      _.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Cascade,
    )

    override def * = (id, quality, quantity, categoryId, eventId, imageMime,
      imageData) <> (Product.tupled, Product.unapply)
  }

  object products extends TableQuery(new Products(_)) {
    private def deinit() = TableMigration(this)
      .dropForeignKeys(_.categoryIdForeignKey, _.eventIdForeignKey)
      .drop
    private def init() = TableMigration(this)
      .create
      .addColumns(_.id, _.quality, _.quantity, _.categoryId, _.eventId, _.imageMime, _.imageData)
      .addForeignKeys(_.categoryIdForeignKey, _.eventIdForeignKey)
    private def seed() = {
      SqlMigration()
    }
    private def migration() = init() & seed()
    def migrate = db.run { migration().apply() }

    def addAppointment(appointment: Appointment): Future[EventId] = {
      val mimePattern = "^data:([a-zA-Z0-9]+/[a-zA-Z0-9]+).*,.*".r
      val transaction = (
        for {
          eventId <- (events returning events.map(_.id)) += appointment.toEvent
          productsAdded <- this ++= appointment.products.map(product => product.image match {
            case Some(image) => Product(
              ProductId(0),
              product.quality,
              product.quantity,
              product.categoryId,
              eventId,
              image match {
                case mimePattern(mime) => Some(mime)
                case _ => None
              },
              Some(
                Base64.getDecoder().decode(
                  image.substring(image.indexOf(",") + 1)
                )
              ),
            )
            case None => Product(
              ProductId(0),
              product.quality,
              product.quantity,
              product.categoryId,
              eventId,
            )
          })
        } yield eventId
      ).transactionally

      db.run(transaction)
    }

    def getAllForEvent(id: EventId): Future[Seq[Product]] = db.run {
      this.filter(_.eventId === id).result
    }

    def getAllWithEventAndLocationAndCategory(): Future[Seq[(Product, Event, Location, Category)]] =
      db.run {
        this
          .join(events)
          .on(_.eventId === _.id)
          .join(locations)
          .on(_._2.locationId === _.id)
          .join(categories)
          .on(_._1._1.categoryId === _.id)
          .map({ case (((product, event), location), category) => (product, event, location, category)})
          .result
      }

    def getAllForEventWithEventAndLocationAndCategory(id: EventId): Future[Seq[(Product, Event, Location, Category)]] =
      db.run {
        this
          .join(events)
          .on(_.eventId === _.id)
          .join(locations)
          .on(_._2.locationId === _.id)
          .join(categories)
          .on(_._1._1.categoryId === _.id)
          .map({ case (((product, event), location), category) => (product, event, location, category)})
          .filter(_._1.eventId === id).result
      }

    def cancelEvent(id: EventId): Future[Int] = {
      val transaction = (
        for {
          productsDeleted <- this.filter(_.eventId === id).delete
          eventsDeleted <- events.filter(_.id === id).delete
        } yield eventsDeleted
      ).transactionally

      db.run(transaction)
    }
  }

}
