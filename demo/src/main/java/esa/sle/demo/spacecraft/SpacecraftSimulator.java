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
        // Simulate spacecraft telemetry data
        ByteBuffer buffer = ByteBuffer.allocate(256);
        
        // Timestamp
        long timestamp = Instant.now().toEpochMilli();
        buffer.putLong(timestamp);
        
        // Simulated sensor data
        buffer.putFloat(random.nextFloat() * 100); // Temperature (°C)
        buffer.putFloat(random.nextFloat() * 50);  // Voltage (V)
        buffer.putFloat(random.nextFloat() * 10);  // Current (A)
        buffer.putInt(random.nextInt(360));        // Attitude (degrees)
        buffer.putInt(random.nextInt(1000));       // Altitude (km)
        
        // Status flags
        buffer.put((byte) (random.nextBoolean() ? 1 : 0)); // Solar panels deployed
        buffer.put((byte) (random.nextBoolean() ? 1 : 0)); // Antenna deployed
        buffer.put((byte) 1); // System nominal
        
        // Fill rest with sequence
        while (buffer.hasRemaining()) {
            buffer.put((byte) (buffer.position() % 256));
        }
        
        return buffer.array();
    }
    
    public void stop() {
        running.set(false);
    }
}
