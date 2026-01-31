package esa.sle.ccsds.utils.frames;

import esa.sle.ccsds.utils.crc.CRC16Calculator;

import java.nio.ByteBuffer;

/**
 * CCSDS Telemetry Transfer Frame Builder
 * 
 * Builds complete CCSDS AOS Telemetry Transfer Frames as specified in
 * CCSDS 732.0-B-3 (AOS Space Data Link Protocol).
 * 
 * Frame structure:
 * - Primary Header (6 bytes)
 * - Transfer Frame Data Field (variable)
 * - Operational Control Field (4 bytes, optional)
 * - Frame Error Control Field (2 bytes, CRC-16)
 * 
 * Reference: CCSDS 732.0-B-3 Section 4.1 (Transfer Frame Structure)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class TelemetryFrameBuilder {
    
    /**
     * Default frame size (1115 bytes)
     */
    public static final int DEFAULT_FRAME_SIZE = 1115;
    
    /**
     * Primary header size (6 bytes)
     */
    public static final int HEADER_SIZE = 6;
    
    /**
     * Operational Control Field size (4 bytes)
     */
    public static final int OCF_SIZE = 4;
    
    /**
     * Frame Error Control Field size (2 bytes)
     */
    public static final int FECF_SIZE = 2;
    
    private int spacecraftId;
    private int virtualChannelId;
    private int frameCount;
    private byte[] data;
    private Integer ocf;  // Optional OCF (CLCW)
    private int frameSize = DEFAULT_FRAME_SIZE;
    private boolean includeOcf = true;
    
    /**
     * Private constructor - use builder() method
     */
    private TelemetryFrameBuilder() {
    }
    
    /**
     * Create a new Telemetry Frame builder.
     * 
     * @return a new builder instance
     */
    public static TelemetryFrameBuilder builder() {
        return new TelemetryFrameBuilder();
    }
    
    /**
     * Set the Spacecraft ID (SCID).
     * 
     * @param spacecraftId the spacecraft ID (0-1023, 10 bits)
     * @return this builder
     * @throws IllegalArgumentException if SCID is out of range
     */
    public TelemetryFrameBuilder setSpacecraftId(int spacecraftId) {
        if (spacecraftId < 0 || spacecraftId > 0x3FF) {
            throw new IllegalArgumentException("Spacecraft ID must be 0-1023");
        }
        this.spacecraftId = spacecraftId;
        return this;
    }
    
    /**
     * Set the Virtual Channel ID (VCID).
     * 
     * @param virtualChannelId the virtual channel ID (0-7, 3 bits for AOS)
     * @return this builder
     * @throws IllegalArgumentException if VCID is out of range
     */
    public TelemetryFrameBuilder setVirtualChannelId(int virtualChannelId) {
        if (virtualChannelId < 0 || virtualChannelId > 0x7) {
            throw new IllegalArgumentException("Virtual Channel ID must be 0-7");
        }
        this.virtualChannelId = virtualChannelId;
        return this;
    }
    
    /**
     * Set the frame count.
     * 
     * @param frameCount the frame count (0-65535, 16 bits total)
     * @return this builder
     * @throws IllegalArgumentException if frame count is out of range
     */
    public TelemetryFrameBuilder setFrameCount(int frameCount) {
        if (frameCount < 0 || frameCount > 0xFFFF) {
            throw new IllegalArgumentException("Frame count must be 0-65535");
        }
        this.frameCount = frameCount;
        return this;
    }
    
    /**
     * Set the frame data field.
     * 
     * @param data the data field (will be padded or truncated to fit frame)
     * @return this builder
     */
    public TelemetryFrameBuilder setData(byte[] data) {
        this.data = data;
        return this;
    }
    
    /**
     * Set the Operational Control Field (OCF) value.
     * Typically contains CLCW for command acknowledgment.
     * 
     * @param ocf the OCF value (32-bit CLCW)
     * @return this builder
     */
    public TelemetryFrameBuilder setOcf(int ocf) {
        this.ocf = ocf;
        this.includeOcf = true;
        return this;
    }
    
    /**
     * Set whether to include OCF in the frame.
     * 
     * @param include true to include OCF, false to omit
     * @return this builder
     */
    public TelemetryFrameBuilder setIncludeOcf(boolean include) {
        this.includeOcf = include;
        return this;
    }
    
    /**
     * Set the frame size.
     * 
     * @param frameSize the frame size in bytes (minimum 13 bytes)
     * @return this builder
     * @throws IllegalArgumentException if frame size is too small
     */
    public TelemetryFrameBuilder setFrameSize(int frameSize) {
        int minSize = HEADER_SIZE + (includeOcf ? OCF_SIZE : 0) + FECF_SIZE + 1;
        if (frameSize < minSize) {
            throw new IllegalArgumentException("Frame size too small (minimum " + minSize + " bytes)");
        }
        this.frameSize = frameSize;
        return this;
    }
    
    /**
     * Build the Telemetry Transfer Frame.
     * 
     * @return the complete frame with header, data, OCF (if included), and FECF
     * @throws IllegalStateException if required fields are not set
     */
    public byte[] build() {
        // Allocate frame buffer
        ByteBuffer buffer = ByteBuffer.allocate(frameSize);
        
        // Build Primary Header (6 bytes)
        // Byte 0-1: Version (2 bits) + Spacecraft ID (10 bits) + Virtual Channel ID (3 bits) + OCF Flag (1 bit)
        int ocfFlag = includeOcf ? 1 : 0;
        int word1 = (0 << 14) |                              // Version = 0 (CCSDS)
                    ((spacecraftId & 0x3FF) << 4) |          // Spacecraft ID (10 bits)
                    ((virtualChannelId & 0x7) << 1) |        // Virtual Channel ID (3 bits)
                    ocfFlag;                                  // OCF Flag (1 bit)
        buffer.putShort((short) word1);
        
        // Byte 2-3: Master Channel Frame Count (8 bits) + Virtual Channel Frame Count (8 bits)
        buffer.put((byte) ((frameCount >>> 8) & 0xFF));
        buffer.put((byte) (frameCount & 0xFF));
        
        // Byte 4-5: Transfer Frame Data Field Status (16 bits)
        // Bit 0: Transfer Frame Secondary Header Flag (0 = not present)
        // Bit 1: Sync Flag (1 = in sync)
        // Bit 2: Packet Order Flag (0 = not used)
        // Bit 3-4: Segment Length ID (00 = not used)
        // Bits 5-15: First Header Pointer (all 1s = no packet start)
        buffer.putShort((short) 0x4000); // Sync flag set, no packet start
        
        // Data Field
        int dataFieldSize = frameSize - HEADER_SIZE - (includeOcf ? OCF_SIZE : 0) - FECF_SIZE;
        
        if (data != null && data.length > 0) {
            int copyLength = Math.min(data.length, dataFieldSize);
            buffer.put(data, 0, copyLength);
            
            // Pad remaining with zeros
            for (int i = copyLength; i < dataFieldSize; i++) {
                buffer.put((byte) 0);
            }
        } else {
            // Fill with zeros if no data provided
            for (int i = 0; i < dataFieldSize; i++) {
                buffer.put((byte) 0);
            }
        }
        
        // Operational Control Field (OCF) - 4 bytes (if included)
        if (includeOcf) {
            int ocfValue = (ocf != null) ? ocf : 0;
            buffer.putInt(ocfValue);
        }
        
        // Frame Error Control Field (FECF) - 2 bytes CRC-16
        byte[] frameWithoutFECF = new byte[frameSize - FECF_SIZE];
        buffer.position(0);
        buffer.get(frameWithoutFECF);
        int crc = CRC16Calculator.calculate(frameWithoutFECF);
        buffer.putShort((short) crc);
        
        return buffer.array();
    }
    
    /**
     * Build a simple telemetry frame with default settings.
     * 
     * Convenience method for the most common use case.
     * 
     * @param spacecraftId the spacecraft ID
     * @param virtualChannelId the virtual channel ID
     * @param frameCount the frame count
     * @param data the frame data
     * @return the complete frame
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static byte[] buildSimple(int spacecraftId, int virtualChannelId, 
                                     int frameCount, byte[] data) {
        return builder()
            .setSpacecraftId(spacecraftId)
            .setVirtualChannelId(virtualChannelId)
            .setFrameCount(frameCount)
            .setData(data)
            .setIncludeOcf(true)
            .build();
    }
    
    /**
     * Build a telemetry frame with CLCW in OCF.
     * 
     * @param spacecraftId the spacecraft ID
     * @param virtualChannelId the virtual channel ID
     * @param frameCount the frame count
     * @param data the frame data
     * @param clcw the CLCW value for OCF
     * @return the complete frame
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static byte[] buildWithClcw(int spacecraftId, int virtualChannelId, 
                                       int frameCount, byte[] data, int clcw) {
        return builder()
            .setSpacecraftId(spacecraftId)
            .setVirtualChannelId(virtualChannelId)
            .setFrameCount(frameCount)
            .setData(data)
            .setOcf(clcw)
            .build();
    }
}
