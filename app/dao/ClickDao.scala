package dao

import dto._

import scala.slick.driver.PostgresDriver.simple._

import java.net.InetAddress
import java.sql.Timestamp

trait ClickDao { this: SessionProvider =>
  def create(linkId: Int, dateTime: Timestamp, referer: String, ip: InetAddress, stats: Option[String]) = {
    val click = NewClick(None, linkId, dateTime, referer, ip, stats)
    Click.autoInc.insert(click)
  }

  def getByToken(userToken: String): List[Click] = {
    ( for {
      u <- User  if u.token === userToken
      l <- Link  if l.userId === u.id
      c <- Click if c.linkId === l.id
    } yield c).list
  }

  def countByLink(link: Link): Int = {
    ( for {
      c <- Click if c.linkId === link.id
    } yield c.length).first
  }
}

