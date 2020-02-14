package com.foo.gaa

import akka.stream._
import akka.stream.scaladsl._
import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths
import TweetExample._


object HelloWorld extends App {
  implicit val system = ActorSystem("QuickStart")
  val source: Source[Int, NotUsed] = Source(1 to 100)

  val done: Future[Done] = source.runForeach(i => println(i))
  implicit val ec = system.dispatcher
//  done.onComplete(_ => system.terminate())

  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

//  val result: Future[IOResult] =
//    factorials.map(_.toString).runWith(lineSink("factorials.txt"))
//  result.onComplete(_ => system.terminate())

  factorials
    .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
    .throttle(1, 1.second)
    .runForeach(println)
}
