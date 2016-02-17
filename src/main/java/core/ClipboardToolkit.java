package core;

import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Set;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.Stage;

public class ClipboardToolkit extends Application {
	final static String FILE_HEADER = "Clipboard Toolkit Header";
	final static String FILE_FOOTER = "Clipboard Toolkit Footer";
	
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
	
	public void save(File saveFile) {
		try {
			saveFile.createNewFile();
			
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile));

			oos.writeObject(FILE_HEADER);
			
			for(ClipboardElement cE: elements) {
				oos.writeObject(cE);
			}
			
			oos.writeObject(FILE_FOOTER);
			
			oos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void load(File loadFile) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(loadFile));
			Object header = ois.readObject();
			if(header instanceof String && header.equals(FILE_HEADER)) {
				Object o = null;
				ArrayList<ClipboardElement> loadList = new ArrayList<ClipboardElement>();
				while((o = ois.readObject()) instanceof ClipboardElement) {
					loadList.add((ClipboardElement) o);
				}
				
				if(o instanceof String && o.equals(FILE_FOOTER)) {
					ois.close();
					elements.setAll(loadList);
				} else {
					ois.close();
					throw new StreamCorruptedException("Expected file footer was not found. File corrupted!");
				}
			} else {
				ois.close();
				throw new StreamCorruptedException("Expected file header was not found. File corrupted!");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
