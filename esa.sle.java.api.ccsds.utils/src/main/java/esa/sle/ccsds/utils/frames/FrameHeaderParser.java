package esa.sle.ccsds.utils.frames;

import java.nio.ByteBuffer;

/**
 * Parser for CCSDS Transfer Frame Headers
 * 
 * Parses the primary header (first 6 bytes) of CCSDS Transfer Frames
 * for both Telemetry (TM) and Telecommand (TC) frames.
 * 
 * Header Structure (6 bytes):
 * - Bytes 0-1: Version (2 bits) + Spacecraft ID (10 bits) + VCID (3 bits) + OCF Flag (1 bit)
 * - Bytes 2-3: Master Channel Frame Count (8 bits) + VC Frame Count (8 bits)
 * - Bytes 4-5: Data Field Status (16 bits)
 * 
 * Reference: CCSDS 732.0-B-3 (AOS Space Data Link Protocol)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class FrameHeaderParser {
    
    /**
     * Minimum frame size (header only)
     */
    private static final int MIN_FRAME_SIZE = 6;
    
    /**
     * Private constructor to prevent instantiation
     */
    private FrameHeaderParser() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Parse the header from a CCSDS Transfer Frame.
     * 
     * This method extracts all header fields from the first 6 bytes
     * of the frame data.
     * 
     * @param frameData the complete frame data (must be at least 6 bytes)
     * @return the parsed FrameHeader
     * @throws IllegalArgumentException if frameData is null or too short
     */
    public static FrameHeader parse(byte[] frameData) {
        if (frameData == null) {
            throw new IllegalArgumentException("Frame data must not be null");
        }
        if (frameData.length < MIN_FRAME_SIZE) {
            throw new IllegalArgumentException(
                    "Frame data must be at least " + MIN_FRAME_SIZE + " bytes");
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(frameData);
        
        // Parse first word (bytes 0-1)
        // Bits 0-1: Version
        // Bits 2-11: Spacecraft ID (10 bits)
        // Bits 12-14: Virtual Channel ID (3 bits)
        // Bit 15: OCF Flag
        short word1 = buffer.getShort();
        int version = (word1 >>> 14) & 0x3;
        int spacecraftId = (word1 >>> 4) & 0x3FF;
        int virtualChannelId = (word1 >>> 1) & 0x7;
        boolean ocfPresent = (word1 & 0x1) == 1;
        
        // Parse frame counts (bytes 2-3)
        byte mcFrameCount = buffer.get();
        byte vcFrameCount = buffer.get();
        int masterChannelFrameCount = mcFrameCount & 0xFF;
        int virtualChannelFrameCount = vcFrameCount & 0xFF;
        
        // Parse data field status (bytes 4-5)
        short dataFieldStatus = buffer.getShort();
        
        // Extract flags from data field status
        // Bit 0: Transfer Frame Secondary Header Flag
        // Bit 1: Sync Flag
        boolean secondaryHeaderPresent = ((dataFieldStatus >>> 15) & 0x1) == 1;
        boolean syncFlag = ((dataFieldStatus >>> 14) & 0x1) == 1;
        
        return new FrameHeader(
                version,
                spacecraftId,
                virtualChannelId,
                masterChannelFrameCount,
                virtualChannelFrameCount,
                dataFieldStatus & 0xFFFF,
                ocfPresent,
                secondaryHeaderPresent,
                syncFlag
        );
    }
    
    /**
     * Parse the header from a portion of frame data.
     * 
     * This is useful when the header is at a specific offset in a buffer.
     * 
     * @param data the data buffer
     * @param offset the offset where the header starts
     * @return the parsed FrameHeader
     * @throws IllegalArgumentException if data is null or offset is invalid
     */
    public static FrameHeader parse(byte[] data, int offset) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        if (offset < 0 || offset + MIN_FRAME_SIZE > data.length) {
            throw new IllegalArgumentException("Invalid offset or insufficient data");
        }
        
        // Extract header bytes
        byte[] headerBytes = new byte[MIN_FRAME_SIZE];
        System.arraycopy(data, offset, headerBytes, 0, MIN_FRAME_SIZE);
        
        return parse(headerBytes);
    }
    
    /**
     * Quick check if frame data appears to be a valid CCSDS frame.
     * 
     * This performs basic sanity checks on the header without full parsing.
     * 
     * @param frameData the frame data to check
     * @return true if the frame appears valid
     */
    public static boolean isValidFrame(byte[] frameData) {
        if (frameData == null || frameData.length < MIN_FRAME_SIZE) {
            return false;
        }
        
        try {
            FrameHeader header = parse(frameData);
            
            // Basic sanity checks
            if (header.getVersion() > 3) return false;
            if (header.getSpacecraftId() > 1023) return false;
            if (header.getVirtualChannelId() > 7) return false;
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extract just the spacecraft ID from frame data.
     * 
     * This is a convenience method for quick SCID extraction without
     * full header parsing.
     * 
     * @param frameData the frame data (must be at least 2 bytes)
     * @return the spacecraft ID (0-1023)
     * @throws IllegalArgumentException if frameData is null or too short
     */
    public static int extractSpacecraftId(byte[] frameData) {
        if (frameData == null || frameData.length < 2) {
            throw new IllegalArgumentException("Frame data must be at least 2 bytes");
        }
        
        short word1 = (short) (((frameData[0] & 0xFF) << 8) | (frameData[1] & 0xFF));
        return (word1 >>> 4) & 0x3FF;
    }
    
    /**
     * Extract just the virtual channel ID from frame data.
     * 
     * This is a convenience method for quick VCID extraction without
     * full header parsing.
     * 
     * @param frameData the frame data (must be at least 2 bytes)
     * @return the virtual channel ID (0-7)
     * @throws IllegalArgumentException if frameData is null or too short
     */
    public static int extractVirtualChannelId(byte[] frameData) {
        if (frameData == null || frameData.length < 2) {
            throw new IllegalArgumentException("Frame data must be at least 2 bytes");
        }
        
        short word1 = (short) (((frameData[0] & 0xFF) << 8) | (frameData[1] & 0xFF));
        return (word1 >>> 1) & 0x7;
    }
    
    /**
     * Extract just the frame count from frame data.
     * 
     * This is a convenience method for quick frame count extraction without
     * full header parsing.
     * 
     * @param frameData the frame data (must be at least 4 bytes)
     * @return the combined frame count (0-65535)
     * @throws IllegalArgumentException if frameData is null or too short
     */
    public static int extractFrameCount(byte[] frameData) {
        if (frameData == null || frameData.length < 4) {
            throw new IllegalArgumentException("Frame data must be at least 4 bytes");
        }
        
        int mcCount = frameData[2] & 0xFF;
        int vcCount = frameData[3] & 0xFF;
        return (mcCount << 8) | vcCount;
    }
}
