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

  def getById(id: Long)(implicit session: Session) = {
    (for {
      f <- Folder if f.id === id
    } yield f).firstOption
  }

  def getByLink(link: Link)(implicit session: Session) = {
    (for {
      f <- Folder if f.id === link.folderId
    } yield f).firstOption
  }
}

