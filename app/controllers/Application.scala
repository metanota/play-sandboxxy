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
      db withTransaction {
        val user = UserDao.getById(user_id)
        val token = user.fold {
          val token = S.generateToken
          UserDao.create(token)
          token
        }(_.token)
        Ok(Json.toJson(token))
      }
    }
  }

  def postLink = Action(parse.json) { request =>
    request.body.validate[(String, String)].map { case (token, url) =>
      val code     = (request.body \ "code")     .asOpt[String]
      val folderId = (request.body \ "folder_id").asOpt[Long]

      db withTransaction {
        UserDao.getByToken(token) match {
          case None    => Unauthorized(s"No user matches token $token")
          case Some(u) => {
            val folder = for {
              fid <- folderId
              f   <- FolderDao.getById(fid)
            } yield f
            folder match {
              case None => PreconditionFailed(Json.toJson(s"Folder with id $folderId doesn't exist"))
              case Some(f) => {
                val linkOpt = LinkDao.getBy(token, code, url, folderId)
                val link = linkOpt getOrElse {
                  val newLinkId = LinkDao.create(u.id, folderId, url, code getOrElse S.generateCode)
                  LinkDao.getById(newLinkId).get
                }
                Created(Json.obj("url" -> link.url, "code" -> link.code))
              }
            }
          }
        }
      }
    } recoverTotal {
      e => UnprocessableEntity("Detected error:" + JsError.toFlatJson(e))
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
        PreconditionFailed(Json.toJson(s"Ip address $remoteIp is not valid"))
      } else {
        db withTransaction {
          val link = LinkDao.getByCode(code)
          link.fold{
            PreconditionFailed(Json.toJson(s"Link $code not found"))
          }{ l =>
            ClickDao.create(l.id, new Timestamp(System.currentTimeMillis()), referer, ip.get, stats)
            Created(Json.toJson(l.url))
          }
        }
      }
    } recoverTotal {
      e => UnprocessableEntity("Detected error:" + JsError.toFlatJson(e))
    }
  }

  def getCode(code: String, token: String) = Action {
    db withTransaction {
      val link   = LinkDao getByCode code
      val folder = link flatMap (l => FolderDao getByLink   l)
      val clicks = link.fold(0) (l => ClickDao  countByLink l)

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
    db withTransaction {
      val links = LinkDao.getBy(token, id)

      val dropped = links drop (offset getOrElse 0)
      val limited = Try(Integer parseInt (limit getOrElse "")) map dropped.take getOrElse dropped

      Ok(links2json(limited))
    }
  }

  def getLink(token: String, offset: Option[Int], limit: Option[String]) = Action {
    db withTransaction {
      val links = LinkDao.getByToken(token)

      val dropped = links drop (offset getOrElse 0)
      val limited = Try(Integer parseInt (limit getOrElse "")) map dropped.take getOrElse dropped

      Ok (links2json(limited))
    }
  }

  def getFolder(token: String) = Action {
    db withTransaction {
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
    db withTransaction {
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
