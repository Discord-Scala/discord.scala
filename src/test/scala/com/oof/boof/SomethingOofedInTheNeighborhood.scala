package com.oof.boof

import org.discordscala.core.Client
import org.discordscala.core.event.Sharding
import org.discordscala.core.event.opzero.{MessageCreateEvent, MessageUpdateEvent}
import org.discordscala.core.models.snowflake.Message
import org.discordscala.core.models.snowflake.guild.Channel
import spire.math.ULong

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object SomethingOofedInTheNeighborhood {

  def main(args: Array[String]): Unit = {
    implicit val sharding: Sharding = Sharding(1)
    implicit val formats = org.discordscala.core.formats
    val c = Client("Bot [token]", handler = {
      case me: MessageCreateEvent =>
        val m = me.d
        implicit val client: Client = me.shard.client
        m.author.flatMap(_.bot) match {
          case Some(true) =>
          case _ =>
            m.content match {
              case Some("chan!dab") =>
                m.channelId match {
                  case Some(cid) =>
                    Channel(cid) match {
                      case Left(e) => println(e)
                      case Right(ch) =>
                        ch.postMessage(Message(content = Some("<:tsundredab:384083757558398976>")))
                    }
                  case None =>
                }
              case Some("chan!ping") =>
                m.channelId match {
                  case Some(cid) =>
                    Channel(cid) match {
                      case Left(e) => println(e)
                      case Right(ch) =>
                        val s = System.currentTimeMillis()
                        Await.result(ch.postMessage(Message(content = Some("Ping"))), Duration.Inf) match {
                          case Left(e) => println(e)
                          case Right(ms) =>
                            val e = System.currentTimeMillis()
                            ms.edit(Message(content = Some(s"Ping: `${e - s}ms`")))
                        }
                    }
                  case None =>
                }
              case _ =>
            }
        }
      case mue: MessageUpdateEvent =>
        implicit val client: Client = mue.shard.client
         mue.shard.cache match {
          case Some(cache) =>
            Channel(ULong(390752467878412288l)) match {
              case Left(e) => println(e)
              case Right(ch) => ch.postMessage(Message(content = Some((cache.messages / mue.d).map(_.content).filter(_.isDefined).map(_.get).mkString("\n\n"))))
            }
        }
    }, myShards = Set(0))
    c.login()
    Thread.sleep(20000)
    implicit val client: Client = c
    Channel(ULong(390752467878412288l)) match {
      case Left(e) => println(e)
      case Right(ch) => ch.postMessage(Message(content = Some("<:tsundredab:384083757558398976>")))
    }
  }

}
