package com.github.cfogrady.dim.modifier.data;


import com.github.cfogrady.dim.modifier.data.bem.BemCardData;
import com.github.cfogrady.dim.modifier.data.card.CardData;
import com.github.cfogrady.dim.modifier.data.card.Character;
import com.github.cfogrady.dim.modifier.data.firmware.FirmwareData;
import com.github.cfogrady.vb.dim.sprite.SpriteData;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Data
public class AppState {
    public static final int SELECTION_SPRITE_IDX = 1;

    private FirmwareData firmwareData;
    private CardData<?, ?, ?> cardData;
    private File lastOpenedFilePath;
    private int selectedBackgroundIndex;
    private Runnable unsavedChangesCallback;

    public CardData<?, ?, ?> getCardData() {
        return cardData;
    }
    
    public void setCardData(CardData<?, ?, ?> cardData) {
        this.cardData = cardData;
    }
    
    public int getSelectedBackgroundIndex() {
        return selectedBackgroundIndex;
    }
    
    public void setSelectedBackgroundIndex(int selectedBackgroundIndex) {
        this.selectedBackgroundIndex = selectedBackgroundIndex;
    }
    
    public SpriteData.Sprite getSelectedBackground() {
        return cardData.getCardSprites().getBackgrounds().get(selectedBackgroundIndex);
    }

    public List<SpriteData.Sprite> getIdleForCharacters() {
        List<SpriteData.Sprite> idleSprites = new ArrayList<>();
        try {
            if (getCardData() == null || getCardData().getCharacters() == null) {
                return idleSprites; // Retorna lista vazia em vez de lançar exceção
            }
            
            for(Character<?, ?> character : getCardData().getCharacters()) {
                try {
                    if (character != null && character.getSprites() != null && 
                        character.getSprites().size() > SELECTION_SPRITE_IDX && 
                        character.getSprites().get(SELECTION_SPRITE_IDX) != null) {
                        idleSprites.add(character.getSprites().get(SELECTION_SPRITE_IDX));
                    } else {
                        // Adicionar um sprite em branco para manter a contagem correta
                        idleSprites.add(createEmptySprite());
                    }
                } catch (Exception e) {
                    // Em caso de erro processando um personagem, adicionar sprite em branco e continuar
                    idleSprites.add(createEmptySprite());
                }
            }
        } catch (Exception e) {
            // Log de erro e retorna o que conseguiu obter até agora
            System.err.println("Error getting idle sprites: " + e.getMessage());
        }
        return idleSprites;
    }

    /**
     * Cria um sprite vazio para usar como fallback
     */
    private SpriteData.Sprite createEmptySprite() {
        // Criar um sprite vazio 16x16 com pixels vazios
        int width = 16;
        int height = 16;
        byte[] data = new byte[width * height * 2]; // 16x16 pixels em formato R5G6B5 (2 bytes por pixel)
        
        // Preencher com cor transparente/verde para visualização
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                data[(y*width + x)*2] = (byte) 0b11100000;     // Verde
                data[(y*width + x)*2 + 1] = (byte) 0b00000111; // Verde
            }
        }
        
        return SpriteData.Sprite.builder()
            .width(width)
            .height(height)
            .pixelData(data)
            .build();
    }

    public Character<?, ?> getCharacter(int characterIndex) {
        return cardData.getCharacters().get(characterIndex);
    }

    public List<SpriteData.Sprite> getAttributes() {
        if(cardData instanceof BemCardData bemCardData) {
            return bemCardData.getCardSprites().getTypes();
        } else {
            return firmwareData.getTypes();
        }
    }

    public void setBackgroundSprite(SpriteData.Sprite sprite) {
        getCardData().getCardSprites().getBackgrounds().set(getSelectedBackgroundIndex(), sprite);
        markUnsavedChanges();
    }
    
    /**
     * Configura o callback que será chamado quando houver alterações não salvas
     */
    public void setUnsavedChangesCallback(Runnable callback) {
        this.unsavedChangesCallback = callback;
    }
    
    /**
     * Marca que há alterações não salvas e notifica o callback se existir
     */
    public void markUnsavedChanges() {
        if (unsavedChangesCallback != null) {
            unsavedChangesCallback.run();
        }
    }
}
