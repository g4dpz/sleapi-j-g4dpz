package esa.sle.ccsds.utils.packets;

/**
 * CCSDS Space Packet Builder
 * 
 * Builds CCSDS Space Packets as specified in CCSDS 133.0-B-2 (Space Packet Protocol).
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
public class SpacePacketBuilder {
    
    /**
     * Packet type: Telemetry
     */
    public static final int TYPE_TM = 0;
    
    /**
     * Packet type: Telecommand
     */
    public static final int TYPE_TC = 1;
    
    /**
     * Sequence flags: Unsegmented packet (standalone)
     */
    public static final int SEQ_UNSEGMENTED = 0b11;
    
    /**
     * Sequence flags: First segment of segmented packet
     */
    public static final int SEQ_FIRST = 0b01;
    
    /**
     * Sequence flags: Continuation segment
     */
    public static final int SEQ_CONTINUATION = 0b00;
    
    /**
     * Sequence flags: Last segment
     */
    public static final int SEQ_LAST = 0b10;
    
    /**
     * Maximum APID value (11 bits)
     */
    public static final int MAX_APID = 0x7FF;
    
    /**
     * Maximum sequence count value (14 bits)
     */
    public static final int MAX_SEQUENCE_COUNT = 0x3FFF;
    
    /**
     * Maximum data length (16 bits, but represents length-1)
     */
    public static final int MAX_DATA_LENGTH = 0xFFFF;
    
    private int apid;
    private int sequenceCount;
    private int sequenceFlags = SEQ_UNSEGMENTED;
    private int type = TYPE_TM;
    private boolean secondaryHeaderFlag = false;
    private byte[] data;
    
    /**
     * Private constructor - use builder() method
     */
    private SpacePacketBuilder() {
    }
    
    /**
     * Create a new Space Packet builder.
     * 
     * @return a new builder instance
     */
    public static SpacePacketBuilder builder() {
        return new SpacePacketBuilder();
    }
    
    /**
     * Set the Application Process ID (APID).
     * 
     * @param apid the APID (0-2047)
     * @return this builder
     * @throws IllegalArgumentException if APID is out of range
     */
    public SpacePacketBuilder setApid(int apid) {
        if (apid < 0 || apid > MAX_APID) {
            throw new IllegalArgumentException("APID must be 0-2047");
        }
        this.apid = apid;
        return this;
    }
    
    /**
     * Set the packet sequence count.
     * 
     * @param sequenceCount the sequence count (0-16383)
     * @return this builder
     * @throws IllegalArgumentException if sequence count is out of range
     */
    public SpacePacketBuilder setSequenceCount(int sequenceCount) {
        if (sequenceCount < 0 || sequenceCount > MAX_SEQUENCE_COUNT) {
            throw new IllegalArgumentException("Sequence count must be 0-16383");
        }
        this.sequenceCount = sequenceCount;
        return this;
    }
    
    /**
     * Set the sequence flags.
     * 
     * @param sequenceFlags the sequence flags (use SEQ_* constants)
     * @return this builder
     * @throws IllegalArgumentException if sequence flags are invalid
     */
    public SpacePacketBuilder setSequenceFlags(int sequenceFlags) {
        if (sequenceFlags < 0 || sequenceFlags > 0b11) {
            throw new IllegalArgumentException("Sequence flags must be 0-3");
        }
        this.sequenceFlags = sequenceFlags;
        return this;
    }
    
    /**
     * Set the packet type.
     * 
     * @param type the packet type (TYPE_TM or TYPE_TC)
     * @return this builder
     * @throws IllegalArgumentException if type is invalid
     */
    public SpacePacketBuilder setType(int type) {
        if (type != TYPE_TM && type != TYPE_TC) {
            throw new IllegalArgumentException("Type must be TYPE_TM or TYPE_TC");
        }
        this.type = type;
        return this;
    }
    
    /**
     * Set whether a secondary header is present.
     * 
     * @param present true if secondary header is present
     * @return this builder
     */
    public SpacePacketBuilder setSecondaryHeaderFlag(boolean present) {
        this.secondaryHeaderFlag = present;
        return this;
    }
    
    /**
     * Set the packet data field (including secondary header if present).
     * 
     * @param data the data field
     * @return this builder
     * @throws IllegalArgumentException if data is null or too large
     */
    public SpacePacketBuilder setData(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        if (data.length > MAX_DATA_LENGTH + 1) {
            throw new IllegalArgumentException("Data too large (max 65536 bytes)");
        }
        this.data = data;
        return this;
    }
    
    /**
     * Build the Space Packet.
     * 
     * @return the complete Space Packet (6 byte header + data)
     * @throws IllegalStateException if required fields are not set
     */
    public byte[] build() {
        if (data == null) {
            throw new IllegalStateException("Data must be set");
        }
        
        // Calculate data length field (actual length - 1)
        int dataLengthField = data.length - 1;
        
        // Allocate packet buffer (6 byte header + data)
        byte[] packet = new byte[6 + data.length];
        
        // Build primary header (6 bytes)
        
        // Byte 0-1: Version (3 bits) + Type (1 bit) + Secondary Header Flag (1 bit) + APID (11 bits)
        int word0 = 0;
        word0 |= (0 << 13);                              // Version = 0 (CCSDS)
        word0 |= (type << 12);                           // Type
        word0 |= ((secondaryHeaderFlag ? 1 : 0) << 11);  // Secondary Header Flag
        word0 |= (apid & 0x7FF);                         // APID (11 bits)
        
        packet[0] = (byte) ((word0 >>> 8) & 0xFF);
        packet[1] = (byte) (word0 & 0xFF);
        
        // Byte 2-3: Sequence Flags (2 bits) + Sequence Count (14 bits)
        int word1 = 0;
        word1 |= ((sequenceFlags & 0b11) << 14);  // Sequence Flags (2 bits)
        word1 |= (sequenceCount & 0x3FFF);        // Sequence Count (14 bits)
        
        packet[2] = (byte) ((word1 >>> 8) & 0xFF);
        packet[3] = (byte) (word1 & 0xFF);
        
        // Byte 4-5: Data Length (16 bits)
        packet[4] = (byte) ((dataLengthField >>> 8) & 0xFF);
        packet[5] = (byte) (dataLengthField & 0xFF);
        
        // Copy data field
        System.arraycopy(data, 0, packet, 6, data.length);
        
        return packet;
    }
    
    /**
     * Build a simple unsegmented telemetry packet.
     * 
     * Convenience method for the most common use case.
     * 
     * @param apid the Application Process ID
     * @param sequenceCount the packet sequence count
     * @param data the packet data
     * @return the complete Space Packet
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static byte[] buildSimple(int apid, int sequenceCount, byte[] data) {
        return builder()
            .setApid(apid)
            .setSequenceCount(sequenceCount)
            .setSequenceFlags(SEQ_UNSEGMENTED)
            .setType(TYPE_TM)
            .setSecondaryHeaderFlag(false)
            .setData(data)
            .build();
    }
}
