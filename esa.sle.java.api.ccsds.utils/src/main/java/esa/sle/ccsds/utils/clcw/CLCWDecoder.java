package esa.sle.ccsds.utils.clcw;

/**
 * CLCW (Communications Link Control Word) Decoder
 * 
 * Decodes CLCW from the Operational Control Field (OCF) of
 * CCSDS Telemetry Transfer Frames.
 * 
 * Reference: CCSDS 232.0-B-3 (TC Space Data Link Protocol)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class CLCWDecoder {
    
    /**
     * Private constructor to prevent instantiation
     */
    private CLCWDecoder() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Decode CLCW from a 32-bit integer.
     * 
     * @param clcwWord the CLCW as a 32-bit integer
     * @return the decoded CLCW
     */
    public static CLCW decode(int clcwWord) {
        return new CLCW(clcwWord);
    }
    
    /**
     * Decode CLCW from a byte array (4 bytes, big-endian).
     * 
     * @param clcwBytes the CLCW as a 4-byte array
     * @return the decoded CLCW
     * @throws IllegalArgumentException if array is not 4 bytes
     */
    public static CLCW decode(byte[] clcwBytes) {
        if (clcwBytes == null || clcwBytes.length != 4) {
            throw new IllegalArgumentException("CLCW must be 4 bytes");
        }
        
        int clcwWord = ((clcwBytes[0] & 0xFF) << 24) |
                       ((clcwBytes[1] & 0xFF) << 16) |
                       ((clcwBytes[2] & 0xFF) << 8) |
                       (clcwBytes[3] & 0xFF);
        
        return new CLCW(clcwWord);
    }
    
    /**
     * Decoded CLCW with all fields accessible.
     */
    public static class CLCW {
        private final int type;
        private final int version;
        private final int statusField;
        private final int copInEffect;
        private final int virtualChannelId;
        private final boolean noRfAvailable;
        private final boolean noBitLock;
        private final boolean lockout;
        private final boolean wait;
        private final boolean retransmit;
        private final int farmBCounter;
        private final int reportValue;
        private final int rawValue;
        
        /**
         * Construct CLCW by decoding a 32-bit word.
         * 
         * @param clcwWord the CLCW as a 32-bit integer
         */
        CLCW(int clcwWord) {
            this.rawValue = clcwWord;
            
            // Bit 0: Type
            this.type = (clcwWord >>> 31) & 0x1;
            
            // Bits 1-2: Version
            this.version = (clcwWord >>> 29) & 0x3;
            
            // Bits 3-5: Status Field
            this.statusField = (clcwWord >>> 26) & 0x7;
            
            // Bits 6-7: COP in Effect
            this.copInEffect = (clcwWord >>> 24) & 0x3;
            
            // Bits 8-13: Virtual Channel ID
            this.virtualChannelId = (clcwWord >>> 18) & 0x3F;
            
            // Bit 16: No RF Available
            this.noRfAvailable = ((clcwWord >>> 15) & 0x1) == 1;
            
            // Bit 17: No Bit Lock
            this.noBitLock = ((clcwWord >>> 14) & 0x1) == 1;
            
            // Bit 18: Lockout
            this.lockout = ((clcwWord >>> 13) & 0x1) == 1;
            
            // Bit 19: Wait
            this.wait = ((clcwWord >>> 12) & 0x1) == 1;
            
            // Bit 20: Retransmit
            this.retransmit = ((clcwWord >>> 11) & 0x1) == 1;
            
            // Bits 21-22: FARM-B Counter
            this.farmBCounter = (clcwWord >>> 9) & 0x3;
            
            // Bits 24-31: Report Value
            this.reportValue = clcwWord & 0xFF;
        }
        
        /**
         * Get the type field (0 = Type-1 report).
         * 
         * @return the type field
         */
        public int getType() {
            return type;
        }
        
        /**
         * Get the version field.
         * 
         * @return the version field
         */
        public int getVersion() {
            return version;
        }
        
        /**
         * Get the status field (0 = nominal).
         * 
         * @return the status field
         */
        public int getStatusField() {
            return statusField;
        }
        
        /**
         * Get the COP in effect field.
         * 
         * @return the COP in effect field
         */
        public int getCopInEffect() {
            return copInEffect;
        }
        
        /**
         * Get the virtual channel ID.
         * 
         * @return the virtual channel ID (0-63)
         */
        public int getVirtualChannelId() {
            return virtualChannelId;
        }
        
        /**
         * Check if RF is available.
         * 
         * @return true if RF is not available
         */
        public boolean isNoRfAvailable() {
            return noRfAvailable;
        }
        
        /**
         * Check if bit lock is achieved.
         * 
         * @return true if bit lock is not achieved
         */
        public boolean isNoBitLock() {
            return noBitLock;
        }
        
        /**
         * Check if locked out.
         * 
         * @return true if locked out
         */
        public boolean isLockout() {
            return lockout;
        }
        
        /**
         * Check if waiting.
         * 
         * @return true if waiting
         */
        public boolean isWait() {
            return wait;
        }
        
        /**
         * Check if retransmit is requested.
         * 
         * @return true if retransmit requested
         */
        public boolean isRetransmit() {
            return retransmit;
        }
        
        /**
         * Get the FARM-B counter.
         * 
         * @return the FARM-B counter (0-3)
         */
        public int getFarmBCounter() {
            return farmBCounter;
        }
        
        /**
         * Get the report value (last received command frame count).
         * 
         * @return the report value (0-255)
         */
        public int getReportValue() {
            return reportValue;
        }
        
        /**
         * Get the raw CLCW value.
         * 
         * @return the raw 32-bit CLCW value
         */
        public int getRawValue() {
            return rawValue;
        }
        
        /**
         * Check if the CLCW indicates nominal status.
         * 
         * @return true if all status indicators are nominal
         */
        public boolean isNominal() {
            return statusField == 0 &&
                   !noRfAvailable &&
                   !noBitLock &&
                   !lockout &&
                   !wait &&
                   !retransmit;
        }
        
        @Override
        public String toString() {
            return String.format("CLCW[VCID=%d, ReportValue=%d, Status=%s]",
                    virtualChannelId,
                    reportValue,
                    isNominal() ? "NOMINAL" : "NON-NOMINAL");
        }
        
        /**
         * Get a detailed string representation of the CLCW.
         * 
         * @return detailed string representation
         */
        public String toDetailedString() {
            return String.format(
                    "CLCW[Type=%d, Version=%d, Status=%d, COP=%d, VCID=%d, " +
                    "NoRF=%b, NoBitLock=%b, Lockout=%b, Wait=%b, Retransmit=%b, " +
                    "FarmB=%d, ReportValue=%d]",
                    type, version, statusField, copInEffect, virtualChannelId,
                    noRfAvailable, noBitLock, lockout, wait, retransmit,
                    farmBCounter, reportValue);
        }
    }
}
