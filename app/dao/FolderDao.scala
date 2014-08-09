package dao

import dto._

import scala.slick.driver.PostgresDriver.simple._

trait FolderDao { this: SessionProvider =>
  val folder = Folders.tableQuery
  val user   = Users  .tableQuery

  // TODO def create

  def getByToken(token: String): List[Folder] = {
    (for {
      u <- user   if u.token === token
      f <- folder if f.userId === u.id
    } yield f).list
  }

  def getById(id: Long): Option[Folder] = {
    folder.filter(_.id === id).firstOption
  }

  def getByLink(link: Link): Option[Folder] = {
    folder.filter(_.id === link.folderId).firstOption
  }
}
