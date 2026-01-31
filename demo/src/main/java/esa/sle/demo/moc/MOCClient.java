package esa.sle.demo.moc;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mission Operations Center (MOC) Client
 * Receives telemetry frames from Ground Station via SLE RAF service
 */
public class MOCClient {
    
    private static final String GROUND_STATION_HOST = "localhost";
    private static final int GROUND_STATION_PORT = 5556;
    private static final int FRAME_SIZE = 1115;
    private static final DateTimeFormatter TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger framesReceived = new AtomicInteger(0);
    private final AtomicInteger dataVolume = new AtomicInteger(0);
    
    public static void main(String[] args) {
        MOCClient client = new MOCClient();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[MOC] Shutting down...");
            client.stop();
            client.printStatistics();
        }));
        
        client.start();
    }
    
    public void start() {
        running.set(true);
        System.out.println("=".repeat(80));
        System.out.println("MISSION OPERATIONS CENTER (MOC) CLIENT");
        System.out.println("=".repeat(80));
        System.out.println("Ground Station: " + GROUND_STATION_HOST + ":" + GROUND_STATION_PORT);
        System.out.println("Service: RAF (Return All Frames)");
        System.out.println("=".repeat(80));
        System.out.println();
        
        while (running.get()) {
            try (Socket socket = new Socket(GROUND_STATION_HOST, GROUND_STATION_PORT)) {
                System.out.println("[MOC] Connected to Ground Station");
                System.out.println("[MOC] Receiving telemetry frames...");
                System.out.println("-".repeat(80));
                
                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[FRAME_SIZE];
                
                while (running.get()) {
                    int bytesRead = 0;
                    while (bytesRead < FRAME_SIZE) {
                        int read = in.read(buffer, bytesRead, FRAME_SIZE - bytesRead);
                        if (read == -1) {
                            throw new IOException("Connection closed");
                        }
                        bytesRead += read;
                    }
                    
                    // Process received frame
                    processFrame(buffer);
                }
                
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[MOC] Connection error: " + e.getMessage());
                    System.out.println("[MOC] Retrying in 5 seconds...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        }
        
        System.out.println("[MOC] Stopped");
    }
    
    private void processFrame(byte[] frameData) {
        int frameNum = framesReceived.incrementAndGet();
        dataVolume.addAndGet(frameData.length);
        
        // Parse frame header
        ByteBuffer buffer = ByteBuffer.wrap(frameData);
        
        // Primary Header (6 bytes)
        short word1 = buffer.getShort();
        int version = (word1 >> 14) & 0x3;
        int spacecraftId = (word1 >> 4) & 0x3FF;
        int virtualChannelId = (word1 >> 1) & 0x7;
        int ocfFlag = word1 & 0x1;
        
        byte mcFrameCount = buffer.get();
        byte vcFrameCount = buffer.get();
        int frameCount = ((mcFrameCount & 0xFF) << 8) | (vcFrameCount & 0xFF);
        
        short dataFieldStatus = buffer.getShort();
        
        // Dump first frame in hex with section descriptions
        if (frameNum == 1) {
            dumpFrameHex(frameData);
        }
        
        // Extract message from data field (skip header, OCF, FECF)
        int dataStart = 6;
        int dataEnd = frameData.length - 6; // Exclude OCF (4) + FECF (2)
        byte[] dataField = new byte[dataEnd - dataStart];
        System.arraycopy(frameData, dataStart, dataField, 0, dataField.length);
        
        // Convert to string (find null terminator)
        int messageLength = 0;
        for (int i = 0; i < dataField.length; i++) {
            if (dataField[i] == 0) {
                messageLength = i;
                break;
            }
        }
        if (messageLength == 0) messageLength = Math.min(256, dataField.length);
        
        String message = new String(dataField, 0, messageLength);
        
        // Display frame information
        System.out.printf("[MOC] Frame #%d | SCID=%d VCID=%d Count=%d | Message: %s%n",
                frameNum,
                spacecraftId,
                virtualChannelId,
                frameCount,
                message
        );
        
        // Print statistics every 10 frames
        if (frameNum % 10 == 0) {
            System.out.println("-".repeat(80));
            System.out.printf("[MOC] Statistics: %d frames received, %.2f KB total%n",
                    frameNum, dataVolume.get() / 1024.0);
            System.out.println("-".repeat(80));
        }
    }
    
    private void dumpFrameHex(byte[] frameData) {
        // Calculate field positions
        int headerEnd = 6;
        int ocfStart = frameData.length - 6;
        int fecfStart = frameData.length - 2;
        
        // Build hex string with pipe separators
        StringBuilder hex = new StringBuilder();
        
        // Primary Header (6 bytes) - 3 fields separated by |
        hex.append(formatHexBytes(frameData, 0, 2)).append(" | ");
        hex.append(formatHexBytes(frameData, 2, 2)).append(" | ");
        hex.append(formatHexBytes(frameData, 4, 2)).append(" | ");
        
        // Data Field - show first 32 bytes
        hex.append(formatHexBytes(frameData, headerEnd, 32)).append(" ... | ");
        
        // OCF (4 bytes)
        hex.append(formatHexBytes(frameData, ocfStart, 4)).append(" | ");
        
        // FECF (2 bytes)
        hex.append(formatHexBytes(frameData, fecfStart, 2));
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CCSDS TM FRAME #1:");
        System.out.println(hex.toString());
        System.out.println();
        System.out.println("FIELD DEFINITIONS:");
        System.out.println("  [0-1]   0B 91       | Primary Header: Version+SCID+VCID+OCF_Flag");
        System.out.println("  [2-3]   00 00       | Primary Header: Frame Counts");
        System.out.println("  [4-5]   40 00       | Primary Header: Data Field Status");
        System.out.println("  [6-1108] ...        | Data Field (1103 bytes)");
        System.out.println("  [1109-1112] ...     | OCF - Operational Control Field (CLCW)");
        System.out.println("  [1113-1114] ...     | FECF - Frame Error Control (CRC-16)");
        System.out.println("=".repeat(80) + "\n");
    }
    
    /**
     * Calculate CRC-16-CCITT for verification
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
    
    private String formatHexBytes(byte[] data, int offset, int length) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (offset + i < data.length) {
                if (i > 0) hex.append(" ");
                hex.append(String.format("%02X", data[offset + i] & 0xFF));
            }
        }
        return hex.toString();
    }
    
    private void printStatistics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("MOC SESSION STATISTICS");
        System.out.println("=".repeat(80));
        System.out.printf("Total Frames Received: %d%n", framesReceived.get());
        System.out.printf("Total Data Volume: %.2f KB (%.2f MB)%n",
                dataVolume.get() / 1024.0,
                dataVolume.get() / (1024.0 * 1024.0));
        System.out.printf("Average Frame Size: %.2f bytes%n",
                framesReceived.get() > 0 ? (double) dataVolume.get() / framesReceived.get() : 0);
        System.out.println("=".repeat(80));
    }
    
    public void stop() {
        running.set(false);
    }
}
