package utils

import java.security.SecureRandom
import java.math.BigInteger

object DummySecurity {

  private lazy val random = new SecureRandom()

  val hardcoded_secret = "123QWE456ert7y"

  def authUser(secret: String) = hardcoded_secret == secret

  def generateToken = new BigInteger(130, random).toString(32)

  def generateCode = new BigInteger(50, random).toString(32)
}
