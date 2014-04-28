package com.oohish.chain

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.oohish.peermessages.Block
import com.oohish.structures.char32

import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.toInvariantFunctorOps
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Writes
import play.api.libs.json.__
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.MongoConnection
import reactivemongo.api.MongoDriver

class MongoBlockStore(
  conn: Option[MongoConnection]) extends BlockStore {

  val connection: MongoConnection = conn.getOrElse {
    // gets an instance of the driver
    // (creates an actor system)
    val driver = new MongoDriver
    driver.connection(List("localhost"))
  }

  // Gets a reference to the database "plugin"
  val db = connection("plugin")

  // Gets a reference to the collection "blocks"
  // By default, you get a BSONCollection.
  def collection: JSONCollection = db.collection[JSONCollection]("blocks")

  // Gets a reference to the collection "chainHead"
  // By default, you get a BSONCollection.
  def chainHeadCollection: JSONCollection = db.collection[JSONCollection]("chainHead")

  def put(block: StoredBlock): Future[Unit] = {
    import JsonFormats.storedBlockWrites
    val writes = storedBlockWrites

    for {
      a <- collection.insert(block)
    } yield ()
  }

  def get(hash: char32): Future[Option[StoredBlock]] = {
    import play.api.libs.json._

    import JsonFormats.storedBlockReads
    val reads = storedBlockReads

    val h = JsonFormats.char32Format.writes(hash)
    collection.find(Json.obj("_id" -> h)).one
  }

  def getChainHead(): Future[Option[StoredBlock]] = {
    import JsonFormats.storedBlockReads
    val reads = storedBlockReads

    for {
      b <- chainHeadCollection.find(Json.obj()).one
    } yield {
      println("got chain head: " + b)
      b
    }
  }

  def setChainHead(cHead: StoredBlock): Future[Unit] = {
    import JsonFormats.storedBlockWrites
    val writes = storedBlockWrites

    for {
      a <- chainHeadCollection.remove(Json.obj())
      b <- chainHeadCollection.insert(cHead)
    } yield {
      println("set chain head: " + cHead)
      Unit
    }
  }

}

object JsonFormats {
  import play.api.libs.json._
  import play.api.data._
  import play.api.data.Forms._
  import play.api.libs.functional.syntax._

  import com.oohish.peermessages._
  import com.oohish.structures._

  // JSON formats
  implicit val uint32_tFormat = Json.format[uint32_t]
  implicit val char32Format = Json.format[char32]
  implicit val outPointFormat = Json.format[OutPoint]
  implicit val txInFormat = Json.format[TxIn]
  implicit val int64_tFormat = Json.format[int64_t]
  implicit val txOutFormat = Json.format[TxOut]
  implicit val txFormat = Json.format[Tx]
  implicit val blockFormat = Json.format[Block]

  implicit val storedBlockWrites = new Writes[StoredBlock] {
    def writes(sb: StoredBlock): JsValue = {
      Json.obj(
        "_id" -> sb.block.hash(),
        "block" -> sb.block,
        "height" -> sb.height)
    }
  }

  implicit val storedBlockReads = (
    (__ \ "block").read[Block] and
    (__ \ "height").read[Int])(StoredBlock.apply _)

}