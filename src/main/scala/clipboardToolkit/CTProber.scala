package clipboardToolkit

import akka.actor.{Cancellable, ActorRef, ActorLogging, Actor}
import clipboardToolkit.Messages._

import scala.concurrent.duration.FiniteDuration
import scalafx.application.Platform
import scalafx.scene.input.{DataFormat, ClipboardContent, Clipboard}

class CTProber(handler: ActorRef) extends Actor with ActorLogging{
  var scheduledTask:Cancellable = null
  var lastFoundContents:ClipboardContent = null

  override def receive: Receive = {
    case _:ProbeClipboard => probeClipboard(true)
    case _:PollTick => probeClipboard(false)
    case PutElementToClipboard(element) => Platform.runLater(Clipboard.systemClipboard.content = element.content)
    case StartPolling(initialDelay, interval) => startPolling(initialDelay, interval)
    case _:StopPolling => stopPolling()
    case unhandledMessage: Message => log.warning("Unhandled message received: " + unhandledMessage)
    case invalidMessage => log.error("Invalid message received: " + invalidMessage)
  }

  def startPolling(initialDelay: FiniteDuration, interval: FiniteDuration): Unit = {
    log.info("Start polling with delay: " + initialDelay + " and interval " + interval)
    if(scheduledTask != null && !scheduledTask.isCancelled) {
      log.info("Stopping previous polling task")
      stopPolling()
    }

    scheduledTask = context.system.scheduler.schedule(initialDelay, interval, this.self, new PollTick)(context.dispatcher)
  }

  def stopPolling(): Unit = {
    if(scheduledTask != null && !scheduledTask.isCancelled) {
      scheduledTask.cancel()
      log.info("Stopped polling")
    }
    else
      log.warning("There was no active poll to be cancelled")
  }

  def probeClipboard(manualProbe: Boolean): Unit = Platform.runLater({
    val currentContents = Clipboard.systemClipboard.content
    val newContent =  if(lastFoundContents != null) {
      // Extract to java raw image type from contents because this would cause equals to always return false if there is an image on the clipboard
      val rawJavaImage = DataFormat.Image
      val modifiedLastContents = lastFoundContents.toMap[javafx.scene.input.DataFormat, AnyRef] - rawJavaImage
      val modifiedCurrentContents = currentContents.toMap[javafx.scene.input.DataFormat, AnyRef] - rawJavaImage
      !modifiedCurrentContents.equals(modifiedLastContents)
    } else {
      true
    }

    if(!newContent) {
      if (manualProbe)
        handler ! NoNewContentOnClipboard
    } else {
      log.info("New content on clipboard: " + currentContents)
      lastFoundContents = currentContents
      handler ! new AddElement(new CTElement(System.currentTimeMillis(), Clipboard.systemClipboard.content))
    }
  })
}
