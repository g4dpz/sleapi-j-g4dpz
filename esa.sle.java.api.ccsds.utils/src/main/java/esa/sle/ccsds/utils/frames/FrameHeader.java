package esa.sle.ccsds.utils.frames;

/**
 * Represents a parsed CCSDS Transfer Frame Header
 * 
 * Immutable object containing all header fields from a CCSDS Transfer Frame.
 * Supports both Telemetry (TM) and Telecommand (TC) frame headers.
 * 
 * Reference: CCSDS 732.0-B-3 (AOS Space Data Link Protocol)
 * 
 * @author SLE Java API Team
 * @version 1.0
 */
public class FrameHeader {
    
    private final int version;
    private final int spacecraftId;
    private final int virtualChannelId;
    private final int masterChannelFrameCount;
    private final int virtualChannelFrameCount;
    private final int dataFieldStatus;
    private final boolean ocfPresent;
    private final boolean secondaryHeaderPresent;
    private final boolean syncFlag;
    
    /**
     * Construct a FrameHeader with all fields.
     * 
     * @param version the transfer frame version number (0-3)
     * @param spacecraftId the spacecraft identifier (0-1023)
     * @param virtualChannelId the virtual channel identifier (0-7)
     * @param masterChannelFrameCount the master channel frame count (0-255)
     * @param virtualChannelFrameCount the virtual channel frame count (0-255)
     * @param dataFieldStatus the data field status word
     * @param ocfPresent true if Operational Control Field is present
     * @param secondaryHeaderPresent true if secondary header is present
     * @param syncFlag true if in sync
     */
    FrameHeader(int version, int spacecraftId, int virtualChannelId,
                int masterChannelFrameCount, int virtualChannelFrameCount,
                int dataFieldStatus, boolean ocfPresent,
                boolean secondaryHeaderPresent, boolean syncFlag) {
        this.version = version;
        this.spacecraftId = spacecraftId;
        this.virtualChannelId = virtualChannelId;
        this.masterChannelFrameCount = masterChannelFrameCount;
        this.virtualChannelFrameCount = virtualChannelFrameCount;
        this.dataFieldStatus = dataFieldStatus;
        this.ocfPresent = ocfPresent;
        this.secondaryHeaderPresent = secondaryHeaderPresent;
        this.syncFlag = syncFlag;
    }
    
    /**
     * Get the transfer frame version number.
     * 
     * @return the version number (0-3)
     */
    public int getVersion() {
        return version;
    }
    
    /**
     * Get the spacecraft identifier (SCID).
     * 
     * @return the spacecraft ID (0-1023)
     */
    public int getSpacecraftId() {
        return spacecraftId;
    }
    
    /**
     * Get the virtual channel identifier (VCID).
     * 
     * @return the virtual channel ID (0-7)
     */
    public int getVirtualChannelId() {
        return virtualChannelId;
    }
    
    /**
     * Get the master channel frame count.
     * 
     * @return the master channel frame count (0-255)
     */
    public int getMasterChannelFrameCount() {
        return masterChannelFrameCount;
    }
    
    /**
     * Get the virtual channel frame count.
     * 
     * @return the virtual channel frame count (0-255)
     */
    public int getVirtualChannelFrameCount() {
        return virtualChannelFrameCount;
    }
    
    /**
     * Get the combined frame count (MC + VC).
     * 
     * This is a convenience method that combines the master channel
     * and virtual channel frame counts into a single 16-bit value.
     * 
     * @return the combined frame count (0-65535)
     */
    public int getFrameCount() {
        return (masterChannelFrameCount << 8) | virtualChannelFrameCount;
    }
    
    /**
     * Get the data field status word.
     * 
     * @return the data field status (16-bit value)
     */
    public int getDataFieldStatus() {
        return dataFieldStatus;
    }
    
    /**
     * Check if Operational Control Field (OCF) is present.
     * 
     * @return true if OCF is present in the frame
     */
    public boolean isOcfPresent() {
        return ocfPresent;
    }
    
    /**
     * Check if secondary header is present.
     * 
     * @return true if secondary header is present
     */
    public boolean isSecondaryHeaderPresent() {
        return secondaryHeaderPresent;
    }
    
    /**
     * Check if frame is in sync.
     * 
     * @return true if sync flag is set
     */
    public boolean isSyncFlag() {
        return syncFlag;
    }
    
    /**
     * Check if this is a command frame.
     * 
     * Command frames typically have bit 0 of the data field status set.
     * 
     * @return true if this appears to be a command frame
     */
    public boolean isCommandFrame() {
        return (dataFieldStatus & 0x8000) != 0;
    }
    
    /**
     * Check if this is a telemetry frame.
     * 
     * Telemetry frames typically have bit 0 of the data field status clear.
     * 
     * @return true if this appears to be a telemetry frame
     */
    public boolean isTelemetryFrame() {
        return !isCommandFrame();
    }
    
    @Override
    public String toString() {
        return String.format("FrameHeader[SCID=%d, VCID=%d, Count=%d, OCF=%b, Type=%s]",
                spacecraftId, virtualChannelId, getFrameCount(),
                ocfPresent, isCommandFrame() ? "CMD" : "TM");
    }
    
    /**
     * Get a detailed string representation of the header.
     * 
     * @return detailed string representation
     */
    public String toDetailedString() {
        return String.format(
                "FrameHeader[Version=%d, SCID=%d, VCID=%d, " +
                "MC_Count=%d, VC_Count=%d, Combined_Count=%d, " +
                "DataFieldStatus=0x%04X, OCF=%b, SecHdr=%b, Sync=%b, Type=%s]",
                version, spacecraftId, virtualChannelId,
                masterChannelFrameCount, virtualChannelFrameCount, getFrameCount(),
                dataFieldStatus, ocfPresent, secondaryHeaderPresent, syncFlag,
                isCommandFrame() ? "COMMAND" : "TELEMETRY");
    }
}
