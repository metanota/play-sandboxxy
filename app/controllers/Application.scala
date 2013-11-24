package controllers

import play.api.db._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current

import scala.slick.driver.PostgresDriver.simple._
import Database.threadLocalSession

import dao._
import dto._
import utils.{Security => S}
import scala.util.Try

import java.net.InetAddress
import java.sql.Timestamp

object Application extends Controller {

  val db = Database.forDataSource(DB.getDataSource())

  val clickDao  = new ClickDao
  val folderDao = new FolderDao
  val linkDao   = new LinkDao
  val userDao   = new UserDao

  implicit val TwoStringsReader = (
    (__ \ 'token).read[String] and
    (__ \ 'url)  .read[String]
  ).tupled

  def token(user_id: Long, secret: String) = Action {
    if (!S.authUser(secret)) {
      BadRequest(Json.toJson("User's secret doesn't fit :)"))
    }
    db withTransaction {
      val user = userDao.getById(user_id)
      val token = user map (_.token) getOrElse {
        val token = S.generateToken
        userDao.create(token)
        token
      }
      Ok(Json.toJson(token))
    }
  }

  def postLink = Action(parse.json) { request =>
    request.body.validate[(String, String)].map { case (token, url) =>
      val code     = (request.body \ "code")     .asOpt[String]
      val folderId = (request.body \ "folder_id").asOpt[Long]

      db withTransaction {
        val user = userDao.getByToken(token)
        user match {
          case None => Ok (Json.arr())
          case Some(u) => {
            val linkOpt = linkDao.getBy(token, code, url, folderId)
            val link = linkOpt getOrElse {
              val newLinkId = linkDao.create(u.id, folderId, url, code getOrElse S.generateCode)
              linkDao.getById(newLinkId).get
            }
            Ok (Json.obj ("url" -> link.url, "code" -> link.code))
          }
        }
      }
    } recoverTotal {
      e => BadRequest("Detected error:" + JsError.toFlatJson(e))
    }
  }

  def postCode(code: String) = Action(parse.json) { request =>
    request.body.validate[(String, String)].map { case (referer, remoteIp) =>
      val stats = (request.body \ "stats").asOpt[String]

      // add error check
      val ip = Try {
        InetAddress.getByName(remoteIp)
      }
      if (ip.isFailure) {
        BadRequest(Json.toJson(s"Ip address $remoteIp is not valid"))
      } else {
        db withTransaction {
          val link = linkDao.getByCode(code)
          link map { l =>
            clickDao.create(l.id, new Timestamp(System.currentTimeMillis()), referer, ip.get, stats)
            Ok(Json.toJson(l.url))
          } getOrElse {
            BadRequest(Json.toJson(s"Link $code not found"))
          }
        }
      }
    } recoverTotal {
      e => BadRequest("Detected error:" + JsError.toFlatJson(e))
    }
  }

  def getCode(code: String, token: String) = Action {
    db withTransaction {
      val link = linkDao.getByTokenWithExtra(token)

      val json = Json.toJson (
        link map { case (l: Link, folderId: Long, count: Int) =>
          Json.obj (
            "link"            -> Json.obj( "url" -> l.url, "code" -> l.code ),
            "folder_id"       -> folderId,
            "count_of_clicks" -> count
          )
        }
      )
      Ok(json)
    }
  }

  def getFolderId(id: Long, token: String, offset: Option[Int], limit: Option[String]) = Action {
    db withTransaction {
      val links = linkDao.getBy(token, id)

      val dropped = links drop (offset getOrElse 0)
      val limited = Try(Integer parseInt (limit getOrElse "")) map dropped.take getOrElse dropped

      Ok(links2json(limited))
    }
  }

  def getLink(token: String, offset: Option[Int], limit: Option[String]) = Action {
    db withTransaction {
      val links = linkDao.getByToken(token)

      val dropped = links drop (offset getOrElse 0)
      val limited = Try(Integer parseInt (limit getOrElse "")) map dropped.take getOrElse dropped

      Ok (links2json(limited))
    }
  }

  def getFolder(token: String) = Action {
    db withTransaction {
      val folders = folderDao.getByToken(token)

      val json = Json.toJson (
        folders map { f: Folder =>
          Json.obj(
            "id"    -> f.id,
            "title" -> f.title
          )
        }
      )
      Ok (json)
    }
  }

  def getClicks(code: String, token: String, offset: Int, limit: String) = Action {
    db withTransaction {
      val clicks = clickDao.getByToken(token)

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

  def links2json(links: List[Link]) = Json.toJson (
    links map { l: Link =>
      Json.obj (
        "url"  -> l.url,
        "code" -> l.code
      )
    }
  )
}
