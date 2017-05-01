package sbtlivereload

import java.util.concurrent.TimeUnit

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.ws.UpgradeToWebsocket
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.execution.Scheduler
import monix.reactive.Observer
import monix.reactive.subjects.ConcurrentSubject

final class LiveReloadServer(host: String, port: Int) {

  private[this] val classLoader = getClass.getClassLoader
  private[this] val config = ConfigFactory.load(classLoader)
  private[this] implicit val system = ActorSystem("live-reload", config, classLoader)
  private[this] implicit val materializer = ActorMaterializer()

  private[this] implicit val scheduler = Scheduler.fixedPool("live-reload", 4)

  private[this] val concurrentConnections = mutable.Set.empty[WebsocketChannels]

  private[this] def createWebsocketFlow(): Flow[Message, Message, Any] = {
    val incoming = ConcurrentSubject.publishToOne[Message]
    val outgoing = ConcurrentSubject.publishToOne[Message]
    val channels = WebsocketChannels(incoming, outgoing)

    val sink = Sink.fromSubscriber(Observer.toReactiveSubscriber(incoming))
    val source = Source.fromPublisher(outgoing.toReactivePublisher)
    val flow = Flow.fromSinkAndSource(sink, source)

    concurrentConnections += channels

    incoming.subscribe(new Observer[Message] {

      def onNext(item: Message): Future[Ack] = item match {
        case message: TextMessage =>
          message.textStream.runWith(Sink.ignore).map(_ => Continue)

        case message: BinaryMessage =>
          message.dataStream.runWith(Sink.ignore).map(_ => Continue)
      }

      def onError(throwable: Throwable): Unit = {
        throwable.printStackTrace()
        concurrentConnections -= channels
      }

      def onComplete(): Unit = {
        concurrentConnections -= channels
      }
    })

    flow
  }

  def notify(notification: Notification): Unit = {
    val text = notification match {
      case ReloadStylesheets => "reload_stylesheets"
      case ReloadPage => "reload_page"
    }

    val message = TextMessage(text)

    concurrentConnections.foreach {
      _.outgoing.onNext(message)
    }
  }

  private[this] val route = get {
    pathEndOrSingleSlash {
      optionalHeaderValueByType[UpgradeToWebsocket]() {
        case Some(upgrade) => complete(upgrade.handleMessages(createWebsocketFlow()))
        case None => complete(StatusCodes.NotFound: StatusCode)
      }
    } ~ path("livereload.js") {
      getFromResource("livereload.js")
    }
  }

  private[this] val binding = Http().bindAndHandle(route, host, port)

  def kill(): Unit = {
    Await.result(
      binding.flatMap(_.unbind()),
      Duration(10, TimeUnit.SECONDS)
    )
  }
}
