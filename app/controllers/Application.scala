package controllers

import play.api._
import play.api.db._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current

import scala.slick.driver.PostgresDriver.simple._
import Database.threadLocalSession

import dto._
import utils.{Security => S}
import scala.util.Try

import java.sql.Timestamp
import java.net.InetAddress

object Application extends Controller {

  lazy val db = Database.forDataSource(DB.getDataSource())

  implicit val TwoStringsReader = (
    (__ \ 'token).read[String] and
    (__ \ 'url)  .read[String]
  ).tupled

  def token(user_id: Long, secret: String) = Action {
    if (!S.authUser(secret)) {
      BadRequest(Json.toJson("User's secret doesn't fit :)"))
    }

    db withSession {
      val user = ( for {
        u <- User if u.id === user_id
      } yield u.token).firstOption

      val token = user getOrElse {
        val token = S.generateToken
        User.autoInc.insert(token)
        token
      }

      Ok(Json.toJson(token))
    }
  }

  // greatly overloaded
  def postLink = Action(parse.json) { request =>
    request.body.validate[(String, String)].map { case (token, url) =>
      val code     = (request.body \ "code")     .asOpt[String]
      val folderId = (request.body \ "folder_id").asOpt[Long]
      db withSession {
        val user = (for {
          u <- User if u.token === token
        } yield u).firstOption

        user match {
          case None => Ok (Json.arr())
          case Some(u) => {
            val linkOpt = (for {
              u <- User   if u.token === token
              l <- Link   if l.code === code && l.url === url && l.userId === u.id
              f <- Folder if f.id === folderId && l.folderId === f.id && l.userId === u.id
            } yield l).firstOption

            val link = linkOpt getOrElse {
              val link = NewLink(None, u.id, folderId, url, code getOrElse S.generateCode)
              val newId = Link.autoInc.insert(link)
              (for (l <- Link if l.id === newId) yield l).first
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
      val ip = Try {
        InetAddress.getByName(remoteIp)
      }
      if (ip.isFailure) {
        BadRequest(Json.toJson(s"Ip address $remoteIp is not valid"))
      } else {
        db withSession {
          val link = (for {
            l <- Link if l.code === code
          } yield l).firstOption

          link map { l =>
            Click.autoInc.insert(NewClick(
              None, l.id, new Timestamp(System.currentTimeMillis()), referer, ip.get, stats))
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
    db withSession {
      val link = (for {
        u <- User   if u.token === token
        l <- Link   if l.userId === u.id
        f <- Folder if l.folderId == f.id && l.userId == u.id
        c <- Click  if c.linkId == l.id
      } yield (l, f.id, c.length)).list()

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
    db withSession {
      val links = (for {
        u <- User   if u.token === token
        f <- Folder if f.id === id && f.userId === u.id
        l <- Link   if l.userId === u.id && l.folderId === f.id
      } yield l).list

      val dropped = links drop (offset getOrElse 0)
      val limited = Try(Integer parseInt (limit getOrElse "")) map (dropped.take) getOrElse dropped

      Ok(links2json(limited))
    }
  }

  def getLink(token: String, offset: Option[Int], limit: Option[String]) = Action {
    db withSession {
      val links = (for {
        u <- User if u.token === token
        l <- Link if l.userId === u.id
      } yield l).list

      val dropped = links drop (offset getOrElse 0)
      val limited = Try(Integer parseInt (limit getOrElse "")) map (dropped.take) getOrElse dropped

      Ok (links2json(limited))
    }
  }

  def getFolder(token: String) = Action {
    db withSession {
      val folders = (for {
        u <- User   if u.token === token
        f <- Folder if f.userId === u.id
      } yield f).list

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
    db withSession {
      val clicks = ( for {
        u <- User  if u.token === token
        l <- Link  if l.userId === u.id
        c <- Click if c.linkId === l.id
      } yield c).list

      val dropped = clicks drop offset
      val limited = Try (Integer parseInt limit) map (dropped.take) getOrElse dropped

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
