package esa.sle.ccsds.utils.cltu;

import java.io.ByteArrayOutputStream;

/**
 * CCSDS Command Link Transmission Unit (CLTU) Encoder
 * 
 * Encodes command data into CLTU format with:
 * - Start Sequence: 0xEB90 (2 bytes)
 * - Code Blocks: Data (7 bytes) + BCH parity (1 byte) per block
 * - Tail Sequence: 0xC5C5C5C5C5C5C5 (7 bytes)
 * 
 * Reference: CCSDS 231.0-B-3 (TC Synchronization and Channel Coding)
 * 
 * @author ESA SLE Java API Team
 * @version 5.1.6
 */
public class CLTUEncoder {
    
    // CLTU constants
    private static final byte[] START_SEQUENCE = {(byte) 0xEB, (byte) 0x90};
    private static final byte[] TAIL_SEQUENCE = {
        (byte) 0xC5, (byte) 0xC5, (byte) 0xC5, (byte) 0xC5,
        (byte) 0xC5, (byte) 0xC5, (byte) 0xC5
    };
    private static final int CODE_BLOCK_DATA_SIZE = 7;  // 7 data bytes per code block
    private static final int CODE_BLOCK_SIZE = 8;       // 7 data + 1 BCH parity
    private static final byte FILL_BYTE = 0x55;         // Fill pattern for partial blocks
    
    /**
     * Encode command data into CLTU format
     * 
     * @param commandData The command data to encapsulate
     * @return The complete CLTU as byte array
     */
    public static byte[] encode(byte[] commandData) {
        ByteArrayOutputStream cltu = new ByteArrayOutputStream();
        
        // 1. Add start sequence
        cltu.write(START_SEQUENCE, 0, START_SEQUENCE.length);
        
        // 2. Add code blocks with BCH encoding
        int dataPos = 0;
        while (dataPos < commandData.length) {
            byte[] codeBlock = new byte[CODE_BLOCK_SIZE];
            
            // Fill data portion (7 bytes)
            int bytesToCopy = Math.min(CODE_BLOCK_DATA_SIZE, commandData.length - dataPos);
            System.arraycopy(commandData, dataPos, codeBlock, 0, bytesToCopy);
            
            // Fill remaining bytes with fill pattern
            for (int i = bytesToCopy; i < CODE_BLOCK_DATA_SIZE; i++) {
                codeBlock[i] = FILL_BYTE;
            }
            
            // Calculate and add BCH parity byte
            codeBlock[CODE_BLOCK_DATA_SIZE] = BCHEncoder.calculateParity(codeBlock, CODE_BLOCK_DATA_SIZE);
            
            // Write code block to CLTU
            cltu.write(codeBlock, 0, CODE_BLOCK_SIZE);
            
            dataPos += bytesToCopy;
        }
        
        // 3. Add tail sequence
        cltu.write(TAIL_SEQUENCE, 0, TAIL_SEQUENCE.length);
        
        return cltu.toByteArray();
    }
    
    /**
     * Calculate CLTU size for given command data length
     * 
     * @param commandDataLength Length of command data in bytes
     * @return Total CLTU size in bytes
     */
    public static int calculateCLTUSize(int commandDataLength) {
        int codeBlocks = (commandDataLength + CODE_BLOCK_DATA_SIZE - 1) / CODE_BLOCK_DATA_SIZE;
        return START_SEQUENCE.length + (codeBlocks * CODE_BLOCK_SIZE) + TAIL_SEQUENCE.length;
    }
    
    /**
     * Get number of code blocks for given command data length
     * 
     * @param commandDataLength Length of command data in bytes
     * @return Number of code blocks
     */
    public static int getCodeBlockCount(int commandDataLength) {
        return (commandDataLength + CODE_BLOCK_DATA_SIZE - 1) / CODE_BLOCK_DATA_SIZE;
    }
}
