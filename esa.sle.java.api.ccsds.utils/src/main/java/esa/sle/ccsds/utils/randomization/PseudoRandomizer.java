package esa.sle.ccsds.utils.randomization;

/**
 * Pseudo-Randomization for CCSDS Data Streams
 * 
 * Implements pseudo-randomization for spectral shaping of CCSDS data streams
 * as specified in CCSDS 131.0-B-3 (TM Synchronization and Channel Coding).
 * 
 * Pseudo-randomization is applied to improve the spectral characteristics of
 * the transmitted signal by eliminating long runs of zeros or ones, which
 * helps with:
 * - Clock recovery at the receiver
 * - Bit synchronization
 * - Spectral shaping for RF transmission
 * 
 * The CCSDS standard uses the polynomial: 1 + x^3 + x^5 + x^7 + x^8
 * 
 * This is a self-synchronizing scrambler where the same operation is used
 * for both randomization and derandomization.
 * 
 * Reference: CCSDS 131.0-B-3 Section 9 (Pseudo-Randomization)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class PseudoRandomizer {
    
    /**
     * CCSDS pseudo-randomization polynomial: 1 + x^3 + x^5 + x^7 + x^8
     * Represented as tap positions in the shift register
     */
    private static final int[] TAPS = {3, 5, 7, 8};
    
    /**
     * Initial state for the pseudo-random sequence generator
     * All ones (0xFF) as specified in CCSDS standard
     */
    private static final int INITIAL_STATE = 0xFF;
    
    /**
     * Private constructor to prevent instantiation
     */
    private PseudoRandomizer() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Apply pseudo-randomization to data.
     * 
     * This method XORs the input data with a pseudo-random sequence
     * generated using the CCSDS polynomial. The same operation is used
     * for both randomization and derandomization (self-synchronizing).
     * 
     * @param data the data to randomize (must not be null)
     * @return the randomized data (new array)
     * @throws IllegalArgumentException if data is null
     */
    public static byte[] randomize(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        
        return process(data, 0, data.length);
    }
    
    /**
     * Apply pseudo-randomization to a portion of data.
     * 
     * @param data the data array (must not be null)
     * @param offset the starting offset
     * @param length the number of bytes to process
     * @return the randomized data (new array of specified length)
     * @throws IllegalArgumentException if data is null or offset/length are invalid
     */
    public static byte[] randomize(byte[] data, int offset, int length) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IllegalArgumentException("Invalid offset or length");
        }
        
        return process(data, offset, length);
    }
    
    /**
     * Remove pseudo-randomization from data.
     * 
     * Since the CCSDS pseudo-randomizer is self-synchronizing, this is
     * the same operation as randomization (XOR with the same sequence).
     * 
     * @param data the randomized data (must not be null)
     * @return the derandomized data (new array)
     * @throws IllegalArgumentException if data is null
     */
    public static byte[] derandomize(byte[] data) {
        // Self-synchronizing: same operation as randomization
        return randomize(data);
    }
    
    /**
     * Remove pseudo-randomization from a portion of data.
     * 
     * @param data the data array (must not be null)
     * @param offset the starting offset
     * @param length the number of bytes to process
     * @return the derandomized data (new array of specified length)
     * @throws IllegalArgumentException if data is null or offset/length are invalid
     */
    public static byte[] derandomize(byte[] data, int offset, int length) {
        // Self-synchronizing: same operation as randomization
        return randomize(data, offset, length);
    }
    
    /**
     * Apply pseudo-randomization in-place.
     * 
     * This modifies the input array directly instead of creating a new array.
     * Useful for large data sets to avoid memory allocation.
     * 
     * @param data the data to randomize in-place (must not be null)
     * @throws IllegalArgumentException if data is null
     */
    public static void randomizeInPlace(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        
        processInPlace(data, 0, data.length);
    }
    
    /**
     * Apply pseudo-randomization in-place to a portion of data.
     * 
     * @param data the data array (must not be null)
     * @param offset the starting offset
     * @param length the number of bytes to process
     * @throws IllegalArgumentException if data is null or offset/length are invalid
     */
    public static void randomizeInPlace(byte[] data, int offset, int length) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IllegalArgumentException("Invalid offset or length");
        }
        
        processInPlace(data, offset, length);
    }
    
    /**
     * Remove pseudo-randomization in-place.
     * 
     * @param data the randomized data (must not be null)
     * @throws IllegalArgumentException if data is null
     */
    public static void derandomizeInPlace(byte[] data) {
        // Self-synchronizing: same operation as randomization
        randomizeInPlace(data);
    }
    
    /**
     * Remove pseudo-randomization in-place from a portion of data.
     * 
     * @param data the data array (must not be null)
     * @param offset the starting offset
     * @param length the number of bytes to process
     * @throws IllegalArgumentException if data is null or offset/length are invalid
     */
    public static void derandomizeInPlace(byte[] data, int offset, int length) {
        // Self-synchronizing: same operation as randomization
        randomizeInPlace(data, offset, length);
    }
    
    /**
     * Process data with pseudo-random sequence (creates new array).
     * 
     * @param data the input data
     * @param offset the starting offset
     * @param length the number of bytes to process
     * @return the processed data (new array)
     */
    private static byte[] process(byte[] data, int offset, int length) {
        byte[] result = new byte[length];
        int state = INITIAL_STATE;
        
        for (int i = 0; i < length; i++) {
            // Generate pseudo-random byte
            int randomByte = 0;
            
            for (int bit = 0; bit < 8; bit++) {
                // Get output bit (LSB of state)
                int outputBit = state & 0x1;
                randomByte = (randomByte << 1) | outputBit;
                
                // Calculate feedback bit using polynomial taps
                // Polynomial: 1 + x^3 + x^5 + x^7 + x^8
                int feedbackBit = ((state >> 2) ^ (state >> 4) ^ (state >> 6) ^ (state >> 7)) & 0x1;
                
                // Shift register and insert feedback bit
                state = ((state >> 1) | (feedbackBit << 7)) & 0xFF;
            }
            
            // XOR data with pseudo-random sequence
            result[i] = (byte) (data[offset + i] ^ randomByte);
        }
        
        return result;
    }
    
    /**
     * Process data with pseudo-random sequence (modifies in-place).
     * 
     * @param data the data array
     * @param offset the starting offset
     * @param length the number of bytes to process
     */
    private static void processInPlace(byte[] data, int offset, int length) {
        int state = INITIAL_STATE;
        
        for (int i = 0; i < length; i++) {
            // Generate pseudo-random byte
            int randomByte = 0;
            
            for (int bit = 0; bit < 8; bit++) {
                // Get output bit (LSB of state)
                int outputBit = state & 0x1;
                randomByte = (randomByte << 1) | outputBit;
                
                // Calculate feedback bit using polynomial taps
                // Polynomial: 1 + x^3 + x^5 + x^7 + x^8
                int feedbackBit = ((state >> 2) ^ (state >> 4) ^ (state >> 6) ^ (state >> 7)) & 0x1;
                
                // Shift register and insert feedback bit
                state = ((state >> 1) | (feedbackBit << 7)) & 0xFF;
            }
            
            // XOR data with pseudo-random sequence
            data[offset + i] ^= (byte) randomByte;
        }
    }
    
    /**
     * Generate the pseudo-random sequence for a given length.
     * 
     * This is useful for testing or when you need the sequence itself
     * rather than applying it to data.
     * 
     * @param length the length of the sequence to generate
     * @return the pseudo-random sequence
     * @throws IllegalArgumentException if length is negative
     */
    public static byte[] generateSequence(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length must not be negative");
        }
        
        byte[] sequence = new byte[length];
        int state = INITIAL_STATE;
        
        for (int i = 0; i < length; i++) {
            // Generate pseudo-random byte
            int randomByte = 0;
            
            for (int bit = 0; bit < 8; bit++) {
                // Get output bit (LSB of state)
                int outputBit = state & 0x1;
                randomByte = (randomByte << 1) | outputBit;
                
                // Calculate feedback bit using polynomial taps
                int feedbackBit = ((state >> 2) ^ (state >> 4) ^ (state >> 6) ^ (state >> 7)) & 0x1;
                
                // Shift register and insert feedback bit
                state = ((state >> 1) | (feedbackBit << 7)) & 0xFF;
            }
            
            sequence[i] = (byte) randomByte;
        }
        
        return sequence;
    }
}
