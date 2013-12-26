package dao

import dto._

import scala.slick.driver.PostgresDriver.simple._

trait LinkDao { this: SessionProvider =>
  def create(userId: Long, folderId: Option[Long], linkUrl: String, linkCode: String) = {
    val link = NewLink(None, userId, folderId, linkUrl, linkCode)
    Link.autoInc.insert(link)
  }

  def getById(id: Int): Option[Link] = {
    (for {
      l <- Link if l.id === id
    } yield l).firstOption
  }

  def getByToken(token: String): List[Link] = {
    (for {
      u <- User if u.token === token
      l <- Link if l.userId === u.id
    } yield l).list
  }

  def getByCode(code: String): Option[Link] = {
    (for {
      l <- Link if l.code === code
    } yield l).firstOption
  }

  def getBy(userToken: String, linkCode: Option[String], linkUrl: String, folderId: Option[Long]) : Option[Link] = {
    (for {
      u <- User   if u.token === userToken
      l <- Link   if l.code === linkCode && l.url === linkUrl && l.userId === u.id
      f <- Folder if f.id === folderId && l.folderId === f.id && l.userId === u.id
    } yield l).firstOption
  }

  def getBy(userToken: String, folderId: Long): List[Link] = {
    (for {
      u <- User   if u.token === userToken
      f <- Folder if f.id === folderId && f.userId === u.id
      l <- Link   if l.userId === u.id && l.folderId === f.id
    } yield l).list
  }
}
