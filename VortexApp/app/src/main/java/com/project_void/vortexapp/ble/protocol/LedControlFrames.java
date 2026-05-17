package com.project_void.vortexapp.ble.protocol;

public final class LedControlFrames {
    private LedControlFrames() {}

    private static final int MAX_NAME_BYTES = 18;

    public static byte[] setAnimation(int mode) {
        return new byte[]{(byte) 0x10, 0x01, (byte) (mode & 0xFF)};
    }

    public static byte[] setBrightness(int bVal) {
        return new byte[]{(byte) 0x11, 0x01, (byte) (bVal & 0xFF)};
    }

    public static byte[] setRgb(int rVal, int gVal, int bVal) {
        return new byte[]{(byte) 0x12, 0x03, (byte) rVal, (byte) gVal, (byte) bVal};
    }

    public static byte[] ping(int token) {
        return new byte[]{(byte) 0x7E, 0x01, (byte) (token & 0xFF)};
    }
    public static byte[] getState() {
        return new byte[]{(byte) 0x20, 0x00};
    }

    public static byte[] setDeviceName(String name) {
        byte[] raw = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int len = Math.min(raw.length, MAX_NAME_BYTES);
        byte[] frame = new byte[2 + len];
        frame[0] = (byte) 0x13;
        frame[1] = (byte) len;
        System.arraycopy(raw, 0, frame, 2, len);
        return frame;
    }
}
