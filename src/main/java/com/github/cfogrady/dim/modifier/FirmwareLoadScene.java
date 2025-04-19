package com.github.cfogrady.dim.modifier;

import com.github.cfogrady.dim.modifier.data.AppState;
import com.github.cfogrady.dim.modifier.data.firmware.FirmwareManager;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URL;

@RequiredArgsConstructor
@Slf4j
public class FirmwareLoadScene {
    private final Stage stage;
    private final FirmwareManager firmwareManager;
    private final FirstLoadScene firstLoadScene;
    private final AppState appState;
    private static final String DARK_THEME_CSS = "/dark-theme.css";

    public void setupScene() {
        Button button = new Button();
        button.setText("Locate BE Firmware");
        button.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BE Firmware", "*.vb2"));
            fileChooser.setTitle("Select BE Firmware File");
            File file = fileChooser.showOpenDialog(stage);
            if(FirmwareManager.isValidFirmwareLocation(file)) {
                try {
                    firmwareManager.setFirmwareLocation(file);
                    appState.setFirmwareData(firmwareManager.loadFirmware());
                    firstLoadScene.setupScene();
                } catch (Throwable th) {
                    log.error("Unable to read firmware.", th);
                    Alert alert = new Alert(Alert.AlertType.NONE, "Unable to read firmware. Are you sure this is BE firmware?");
                    alert.getButtonTypes().add(ButtonType.OK);
                    alert.show();
                    firmwareManager.clearFirmwareLocation();
                }
            }
        });
        
        StackPane root = new StackPane(button);
        root.getStyleClass().add("root");
        
        Scene scene = new Scene(root, 640, 480);
        applyStylesheet(scene);

        stage.setScene(scene);
        stage.show();
    }
    
    /**
     * Aplica o CSS à cena
     */
    private void applyStylesheet(Scene scene) {
        try {
            URL cssResource = getClass().getResource(DARK_THEME_CSS);
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                log.error("CSS stylesheet not found: {}", DARK_THEME_CSS);
            }
        } catch (Exception e) {
            log.error("Error applying stylesheet", e);
        }
    }
}
