package ch.webruler.app.markdown;

import ch.webruler.app.markdown.util.Renderer;
import ch.webruler.app.markdown.util.Wrapper;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
        stage.setTitle("Markdown Editor");
        stage.setScene(scene);
        stage.show();
    }

    private void onExit(ActionEvent event) {
        for (Tab t : tabPane.getTabs()) {
            if ((Boolean) t.getProperties().get(CHANGED)) {
                System.out.println("Unsaved stuff!!!");
                System.exit(0);
            }
        }
    }

    private void onOpen(ActionEvent event) {
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            file = file.getAbsoluteFile();
            for (Tab t : tabPane.getTabs()) {
                if (t.getProperties().containsKey(FILE) && t.getProperties().get(FILE).equals(file)) {
                    tabPane.getSelectionModel().select(t);
                    return;
                }
            }
            final Tab tab = new Tab();
            tab.getProperties().put(FILE, file);
            tab.getProperties().put(CHANGED, Boolean.FALSE);
            try {
                String fileContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                createTabContent(tab, file.getName(), fileContent);
            } catch (IOException e) {
                createTabContent(tab, file.getName(), "Error loading file\n" + e.getLocalizedMessage());
            }
        }
    }

    private void onNew(ActionEvent event) {
        final Tab tab = new Tab();
        tab.getProperties().put(CHANGED, Boolean.TRUE);
        createTabContent(tab, NEW_FILE, "");
    }


    private void onSave(ActionEvent event) {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if ((Boolean) tab.getProperties().get(CHANGED)) {
            if (tab.getText().equals(NEW_FILE)) {
                File file = fileChooser.showOpenDialog(null);
                if (file != null) {
                    saveContentToFile(tab, file);
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
