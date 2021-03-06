package org.discordscala

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import net.liftweb.json._
import org.discordscala.core.models.snowflake.Snowflaked
import org.discordscala.core.serializers._
import org.discordscala.core.util.Overlay
import spire.math.ULong

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

package object core {

  /**
    * Start of the Discord Epoch
    */
  val discordEpoch = ULong(1420070400000l)

  /**
    * Standard Discord.Scala Useragent
    */
  val userAgentName: String = "discord.scala"
  val userAgentVersion: String = "0.1.0"

  implicit val executor: ExecutionContextExecutor = ExecutionContext.global
  implicit val backend: SttpBackend[Future, Source[ByteString, Any]] = AkkaHttpBackend()
  implicit val formats: Formats = DefaultFormats ++ Seq(
    SnowflakeSerializer,
    USnowflakeSerializer,
    ColorSerializer,
    WebsocketEventSerializer,
    TimestampSerializer,
  )
  implicit val system: ActorSystem = ActorSystem("DiscordScala")

  implicit class SnowflakeUtil(s: Snowflaked) {

    /**
      * Converts a snowflake to an Instant
      *
      * @return Approximate creation time of the snowflake
      */
    def toTime: Option[Instant] = {
      s.id match {
        case Some(id) =>
          val depoch = id >> 22
          val uepoch = depoch + discordEpoch
          Some(Instant.ofEpochMilli(uepoch.signed))
        case None => None
      }
    }

  }

  implicit class OverlayOps[A](a: A)(implicit ev: Overlay[A]) {
    def over(b: A): A = ev(a, b)
  }

}
