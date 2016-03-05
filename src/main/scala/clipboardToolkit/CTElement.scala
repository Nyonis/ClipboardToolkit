package clipboardToolkit

import scalafx.beans.property.StringProperty
import scalafx.scene.input.ClipboardContent

class CTElement (val recordedTime: Long, val content: ClipboardContent, val name: StringProperty = StringProperty("Unnamed")){

}
