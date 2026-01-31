package esa.sle.ccsds.utils.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * CCSDS Day Segmented Time Code (CDS) Decoder
 * 
 * Decodes timestamps from CCSDS Day Segmented Time Code format as specified
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
public class CDSTimeDecoder {
    
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
    private CDSTimeDecoder() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Decode CDS time code to Instant.
     * 
     * Supports both basic format (6 bytes) and extended format with
     * sub-millisecond precision (8 bytes).
     * 
     * @param cdsTime the CDS time code bytes (6 or 8 bytes)
     * @return the decoded timestamp
     * @throws IllegalArgumentException if cdsTime is invalid
     */
    public static Instant decode(byte[] cdsTime) {
        if (cdsTime == null) {
            throw new IllegalArgumentException("CDS time must not be null");
        }
        if (cdsTime.length != 6 && cdsTime.length != 8) {
            throw new IllegalArgumentException("CDS time must be 6 or 8 bytes");
        }
        
        // Decode day counter (2 bytes, big-endian)
        int daysSinceEpoch = ((cdsTime[0] & 0xFF) << 8) | (cdsTime[1] & 0xFF);
        
        // Decode milliseconds of day (4 bytes, big-endian)
        long millisOfDay = ((long)(cdsTime[2] & 0xFF) << 24)
                         | ((long)(cdsTime[3] & 0xFF) << 16)
                         | ((long)(cdsTime[4] & 0xFF) << 8)
                         | (long)(cdsTime[5] & 0xFF);
        
        // Validate milliseconds of day
        if (millisOfDay < 0 || millisOfDay >= MILLIS_PER_DAY) {
            throw new IllegalArgumentException("Invalid milliseconds of day: " + millisOfDay);
        }
        
        // Calculate base timestamp
        LocalDate date = CCSDS_EPOCH.plusDays(daysSinceEpoch);
        Instant baseInstant = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant timestamp = baseInstant.plusMillis(millisOfDay);
        
        // Add sub-millisecond precision if present
        if (cdsTime.length == 8) {
            // Decode sub-millisecond field (2 bytes, big-endian)
            int submillis = ((cdsTime[6] & 0xFF) << 8) | (cdsTime[7] & 0xFF);
            
            // Extract microseconds from upper 10 bits
            int microsWithinMilli = (submillis >>> 6) & 0x3FF;
            
            // Validate microseconds
            if (microsWithinMilli > 999) {
                throw new IllegalArgumentException("Invalid microseconds: " + microsWithinMilli);
            }
            
            // Add microseconds to timestamp
            timestamp = timestamp.plusNanos(microsWithinMilli * 1000L);
        }
        
        return timestamp;
    }
    
    /**
     * Decode CDS time code with basic format (6 bytes).
     * 
     * @param cdsTime the CDS time code bytes (must be 6 bytes)
     * @return the decoded timestamp
     * @throws IllegalArgumentException if cdsTime is invalid
     */
    public static Instant decodeBasic(byte[] cdsTime) {
        if (cdsTime == null || cdsTime.length != 6) {
            throw new IllegalArgumentException("Basic CDS time must be 6 bytes");
        }
        return decode(cdsTime);
    }
    
    /**
     * Decode CDS time code with extended format (8 bytes).
     * 
     * @param cdsTime the CDS time code bytes (must be 8 bytes)
     * @return the decoded timestamp
     * @throws IllegalArgumentException if cdsTime is invalid
     */
    public static Instant decodeExtended(byte[] cdsTime) {
        if (cdsTime == null || cdsTime.length != 8) {
            throw new IllegalArgumentException("Extended CDS time must be 8 bytes");
        }
        return decode(cdsTime);
    }
    
    /**
     * Get the CCSDS epoch used for CDS decoding.
     * 
     * @return the CCSDS epoch (1958-01-01)
     */
    public static LocalDate getEpoch() {
        return CCSDS_EPOCH;
    }
}
