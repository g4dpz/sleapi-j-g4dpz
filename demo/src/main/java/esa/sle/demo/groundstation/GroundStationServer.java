package esa.sle.demo.groundstation;

import esa.sle.demo.common.TelemetryFrame;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ground Station Server
 * Receives telemetry frames from spacecraft and provides them via SLE RAF service to MOC
 */
public class GroundStationServer {
    
    private static final int SPACECRAFT_PORT = 5555;
    private static final int SLE_PORT = 5556;
    private static final int FRAME_SIZE = 1115;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final BlockingQueue<byte[]> frameQueue = new LinkedBlockingQueue<>(1000);
    private ServerSocket spacecraftSocket;
    private ServerSocket sleSocket;
    
    public static void main(String[] args) {
        GroundStationServer server = new GroundStationServer();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[GROUND STATION] Shutting down...");
            server.stop();
        }));
        
        server.start();
    }
    
    public void start() {
        running.set(true);
        System.out.println("=".repeat(80));
        System.out.println("GROUND STATION SERVER");
        System.out.println("=".repeat(80));
        System.out.println("Spacecraft Port: " + SPACECRAFT_PORT);
        System.out.println("SLE Service Port: " + SLE_PORT);
        System.out.println("Frame Queue Size: " + frameQueue.remainingCapacity());
        System.out.println("=".repeat(80));
        System.out.println();
        
        // Start spacecraft receiver thread
        Thread spacecraftThread = new Thread(this::receiveFromSpacecraft, "Spacecraft-Receiver");
        spacecraftThread.start();
        
        // Start SLE service thread
        Thread sleThread = new Thread(this::serveSLE, "SLE-Service");
        sleThread.start();
        
        try {
            spacecraftThread.join();
            sleThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[GROUND STATION] Stopped");
    }
    
    private void receiveFromSpacecraft() {
        try {
            spacecraftSocket = new ServerSocket(SPACECRAFT_PORT);
            System.out.println("[GROUND STATION] Listening for spacecraft on port " + SPACECRAFT_PORT);
            
            while (running.get()) {
                try (Socket client = spacecraftSocket.accept()) {
                    System.out.println("[GROUND STATION] Spacecraft connected: " + client.getRemoteSocketAddress());
                    
                    InputStream in = client.getInputStream();
                    byte[] buffer = new byte[FRAME_SIZE];
                    int frameCount = 0;
                    
                    while (running.get()) {
                        int bytesRead = 0;
                        while (bytesRead < FRAME_SIZE) {
                            int read = in.read(buffer, bytesRead, FRAME_SIZE - bytesRead);
                            if (read == -1) {
                                throw new IOException("Connection closed");
                            }
                            bytesRead += read;
                        }
                        
                        // Queue frame for SLE service
                        byte[] frame = new byte[FRAME_SIZE];
                        System.arraycopy(buffer, 0, frame, 0, FRAME_SIZE);
                        
                        if (frameQueue.offer(frame)) {
                            frameCount++;
                            System.out.printf("[GROUND STATION] Received frame #%d from spacecraft (Queue: %d)%n",
                                    frameCount, frameQueue.size());
                        } else {
                            System.err.println("[GROUND STATION] Frame queue full! Dropping frame.");
                        }
                    }
                    
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[GROUND STATION] Spacecraft connection error: " + e.getMessage());
                        System.out.println("[GROUND STATION] Waiting for spacecraft reconnection...");
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("[GROUND STATION] Failed to start spacecraft receiver: " + e.getMessage());
        } finally {
            closeSocket(spacecraftSocket);
        }
    }
    
    private void serveSLE() {
        try {
            sleSocket = new ServerSocket(SLE_PORT);
            System.out.println("[GROUND STATION] SLE RAF service listening on port " + SLE_PORT);
            
            while (running.get()) {
                try (Socket client = sleSocket.accept()) {
                    System.out.println("[GROUND STATION] MOC connected: " + client.getRemoteSocketAddress());
                    
                    var out = client.getOutputStream();
                    int framesSent = 0;
                    
                    while (running.get() && !client.isClosed()) {
                        // Get frame from queue (blocking with timeout)
                        byte[] frame = frameQueue.poll(1, java.util.concurrent.TimeUnit.SECONDS);
                        
                        if (frame != null) {
                            // Send frame to MOC
                            out.write(frame);
                            out.flush();
                            framesSent++;
                            
                            System.out.printf("[GROUND STATION] Forwarded frame #%d to MOC (Queue: %d)%n",
                                    framesSent, frameQueue.size());
                        }
                    }
                    
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[GROUND STATION] MOC connection error: " + e.getMessage());
                        System.out.println("[GROUND STATION] Waiting for MOC reconnection...");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
        } catch (IOException e) {
            System.err.println("[GROUND STATION] Failed to start SLE service: " + e.getMessage());
        } finally {
            closeSocket(sleSocket);
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
        closeSocket(spacecraftSocket);
        closeSocket(sleSocket);
    }
}
