package com.github.cfogrady.dim.modifier;

import com.github.cfogrady.dim.modifier.data.AppState;
import com.github.cfogrady.dim.modifier.data.card.CardData;
import com.github.cfogrady.dim.modifier.controllers.LoadedViewController;
import com.github.cfogrady.dim.modifier.data.card.CardDataIO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javafx.geometry.Pos;

import java.io.*;
import java.net.URL;

@RequiredArgsConstructor
@Slf4j
public class FirstLoadScene {
    private final AppState appState;
    private final Stage stage;
    private final CardDataIO cardDataIO;
    private final LoadedViewController loadedViewController;
    private static final String DARK_THEME_CSS = "/dark-theme.css";

    public void setupScene() {
        // Create main container
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("root");
        
        // Add icon
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/icon.png")));
        icon.setFitHeight(100);
        icon.setFitWidth(100);
        icon.setPreserveRatio(true);
        
        // Add description
        Text description = new Text("DIM Modifier");
        description.getStyleClass().add("description-text");
        
        // Create button with custom styling
        Button button = new Button();
        button.setText("Open DIM File");
        button.getStyleClass().add("primary-button");
        button.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select DIM File");
            File file = fileChooser.showOpenDialog(stage);
            if(file != null) {
                loadCard(file);
                setupLoadedDataView();
            }
        });
        
        // Add credits
        VBox creditsBox = new VBox(5);
        creditsBox.setAlignment(Pos.CENTER);
        Text credits1 = new Text("DIM Modifier (Modded by Aderek)");
        Text credits2 = new Text("Original software by Graddy");
        credits1.getStyleClass().add("credits-text");
        credits2.getStyleClass().add("credits-text");
        creditsBox.getChildren().addAll(credits1, credits2);
        
        // Add all elements to root
        root.getChildren().addAll(icon, description, button, creditsBox);
        
        Scene scene = new Scene(root, 640, 480);
        applyStylesheet(scene);

        stage.setScene(scene);
        stage.show();
    }

    private void loadCard(File file) {
        try(FileInputStream fileInputStream = new FileInputStream(file)) {
            CardData<?, ?, ?> cardData = cardDataIO.readFromStream(fileInputStream);
            appState.setCardData(cardData);
            appState.setLastOpenedFilePath(file);
        } catch (IOException e) {
            log.error("Error loading file: {}", file.getAbsolutePath(), e);
        }
    }

    private void setupLoadedDataView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoadedView.fxml"));
            loader.setControllerFactory(p -> loadedViewController);
            Scene scene = new Scene(loader.load(), 1520, 720);
            
            // Aplicar o tema escuro
            applyStylesheet(scene);
            
            // Atualizar a visualização e o título
            loadedViewController.refreshAll();
            
            // Forçar a atualização do título usando o método do controller
            loadedViewController.updateWindowTitle();
            
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            log.error("Unable to load layout for loaded data view!", e);
        }
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
