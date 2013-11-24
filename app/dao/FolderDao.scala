package dao

import dto._

import scala.slick.driver.PostgresDriver.simple._

class FolderDao {
  // TODO def create

  def getByToken(token: String)(implicit session: Session) = {
    (for {
      u <- User   if u.token === token
      f <- Folder if f.userId === u.id
    } yield f).list
  }
}

