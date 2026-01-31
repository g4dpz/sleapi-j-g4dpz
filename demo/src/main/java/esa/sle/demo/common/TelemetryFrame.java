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
        ByteBuffer buffer = ByteBuffer.allocate(FRAME_SIZE);
        
        // Primary Header (6 bytes)
        // Version (2 bits) + Spacecraft ID (10 bits) + Virtual Channel ID (3 bits) + OCF Flag (1 bit)
        int ocfFlag = 1; // OCF is present
        int word1 = (0 << 14) | ((spacecraftId & 0x3FF) << 4) | ((virtualChannelId & 0x7) << 1) | ocfFlag;
        buffer.putShort((short) word1);
        
        // Master Channel Frame Count (8 bits) + Virtual Channel Frame Count (8 bits)
        buffer.put((byte) (frameCount >> 8));
        buffer.put((byte) (frameCount & 0xFF));
        
        // Transfer Frame Data Field Status (16 bits)
        // Bit 0: Transfer Frame Secondary Header Flag (0 = not present)
        // Bit 1: Sync Flag (1 = in sync)
        // Bit 2: Packet Order Flag (0 = not used)
        // Bit 3: Segment Length ID (00 = not used)
        // Bits 5-15: First Header Pointer (all 1s = no packet start)
        buffer.putShort((short) 0x4000); // Sync flag set
        
        // Data Field (1115 - 6 header - 4 OCF - 2 FECF = 1103 bytes)
        int dataLength = FRAME_SIZE - HEADER_SIZE - OCF_SIZE - FECF_SIZE;
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
        
        // Operational Control Field (OCF) - 4 bytes
        // Contains CLCW (Command Link Control Word) for command acknowledgment
        // CLCW Structure (32 bits):
        // - Type (1 bit) = 0 (Type-1 report)
        // - Version (2 bits) = 00
        // - Status Field (3 bits) = 000 (nominal)
        // - COP in Effect (2 bits) = 00
        // - Virtual Channel ID (6 bits) = virtualChannelId
        // - Spare (2 bits) = 00
        // - No RF Available (1 bit) = 0
        // - No Bit Lock (1 bit) = 0
        // - Lockout (1 bit) = 0
        // - Wait (1 bit) = 0
        // - Retransmit (1 bit) = 0
        // - FarmB Counter (2 bits) = 00
        // - Spare (1 bit) = 0
        // - Report Value (8 bits) = last command frame count acknowledged
        
        int clcw = 0;
        
        // Set VCID (bits 8-13)
        clcw |= (virtualChannelId & 0x3F) << 18;
        
        // Set Report Value (bits 24-31) - last command frame count received
        if (lastCommandReceived >= 0) {
            clcw |= (lastCommandReceived & 0xFF);
        }
        
        buffer.putInt(clcw);
        
        // Frame Error Control Field (FECF) - 2 bytes CRC-16
        // Calculate CRC-16-CCITT over entire frame (excluding FECF itself)
        byte[] frameWithoutFECF = new byte[FRAME_SIZE - FECF_SIZE];
        buffer.position(0);
        buffer.get(frameWithoutFECF);
        int crc = calculateCRC16(frameWithoutFECF);
        buffer.putShort((short) crc);
        
        return buffer.array();
    }
    
    /**
     * Calculate CRC-16-CCITT (polynomial 0x1021)
     * Used for CCSDS Frame Error Control Field
     */
    private int calculateCRC16(byte[] data) {
        int crc = 0xFFFF; // Initial value
        int polynomial = 0x1021; // CRC-16-CCITT polynomial
        
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
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("TM Frame [SCID=%d, VCID=%d, Count=%d, Size=%d bytes, Time=%s]",
                spacecraftId, virtualChannelId, frameCount, data.length, timestamp);
    }
}
