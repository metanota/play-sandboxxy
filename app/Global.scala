import play.api.GlobalSettings
import play.api.db.DB
import scala.slick.driver.PostgresDriver.simple._
import Database.threadLocalSession

import play.api.Application
import play.api.Play.current

import dao._
import scala.slick.jdbc.meta.MTable

/**
 * Settings must be in root package.
 */
object Global extends GlobalSettings {

  override def onStart(app: Application) {
    lazy val database = Database.forDataSource(DB.getDataSource())

    // Probably there must be some version check
    database withSession {
      if (MTable.getTables("user_tbl").list().isEmpty)   User.ddl.create
      if (MTable.getTables("folder")  .list().isEmpty) Folder.ddl.create
      if (MTable.getTables("link")    .list().isEmpty)   Link.ddl.create
      if (MTable.getTables("click")   .list().isEmpty)  Click.ddl.create
    }
  }
}
