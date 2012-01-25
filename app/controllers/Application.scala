package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import utils.FormatsObjectId._
import validation.Constraints._

object Application extends Controller {
  val userForm = Form(
    mapping(
      "_id" -> of[ObjectId],
      "name" -> text,
      "pwd" -> text,
      "email" -> email,
      "address" -> mapping(
        "street" -> text,
        "city" -> text)(Address.apply)(Address.unapply))(User.apply)(User.unapply))

  def index = Action {
    val users = UserDAO.find(MongoDBObject.empty).toSeq
    Logger.info("---------" + users.length + " users")

    Ok(views.html.index(users))
  }

  def editUser(id: String) = Action {
    Logger.info("editUser: " + id)

    UserDAO.findOneByID(new ObjectId(id)).map {
      user => Ok(views.html.edit(userForm.fill(user)))
    }.getOrElse(NotFound("No user for id: " + id))
  }

  def newUser() = Action {
    val filledUserform = userForm.fill(User(new ObjectId(), "", "", "", Address("", "")))
    Ok(views.html.edit(filledUserform))
  }

  def saveUser() = Action {
    implicit request =>

      userForm.bindFromRequest.fold(
        formWithErrors => {
          Logger.info("saving user with id: " + userForm.data.contains("_id"))
          Logger.info("errors! " + formWithErrors.errors)
          Ok(views.html.edit(formWithErrors))
        },
        user => {
          //          instead of grater this can be used to update specific fields: MongoDBObject("name" -> user.name, "pwd" -> user.pwd),
          UserDAO.update(MongoDBObject("_id" -> user._id), grater[User].asDBObject(user), upsert = true, multi = false)
          Redirect(routes.Application.index)
        })
  }

  def removeUser(id: String) = Action {
    UserDAO.findOneByID(new ObjectId(id)) match {
      case Some(user) =>
        UserDAO.remove(user)
      case None =>
    }
    Redirect(routes.Application.index)
  }
}

object DB {
  lazy val connection = {
    import com.mongodb.casbah.commons.Imports._
    import com.mongodb.casbah.MongoConnection
    import play.api.Play
    import play.api.Play.current
    val conn = MongoConnection(Play.configuration.getString("mongodb.url").get, Play.configuration.getInt("mongodb.port").get)(Play.configuration.getString("mongodb.dbname").get)
    //    conn.authenticate(Play.configuration.getString("mongodb.user").get, Play.configuration.getString("mongodb.password").get)
    conn
  }
}

case class User(_id: ObjectId = new ObjectId, name: String, pwd: String, email: String, address: Address /*, hobbies : Seq[Hobby] = Nil*/ )
case class Address(street: String, city: String)
case class Hobby(title: String)

object UserDAO extends SalatDAO[User, ObjectId](collection = DB.connection("users")) {
  def findByEmail(email: String) = findOne(MongoDBObject("email" -> email))
}