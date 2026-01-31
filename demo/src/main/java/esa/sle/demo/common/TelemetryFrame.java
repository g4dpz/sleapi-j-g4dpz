package esa.sle.demo.common;

import esa.sle.ccsds.utils.clcw.CLCWEncoder;

import java.time.Instant;

/**
 * Represents a CCSDS Telemetry Transfer Frame
 * Simplified version for demonstration purposes
 */
public class TelemetryFrame {
    
    private static final int FRAME_SIZE = 1115; // Standard CCSDS frame size
    private static final int HEADER_SIZE = 6;
    private static final int OCF_SIZE = 4;      // Operational Control Field
    private static final int FECF_SIZE = 2;     // Frame Error Control Field (CRC-16)
    
    private final int spacecraftId;
    private final int virtualChannelId;
    private final int frameCount;
    private final byte[] data;
    private final Instant timestamp;
    private int lastCommandReceived = -1;  // For CLCW report value
    
    public TelemetryFrame(int spacecraftId, int virtualChannelId, int frameCount, byte[] payload) {
        this(spacecraftId, virtualChannelId, frameCount, payload, -1);
    }
    
    public TelemetryFrame(int spacecraftId, int virtualChannelId, int frameCount, byte[] payload, int commandAck) {
        this.spacecraftId = spacecraftId;
        this.virtualChannelId = virtualChannelId;
        this.frameCount = frameCount;
        this.timestamp = Instant.now();
        this.lastCommandReceived = commandAck;
        this.data = buildFrame(payload);
    }
    
    private byte[] buildFrame(byte[] payload) {
        // Build CLCW for OCF
        int clcw = (lastCommandReceived >= 0) ?
                CLCWEncoder.encode(virtualChannelId, lastCommandReceived) :
                CLCWEncoder.encode(virtualChannelId, 0);
        
        // Use library utility to build complete frame
        return esa.sle.ccsds.utils.frames.TelemetryFrameBuilder.builder()
                .setSpacecraftId(spacecraftId)
                .setVirtualChannelId(virtualChannelId)
                .setFrameCount(frameCount)
                .setData(payload)
                .setOcf(clcw)
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
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("TM Frame [SCID=%d, VCID=%d, Count=%d, Size=%d bytes, Time=%s]",
                spacecraftId, virtualChannelId, frameCount, data.length, timestamp);
    }
}
