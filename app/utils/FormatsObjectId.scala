package utils
import play.api.data.FormError
import play.api.data.format.Formatter
import org.bson.types.ObjectId
import play.data.format.Formats
import play.api.data.format.Formats
import play.Logger

object FormatsObjectId extends Formats {

  implicit def objectIdFormat = new Formatter[ObjectId] {

    def bind(key: String, data: Map[String, String]) = {
      val error = FormError(key, "error.required.ObjectId", Nil)
      val s = Seq(error)
      val k = data.get(key)
      k.toRight(s).right.flatMap {
        case str: String if (str.length() > 0) => Right(new ObjectId(str))
        case _ => Left(s)
      }
    }

    def unbind(key: String, value: ObjectId) = Map(key -> value.toStringMongod())
  }
} 