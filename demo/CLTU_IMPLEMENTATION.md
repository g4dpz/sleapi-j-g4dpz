# CCSDS Command Link Transmission Unit (CLTU) Implementation

## Overview

The demo now uses CCSDS CLTU format for uplink commands, providing proper framing, synchronization, and error detection for the command link.

## CLTU Structure

A CLTU consists of three parts:

```
┌─────────────────┬──────────────────────────────────┬─────────────────┐
│ Start Sequence  │      Code Blocks (N blocks)      │  Tail Sequence  │
│    (2 bytes)    │  (8 bytes each: 7 data + 1 BCH)  │    (7 bytes)    │
└─────────────────┴──────────────────────────────────┴─────────────────┘
     0xEB90          Data + BCH parity per block        0xC5C5C5C5C5C5C5
```

### 1. Start Sequence (2 bytes)
- Fixed pattern: `0xEB90`
- Provides synchronization for receiver
- Allows receiver to identify beginning of CLTU

### 2. Code Blocks (variable length)
- Each block: 7 data bytes + 1 BCH parity byte = 8 bytes total
- BCH(63,56) error detection code
- Data is segmented into 7-byte chunks
- Last block padded with fill bytes (0x55) if needed

### 3. Tail Sequence (7 bytes)
- Fixed pattern: `0xC5C5C5C5C5C5C5` (seven 0xC5 bytes)
- Marks end of CLTU
- Allows receiver to detect CLTU completion

## Implementation Details

### CLTU.java
New class that handles CLTU encoding and decoding:

**Encoding (buildCLTU)**:
1. Add start sequence (0xEB90)
2. Segment command frame data into 7-byte chunks
3. Calculate BCH parity for each chunk
4. Append parity byte to create 8-byte code blocks
5. Pad last block with 0x55 if needed
6. Add tail sequence (0xC5C5C5C5C5C5C5)

**Decoding (extractCommandData)**:
1. Verify start sequence
2. Find tail sequence
3. Extract code blocks between start and tail
4. Verify BCH parity for each block
5. Remove fill bytes (0x55)
6. Reassemble original command frame data

### BCH Error Detection
- Simplified BCH(63,56) implementation for demonstration
- Uses XOR checksum with bit rotation
- Production systems would use proper BCH polynomial
- Provides single-byte parity for error detection

### MOCClient.java Changes
- Wraps CommandFrame in CLTU before transmission
- Displays CLTU size and code block count
- Example output:
  ```
  [FSP] Sent command #1: DEPLOY_SOLAR_PANELS
  [FSP] CLTU: 1289 bytes, 160 code blocks
  ```

### SpacecraftSimulator.java Changes
- Reads CLTUs from uplink socket
- Searches for start sequence (0xEB90)
- Reads until tail sequence found
- Decodes CLTU to extract command frame
- Logs CLTU size and decoded frame size

## CLTU Size Calculation

For a 1115-byte command frame:
- Data bytes: 1115
- Code blocks needed: ⌈1115 / 7⌉ = 160 blocks
- Code block bytes: 160 × 8 = 1280 bytes
- Start sequence: 2 bytes
- Tail sequence: 7 bytes
- **Total CLTU size: 1289 bytes**

## Benefits of CLTU

1. **Synchronization**: Start sequence allows receiver to lock onto transmission
2. **Framing**: Clear boundaries with start and tail sequences
3. **Error Detection**: BCH parity detects transmission errors
4. **Robustness**: Can recover from bit slips and synchronization loss
5. **CCSDS Standard**: Industry-standard format used by real space missions

## Test Results

```
Commands Sent: 2
  [FSP] Sent command #1: DEPLOY_SOLAR_PANELS
  [FSP] CLTU: 1289 bytes, 160 code blocks
  [FSP] Sent command #2: ACTIVATE_ANTENNA
  [FSP] CLTU: 1289 bytes, 160 code blocks

Spacecraft Received:
  [UPLINK] Received CLTU: 1289 bytes, decoded to 1115 byte command frame
  [UPLINK] Command #1 (Frame Count: 0): DEPLOY_SOLAR_PANELS
```

## Comparison: Before vs After

### Before (Raw Command Frames)
- Direct transmission of 1115-byte frames
- No synchronization pattern
- No error detection beyond frame CRC
- Difficult to recover from bit errors

### After (CLTU Format)
- 1289-byte CLTUs (15.6% overhead)
- Clear synchronization with start/tail sequences
- BCH error detection on each code block
- Standard CCSDS format

## CCSDS Reference

Implementation based on:
- **CCSDS 231.0-B-3**: TC Synchronization and Channel Coding
- **CCSDS 232.0-B-3**: TC Space Data Link Protocol

## Future Enhancements

1. **Full BCH(63,56)**: Implement proper BCH polynomial for error correction
2. **Randomization**: Add pseudo-randomization for spectral shaping
3. **Multiple CLTUs**: Support batching multiple commands in one transmission
4. **CLTU Statistics**: Track error rates, retransmissions, etc.
5. **Variable Code Block Size**: Support different BCH configurations

## Example CLTU Hex Dump

```
START: EB 90
BLOCK 1: 0B 91 00 00 40 00 54 | A3  (7 data bytes | 1 BCH parity)
BLOCK 2: 4D 20 46 72 61 6D 65 | 2F
BLOCK 3: 20 23 30 20 7C 20 54 | 8B
...
BLOCK 160: 00 00 00 00 00 00 55 | 7E  (padded with 0x55)
TAIL:  C5 C5 C5 C5 C5 C5 C5
```

## Usage

CLTU encoding/decoding is automatic and transparent:
- MOC wraps commands in CLTU before sending
- Ground Station forwards CLTUs unchanged
- Spacecraft decodes CLTU and extracts command frame
- No manual intervention required
