package esa.sle.ccsds.utils.clcw;

/**
 * CLCW (Communications Link Control Word) Encoder
 * 
 * Encodes CLCW for inclusion in the Operational Control Field (OCF) of
 * CCSDS Telemetry Transfer Frames.
 * 
 * CLCW Structure (32 bits):
 * - Type (1 bit): 0 = Type-1 report
 * - Version (2 bits): 00
 * - Status Field (3 bits): 000 = nominal
 * - COP in Effect (2 bits): 00
 * - Virtual Channel ID (6 bits): 0-63
 * - Spare (2 bits): 00
 * - No RF Available (1 bit): 0 = RF available
 * - No Bit Lock (1 bit): 0 = bit lock
 * - Lockout (1 bit): 0 = not locked out
 * - Wait (1 bit): 0 = not waiting
 * - Retransmit (1 bit): 0 = no retransmit
 * - FarmB Counter (2 bits): 00
 * - Spare (1 bit): 0
 * - Report Value (8 bits): 0-255 (last received command frame count)
 * 
 * Reference: CCSDS 232.0-B-3 (TC Space Data Link Protocol)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class CLCWEncoder {
    
    /**
     * Private constructor to prevent instantiation
     */
    private CLCWEncoder() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Encode a simple CLCW with only VCID and Report Value set.
     * 
     * This is the most common use case for command acknowledgment.
     * All other fields are set to their nominal values.
     * 
     * @param virtualChannelId the virtual channel ID (0-63)
     * @param reportValue the report value (last received command frame count, 0-255)
     * @return the encoded CLCW as a 32-bit integer
     * @throws IllegalArgumentException if parameters are out of range
     */
    public static int encode(int virtualChannelId, int reportValue) {
        if (virtualChannelId < 0 || virtualChannelId > 63) {
            throw new IllegalArgumentException("Virtual Channel ID must be 0-63");
        }
        if (reportValue < 0 || reportValue > 255) {
            throw new IllegalArgumentException("Report Value must be 0-255");
        }
        
        int clcw = 0;
        
        // Set VCID (bits 8-13)
        clcw |= (virtualChannelId & 0x3F) << 18;
        
        // Set Report Value (bits 24-31)
        clcw |= (reportValue & 0xFF);
        
        return clcw;
    }
    
    /**
     * Create a builder for constructing CLCW with all fields.
     * 
     * @return a new CLCWBuilder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for constructing CLCW with full control over all fields.
     */
    public static class Builder {
        private int type = 0;              // Type-1 report
        private int version = 0;           // Version 0
        private int statusField = 0;       // Nominal
        private int copInEffect = 0;       // COP-1
        private int vcid = 0;              // Virtual Channel ID
        private boolean noRfAvailable = false;
        private boolean noBitLock = false;
        private boolean lockout = false;
        private boolean wait = false;
        private boolean retransmit = false;
        private int farmBCounter = 0;
        private int reportValue = 0;
        
        /**
         * Set the virtual channel ID (0-63).
         * 
         * @param vcid the virtual channel ID
         * @return this builder
         * @throws IllegalArgumentException if vcid is out of range
         */
        public Builder setVirtualChannelId(int vcid) {
            if (vcid < 0 || vcid > 63) {
                throw new IllegalArgumentException("Virtual Channel ID must be 0-63");
            }
            this.vcid = vcid;
            return this;
        }
        
        /**
         * Set the report value (last received command frame count, 0-255).
         * 
         * @param value the report value
         * @return this builder
         * @throws IllegalArgumentException if value is out of range
         */
        public Builder setReportValue(int value) {
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException("Report Value must be 0-255");
            }
            this.reportValue = value;
            return this;
        }
        
        /**
         * Set the status field (0-7).
         * 
         * @param status the status field value
         * @return this builder
         * @throws IllegalArgumentException if status is out of range
         */
        public Builder setStatusField(int status) {
            if (status < 0 || status > 7) {
                throw new IllegalArgumentException("Status Field must be 0-7");
            }
            this.statusField = status;
            return this;
        }
        
        /**
         * Set the COP in effect (0-3).
         * 
         * @param cop the COP value
         * @return this builder
         * @throws IllegalArgumentException if cop is out of range
         */
        public Builder setCopInEffect(int cop) {
            if (cop < 0 || cop > 3) {
                throw new IllegalArgumentException("COP in Effect must be 0-3");
            }
            this.copInEffect = cop;
            return this;
        }
        
        /**
         * Set the No RF Available flag.
         * 
         * @param noRf true if RF is not available
         * @return this builder
         */
        public Builder setNoRfAvailable(boolean noRf) {
            this.noRfAvailable = noRf;
            return this;
        }
        
        /**
         * Set the No Bit Lock flag.
         * 
         * @param noBitLock true if bit lock is not achieved
         * @return this builder
         */
        public Builder setNoBitLock(boolean noBitLock) {
            this.noBitLock = noBitLock;
            return this;
        }
        
        /**
         * Set the Lockout flag.
         * 
         * @param lockout true if locked out
         * @return this builder
         */
        public Builder setLockout(boolean lockout) {
            this.lockout = lockout;
            return this;
        }
        
        /**
         * Set the Wait flag.
         * 
         * @param wait true if waiting
         * @return this builder
         */
        public Builder setWait(boolean wait) {
            this.wait = wait;
            return this;
        }
        
        /**
         * Set the Retransmit flag.
         * 
         * @param retransmit true if retransmit requested
         * @return this builder
         */
        public Builder setRetransmit(boolean retransmit) {
            this.retransmit = retransmit;
            return this;
        }
        
        /**
         * Set the FARM-B counter (0-3).
         * 
         * @param counter the FARM-B counter value
         * @return this builder
         * @throws IllegalArgumentException if counter is out of range
         */
        public Builder setFarmBCounter(int counter) {
            if (counter < 0 || counter > 3) {
                throw new IllegalArgumentException("FARM-B Counter must be 0-3");
            }
            this.farmBCounter = counter;
            return this;
        }
        
        /**
         * Build the CLCW.
         * 
         * @return the encoded CLCW as a 32-bit integer
         */
        public int build() {
            int clcw = 0;
            
            // Bit 0: Type (always 0 for Type-1)
            clcw |= (type & 0x1) << 31;
            
            // Bits 1-2: Version
            clcw |= (version & 0x3) << 29;
            
            // Bits 3-5: Status Field
            clcw |= (statusField & 0x7) << 26;
            
            // Bits 6-7: COP in Effect
            clcw |= (copInEffect & 0x3) << 24;
            
            // Bits 8-13: Virtual Channel ID
            clcw |= (vcid & 0x3F) << 18;
            
            // Bits 14-15: Spare (always 0)
            
            // Bit 16: No RF Available
            if (noRfAvailable) clcw |= (1 << 15);
            
            // Bit 17: No Bit Lock
            if (noBitLock) clcw |= (1 << 14);
            
            // Bit 18: Lockout
            if (lockout) clcw |= (1 << 13);
            
            // Bit 19: Wait
            if (wait) clcw |= (1 << 12);
            
            // Bit 20: Retransmit
            if (retransmit) clcw |= (1 << 11);
            
            // Bits 21-22: FARM-B Counter
            clcw |= (farmBCounter & 0x3) << 9;
            
            // Bit 23: Spare (always 0)
            
            // Bits 24-31: Report Value
            clcw |= (reportValue & 0xFF);
            
            return clcw;
        }
    }
}
