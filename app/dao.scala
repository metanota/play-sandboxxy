import scala.slick.driver.PostgresDriver.simple._

/**
 * Thanks to <a href="https://github.com/freekh/play-slick/wiki/ScalaSlickTables">Freekh</a>
 */
package object dao {
  def ClickDao (implicit session: Session) = new SessionProvider with ClickDao  { override val implicitSession = session }
  def FolderDao(implicit session: Session) = new SessionProvider with FolderDao { override val implicitSession = session }
  def LinkDao  (implicit session: Session) = new SessionProvider with LinkDao   { override val implicitSession = session }
  def UserDao  (implicit session: Session) = new SessionProvider with UserDao   { override val implicitSession = session }
}
