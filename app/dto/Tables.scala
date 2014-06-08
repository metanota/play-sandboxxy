package dto

import scala.slick.driver.PostgresDriver.simple._

import java.net.InetAddress
import java.sql.Timestamp

case class User(id: Long, token: String)
class Users(tag: Tag) extends Table[User](tag, "user_tbl") {
  def id      = column[Long]  ("id", O.PrimaryKey, O.AutoInc)
  def token   = column[String]("name")
  def *       = (id, token) <> (User.tupled, User.unapply)
}
object Users {
  val tableQuery = TableQuery[Users]
}

case class Folder(id: Long, userId: Long, title: String)
class Folders(tag: Tag) extends Table[Folder](tag, "folder") {
  def id     = column[Long]  ("id", O.PrimaryKey)
  def userId = column[Long]  ("user_id")
  def title  = column[String]("title")
  def *      = (id, userId, title) <> (Folder.tupled, Folder.unapply)
  def user   = foreignKey("user_id", userId, Users.tableQuery)(_.id)
}
object Folders {
  val tableQuery = TableQuery[Folders]
}

case class Link(id: Int, userId: Long, folderId: Option[Long], url: String, code: String)
class Links(tag: Tag) extends Table[Link](tag, "link") {
  def id       = column[Int]         ("id", O.PrimaryKey, O.AutoInc)
  def userId   = column[Long]        ("user_id")
  def folderId = column[Option[Long]]("folder_id", O.Nullable)
  def url      = column[String]      ("url")
  def code     = column[String]      ("code")
  def *        = (id, userId, folderId, url, code) <> (Link.tupled, Link.unapply)
  def user     = foreignKey("user_id",   userId,   Users.tableQuery)  (_.id)
  def folder   = foreignKey("folder_id", folderId, Folders.tableQuery)(_.id)
}
object Links {
  val tableQuery = TableQuery[Links]
}

case class Click (id: Int, linkId: Int, date: Timestamp, referer: String, remoteIp: InetAddress, stats: Option[String])
class Clicks(tag: Tag) extends Table[Click](tag, "click") {
  import ImpicitConversions._
  def id       = column[Int]           ("id", O.PrimaryKey, O.AutoInc)
  def linkId   = column[Int]           ("link_id")
  def date     = column[Timestamp]     ("date")
  def referer  = column[String]        ("referer")
  def remoteIp = column[InetAddress]   ("remote_ip")
  def stats    = column[Option[String]]("stats")
  def *        = (id, linkId, date, referer, remoteIp, stats) <> (Click.tupled, Click.unapply)
  def link     = foreignKey("link_id", linkId, Links.tableQuery)(_.id)
}
object Clicks {
  val tableQuery = TableQuery[Clicks]
}
