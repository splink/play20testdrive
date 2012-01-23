package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.format.Formats._
import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._

//TODO insertOrUpdate
//TODO Form(of(Address.apply was macht das?
//TODO how does the id thing work
//TODO why arent embedded documents deserialized to case classes 
//TODO abstract sealed class Profile: why differentiate between Sponsor & OrganizerProfile
//TODO seq in case class constructor: How to define userForm
object Application extends Controller {

  val userForm = Form(
    of(User.apply _, User.unapply _)(
      "name" -> text,
      "pwd" -> text,
      "email" -> email,
      "address" -> of(Address.apply _, Address.unapply _)(
        "street" -> text,
        "city" -> text)))

  def index = Action {
    val users = UserDAO.find(MongoDBObject.empty).toSeq
    Logger.info("---------" + users.length + " users")

    if (users.length < 1) {
      UserDAO.insert(User("Anna", "secret", "doktorkoehler@googlemail.com", Address("Domstr.77", "Koeln")))
      UserDAO.insert(User("Max", "secret", "maxkugland@gmail.com", Address("Domstr.77", "Koeln")))
      Logger.info("insert 2 users")
    }

    Ok(views.html.index(users))
  }

  def editUser(email: String) = Action {
    val user = UserDAO.findByEmail(email).getOrElse(User("", "", "", Address("", "")))
    val filledForm = userForm.fill(user)
    Ok(views.html.edit(filledForm))
  }

  def newUser() = Action {
    Ok(views.html.edit(userForm))
  }

  def saveUser() = Action {
    implicit request =>
      userForm.bindFromRequest.fold(
        formWithErrors => Ok(views.html.edit(formWithErrors)),
        user =>
          if (UserDAO.findByEmail(user.email).isDefined)
            // instead of grater this can be used to update specific fields: MongoDBObject("name" -> user.name, "pwd" -> user.pwd),
            UserDAO.update(MongoDBObject("email" -> user.email), grater[User].asDBObject(user), upsert = false, multi = false)

          else
            UserDAO.save(user))

      Redirect(routes.Application.index)
  }

  def removeUser(email: String) = Action {
    UserDAO.findByEmail(email) match {
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

case class User(name: String, pwd: String, email: String, address: Address/*, hobbies : Seq[Hobby] = List[Hobby]()*/)
case class Address(street: String, city: String)
case class Hobby(title : String)

object UserDAO extends SalatDAO[User, ObjectId](collection = DB.connection("users")) {
  def findByEmail(email: String) = findOne(MongoDBObject("email" -> email))
}