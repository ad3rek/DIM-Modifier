package com.github.cfogrady.dim.modifier.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Classe utilitária para ler o DIM ID diretamente dos bytes do arquivo .bin
 * Isso serve como um fallback quando o DimReader não consegue ler o ID corretamente
 */
public class DimIdReader {
    
    /**
     * Lê o DIM ID diretamente do arquivo .bin no offset 0x32
     * @param inputStream Stream do arquivo .bin
     * @return O DIM ID ou -1 em caso de erro
     */
    public static int readDimIdFromBin(InputStream inputStream) {
        try {
            // Posiciona no offset 0x32 onde fica o DIM ID
            inputStream.skip(0x32);
            
            // Lê os 2 bytes do DIM ID
            byte[] idBytes = new byte[2];
            int bytesRead = inputStream.read(idBytes);
            
            if (bytesRead == 2) {
                // Converte para little-endian
                ByteBuffer buffer = ByteBuffer.wrap(idBytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                int dimId = buffer.getShort() & 0xFFFF; // Unsigned short
                
                System.out.println("DIM ID lido diretamente do arquivo: " + dimId);
                return dimId;
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler DIM ID: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Extrai o DIM ID do nome do arquivo se possível
     * @param fileName Nome do arquivo
     * @return O DIM ID ou -1 se não encontrado
     */
    public static int extractDimIdFromFileName(String fileName) {
        try {
            // Se o nome do arquivo tiver um formato que inclui o ID como, por exemplo: DIM_42.bin
            if (fileName.matches(".*\\d+.*")) {
                // Extrai os números do nome do arquivo
                String idStr = fileName.replaceAll("[^0-9]", "");
                if (!idStr.isEmpty()) {
                    int possibleId = Integer.parseInt(idStr);
                    System.out.println("ID extraído do nome do arquivo: " + possibleId);
                    return possibleId;
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao extrair ID do nome do arquivo: " + e.getMessage());
        }
        
        return -1;
    }
} 