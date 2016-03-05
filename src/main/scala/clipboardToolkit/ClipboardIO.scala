package clipboardToolkit

import java.io._

import akka.actor.{ActorLogging, Actor}
import core.ClipboardElement

class ClipboardIO extends Actor with ActorLogging{
  def save(saveFilePath: String): Unit = {

  }

  def load(loadFilePath: String): Unit = {

  }

  override def receive: Receive = {
    case unknownMessage => log.warning("Unknown message received: " + unknownMessage)
  }
}

object ClipboardIO {
  private val FILE_HEADER: String = "Clipboard Toolkit Header"
  private val FILE_FOOTER: String = "Clipboard Toolkit Footer"

  /**
    * Deprecated
    * @param saveFile
    * @param elements
    */
  def save(saveFile: File, elements: Iterable[ClipboardElement]) {
    try {
      saveFile.createNewFile
      val oos: ObjectOutputStream = new ObjectOutputStream(new FileOutputStream(saveFile))
      oos.writeObject(FILE_HEADER)
      for (cE <- elements) {
        oos.writeObject(cE)
      }
      oos.writeObject(FILE_FOOTER)
      oos.close
    }
    catch {
      case e: IOException => {
        e.printStackTrace
      }
    }
  }

  /**
    * Deprecated
    * @param loadFile
    * @return
    */
  def load(loadFile: File) : Seq[ClipboardElement] = {
    try {
      val ois: ObjectInputStream = new ObjectInputStream(new FileInputStream(loadFile))
      val header: AnyRef = ois.readObject
      if (header.isInstanceOf[String] && (header == FILE_HEADER)) {
        var o: AnyRef = null
        var loadList: Seq[ClipboardElement] = Seq.empty[ClipboardElement]
        while ((({
          o = ois.readObject;
          o
        })).isInstanceOf[ClipboardElement]) {
          loadList = loadList :+ o.asInstanceOf[ClipboardElement]
        }
        if (o.isInstanceOf[String] && (o == FILE_FOOTER)) {
          ois.close
          return loadList
        }
        else {
          ois.close
          throw new StreamCorruptedException("Expected file footer was not found. File corrupted!")
        }
      }
      else {
        ois.close
        throw new StreamCorruptedException("Expected file header was not found. File corrupted!")
      }
    }
    catch {
      case e: IOException => {
        e.printStackTrace
      }
      case e: ClassNotFoundException => {
        e.printStackTrace
      }
    }

    return Seq.empty
  }
}
