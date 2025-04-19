package com.github.cfogrady.dim.modifier;

import com.github.cfogrady.dim.modifier.data.firmware.FirmwareManager;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

@Slf4j
public class Main extends Application {
    private ApplicationOrchestrator applicationOrchestrator;
    
    // Estilo CSS para a aplicação
    private static final String DARK_THEME_CSS = "/dark-theme.css";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            applicationOrchestrator = ApplicationOrchestrator.buildOrchestration(primaryStage);
            
            // Aplicar o tema escuro à aplicação
            applyDarkTheme();
            
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        primaryStage.setTitle("DIM Modifier");
        loadFirstScene();
    }
    
    /**
     * Aplica o tema escuro carregando o CSS para uso em todas as cenas
     */
    private void applyDarkTheme() {
        try {
            URL cssResource = getClass().getResource(DARK_THEME_CSS);
            if (cssResource != null) {
                String css = cssResource.toExternalForm();
                log.info("Aplicando tema escuro da aplicação: {}", css);
                Application.setUserAgentStylesheet(css);
            } else {
                log.error("Arquivo CSS do tema escuro não encontrado: {}", DARK_THEME_CSS);
            }
        } catch (Exception e) {
            log.error("Erro ao carregar o tema escuro", e);
        }
    }

    @Override
    public void stop() {
        applicationOrchestrator.getTimer().cancel();
        applicationOrchestrator.getTimer().purge();
    }

    private void loadFirstScene() {
        FirmwareManager firmwareManager = applicationOrchestrator.getFirmwareManager();
        if(firmwareManager.isValidFirmwareLocationSet()) {
            try {
                applicationOrchestrator.getAppState().setFirmwareData(firmwareManager.loadFirmware());
                applicationOrchestrator.getFirstLoadScene().setupScene();
            } catch (Throwable th) {
                log.error("Unable to load firmware. Please select firmware location.", th);
                Alert alert = new Alert(Alert.AlertType.NONE, "Unable to read firmware. Please select a new firmware location.");
                alert.getButtonTypes().add(ButtonType.OK);
                alert.show();
                firmwareManager.clearFirmwareLocation();
                applicationOrchestrator.getFirmwareLoadScene().setupScene();
            }
        } else {
            //firmware load scene
            applicationOrchestrator.getFirmwareLoadScene().setupScene();
        }
    }
}
