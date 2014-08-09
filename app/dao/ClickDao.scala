package dao

import dto._

import scala.slick.driver.PostgresDriver.simple._

import java.net.InetAddress
import java.sql.Timestamp

trait ClickDao { this: SessionProvider =>
  val clicks = Clicks.tableQuery
  val links  = Links .tableQuery
  val users  = Users .tableQuery

  def create(linkId: Int, dateTime: Timestamp, referer: String, ip: InetAddress, stats: Option[String]) = {
    (clicks returning clicks.map(_.id)) += Click(0, linkId, dateTime, referer, ip, stats)
  }

  def getByToken(userToken: String): List[Click] = {
    ( for {
      u <- users  if u.token === userToken
      l <- links  if l.userId === u.id
      c <- clicks if c.linkId === l.id
    } yield c).list
  }

  def countByLink(link: Link): Int = {
    clicks.filter(_.linkId === link.id).length.run
  }
}
