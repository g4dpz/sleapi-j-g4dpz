package esa.sle.demo.common;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Represents a CCSDS AOS Forward Frame for telecommands
 * Simplified version for demonstration purposes
 */
public class CommandFrame {
    
    private static final int FRAME_SIZE = 1115; // Standard CCSDS frame size
    private static final int HEADER_SIZE = 6;
    private static final int FECF_SIZE = 2;     // Frame Error Control Field (CRC-16)
    
    private final int spacecraftId;
    private final int virtualChannelId;
    private final int frameCount;
    private final String command;
    private final byte[] data;
    private final Instant timestamp;
    
    public CommandFrame(int spacecraftId, int virtualChannelId, int frameCount, String command) {
        this.spacecraftId = spacecraftId;
        this.virtualChannelId = virtualChannelId;
        this.frameCount = frameCount;
        this.command = command;
        this.timestamp = Instant.now();
        this.data = buildFrame();
    }
    
    /**
     * Parse a command frame from raw bytes
     */
    public CommandFrame(byte[] frameData) {
        this.data = frameData;
        this.timestamp = Instant.now();
        
        ByteBuffer buffer = ByteBuffer.wrap(frameData);
        
        // Parse header
        short word1 = buffer.getShort();
        this.spacecraftId = (word1 >> 4) & 0x3FF;
        this.virtualChannelId = (word1 >> 1) & 0x7;
        
        byte mcFrameCount = buffer.get();
        byte vcFrameCount = buffer.get();
        this.frameCount = ((mcFrameCount & 0xFF) << 8) | (vcFrameCount & 0xFF);
        
        buffer.getShort(); // Skip data field status
        
        // Extract command from data field
        int dataLength = frameData.length - HEADER_SIZE - FECF_SIZE;
        byte[] commandBytes = new byte[dataLength];
        buffer.get(commandBytes);
        
        // Find null terminator
        int commandLength = 0;
        for (int i = 0; i < commandBytes.length; i++) {
            if (commandBytes[i] == 0) {
                commandLength = i;
                break;
            }
        }
        if (commandLength == 0) commandLength = Math.min(256, commandBytes.length);
        
        this.command = new String(commandBytes, 0, commandLength);
    }
    
    private byte[] buildFrame() {
        ByteBuffer buffer = ByteBuffer.allocate(FRAME_SIZE);
        
        // Primary Header (6 bytes)
        // Version (2 bits) + Spacecraft ID (10 bits) + Virtual Channel ID (3 bits) + Reserved (1 bit)
        int word1 = (0 << 14) | ((spacecraftId & 0x3FF) << 4) | ((virtualChannelId & 0x7) << 1) | 0;
        buffer.putShort((short) word1);
        
        // Master Channel Frame Count (8 bits) + Virtual Channel Frame Count (8 bits)
        buffer.put((byte) (frameCount >> 8));
        buffer.put((byte) (frameCount & 0xFF));
        
        // Transfer Frame Data Field Status (16 bits)
        buffer.putShort((short) 0x8000); // Command frame indicator
        
        // Data Field - Command string
        byte[] commandBytes = command.getBytes();
        int dataLength = FRAME_SIZE - HEADER_SIZE - FECF_SIZE;
        
        // Copy command
        int copyLength = Math.min(commandBytes.length, dataLength);
        buffer.put(commandBytes, 0, copyLength);
        
        // Pad with zeros
        for (int i = copyLength; i < dataLength; i++) {
            buffer.put((byte) 0);
        }
        
        // Frame Error Control Field (FECF) - 2 bytes CRC-16
        byte[] frameWithoutFECF = new byte[FRAME_SIZE - FECF_SIZE];
        buffer.position(0);
        buffer.get(frameWithoutFECF);
        int crc = calculateCRC16(frameWithoutFECF);
        buffer.putShort((short) crc);
        
        return buffer.array();
    }
    
    /**
     * Calculate CRC-16-CCITT (polynomial 0x1021)
     */
    private int calculateCRC16(byte[] data) {
        int crc = 0xFFFF;
        int polynomial = 0x1021;
        
        for (byte b : data) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ polynomial;
                } else {
                    crc = crc << 1;
                }
            }
        }
        
        return crc & 0xFFFF;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public int getSpacecraftId() {
        return spacecraftId;
    }
    
    public int getVirtualChannelId() {
        return virtualChannelId;
    }
    
    public int getFrameCount() {
        return frameCount;
    }
    
    public String getCommand() {
        return command;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("CMD Frame [SCID=%d, VCID=%d, Count=%d, Command='%s', Time=%s]",
                spacecraftId, virtualChannelId, frameCount, command, timestamp);
    }
}
