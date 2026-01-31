package esa.sle.ccsds.utils.cltu;

/**
 * BCH(63,56) Error Detection Code Encoder
 * 
 * Simplified implementation for CLTU code blocks.
 * Uses XOR checksum with bit rotation for demonstration.
 * 
 * Note: Production systems should use proper BCH(63,56) with generator polynomial.
 * 
 * Reference: CCSDS 231.0-B-3 (TC Synchronization and Channel Coding)
 * 
 * @author ESA SLE Java API Team
 * @version 5.1.6
 */
public class BCHEncoder {
    
    /**
     * Calculate BCH(63,56) parity byte for code block
     * 
     * Simplified implementation using XOR checksum with bit rotation.
     * Real implementation would use proper BCH polynomial.
     * 
     * @param data The data bytes (typically 7 bytes)
     * @param length Number of bytes to process
     * @return The BCH parity byte
     */
    public static byte calculateParity(byte[] data, int length) {
        // Simplified BCH calculation using XOR checksum
        // In production, use proper BCH(63,56) with generator polynomial
        byte parity = 0;
        for (int i = 0; i < length; i++) {
            parity ^= data[i];
            // Rotate for better distribution
            parity = (byte) ((parity << 1) | ((parity & 0x80) != 0 ? 1 : 0));
        }
        return parity;
    }
    
    /**
     * Verify BCH parity for code block
     * 
     * @param codeBlock The complete code block (data + parity)
     * @param dataLength Number of data bytes (excluding parity)
     * @return true if parity is correct, false otherwise
     */
    public static boolean verifyParity(byte[] codeBlock, int dataLength) {
        if (codeBlock.length < dataLength + 1) {
            return false;
        }
        byte calculatedParity = calculateParity(codeBlock, dataLength);
        return calculatedParity == codeBlock[dataLength];
    }
}
