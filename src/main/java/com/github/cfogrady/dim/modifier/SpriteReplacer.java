package com.github.cfogrady.dim.modifier;

import com.github.cfogrady.dim.modifier.data.AppState;
import com.github.cfogrady.vb.dim.sprite.SpriteData;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

@Slf4j
@RequiredArgsConstructor
public class SpriteReplacer {
    private final AppState appState;
    private final Stage stage;
    private final SpriteImageTranslator spriteImageTranslator;

    public SpriteData.Sprite replaceSprite(Integer expectedWidth, Integer expectedHeight, File file) {
        if(file == null) {
            return null;
        }
        SpriteData.Sprite newSprite = loadSpriteFromFile(file);
        boolean validReplacement = true;
        if(expectedWidth != null && expectedWidth != newSprite.getWidth()) {
            validReplacement = false;
        }
        if(expectedHeight != null && expectedHeight != newSprite.getHeight()) {
            validReplacement = false;
        }
        if(validReplacement) {
            return newSprite;
        }
        log.warn("Selected sprite doesn't match expected dimensions");
        return null;
    }

    public SpriteData.Sprite loadSpriteFromFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select sprite replacement.");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image format", "*.png", "*.bmp"));
        if(appState.getLastOpenedFilePath() != null) {
            File parentDir = appState.getLastOpenedFilePath().getParentFile();
            if(parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
                try {
                    fileChooser.setInitialDirectory(parentDir);
                } catch (IllegalArgumentException e) {
                    log.warn("Could not set initial directory: {}", parentDir.getAbsolutePath(), e);
                    // Use home directory as fallback
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                }
            }
        }
        File file = fileChooser.showOpenDialog(stage);
        if(file != null) {
            File parentDir = file.getParentFile();
            if(parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
                appState.setLastOpenedFilePath(parentDir);
            }
            return loadSpriteFromFile(file);
        }
        return null;
    }

    public SpriteData.Sprite replaceSprite(SpriteData.Sprite sprite, boolean sameWidth, boolean sameHeight) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select sprite replacement. Should be " + sprite.getWidth() + " x " + sprite.getHeight());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image format", "*.png", "*.bmp"));
        if(appState.getLastOpenedFilePath() != null) {
            File parentDir = appState.getLastOpenedFilePath().getParentFile();
            if(parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
                try {
                    fileChooser.setInitialDirectory(parentDir);
                } catch (IllegalArgumentException e) {
                    log.warn("Could not set initial directory: {}", parentDir.getAbsolutePath(), e);
                    // Use home directory as fallback
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                }
            }
        }
        File file = fileChooser.showOpenDialog(stage);
        if(file != null) {
            File parentDir = file.getParentFile();
            if(parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
                appState.setLastOpenedFilePath(parentDir);
            }
        }
        return replaceSprite(sameWidth ? sprite.getWidth() : null, sameHeight ? sprite.getHeight() : null, file);
    }

    public SpriteData.Sprite loadSpriteFromFile(File file) {
        return spriteImageTranslator.loadSprite(file);
    }
}
