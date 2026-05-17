package com.project_void.vortexapp.protocol;

import com.project_void.vortexapp.ble.protocol.LedControlFrames;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class LedControlFramesTest {

    private static final int OP_SET_NAME  = 0x13;
    private static final int MAX_NAME_BYTES = 18;

    // --- setDeviceName frame structure ---

    @Test
    public void setDeviceName_opcode_is0x13() {
        byte[] frame = LedControlFrames.setDeviceName("abc");
        assertEquals(OP_SET_NAME, frame[0] & 0xFF);
    }

    @Test
    public void setDeviceName_shortName_correctFrameLength() {
        byte[] frame = LedControlFrames.setDeviceName("abc");
        assertEquals(2 + 3, frame.length);
    }

    @Test
    public void setDeviceName_shortName_payloadLengthByte() {
        byte[] frame = LedControlFrames.setDeviceName("abc");
        assertEquals(3, frame[1] & 0xFF);
    }

    @Test
    public void setDeviceName_shortName_payloadBytes() {
        byte[] frame = LedControlFrames.setDeviceName("abc");
        assertEquals('a', frame[2] & 0xFF);
        assertEquals('b', frame[3] & 0xFF);
        assertEquals('c', frame[4] & 0xFF);
    }

    // --- truncation at MAX_NAME_BYTES ---

    @Test
    public void setDeviceName_exactlyMaxLength_notTruncated() {
        String name = "123456789012345678"; // exactly 18 chars
        assertEquals(MAX_NAME_BYTES, name.length());

        byte[] frame = LedControlFrames.setDeviceName(name);
        assertEquals(2 + MAX_NAME_BYTES, frame.length);
        assertEquals(MAX_NAME_BYTES, frame[1] & 0xFF);
    }

    @Test
    public void setDeviceName_overMaxLength_truncatedTo18Bytes() {
        String name = "1234567890123456789"; // 19 chars
        byte[] frame = LedControlFrames.setDeviceName(name);

        assertEquals(2 + MAX_NAME_BYTES, frame.length);
        assertEquals(MAX_NAME_BYTES, frame[1] & 0xFF);

        // payload must be the first 18 ASCII bytes of the name
        byte[] expected = name.substring(0, MAX_NAME_BYTES).getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < MAX_NAME_BYTES; i++) {
            assertEquals("byte at " + i, expected[i], frame[2 + i]);
        }
    }

    // --- empty name ---

    @Test
    public void setDeviceName_emptyString_zeroLengthPayload() {
        byte[] frame = LedControlFrames.setDeviceName("");
        assertEquals(2, frame.length);
        assertEquals(0, frame[1] & 0xFF);
    }

    // --- UTF-8 multi-byte handling ---

    @Test
    public void setDeviceName_utf8Name_byteCountUsedNotCharCount() {
        // "ä" is U+00E4 → 2 bytes in UTF-8 (0xC3 0xA4)
        // 6 such chars = 12 bytes — below the 18-byte cap
        String name = "ääääää"; // 6 × 2 = 12 UTF-8 bytes
        byte[] frame = LedControlFrames.setDeviceName(name);

        assertEquals(2 + 12, frame.length);
        assertEquals(12, frame[1] & 0xFF);
    }

    @Test
    public void setDeviceName_utf8NameExceedsCap_truncatedAtByteLevel() {
        // "ä" is 2 UTF-8 bytes. 10 × "ä" = 20 bytes → truncated to 18 bytes.
        // 18 bytes = 9 complete "ä" characters.
        String name = "ääääääääää"; // 10 × 2 = 20 UTF-8 bytes
        byte[] frame = LedControlFrames.setDeviceName(name);

        assertEquals(2 + MAX_NAME_BYTES, frame.length);
        assertEquals(MAX_NAME_BYTES, frame[1] & 0xFF);

        // The 18 payload bytes must be the first 18 bytes of the UTF-8 encoding
        byte[] raw = name.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < MAX_NAME_BYTES; i++) {
            assertEquals("utf8 byte " + i, raw[i], frame[2 + i]);
        }
    }

    // --- consistency with other frame builders ---

    @Test
    public void setDeviceName_doesNotConflictWithSetAnimation_opcode() {
        byte[] nameFrame = LedControlFrames.setDeviceName("test");
        byte[] animFrame = LedControlFrames.setAnimation(1);
        assertNotEquals(nameFrame[0], animFrame[0]);
    }

    @Test
    public void setDeviceName_doesNotConflictWithSetBrightness_opcode() {
        byte[] nameFrame = LedControlFrames.setDeviceName("test");
        byte[] brightFrame = LedControlFrames.setBrightness(128);
        assertNotEquals(nameFrame[0], brightFrame[0]);
    }

    // ─────────────────────────────────────────────────────────────
    // setAnimation
    // ─────────────────────────────────────────────────────────────

    @Test
    public void setAnimation_opcode_is0x10() {
        assertEquals(0x10, LedControlFrames.setAnimation(0)[0] & 0xFF);
    }

    @Test
    public void setAnimation_frameLength_is3() {
        assertEquals(3, LedControlFrames.setAnimation(5).length);
    }

    @Test
    public void setAnimation_payloadLengthByte_is1() {
        assertEquals(1, LedControlFrames.setAnimation(5)[1] & 0xFF);
    }

    @Test
    public void setAnimation_modeByte_encodedCorrectly() {
        assertEquals(7, LedControlFrames.setAnimation(7)[2] & 0xFF);
    }

    @Test
    public void setAnimation_modeByte_maskedTo8Bits() {
        // 0x1FF & 0xFF = 0xFF
        assertEquals(0xFF, LedControlFrames.setAnimation(0x1FF)[2] & 0xFF);
    }

    @Test
    public void setAnimation_mode0_encodedCorrectly() {
        assertEquals(0, LedControlFrames.setAnimation(0)[2] & 0xFF);
    }

    @Test
    public void setAnimation_mode255_encodedCorrectly() {
        assertEquals(255, LedControlFrames.setAnimation(255)[2] & 0xFF);
    }

    // ─────────────────────────────────────────────────────────────
    // setBrightness
    // ─────────────────────────────────────────────────────────────

    @Test
    public void setBrightness_opcode_is0x11() {
        assertEquals(0x11, LedControlFrames.setBrightness(0)[0] & 0xFF);
    }

    @Test
    public void setBrightness_frameLength_is3() {
        assertEquals(3, LedControlFrames.setBrightness(128).length);
    }

    @Test
    public void setBrightness_payloadLengthByte_is1() {
        assertEquals(1, LedControlFrames.setBrightness(128)[1] & 0xFF);
    }

    @Test
    public void setBrightness_valueByte_encodedCorrectly() {
        assertEquals(128, LedControlFrames.setBrightness(128)[2] & 0xFF);
    }

    @Test
    public void setBrightness_valueByte_maskedTo8Bits() {
        assertEquals(0x01, LedControlFrames.setBrightness(0x101)[2] & 0xFF);
    }

    @Test
    public void setBrightness_value0_encodedCorrectly() {
        assertEquals(0, LedControlFrames.setBrightness(0)[2] & 0xFF);
    }

    @Test
    public void setBrightness_value255_encodedCorrectly() {
        assertEquals(255, LedControlFrames.setBrightness(255)[2] & 0xFF);
    }

    // ─────────────────────────────────────────────────────────────
    // setRgb
    // ─────────────────────────────────────────────────────────────

    @Test
    public void setRgb_opcode_is0x12() {
        assertEquals(0x12, LedControlFrames.setRgb(0, 0, 0)[0] & 0xFF);
    }

    @Test
    public void setRgb_frameLength_is5() {
        assertEquals(5, LedControlFrames.setRgb(1, 2, 3).length);
    }

    @Test
    public void setRgb_payloadLengthByte_is3() {
        assertEquals(3, LedControlFrames.setRgb(1, 2, 3)[1] & 0xFF);
    }

    @Test
    public void setRgb_channelBytes_encodedCorrectly() {
        byte[] frame = LedControlFrames.setRgb(10, 20, 30);
        assertEquals(10, frame[2] & 0xFF);
        assertEquals(20, frame[3] & 0xFF);
        assertEquals(30, frame[4] & 0xFF);
    }

    @Test
    public void setRgb_allMax_encodedCorrectly() {
        byte[] frame = LedControlFrames.setRgb(255, 255, 255);
        assertEquals(255, frame[2] & 0xFF);
        assertEquals(255, frame[3] & 0xFF);
        assertEquals(255, frame[4] & 0xFF);
    }

    @Test
    public void setRgb_allZero_encodedCorrectly() {
        byte[] frame = LedControlFrames.setRgb(0, 0, 0);
        assertEquals(0, frame[2] & 0xFF);
        assertEquals(0, frame[3] & 0xFF);
        assertEquals(0, frame[4] & 0xFF);
    }

    @Test
    public void setRgb_channelsAreIndependent() {
        byte[] frame = LedControlFrames.setRgb(100, 150, 200);
        assertEquals(100, frame[2] & 0xFF);
        assertEquals(150, frame[3] & 0xFF);
        assertEquals(200, frame[4] & 0xFF);
    }

    // ─────────────────────────────────────────────────────────────
    // ping
    // ─────────────────────────────────────────────────────────────

    @Test
    public void ping_opcode_is0x7E() {
        assertEquals(0x7E, LedControlFrames.ping(0)[0] & 0xFF);
    }

    @Test
    public void ping_frameLength_is3() {
        assertEquals(3, LedControlFrames.ping(42).length);
    }

    @Test
    public void ping_payloadLengthByte_is1() {
        assertEquals(1, LedControlFrames.ping(42)[1] & 0xFF);
    }

    @Test
    public void ping_tokenByte_encodedCorrectly() {
        assertEquals(42, LedControlFrames.ping(42)[2] & 0xFF);
    }

    @Test
    public void ping_tokenByte_maskedTo8Bits() {
        assertEquals(0xAB, LedControlFrames.ping(0x1AB)[2] & 0xFF);
    }

    @Test
    public void ping_token0_encodedCorrectly() {
        assertEquals(0, LedControlFrames.ping(0)[2] & 0xFF);
    }

    @Test
    public void ping_token255_encodedCorrectly() {
        assertEquals(255, LedControlFrames.ping(255)[2] & 0xFF);
    }

    // ─────────────────────────────────────────────────────────────
    // getState
    // ─────────────────────────────────────────────────────────────

    @Test
    public void getState_opcode_is0x20() {
        assertEquals(0x20, LedControlFrames.getState()[0] & 0xFF);
    }

    @Test
    public void getState_frameLength_is2() {
        assertEquals(2, LedControlFrames.getState().length);
    }

    @Test
    public void getState_payloadLengthByte_is0() {
        assertEquals(0, LedControlFrames.getState()[1] & 0xFF);
    }

    // ─────────────────────────────────────────────────────────────
    // All opcodes are unique
    // ─────────────────────────────────────────────────────────────

    @Test
    public void allFrameBuilders_haveUniqueOpcodes() {
        int[] opcodes = {
            LedControlFrames.setAnimation(0)[0] & 0xFF,
            LedControlFrames.setBrightness(0)[0] & 0xFF,
            LedControlFrames.setRgb(0, 0, 0)[0] & 0xFF,
            LedControlFrames.setDeviceName("x")[0] & 0xFF,
            LedControlFrames.getState()[0] & 0xFF,
            LedControlFrames.ping(0)[0] & 0xFF,
        };
        for (int i = 0; i < opcodes.length; i++) {
            for (int j = i + 1; j < opcodes.length; j++) {
                assertNotEquals("opcodes[" + i + "] and opcodes[" + j + "] must differ",
                        opcodes[i], opcodes[j]);
            }
        }
    }
}
