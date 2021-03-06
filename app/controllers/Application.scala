package controllers

import play.api.db._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

import dao._
import dto._
import utils.{DummySecurity => S}

import java.net.InetAddress
import java.sql.Timestamp

import scala.util.Try

object Application extends Controller {

  val db = Database.forDataSource(DB.getDataSource())

  implicit val TwoStringsReader = (
    (__ \ 'token).read[String] and
    (__ \ 'url)  .read[String]
  ).tupled

  def token(user_id: Long, secret: String) = Action {
    if (!S.authUser(secret)) {
      Unauthorized(Json.toJson("User's secret doesn't fit :)"))
    } else {
      db withDynTransaction {
        val user = UserDao.getById(user_id)
        val token = user map ( _.token ) getOrElse {
          val token = S.generateToken
          UserDao.create(token)
          token
        }
        Ok(Json.toJson(token))
      }
    }
  }

  def postLink = Action(parse.json) { request =>
    request.body.validate[(String, String)].map { case (token, url) =>
      val code     = (request.body \ "code")     .asOpt[String]
      val folderId = (request.body \ "folder_id").asOpt[Long]

      db withDynTransaction {
        UserDao.getByToken(token) match {
          case None       => Unauthorized(Json.toJson(s"No user matches token $token"))
          case Some(user) =>
            folderId match {
              case None              => createLink(user, code, url, None)
              case Some(reqFolderId) =>
                FolderDao.getById(reqFolderId) match {
                  case None    => PreconditionFailed(Json.toJson(s"Folder with id $reqFolderId doesn't exist"))
                  case Some(f) => createLink(user, code, url, folderId)
                }
            }
        }
      }
    } recoverTotal {
      e => UnprocessableEntity("Detected error:" + JsError.toFlatJson(e))
    }
  }

  private [this] def createLink(user: User, code: Option[String], url: String, folder: Option[Long]): Result = {
    val link = LinkDao.getBy(user, code, url, folder) getOrElse {
      LinkDao.createAndGet(user, folder, url, code getOrElse S.generateCode)
    }
    Created(Json.obj("url" -> link.url, "code" -> link.code))
  }

  def postCode(code: String) = Action(parse.json) { request =>
    request.body.validate[(String, String)].map { case (referer, remoteIp) =>
      val stats = (request.body \ "stats").asOpt[String]

      // add error check
      val ip = Try {
        InetAddress.getByName(remoteIp)
      }
      if (ip.isFailure) {
        PreconditionFailed(Json.toJson(s"Ip address $remoteIp is not valid"))
      } else {
        db withDynTransaction {
          val link = LinkDao.getByCode(code)
          link map { l =>
            ClickDao.create(l.id, new Timestamp(System.currentTimeMillis()), referer, ip.get, stats)
            Created(Json.toJson(l.url))
          } getOrElse {
            PreconditionFailed(Json.toJson(s"Link $code not found"))
          }
        }
      }
    } recoverTotal {
      e => UnprocessableEntity("Detected error:" + JsError.toFlatJson(e))
    }
  }

  def getCode(code: String, token: String) = Action {
    db withDynTransaction {
      val link   = LinkDao getByCode code
      val folder = link flatMap (l => FolderDao getByLink   l)
      val clicks = link.fold(0) (l => ClickDao  countByLink l).toString

      val folderId = if (folder.isDefined) folder.get.id.toString else ""

      val json = Json.toJson (
        link map { l =>
          Json.obj (
            "link"            -> Json.obj( "url" -> l.url, "code" -> l.code ),
            "folder_id"       -> folderId,
            "count_of_clicks" -> clicks
          )
        }
      )
      Ok(json)
    }
  }

  def getFolderId(id: Long, token: String, offset: Option[Int], limit: Option[String]) = Action {
    db withDynTransaction {
      val links = LinkDao.getBy(token, id)

      val dropped = links drop (offset getOrElse 0)
      val limited = Try(Integer parseInt (limit getOrElse "")) map dropped.take getOrElse dropped

      Ok(links2json(limited))
    }
  }

  def getLink(token: String, offset: Option[Int], limit: Option[String]) = Action {
    db withDynTransaction {
      val links = LinkDao.getByToken(token)

      val dropped = links drop (offset getOrElse 0)
      val limited = Try(Integer parseInt (limit getOrElse "")) map dropped.take getOrElse dropped

      Ok (links2json(limited))
    }
  }

  def getFolder(token: String) = Action {
    db withDynTransaction {
      val folders = FolderDao.getByToken(token)

      val json = Json.toJson (
        folders map { f: Folder =>
          Json.obj (
            "id"    -> f.id,
            "title" -> f.title
          )
        }
      )
      Ok (json)
    }
  }

  def getClicks(code: String, token: String, offset: Int, limit: String) = Action {
    db withDynTransaction {
      val clicks = ClickDao.getByToken(token)

      val dropped = clicks drop offset
      val limited = Try (Integer parseInt limit) map dropped.take getOrElse dropped

      val json = Json.toJson (
        limited map { c: Click =>
          Json.obj (
            "date"      -> c.date,
            "referer"   -> c.referer,
            "remote_ip" -> c.remoteIp.toString
          )
        }
      )
      Ok(json)
    }
  }

  private [this] def links2json(links: List[Link]) = Json.toJson (
    links map { l: Link =>
      Json.obj (
        "url"  -> l.url,
        "code" -> l.code
      )
    }
  )
}
