package dao

import dto._

import scala.slick.driver.PostgresDriver.simple._

trait FolderDao { this: SessionProvider =>
  // TODO def create

  def getByToken(token: String): List[Folder] = {
    (for {
      u <- User   if u.token === token
      f <- Folder if f.userId === u.id
    } yield f).list
  }

  def getById(id: Long): Option[Folder] = {
    (for {
      f <- Folder if f.id === id
    } yield f).firstOption
  }

  def getByLink(link: Link): Option[Folder] = {
    (for {
      f <- Folder if f.id === link.folderId
    } yield f).firstOption
  }
}

