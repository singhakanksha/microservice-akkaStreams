package com.foo.gaa

import java.nio.file.Paths

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, IOResult}
import akka.util.ByteString
import akka.stream.scaladsl._

import scala.concurrent.Future

object TweetVar1Example extends App{
  implicit val system = ActorSystem("reactive-tweets")

  final case class Author(handle: String)

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] =
      body
        .split(" ")
        .collect {
          case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
        }
        .toSet
  }

  val akkaTag = Hashtag("#akka")

  val tweets: Source[Tweet, NotUsed] = Source(
    Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
      Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
      Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
      Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
      Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
      Nil)



  // Flow is like a Source but with an “open” input
  def lineSink(filename: String): Sink[String, Future[IOResult]] = {
    Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
  }

  //  tweets
  //    .map(_.hashtags) // Get all sets of hashtags ...
  //    .reduce(_ ++ _) // ... and reduce them to a single set, removing duplicates across all tweets
  //    .mapConcat(identity) // Flatten the set of hashtags to a stream of hashtags
  //    .map(_.name.toUpperCase) // Convert all hashtags to upper case
  //    .runWith(Sink.foreach(println)) // Attach the Flow to a Sink that will finally print the hashtags
  val authors: Source[Author, NotUsed] = tweets.filter(tweet => tweet.hashtags.contains(akkaTag)).map(_.author)

  authors.runWith(Sink.foreach(println))
  //Flattening sequences in streams
  // mapConcat is similar to flatMap for scala sequences, difference being it emits an iterator
  val hashtags: Source[Hashtag, NotUsed] = tweets.mapConcat(_.hashtags.toList)

  //Broadcasting a stream
  // we have to split the source stream into two streams
  // it emits elements from its input port to all of its output ports.
  val writeAuthors: Sink[Author, Future[Done]] = Sink.ignore
  val writeHashtags: Sink[Hashtag, Future[Done]] = Sink.ignore
  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[Tweet](2))
    tweets ~> bcast.in
    bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors
    bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtags
    ClosedShape
  })
  g.run()
}

