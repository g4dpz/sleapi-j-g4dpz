package esa.sle.ccsds.utils.cltu;

import java.io.ByteArrayOutputStream;

/**
 * CCSDS Command Link Transmission Unit (CLTU) Decoder
 * 
 * Decodes CLTU format and extracts command data.
 * Verifies start sequence, tail sequence, and BCH parity.
 * 
 * Reference: CCSDS 231.0-B-3 (TC Synchronization and Channel Coding)
 * 
 * @author ESA SLE Java API Team
 * @version 5.1.6
 */
public class CLTUDecoder {
    
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
     * Decode CLTU and extract command data
     * 
     * @param cltuData The complete CLTU
     * @return The extracted command data
     * @throws CLTUException if CLTU is invalid or BCH parity check fails
     */
    public static byte[] decode(byte[] cltuData) throws CLTUException {
        // Verify start sequence
        if (cltuData.length < 2 || 
            cltuData[0] != START_SEQUENCE[0] || 
            cltuData[1] != START_SEQUENCE[1]) {
            throw new CLTUException("Invalid CLTU start sequence");
        }
        
        // Find tail sequence
        int tailStart = findTailSequence(cltuData);
        if (tailStart == -1) {
            throw new CLTUException("CLTU tail sequence not found");
        }
        
        // Extract and decode code blocks
        ByteArrayOutputStream commandData = new ByteArrayOutputStream();
        int pos = 2; // Skip start sequence
        
        while (pos + CODE_BLOCK_SIZE <= tailStart) {
            byte[] codeBlock = new byte[CODE_BLOCK_SIZE];
            System.arraycopy(cltuData, pos, codeBlock, 0, CODE_BLOCK_SIZE);
            
            // Verify BCH parity
            byte calculatedParity = BCHEncoder.calculateParity(codeBlock, CODE_BLOCK_DATA_SIZE);
            if (calculatedParity != codeBlock[CODE_BLOCK_DATA_SIZE]) {
                throw new CLTUException("BCH parity error at position " + pos);
            }
            
            // Extract data bytes (remove fill bytes at end)
            for (int i = 0; i < CODE_BLOCK_DATA_SIZE; i++) {
                if (codeBlock[i] != FILL_BYTE) {
                    commandData.write(codeBlock[i]);
                } else {
                    // Stop at first fill byte (rest are padding)
                    break;
                }
            }
            
            pos += CODE_BLOCK_SIZE;
        }
        
        return commandData.toByteArray();
    }
    
    /**
     * Verify CLTU structure without extracting data
     * 
     * @param cltuData The CLTU to verify
     * @return true if CLTU is valid, false otherwise
     */
    public static boolean verify(byte[] cltuData) {
        try {
            decode(cltuData);
            return true;
        } catch (CLTUException e) {
            return false;
        }
    }
    
    /**
     * Find tail sequence in CLTU data
     */
    private static int findTailSequence(byte[] cltuData) {
        for (int i = 0; i <= cltuData.length - TAIL_SEQUENCE.length; i++) {
            boolean found = true;
            for (int j = 0; j < TAIL_SEQUENCE.length; j++) {
                if (cltuData[i + j] != TAIL_SEQUENCE[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }
}
