package esa.sle.demo.spacecraft;

import esa.sle.demo.common.CommandFrame;
import esa.sle.demo.common.TelemetryFrame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spacecraft Simulator - Bidirectional
 * Sends telemetry frames and receives/executes commands
 */
public class SpacecraftSimulator {
    
    private static final int SPACECRAFT_ID = 185;
    private static final int VIRTUAL_CHANNEL_ID = 0;
    private static final String GROUND_STATION_HOST = "localhost";
    private static final int DOWNLINK_PORT = 5555;  // Send TM
    private static final int UPLINK_PORT = 5557;    // Receive TC
    private static final int FRAME_RATE_MS = 1000;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger telemetryFrameCount = new AtomicInteger(0);
    private final AtomicInteger commandsReceived = new AtomicInteger(0);
    private volatile int lastCommandFrameCount = -1;  // For CLCW acknowledgment
    
    // Spacecraft state
    private volatile boolean solarPanelsDeployed = false;
    private volatile boolean antennaActive = false;
    private volatile String powerMode = "NOMINAL";
    private volatile String lastCommandAck = "";  // Last command acknowledgment
    
    public static void main(String[] args) {
        SpacecraftSimulator simulator = new SpacecraftSimulator();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SPACECRAFT] Shutting down...");
            simulator.stop();
        }));
        
        simulator.start();
    }
    
    public void start() {
        running.set(true);
        System.out.println("=".repeat(80));
        System.out.println("SPACECRAFT SIMULATOR - BIDIRECTIONAL");
        System.out.println("=".repeat(80));
        System.out.println("Spacecraft ID: " + SPACECRAFT_ID);
        System.out.println("Virtual Channel: " + VIRTUAL_CHANNEL_ID);
        System.out.println("Ground Station: " + GROUND_STATION_HOST);
        System.out.println("Downlink Port: " + DOWNLINK_PORT + " (send TM)");
        System.out.println("Uplink Port: " + UPLINK_PORT + " (receive TC)");
        System.out.println("Frame Rate: " + FRAME_RATE_MS + " ms");
        System.out.println("=".repeat(80));
        System.out.println();
        
        // Start both threads
        Thread downlinkThread = new Thread(this::handleDownlink, "Downlink");
        Thread uplinkThread = new Thread(this::handleUplink, "Uplink");
        
        downlinkThread.start();
        uplinkThread.start();
        
        try {
            downlinkThread.join();
            uplinkThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[SPACECRAFT] Stopped");
    }
    
    /**
     * Send telemetry frames to ground station
     */
    private void handleDownlink() {
        while (running.get()) {
            try (Socket socket = new Socket(GROUND_STATION_HOST, DOWNLINK_PORT)) {
                System.out.println("[DOWNLINK] Connected to Ground Station");
                OutputStream out = socket.getOutputStream();
                
                while (running.get()) {
                    // Generate telemetry message
                    String message = generateTelemetryMessage();
                    
                    // Create and send frame with CLCW acknowledgment
                    TelemetryFrame frame = new TelemetryFrame(
                            SPACECRAFT_ID,
                            VIRTUAL_CHANNEL_ID,
                            telemetryFrameCount.getAndIncrement(),
                            message.getBytes(),
                            lastCommandFrameCount  // Include last command frame count in CLCW
                    );
                    
                    out.write(frame.getData());
                    out.flush();
                    
                    System.out.printf("[DOWNLINK] Sent TM frame #%d (CLCW ACK: %d)%n", 
                            telemetryFrameCount.get(), lastCommandFrameCount);
                    
                    Thread.sleep(FRAME_RATE_MS);
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[DOWNLINK] Connection error: " + e.getMessage());
                    System.out.println("[DOWNLINK] Retrying in 5 seconds...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    /**
     * Receive and execute commands from ground station
     */
    private void handleUplink() {
        // Wait a bit for downlink to establish first
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return;
        }
        
        while (running.get()) {
            try (Socket socket = new Socket(GROUND_STATION_HOST, UPLINK_PORT)) {
                System.out.println("[UPLINK] Connected to Ground Station");
                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[1115];
                
                while (running.get()) {
                    // Read command frame
                    int bytesRead = 0;
                    while (bytesRead < buffer.length) {
                        int read = in.read(buffer, bytesRead, buffer.length - bytesRead);
                        if (read == -1) throw new IOException("Connection closed");
                        bytesRead += read;
                    }
                    
                    // Parse and execute command
                    CommandFrame cmdFrame = new CommandFrame(buffer);
                    String command = cmdFrame.getCommand();
                    int cmdFrameCount = cmdFrame.getFrameCount();
                    
                    commandsReceived.incrementAndGet();
                    lastCommandFrameCount = cmdFrameCount;  // Store for CLCW acknowledgment
                    
                    System.out.printf("[UPLINK] Received command #%d (Frame Count: %d): %s%n",
                            commandsReceived.get(), cmdFrameCount, command);
                    
                    executeCommand(command);
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[UPLINK] Connection error: " + e.getMessage());
                    System.out.println("[UPLINK] Retrying in 5 seconds...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Execute received command
     */
    private void executeCommand(String command) {
        System.out.println("[SPACECRAFT] Executing: " + command);
        
        switch (command.toUpperCase()) {
            case "DEPLOY_SOLAR_PANELS":
                solarPanelsDeployed = true;
                lastCommandAck = "ACK:DEPLOY_SOLAR_PANELS:SUCCESS";
                System.out.println("[SPACECRAFT] ✓ Solar panels deployed");
                break;
                
            case "STOW_SOLAR_PANELS":
                solarPanelsDeployed = false;
                lastCommandAck = "ACK:STOW_SOLAR_PANELS:SUCCESS";
                System.out.println("[SPACECRAFT] ✓ Solar panels stowed");
                break;
                
            case "ACTIVATE_ANTENNA":
                antennaActive = true;
                lastCommandAck = "ACK:ACTIVATE_ANTENNA:SUCCESS";
                System.out.println("[SPACECRAFT] ✓ Antenna activated");
                break;
                
            case "DEACTIVATE_ANTENNA":
                antennaActive = false;
                lastCommandAck = "ACK:DEACTIVATE_ANTENNA:SUCCESS";
                System.out.println("[SPACECRAFT] ✓ Antenna deactivated");
                break;
                
            case "SET_POWER_MODE:LOW":
                powerMode = "LOW";
                lastCommandAck = "ACK:SET_POWER_MODE:LOW:SUCCESS";
                System.out.println("[SPACECRAFT] ✓ Power mode set to LOW");
                break;
                
            case "SET_POWER_MODE:NOMINAL":
                powerMode = "NOMINAL";
                lastCommandAck = "ACK:SET_POWER_MODE:NOMINAL:SUCCESS";
                System.out.println("[SPACECRAFT] ✓ Power mode set to NOMINAL");
                break;
                
            case "SET_POWER_MODE:HIGH":
                powerMode = "HIGH";
                lastCommandAck = "ACK:SET_POWER_MODE:HIGH:SUCCESS";
                System.out.println("[SPACECRAFT] ✓ Power mode set to HIGH");
                break;
                
            case "REQUEST_STATUS":
                lastCommandAck = "ACK:REQUEST_STATUS:SUCCESS";
                System.out.println("[SPACECRAFT] ✓ Status requested (will send in next TM)");
                break;
                
            default:
                lastCommandAck = "ACK:" + command + ":UNKNOWN_COMMAND";
                System.out.println("[SPACECRAFT] ⚠ Unknown command: " + command);
        }
    }
    
    /**
     * Generate telemetry message with current state
     */
    private String generateTelemetryMessage() {
        return String.format("TM Frame #%d | Time: %s | Solar: %s | Antenna: %s | Power: %s | Commands RX: %d",
                telemetryFrameCount.get(),
                Instant.now().toString(),
                solarPanelsDeployed ? "DEPLOYED" : "STOWED",
                antennaActive ? "ACTIVE" : "INACTIVE",
                powerMode,
                commandsReceived.get()
        );
    }
    
    public void stop() {
        running.set(false);
    }
}
