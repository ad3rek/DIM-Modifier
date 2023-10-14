package com.github.cfogrady.dim.modifier.controllers;

import com.github.cfogrady.dim.modifier.SpriteImageTranslator;
import com.github.cfogrady.dim.modifier.controls.ImageIntComboBox;
import com.github.cfogrady.dim.modifier.controls.ImageIntPair;
import com.github.cfogrady.dim.modifier.controls.IntegerTextField;
import com.github.cfogrady.dim.modifier.data.AppState;
import com.github.cfogrady.dim.modifier.data.bem.BemAdventure;
import com.github.cfogrady.dim.modifier.data.bem.BemCardData;
import com.github.cfogrady.dim.modifier.data.card.Adventure;
import com.github.cfogrady.vb.dim.sprite.SpriteData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Slf4j
public class AdventureGridController {
    private static final Insets INSETS = new Insets(10);

    private final SpriteImageTranslator spriteImageTranslator;
    private final AppState appState;

    @Setter
    private StackPane stackPane;

    @RequiredArgsConstructor
    @Data
    static class ImageOptions {
        private final ObservableList<ImageIntPair> characterOptions;
        private final ObservableList<ImageIntPair> characterOptionsWithNoneOption;
        private final ObservableList<ImageIntPair> smallAttackSpriteOptions;
        private final ObservableList<ImageIntPair> bigAttackSpriteOptions;
        private final ObservableList<ImageIntPair> backgroundOptions;
    }

    private ImageOptions buildImageOptions() {
        ObservableList<ImageIntPair> characterOptions = spriteImageTranslator.createImageValuePairs(appState.getIdleForCharacters());

        ObservableList<ImageIntPair> characterOptionsWithNoneOption = FXCollections.observableArrayList(characterOptions);
        characterOptionsWithNoneOption.add(0, new ImageIntPair(null, null));

        ObservableList<ImageIntPair> backgroundOptions = spriteImageTranslator.createImageValuePairs(appState.getCardData().getCardSprites().getBackgrounds());

        List<SpriteData.Sprite> smallAttackSprites = appState.getFirmwareData().getSmallAttacks();
        smallAttackSprites.addAll(appState.getCardData().getCardSprites().getSmallAttacks());
        ObservableList<ImageIntPair> smallAttackSpriteOptions = spriteImageTranslator.createImageValuePairs(smallAttackSprites);
        smallAttackSpriteOptions.add(0, new ImageIntPair(null, null));

        List<SpriteData.Sprite> bigAttackSprites = appState.getFirmwareData().getBigAttacks();
        bigAttackSprites.addAll(appState.getCardData().getCardSprites().getBigAttacks());
        ObservableList<ImageIntPair> bigAttackSpriteOptions = spriteImageTranslator.createImageValuePairs(bigAttackSprites);
        bigAttackSpriteOptions.add(0, new ImageIntPair(null, null));
        return new ImageOptions(characterOptions, characterOptionsWithNoneOption, smallAttackSpriteOptions, bigAttackSpriteOptions, backgroundOptions);
    }

    public void refreshAll() {
        stackPane.getChildren().clear();
        stackPane.getChildren().add(setupAdventureGrid());
    }

    private GridPane setupAdventureGrid() {
        GridPane gridPane = new GridPane();
        gridPane.setGridLinesVisible(true);
        int rowIndex = 1;
        setupHeader(gridPane);
        // cache so that we don't do image conversion 12*(23+23+50+32+6+6) times
        ImageOptions imageOptions = buildImageOptions();
        for(Adventure adventure : appState.getCardData().getAdventures()) {
            addRow(gridPane, adventure, imageOptions, rowIndex++);
        }
        return gridPane;
    }

    private void setupHeader(GridPane gridPane) {
        int columnIndex = 0;
        gridPane.add(getTitleText("Adventure Level"), columnIndex++, 0);
        gridPane.add(getTitleText("Steps"), columnIndex++, 0);
        gridPane.add(getTitleText("Boss"), columnIndex++, 0);
        gridPane.add(getTitleText("Boss BP"), columnIndex++, 0);
        gridPane.add(getTitleText("Boss HP"), columnIndex++, 0);
        gridPane.add(getTitleText("Boss AP"), columnIndex++, 0);
        if(appState.getCardData() instanceof BemCardData) {
            gridPane.add(getTitleText("Boss Small Attack"), columnIndex++, 0);
            gridPane.add(getTitleText("Boss Big Attack"), columnIndex++, 0);
            gridPane.add(getTitleText("Boss Is Secret"), columnIndex++, 0);
            gridPane.add(getTitleText("Reward Character"), columnIndex++, 0);
            gridPane.add(getTitleText("Walking Background"), columnIndex++, 0);
            gridPane.add(getTitleText("Battle Background"), columnIndex++, 0);
        }
    }

    private Text getTitleText(String str) {
        Text text = new Text(str);
        Font defaultFont = text.getFont();
        text.setFont(Font.font(defaultFont.getFamily(), FontWeight.BOLD, defaultFont.getSize()));
        GridPane.setMargin(text, INSETS);
        return text;
    }

    private void addRow(GridPane gridPane, Adventure adventure, ImageOptions imageOptions, int rowIndex) {
        int columnIndex = 0;
        Text adventureLevel = new Text(Integer.toString(rowIndex));
        GridPane.setMargin(adventureLevel, INSETS);
        gridPane.add(adventureLevel, columnIndex++, rowIndex);
        gridPane.add(createIntegerTextField(adventure::getSteps, adventure::setSteps), columnIndex++, rowIndex);
        gridPane.add(createCharacterSelectorComboBox(adventure::getBossId, adventure::setBossId, imageOptions.getCharacterOptions()), columnIndex++, rowIndex);
        gridPane.add(createIntegerTextField(adventure::getBossBp, adventure::setBossBp), columnIndex++, rowIndex);
        gridPane.add(createIntegerTextField(adventure::getBossHp, adventure::setBossHp), columnIndex++, rowIndex);
        gridPane.add(createIntegerTextField(adventure::getBossAp, adventure::setBossAp), columnIndex++, rowIndex);
        if(adventure instanceof BemAdventure bemAdventure) {
            gridPane.add(createCheckbox("Show Boss Preview", bemAdventure::isShowBossIdentiy, bemAdventure::setShowBossIdentiy), columnIndex++, rowIndex);
            gridPane.add(createSmallAttackSelection(bemAdventure, imageOptions.getSmallAttackSpriteOptions()), columnIndex++, rowIndex);
            gridPane.add(createBigAttackSelection(bemAdventure, imageOptions.getBigAttackSpriteOptions()), columnIndex++, rowIndex);
            gridPane.add(createCharacterSelectorComboBox(bemAdventure::getGiftCharacter, bemAdventure::setGiftCharacter, imageOptions.getCharacterOptionsWithNoneOption()), columnIndex++, rowIndex);
            gridPane.add(createBackgroundSelection(bemAdventure::getWalkingBackground, bemAdventure::setWalkingBackground, imageOptions.getBackgroundOptions()), columnIndex++, rowIndex);
            gridPane.add(createBackgroundSelection(bemAdventure::getBattleBackground, bemAdventure::setBattleBackground, imageOptions.getBackgroundOptions()), columnIndex++, rowIndex);
        }
    }

    private CheckBox createCheckbox(String text, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        CheckBox checkBox = new CheckBox(text);
        checkBox.setSelected(getter.get());
        checkBox.setOnMouseClicked(e -> setter.accept(checkBox.isSelected()));
        GridPane.setMargin(checkBox, new Insets(10));
        return checkBox;
    }

    private ImageIntComboBox createCharacterSelectorComboBox(Supplier<UUID> getter, Consumer<UUID> setter, ObservableList<ImageIntPair> characterOptions) {
        ImageIntComboBox imageIntComboBox = new ImageIntComboBox();
        imageIntComboBox.initialize(characterOptions);
        imageIntComboBox.setOnAction(e -> {
            Integer slotId = imageIntComboBox.getValue().getValue();
            if(slotId == null) {
                setter.accept(null);
            } else {
                setter.accept(appState.getCharacter(slotId).getId());
            }
        });
        UUID characterId = getter.get();
        Integer slotId = appState.getCardData().getUuidToCharacterSlot().get(getter.get());
        if(characterId == null) {
            imageIntComboBox.setValue(imageIntComboBox.getItemForValue(null));
        }
        if(slotId != null) {
            imageIntComboBox.setValue(imageIntComboBox.getItemForValue(slotId));
        }
        GridPane.setMargin(imageIntComboBox, INSETS);
        return imageIntComboBox;
    }

    private IntegerTextField createIntegerTextField(Supplier<Integer> valueGetter, Consumer<Integer> valueSetter) {
        IntegerTextField integerTextField = new IntegerTextField(valueGetter.get(), valueSetter);
        integerTextField.setAllowBlanks(false);
        integerTextField.setPrefWidth(50);
        GridPane.setMargin(integerTextField, INSETS);
        return integerTextField;
    }

    private ImageIntComboBox createSmallAttackSelection(BemAdventure adventure, ObservableList<ImageIntPair> options) {
        ImageIntComboBox comboBox = new ImageIntComboBox();
        comboBox.initialize(adventure.getSmallAttackId(), options, 1.0, null, "Default Attack");
        comboBox.setOnAction(e -> adventure.setSmallAttackId(comboBox.getValue().getValue()));
        comboBox.setPrefWidth(120);
        GridPane.setMargin(comboBox, INSETS);
        return comboBox;
    }

    private ImageIntComboBox createBigAttackSelection(BemAdventure adventure, ObservableList<ImageIntPair> options) {
        ImageIntComboBox comboBox = new ImageIntComboBox();
        comboBox.initialize(adventure.getBigAttackId(),options, 1.0, null, "Default Attack");
        comboBox.setOnAction(e -> adventure.setBigAttackId(comboBox.getValue().getValue()));
        comboBox.setPrefWidth(120);
        GridPane.setMargin(comboBox, INSETS);
        return comboBox;
    }

    private ImageIntComboBox createBackgroundSelection(Supplier<Integer> backgroundFetcher, Consumer<Integer> backgroundSetter, ObservableList<ImageIntPair> options) {
        ImageIntComboBox imageIntComboBox = new ImageIntComboBox();
        imageIntComboBox.initialize(backgroundFetcher.get(), options, 1.0, null, null);
        imageIntComboBox.setOnAction(e -> backgroundSetter.accept(imageIntComboBox.getValue().getValue()));
        GridPane.setMargin(imageIntComboBox, INSETS);
        return imageIntComboBox;
    }
}
