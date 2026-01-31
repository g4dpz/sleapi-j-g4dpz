package esa.sle.demo.moc;

import esa.sle.ccsds.utils.cltu.CLTUEncoder;
import esa.sle.ccsds.utils.clcw.CLCWDecoder;
import esa.sle.demo.common.CommandFrame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mission Operations Center (MOC) Client - Bidirectional
 * Receives telemetry via RAF and sends commands via FSP
 */
public class MOCClient {
    
    private static final int SPACECRAFT_ID = 185;
    private static final int VIRTUAL_CHANNEL_ID = 0;
    private static final String GROUND_STATION_HOST = "localhost";
    private static final int RAF_PORT = 5556;  // Receive TM
    private static final int FSP_PORT = 5558;  // Send TC
    private static final int FRAME_SIZE = 1115;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean fspConnected = new AtomicBoolean(false);
    private final AtomicInteger framesReceived = new AtomicInteger(0);
    private final AtomicInteger commandsSent = new AtomicInteger(0);
    private final AtomicInteger dataVolume = new AtomicInteger(0);
    
    private Socket fspSocket;
    private OutputStream commandOutput;
    
    public static void main(String[] args) {
        MOCClient client = new MOCClient();
        
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
        System.out.println("MISSION OPERATIONS CENTER (MOC) CLIENT - AUTOMATED DEMO");
        System.out.println("=".repeat(80));
        System.out.println("Ground Station: " + GROUND_STATION_HOST);
        System.out.println("RAF Service (TM): Port " + RAF_PORT);
        System.out.println("FSP Service (TC): Port " + FSP_PORT);
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("Automated Command Sequence:");
        System.out.println("  Frame 10: DEPLOY_SOLAR_PANELS");
        System.out.println("  Frame 20: ACTIVATE_ANTENNA");
        System.out.println("=".repeat(80));
        System.out.println();
        
        // Start telemetry receiver thread
        Thread rafThread = new Thread(this::handleTelemetry, "RAF-Receiver");
        rafThread.start();
        
        // Start command sender thread
        Thread fspThread = new Thread(this::handleCommands, "FSP-Sender");
        fspThread.start();
        
        try {
            rafThread.join();
            fspThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[MOC] Stopped");
    }
    
    /**
     * Receive telemetry frames via RAF service
     */
    private void handleTelemetry() {
        while (running.get()) {
            try (Socket socket = new Socket(GROUND_STATION_HOST, RAF_PORT)) {
                System.out.println("[RAF] Connected to Ground Station");
                System.out.println("[RAF] Receiving telemetry frames...");
                System.out.println("-".repeat(80));
                
                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[FRAME_SIZE];
                
                while (running.get()) {
                    int bytesRead = 0;
                    while (bytesRead < FRAME_SIZE) {
                        int read = in.read(buffer, bytesRead, FRAME_SIZE - bytesRead);
                        if (read == -1) throw new IOException("Connection closed");
                        bytesRead += read;
                    }
                    
                    processFrame(buffer);
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[RAF] Connection error: " + e.getMessage());
                    System.out.println("[RAF] Retrying in 5 seconds...");
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
     * Send commands via FSP service
     */
    private void handleCommands() {
        // Wait for RAF to connect first
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return;
        }
        
        // Connect to FSP service
        while (running.get() && fspSocket == null) {
            try {
                fspSocket = new Socket(GROUND_STATION_HOST, FSP_PORT);
                commandOutput = fspSocket.getOutputStream();
                fspConnected.set(true);
                System.out.println("[FSP] Connected to Ground Station");
                System.out.println("[FSP] Automated command sequence enabled");
                System.out.println("-".repeat(80));
            } catch (IOException e) {
                System.err.println("[FSP] Connection failed, retrying...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
        
        // Keep connection alive
        try {
            while (running.get() && fspSocket != null && !fspSocket.isClosed()) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Send command to spacecraft
     */
    private void sendCommand(String command) {
        try {
            if (!fspConnected.get() || commandOutput == null) {
                System.out.println("[FSP] Not connected yet, command skipped: " + command);
                return;
            }
            
            // Create command frame
            CommandFrame cmdFrame = new CommandFrame(
                    SPACECRAFT_ID,
                    VIRTUAL_CHANNEL_ID,
                    commandsSent.get(),
                    command
            );
            
            // Wrap in CLTU for uplink using library encoder
            byte[] cltu = CLTUEncoder.encode(cmdFrame.getData());
            
            // Send CLTU
            commandOutput.write(cltu);
            commandOutput.flush();
            
            commandsSent.incrementAndGet();
            int codeBlocks = CLTUEncoder.getCodeBlockCount(cmdFrame.getData().length);
            System.out.printf("[FSP] Sent command #%d: %s%n", commandsSent.get(), command);
            System.out.printf("[FSP] CLTU: %d bytes, %d code blocks%n", cltu.length, codeBlocks);
            
        } catch (IOException e) {
            System.err.println("[FSP] Failed to send command: " + e.getMessage());
        }
    }
    
    /**
     * Process received telemetry frame
     */
    private void processFrame(byte[] frameData) {
        int frameNum = framesReceived.incrementAndGet();
        dataVolume.addAndGet(frameData.length);
        
        // Parse frame header
        ByteBuffer buffer = ByteBuffer.wrap(frameData);
        
        short word1 = buffer.getShort();
        int spacecraftId = (word1 >> 4) & 0x3FF;
        int virtualChannelId = (word1 >> 1) & 0x7;
        
        byte mcFrameCount = buffer.get();
        byte vcFrameCount = buffer.get();
        int frameCount = ((mcFrameCount & 0xFF) << 8) | (vcFrameCount & 0xFF);
        
        buffer.getShort(); // Skip data field status
        
        // Dump first frame structure
        if (frameNum == 1) {
            dumpFrameHex(frameData);
        }
        
        // Extract message from data field
        int dataStart = 6;
        int dataEnd = frameData.length - 6;
        byte[] dataField = new byte[dataEnd - dataStart];
        System.arraycopy(frameData, dataStart, dataField, 0, dataField.length);
        
        int messageLength = 0;
        for (int i = 0; i < dataField.length; i++) {
            if (dataField[i] == 0) {
                messageLength = i;
                break;
            }
        }
        if (messageLength == 0) messageLength = Math.min(256, dataField.length);
        
        String message = new String(dataField, 0, messageLength);
        
        // Decode CLCW from OCF (bytes 1109-1112) using library utility
        int ocfStart = frameData.length - 6;
        buffer.position(ocfStart);
        int clcwWord = buffer.getInt();
        
        // Decode CLCW to extract report value
        CLCWDecoder.CLCW clcw = CLCWDecoder.decode(clcwWord);
        int clcwReportValue = clcw.getReportValue();
        
        // Display frame with CLCW acknowledgment
        String clcwInfo = String.format("CLCW_ACK=%d", clcwReportValue);
        
        System.out.printf("[RAF] Frame #%d | SCID=%d VCID=%d Count=%d | %s | %s%n",
                frameNum, spacecraftId, virtualChannelId, frameCount, clcwInfo, message);
        
        // Automated command sequence
        if (frameNum == 10) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("[AUTO] Triggering command at frame 10...");
            System.out.println("=".repeat(80));
            sendCommand("DEPLOY_SOLAR_PANELS");
        } else if (frameNum == 20) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("[AUTO] Triggering command at frame 20...");
            System.out.println("=".repeat(80));
            sendCommand("ACTIVATE_ANTENNA");
        }
        
        if (frameNum % 10 == 0) {
            System.out.println("-".repeat(80));
            System.out.printf("[RAF] Statistics: %d frames RX, %d commands TX, %.2f KB%n",
                    frameNum, commandsSent.get(), dataVolume.get() / 1024.0);
            System.out.println("-".repeat(80));
        }
    }
    
    private void dumpFrameHex(byte[] frameData) {
        int headerEnd = 6;
        int ocfStart = frameData.length - 6;
        int fecfStart = frameData.length - 2;
        
        StringBuilder hex = new StringBuilder();
        hex.append(formatHexBytes(frameData, 0, 2)).append(" | ");
        hex.append(formatHexBytes(frameData, 2, 2)).append(" | ");
        hex.append(formatHexBytes(frameData, 4, 2)).append(" | ");
        hex.append(formatHexBytes(frameData, headerEnd, 32)).append(" ... | ");
        hex.append(formatHexBytes(frameData, ocfStart, 4)).append(" | ");
        hex.append(formatHexBytes(frameData, fecfStart, 2));
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CCSDS TM FRAME #1:");
        System.out.println(hex.toString());
        System.out.println();
        System.out.println("FIELD DEFINITIONS:");
        System.out.println("  [0-1]   Header: Version+SCID+VCID+OCF_Flag");
        System.out.println("  [2-3]   Header: Frame Counts");
        System.out.println("  [4-5]   Header: Data Field Status");
        System.out.println("  [6-1108] Data Field (1103 bytes)");
        System.out.println("  [1109-1112] OCF - Operational Control Field");
        System.out.println("  [1113-1114] FECF - Frame Error Control (CRC-16)");
        System.out.println("=".repeat(80) + "\n");
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
        System.out.printf("Total Commands Sent: %d%n", commandsSent.get());
        System.out.printf("Total Data Volume: %.2f KB (%.2f MB)%n",
                dataVolume.get() / 1024.0,
                dataVolume.get() / (1024.0 * 1024.0));
        System.out.println("=".repeat(80));
    }
    
    public void stop() {
        running.set(false);
        if (fspSocket != null) {
            try {
                fspSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
