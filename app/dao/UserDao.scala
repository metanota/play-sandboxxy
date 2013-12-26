package dao

import dto.User

import scala.slick.driver.PostgresDriver.simple._

trait UserDao { this: SessionProvider =>
  def create(token: String) = {
    User.autoInc.insert(token)
  }

  def getById(id: Long): Option[User] = {
    (for {
      u <- User if u.id === id
    } yield u).firstOption
  }

  def getByToken(token: String): Option[User] = {
    (for {
      u <- User if u.token === token
    } yield u).firstOption
  }
}
