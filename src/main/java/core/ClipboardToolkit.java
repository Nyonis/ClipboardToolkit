package core;

import java.util.List;
import java.io.File;
import java.util.Set;

import clipboardToolkit.ClipboardIO$;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.Stage;
import scala.collection.JavaConversions;

public class ClipboardToolkit extends Application {
	
	private ObservableList<ClipboardElement> elements;
	private Clipboard systemClipboard;
	
	public ClipboardToolkit() {
		elements = FXCollections.observableArrayList();
		systemClipboard = Clipboard.getSystemClipboard();
	}

	public boolean copyFromClipboard() {
		ClipboardElement element = new ClipboardElement(System.currentTimeMillis());
		
		Set<DataFormat> contentTypes = systemClipboard.getContentTypes();
		for(DataFormat format: contentTypes) {
			try {
				element.addContent(format, systemClipboard.getContent(format));
			} catch (IllegalArgumentException e) {
				System.err.println("Insertion of clipboard content failed! Aborting!");
				e.printStackTrace();
				return false;
			}
		}
		
		elements.add(element);
		return true;
	}
	
	public boolean pasteToClipboard(int elementIndex) {
		return pasteToClipboard(elements.get(elementIndex));
	}
	
	public boolean pasteToClipboard(ClipboardElement clipboardElement) {
		systemClipboard.setContent(clipboardElement.contentsProperty());
		return true;
	}
	
	public ObservableList<ClipboardElement> getElements() {
		return elements;
	}

	//Legacy method
	public void load(File loadFile) {
		elements.setAll(JavaConversions.seqAsJavaList(ClipboardIO$.MODULE$.load(loadFile)));
	}

	//Legacy method
	public void save(File saveFile) {
		ClipboardIO$.MODULE$.save(saveFile, JavaConversions.iterableAsScalaIterable(elements));
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		List<String> args = getParameters().getRaw();
		
		if(args.size() == 1) {
			File loadFile = new File(args.get(0));
			if(loadFile.exists())
				load(loadFile);
			else
				System.err.println("The specified file does not exist");
		}
		
		new ClipboardToolkitUI(primaryStage, this);
	}

	public static void main(String[] args) {
		launch(args);
	}

}
