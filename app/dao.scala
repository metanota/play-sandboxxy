import scala.slick.driver.PostgresDriver.simple._

/**
 * Thanks to <a href="https://github.com/freekh/play-slick/wiki/ScalaSlickTables">Freekh</a>
 *
 * "Expression problem" fix is: in real world we will add DAO's less frequently than their methods.
 * So let's write more "session" words here than spamming up all the realizations.
 *
 * Extra problem is: creating new object on every DB call.
 * This may become too expensive for GC in highload apps.
 *
 * Need to think and measure it later.
 */
package object dao {
  trait SessionProvider {
    implicit val implicitSession: Session
  }

  def ClickDao (implicit session: Session) = new SessionProvider with ClickDao  { override val implicitSession = session }
  def FolderDao(implicit session: Session) = new SessionProvider with FolderDao { override val implicitSession = session }
  def LinkDao  (implicit session: Session) = new SessionProvider with LinkDao   { override val implicitSession = session }
  def UserDao  (implicit session: Session) = new SessionProvider with UserDao   { override val implicitSession = session }
}
