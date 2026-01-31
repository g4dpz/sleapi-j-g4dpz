package esa.sle.demo.common;

import esa.sle.ccsds.utils.frames.FrameHeader;
import esa.sle.ccsds.utils.frames.FrameHeaderParser;

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
        
        // Parse header using library utility
        FrameHeader header = FrameHeaderParser.parse(frameData);
        this.spacecraftId = header.getSpacecraftId();
        this.virtualChannelId = header.getVirtualChannelId();
        this.frameCount = header.getFrameCount();
        
        // Extract command from data field
        ByteBuffer buffer = ByteBuffer.wrap(frameData);
        buffer.position(HEADER_SIZE); // Skip header
        
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
        // Use library utility to build complete frame
        return esa.sle.ccsds.utils.frames.CommandFrameBuilder.builder()
                .setSpacecraftId(spacecraftId)
                .setVirtualChannelId(virtualChannelId)
                .setFrameCount(frameCount)
                .setData(command.getBytes())
                .setFrameSize(FRAME_SIZE)
                .build();
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
