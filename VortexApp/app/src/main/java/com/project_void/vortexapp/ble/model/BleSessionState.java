package com.project_void.vortexapp.ble.model;

import java.util.Locale;

public record BleSessionState(BleSessionState.Status status, String deviceAddress) {
    public enum Status {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING;

        public String toLowerExceptFirst() {
            String s = name();
            if (s.isEmpty()) return s;
            return s.charAt(0) + s.substring(1).toLowerCase(Locale.ROOT);
        }
    }
}
