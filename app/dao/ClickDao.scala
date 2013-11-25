package dao

import dto._

import scala.slick.driver.PostgresDriver.simple._

import java.net.InetAddress
import java.sql.Timestamp

class ClickDao {
  def create(linkId: Int, dateTime: Timestamp, referer: String, ip: InetAddress, stats: Option[String])
            (implicit session: Session) = {
    val click = NewClick(None, linkId, dateTime, referer, ip, stats)
    Click.autoInc.insert(click)
  }

  def getByToken(userToken: String)(implicit session: Session) = {
    ( for {
      u <- User  if u.token === userToken
      l <- Link  if l.userId === u.id
      c <- Click if c.linkId === l.id
    } yield c).list
  }

  def countByLink(link: Link)(implicit session: Session) = {
    ( for {
      c <- Click if c.linkId === link.id
    } yield c.length).first
  }
}

