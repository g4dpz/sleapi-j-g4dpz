package esa.sle.demo.groundstation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ground Station Server - Bidirectional
 * DOWNLINK: Spacecraft → Ground Station → MOC (TM frames via RAF)
 * UPLINK: MOC → Ground Station → Spacecraft (TC frames via FSP)
 */
public class GroundStationServer {
    
    // Downlink ports (TM frames)
    private static final int SPACECRAFT_DOWNLINK_PORT = 5555;
    private static final int MOC_RAF_PORT = 5556;
    
    // Uplink ports (TC frames)
    private static final int MOC_FSP_PORT = 5558;
    private static final int SPACECRAFT_UPLINK_PORT = 5557;
    
    private static final int FRAME_SIZE = 1115;
    private static final int BUFFER_SIZE = 1000;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final BlockingQueue<byte[]> telemetryBuffer = new LinkedBlockingQueue<>(BUFFER_SIZE);
    private final BlockingQueue<byte[]> commandBuffer = new LinkedBlockingQueue<>(BUFFER_SIZE);
    
    private ServerSocket spacecraftDownlinkSocket;
    private ServerSocket spacecraftUplinkSocket;
    private ServerSocket mocRafSocket;
    private ServerSocket mocFspSocket;
    
    public static void main(String[] args) {
        GroundStationServer server = new GroundStationServer();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[GROUND STATION] Shutting down...");
            server.stop();
        }));
        
        server.start();
    }
    
    public void start() {
        running.set(true);
        System.out.println("=".repeat(80));
        System.out.println("GROUND STATION SERVER - BIDIRECTIONAL");
        System.out.println("=".repeat(80));
        System.out.println("DOWNLINK (Telemetry):");
        System.out.println("  Spacecraft Port: " + SPACECRAFT_DOWNLINK_PORT + " (receive TM)");
        System.out.println("  MOC RAF Port: " + MOC_RAF_PORT + " (forward TM)");
        System.out.println("UPLINK (Commands):");
        System.out.println("  MOC FSP Port: " + MOC_FSP_PORT + " (receive TC)");
        System.out.println("  Spacecraft Port: " + SPACECRAFT_UPLINK_PORT + " (forward TC)");
        System.out.println("Buffer Size: " + BUFFER_SIZE + " frames each");
        System.out.println("=".repeat(80));
        System.out.println();
        
        // Start all four server threads
        Thread spacecraftDownlinkThread = new Thread(this::handleSpacecraftDownlink, "SC-Downlink");
        Thread spacecraftUplinkThread = new Thread(this::handleSpacecraftUplink, "SC-Uplink");
        Thread mocRafThread = new Thread(this::handleMocRaf, "MOC-RAF");
        Thread mocFspThread = new Thread(this::handleMocFsp, "MOC-FSP");
        
        spacecraftDownlinkThread.start();
        spacecraftUplinkThread.start();
        mocRafThread.start();
        mocFspThread.start();
        
        try {
            spacecraftDownlinkThread.join();
            spacecraftUplinkThread.join();
            mocRafThread.join();
            mocFspThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[GROUND STATION] Stopped");
    }
    
    /**
     * Receive telemetry frames from spacecraft
     */
    private void handleSpacecraftDownlink() {
        try {
            spacecraftDownlinkSocket = new ServerSocket(SPACECRAFT_DOWNLINK_PORT);
            System.out.println("[DOWNLINK] Listening for spacecraft on port " + SPACECRAFT_DOWNLINK_PORT);
            
            while (running.get()) {
                try (Socket client = spacecraftDownlinkSocket.accept()) {
                    System.out.println("[DOWNLINK] Spacecraft connected: " + client.getRemoteSocketAddress());
                    
                    InputStream in = client.getInputStream();
                    byte[] buffer = new byte[FRAME_SIZE];
                    int frameCount = 0;
                    
                    while (running.get()) {
                        int bytesRead = 0;
                        while (bytesRead < FRAME_SIZE) {
                            int read = in.read(buffer, bytesRead, FRAME_SIZE - bytesRead);
                            if (read == -1) throw new IOException("Connection closed");
                            bytesRead += read;
                        }
                        
                        byte[] frame = new byte[FRAME_SIZE];
                        System.arraycopy(buffer, 0, frame, 0, FRAME_SIZE);
                        
                        if (telemetryBuffer.offer(frame)) {
                            frameCount++;
                            System.out.printf("[DOWNLINK] RX TM frame #%d (Queue: %d)%n",
                                    frameCount, telemetryBuffer.size());
                        } else {
                            System.err.println("[DOWNLINK] Telemetry buffer full! Dropping frame.");
                        }
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[DOWNLINK] Error: " + e.getMessage());
                        Thread.sleep(1000);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DOWNLINK] Failed to start: " + e.getMessage());
        } finally {
            closeSocket(spacecraftDownlinkSocket);
        }
    }
    
    /**
     * Forward telemetry frames to MOC via RAF service
     */
    private void handleMocRaf() {
        try {
            mocRafSocket = new ServerSocket(MOC_RAF_PORT);
            System.out.println("[RAF] SLE RAF service listening on port " + MOC_RAF_PORT);
            
            while (running.get()) {
                try (Socket client = mocRafSocket.accept()) {
                    System.out.println("[RAF] MOC connected: " + client.getRemoteSocketAddress());
                    
                    OutputStream out = client.getOutputStream();
                    int framesSent = 0;
                    
                    while (running.get() && !client.isClosed()) {
                        byte[] frame = telemetryBuffer.poll(1, TimeUnit.SECONDS);
                        
                        if (frame != null) {
                            out.write(frame);
                            out.flush();
                            framesSent++;
                            System.out.printf("[RAF] TX TM frame #%d to MOC (Queue: %d)%n",
                                    framesSent, telemetryBuffer.size());
                        }
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[RAF] Error: " + e.getMessage());
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[RAF] Failed to start: " + e.getMessage());
        } finally {
            closeSocket(mocRafSocket);
        }
    }
    
    /**
     * Receive CLTUs from MOC via FSP service
     */
    private void handleMocFsp() {
        try {
            mocFspSocket = new ServerSocket(MOC_FSP_PORT);
            System.out.println("[FSP] SLE FSP service listening on port " + MOC_FSP_PORT);
            
            while (running.get()) {
                try (Socket client = mocFspSocket.accept()) {
                    System.out.println("[FSP] MOC connected: " + client.getRemoteSocketAddress());
                    
                    InputStream in = client.getInputStream();
                    byte[] buffer = new byte[4096]; // Large buffer for CLTUs
                    int frameCount = 0;
                    
                    while (running.get()) {
                        // Read CLTU: start sequence (0xEB90) + code blocks + tail (0xC5 x 7)
                        int bytesRead = 0;
                        
                        // Find start sequence
                        boolean foundStart = false;
                        while (!foundStart && running.get()) {
                            int b1 = in.read();
                            if (b1 == -1) throw new IOException("Connection closed");
                            
                            if (b1 == 0xEB) {
                                int b2 = in.read();
                                if (b2 == -1) throw new IOException("Connection closed");
                                
                                if (b2 == 0x90) {
                                    buffer[bytesRead++] = (byte) b1;
                                    buffer[bytesRead++] = (byte) b2;
                                    foundStart = true;
                                }
                            }
                        }
                        
                        if (!foundStart) continue;
                        
                        // Read until tail sequence (7 consecutive 0xC5 bytes)
                        int tailMatchCount = 0;
                        while (bytesRead < buffer.length && running.get()) {
                            int b = in.read();
                            if (b == -1) throw new IOException("Connection closed");
                            buffer[bytesRead++] = (byte) b;
                            
                            if (b == 0xC5) {
                                tailMatchCount++;
                                if (tailMatchCount >= 7) {
                                    break; // Complete CLTU received
                                }
                            } else {
                                tailMatchCount = 0;
                            }
                        }
                        
                        if (tailMatchCount < 7) {
                            System.err.println("[FSP] CLTU tail sequence not found");
                            continue;
                        }
                        
                        // Extract complete CLTU
                        byte[] cltu = new byte[bytesRead];
                        System.arraycopy(buffer, 0, cltu, 0, bytesRead);
                        
                        if (commandBuffer.offer(cltu)) {
                            frameCount++;
                            System.out.printf("[FSP] RX CLTU #%d from MOC (%d bytes, Queue: %d)%n",
                                    frameCount, cltu.length, commandBuffer.size());
                        } else {
                            System.err.println("[FSP] Command buffer full! Dropping CLTU.");
                        }
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[FSP] Error: " + e.getMessage());
                        Thread.sleep(1000);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[FSP] Failed to start: " + e.getMessage());
        } finally {
            closeSocket(mocFspSocket);
        }
    }
    
    /**
     * Forward CLTUs to spacecraft via uplink
     */
    private void handleSpacecraftUplink() {
        try {
            spacecraftUplinkSocket = new ServerSocket(SPACECRAFT_UPLINK_PORT);
            System.out.println("[UPLINK] Listening for spacecraft on port " + SPACECRAFT_UPLINK_PORT);
            
            while (running.get()) {
                try (Socket client = spacecraftUplinkSocket.accept()) {
                    System.out.println("[UPLINK] Spacecraft connected: " + client.getRemoteSocketAddress());
                    
                    OutputStream out = client.getOutputStream();
                    int cltusSent = 0;
                    
                    while (running.get() && !client.isClosed()) {
                        byte[] cltu = commandBuffer.poll(1, TimeUnit.SECONDS);
                        
                        if (cltu != null) {
                            out.write(cltu);
                            out.flush();
                            cltusSent++;
                            System.out.printf("[UPLINK] TX CLTU #%d to spacecraft (%d bytes, Queue: %d)%n",
                                    cltusSent, cltu.length, commandBuffer.size());
                        }
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[UPLINK] Error: " + e.getMessage());
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[UPLINK] Failed to start: " + e.getMessage());
        } finally {
            closeSocket(spacecraftUplinkSocket);
        }
    }
    
    private void closeSocket(ServerSocket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    public void stop() {
        running.set(false);
        closeSocket(spacecraftDownlinkSocket);
        closeSocket(spacecraftUplinkSocket);
        closeSocket(mocRafSocket);
        closeSocket(mocFspSocket);
    }
}
