package esa.sle.demo.common;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Represents a CCSDS Telemetry Transfer Frame
 * Simplified version for demonstration purposes
 */
public class TelemetryFrame {
    
    private static final int FRAME_SIZE = 1115; // Standard CCSDS frame size
    private static final int HEADER_SIZE = 6;
    
    private final int spacecraftId;
    private final int virtualChannelId;
    private final int frameCount;
    private final byte[] data;
    private final Instant timestamp;
    
    public TelemetryFrame(int spacecraftId, int virtualChannelId, int frameCount, byte[] payload) {
        this.spacecraftId = spacecraftId;
        this.virtualChannelId = virtualChannelId;
        this.frameCount = frameCount;
        this.timestamp = Instant.now();
        this.data = buildFrame(payload);
    }
    
    private byte[] buildFrame(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.allocate(FRAME_SIZE);
        
        // Primary Header (6 bytes)
        // Version (2 bits) + Spacecraft ID (10 bits) + Virtual Channel ID (3 bits) + OCF Flag (1 bit)
        int word1 = (0 << 14) | ((spacecraftId & 0x3FF) << 4) | ((virtualChannelId & 0x7) << 1) | 0;
        buffer.putShort((short) word1);
        
        // Master Channel Frame Count (8 bits) + Virtual Channel Frame Count (8 bits)
        buffer.put((byte) (frameCount >> 8));
        buffer.put((byte) (frameCount & 0xFF));
        
        // Transfer Frame Data Field Status (16 bits)
        buffer.putShort((short) 0);
        
        // Data Field
        int dataLength = FRAME_SIZE - HEADER_SIZE;
        if (payload != null && payload.length > 0) {
            int copyLength = Math.min(payload.length, dataLength);
            buffer.put(payload, 0, copyLength);
            // Pad remaining with zeros
            for (int i = copyLength; i < dataLength; i++) {
                buffer.put((byte) 0);
            }
        } else {
            // Fill with test pattern
            for (int i = 0; i < dataLength; i++) {
                buffer.put((byte) (i % 256));
            }
        }
        
        return buffer.array();
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
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("TM Frame [SCID=%d, VCID=%d, Count=%d, Size=%d bytes, Time=%s]",
                spacecraftId, virtualChannelId, frameCount, data.length, timestamp);
    }
}
