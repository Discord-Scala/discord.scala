package github.discordscala.core

import github.discordscala.core.event.{Sharding, WebsocketListener}
import github.discordscala.core.models.snowflake.User
import github.discordscala.core.util.{DiscordException, Patch, RequestUtil}
import net.liftweb.json._
import spire.math.ULong

import scala.concurrent.Future

// TODO
/**
  * Discord API Client
  * Defaults are sensible.
  *
  * @param token      Token of the Client (prefix with Bot if necessary)
  * @param gatewayURL URL of the discord-compatible Gateway (e.g. Discord or Litecord)
  * @param apiURL     URL under which the API can be found
  * @param myShards   Set of ID of shards
  * @param sharding   ??
  */
case class Client(token: String, gatewayURL: String = "wss://gateway.discord.gg/", apiURL: String = "https://discordapp.com/api/v6/", myShards: Set[Int])(implicit sharding: Sharding) {

  /**
    * List of Listeners per Shard
    */
  lazy val shards: Set[WebsocketListener] = myShards.map((sc) => new WebsocketListener(this, Some(sc)))

  /**
    * Access the user the client logged in as
    *
    * @return User logged in
    */
  def ourUser = User(this)

  /**
    * Get any user from the API
    * Eventually will also contain member data for all shared guilds.
    *
    * @param id ID of the user to get
    * @return User with that ID
    */
  def user(id: ULong) = User(this, id)

  /**
    * Changes the username of the user logged in
    *
    * @param newUsername New username to change to
    * @return Updated User object
    */
  def username_=(newUsername: String): Future[Either[DiscordException, User]] = RequestUtil.restRequestFuture(s"${apiURL}users/@me", Map("Authorization" -> token), Patch, Extraction.decompose(User(username = Some(newUsername)))).map {
    case Left(e) => Left(e)
    case Right(j) => Right(j.extract[User])
  }

  /**
    * Login to the gateway
    */
  def login(): Unit = {
    shards.foreach(_.start())
  }

  /**
    * Logout of the gateway
    */
  def logout(): Unit = {
    shards.foreach(_.stop())
  }

}
