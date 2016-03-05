package clipboardToolkit

import scalafx.beans.property.ObjectProperty

class CTState {
  val registeredElements = ObjectProperty[Seq[CTElement]](Seq.empty)


}
