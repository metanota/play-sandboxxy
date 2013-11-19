package dao

import scala.slick.driver.PostgresDriver.simple._

import java.net.InetAddress
import java.sql.Timestamp

case class User(id: Long, token: String)
object User extends Table[User]("user_tbl") {
  def id    = column[Long]  ("id", O.PrimaryKey, O.AutoInc)
  def token = column[String]("name")
  def *       = id ~ token <> (User.apply _, User.unapply _)
  def autoInc =      token returning id
}

case class Folder(id: Long, userId: Long, title: String)
object Folder extends Table[Folder]("folder") {
  def id     = column[Long]  ("id", O.PrimaryKey)
  def userId = column[Long]  ("user_id")
  def title  = column[String]("title")
  def * = id ~ userId ~ title <> (Folder.apply _, Folder.unapply _)
  def user = foreignKey("user_id", userId, User)(_.id)
}

case class Link(   id: Int,         userId: Long, folderId: Option[Long], url: String, code: String)
case class NewLink(id: Option[Int], userId: Long, folderId: Option[Long], url: String, code: String)
object Link extends Table[Link]("link") {
  def id       = column[Int]         ("id", O.PrimaryKey, O.AutoInc)
  def userId   = column[Long]        ("user_id")
  def folderId = column[Option[Long]]("folder_id", O.Nullable)
  def url      = column[String]      ("url")
  def code     = column[String]      ("code")
  def * = id ~ userId ~ folderId ~ url ~ code <> (Link.apply _, Link.unapply _)
  def autoInc = (userId ~ folderId ~ url ~ code) <> (
    {(userId, folderId, url, code) => NewLink(None, userId, folderId, url, code)},
    {(link: NewLink) => Some(link.userId, link.folderId, link.url, link.code)}
    )
  def user   = foreignKey("user_id",   userId,   User)  (_.id)
  def folder = foreignKey("folder_id", folderId, Folder)(_.id)
}

case class Click   (id: Int,         linkId: Int, date: Timestamp, referer: String, remoteIp: InetAddress, stats: Option[String])
case class NewClick(id: Option[Int], linkId: Int, date: Timestamp, referer: String, remoteIp: InetAddress, stats: Option[String])
object Click extends Table[Click]("click") {
  import ImpicitConversions._
  def id       = column[Int]           ("id", O.PrimaryKey, O.AutoInc)
  def linkId   = column[Int]           ("link_id")
  def date     = column[Timestamp]     ("date")
  def referer  = column[String]        ("referer")
  def remoteIp = column[InetAddress]   ("remote_ip")
  def stats    = column[Option[String]]("stats")
  def * = id ~ linkId ~ date ~ referer ~ remoteIp ~ stats <> (Click.apply _, Click.unapply _)
  def autoInc = (linkId ~ date ~ referer ~ remoteIp ~ stats) <> (
    {(linkId, date, referer, remoteIp, stats) => NewClick(None, linkId, date, referer, remoteIp, stats)},
    {(click: NewClick) => Some(click.linkId, click.date, click.referer, click.remoteIp, click.stats)}
  )
  def link = foreignKey("link_id", linkId, Link)(_.id)
}
