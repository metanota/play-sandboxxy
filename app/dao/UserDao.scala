package dao

import dto._

import scala.slick.driver.PostgresDriver.simple._

trait UserDao { this: SessionProvider =>
  val users = Users.tableQuery

  def create(token: String) = {
    (users returning users.map(_.id)) += User(0, token)
  }

  def getById(id: Long): Option[User] = {
    users.where(_.id === id).firstOption
  }

  def getByToken(token: String): Option[User] = {
    users.where(_.token === token).firstOption
  }
}
