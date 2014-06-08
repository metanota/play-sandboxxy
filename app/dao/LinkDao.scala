package dao

import dto._

import scala.slick.driver.PostgresDriver.simple._

trait LinkDao { this: SessionProvider =>
  val links   = Links  .tableQuery
  val folders = Folders.tableQuery
  val users   = Users  .tableQuery

  def create(userId: Long, folderId: Option[Long], linkUrl: String, linkCode: String) = {
    (links returning links.map(_.id)) += Link(0, userId, folderId, linkUrl, linkCode)
  }

  def createAndGet(user: User, folderId: Option[Long], linkUrl: String, linkCode: String) = {
    (links returning links.map(_.id) into ((link, id) => link.copy(id = id))) +=
      Link(0, user.id, folderId, linkUrl, linkCode)
  }

  def getById(id: Int): Option[Link] = {
    links.where(_.id === id).firstOption
  }

  def getByToken(token: String): List[Link] = {
    (for {
      u <- users if u.token === token
      l <- links if l.userId === u.id
    } yield l).list
  }

  def getByCode(code: String): Option[Link] = {
    links.where(_.code === code).firstOption
  }

  def getBy(user: User, linkCode: Option[String], linkUrl: String, folderId: Option[Long]) : Option[Link] = {
    (for {
      l <- links   if l.code === linkCode && l.url === linkUrl && l.userId === user.id
      f <- folders if f.id === folderId && l.folderId === f.id && l.userId === user.id
    } yield l).firstOption
  }

  def getBy(userToken: String, folderId: Long): List[Link] = {
    (for {
      u <- users   if u.token === userToken
      f <- folders if f.id === folderId && f.userId === u.id
      l <- links   if l.userId === u.id && l.folderId === f.id
    } yield l).list
  }
}
