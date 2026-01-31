package esa.sle.ccsds.utils.crc;

/**
 * CRC-16-CCITT Calculator for CCSDS Frame Error Control Field (FECF)
 * 
 * Implements CRC-16-CCITT with polynomial 0x1021 as specified in CCSDS standards.
 * Used for Frame Error Control Field (FECF) in CCSDS Transfer Frames.
 * 
 * Reference: CCSDS 131.0-B-3 (TM Synchronization and Channel Coding)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class CRC16Calculator {
    
    /**
     * CRC-16-CCITT polynomial: x^16 + x^12 + x^5 + 1
     */
    private static final int POLYNOMIAL = 0x1021;
    
    /**
     * Initial CRC value
     */
    private static final int INITIAL_VALUE = 0xFFFF;
    
    /**
     * Private constructor to prevent instantiation
     */
    private CRC16Calculator() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Calculate CRC-16-CCITT for the given data.
     * 
     * This method implements the CCSDS standard CRC-16-CCITT algorithm
     * with polynomial 0x1021 and initial value 0xFFFF.
     * 
     * @param data the data to calculate CRC for (must not be null)
     * @return the calculated CRC-16 value (16-bit unsigned)
     * @throws IllegalArgumentException if data is null
     */
    public static int calculate(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        
        int crc = INITIAL_VALUE;
        
        for (byte b : data) {
            crc ^= (b & 0xFF) << 8;
            
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ POLYNOMIAL;
                } else {
                    crc = crc << 1;
                }
            }
        }
        
        return crc & 0xFFFF;
    }
    
    /**
     * Calculate CRC-16-CCITT for a portion of the given data.
     * 
     * @param data the data array (must not be null)
     * @param offset the starting offset in the data array
     * @param length the number of bytes to process
     * @return the calculated CRC-16 value (16-bit unsigned)
     * @throws IllegalArgumentException if data is null or offset/length are invalid
     */
    public static int calculate(byte[] data, int offset, int length) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IllegalArgumentException("Invalid offset or length");
        }
        
        int crc = INITIAL_VALUE;
        
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF) << 8;
            
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ POLYNOMIAL;
                } else {
                    crc = crc << 1;
                }
            }
        }
        
        return crc & 0xFFFF;
    }
    
    /**
     * Verify that the CRC-16 of the given data matches the expected value.
     * 
     * This is useful for validating received frames where the CRC is appended
     * to the data.
     * 
     * @param data the data to verify (must not be null)
     * @param expectedCrc the expected CRC-16 value
     * @return true if the calculated CRC matches the expected value
     * @throws IllegalArgumentException if data is null
     */
    public static boolean verify(byte[] data, int expectedCrc) {
        return calculate(data) == (expectedCrc & 0xFFFF);
    }
    
    /**
     * Verify that the CRC-16 appended to the data is correct.
     * 
     * This method assumes the last 2 bytes of the data contain the CRC-16
     * in big-endian format, and verifies it against the rest of the data.
     * 
     * @param dataWithCrc the data with CRC-16 appended (must be at least 2 bytes)
     * @return true if the CRC is valid
     * @throws IllegalArgumentException if data is null or too short
     */
    public static boolean verifyAppended(byte[] dataWithCrc) {
        if (dataWithCrc == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        if (dataWithCrc.length < 2) {
            throw new IllegalArgumentException("Data must be at least 2 bytes");
        }
        
        // Extract CRC from last 2 bytes (big-endian)
        int appendedCrc = ((dataWithCrc[dataWithCrc.length - 2] & 0xFF) << 8) |
                          (dataWithCrc[dataWithCrc.length - 1] & 0xFF);
        
        // Calculate CRC for data without the appended CRC
        int calculatedCrc = calculate(dataWithCrc, 0, dataWithCrc.length - 2);
        
        return calculatedCrc == appendedCrc;
    }
    
    /**
     * Append CRC-16 to the given data.
     * 
     * Creates a new byte array containing the original data followed by
     * the CRC-16 in big-endian format.
     * 
     * @param data the data to append CRC to (must not be null)
     * @return new byte array with CRC-16 appended
     * @throws IllegalArgumentException if data is null
     */
    public static byte[] appendCrc(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        
        int crc = calculate(data);
        
        byte[] result = new byte[data.length + 2];
        System.arraycopy(data, 0, result, 0, data.length);
        
        // Append CRC in big-endian format
        result[data.length] = (byte) ((crc >> 8) & 0xFF);
        result[data.length + 1] = (byte) (crc & 0xFF);
        
        return result;
    }
}
