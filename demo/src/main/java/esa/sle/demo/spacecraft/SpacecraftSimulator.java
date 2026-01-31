package esa.sle.demo.spacecraft;

import esa.sle.demo.common.TelemetryFrame;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spacecraft Simulator
 * Generates telemetry frames and sends them to the Ground Station
 */
public class SpacecraftSimulator {
    
    private static final int SPACECRAFT_ID = 185; // Example spacecraft ID (0xB9)
    private static final int VIRTUAL_CHANNEL_ID = 0;
    private static final String GROUND_STATION_HOST = "localhost";
    private static final int GROUND_STATION_PORT = 5555;
    private static final int FRAME_RATE_MS = 1000; // 1 frame per second
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private int frameCount = 0;
    private final Random random = new Random();
    
    public static void main(String[] args) {
        SpacecraftSimulator simulator = new SpacecraftSimulator();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SPACECRAFT] Shutting down...");
            simulator.stop();
        }));
        
        simulator.start();
    }
    
    public void start() {
        running.set(true);
        System.out.println("=".repeat(80));
        System.out.println("SPACECRAFT SIMULATOR");
        System.out.println("=".repeat(80));
        System.out.println("Spacecraft ID: " + SPACECRAFT_ID);
        System.out.println("Virtual Channel: " + VIRTUAL_CHANNEL_ID);
        System.out.println("Ground Station: " + GROUND_STATION_HOST + ":" + GROUND_STATION_PORT);
        System.out.println("Frame Rate: " + FRAME_RATE_MS + " ms");
        System.out.println("=".repeat(80));
        System.out.println();
        
        while (running.get()) {
            try (Socket socket = new Socket(GROUND_STATION_HOST, GROUND_STATION_PORT)) {
                System.out.println("[SPACECRAFT] Connected to Ground Station");
                OutputStream out = socket.getOutputStream();
                
                while (running.get()) {
                    // Generate telemetry frame
                    TelemetryFrame frame = generateFrame();
                    
                    // Send frame to ground station
                    out.write(frame.getData());
                    out.flush();
                    
                    System.out.printf("[SPACECRAFT] Sent: %s%n", frame);
                    
                    // Wait before sending next frame
                    Thread.sleep(FRAME_RATE_MS);
                }
                
            } catch (IOException e) {
                System.err.println("[SPACECRAFT] Connection error: " + e.getMessage());
                System.out.println("[SPACECRAFT] Retrying in 5 seconds...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    break;
                }
            } catch (InterruptedException e) {
                break;
            }
        }
        
        System.out.println("[SPACECRAFT] Stopped");
    }
    
    private TelemetryFrame generateFrame() {
        // Generate sample telemetry data
        byte[] payload = generateTelemetryData();
        
        TelemetryFrame frame = new TelemetryFrame(
                SPACECRAFT_ID,
                VIRTUAL_CHANNEL_ID,
                frameCount++,
                payload
        );
        
        return frame;
    }
    
    private byte[] generateTelemetryData() {
        // Simple text message as payload
        String message = String.format("Hello from Spacecraft! Frame #%d, Time: %s", 
                frameCount, Instant.now().toString());
        
        byte[] messageBytes = message.getBytes();
        byte[] payload = new byte[256];
        
        // Copy message to payload
        System.arraycopy(messageBytes, 0, payload, 0, Math.min(messageBytes.length, payload.length));
        
        return payload;
    }
    
    public void stop() {
        running.set(false);
    }
}
