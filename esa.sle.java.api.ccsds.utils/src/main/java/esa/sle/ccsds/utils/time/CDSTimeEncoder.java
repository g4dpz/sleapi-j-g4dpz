package esa.sle.ccsds.utils.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * CCSDS Day Segmented Time Code (CDS) Encoder
 * 
 * Encodes timestamps in CCSDS Day Segmented Time Code format as specified
 * in CCSDS 301.0-B-4 (Time Code Formats).
 * 
 * CDS format consists of:
 * - Day counter: Days since epoch (2 bytes)
 * - Milliseconds of day: 0-86399999 (4 bytes)
 * - Optional: Microseconds or picoseconds (2 bytes)
 * 
 * The epoch is typically 1958-01-01 00:00:00 TAI (CCSDS epoch).
 * This implementation uses a configurable epoch, defaulting to CCSDS epoch.
 * 
 * Reference: CCSDS 301.0-B-4 Section 3.3 (CDS Time Code)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class CDSTimeEncoder {
    
    /**
     * CCSDS epoch: 1958-01-01 00:00:00 TAI
     * Approximated as 1958-01-01 00:00:00 UTC for simplicity
     */
    private static final LocalDate CCSDS_EPOCH = LocalDate.of(1958, 1, 1);
    
    /**
     * Milliseconds per day
     */
    private static final long MILLIS_PER_DAY = 86_400_000L;
    
    /**
     * Private constructor to prevent instantiation
     */
    private CDSTimeEncoder() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Encode timestamp in CDS format (basic: day + milliseconds).
     * 
     * Format: 2 bytes day + 4 bytes milliseconds = 6 bytes total
     * 
     * @param timestamp the timestamp to encode
     * @return the encoded CDS time code (6 bytes)
     * @throws IllegalArgumentException if timestamp is null or before epoch
     */
    public static byte[] encode(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp must not be null");
        }
        
        // Convert to UTC date/time
        LocalDate date = timestamp.atZone(ZoneOffset.UTC).toLocalDate();
        
        // Calculate days since epoch
        long daysSinceEpoch = ChronoUnit.DAYS.between(CCSDS_EPOCH, date);
        
        if (daysSinceEpoch < 0 || daysSinceEpoch > 65535) {
            throw new IllegalArgumentException("Date out of range for CDS format");
        }
        
        // Calculate milliseconds of day
        long millisOfDay = timestamp.toEpochMilli() % MILLIS_PER_DAY;
        if (millisOfDay < 0) {
            millisOfDay += MILLIS_PER_DAY;
        }
        
        // Encode to bytes
        byte[] result = new byte[6];
        
        // Day counter (2 bytes, big-endian)
        result[0] = (byte) ((daysSinceEpoch >>> 8) & 0xFF);
        result[1] = (byte) (daysSinceEpoch & 0xFF);
        
        // Milliseconds of day (4 bytes, big-endian)
        result[2] = (byte) ((millisOfDay >>> 24) & 0xFF);
        result[3] = (byte) ((millisOfDay >>> 16) & 0xFF);
        result[4] = (byte) ((millisOfDay >>> 8) & 0xFF);
        result[5] = (byte) (millisOfDay & 0xFF);
        
        return result;
    }
    
    /**
     * Encode timestamp in CDS format with sub-millisecond precision.
     * 
     * Format: 2 bytes day + 4 bytes milliseconds + 2 bytes submillis = 8 bytes total
     * 
     * The sub-millisecond field encodes microseconds (0-999) in the upper 10 bits
     * and can optionally encode picoseconds in the remaining bits.
     * 
     * @param timestamp the timestamp to encode
     * @param includeSubmillis true to include sub-millisecond precision
     * @return the encoded CDS time code (6 or 8 bytes)
     * @throws IllegalArgumentException if timestamp is null or before epoch
     */
    public static byte[] encode(Instant timestamp, boolean includeSubmillis) {
        if (!includeSubmillis) {
            return encode(timestamp);
        }
        
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp must not be null");
        }
        
        // Get basic encoding
        byte[] basic = encode(timestamp);
        
        // Add sub-millisecond precision
        byte[] result = new byte[8];
        System.arraycopy(basic, 0, result, 0, 6);
        
        // Calculate microseconds within millisecond (0-999)
        int nanos = timestamp.getNano();
        int microsWithinMilli = (nanos / 1000) % 1000;
        
        // Encode microseconds in upper 10 bits of 2-byte field
        // Remaining 6 bits can be used for picoseconds if needed
        int submillis = microsWithinMilli << 6;
        
        result[6] = (byte) ((submillis >>> 8) & 0xFF);
        result[7] = (byte) (submillis & 0xFF);
        
        return result;
    }
    
    /**
     * Encode current time in CDS format.
     * 
     * @return the encoded CDS time code (6 bytes)
     */
    public static byte[] encodeNow() {
        return encode(Instant.now());
    }
    
    /**
     * Encode current time in CDS format with sub-millisecond precision.
     * 
     * @param includeSubmillis true to include sub-millisecond precision
     * @return the encoded CDS time code (6 or 8 bytes)
     */
    public static byte[] encodeNow(boolean includeSubmillis) {
        return encode(Instant.now(), includeSubmillis);
    }
    
    /**
     * Get the CCSDS epoch used for CDS encoding.
     * 
     * @return the CCSDS epoch (1958-01-01)
     */
    public static LocalDate getEpoch() {
        return CCSDS_EPOCH;
    }
}
