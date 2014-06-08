package dto

import java.net.InetAddress

import scala.slick.driver.PostgresDriver.simple._

package object ImpicitConversions {
  implicit val InetAddressMapper =
    MappedColumnType.base[InetAddress, String](
      i => i.toString,
      s => InetAddress.getByName(s))
}
