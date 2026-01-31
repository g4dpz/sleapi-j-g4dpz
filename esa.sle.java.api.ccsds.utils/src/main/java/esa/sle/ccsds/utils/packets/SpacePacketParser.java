package esa.sle.ccsds.utils.packets;

/**
 * CCSDS Space Packet Parser
 * 
 * Parses CCSDS Space Packets as specified in CCSDS 133.0-B-2 (Space Packet Protocol).
 * 
 * Space Packet structure:
 * - Primary Header (6 bytes):
 *   * Version (3 bits): Always 0 for CCSDS
 *   * Type (1 bit): 0=TM, 1=TC
 *   * Secondary Header Flag (1 bit): 1=present, 0=absent
 *   * APID (11 bits): Application Process ID
 *   * Sequence Flags (2 bits): 11=unsegmented, 01=first, 00=continuation, 10=last
 *   * Sequence Count (14 bits): Packet sequence number
 *   * Data Length (16 bits): Length of data field - 1
 * - Secondary Header (optional, variable length)
 * - User Data Field (variable length)
 * 
 * Reference: CCSDS 133.0-B-2 Section 4 (Space Packet Protocol)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class SpacePacketParser {
    
    /**
     * Minimum packet size (6 byte header + at least 1 byte data)
     */
    public static final int MIN_PACKET_SIZE = 7;
    
    /**
     * Private constructor to prevent instantiation
     */
    private SpacePacketParser() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Parse a Space Packet.
     * 
     * @param packetData the packet data (must be at least 7 bytes)
     * @return the parsed Space Packet
     * @throws IllegalArgumentException if packet data is invalid
     */
    public static SpacePacket parse(byte[] packetData) {
        if (packetData == null) {
            throw new IllegalArgumentException("Packet data must not be null");
        }
        if (packetData.length < MIN_PACKET_SIZE) {
            throw new IllegalArgumentException("Packet too short (minimum 7 bytes)");
        }
        
        // Parse primary header (6 bytes)
        
        // Byte 0-1: Version + Type + Secondary Header Flag + APID
        int word0 = ((packetData[0] & 0xFF) << 8) | (packetData[1] & 0xFF);
        
        int version = (word0 >>> 13) & 0b111;
        int type = (word0 >>> 12) & 0b1;
        boolean secondaryHeaderFlag = ((word0 >>> 11) & 0b1) == 1;
        int apid = word0 & 0x7FF;
        
        // Validate version
        if (version != 0) {
            throw new IllegalArgumentException("Invalid version (expected 0, got " + version + ")");
        }
        
        // Byte 2-3: Sequence Flags + Sequence Count
        int word1 = ((packetData[2] & 0xFF) << 8) | (packetData[3] & 0xFF);
        
        int sequenceFlags = (word1 >>> 14) & 0b11;
        int sequenceCount = word1 & 0x3FFF;
        
        // Byte 4-5: Data Length
        int dataLengthField = ((packetData[4] & 0xFF) << 8) | (packetData[5] & 0xFF);
        int dataLength = dataLengthField + 1;
        
        // Validate packet length
        int expectedLength = 6 + dataLength;
        if (packetData.length < expectedLength) {
            throw new IllegalArgumentException(
                "Packet too short (expected " + expectedLength + " bytes, got " + packetData.length + ")");
        }
        
        // Extract data field
        byte[] data = new byte[dataLength];
        System.arraycopy(packetData, 6, data, 0, dataLength);
        
        return new SpacePacket(version, type, secondaryHeaderFlag, apid, 
                              sequenceFlags, sequenceCount, dataLength, data);
    }
    
    /**
     * Extract APID from packet without full parsing.
     * 
     * @param packetData the packet data (must be at least 2 bytes)
     * @return the APID
     * @throws IllegalArgumentException if packet data is too short
     */
    public static int extractApid(byte[] packetData) {
        if (packetData == null || packetData.length < 2) {
            throw new IllegalArgumentException("Packet data must be at least 2 bytes");
        }
        
        int word0 = ((packetData[0] & 0xFF) << 8) | (packetData[1] & 0xFF);
        return word0 & 0x7FF;
    }
    
    /**
     * Extract sequence count from packet without full parsing.
     * 
     * @param packetData the packet data (must be at least 4 bytes)
     * @return the sequence count
     * @throws IllegalArgumentException if packet data is too short
     */
    public static int extractSequenceCount(byte[] packetData) {
        if (packetData == null || packetData.length < 4) {
            throw new IllegalArgumentException("Packet data must be at least 4 bytes");
        }
        
        int word1 = ((packetData[2] & 0xFF) << 8) | (packetData[3] & 0xFF);
        return word1 & 0x3FFF;
    }
    
    /**
     * Extract packet type from packet without full parsing.
     * 
     * @param packetData the packet data (must be at least 1 byte)
     * @return the packet type (0=TM, 1=TC)
     * @throws IllegalArgumentException if packet data is too short
     */
    public static int extractType(byte[] packetData) {
        if (packetData == null || packetData.length < 1) {
            throw new IllegalArgumentException("Packet data must be at least 1 byte");
        }
        
        return (packetData[0] >>> 4) & 0b1;
    }
    
    /**
     * Check if packet has secondary header without full parsing.
     * 
     * @param packetData the packet data (must be at least 1 byte)
     * @return true if secondary header is present
     * @throws IllegalArgumentException if packet data is too short
     */
    public static boolean hasSecondaryHeader(byte[] packetData) {
        if (packetData == null || packetData.length < 1) {
            throw new IllegalArgumentException("Packet data must be at least 1 byte");
        }
        
        return ((packetData[0] >>> 3) & 0b1) == 1;
    }
    
    /**
     * Validate packet structure without full parsing.
     * 
     * @param packetData the packet data
     * @return true if packet structure is valid
     */
    public static boolean isValidPacket(byte[] packetData) {
        if (packetData == null || packetData.length < MIN_PACKET_SIZE) {
            return false;
        }
        
        try {
            // Check version
            int version = (packetData[0] >>> 5) & 0b111;
            if (version != 0) {
                return false;
            }
            
            // Check data length consistency
            int dataLengthField = ((packetData[4] & 0xFF) << 8) | (packetData[5] & 0xFF);
            int expectedLength = 6 + dataLengthField + 1;
            
            return packetData.length >= expectedLength;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Immutable Space Packet representation.
     */
    public static class SpacePacket {
        private final int version;
        private final int type;
        private final boolean secondaryHeaderFlag;
        private final int apid;
        private final int sequenceFlags;
        private final int sequenceCount;
        private final int dataLength;
        private final byte[] data;
        
        /**
         * Create a Space Packet.
         * 
         * @param version the version number
         * @param type the packet type (0=TM, 1=TC)
         * @param secondaryHeaderFlag true if secondary header is present
         * @param apid the Application Process ID
         * @param sequenceFlags the sequence flags
         * @param sequenceCount the sequence count
         * @param dataLength the data field length
         * @param data the data field
         */
        public SpacePacket(int version, int type, boolean secondaryHeaderFlag, int apid,
                          int sequenceFlags, int sequenceCount, int dataLength, byte[] data) {
            this.version = version;
            this.type = type;
            this.secondaryHeaderFlag = secondaryHeaderFlag;
            this.apid = apid;
            this.sequenceFlags = sequenceFlags;
            this.sequenceCount = sequenceCount;
            this.dataLength = dataLength;
            this.data = data.clone(); // Defensive copy
        }
        
        /**
         * Get the version number (always 0 for CCSDS).
         * 
         * @return the version number
         */
        public int getVersion() {
            return version;
        }
        
        /**
         * Get the packet type.
         * 
         * @return the packet type (0=TM, 1=TC)
         */
        public int getType() {
            return type;
        }
        
        /**
         * Check if packet is telemetry.
         * 
         * @return true if packet is telemetry
         */
        public boolean isTelemetry() {
            return type == SpacePacketBuilder.TYPE_TM;
        }
        
        /**
         * Check if packet is telecommand.
         * 
         * @return true if packet is telecommand
         */
        public boolean isTelecommand() {
            return type == SpacePacketBuilder.TYPE_TC;
        }
        
        /**
         * Check if secondary header is present.
         * 
         * @return true if secondary header is present
         */
        public boolean hasSecondaryHeader() {
            return secondaryHeaderFlag;
        }
        
        /**
         * Get the Application Process ID.
         * 
         * @return the APID
         */
        public int getApid() {
            return apid;
        }
        
        /**
         * Get the sequence flags.
         * 
         * @return the sequence flags
         */
        public int getSequenceFlags() {
            return sequenceFlags;
        }
        
        /**
         * Check if packet is unsegmented.
         * 
         * @return true if packet is unsegmented
         */
        public boolean isUnsegmented() {
            return sequenceFlags == SpacePacketBuilder.SEQ_UNSEGMENTED;
        }
        
        /**
         * Check if packet is first segment.
         * 
         * @return true if packet is first segment
         */
        public boolean isFirstSegment() {
            return sequenceFlags == SpacePacketBuilder.SEQ_FIRST;
        }
        
        /**
         * Check if packet is continuation segment.
         * 
         * @return true if packet is continuation segment
         */
        public boolean isContinuationSegment() {
            return sequenceFlags == SpacePacketBuilder.SEQ_CONTINUATION;
        }
        
        /**
         * Check if packet is last segment.
         * 
         * @return true if packet is last segment
         */
        public boolean isLastSegment() {
            return sequenceFlags == SpacePacketBuilder.SEQ_LAST;
        }
        
        /**
         * Get the sequence count.
         * 
         * @return the sequence count
         */
        public int getSequenceCount() {
            return sequenceCount;
        }
        
        /**
         * Get the data field length.
         * 
         * @return the data field length
         */
        public int getDataLength() {
            return dataLength;
        }
        
        /**
         * Get the data field.
         * 
         * @return a copy of the data field
         */
        public byte[] getData() {
            return data.clone(); // Defensive copy
        }
        
        @Override
        public String toString() {
            return String.format("SpacePacket[APID=%d, Type=%s, SeqCount=%d, SeqFlags=%d, DataLen=%d, SecHdr=%s]",
                apid, 
                isTelemetry() ? "TM" : "TC",
                sequenceCount,
                sequenceFlags,
                dataLength,
                secondaryHeaderFlag ? "Yes" : "No");
        }
    }
}
