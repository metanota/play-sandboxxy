package dao

import dto.User

import scala.slick.driver.PostgresDriver.simple._

class UserDao {
  def create(token: String)(implicit session: Session) = {
    User.autoInc.insert(token)
  }

  def getById(id: Long)(implicit session: Session) = {
    (for {
      u <- User if u.id === id
    } yield u).firstOption
  }

  def getByToken(token: String)(implicit session: Session) = {
    (for {
      u <- User if u.token === token
    } yield u).firstOption
  }
}
