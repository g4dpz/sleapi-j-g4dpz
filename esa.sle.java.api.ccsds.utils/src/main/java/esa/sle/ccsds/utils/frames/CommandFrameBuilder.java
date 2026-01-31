package esa.sle.ccsds.utils.frames;

import esa.sle.ccsds.utils.crc.CRC16Calculator;

import java.nio.ByteBuffer;

/**
 * CCSDS Telecommand Transfer Frame Builder
 * 
 * Builds complete CCSDS TC Transfer Frames as specified in
 * CCSDS 232.0-B-3 (TC Space Data Link Protocol).
 * 
 * Frame structure:
 * - Primary Header (5 bytes)
 * - Frame Data Field (variable)
 * - Frame Error Control Field (2 bytes, CRC-16)
 * 
 * Note: This implementation uses 6-byte header (compatible with AOS format)
 * for consistency with the demo. Standard TC frames use 5-byte header.
 * 
 * Reference: CCSDS 232.0-B-3 Section 4.1 (Transfer Frame Structure)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class CommandFrameBuilder {
    
    /**
     * Default frame size (1115 bytes)
     */
    public static final int DEFAULT_FRAME_SIZE = 1115;
    
    /**
     * Primary header size (6 bytes for AOS-compatible format)
     */
    public static final int HEADER_SIZE = 6;
    
    /**
     * Frame Error Control Field size (2 bytes)
     */
    public static final int FECF_SIZE = 2;
    
    private int spacecraftId;
    private int virtualChannelId;
    private int frameCount;
    private byte[] data;
    private int frameSize = DEFAULT_FRAME_SIZE;
    
    /**
     * Private constructor - use builder() method
     */
    private CommandFrameBuilder() {
    }
    
    /**
     * Create a new Command Frame builder.
     * 
     * @return a new builder instance
     */
    public static CommandFrameBuilder builder() {
        return new CommandFrameBuilder();
    }
    
    /**
     * Set the Spacecraft ID (SCID).
     * 
     * @param spacecraftId the spacecraft ID (0-1023, 10 bits)
     * @return this builder
     * @throws IllegalArgumentException if SCID is out of range
     */
    public CommandFrameBuilder setSpacecraftId(int spacecraftId) {
        if (spacecraftId < 0 || spacecraftId > 0x3FF) {
            throw new IllegalArgumentException("Spacecraft ID must be 0-1023");
        }
        this.spacecraftId = spacecraftId;
        return this;
    }
    
    /**
     * Set the Virtual Channel ID (VCID).
     * 
     * @param virtualChannelId the virtual channel ID (0-7, 3 bits)
     * @return this builder
     * @throws IllegalArgumentException if VCID is out of range
     */
    public CommandFrameBuilder setVirtualChannelId(int virtualChannelId) {
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
    public CommandFrameBuilder setFrameCount(int frameCount) {
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
    public CommandFrameBuilder setData(byte[] data) {
        this.data = data;
        return this;
    }
    
    /**
     * Set the frame size.
     * 
     * @param frameSize the frame size in bytes (minimum 9 bytes)
     * @return this builder
     * @throws IllegalArgumentException if frame size is too small
     */
    public CommandFrameBuilder setFrameSize(int frameSize) {
        int minSize = HEADER_SIZE + FECF_SIZE + 1;
        if (frameSize < minSize) {
            throw new IllegalArgumentException("Frame size too small (minimum " + minSize + " bytes)");
        }
        this.frameSize = frameSize;
        return this;
    }
    
    /**
     * Build the Command Transfer Frame.
     * 
     * @return the complete frame with header, data, and FECF
     * @throws IllegalStateException if required fields are not set
     */
    public byte[] build() {
        // Allocate frame buffer
        ByteBuffer buffer = ByteBuffer.allocate(frameSize);
        
        // Build Primary Header (6 bytes - AOS-compatible format)
        // Byte 0-1: Version (2 bits) + Spacecraft ID (10 bits) + Virtual Channel ID (3 bits) + Reserved (1 bit)
        int word1 = (0 << 14) |                              // Version = 0 (CCSDS)
                    ((spacecraftId & 0x3FF) << 4) |          // Spacecraft ID (10 bits)
                    ((virtualChannelId & 0x7) << 1) |        // Virtual Channel ID (3 bits)
                    0;                                        // Reserved (1 bit)
        buffer.putShort((short) word1);
        
        // Byte 2-3: Master Channel Frame Count (8 bits) + Virtual Channel Frame Count (8 bits)
        buffer.put((byte) ((frameCount >>> 8) & 0xFF));
        buffer.put((byte) (frameCount & 0xFF));
        
        // Byte 4-5: Transfer Frame Data Field Status (16 bits)
        // For command frames, set bit 15 to indicate command frame
        buffer.putShort((short) 0x8000); // Command frame indicator
        
        // Data Field
        int dataFieldSize = frameSize - HEADER_SIZE - FECF_SIZE;
        
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
        
        // Frame Error Control Field (FECF) - 2 bytes CRC-16
        byte[] frameWithoutFECF = new byte[frameSize - FECF_SIZE];
        buffer.position(0);
        buffer.get(frameWithoutFECF);
        int crc = CRC16Calculator.calculate(frameWithoutFECF);
        buffer.putShort((short) crc);
        
        return buffer.array();
    }
    
    /**
     * Build a simple command frame with default settings.
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
            .build();
    }
}
