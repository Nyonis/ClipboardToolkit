package clipboardToolkit

import akka.actor.{ActorLogging, Actor}
import clipboardToolkit.Messages._

class CTStateHandler(state: CTState) extends Actor with ActorLogging{
  override def receive: Receive = {
    case AddElement(element) => state.registeredElements() = state.registeredElements() :+ element
    case RemoveElement(index) => state.registeredElements() = state.registeredElements().take(index) ++ state.registeredElements().drop(index + 1)
    case unhandledMessage: Message => log.warning("Unhandled message received: " + unhandledMessage)
    case invalidMessage => log.error("Invalid message received: " + invalidMessage)
  }
}
