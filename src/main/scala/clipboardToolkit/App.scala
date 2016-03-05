package clipboardToolkit

import akka.actor.Actor.Receive
import clipboardToolkit.ClipboardIO
import clipboardToolkit.Messages.{StartPolling, ProbeClipboard}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

import akka.actor.{Actor, Props, ActorSystem}

import scalafx.application.JFXApp
import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.event.ActionEvent
import scalafx.scene.Scene
import scalafx.scene.control.{MenuItem, MenuBar, Menu}
import scalafx.scene.input.{KeyCodeCombination, KeyCode, KeyCombination}
import scalafx.scene.layout.BorderPane
import scalafx.stage.WindowEvent

object App extends JFXApp {
  val state = new CTState()

  val conf = ConfigFactory.load()
  val system = ActorSystem("clipboardToolkit-AS", conf)
  val handler = system.actorOf(Props(new CTStateHandler(state)), "handler")
  val prober = system.actorOf(Props(new CTProber(handler)), "prober")
  val io = system.actorOf(Props(new ClipboardIO()), "io")

  stage = new PrimaryStage(){
    val menuBar = new MenuBar() {
      menus += new Menu("Edit") {
        items += new MenuItem("Copy from Clipboard") {
          accelerator() = new KeyCodeCombination(KeyCode.P, KeyCombination.ShortcutDown)
        }.delegate
        items += new MenuItem("Paste to Clipboard") {
          accelerator() = new KeyCodeCombination(KeyCode.C, KeyCombination.ShortcutDown)
        }.delegate
      }.delegate
    }
    scene = new Scene(new BorderPane() {
      top = menuBar
    })
    onCloseRequest = (we: WindowEvent) => {system.terminate()}
  }
}
