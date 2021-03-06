package org.discordscala.core.models.snowflake

import net.liftmodules.jsonextractorng.Extraction._
import org.discordscala.core.models.snowflake.guild.{Guild, GuildedMember}
import org.discordscala.core.util.{DiscordException, RequestUtil}
import org.discordscala.core.{Client, _}
import spire.math.ULong

/**
  * Representation of an User
  *
  * @param id            ID of the user
  * @param username      Name of the user
  * @param discriminator Discriminator of the user (eg if full name is ABC#1234, 1234 is the discriminator)
  * @param avatar        Hash of the users Avatar
  * @param bot           Whether or not the user is a bot
  * @param mfa           Whether or not 2fa is enabled (Only available to self user)
  * @param verified      Whether or not the email is verified (Only available to self user)
  * @param email         Email of the user (Only available to self user)
  */
case class User(
                 id: Option[ULong] = None,
                 username: Option[String] = None,
                 discriminator: Option[String] = None,
                 avatar: Option[String] = None,
                 bot: Option[Boolean] = None,
                 mfa: Option[Boolean] = None,
                 verified: Option[Boolean] = None,
                 email: Option[String] = None) extends Snowflaked {

  override type Self = User

  def asMemberOf(g: Guild): GuildedMember = {
    null // FIXME complete this @gerd
  }

  def !(implicit client: Client): Either[DiscordException, User] = User(id.get)

}

object User {

  def apply(id: ULong)(implicit client: Client): Either[DiscordException, User] = RequestUtil.awaitRestRequestFuture(client.apiURL + s"users/$id", Map("Authorization" -> client.token)) match {
    case Left(e) => Left(e)
    case Right(j) => Right(j.extractNg[User])
  }

  def apply(c: Client): Either[DiscordException, User] = RequestUtil.awaitRestRequestFuture(c.apiURL + "users/@me", Map("Authorization" -> c.token)) match {
    case Left(e) => Left(e)
    case Right(j) => Right(j.extractNg[User])
  }

}