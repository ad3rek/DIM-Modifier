package com.github.cfogrady.dim.modifier.data.card;

import com.github.cfogrady.dim.modifier.data.bem.BemCardData;
import com.github.cfogrady.dim.modifier.data.bem.BemCardDataReader;
import com.github.cfogrady.dim.modifier.data.bem.BemCardDataWriter;
import com.github.cfogrady.dim.modifier.data.dim.DimCardData;
import com.github.cfogrady.dim.modifier.data.dim.DimCardDataReader;
import com.github.cfogrady.dim.modifier.data.dim.DimCardDataWriter;
import com.github.cfogrady.vb.dim.card.BemCard;
import com.github.cfogrady.vb.dim.card.Card;
import com.github.cfogrady.vb.dim.card.DimCard;
import com.github.cfogrady.vb.dim.card.DimReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
public class CardDataIO {
    private final DimReader dimReader;
    private final DimCardDataWriter dimCardDataWriter;
    private final DimCardDataReader dimCardDataReader;
    private final BemCardDataWriter bemCardDataWriter;
    private final BemCardDataReader bemCardDataReader;

    public CardData<?, ?, ?> readFromStream(InputStream inputStream) throws IOException {
        Card card = dimReader.readCard(inputStream, true);
        if(card.getChecksum() != card.getCalculatedCheckSum()) {
            throw new IllegalStateException("Checksum mismatch. This is probably the result of a bad read.");
        }
        if(card instanceof BemCard bemCard) {
            return bemCardDataReader.fromCard(bemCard);
        } else if(card instanceof DimCard dimCard) {
            return dimCardDataReader.fromCard(dimCard);
        } else {
            throw new IllegalArgumentException("Unknown card type: " + card.getClass().getName());
        }
    }

    public void writeToFile(CardData<?, ?, ?> cardData, File file) {
        if(cardData instanceof BemCardData bemCardData) {
            bemCardDataWriter.write(file, bemCardData);
        } else if (cardData instanceof DimCardData dimCardData) {
            dimCardDataWriter.write(file, dimCardData);
        } else {
            throw new IllegalArgumentException("Unknown CardData type: " + cardData.getClass().getName());
        }
    }
    
    /**
     * Lê um arquivo .bin e retorna o CardData correspondente
     * @param file Arquivo .bin para ler
     * @return CardData lido do arquivo
     * @throws IOException Em caso de erro na leitura do arquivo
     */
    public CardData<?, ?, ?> readFromFile(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return readFromStream(inputStream);
        }
    }
}
