package github.discordscala.core.util

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.softwaremill.sttp._
import github.discordscala.core._
import net.liftweb.json._
import net.liftweb.json.JsonAST.JValue

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

/**
  * Object that does the heavy lifting for API requests
  */
object RequestUtil {

  implicit val system = ActorSystem("DiscordScalaHTTPRequests")
  implicit val materializer = ActorMaterializer()

  /**
    * Method that makes a REST request
    *
    * @param url     URL to request to
    * @param headers Map of Headers
    * @param method  REST Method to use
    * @param body    JSON representation of the body
    * @return Future of the request
    */
  def restRequestFuture(url: String, headers: Map[String, String], method: RequestMethod, body: JValue): Future[Either[DiscordException, JValue]] = restRequestFuture(url, headers, method, Some(("application/json", compactRender(body))))

  /**
    * Like restRequestFuture, but waits for the result
    *
    * @param url     URL to request to
    * @param headers Map of Headers
    * @param method  REST Method to use
    * @param body    JSON representation of the body
    * @param timeout How long to wait for an Answer
    * @return Either a Discord Exception or the response
    */
  def awaitRestRequestFuture(url: String, headers: Map[String, String], method: RequestMethod, body: JValue, timeout: Duration): Either[DiscordException, JValue] = awaitRestRequestFuture(url, headers, method, Some(("application/json", compactRender(body))), timeout)

  /**
    * Like restRequestFuture, but waits for the result
    *
    * @param url     URL to request to
    * @param headers Map of Headers
    * @param method  REST Method to use
    * @param body    Body of the request, with type (type, body)
    * @param timeout How long to wait for an Answer
    * @return Either a Discord Exception or the response
    */
  def awaitRestRequestFuture(url: String, headers: Map[String, String], method: RequestMethod = Get, body: Option[(String, String)] = None, timeout: Duration = Duration.Inf): Either[DiscordException, JValue] = Await.result(restRequestFuture(url, headers), timeout)

  /**
    * Method that makes a REST request
    *
    * @param url     URL to request to
    * @param headers Map of Headers
    * @param method  REST method to use
    * @param body    Body of the request, with type (type, body)
    * @return Future of the request
    */
  def restRequestFuture(url: String, headers: Map[String, String], method: RequestMethod = Get, body: Option[(String, String)] = None): Future[Either[DiscordException, JValue]] = Future {
    var status = 0
    var eResponse: Either[DiscordException, String] = null
    do {
      val brequest = (method match {
        case Get => sttp.get(uri"$url")
        case Post => sttp.post(uri"$url")
        case Patch => sttp.patch(uri"$url")
      }).headers(headers + userAgent)
      val request = (body match {
        case Some((mime, content)) => brequest.contentType(mime, "UTF-8").streamBody(Source.single(ByteString(content, "UTF-8")))
        case None => brequest
      }).response(asStream[Source[ByteString, NotUsed]])
      val waitResponse = request.send()
      val response = Await.result(waitResponse, Duration.Inf)
      status = response.code
      if (status == 429) {
        val unRatelimitTime = response.header("X-RateLimit-Reset").get.toLong
        Thread.sleep(System.currentTimeMillis() - (unRatelimitTime * 1000 + 500))
      } else {
        if (status / 100 == 4 || status / 100 == 5) {
          eResponse = Left(status match {
            case 400 => BadRequest
            case 401 => Unauthorized
            case 403 => Forbidden
            case 404 => NotFound
            case 502 => BadRequest
          })
        } else {
          eResponse = Right(response.body match {
            case Left(str) => str
            case Right(sor) => Await.result(sor.runFold("")((acc, ns) => acc + ns.foldLeft("")((s, b) => s + b.toChar.toString)), Duration.Inf)
          })
        }
      }
    } while (status == 429)
    eResponse match {
      case Left(e) => Left(e)
      case Right(s) => Right(parse(s))
    }
  }(executor = ExecutionContext.global)

}

/**
  * Base trait for request methods
  */
sealed trait RequestMethod

/**
  * GET Request
  */
case object Get extends RequestMethod

/**
  * POST request
  */
case object Post extends RequestMethod

/**
  * PATCH request
  */
case object Patch extends RequestMethod

/**
  * Exceptions that can occur when making connections
  */
sealed trait DiscordException

case object BadRequest extends DiscordException

case object Unauthorized extends DiscordException

case object Forbidden extends DiscordException

case object NotFound extends DiscordException

case object GatewayUnavailable extends DiscordException