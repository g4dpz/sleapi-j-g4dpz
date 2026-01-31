package esa.sle.ccsds.utils.time;

import java.time.Instant;

/**
 * CCSDS Unsegmented Time Code (CUC) Encoder
 * 
 * Encodes timestamps in CCSDS Unsegmented Time Code format as specified
 * in CCSDS 301.0-B-4 (Time Code Formats).
 * 
 * CUC is a simple binary time code consisting of:
 * - Coarse time: Seconds since epoch (1-4 bytes)
 * - Fine time: Sub-second resolution (0-3 bytes)
 * 
 * The epoch is mission-specific but commonly uses:
 * - TAI epoch: 1958-01-01 00:00:00 TAI
 * - Unix epoch: 1970-01-01 00:00:00 UTC
 * - J2000 epoch: 2000-01-01 12:00:00 TT
 * 
 * This implementation uses Unix epoch for simplicity.
 * 
 * Reference: CCSDS 301.0-B-4 Section 3.2 (CUC Time Code)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class CUCTimeEncoder {
    
    /**
     * Unix epoch: 1970-01-01 00:00:00 UTC
     */
    private static final Instant UNIX_EPOCH = Instant.EPOCH;
    
    /**
     * Private constructor to prevent instantiation
     */
    private CUCTimeEncoder() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Encode timestamp in CUC format with specified precision.
     * 
     * @param timestamp the timestamp to encode
     * @param coarseBytes number of bytes for coarse time (1-4)
     * @param fineBytes number of bytes for fine time (0-3)
     * @return the encoded CUC time code
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static byte[] encode(Instant timestamp, int coarseBytes, int fineBytes) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp must not be null");
        }
        if (coarseBytes < 1 || coarseBytes > 4) {
            throw new IllegalArgumentException("Coarse bytes must be 1-4");
        }
        if (fineBytes < 0 || fineBytes > 3) {
            throw new IllegalArgumentException("Fine bytes must be 0-3");
        }
        
        // Calculate seconds and nanoseconds since epoch
        long seconds = timestamp.getEpochSecond();
        int nanos = timestamp.getNano();
        
        // Create result array
        byte[] result = new byte[coarseBytes + fineBytes];
        
        // Encode coarse time (seconds)
        for (int i = 0; i < coarseBytes; i++) {
            int shift = (coarseBytes - 1 - i) * 8;
            result[i] = (byte) ((seconds >>> shift) & 0xFF);
        }
        
        // Encode fine time (sub-seconds)
        if (fineBytes > 0) {
            // Convert nanoseconds to fractional value
            // Fine time represents fraction of a second as: value / 2^(fineBytes*8)
            long maxFineValue = 1L << (fineBytes * 8);
            long fineValue = (nanos * maxFineValue) / 1_000_000_000L;
            
            for (int i = 0; i < fineBytes; i++) {
                int shift = (fineBytes - 1 - i) * 8;
                result[coarseBytes + i] = (byte) ((fineValue >>> shift) & 0xFF);
            }
        }
        
        return result;
    }
    
    /**
     * Encode current time in CUC format.
     * 
     * @param coarseBytes number of bytes for coarse time (1-4)
     * @param fineBytes number of bytes for fine time (0-3)
     * @return the encoded CUC time code
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static byte[] encodeNow(int coarseBytes, int fineBytes) {
        return encode(Instant.now(), coarseBytes, fineBytes);
    }
    
    /**
     * Encode timestamp in CUC format with 4 bytes coarse, 3 bytes fine.
     * 
     * This is a common configuration providing:
     * - Coarse: ~136 years range
     * - Fine: ~0.06 microsecond resolution
     * 
     * @param timestamp the timestamp to encode
     * @return the encoded CUC time code (7 bytes)
     * @throws IllegalArgumentException if timestamp is null
     */
    public static byte[] encode(Instant timestamp) {
        return encode(timestamp, 4, 3);
    }
    
    /**
     * Encode current time in CUC format with 4 bytes coarse, 3 bytes fine.
     * 
     * @return the encoded CUC time code (7 bytes)
     */
    public static byte[] encodeNow() {
        return encode(Instant.now(), 4, 3);
    }
    
    /**
     * Calculate the size of CUC time code for given configuration.
     * 
     * @param coarseBytes number of bytes for coarse time (1-4)
     * @param fineBytes number of bytes for fine time (0-3)
     * @return the total size in bytes
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static int getSize(int coarseBytes, int fineBytes) {
        if (coarseBytes < 1 || coarseBytes > 4) {
            throw new IllegalArgumentException("Coarse bytes must be 1-4");
        }
        if (fineBytes < 0 || fineBytes > 3) {
            throw new IllegalArgumentException("Fine bytes must be 0-3");
        }
        return coarseBytes + fineBytes;
    }
}
