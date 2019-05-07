package ch.webruler.app.markdown;

import ch.webruler.app.markdown.util.Renderer;
import ch.webruler.app.markdown.util.Wrapper;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Markdown editor application
 *
 * @author Lorenz Pfisterer, Webruler
 */
public class MarkDownEditor extends Application {

	private static final Object FILE = new Object();
	private static final Object CHANGED = new Object();
	private static final Object TEXTAREA = new Object();
	private static final String NEW_FILE = "* New File";

	public static void main(String[] args) {
		launch(args);
	}

	private TabPane tabPane;
	private final FileChooser fileChooser = new FileChooser();
	private final Renderer renderer;

	public MarkDownEditor() {
		Wrapper wrapper = new Wrapper();
		this.renderer = wrapper.getRenderer();
	}

	@Override
	public void start(Stage stage) {
		// Create all menu items.
		Menu fileMenu = new Menu("File");
		MenuItem fileNewMenu = new MenuItem("New");
		MenuItem fileOpenMenu = new MenuItem("Open");
		MenuItem fileSaveMenu = new MenuItem("Save");
		MenuItem fileExitMenu = new MenuItem("Exit");

		// Assemble the menu bar
		fileMenu.getItems().addAll(fileNewMenu, fileOpenMenu, fileSaveMenu, new SeparatorMenuItem(), fileExitMenu);
		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(fileMenu);

		// Bind in the handler
		fileNewMenu.setOnAction(this::onNew);
		fileOpenMenu.setOnAction(this::onOpen);
		fileExitMenu.setOnAction(this::onExit);
		fileSaveMenu.setOnAction(this::onSave);

		// create and store the tab pane
		tabPane = new TabPane();

		// Add the menu and tab view to the root pane
		BorderPane root = new BorderPane();
		root.setTop(menuBar);
		root.setCenter(tabPane);

		// Set up the main (and only) scene.
		Scene scene = new Scene(root, 1600, 1000);
		final KeyCombination keyComb1 = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
		scene.addEventHandler(KeyEvent.KEY_RELEASED, (event) -> {
			if (keyComb1.match(event)) {
				save();
			}
		});

		File markdownCheatsheet = initHomeDirectory();
		openFile(markdownCheatsheet);
		openInitialFiles();

		stage.setTitle("Markdown Editor");
		stage.setScene(scene);
		stage.show();
	}

	private void openInitialFiles() {
		//Open given files
		Parameters parameters = getParameters();
		List<String> args = parameters.getRaw();
		if (args.size() > 0) {
			for (String fileName : args) {
				File file = new File(fileName);
				if (file.exists()) {
					openFile(file);
				}
			}
		}
	}

	private File initHomeDirectory() {
		File markdownSheet = null;
		File home = new File(System.getProperty("user.home"));
		if (home.exists()) {
			File appFolder = new File(home, ".ch.webruler.app.markdown.MarkDownEditor");
			if (!appFolder.exists()) {
				appFolder.mkdir();
			}
			markdownSheet = new File(appFolder, "Markdown-Cheatsheet.md");
			if (!markdownSheet.exists()) {
				InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Markdown-Cheatsheet.md");
				try {
					Files.copy(inputStream, markdownSheet.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return markdownSheet;
	}

	@Override
	public void stop() {
		onExit(null);
	}

	private void onExit(ActionEvent event) {
		ArrayList<Tab> tabs = new ArrayList<>();

		boolean hasUnsavedChanges = false;
		for (Tab t : tabPane.getTabs()) {
			if ((Boolean) t.getProperties().get(CHANGED)) {
				hasUnsavedChanges = true;
				tabs.add(t);
			}
		}

		if (hasUnsavedChanges) {
			askToSaveFiles(tabs);
		}
		System.exit(0);
	}

	private void askToSaveFiles(List<Tab> tabs) {
		Alert alert = new Alert(Alert.AlertType.WARNING, "Save files?", ButtonType.OK, ButtonType.CANCEL);
		alert.setTitle("Save files");
		Optional<ButtonType> result = alert.showAndWait();

		if (result.get() == ButtonType.OK) {
			for (Tab t : tabs) {
				saveTab(t);
			}
		}
	}

	private void onOpen(ActionEvent event) {
		File file = fileChooser.showOpenDialog(null);
		fileChooser.setInitialDirectory(file.getParentFile());
		if (file != null) {
			file = file.getAbsoluteFile();
			for (Tab t : tabPane.getTabs()) {
				if (t.getProperties().containsKey(FILE) && t.getProperties().get(FILE).equals(file)) {
					tabPane.getSelectionModel().select(t);
					return;
				}
			}
			openFile(file);
		}
	}

	private void openFile(File file) {
		final Tab tab = new Tab();
		tab.getProperties().put(FILE, file);
		tab.getProperties().put(CHANGED, Boolean.FALSE);
		tab.setOnCloseRequest((closeEvent) -> {
			if ((Boolean) tab.getProperties().get(CHANGED)) {
				askToSaveFiles(Arrays.asList(tab));
			}
		});
		try {
			String fileContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			createTabContent(tab, file.getName(), fileContent);
		} catch (IOException e) {
			createTabContent(tab, file.getName(), "Error loading file\n" + e.getLocalizedMessage());
		}
	}

	private void onNew(ActionEvent event) {
		final Tab tab = new Tab();
		tab.getProperties().put(CHANGED, Boolean.TRUE);
		createTabContent(tab, NEW_FILE, "");
	}

	private void onSave(ActionEvent event) {
		save();
	}

	private void save() {
		Tab tab = tabPane.getSelectionModel().getSelectedItem();
		saveTab(tab);
	}

	private void saveTab(Tab tab) {
		if ((Boolean) tab.getProperties().get(CHANGED)) {
			if (tab.getText().equals(NEW_FILE)) {
				File file = fileChooser.showOpenDialog(null);
				fileChooser.setInitialDirectory(file.getParentFile());
				if (file != null) {
					saveContentToFile(tab, file);
					tab.getProperties().put(FILE, file);
				}
			} else {
				File file = (File) tab.getProperties().get(FILE);
				saveContentToFile(tab, file);
			}
		}
	}

	private void saveContentToFile(Tab tab, File file) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			bw.write(((TextArea) tab.getProperties().get(TEXTAREA)).getText());
			bw.close();
			tab.getProperties().put(CHANGED, Boolean.FALSE);
			tab.setText(file.getName());
		} catch (IOException e) {
			createTabContent(tab, file.getName(), "Error writing file\n" + e.getLocalizedMessage());
		}
	}

	@Override
	public void init() throws Exception {
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown", "*.md", "*.markdown", "*.MD"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
	}

	private void createTabContent(final Tab tab, final String title, final String fileContent) {
		final WebView wv = new WebView();
		TextArea textArea = new TextArea();
		textArea.setPrefSize(800, 1000);
		WebEngine webEngine = wv.getEngine();
		textArea.setText(fileContent);
		String renderedValue = renderer.render(fileContent);
		webEngine.loadContent(renderedValue, "text/html");
		textArea.textProperty().addListener((ObservableValue<? extends String> ov, String oldValue, String newValue) -> {
					if (!(Boolean) tab.getProperties().get(CHANGED)) {
						tab.getProperties().put(CHANGED, Boolean.TRUE);
						tab.setText("* " + tab.getText());
					}
					wv.getEngine().loadContent(renderer.render(newValue), "text/html");
				}
		);
		SplitPane content = new SplitPane();
		content.getItems().addAll(textArea, wv);
		tab.setContent(content);
		tab.setText(title);
		tab.getProperties().put(TEXTAREA, textArea);
		wv.setContextMenuEnabled(false);
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
	}

}
