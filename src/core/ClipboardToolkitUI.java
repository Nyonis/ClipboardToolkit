package core;

import java.io.File;
import java.util.Set;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ClipboardToolkitUI {
	private final Stage primaryStage;
	private Scene scene;
	private Button copyFromClipboardButton;
	private Button pasteToClipboardButton;
	private TableView<ClipboardElement> elementsTable;
	private TabPane mapView;
	
	private final ClipboardToolkit ct;
	private File lastSaveFile;
	private File lastSaveDirectory;
	
	@SuppressWarnings("unchecked")
	public ClipboardToolkitUI(Stage primaryStage, ClipboardToolkit ct) {
		this.primaryStage = primaryStage;
		this.ct = ct;
		
		
		mapView = new TabPane();
		mapView.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		updateMapView(null, null);
		
		elementsTable = new TableView<ClipboardElement>(ct.getElements());
		elementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		elementsTable.setEditable(true);
		
		
		TableColumn<ClipboardElement, String> nameCol = new TableColumn<ClipboardElement, String>("Name");
		nameCol.setCellValueFactory(new PropertyValueFactory<ClipboardElement, String>("name"));
		nameCol.prefWidthProperty().bind(elementsTable.widthProperty().divide(4));
		nameCol.setEditable(true);
		nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
		
		TableColumn<ClipboardElement, String> recordTimeCol = new TableColumn<ClipboardElement, String>("Record time");
		recordTimeCol.setCellValueFactory(new PropertyValueFactory<ClipboardElement, String>("recordTime"));
		recordTimeCol.prefWidthProperty().bind(elementsTable.widthProperty().divide(2));
		
		TableColumn<ClipboardElement, Integer> mapEntryCountCol = new TableColumn<ClipboardElement, Integer>("Entry count");
		mapEntryCountCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ClipboardElement,Integer>, ObservableValue<Integer>>() {
			
			@Override
			public ObservableValue<Integer> call(
					CellDataFeatures<ClipboardElement, Integer> param) {
				return new ReadOnlyObjectWrapper<Integer>(param.getValue().getMapEntryCount());
			}
		});
		mapEntryCountCol.prefWidthProperty().bind(elementsTable.widthProperty().divide(4));
		
		elementsTable.setRowFactory(new Callback<TableView<ClipboardElement>, TableRow<ClipboardElement>>() {

			@Override
			public TableRow<ClipboardElement> call(
					TableView<ClipboardElement> param) {
				TableRow<ClipboardElement> row = new TableRow<ClipboardElement>();
				
				ContextMenu rowMenu = new ContextMenu();
				
			    MenuItem removeItem = new MenuItem("Remove element");
			    removeItem.setOnAction((event) -> {
			    	ct.getElements().remove(row.getItem());
			    });

			    rowMenu.getItems().add(removeItem);

			    // only display context menu for non-null items:
			    row.contextMenuProperty().bind(
			      Bindings.when(Bindings.isNotNull(row.itemProperty()))
			      .then(rowMenu)
			      .otherwise((ContextMenu)null));
				
				return row;
			}
		});
		
		elementsTable.getColumns().setAll(nameCol, recordTimeCol, mapEntryCountCol);
		
		elementsTable.getSelectionModel().selectedItemProperty().addListener(
				(observableValue, oldValue, newValue) -> updateMapView(oldValue, newValue));
		elementsTable.setFocusModel(new TableViewFocusModel<ClipboardElement>(elementsTable));
		
		pasteToClipboardButton = new Button("Paste to Clipboard");
		pasteToClipboardButton.setOnAction((event) -> pasteToClipboard());
				
		copyFromClipboardButton = new Button("Copy from Clipboard");
		copyFromClipboardButton.setOnAction((event) -> ct.copyFromClipboard());
		

		MenuItem copyItem = new MenuItem("Copy");
		copyItem.setOnAction((event) -> pasteToClipboard());
		copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
		
		MenuItem pasteItem = new MenuItem("Paste");
		pasteItem.setOnAction((event) -> ct.copyFromClipboard());
		pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
		
		MenuItem removeItem = new MenuItem("Delete");
		removeItem.setOnAction((event) -> removeSelectedItem());
		removeItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
		
		
		Menu editMenu = new Menu("Edit");
		editMenu.getItems().addAll(copyItem, pasteItem, removeItem);
		
		MenuItem saveItem = new MenuItem("Save");
		saveItem.setOnAction(event -> onSave());
		saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
		MenuItem saveAsItem = new MenuItem("SaveAs");
		saveAsItem.setOnAction(event -> onSaveAs());
		saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
		MenuItem loadItem = new MenuItem("Load");
		loadItem.setOnAction(event -> onLoad());
		loadItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
		MenuItem separatorItem = new SeparatorMenuItem();
		MenuItem quitItem = new MenuItem("Quit");
		quitItem.setOnAction(event -> onQuit());
		quitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q , KeyCombination.SHORTCUT_DOWN));
		
		
		Menu fileMenu = new Menu("File");
		fileMenu.getItems().addAll(saveItem, saveAsItem, loadItem, separatorItem, quitItem);
		
		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(fileMenu, editMenu);
				
			
		SplitPane contentPane = new SplitPane();
		
		contentPane.getItems().addAll(elementsTable, mapView);
		
		FlowPane buttonPane = new FlowPane(Orientation.HORIZONTAL);
		buttonPane.setHgap(10);
		
		buttonPane.getChildren().add(copyFromClipboardButton);
		buttonPane.getChildren().add(pasteToClipboardButton);
		
		BorderPane root = new BorderPane();
		
		root.setCenter(contentPane);
		BorderPane.setMargin(contentPane, new Insets(0, 0, 5, 0));
		root.setBottom(buttonPane);
		BorderPane.setMargin(buttonPane, new Insets(0, 0, 5, 5));
		root.setTop(menuBar);
		
		scene = new Scene(root, 800, 600);
		
		this.primaryStage.setScene(scene);
		this.primaryStage.setTitle("Clipboard Toolkit");
		this.primaryStage.show();
	}
	
	private void updateMapView(ClipboardElement oldValue, ClipboardElement newValue) {
		if(newValue == null) {
			mapView.getTabs().clear();
			mapView.getTabs().add(new Tab("Nothing selected"));
		} else if(oldValue != newValue) {
			mapView.getTabs().clear();
			MapProperty<DataFormat, Object> newContents = newValue.contentsProperty();
			
			for(DataFormat df: newContents.keySet()) {
				Set<String> tmp = df.getIdentifiers();
				String[] idArray = {"Unnamed"};
				StringBuilder tooltipBuilder = new StringBuilder("Data Format:");
				Tab t = new Tab();
				
				if(tmp.size() > 0) {
					idArray = new String[tmp.size()];
					tmp.toArray(idArray);
					
					for(int i = 0; i < idArray.length; i++) {
						tooltipBuilder.append("\n" + idArray[i]);
					}
					
					t.setTooltip(new Tooltip(tooltipBuilder.toString()));
				}
			
				t.setText(idArray[0]);
				
				t.setContent(new TextArea(newContents.get(df).toString()));
				//TODO make proper editor with representation of content
				mapView.getTabs().add(t);
			}
		}
	}
	
	private void pasteToClipboard() {
		ClipboardElement selectedElement = elementsTable.getSelectionModel().getSelectedItem();
		if(selectedElement != null) {
			ct.pasteToClipboard(selectedElement);
		}
	}
	
	private void removeSelectedItem() {
		ClipboardElement selectedElement = elementsTable.getSelectionModel().getSelectedItem();
		if(selectedElement != null) {
			ct.getElements().remove(selectedElement);
		}
	}
	
	private void onSave() {
		if(lastSaveFile == null) {
			onSaveAs();
		} else {
			ct.save(lastSaveFile);
		}
	}
	
	private void onSaveAs() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Save");
		fc.getExtensionFilters().add(new ExtensionFilter("Clipboard Toolkit file", "*.ctf"));
		if(lastSaveDirectory != null)
			fc.setInitialDirectory(lastSaveDirectory);
			
		lastSaveFile = fc.showSaveDialog(primaryStage);
		if(lastSaveFile.getParentFile().isDirectory())
			lastSaveDirectory = lastSaveFile.getParentFile();
		
		if(lastSaveFile.getName().endsWith(".ctf"))
			ct.save(lastSaveFile);
		else
			ct.save(new File(lastSaveFile.getAbsolutePath() + ".ctf"));
	}
	
	private void onLoad() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Load");
		fc.getExtensionFilters().add(new ExtensionFilter("Clipboard Toolkit file", "*.ctf"));
		if(lastSaveDirectory != null) 
			fc.setInitialDirectory(lastSaveDirectory);
		
		lastSaveFile = fc.showOpenDialog(primaryStage);
		
		ct.load(lastSaveFile);
	}
	
	private void onQuit() {
		Platform.exit();
	}
}
