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
        
        // Extract sample telemetry data (if present)
        if (buffer.remaining() >= 256) {
            long timestamp = buffer.getLong();
            float temperature = buffer.getFloat();
            float voltage = buffer.getFloat();
            float current = buffer.getFloat();
            int attitude = buffer.getInt();
            int altitude = buffer.getInt();
            byte solarPanels = buffer.get();
            byte antenna = buffer.get();
            byte systemStatus = buffer.get();
            
            // Display frame information
            System.out.printf("[MOC] Frame #%d | SCID=%d VCID=%d Count=%d | " +
                            "Temp=%.1f°C Volt=%.1fV Curr=%.1fA Alt=%dkm Att=%d° | " +
                            "Solar=%s Ant=%s Sys=%s | Time=%s%n",
                    frameNum,
                    spacecraftId,
                    virtualChannelId,
                    frameCount,
                    temperature,
                    voltage,
                    current,
                    altitude,
                    attitude,
                    solarPanels == 1 ? "✓" : "✗",
                    antenna == 1 ? "✓" : "✗",
                    systemStatus == 1 ? "OK" : "ERR",
                    TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp))
            );
        } else {
            // Just show frame metadata
            System.out.printf("[MOC] Frame #%d | SCID=%d VCID=%d Count=%d | Size=%d bytes%n",
                    frameNum,
                    spacecraftId,
                    virtualChannelId,
                    frameCount,
                    frameData.length
            );
        }
        
        // Print statistics every 10 frames
        if (frameNum % 10 == 0) {
            System.out.println("-".repeat(80));
            System.out.printf("[MOC] Statistics: %d frames received, %.2f KB total%n",
                    frameNum, dataVolume.get() / 1024.0);
            System.out.println("-".repeat(80));
        }
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
