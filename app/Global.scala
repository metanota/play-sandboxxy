import dto._

import play.api.Application
import play.api.GlobalSettings
import play.api.db.DB
import play.api.Play.current

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.meta.MTable

/**
 * Settings must be in root package.
 */
object Global extends GlobalSettings {

  override def onStart(app: Application) {
    lazy val database = Database.forDataSource(DB.getDataSource())

    // Probably there must be some version check
    database withDynSession {
      if (MTable.getTables("user_tbl").list().isEmpty)   Users.tableQuery.ddl.create
      if (MTable.getTables("folder")  .list().isEmpty) Folders.tableQuery.ddl.create
      if (MTable.getTables("link")    .list().isEmpty)   Links.tableQuery.ddl.create
      if (MTable.getTables("click")   .list().isEmpty)  Clicks.tableQuery.ddl.create
    }
  }
}
