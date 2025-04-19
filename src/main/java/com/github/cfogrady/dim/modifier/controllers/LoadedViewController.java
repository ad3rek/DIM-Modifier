package com.github.cfogrady.dim.modifier.controllers;

import com.github.cfogrady.dim.modifier.SpriteImageTranslator;
import com.github.cfogrady.dim.modifier.data.AppState;
import com.github.cfogrady.dim.modifier.data.bem.BemCardData;
import com.github.cfogrady.dim.modifier.data.bem.BemCharacter;
import com.github.cfogrady.dim.modifier.data.bem.BemTransformationEntry;
import com.github.cfogrady.dim.modifier.data.card.Character;
import com.github.cfogrady.dim.modifier.data.card.MetaData;
import com.github.cfogrady.dim.modifier.data.card.TransformationEntry;
import com.github.cfogrady.dim.modifier.data.dim.DimCardData;
import com.github.cfogrady.dim.modifier.data.dim.DimCharacter;
import com.github.cfogrady.dim.modifier.data.dim.DimTransformationEntity;
import com.github.cfogrady.vb.dim.sprite.SpriteData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javafx.scene.image.PixelReader;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import javax.imageio.ImageIO;

@Slf4j
@RequiredArgsConstructor
public class LoadedViewController implements Initializable {
    private final AppState appState;
    private final Node charactersSubView;
    private final CharacterViewController characterViewController;
    private final BattlesViewController battlesViewController;
    private final Node battlesSubView;
    private final BemSystemViewController bemSystemViewController;
    private final Node bemSystemSubView;
    private final DimSystemViewController dimSystemViewController;
    private final Node dimSystemSubView;
    private final DimIOController dimIOController;
    private final SpriteImageTranslator spriteImageTranslator;
    private final Stage primaryStage;

    @FXML
    private MenuItem openMenuItem;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private MenuItem saveAsMenuItem;
    @FXML
    private MenuItem exportAllMenuItem;
    @FXML
    private MenuItem exportSpritesMenuItem;
    @FXML
    private MenuItem exportDataMenuItem;
    @FXML
    private Text dimIdText;
    @FXML
    private Text revisionIdText;
    @FXML
    private Text factoryDateText;
    @FXML
    private Text checksumText;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab charactersTab;
    @FXML
    private Tab battlesTab;
    @FXML
    private Tab systemTab;
    @FXML
    private AnchorPane charactersSubViewPane;
    @FXML
    private AnchorPane battlesSubViewPane;
    @FXML
    private AnchorPane systemSubViewPane;

    private SubViewSelection subViewSelection = SubViewSelection.CHARACTERS;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    
    private boolean hasUnsavedChanges = false;

    private enum SubViewSelection {
        CHARACTERS,
        BATTLES,
        SYSTEM;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurar os listeners para mudança de abas
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (newValue == charactersTab) {
                    subViewSelection = SubViewSelection.CHARACTERS;
                } else if (newValue == battlesTab) {
                    subViewSelection = SubViewSelection.BATTLES;
                } else if (newValue == systemTab) {
                    subViewSelection = SubViewSelection.SYSTEM;
                }
                refreshSubview();
                log.debug("Tab changed to: {}", subViewSelection);
            } catch (Exception e) {
                log.error("Error changing tab: {}", e.getMessage(), e);
            }
        });
        
        openMenuItem.setOnAction(this::openButton);
        saveMenuItem.setOnAction(e -> saveButton(false));
        saveAsMenuItem.setOnAction(e -> saveButton(true));
        
        exportAllMenuItem.setOnAction(this::exportAll);
        exportSpritesMenuItem.setOnAction(this::exportSprites);
        exportDataMenuItem.setOnAction(this::exportData);
        
        // Configurar o callback para quando houver alterações não salvas
        appState.setUnsavedChangesCallback(this::markUnsavedChanges);
        
        // Inicializar o título da aplicação
        updateWindowTitle();
    }

    private void openButton(ActionEvent event) {
        // Criar um callback simplificado que apenas limpa o estado e atualiza o título
        Runnable afterLoadCallback = () -> {
            clearState();
            updateWindowTitle(); // Chamado após lastOpenedFilePath ser atualizado no DimIOController
        };
        
        // Abrir o DIM e passar o callback
        dimIOController.openDim(afterLoadCallback);
    }
    
    private void saveButton(boolean saveAs) {
        if (saveAs) {
            // Lógica para "Save As"
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save DIM File As...");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DIM Files", "*.bin"));
            
            // Definir o diretório inicial se disponível
            if (appState.getLastOpenedFilePath() != null) {
                File parentDir = appState.getLastOpenedFilePath().getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    fileChooser.setInitialDirectory(parentDir);
                }
            }
            
            // Definir nome do arquivo inicial
            if (appState.getLastOpenedFilePath() != null) {
                fileChooser.setInitialFileName(appState.getLastOpenedFilePath().getName());
            }
            
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                dimIOController.saveDimToFile(file);
                appState.setLastOpenedFilePath(file);
                hasUnsavedChanges = false;
                updateWindowTitle();
            }
        } else {
            // Lógica para "Save" (sobrescrever o arquivo atual)
            if (appState.getLastOpenedFilePath() != null) {
                dimIOController.saveDimToFile(appState.getLastOpenedFilePath());
                hasUnsavedChanges = false;
                updateWindowTitle();
            } else {
                // Se não houver arquivo aberto, comporta-se como "Save As"
                saveButton(true);
            }
        }
    }

    private void clearState() {
        appState.setSelectedBackgroundIndex(0);
        characterViewController.clearState();
        bemSystemViewController.clearState();
        dimSystemViewController.clearState();
        hasUnsavedChanges = false;
        refreshAll();
    }
    
    /**
     * Atualiza o título da janela baseado no arquivo aberto
     */
    public void updateWindowTitle() {
        String title = "DIM Modifier";
        
        if (appState.getLastOpenedFilePath() != null) {
            String fileName = appState.getLastOpenedFilePath().getName();
            title += " - [" + fileName + "]";
            
            if (hasUnsavedChanges) {
                title += " - Unsaved";
            }
        } else if (appState.getCardData() != null) {
            title += " - Unsaved";
        }
        
        primaryStage.setTitle(title);
    }
    
    /**
     * Marca que há alterações não salvas e atualiza o título
     */
    public void markUnsavedChanges() {
        if (!hasUnsavedChanges) {
            hasUnsavedChanges = true;
            updateWindowTitle();
        }
    }
    
    private void exportAll(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Export Directory");
        try {
            // Set initial directory to user home by default to avoid issues with spaces
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File directory = directoryChooser.showDialog(null);
            
            if (directory != null) {
                // Criar um diretório com o nome do binário aberto
                String dimName = getDimName();
                File dimDirectory = new File(directory, dimName);
                if (!dimDirectory.exists()) {
                    boolean created = dimDirectory.mkdir();
                    if (!created) {
                        log.error("Failed to create directory: {}", dimDirectory.getAbsolutePath());
                        return;
                    }
                }
                
                exportSpritesToDirectory(dimDirectory);
                exportDataToDirectory(dimDirectory);
                log.info("Export All completed to {}", dimDirectory.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error during export: ", e);
        }
    }
    
    private void exportSprites(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Export Directory for Sprites");
        try {
            // Set initial directory to user home by default to avoid issues with spaces
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File directory = directoryChooser.showDialog(null);
            
            if (directory != null) {
                // Criar um diretório com o nome do binário aberto
                String dimName = getDimName();
                File dimDirectory = new File(directory, dimName);
                if (!dimDirectory.exists()) {
                    boolean created = dimDirectory.mkdir();
                    if (!created) {
                        log.error("Failed to create directory: {}", dimDirectory.getAbsolutePath());
                        return;
                    }
                }
                
                exportSpritesToDirectory(dimDirectory);
                log.info("Export Sprites completed to {}", dimDirectory.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error during sprite export: ", e);
        }
    }
    
    private void exportData(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Export Directory for Data");
        try {
            // Set initial directory to user home by default to avoid issues with spaces
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File directory = directoryChooser.showDialog(null);
            
            if (directory != null) {
                // Criar um diretório com o nome do binário aberto
                String dimName = getDimName();
                File dimDirectory = new File(directory, dimName);
                if (!dimDirectory.exists()) {
                    boolean created = dimDirectory.mkdir();
                    if (!created) {
                        log.error("Failed to create directory: {}", dimDirectory.getAbsolutePath());
                        return;
                    }
                }
                
                exportDataToDirectory(dimDirectory);
                log.info("Export Data completed to {}", dimDirectory.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error during data export: ", e);
        }
    }
    
    /**
     * Obtém o nome do arquivo DIM aberto para usar como nome da pasta de exportação
     */
    private String getDimName() {
        if (appState.getLastOpenedFilePath() != null) {
            String fileName = appState.getLastOpenedFilePath().getName();
            // Remover a extensão do arquivo
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                return fileName.substring(0, dotIndex);
            }
            return fileName;
        }
        
        // Usar o ID do DIM se não tiver um nome de arquivo
        if (appState.getCardData() != null && appState.getCardData().getMetaData() != null) {
            return "DIM_" + appState.getCardData().getMetaData().getId();
        }
        
        // Fallback se não houver nem arquivo nem ID
        return "DIM_export_" + System.currentTimeMillis();
    }
    
    private void exportSpritesToDirectory(File directory) {
        try {
            // Criar diretório para sprites se não existir
            File spritesDir = new File(directory, "sprites");
            if (!spritesDir.exists()) {
                spritesDir.mkdir();
            }
            
            log.info("Exporting sprites to {}", spritesDir.getAbsolutePath());
            
            if (appState.getCardData() != null) {
                // 1. Criar diretório para sprites de personagens
                File charactersDir = new File(spritesDir, "characters");
                if (!charactersDir.exists()) {
                    charactersDir.mkdir();
                }
                
                // Exportar sprites de cada personagem
                for (int charIndex = 0; charIndex < appState.getCardData().getCharacters().size(); charIndex++) {
                    try {
                        com.github.cfogrady.dim.modifier.data.card.Character<?, ?> character = appState.getCardData().getCharacters().get(charIndex);
                        
                        // Número do arquivo baseado no índice sequencial (número do digimon) - inicia em 00
                        int characterNumber = charIndex;
                        String characterPrefix = String.format("character_%02d", characterNumber);
                        File characterSpriteSheetFile = new File(charactersDir, characterPrefix + "_spritesheet.png");
                        
                        // Verificar e logar o número de sprites
                        log.info("Character {} (Number: {}, ID: {}) has {} sprites", 
                            charIndex, characterNumber, character.getId(), character.getSprites().size());
                        
                        // Exportar sprites individuais primeiro (isso sempre deve funcionar)
                        File characterSpritesDir = new File(charactersDir, characterPrefix);
                        // Se o diretório já existir, remova-o para garantir a sobrescrita completa
                        if (characterSpritesDir.exists()) {
                            for (File file : characterSpritesDir.listFiles()) {
                                file.delete();
                            }
                        } else {
                            characterSpritesDir.mkdir();
                        }
                        
                        // Exporta cada sprite individualmente
                        for (int i = 0; i < character.getSprites().size(); i++) {
                            SpriteData.Sprite sprite = character.getSprites().get(i);
                            if (sprite != null) {
                                File spriteFile = new File(characterSpritesDir, String.format("sprite_%02d.png", i));
                                try {
                                    // Usar o método existente de SpriteImageTranslator
                                    BufferedImage image = SpriteImageTranslator.createBufferedImage(sprite);
                                    writeImageToFile(image, spriteFile);
                                    log.info("Exported sprite {} for character ID {}", i, character.getId());
                                } catch (Exception e) {
                                    log.error("Error exporting sprite {} for character ID {}", i, character.getId(), e);
                                }
                            }
                        }
                        
                        // Tentar exportar sprite sheet
                        try {
                            // Determinar se é personagem baby ou normal
                            boolean isBaby = character.getStage() == 1 || character.getStage() == 0;
                            
                            if (isBaby) {
                                log.info("Exporting baby character sprite sheet for ID {}", character.getId());
                                try {
                                    // Implementar a lógica de exportação de baby sprite sheet diretamente
                                    if (character.getSprites().size() <= 7) {
                                        List<BufferedImage> images = new ArrayList<>();
                                        for (int i = 0; i < Math.min(character.getSprites().size(), 7); i++) {
                                            SpriteData.Sprite sprite = character.getSprites().get(i);
                                            if (sprite != null) {
                                                images.add(SpriteImageTranslator.createBufferedImage(sprite));
                                            } else {
                                                images.add(createBlankSprite());
                                            }
                                        }
                                        
                                        // Garantir que temos pelo menos 7 imagens
                                        while (images.size() < 7) {
                                            images.add(createBlankSprite());
                                        }
                                        
                                        BufferedImage backgroundImage = createPinkBackground();
                                        Graphics background = backgroundImage.getGraphics();
                                        addBlanks(background);
                                        
                                        // Desenhar os sprites baby nas posições corretas (ignorando sprite_00.png se necessário)
                                        if (images.size() > 1) drawNormalSpriteImage(images.get(1), background, 1, 1);    // sprite_01.png
                                        if (images.size() > 2) drawNormalSpriteImage(images.get(2), background, 66, 1);   // sprite_02.png
                                        if (images.size() > 3) drawNormalSpriteImage(images.get(3), background, 131, 1);  // sprite_03.png
                                        if (images.size() > 4) drawNormalSpriteImage(images.get(4), background, 1, 115);  // sprite_04.png
                                        if (images.size() > 5) drawNormalSpriteImage(images.get(5), background, 66, 115); // sprite_05.png
                                        
                                        // Sprite de fundo, se disponível (sprite_06.png)
                                        if (images.size() > 6) {
                                            background.drawImage(images.get(6), 261, 1, null);
                                        }
                                        
                                        writeImageToFile(backgroundImage, characterSpriteSheetFile);
                                        log.info("Successfully exported baby sprite sheet to {}", characterSpriteSheetFile.getAbsolutePath());
                                    }
                                } catch (Exception e) {
                                    log.error("Error exporting baby sprite sheet: {}", e.getMessage(), e);
                                }
                            } else {
                                log.info("Exporting normal character sprite sheet for ID {}", character.getId());
                                
                                // Se tem pelo menos 13 sprites, exporta normalmente
                                if (character.getSprites().size() >= 14) {
                                    // Usar o método existente de SpriteImageTranslator
                                    List<BufferedImage> images = new ArrayList<>();
                                    for (int i = 1; i < 14; i++) { // Pegar índices 1-13, ignorando o 0
                                        SpriteData.Sprite sprite = character.getSprites().get(i);
                                        if (sprite != null) {
                                            images.add(SpriteImageTranslator.createBufferedImage(sprite));
                                        } else {
                                            images.add(createBlankSprite());
                                        }
                                    }
                                    
                                    BufferedImage backgroundImage = createPinkBackground();
                                    Graphics background = backgroundImage.getGraphics();
                                    
                                    // Desenhar os sprites nas posições padrão
                                    drawNormalSpriteImage(images.get(0), background, 1, 1);     // sprite_01.png
                                    drawNormalSpriteImage(images.get(1), background, 66, 1);    // sprite_02.png
                                    drawNormalSpriteImage(images.get(2), background, 131, 1);   // sprite_03.png
                                    drawNormalSpriteImage(images.get(3), background, 196, 1);   // sprite_04.png
                                    drawNormalSpriteImage(images.get(4), background, 1, 58);    // sprite_05.png
                                    drawNormalSpriteImage(images.get(5), background, 66, 58);   // sprite_06.png
                                    drawNormalSpriteImage(images.get(6), background, 131, 58);  // sprite_07.png
                                    drawNormalSpriteImage(images.get(7), background, 196, 58);  // sprite_08.png
                                    drawNormalSpriteImage(images.get(8), background, 1, 115);   // sprite_09.png
                                    drawNormalSpriteImage(images.get(9), background, 66, 115);  // sprite_10.png
                                    drawNormalSpriteImage(images.get(10), background, 131, 115); // sprite_11.png
                                    drawNormalSpriteImage(images.get(11), background, 196, 115); // sprite_12.png
                                    
                                    // Adiciona o sprite de fundo se disponível (sprite_13.png)
                                    background.drawImage(images.get(12), 261, 1, null);
                                    
                                    writeImageToFile(backgroundImage, characterSpriteSheetFile);
                                } else {
                                    // Se não tem sprites suficientes, usa uma versão adaptada
                                    log.info("Character has less than 14 sprites, adapting export for ID {}", character.getId());
                                    
                                    List<BufferedImage> images = new ArrayList<>();
                                    // Ignorar o sprite_00.png e começar do sprite_01.png em diante
                                    for (int i = 1; i < character.getSprites().size(); i++) {
                                        SpriteData.Sprite sprite = character.getSprites().get(i);
                                        if (sprite != null) {
                                            images.add(SpriteImageTranslator.createBufferedImage(sprite));
                                        } else {
                                            images.add(createBlankSprite());
                                        }
                                    }
                                    
                                    // Garantir que temos pelo menos imagens suficientes para o layout
                                    while (images.size() < 12) {
                                        images.add(createBlankSprite());
                                    }
                                    
                                    BufferedImage backgroundImage = createPinkBackground();
                                    Graphics background = backgroundImage.getGraphics();
                                    
                                    // Desenhar os sprites disponíveis nas posições padrão
                                    int positions = Math.min(images.size(), 12); // Limitado a 12 posições na grade
                                    for (int i = 0; i < positions; i++) {
                                        int col = i % 4;
                                        int row = i / 4;
                                        int x = 1 + col * 65;
                                        int y = 1 + row * 57;
                                        drawNormalSpriteImage(images.get(i), background, x, y);
                                    }
                                    
                                    // Sprite de fundo, se disponível
                                    if (character.getSprites().size() > 13 && character.getSprites().get(13) != null) {
                                        BufferedImage backgroundSprite = SpriteImageTranslator.createBufferedImage(character.getSprites().get(13));
                                        background.drawImage(backgroundSprite, 261, 1, null);
                                    }
                                    
                                    writeImageToFile(backgroundImage, characterSpriteSheetFile);
                                }
                            }
                            log.info("Successfully exported sprite sheet to {}", characterSpriteSheetFile.getAbsolutePath());
                        } catch (Exception e) {
                            log.error("Error exporting sprite sheet for character ID {}", character.getId(), e);
                        }
                    } catch (Exception e) {
                        log.error("Error processing character at index {}", charIndex, e);
                    }
                }
                
                // 2. Exportar sprites do sistema
                File systemDir = new File(spritesDir, "system");
                if (!systemDir.exists()) {
                    systemDir.mkdir();
                }
                
                // 2.1 Exportar backgrounds
                File backgroundsDir = new File(systemDir, "backgrounds");
                if (!backgroundsDir.exists()) {
                    backgroundsDir.mkdir();
                }
                
                List<SpriteData.Sprite> backgrounds = appState.getCardData().getCardSprites().getBackgrounds();
                for (int i = 0; i < backgrounds.size(); i++) {
                    SpriteData.Sprite sprite = backgrounds.get(i);
                    File spriteFile = new File(backgroundsDir, String.format("background_%02d.png", i));
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(sprite), spriteFile);
                }
                
                // 2.2 Exportar tipos/atributos
                File typesDir = new File(systemDir, "types");
                if (!typesDir.exists()) {
                    typesDir.mkdir();
                }
                
                List<SpriteData.Sprite> types = appState.getCardData().getCardSprites().getTypes();
                for (int i = 0; i < types.size(); i++) {
                    SpriteData.Sprite sprite = types.get(i);
                    File spriteFile = new File(typesDir, String.format("type_%02d.png", i));
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(sprite), spriteFile);
                }
                
                // 2.3 Exportar ataques pequenos
                File smallAttacksDir = new File(systemDir, "small_attacks");
                if (!smallAttacksDir.exists()) {
                    smallAttacksDir.mkdir();
                }
                
                List<SpriteData.Sprite> smallAttacks = appState.getCardData().getCardSprites().getSmallAttacks();
                for (int i = 0; i < smallAttacks.size(); i++) {
                    SpriteData.Sprite sprite = smallAttacks.get(i);
                    File spriteFile = new File(smallAttacksDir, String.format("small_attack_%02d.png", i));
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(sprite), spriteFile);
                }
                
                // 2.4 Exportar ataques grandes
                File bigAttacksDir = new File(systemDir, "big_attacks");
                if (!bigAttacksDir.exists()) {
                    bigAttacksDir.mkdir();
                }
                
                List<SpriteData.Sprite> bigAttacks = appState.getCardData().getCardSprites().getBigAttacks();
                for (int i = 0; i < bigAttacks.size(); i++) {
                    SpriteData.Sprite sprite = bigAttacks.get(i);
                    File spriteFile = new File(bigAttacksDir, String.format("big_attack_%02d.png", i));
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(sprite), spriteFile);
                }
                
                // 2.5 Outros sprites do sistema (logo, egg, etc.)
                File otherSpritesDir = new File(systemDir, "other");
                if (!otherSpritesDir.exists()) {
                    otherSpritesDir.mkdir();
                }
                
                // Logo
                if (appState.getCardData().getCardSprites().getLogo() != null) {
                    File spriteFile = new File(otherSpritesDir, "logo.png");
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(
                            appState.getCardData().getCardSprites().getLogo()), spriteFile);
                }
                
                // Egg sprites
                List<SpriteData.Sprite> eggSprites = appState.getCardData().getCardSprites().getEgg();
                for (int i = 0; i < eggSprites.size(); i++) {
                    SpriteData.Sprite sprite = eggSprites.get(i);
                    File spriteFile = new File(otherSpritesDir, String.format("egg_%02d.png", i));
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(sprite), spriteFile);
                }
                
                // Outros sprites individuais
                if (appState.getCardData().getCardSprites().getReady() != null) {
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(
                            appState.getCardData().getCardSprites().getReady()), 
                            new File(otherSpritesDir, "ready.png"));
                }
                
                if (appState.getCardData().getCardSprites().getGo() != null) {
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(
                            appState.getCardData().getCardSprites().getGo()), 
                            new File(otherSpritesDir, "go.png"));
                }
                
                if (appState.getCardData().getCardSprites().getWin() != null) {
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(
                            appState.getCardData().getCardSprites().getWin()), 
                            new File(otherSpritesDir, "win.png"));
                }
                
                if (appState.getCardData().getCardSprites().getLose() != null) {
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(
                            appState.getCardData().getCardSprites().getLose()), 
                            new File(otherSpritesDir, "lose.png"));
                }
                
                // Hits
                List<SpriteData.Sprite> hitsSprites = appState.getCardData().getCardSprites().getHits();
                for (int i = 0; i < hitsSprites.size(); i++) {
                    SpriteData.Sprite sprite = hitsSprites.get(i);
                    File spriteFile = new File(otherSpritesDir, String.format("hit_%02d.png", i));
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(sprite), spriteFile);
                }
                
                // Stages
                List<SpriteData.Sprite> stagesSprites = appState.getCardData().getCardSprites().getStages();
                for (int i = 0; i < stagesSprites.size(); i++) {
                    SpriteData.Sprite sprite = stagesSprites.get(i);
                    File spriteFile = new File(otherSpritesDir, String.format("stage_%02d.png", i));
                    writeImageToFile(SpriteImageTranslator.createBufferedImage(sprite), spriteFile);
                }
            }
        } catch (Exception e) {
            log.error("Error exporting sprites", e);
        }
    }
    
    // Métodos auxiliares para desenhar as sprite sheets
    private BufferedImage createPinkBackground() {
        BufferedImage backgroundImage = new BufferedImage(342, 172, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = backgroundImage.createGraphics();
        
        // Usar fundo rosa claro para a sprite sheet
        g2d.setColor(new java.awt.Color(255, 0, 255)); // Pink magenta
        
        g2d.fillRect(0, 0, backgroundImage.getWidth(), backgroundImage.getHeight());
        return backgroundImage;
    }
    
    private BufferedImage createBlankSprite() {
        BufferedImage blank = new BufferedImage(64, 56, BufferedImage.TYPE_INT_RGB);
        Graphics g = blank.getGraphics();
        g.setColor(java.awt.Color.GREEN); // Verde para os sprites em branco/transparentes
        g.fillRect(0, 0, blank.getWidth(), blank.getHeight());
        g.dispose();
        return blank;
    }

    private void addBlanks(Graphics background) {
        BufferedImage blank = createBlankSprite();
        drawNormalSpriteImage(blank, background, 1, 1);
        drawNormalSpriteImage(blank, background, 66, 1);
        drawNormalSpriteImage(blank, background, 131, 1);
        drawNormalSpriteImage(blank, background, 196, 1);
        drawNormalSpriteImage(blank, background, 1, 58);
        drawNormalSpriteImage(blank, background, 66, 58);
        drawNormalSpriteImage(blank, background, 131, 58);
        drawNormalSpriteImage(blank, background, 196, 58);
        drawNormalSpriteImage(blank, background, 1, 115);
        drawNormalSpriteImage(blank, background, 66, 115);
        drawNormalSpriteImage(blank, background, 131, 115);
        drawNormalSpriteImage(blank, background, 196, 115);
    }

    private void drawNormalSpriteImage(BufferedImage from, Graphics background, int x, int y) {
        int relativeX = (64 - from.getWidth())/2;
        int relativeY = 56 - from.getHeight();
        background.drawImage(from, relativeX + x, relativeY + y, null);
    }
    
    private void writeImageToFile(BufferedImage image, File file) {
        try {
            // Ensure parent directory exists
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    log.error("Failed to create parent directory: {}", parentDir.getAbsolutePath());
                    return;
                }
            }
            
            // Write the image
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            log.error("Failed to write image to file: {}", file.getAbsolutePath(), e);
        } catch (Exception e) {
            log.error("Unexpected error writing to file: {}", file.getAbsolutePath(), e);
        }
    }
    
    private void exportDataToDirectory(File directory) {
        try {
            // Criar diretório para dados JSON se não existir
            File dataDir = new File(directory, "data");
            if (!dataDir.exists()) {
                boolean dirCreated = dataDir.mkdirs(); // Use mkdirs instead of mkdir to create parent directories if needed
                if (!dirCreated) {
                    log.error("Failed to create directory: {}", dataDir.getAbsolutePath());
                    return;
                }
            }
            
            log.info("Exporting data to {}", dataDir.getAbsolutePath());
            
            // Exportar dados dos personagens diretamente na pasta data
            if (appState.getCardData() != null) {
                // Limpar arquivos existentes para garantir a sobrescrita completa
                File[] existingFiles = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
                if (existingFiles != null) {
                    for (File file : existingFiles) {
                        boolean deleted = file.delete();
                        if (!deleted) {
                            log.warn("Failed to delete existing file: {}", file.getAbsolutePath());
                        }
                    }
                }

                // Configurar o ObjectMapper para formatar o JSON conforme o exemplo
                // Importante: Incluir todos os campos, mesmo que nulos
                ObjectMapper characterMapper = new ObjectMapper()
                    .configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                    .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
                    .configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, true)
                    .disable(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_SELF_REFERENCES);

                for (int i = 0; i < appState.getCardData().getCharacters().size(); i++) {
                    try {
                        com.github.cfogrady.dim.modifier.data.card.Character<?, ?> character = appState.getCardData().getCharacters().get(i);
                        // Número do personagem começa em 0 em vez de 1
                        int characterNumber = i;
                        File characterFile = new File(dataDir, String.format("character_%02d.json", characterNumber));
                        
                        // Criar um LinkedHashMap para preservar a ordem dos campos
                        java.util.Map<String, Object> characterData = new java.util.LinkedHashMap<>();
                        
                        // Adicionar campos na ordem específica conforme o exemplo
                        characterData.put("id", i); // Usar o índice como ID (base-0)
                        
                        // Tipo do personagem (DIM ou BEM)
                        if (character instanceof BemCharacter) {
                            characterData.put("type", "BEM");
                        } else if (character instanceof DimCharacter) {
                            characterData.put("type", "DIM");
                        }
                        
                        characterData.put("stage", character.getStage());
                        characterData.put("attribute", character.getAttribute());
                        characterData.put("bigAttack", character.getBigAttack());
                        characterData.put("activityType", character.getActivityType());
                        characterData.put("smallAttack", character.getSmallAttack());
                        characterData.put("hp", character.getHp());
                        
                        // Campos específicos para DIM
                        if (character instanceof DimCharacter) {
                            DimCharacter dimCharacter = (DimCharacter) character;
                            characterData.put("stars", dimCharacter.getStars());
                        } else {
                            // Adicionar campo vazio para manter a estrutura consistente
                            characterData.put("stars", 0);
                        }
                        
                        characterData.put("bp", character.getBp());
                        characterData.put("ap", character.getAp());
                        
                        // Campos específicos para DIM - mesmo para não-DIM, adicionar para manter a estrutura
                        if (character instanceof DimCharacter) {
                            DimCharacter dimCharacter = (DimCharacter) character;
                            characterData.put("finishAdventureToUnlock", dimCharacter.isFinishAdventureToUnlock());
                        } else {
                            characterData.put("finishAdventureToUnlock", false);
                        }
                        
                        // Adicionar pools de batalha - garantir que estão presentes mesmo se vazios
                        characterData.put("firstPoolBattleChance", character.getFirstPoolBattleChance() != null ? 
                            character.getFirstPoolBattleChance() : 0);
                        characterData.put("secondPoolBattleChance", character.getSecondPoolBattleChance() != null ? 
                            character.getSecondPoolBattleChance() : 0);
                        
                        if (character instanceof BemCharacter) {
                            BemCharacter bemCharacter = (BemCharacter) character;
                            characterData.put("thirdPoolBattleChance", bemCharacter.getThirdPoolBattleChance() != null ? 
                                bemCharacter.getThirdPoolBattleChance() : 0);
                        } else {
                            // Adicionar campo vazio para não-BEM para manter estrutura
                            characterData.put("thirdPoolBattleChance", 0);
                        }
                        
                        // Transformações
                        java.util.List<java.util.Map<String, Object>> transformations = new java.util.ArrayList<>();
                        for (Object entry : character.getTransformationEntries()) {
                            java.util.LinkedHashMap<String, Object> transformData = new java.util.LinkedHashMap<>();
                            try {
                                // Métodos comuns em todas as transformações
                                TransformationEntry transformEntry = (TransformationEntry) entry;
                                
                                // Capturar o ID do personagem de destino
                                UUID evolveToId = transformEntry.getToCharacter();
                                if (evolveToId != null) {
                                    // Encontrar o índice do personagem de destino
                                    Integer evolveToIndex = appState.getCardData().getUuidToCharacterSlot().get(evolveToId);
                                    if (evolveToIndex != null) {
                                        transformData.put("evolveTo", evolveToIndex); // Usar índice base-0
                                    } else {
                                        transformData.put("evolveTo", 0); // Valor padrão se não encontrado
                                    }
                                } else {
                                    transformData.put("evolveTo", 0); // Valor padrão se não especificado
                                }
                                
                                transformData.put("battlesRequirement", transformEntry.getBattleRequirement());
                                
                                // Capturar valores específicos por tipo
                                if (entry instanceof BemTransformationEntry) {
                                    BemTransformationEntry bemEntry = (BemTransformationEntry) entry;
                                    transformData.put("minutesUntilTransformation", bemEntry.getMinutesUntilTransformation());
                                    transformData.put("transformationType", "BEM");
                                    // Adicionar campo de horas como 0 para manter estrutura consistente
                                    transformData.put("hoursUntilTransformation", 0);
                                } else if (entry instanceof DimTransformationEntity) {
                                    DimTransformationEntity dimEntry = (DimTransformationEntity) entry;
                                    transformData.put("hoursUntilTransformation", dimEntry.getHoursUntilTransformation());
                                    transformData.put("transformationType", "DIM");
                                    // Adicionar campo de minutos como 0 para manter estrutura consistente
                                    transformData.put("minutesUntilTransformation", 0);
                                } else {
                                    // Tipo desconhecido, adicionar valores padrão
                                    transformData.put("hoursUntilTransformation", 0);
                                    transformData.put("minutesUntilTransformation", 0);
                                    transformData.put("transformationType", "UNKNOWN");
                                }
                                
                                transformData.put("vitalityRequirement", transformEntry.getVitalRequirements());
                                transformData.put("winRatioRequirement", transformEntry.getWinRatioRequirement());
                                transformData.put("ppRequirement", transformEntry.getTrophyRequirement());
                                
                                transformations.add(transformData);
                            } catch (Exception e) {
                                log.error("Failed to extract transformation data: {}", e.getMessage(), e);
                            }
                        }
                        characterData.put("transformations", transformations);
                        
                        // Attribute Fusions - sempre incluir mesmo se vazio
                        java.util.Map<String, Object> attributeFusions = new java.util.LinkedHashMap<>();
                        
                        if (character.getFusions() != null) {
                            if (character.getFusions().getType1FusionResult() != null) {
                                Integer fusionIndex = appState.getCardData().getUuidToCharacterSlot().get(character.getFusions().getType1FusionResult());
                                if (fusionIndex != null) {
                                    attributeFusions.put("type1", fusionIndex); // Base-0
                                } else {
                                    attributeFusions.put("type1", 0);
                                }
                            } else {
                                attributeFusions.put("type1", 0);
                            }
                            
                            if (character.getFusions().getType2FusionResult() != null) {
                                Integer fusionIndex = appState.getCardData().getUuidToCharacterSlot().get(character.getFusions().getType2FusionResult());
                                if (fusionIndex != null) {
                                    attributeFusions.put("type2", fusionIndex); // Base-0
                                } else {
                                    attributeFusions.put("type2", 0);
                                }
                            } else {
                                attributeFusions.put("type2", 0);
                            }
                            
                            if (character.getFusions().getType3FusionResult() != null) {
                                Integer fusionIndex = appState.getCardData().getUuidToCharacterSlot().get(character.getFusions().getType3FusionResult());
                                if (fusionIndex != null) {
                                    attributeFusions.put("type3", fusionIndex); // Base-0
                                } else {
                                    attributeFusions.put("type3", 0);
                                }
                            } else {
                                attributeFusions.put("type3", 0);
                            }
                            
                            if (character.getFusions().getType4FusionResult() != null) {
                                Integer fusionIndex = appState.getCardData().getUuidToCharacterSlot().get(character.getFusions().getType4FusionResult());
                                if (fusionIndex != null) {
                                    attributeFusions.put("type4", fusionIndex); // Base-0
                                } else {
                                    attributeFusions.put("type4", 0);
                                }
                            } else {
                                attributeFusions.put("type4", 0);
                            }
                        } else {
                            // Se não houver fusões, adicionar valores padrão
                            attributeFusions.put("type1", 0);
                            attributeFusions.put("type2", 0);
                            attributeFusions.put("type3", 0);
                            attributeFusions.put("type4", 0);
                        }
                        
                        // Adicionar attributeFusions ao characterData
                        characterData.put("attributeFusions", attributeFusions);
                        
                        // Specific Fusions - sempre representar como objeto
                        java.util.Map<String, Object> specificFusions = new java.util.LinkedHashMap<>();
                        
                        // Adicionar fusões específicas ao objeto se existirem
                        if (character.getSpecificFusions() != null && !character.getSpecificFusions().isEmpty()) {
                            for (int index = 0; index < character.getSpecificFusions().size(); index++) {
                                com.github.cfogrady.dim.modifier.data.card.SpecificFusion specificFusion = character.getSpecificFusions().get(index);
                                java.util.Map<String, Object> fusionData = new java.util.LinkedHashMap<>();
                                
                                // Support DIM ID
                                fusionData.put("supportDimId", specificFusion.getPartnerDimId());
                                
                                // Support Character
                                Integer supportCharacter = specificFusion.getPartnerDimSlotId() != null ? 
                                    specificFusion.getPartnerDimSlotId() : 0;
                                fusionData.put("supportCharacter", supportCharacter);
                                
                                // Evolve To
                                Integer evolveTo = 0;
                                if (specificFusion.getEvolveToCharacterId() != null) {
                                    Integer evolveToIndex = appState.getCardData().getUuidToCharacterSlot().get(specificFusion.getEvolveToCharacterId());
                                    if (evolveToIndex != null) {
                                        evolveTo = evolveToIndex; // Base-0
                                    }
                                }
                                fusionData.put("evolveTo", evolveTo);
                                
                                // Adicionar ao mapa com chave igual ao índice
                                specificFusions.put(Integer.toString(index), fusionData);
                            }
                        }
                        
                        // Adicionar specific fusions
                        characterData.put("specificFusions", specificFusions);
                        
                        // Usar writer com configuração para controlar a formatação e indentação
                        String json = characterMapper.writerWithDefaultPrettyPrinter().writeValueAsString(characterData);
                        
                        // Escrever o arquivo JSON
                        try (FileWriter writer = new FileWriter(characterFile)) {
                            writer.write(json);
                        }
                        
                        log.info("Exported character data for number: {}", characterNumber);
                    } catch (Exception e) {
                        log.error("Error exporting character data at index {}: {}", i, e.getMessage(), e);
                    }
                }
            } else {
                log.error("Cannot export data: cardData is null");
            }
        } catch (Exception e) {
            log.error("Error exporting data: {}", e.getMessage(), e);
        }
    }
    
    public void refreshAll() {
        if (appState.getCardData() != null) {
            MetaData metaData = appState.getCardData().getMetaData();
            dimIdText.setText("DIM ID: " + metaData.getId());
            revisionIdText.setText("Revision: " + metaData.getRevision());
            factoryDateText.setText("Factory Date: " + metaData.getYear() + "/" + metaData.getMonth() + "/" + metaData.getDay());
            checksumText.setText("Checksum At Load: " + Integer.toHexString(metaData.getOriginalChecksum()));
            refreshSubview();
        }
    }

    public void refreshButtons() {
        // Método não necessário com TabPane, mas mantido para compatibilidade
    }

    public void refreshSubview() {
        // Garantir que a aba selecionada corresponde à subview atual
        try {
            switch (subViewSelection) {
                case CHARACTERS -> tabPane.getSelectionModel().select(charactersTab);
                case BATTLES -> tabPane.getSelectionModel().select(battlesTab);
                case SYSTEM -> tabPane.getSelectionModel().select(systemTab);
            }
        } catch (Exception e) {
            log.error("Error updating selected tab: {}", e.getMessage(), e);
        }
        
        updateSubViews();
    }
    
    private void updateSubViews() {
        try {
            // Limpar todos os panes
            charactersSubViewPane.getChildren().clear();
            battlesSubViewPane.getChildren().clear();
            systemSubViewPane.getChildren().clear();
            
            // Adicionar as views relevantes aos painéis apropriados
            charactersSubViewPane.getChildren().add(charactersSubView);
            battlesSubViewPane.getChildren().add(battlesSubView);
            
            // Sistema depende do tipo de cartão
            if(appState.getCardData() instanceof BemCardData) {
                systemSubViewPane.getChildren().add(bemSystemSubView);
            } else if(appState.getCardData() instanceof DimCardData) {
                systemSubViewPane.getChildren().add(dimSystemSubView);
            }
            
            // Atualizar as subviews baseado na seleção atual
            switch (subViewSelection) {
                case CHARACTERS -> characterViewController.refreshAll();
                case BATTLES -> battlesViewController.refreshAll();
                case SYSTEM -> {
                    if(appState.getCardData() instanceof BemCardData) {
                        bemSystemViewController.refreshAll();
                    } else if(appState.getCardData() instanceof DimCardData) {
                        dimSystemViewController.refreshAll();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error updating subviews: {}", e.getMessage(), e);
        }
    }

    private Node getSubview() {
        // Este método não é mais necessário com a nova abordagem de abas, 
        // mas mantido para compatibilidade com código existente
        switch (subViewSelection) {
            case CHARACTERS -> {
                characterViewController.refreshAll();
                return charactersSubView;
            }
            case BATTLES -> {
                battlesViewController.refreshAll();
                return battlesSubView;
            }
            case SYSTEM -> {
                if(appState.getCardData() instanceof BemCardData) {
                    bemSystemViewController.refreshAll();
                    return bemSystemSubView;
                } else if(appState.getCardData() instanceof DimCardData) {
                    dimSystemViewController.refreshAll();
                    return dimSystemSubView;
                } else {
                    throw new IllegalArgumentException("Cannot load system view for unknown card type " + appState.getCardData().getClass().getName());
                }
            }
            default -> {
                return null;
            }
        }
    }

    // Converte os pixels para o formato R5G6B5
    private byte[] convertToR5G6B5(PixelReader pixelReader, int width, int height) {
        byte[] result = new byte[width * height * 2];
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                int index = (y * width + x) * 2;
                Color color = pixelReader.getColor(x, y);
                int red = (int)(color.getRed() * 31) & 0x1F;
                int green = (int)(color.getGreen() * 63) & 0x3F;
                int blue = (int)(color.getBlue() * 31) & 0x1F;
                int rgb565 = (red << 11) | (green << 5) | blue;
                result[index] = (byte)(rgb565 & 0xFF);
                result[index+1] = (byte)((rgb565 >> 8) & 0xFF);
            }
        }
        return result;
    }
}
