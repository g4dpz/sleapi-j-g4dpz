package esa.sle.ccsds.utils.time;

import java.time.Instant;

/**
 * CCSDS Unsegmented Time Code (CUC) Decoder
 * 
 * Decodes timestamps from CCSDS Unsegmented Time Code format as specified
 * in CCSDS 301.0-B-4 (Time Code Formats).
 * 
 * This implementation uses Unix epoch (1970-01-01 00:00:00 UTC).
 * 
 * Reference: CCSDS 301.0-B-4 Section 3.2 (CUC Time Code)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class CUCTimeDecoder {
    
    /**
     * Private constructor to prevent instantiation
     */
    private CUCTimeDecoder() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Decode CUC time code to Instant.
     * 
     * @param cucTime the CUC time code bytes
     * @param coarseBytes number of bytes for coarse time (1-4)
     * @param fineBytes number of bytes for fine time (0-3)
     * @return the decoded timestamp
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static Instant decode(byte[] cucTime, int coarseBytes, int fineBytes) {
        if (cucTime == null) {
            throw new IllegalArgumentException("CUC time must not be null");
        }
        if (coarseBytes < 1 || coarseBytes > 4) {
            throw new IllegalArgumentException("Coarse bytes must be 1-4");
        }
        if (fineBytes < 0 || fineBytes > 3) {
            throw new IllegalArgumentException("Fine bytes must be 0-3");
        }
        if (cucTime.length < coarseBytes + fineBytes) {
            throw new IllegalArgumentException("CUC time too short");
        }
        
        // Decode coarse time (seconds)
        long seconds = 0;
        for (int i = 0; i < coarseBytes; i++) {
            seconds = (seconds << 8) | (cucTime[i] & 0xFF);
        }
        
        // Decode fine time (sub-seconds)
        int nanos = 0;
        if (fineBytes > 0) {
            long fineValue = 0;
            for (int i = 0; i < fineBytes; i++) {
                fineValue = (fineValue << 8) | (cucTime[coarseBytes + i] & 0xFF);
            }
            
            // Convert fractional value to nanoseconds
            // Fine time represents fraction of a second as: value / 2^(fineBytes*8)
            long maxFineValue = 1L << (fineBytes * 8);
            nanos = (int) ((fineValue * 1_000_000_000L) / maxFineValue);
        }
        
        return Instant.ofEpochSecond(seconds, nanos);
    }
    
    /**
     * Decode CUC time code with 4 bytes coarse, 3 bytes fine.
     * 
     * @param cucTime the CUC time code bytes (must be at least 7 bytes)
     * @return the decoded timestamp
     * @throws IllegalArgumentException if cucTime is invalid
     */
    public static Instant decode(byte[] cucTime) {
        return decode(cucTime, 4, 3);
    }
}
