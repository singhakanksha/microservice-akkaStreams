package com.foo.gaa

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.Deflate
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.scaladsl._
import akka.util.ByteString


case class EventBatch(name: String, address: String)
object RealWorlExample extends App{
  implicit val actorSystem = ActorSystem("RealWorlExample")
  val githubSource = "https://api.github.com"

  import java.net.URI

  val uri = Uri(githubSource)
  val http = Http(actorSystem)

  val githubConnectionFlow = http.outgoingConnectionHttps(
    githubSource)
  val authorization = Authorization(BasicHttpCredentials("singhakanksha", "*****"))
  val eventBatchSource: Source[EventBatch, NotUsed] =
  // The stream start with a single request object ...
    Source.single(HttpRequest(
      HttpMethods.GET,
      uri,
      headers = List(authorization)))
      // ... that goes through a connection (i.e. is sent to the server)
      .via(githubConnectionFlow)
      .flatMapConcat {
        case response @ HttpResponse(StatusCodes.OK, _, _, _) =>
          response.entity.dataBytes
            // Decompress deflate-compressed bytes.
            .via(Deflate.decoderFlow)
            // Coalesce chunks into a line.
            .via(Framing.delimiter(ByteString("\n"), Int.MaxValue))
            // Deserialize JSON.
            .map(bs => Json.read[EventBatch](bs.utf8String))
        // process erroneous responses
      }
}
