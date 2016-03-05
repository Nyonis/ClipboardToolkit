package clipboardToolkit

import scala.concurrent.duration.FiniteDuration

object Messages {

  trait Message {

  }

  case class PutElementToClipboard(element: CTElement) extends Message

  case class AddElement(element: CTElement)

  case class RemoveElement(index: Int) extends Message

  case class StartPolling(initialDelay: FiniteDuration, interval: FiniteDuration) extends Message

  case class PollTick() extends Message

  case class StopPolling() extends Message

  case class ProbeClipboard() extends Message

  case class NoNewContentOnClipboard() extends Message
}
