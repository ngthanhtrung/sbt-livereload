package sbtlivereload

import akka.http.scaladsl.model.ws.Message
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject

private[sbtlivereload] final case class WebsocketChannels(
  incoming: Observable[Message],
  outgoing: ConcurrentSubject[Message, Message]
)
