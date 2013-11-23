package dto

import scala.slick.lifted.TypeMapper
import java.net.InetAddress
import scala.slick.lifted.MappedTypeMapper._

package object ImpicitConversions {
  implicit val InetAddressMapper: TypeMapper[InetAddress] =
    base[InetAddress, String](
      i => new String(i.toString),
      s => InetAddress.getByName(s))
}
